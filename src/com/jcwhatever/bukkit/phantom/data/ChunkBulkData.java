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

package com.jcwhatever.bukkit.phantom.data;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.generic.regions.data.WorldInfo;
import com.jcwhatever.bukkit.generic.utils.reflection.ReflectedArray;
import com.jcwhatever.bukkit.phantom.NmsTypes;

public class ChunkBulkData {

    private final WorldInfo _world;

    private IChunkData[] _chunkData;

    ChunkBulkData (WorldInfo world) {
        _world = world;
    }

    public WorldInfo getWorld() {
        return _world;
    }

    public int getTotalChunks() {
        return _chunkData.length;
    }

    public IChunkData[] getChunkData() {
        return _chunkData;
    }

    public static ChunkBulkData fromMapChunkBulkPacket(PacketContainer packet, WorldInfo world) {

        ChunkBulkData bulk = new ChunkBulkData(world);
        StructureModifier<Object> objects = packet.getModifier();

        int[] chunkX = (int[])objects.read(0);
        int[] chunkZ = (int[])objects.read(1);

        ReflectedArray<?> nmsChunkMaps = new ReflectedArray<>(NmsTypes.CHUNK_MAP, objects.read(2));

        int totalChunks = nmsChunkMaps.length();

        bulk._chunkData = new IChunkData[totalChunks];

        // iterate over chunk data and create ChunkData instance for each chunk
        for (int i=0; i < totalChunks; i++) {

            ChunkData chunkData = new ChunkData(world);
            bulk._chunkData[i] = chunkData;

            int x = chunkX[i];
            int z = chunkZ[i];
            byte[] data = nmsChunkMaps.getReflected(i).getFields().get(0);//.a  chunk data
            int mask = nmsChunkMaps.getReflected(i).getFields().get(1); //.b  section mask

            chunkData.init(x, z, mask, data, true);
        }

        return bulk;
    }
}
