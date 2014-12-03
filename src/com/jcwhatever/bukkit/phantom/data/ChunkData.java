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

import org.bukkit.World.Environment;

import net.minecraft.server.v1_8_R1.ChunkMap;

/*
 * 
 */
public class ChunkData implements IChunkData {

    public static final int BLOCK_SIZE = 2;
    public static final int BLOCK_DATA_SIZE = 8192;
    public static final int EMITTED_LIGHT_DATA_SIZE = 2048;
    public static final int SKYLIGHT_DATA_SIZE = 2048;
    public static final int BIOME_DATA_SIZE = 256;

    public static ChunkData fromMapChunkPacket(PacketContainer packet, WorldInfo world) {
        ChunkData data = new ChunkData(world);
        data.initMapChunkPacket(packet);
        return data;
    }

    private final WorldInfo _world;

    private int _chunkX;
    private int _chunkZ;
    private int _mask;
    private byte[] _data;

    private int _continuous;
    private int _startIndex;
    private int _sectionDataCount;

    private int _skylight;
    private int _chunkSize;
    private int _blockSize;

    private int _blockLightStart;
    private int _skylightStart;

    private int[] _sectionChunkIndexes = new int[16];

    ChunkData(WorldInfo world) {
        _world = world;
    }

    @Override
    public WorldInfo getWorld() {
        return _world;
    }

    @Override
    public int getX() {
        return _chunkX;
    }

    @Override
    public int getZ() {
        return _chunkZ;
    }

    @Override
    public int getSectionMask() {
        return _mask;
    }

    @Override
    public byte[] getData() {
        return _data;
    }

    @Override
    public boolean isContinuous() {
        return _continuous != 0;
    }

    @Override
    public int getStartIndex() {
        return _startIndex;
    }

    @Override
    public boolean hasSkylight() {
        return _skylight != 0;
    }

    @Override
    public int getChunkSize() {
        return _chunkSize;
    }

    @Override
    public int getBlockSize() {
        return _blockSize;
    }

    @Override
    public int getSectionDataCount() {
        return _sectionDataCount;
    }

    @Override
    public int getSectionChunkIndex(int sectionDataIndex) {
        // convert section data index to section chunk index
        return _sectionChunkIndexes[sectionDataIndex];
    }

    @Override
    public int getBlockStart(int sectionDataIndex) {
        return getStartIndex() + (sectionDataIndex * BLOCK_DATA_SIZE);
    }

    @Override
    public int getBlockLightStart(int sectionDataIndex) {
        return _blockLightStart + (sectionDataIndex * EMITTED_LIGHT_DATA_SIZE);
    }

    @Override
    public int getSkylightStart(int sectionDataIndex) {
        return _skylightStart + (sectionDataIndex * SKYLIGHT_DATA_SIZE);
    }

    @Override
    public boolean hasChunkSection(int sectionChunkIndex) {
        return (_mask & (1 << sectionChunkIndex)) > 0;
    }

    void setStartIndex(int index) {
        _startIndex = index;
    }

    void init(int x, int z, int mask, byte[] data, boolean isContinuous) {

        _chunkX = x;
        _chunkZ = z;
        _mask = mask;
        _data = data;
        _skylight = _world.getEnvironment() == Environment.NORMAL ? 1 : 0;

        _continuous = isContinuous
                ? BIOME_DATA_SIZE
                : 0;

        for (int i = 0; i < 16; i++) {

            if (hasChunkSection(i)) {

                // record section chunk position
                _sectionChunkIndexes[_sectionDataCount] = i;

                // increment total sections in data
                _sectionDataCount++;
            }
        }

        _chunkSize = (_sectionDataCount * BLOCK_DATA_SIZE * EMITTED_LIGHT_DATA_SIZE *
                (hasSkylight() ? SKYLIGHT_DATA_SIZE : 1)) + _continuous;

        _blockSize = BLOCK_DATA_SIZE * _sectionDataCount;

        _blockLightStart = _startIndex + _blockSize;

        _skylightStart = hasSkylight() ? -1 : _startIndex + _blockSize +
                        (_sectionDataCount * EMITTED_LIGHT_DATA_SIZE);
    }

    private void initMapChunkPacket(PacketContainer packet) {

        StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
        StructureModifier<Object> objects = packet.getModifier();

        //TODO: Current use of NMS code will break with version changes

        ChunkMap nmsChunkMap = (ChunkMap)objects.read(2);

        int chunkX = integers.read(0);
        int chunkZ = integers.read(1);
        int mask = nmsChunkMap.b; // sectionMask
        byte[] data = nmsChunkMap.a; // data array

        Boolean isContinuous = packet.getBooleans().readSafely(0);

        init(chunkX, chunkZ, mask, data, isContinuous != null && isContinuous);
    }
}
