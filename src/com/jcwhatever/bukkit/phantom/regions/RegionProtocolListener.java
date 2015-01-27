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

package com.jcwhatever.bukkit.phantom.regions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangeFactory;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.IBlockDigPacket;
import com.jcwhatever.bukkit.phantom.packets.IBlockPlacePacket;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.translators.BlockPacketTranslator;
import com.jcwhatever.nucleus.Nucleus;
import com.jcwhatever.nucleus.regions.data.ChunkBlockInfo;
import com.jcwhatever.nucleus.regions.data.WorldInfo;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.Scheduler;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/*
 * 
 */
public class RegionProtocolListener extends PacketAdapter {

    private final PhantomRegionManager _manager;

    public RegionProtocolListener(Plugin plugin, PhantomRegionManager manager) {
        super(plugin, Server.MAP_CHUNK,
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

        if (type == Client.BLOCK_PLACE) {

            IBlockPlacePacket blockPlace = PhantomPackets.getNms().getBlockPlacePacket(packet);

            repairPhantomBlock(event.getPlayer(), blockPlace.getX(), blockPlace.getY(), blockPlace.getZ());
        }
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

        if (!_manager.hasRegionInWorld(world))
            return;

        PacketType type = event.getPacketType();

        WorldInfo worldInfo = new WorldInfo(world);

        PacketContainer packet = event.getPacket();

        if (type == Server.BLOCK_CHANGE) {

            IBlockChangePacket wrapper = PhantomPackets.getNms().getBlockChangePacket(packet);

            List<PhantomRegion> regions = Nucleus.getRegionManager()
                    .getRegions(world, wrapper.getX(), wrapper.getY(), wrapper.getZ(), PhantomRegion.class);

            if (regions.isEmpty())
                return;

            IBlockChangePacket clone = null;
            boolean isChanged = false;

            for (PhantomRegion phantom : regions) {

                if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                    continue;

                if (!phantom.canSee(event.getPlayer()))
                    continue;

                if (clone == null) {
                    clone = wrapper.clonePacket();
                }

                if (BlockPacketTranslator.translateBlockChange(
                        clone, worldInfo, phantom.getBlockPacketTranslator())) {
                    isChanged = true;
                }
            }

            if (isChanged) {
                clone.saveChanges();
                event.setPacket(clone.getPacket());
            }
        }
        else if (type == Server.MULTI_BLOCK_CHANGE) {

            IMultiBlockChangePacket wrapper = PhantomPackets.getNms().getMultiBlockChangePacket(packet);

            List<PhantomRegion> regions = Nucleus.getRegionManager()
                    .getRegionsInChunk(world, wrapper.getChunkX(), wrapper.getChunkZ(), PhantomRegion.class);

            if (regions.isEmpty())
                return;

            IMultiBlockChangePacket cloned = null;

            for (PhantomRegion phantom : regions) {

                if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                    continue;

                if (!phantom.canSee(event.getPlayer()))
                    continue;

                if (cloned == null) {
                    cloned = wrapper.clonePacket();
                }

                BlockPacketTranslator.translateMultiBlockChange(
                        cloned, worldInfo, phantom.getBlockPacketTranslator());

            }

            if (cloned != null) {
                cloned.saveChanges();
                event.setPacket(cloned.getPacket());
            }
        }
        else if (type == Server.MAP_CHUNK) {

            StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
            int chunkX = integers.read(0);
            int chunkZ = integers.read(1);

            List<PhantomRegion> regions = Nucleus.getRegionManager()
                    .getRegionsInChunk(world, chunkX, chunkZ, PhantomRegion.class);

            if (regions.isEmpty())
                return;

            for (PhantomRegion phantom : regions) {

                if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                    continue;

                if (!phantom.canSee(event.getPlayer()))
                    continue;

                BlockPacketTranslator.translateMapChunk(packet, phantom);
            }
        }
        else if (type == Server.MAP_CHUNK_BULK) {

            StructureModifier<int[]> integerArrays = packet.getSpecificModifier(int[].class);
            int[] chunkXArray = integerArrays.read(0);
            int[] chunkZArray = integerArrays.read(1);

            for (int i = 0; i < chunkXArray.length; i++) {

                int chunkX = chunkXArray[i];
                int chunkZ = chunkZArray[i];


                List<PhantomRegion> regions = Nucleus.getRegionManager()
                        .getRegionsInChunk(world, chunkX, chunkZ, PhantomRegion.class);

                if (regions.isEmpty())
                    return;

                for (PhantomRegion phantom : regions) {

                    if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                        continue;

                    if (!phantom.canSee(event.getPlayer()))
                        continue;

                    BlockPacketTranslator.translateMapChunkBulk(packet, phantom);
                }
            }
        }
    }

    private void repairPhantomBlock(final Player player, int x, int y, int z) {

        if (!PhantomPackets.getPlugin().getRegionManager().hasRegionInWorld(player.getWorld()))
            return;

        List<PhantomRegion> regions = Nucleus.getRegionManager().getRegions(
                player.getWorld(), x, y, z, PhantomRegion.class);

        if (regions.isEmpty())
            return;

        World world = player.getWorld();
        WorldInfo worldInfo = new WorldInfo(world);

        for (PhantomRegion phantom : regions) {

            if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                continue;

            if (!phantom.canSee(player))
                continue;

            ChunkBlockInfo blockInfo = phantom.getBlockPacketTranslator()
                    .translate(worldInfo, x, y, z, null, (byte)0);

            if (blockInfo == null ||
                    (blockInfo.getMaterial() == Material.AIR && phantom.ignoresAir())) {
                continue;
            }

            IBlockChangeFactory factory = PhantomPackets.getNms().getBlockChangeFactory(
                    x, y, z, blockInfo.getMaterial(), (byte)blockInfo.getData());

            final PacketContainer packet = factory.createPacket();

            Scheduler.runTaskLater(PhantomPackets.getPlugin(), 1, new Runnable() {
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
}
