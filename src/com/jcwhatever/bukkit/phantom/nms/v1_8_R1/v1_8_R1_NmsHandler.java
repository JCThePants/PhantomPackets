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

package com.jcwhatever.bukkit.phantom.nms.v1_8_R1;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.generic.regions.data.ChunkBlockInfo;
import com.jcwhatever.bukkit.generic.regions.data.ChunkInfo;
import com.jcwhatever.bukkit.generic.regions.data.WorldInfo;
import com.jcwhatever.bukkit.generic.reflection.Fields;
import com.jcwhatever.bukkit.generic.reflection.INonApiHandler;
import com.jcwhatever.bukkit.generic.reflection.ReflectedArray;
import com.jcwhatever.bukkit.generic.reflection.ReflectedInstance;
import com.jcwhatever.bukkit.generic.reflection.ReflectedType;
import com.jcwhatever.bukkit.generic.reflection.ReflectionManager;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.data.ChunkBulkData;
import com.jcwhatever.bukkit.phantom.data.ChunkData;
import com.jcwhatever.bukkit.phantom.data.IChunkData;
import com.jcwhatever.bukkit.phantom.nms.INmsHandler;
import com.jcwhatever.bukkit.phantom.nms.NmsTypes;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangeFactory;
import com.jcwhatever.bukkit.phantom.packets.IBlockDigPacket;
import com.jcwhatever.bukkit.phantom.packets.IBlockPlacePacket;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangeFactory;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangePacket;

import org.bukkit.Material;

import java.util.List;

/*
 * 
 */
public class v1_8_R1_NmsHandler implements INmsHandler, INonApiHandler {

    private final ReflectedType<?> BLOCK;

    private final ReflectedType<?> IBLOCK_DATA;

    private final ReflectedType<?> BASE_BLOCK_POSITION;

    private final ReflectedType<?> MULTI_BLOCK_CHANGE_INFO;

    private final ReflectedType<?> PACKET_PLAY_OUT_MULTIBLOCK_CHANGE;

    private final ReflectedType<?> CHUNK_COORD_INT_PAIR;

    private final ReflectedType<?> CHUNK_MAP;

    public v1_8_R1_NmsHandler() {
        ReflectionManager manager = PhantomPackets.getPlugin().getReflectionManager();

        BLOCK = manager.typeFrom("net.minecraft.server.Block");
        IBLOCK_DATA = manager.typeFrom("net.minecraft.server.IBlockData");
        BASE_BLOCK_POSITION = manager.typeFrom("net.minecraft.server.BaseBlockPosition");
        MULTI_BLOCK_CHANGE_INFO = manager.typeFrom("net.minecraft.server.MultiBlockChangeInfo");
        PACKET_PLAY_OUT_MULTIBLOCK_CHANGE = manager.typeFrom("net.minecraft.server.PacketPlayOutMultiBlockChange");
        CHUNK_COORD_INT_PAIR = manager.typeFrom("net.minecraft.server.ChunkCoordIntPair");
        CHUNK_MAP = manager.typeFrom("net.minecraft.server.ChunkMap");
    }

    @Override
    public IBlockDigPacket getBlockDigPacket(PacketContainer packet) {
        return new BlockDigPacket(packet);
    }

    @Override
    public IBlockPlacePacket getBlockPlacePacket(PacketContainer packet) {
        return new BlockPlacePacket(packet);
    }

    @Override
    public IBlockChangeFactory getBlockChangeFactory(int x, int y, int z, Material material, byte meta) {
        return new BlockChangeFactory(x, y, z, material, meta);
    }

    @Override
    public BlockChangePacket getBlockChangePacket(PacketContainer packet) {

        StructureModifier<Object> objects = packet.getModifier();

        ReflectedInstance<?> nmsBlockPosition = BASE_BLOCK_POSITION.reflect(objects.read(0));
        Fields intFields = nmsBlockPosition.getFields(int.class);

        int x = intFields.get(0);
        int y = intFields.get(1);
        int z = intFields.get(2);

        return new BlockChangePacket(this, packet, x, y, z);
    }

    @Override
    public IMultiBlockChangeFactory getMultiBlockChangeFactory(ChunkInfo chunkInfo, List<ChunkBlockInfo> blocks) {
        return new MultiBlockChangeFactory(this, chunkInfo, blocks);
    }

    @Override
    public IMultiBlockChangePacket getMultiBlockChangePacket(PacketContainer packet) {

        StructureModifier<Object> objects = packet.getModifier();

        ReflectedInstance<?> nmsCoords = getReflectedType(NmsTypes.CHUNK_COORD_INT_PAIR).reflect(objects.read(0));
        if (nmsCoords == null)
            throw new RuntimeException("Failed to ChunkCoordIntPair for MultiBlockChangePacket");

        Fields fields = nmsCoords.getFields(int.class);

        int chunkX = fields.get(0);
        int chunkZ = fields.get(1);

        return new MultiBlockChangePacket(this, packet, chunkX, chunkZ);
    }

    @Override
    public ChunkBulkData getChunkBulkData(PacketContainer packet, WorldInfo world) {

        StructureModifier<Object> objects = packet.getModifier();

        int[] chunkX = (int[])objects.read(0);
        int[] chunkZ = (int[])objects.read(1);

        ReflectedArray<?> nmsChunkMaps = reflectArray(NmsTypes.CHUNK_MAP, objects.read(2));

        int totalChunks = nmsChunkMaps.length();

        IChunkData[] chunkDataArray = new IChunkData[totalChunks];

        // iterate over chunk data and create ChunkData instance for each chunk
        for (int i=0; i < totalChunks; i++) {

            ChunkData chunkData = new ChunkData(world);
            chunkDataArray[i] = chunkData;

            int x = chunkX[i];
            int z = chunkZ[i];
            byte[] data = nmsChunkMaps.getReflected(i).getFields().get(0);//.a  chunk data
            int mask = nmsChunkMaps.getReflected(i).getFields().get(1); //.b  section mask

            chunkData.init(x, z, mask, data, true);
        }

        return new ChunkBulkData(world, chunkDataArray);
    }

    @Override
    public ChunkData getChunkData(PacketContainer packet, WorldInfo world) {

        ChunkData chunkData = new ChunkData(world);

        StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
        StructureModifier<Object> objects = packet.getModifier();

        int chunkX = integers.read(0);
        int chunkZ = integers.read(1);

        ReflectedInstance<?> nmsChunkMap = reflect(NmsTypes.CHUNK_MAP, objects.read(2));
        Fields fields = nmsChunkMap.getFields();
        byte[] data = fields.get(0);//.a  data array
        int mask = fields.get(1); //.b sectionMask

        Boolean isContinuous = packet.getBooleans().readSafely(0);

        chunkData.init(chunkX, chunkZ, mask, data, isContinuous != null && isContinuous);

        return chunkData;
    }

    @Override
    public int getBlockCombinedId(Object iNmsBlockData) {
        return BLOCK.call("getCombinedId", iNmsBlockData);
    }

    @Override
    public Object getBlockByCombinedId(int id) {
        return BLOCK.call("getByCombinedId", id);
    }

    @Override
    public ReflectedInstance<?> reflect(NmsTypes type, Object instance) {
        return getReflectedType(type).reflect(instance);
    }

    @Override
    public ReflectedArray<?> reflectArray(NmsTypes type, Object instance) {
        return getReflectedType(type).reflectArray(instance);
    }

    @Override
    public ReflectedType<?> getReflectedType(NmsTypes type) {
        switch (type) {
            case IBLOCK_DATA:
                return IBLOCK_DATA;

            case BASE_BLOCK_POSITION:
                return BASE_BLOCK_POSITION;

            case MULTI_BLOCK_CHANGE_INFO:
                return MULTI_BLOCK_CHANGE_INFO;

            case PACKET_PLAY_OUT_MULTIBLOCK_CHANGE:
                return PACKET_PLAY_OUT_MULTIBLOCK_CHANGE;

            case CHUNK_COORD_INT_PAIR:
                return CHUNK_COORD_INT_PAIR;

            case CHUNK_MAP:
                return CHUNK_MAP;

            default:
                throw new AssertionError();
        }
    }

}
