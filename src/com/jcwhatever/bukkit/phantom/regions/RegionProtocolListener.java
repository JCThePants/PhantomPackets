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
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.generic.GenericsLib;
import com.jcwhatever.bukkit.generic.regions.IRegion;
import com.jcwhatever.bukkit.generic.regions.data.WorldInfo;
import com.jcwhatever.bukkit.generic.utils.PreCon;
import com.jcwhatever.bukkit.phantom.packets.BlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.translators.BlockPacketTranslator;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;

/*
 * 
 */
public class RegionProtocolListener extends PacketAdapter {

    private final PhantomRegionManager _manager;

    public RegionProtocolListener(Plugin plugin, PhantomRegionManager manager) {
        super(plugin, Server.MAP_CHUNK,
                Server.MAP_CHUNK_BULK,
                Server.BLOCK_CHANGE,
                Server.MULTI_BLOCK_CHANGE);

        PreCon.notNull(manager);

        _manager = manager;
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

            BlockChangePacket wrapper = new BlockChangePacket(packet);

            List<IRegion> regions = GenericsLib.getRegionManager()
                    .getRegions(world, wrapper.getX(), wrapper.getY(), wrapper.getZ());

            if (regions.isEmpty())
                return;

            BlockChangePacket clone = null;
            boolean isChanged = false;

            for (IRegion region : regions) {
                PhantomRegion phantom = region.getMeta(PhantomRegion.REGION_KEY);
                if (phantom == null)
                    return;

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

            MultiBlockChangePacket wrapper = new MultiBlockChangePacket(packet);

            Set<IRegion> regions = GenericsLib.getRegionManager()
                    .getRegionsInChunk(world, wrapper.getChunkX(), wrapper.getChunkZ());

            if (regions.isEmpty())
                return;

            MultiBlockChangePacket cloned = null;

            for (IRegion region : regions) {
                PhantomRegion phantom = region.getMeta(PhantomRegion.REGION_KEY);
                if (phantom == null)
                    continue;

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

            Set<IRegion> regions = GenericsLib.getRegionManager()
                    .getRegionsInChunk(world, chunkX, chunkZ);

            if (regions.isEmpty())
                return;

            for (IRegion region : regions) {
                PhantomRegion phantom = region.getMeta(PhantomRegion.REGION_KEY);
                if (phantom == null)
                    continue;

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


                Set<IRegion> regions = GenericsLib.getRegionManager()
                        .getRegionsInChunk(world, chunkX, chunkZ);

                if (regions.isEmpty())
                    return;

                for (IRegion region : regions) {
                    PhantomRegion phantom = region.getMeta(PhantomRegion.REGION_KEY);
                    if (phantom == null)
                        continue;

                    if (phantom.isLoading() || phantom.isSaving() || phantom.isBuilding())
                        continue;

                    if (!phantom.canSee(event.getPlayer()))
                        continue;

                    BlockPacketTranslator.translateMapChunkBulk(packet, phantom);

                }

            }
        }

    }
}
