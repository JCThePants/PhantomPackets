/*
 * This file is part of PhantomPackets for Bukkit, licensed under the MIT License (MIT).
 *
 * Copyright (c) JCThePants (www.jcwhatever.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jcwhatever.phantom.blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.nucleus.managed.scheduler.Scheduler;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.phantom.IPhantomBlock;
import com.jcwhatever.phantom.IPhantomBlockContext;
import com.jcwhatever.phantom.IBlockContextManager;
import com.jcwhatever.phantom.PhantomPackets;
import com.jcwhatever.phantom.nms.factory.IBlockChangeFactory;
import com.jcwhatever.phantom.nms.packets.IBlockChangePacket;
import com.jcwhatever.phantom.nms.packets.IBlockDigPacket;
import com.jcwhatever.phantom.nms.packets.IBlockPlacePacket;
import com.jcwhatever.phantom.nms.packets.IMultiBlockChangePacket;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Protocol listener for block contexts.
 */
public class BlocksProtocolListener extends PacketAdapter {

    private final IBlockContextManager _manager;

    public BlocksProtocolListener(IBlockContextManager manager) {
        super(PhantomPackets.getPlugin(), Server.MAP_CHUNK,
                Server.MAP_CHUNK_BULK,
                Server.BLOCK_CHANGE,
                Server.MULTI_BLOCK_CHANGE,
                Client.BLOCK_DIG,
                Client.BLOCK_PLACE);

        PreCon.notNull(manager);

        _manager = manager;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {

        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();

        /* Block Place */
        if (type == Client.BLOCK_PLACE) {

            IBlockPlacePacket blockPlace = PhantomPackets.getNms().getBlockPlacePacket(packet);

            repairPhantomBlock(
                    event.getPlayer(), blockPlace.getX(), blockPlace.getY(), blockPlace.getZ());
        }
        /* Block Dig */
        else if (type == Client.BLOCK_DIG) {

            IBlockDigPacket dig = PhantomPackets.getNms().getBlockDigPacket(packet);

            // Packet isn't handled by minecraft if the block material is air,
            // so its handled here
            repairPhantomBlock(event.getPlayer(), dig.getX(), dig.getY(), dig.getZ());
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {

        World world = event.getPlayer().getWorld();

        if (!_manager.hasPhantomBlocksInWorld(world))
            return;

        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();

        /* Block Change */
        if (type == Server.BLOCK_CHANGE) {

            IBlockChangePacket wrapper = PhantomPackets.getNms().getBlockChangePacket(packet);

            IPhantomBlock block = _manager.getBlockAt(
                    world, wrapper.getX(), wrapper.getY(), wrapper.getZ());
            if (block == null)
                return;

            if (!block.canSee(event.getPlayer()))
                return;

            IBlockChangePacket clone = wrapper.clonePacket();

            clone.setBlock(block.getMaterial(), block.getData());
            clone.saveChanges();
            event.setPacket(clone.getPacket());
        }
        /* Multi Block Change */
        else if (type == Server.MULTI_BLOCK_CHANGE) {

            IMultiBlockChangePacket wrapper = PhantomPackets.getNms().getMultiBlockChangePacket(packet);

            Collection<IPhantomBlockContext> contexts = _manager.getChunkContexts(
                    world, wrapper.getChunkX(), wrapper.getChunkZ());
            if (contexts.isEmpty())
                return;

            for (IPhantomBlockContext context : contexts) {

                if (!context.canSee(event.getPlayer()))
                    continue;

                IMultiBlockChangePacket cloned = wrapper.clonePacket();

                context.translateMultiBlock(cloned);
                cloned.saveChanges();
                event.setPacket(cloned.getPacket());
            }
        }
        /* Map Chunk */
        else if (type == Server.MAP_CHUNK) {

            StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
            int chunkX = integers.read(0);
            int chunkZ = integers.read(1);

            Collection<IPhantomBlockContext> contexts = _manager.getChunkContexts(
                    world, chunkX, chunkZ);
            if (contexts.isEmpty())
                return;

            for (IPhantomBlockContext context : contexts) {
                if (!context.canSee(event.getPlayer()))
                    continue;

                context.translateMapChunk(packet);
            }
        }
        /* Map Chunk Bulk */
        else if (type == Server.MAP_CHUNK_BULK) {

            StructureModifier<int[]> integerArrays = packet.getSpecificModifier(int[].class);
            int[] chunkXArray = integerArrays.read(0);
            int[] chunkZArray = integerArrays.read(1);

            for (int i = 0; i < chunkXArray.length; i++) {

                int chunkX = chunkXArray[i];
                int chunkZ = chunkZArray[i];

                Collection<IPhantomBlockContext> contexts = _manager.getChunkContexts(
                        world, chunkX, chunkZ);
                if (contexts.isEmpty())
                    return;

                for (IPhantomBlockContext context : contexts) {
                    if (!context.canSee(event.getPlayer()))
                        continue;

                    context.translateMapChunkBulk(packet);
                }
            }
        }
    }

    private void repairPhantomBlock(final Player player, int x, int y, int z) {

        World world = player.getWorld();

        IPhantomBlock block = PhantomPackets.getBlockContexts().getBlockAt(world, x, y, z);
        if (block == null)
            return;

        if (!block.canSee(player))
            return;

        IBlockChangeFactory factory = PhantomPackets.getNms().getBlockChangeFactory(
                x, y, z, block.getMaterial(), block.getData());

        final PacketContainer packet = factory.createPacket();

        Scheduler.runTaskLater(PhantomPackets.getPlugin(), new Runnable() {
            @Override
            public void run() {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
