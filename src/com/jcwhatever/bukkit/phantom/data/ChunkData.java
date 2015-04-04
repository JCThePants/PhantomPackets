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

import com.jcwhatever.nucleus.utils.coords.ChunkCoords;
import com.jcwhatever.nucleus.utils.coords.WorldInfo;
import com.jcwhatever.bukkit.phantom.Utils;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;

/*
 * 
 */
public class ChunkData implements IChunkData {

    public static final int BLOCK_SIZE = 2;
    public static final int BLOCK_DATA_SIZE = 8192;
    public static final int EMITTED_LIGHT_DATA_SIZE = 2048;
    public static final int SKYLIGHT_DATA_SIZE = 2048;
    public static final int BIOME_DATA_SIZE = 256;

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
    private int[] _sectionDataIndexes = new int[16];

    public ChunkData(WorldInfo world) {
        _world = world;
    }

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
    public String getWorldName() {
        return _world.getName();
    }

    @Override
    public Chunk getChunk() {
        World world = _world.getBukkitWorld();
        if (world == null)
            return null;

        return world.getChunkAt(_chunkX, _chunkZ);
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

    @Override
    public boolean hasBlock(int relativeX, int y, int relativeZ) {

        int sectionChunkIndex = (int)Math.floor((double)y / 16);

        return hasChunkSection(sectionChunkIndex);
    }

    @Override
    public void setBlock(int relativeX, int y, int relativeZ, Material material, byte meta) {

        int sectionChunkIndex = (int)Math.floor((double)y / 16);

        if (!hasChunkSection(sectionChunkIndex))
            return;

        int sectionDataIndex = _sectionDataIndexes[sectionChunkIndex];

        y = y % 16;

        int index = 512 * y + 32 * relativeZ + 2 * relativeX;// ((y * 16/*x*/ * 16/*y*/) + (relativeZ * 16) + relativeX) * 2;

        index += getBlockStart(sectionDataIndex);

        int id = Utils.getLegacyId(material, meta);

        _data[index] = (byte) (id & 0xFF);
        _data[index + 1] = (byte) (id >> 8 & 0xFF);
    }

    @Override
    public int hashCode() {
        return _world.hashCode() ^ getX() ^ getZ();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ChunkCoords) {
            ChunkCoords other = (ChunkCoords)obj;

            return other.getWorldName().equals(_world.getName()) &&
                    other.getX() == getX() && other.getZ() == getZ();
        }

        return false;
    }

    public void setStartIndex(int index) {
        _startIndex = index;
    }

    public void init(int x, int z, int mask, byte[] data, boolean isContinuous) {

        _chunkX = x;
        _chunkZ = z;
        _mask = mask;
        _data = data;
        _skylight = _world.getEnvironment() == Environment.NORMAL ? 1 : 0;

        _continuous = isContinuous
                ? BIOME_DATA_SIZE
                : 0;

        for (int sectionChunkIndex = 0; sectionChunkIndex < 16; sectionChunkIndex++) {

            if (hasChunkSection(sectionChunkIndex)) {

                // record section chunk position
                _sectionChunkIndexes[_sectionDataCount] = sectionChunkIndex;

                _sectionDataIndexes[sectionChunkIndex] = _sectionDataCount;

                // increment total sections in data
                _sectionDataCount++;

            }
            else {
                _sectionDataIndexes[sectionChunkIndex] = -1;
            }


        }

        _chunkSize = (_sectionDataCount * BLOCK_DATA_SIZE * EMITTED_LIGHT_DATA_SIZE *
                (hasSkylight() ? SKYLIGHT_DATA_SIZE : 1)) + _continuous;

        _blockSize = BLOCK_DATA_SIZE * _sectionDataCount;

        _blockLightStart = _startIndex + _blockSize;

        _skylightStart = hasSkylight() ? -1 : _startIndex + _blockSize +
                        (_sectionDataCount * EMITTED_LIGHT_DATA_SIZE);
    }

}
