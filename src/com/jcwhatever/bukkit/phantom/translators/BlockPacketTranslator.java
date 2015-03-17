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

package com.jcwhatever.bukkit.phantom.translators;

import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.nucleus.regions.data.ChunkBlockInfo;
import com.jcwhatever.nucleus.regions.data.WorldInfo;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.data.ChunkBulkData;
import com.jcwhatever.bukkit.phantom.data.ChunkData;
import com.jcwhatever.bukkit.phantom.data.IChunkData;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.PacketBlock;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;

import org.bukkit.Material;

import java.util.List;

/*
 * 
 */
public class BlockPacketTranslator {

    private BlockPacketTranslator() {}

    public static boolean translateBlockChange(IBlockChangePacket packet, WorldInfo world,
                                        BlockTypeTranslator translator) {
        int x = packet.getX();
        int y = packet.getY();
        int z = packet.getZ();

        ChunkBlockInfo info = translator.translate(
                world, x, y, z, packet.getMaterial(), packet.getMeta());
        if (info == null)
            return false;

        packet.setBlock(info.getMaterial(), (byte) info.getData());

        return true;
    }

    public static boolean translateMultiBlockChange(IMultiBlockChangePacket packet, WorldInfo world,
                                              BlockTypeTranslator translator) {

        boolean isChanged = false;

        for (PacketBlock block : packet) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            ChunkBlockInfo info = translator.translate(
                    world, x, y, z, block.getMaterial(), block.getMeta());
            if (info == null)
                continue;

            isChanged = true;

            block.setBlock(info.getMaterial(), info.getData());
        }

        return isChanged;
    }

    public static void translateMapChunk(PacketContainer packet, PhantomRegion region) {

        ChunkData data = PhantomPackets.getNms().getChunkData(packet, new WorldInfo(region.getWorld()));

        if (data.getData() == null)
            return;

        if (data.getBlockSize() > data.getData().length)
            return;

        translateChunkData(region, data);
    }

    public static void translateMapChunkBulk(PacketContainer packet, PhantomRegion region) {

        ChunkBulkData bulkData = PhantomPackets.getNms().getChunkBulkData(packet, new WorldInfo(region.getWorld()));

        IChunkData[] dataArray =  bulkData.getChunkData();

        for (IChunkData data : dataArray) {

            translateChunkData(region, data);
        }
    }

    private static void translateChunkData(PhantomRegion region, IChunkData chunkData) {

        List<ChunkBlockInfo> blocks = region.getChunkBlocks(chunkData);
        for (ChunkBlockInfo info : blocks) {

            if (info.getMaterial() == Material.AIR && region.ignoresAir())
                continue;

            chunkData.setBlock(info.getX(), info.getY(), info.getZ(), info.getMaterial(), (byte) info.getData());
        }
    }
}
