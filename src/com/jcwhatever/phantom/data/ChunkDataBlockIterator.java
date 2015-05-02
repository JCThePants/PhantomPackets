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

package com.jcwhatever.phantom.data;

import com.jcwhatever.phantom.Utils;

import org.bukkit.Material;

/*
 * Iterate over block data contained in chunk byte data.
 */
public class ChunkDataBlockIterator {

    private IChunkData _chunkData;
    private byte[] _data;

    private int _blockIndex;
    private int _blockDataIndex;
    private int _lightIndex;
    private int _skylightIndex = -1;

    private int _sectionDataIndex = 0;
    private int _sectionChunkIndex = 0;
    private int _sectionX;
    private int _sectionY;
    private int _sectionZ;

    private int _xStart;
    private int _zStart;

    /**
     * Constructor.
     *
     * @param data The chunk packet data to iterate over.
     */
    public ChunkDataBlockIterator(IChunkData data) {
        _chunkData = data;
        _data = data.getData();

        _blockIndex = data.getBlockStart(0);
        _blockDataIndex = _blockIndex;
        _lightIndex = data.getBlockLightStart(0);

        _xStart = data.getX() * 16;
        _zStart = data.getZ() * 16;
    }

    /**
     * Determine if skylight data is present.
     */
    public boolean hasSkylight() {
        return _chunkData.hasSkylight();
    }

    /**
     * Get the index of the current chunk section relative to the available section data.
     */
    public int getSectionDataIndex() {
        return _sectionDataIndex;
    }

    /**
     * Get the index of the current chunk section relative to the chunk.
     */
    public int getSectionChunkIndex() {
        return _sectionChunkIndex;
    }

    /**
     * Determine if there is more block data to read.
     */
    public boolean hasNext() {
        return !(_sectionX == 15 && _sectionY == 15 && _sectionZ == 15 &&
                _sectionDataIndex == _chunkData.getSectionDataCount()) &&
                _chunkData.getSectionDataCount() > 0;
    }

    /**
     * Move to next block.
     */
    public void next() {

        _sectionX++;
        if (_sectionX == 16) {
            _sectionX = 0;
            _sectionZ++;
        }

        if (_sectionZ == 16) {
            _sectionZ = 0;
            _sectionY++;
        }

        if (_sectionY == 16) {
            _sectionY = 0;
            _sectionDataIndex++;
            _sectionChunkIndex = _chunkData.getSectionChunkIndex(_sectionDataIndex);

            _blockIndex = _chunkData.getBlockStart(_sectionDataIndex) - 1;
            _blockDataIndex = _blockIndex - (ChunkData.BLOCK_SIZE - 1);
            _lightIndex = _chunkData.getBlockLightStart(_sectionDataIndex) - 1;
            _skylightIndex = _chunkData.getSkylightStart(_sectionDataIndex) - 1;
        }

        _blockIndex++;
        _blockDataIndex += ChunkData.BLOCK_SIZE;

        if ((_blockIndex & 0x1) == 0) { // MODULUS
            _lightIndex++;
            _skylightIndex++;
        }
    }

    /**
     * Get the current block X coordinates relative to the chunk section.
     */
    public int getRelativeX() {
        return _sectionX;
    }

    /**
     * Get the current block Y coordinates relative to the chunk section.
     */
    public int getRelativeY() {
        return _sectionY;
    }

    /**
     * Get the current block Z coordinates relative to the chunk section.
     */
    public int getRelativeZ() {
        return _sectionZ;
    }

    /**
     * Get the current block X coordinates.
     */
    public int getX() {
        return _sectionX + _xStart;
    }

    /**
     * Get the current block Z coordinates.
     */
    public int getZ() {
        return _sectionZ + _zStart;
    }

    /**
     * Get the current block Y coordinates.
     */
    public int getY() {
        return _sectionY + (_sectionChunkIndex * 16);
    }

    /**
     * Get the current block legacy ID.
     *
     * <p>
     *     blockId << 4 | meta
     * </p>
     *
     */
    public int getBlockId() {
         return (_data[_blockDataIndex] & 0xFF) | ((_data[_blockDataIndex + 1] << 8) & 0xFF00);
    }

    /**
     * Set the current block legacy ID.
     *
     * <p>
     *     blockId << 4 | meta
     * </p>
     *
     * @param id  The ID value.
     */
    public void setBlockId(int id) {
        _data[_blockDataIndex] = (byte) (id & 0xFF);
        _data[_blockDataIndex + 1] = (byte) (id >> 8 & 0xFF);
    }

    /**
     * Get the current block material.
     */
    public Material getBlockMaterial() {
        int id = getBlockId();
        return Utils.getMaterialFromLegacyId(id);
    }

    /**
     * Set the current block material.
     *
     * @param material
     */
    public void setBlockMaterial(Material material) {
        byte meta = getBlockMeta();
        int legacyId = Utils.getLegacyId(material, meta);

        setBlockId(legacyId);
    }

    /**
     * Get the current block meta data value.
     */
    public byte getBlockMeta() {
        return Utils.getDataFromLegacyId(getBlockId());
    }

    /**
     * Set current block meta data value. Block meta data value is a 4 bit number.
     *
     * @param meta  A value from 0-15.
     */
    public void setBlockMeta(int meta) {
        Material material = getBlockMaterial();
        int combinedId = Utils.getLegacyId(material, (byte) meta);
        setBlockId(combinedId);
    }

    /**
     * Get the current blocks light value.
     */
    public int getBlockLight() {
        int light = _data[_lightIndex];

        return (_blockIndex & 0x1) == 0 // MODULUS
                ? light & 0x0F
                : (light & 0xF0) >> 4;
    }

    /**
     * Set current block light value. Block light value is a 4 bit number.
     *
     * @param light  A value from 0-15.
     */
    public void setBlockLight(int light) {
        int current = _data[_lightIndex];

        if ((_blockIndex & 0x1) == 0) { // MODULUS
            current &= 0xF0;
        } else {
            current &= 0x0F;
            light <<= 4;
        }

        _data[_lightIndex] = (byte)(current | light);
    }

    /**
     * Get the current blocks skylight value.
     */
    public int getSkylight() {
        if (!_chunkData.hasSkylight())
            throw new IllegalStateException("Cannot get skylight data because it's not present in the data.");

        int light = _data[_skylightIndex];

        return (_blockIndex & 0x1) == 0 // MODULUS
                ? light & 0x0F
                : (light & 0xF0) >> 4;
    }

    /**
     * Set current skylight value. Skylight value is a 4 bit number.
     *
     * @param light  A value from 0-15.
     */
    public void setSkylight(int light) {
        if (!_chunkData.hasSkylight())
            throw new IllegalStateException("Cannot set skylight data because it's not present in the data.");

        int current = _data[_skylightIndex];

        if ((_blockIndex & 0x1) == 0) { // MODULUS
            current &= 0xF0;
        } else {
            current &= 0x0F;
            light <<= 4;
        }

        _data[_skylightIndex] = (byte)(current | light);
    }

}
