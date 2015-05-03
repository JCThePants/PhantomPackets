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

package com.jcwhatever.phantom.nms.v1_8_R2;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.nucleus.utils.coords.ICoords2Di;
import com.jcwhatever.nucleus.utils.nms.INmsHandler;
import com.jcwhatever.phantom.IPhantomChunk;
import com.jcwhatever.phantom.data.ChunkBulkData;
import com.jcwhatever.phantom.data.ChunkData;
import com.jcwhatever.phantom.data.IChunkData;
import com.jcwhatever.phantom.nms.factory.IBlockChangeFactory;
import com.jcwhatever.phantom.nms.factory.IMultiBlockChangeFactory;
import com.jcwhatever.phantom.nms.packets.IBlockDigPacket;
import com.jcwhatever.phantom.nms.packets.IBlockPlacePacket;
import com.jcwhatever.phantom.nms.packets.IMultiBlockChangePacket;
import com.jcwhatever.phantom.nms.v1_8_R2.factory.BlockChangeFactory_v1_8_R2;
import com.jcwhatever.phantom.nms.v1_8_R2.factory.MultiBlockChangeFactory_v1_8_R2;
import com.jcwhatever.phantom.nms.v1_8_R2.packets.BlockChangePacket_v1_8_R2;
import com.jcwhatever.phantom.nms.v1_8_R2.packets.BlockDigPacket_v1_8_R2;
import com.jcwhatever.phantom.nms.v1_8_R2.packets.BlockPlacePacket_v1_8_R2;
import com.jcwhatever.phantom.nms.v1_8_R2.packets.MultiBlockChangePacket_v1_8_R2;

import org.bukkit.Material;
import org.bukkit.World;

import net.minecraft.server.v1_8_R2.BaseBlockPosition;
import net.minecraft.server.v1_8_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R2.PacketPlayOutMapChunk.ChunkMap;

/*
 * 
 */
public class NmsHandler_v1_8_R2 implements com.jcwhatever.phantom.nms.INmsHandler, INmsHandler {

    public NmsHandler_v1_8_R2() {}

    @Override
    public IBlockDigPacket getBlockDigPacket(PacketContainer packet) {
        return new BlockDigPacket_v1_8_R2(packet);
    }

    @Override
    public IBlockPlacePacket getBlockPlacePacket(PacketContainer packet) {
        return new BlockPlacePacket_v1_8_R2(packet);
    }

    @Override
    public IBlockChangeFactory getBlockChangeFactory(int x, int y, int z, Material material, byte meta) {
        return new BlockChangeFactory_v1_8_R2(x, y, z, material, meta);
    }

    @Override
    public BlockChangePacket_v1_8_R2 getBlockChangePacket(PacketContainer packet) {

        StructureModifier<Object> objects = packet.getModifier();

        BaseBlockPosition nmsBlockPosition = (BaseBlockPosition)objects.read(0);

        int x = nmsBlockPosition.getX();
        int y = nmsBlockPosition.getY();
        int z = nmsBlockPosition.getZ();

        return new BlockChangePacket_v1_8_R2(this, packet, x, y, z);
    }

    @Override
    public IMultiBlockChangeFactory getMultiBlockChangeFactory(World world, ICoords2Di coords, IPhantomChunk chunk) {
        return new MultiBlockChangeFactory_v1_8_R2(world, coords, chunk);
    }

    @Override
    public IMultiBlockChangePacket getMultiBlockChangePacket(PacketContainer packet) {

        StructureModifier<Object> objects = packet.getModifier();

        ChunkCoordIntPair nmsCoords = (ChunkCoordIntPair)objects.read(0);
        if (nmsCoords == null)
            throw new RuntimeException("Failed to ChunkCoordIntPair for MultiBlockChangePacket");

        int chunkX = nmsCoords.x;
        int chunkZ = nmsCoords.z;

        return new MultiBlockChangePacket_v1_8_R2(this, packet, chunkX, chunkZ);
    }

    @Override
    public ChunkBulkData getChunkBulkData(PacketContainer packet, World world) {

        StructureModifier<Object> objects = packet.getModifier();

        int[] chunkX = (int[])objects.read(0);
        int[] chunkZ = (int[])objects.read(1);

        ChunkMap[] nmsChunkMaps = (ChunkMap[])objects.read(2);

        int totalChunks = nmsChunkMaps.length;

        IChunkData[] chunkDataArray = new IChunkData[totalChunks];

        // iterate over chunk data and create ChunkData instance for each chunk
        for (int i=0; i < totalChunks; i++) {

            ChunkData chunkData = new ChunkData(world);
            chunkDataArray[i] = chunkData;

            int x = chunkX[i];
            int z = chunkZ[i];
            byte[] data = nmsChunkMaps[i].a;  // chunk data
            int mask = nmsChunkMaps[i].b;//  section mask

            chunkData.init(x, z, mask, data, true);
        }

        return new ChunkBulkData(world, chunkDataArray);
    }

    @Override
    public ChunkData getChunkData(PacketContainer packet, World world) {

        ChunkData chunkData = new ChunkData(world);

        StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
        StructureModifier<Object> objects = packet.getModifier();

        int chunkX = integers.read(0);
        int chunkZ = integers.read(1);

        ChunkMap nmsChunkMap = (ChunkMap)objects.read(2);

        byte[] data = nmsChunkMap.a; //  data array
        int mask = nmsChunkMap.b; // sectionMask

        Boolean isContinuous = packet.getBooleans().readSafely(0);

        chunkData.init(chunkX, chunkZ, mask, data, isContinuous != null && isContinuous);

        return chunkData;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
