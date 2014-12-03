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
import com.jcwhatever.bukkit.generic.regions.data.ChunkBlockInfo;
import com.jcwhatever.bukkit.generic.utils.EntryValidator;
import com.jcwhatever.bukkit.phantom.data.ChunkBulkData;
import com.jcwhatever.bukkit.phantom.data.ChunkData;
import com.jcwhatever.bukkit.phantom.data.ChunkDataBlockIterator;
import com.jcwhatever.bukkit.phantom.data.ChunkInfo;
import com.jcwhatever.bukkit.phantom.data.IChunkCoordinates;
import com.jcwhatever.bukkit.phantom.data.IChunkData;
import com.jcwhatever.bukkit.phantom.data.WorldInfo;
import com.jcwhatever.bukkit.phantom.packets.BlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket.PacketBlock;

import org.bukkit.Material;

/*
 * 
 */
public class BlockPacketTranslator {


    public boolean translateBlockChange(BlockChangePacket packet, WorldInfo world,
                                        BlockTypeTranslator translator) {

        // TODO: Use of NMS code breaks with version changes

        int x = packet.getX();
        int y = packet.getY();
        int z = packet.getZ();

        ChunkBlockInfo info = translator.translate(world, x, y, z, packet.getMaterial(), packet.getMeta());
        if (info == null)
            return false;

        packet.setBlock(info.getMaterial(), (byte) info.getData());

        return true;
    }

    public boolean translateMultiBlockChange(MultiBlockChangePacket packet, WorldInfo world,
                                              BlockTypeTranslator translator,
                                              EntryValidator<IChunkCoordinates> chunkValidator) {

        boolean isChanged = false;

        int chunkX = packet.getChunkX();
        int chunkZ = packet.getChunkZ();

        if (!chunkValidator.isValid(new ChunkInfo(world, chunkX, chunkZ)))
            return false;

        for (PacketBlock block : packet) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            ChunkBlockInfo info = translator.translate(world, x, y, z, block.getMaterial(), block.getMeta());
            if (info == null)
                continue;

            isChanged = true;

            block.setBlock(info.getMaterial(), info.getData());
        }

        return isChanged;
    }


    public void translateMapChunk(PacketContainer packet, WorldInfo world,
                                   BlockTypeTranslator translator,
                                   EntryValidator<IChunkCoordinates> chunkValidator) {

        ChunkData data = ChunkData.fromMapChunkPacket(packet, world);

        if (data.getData() == null)
            return;

        if (data.getBlockSize() > data.getData().length)
            return;

        if (!chunkValidator.isValid(data))
            return;

        translateChunkData(data, translator);
    }

    public void translateMapChunkBulk(PacketContainer packet, WorldInfo world,
                                      BlockTypeTranslator translator,
                                      EntryValidator<IChunkCoordinates> chunkValidator) {

        ChunkBulkData bulkData = ChunkBulkData.fromMapChunkBulkPacket(packet, world);

        IChunkData[] dataArray =  bulkData.getChunkData();

        for (IChunkData data : dataArray) {

            if (!chunkValidator.isValid(data))
                continue;

            translateChunkData(data, translator);
        }
    }

    private void translateChunkData(IChunkData chunkData, BlockTypeTranslator translator) {

        ChunkDataBlockIterator iterator = new ChunkDataBlockIterator(chunkData);

        while (iterator.hasNext()) {
            iterator.next();

            int blockId = iterator.getBlockId();
            Material material = Material.getMaterial(blockId);
            byte meta = iterator.getBlockMeta();

            ChunkBlockInfo info = translator.translate(
                    chunkData.getWorld(),
                    iterator.getX(),
                    iterator.getY(),
                    iterator.getZ(),
                    material,
                    meta);

            if (info == null)
                continue;

            iterator.setBlockMaterial(info.getMaterial());
            iterator.setBlockMeta(info.getData());
            iterator.setBlockLight(info.getEmittedLight());

            if (iterator.hasSkylight())
                iterator.setSkylight(info.getSkylight());
        }
    }
}
