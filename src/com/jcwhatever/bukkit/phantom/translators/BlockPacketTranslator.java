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
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.jcwhatever.bukkit.generic.regions.data.ChunkBlockInfo;
import com.jcwhatever.bukkit.generic.utils.EntryValidator;
import com.jcwhatever.bukkit.phantom.data.ChunkBulkData;
import com.jcwhatever.bukkit.phantom.data.ChunkData;
import com.jcwhatever.bukkit.phantom.data.ChunkDataBlockIterator;
import com.jcwhatever.bukkit.phantom.data.ChunkInfo;
import com.jcwhatever.bukkit.phantom.data.IChunkData;
import com.jcwhatever.bukkit.phantom.data.IChunkCoordinates;
import com.jcwhatever.bukkit.phantom.data.WorldInfo;

/*
 * 
 */
public class BlockPacketTranslator {


    public boolean translateBlockChange(PacketContainer packet, WorldInfo world,
                                        BlockTypeTranslator translator) {

        StructureModifier<Integer> ints = packet.getIntegers();
        int x = ints.read(0);
        int y = ints.read(1);
        int z = ints.read(2);

        ChunkBlockInfo info = translator.translate(world, x, y, z, 0);
        if (info == null)
            return false;

        packet.getBlocks().write(0, info.getMaterial());
        ints.write(3, info.getData());

        return true;
    }

    public boolean translateMultiBlockChange(PacketContainer packet, WorldInfo world,
                                              BlockTypeTranslator translator,
                                              EntryValidator<IChunkCoordinates> chunkValidator) {

        StructureModifier<byte[]> byteArrays = packet.getByteArrays();

        boolean isChanged = false;

        ChunkInfo chunkCoords = getChunkInfo(packet, world);

        if (!chunkValidator.isValid(chunkCoords))
            return false;

        byte[] data = byteArrays.read(0);

        int xStart = chunkCoords.getX() * 16;
        int zStart = chunkCoords.getZ() * 16;

        for (int position=0; position < data.length; position += 4) {

            int xz = data[position];
            int y = data[position + 1];

            int z = zStart + (xz & 0x0F);
            int x = xStart + (xz >> 4);

            int lower = data[position + 2];
            int upper = data[position + 3];
            int blockId = (lower << 8) | upper;

            ChunkBlockInfo info = translator.translate(world, x, y, z, blockId);
            if (info != null) {

                blockId = (info.getMaterial().getId() << 4) | info.getData();

                lower = blockId >> 8;
                upper = blockId & 0x00FF;

                data[position + 2] = (byte)lower;
                data[position + 3] = (byte)upper;

                isChanged = true;
            }
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

            ChunkBlockInfo info = translator.translate(
                    chunkData.getWorld(),
                    iterator.getX(),
                    iterator.getY(),
                    iterator.getZ(),
                    iterator.getBlockId());

            if (info == null)
                continue;

            iterator.setBlockId(info.getMaterial().getId());
            iterator.setBlockMeta(info.getData());
            iterator.setBlockLight(info.getEmittedLight());

            if (iterator.hasSkylight())
                iterator.setSkylight(info.getSkylight());
        }
    }


    private ChunkInfo getChunkInfo(PacketContainer packet, WorldInfo world) {
        StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);

        if (integers.size() >= 2) {
            return new ChunkInfo(world, integers.read(0), integers.read(1));
        }
        else {

            Object handle = packet.getModifier().read(0);

            FieldAccessor xField = Accessors.getFieldAccessor(handle.getClass(), "x", true);
            FieldAccessor zField = Accessors.getFieldAccessor(handle.getClass(), "z", true);

            return new ChunkInfo(world, (Integer)xField.get(handle), (Integer)zField.get(handle));
        }
    }
}
