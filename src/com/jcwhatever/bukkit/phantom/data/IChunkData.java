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

/**
 * Represents a chunk data array and pre-calculated values.
 */
public interface IChunkData extends IChunkCoordinates {

    /**
     * Get the bit mask that contains information about which chunk
     * sections are included in the data.
     */
    int getSectionMask();

    /**
     * Get the byte array that contains the chunk data.
     */
    byte[] getData();

    /**
     * Determine if chunk data contains all chunk sections.
     */
    boolean isContinuous();

    /**
     * Get the index position where the chunk data begins.
     */
    int getStartIndex();

    /**
     * Determine if the chunk data contains skylight data.
     */
    boolean hasSkylight();

    /**
     * Get the total size of the chunk data.
     */
    int getChunkSize();

    /**
     * Get the total size of the block data.
     */
    int getBlockSize();

    /**
     * Get the total number of chunk sections included
     * in the data.
     *
     * <p>There can only be up to 16 sections since a chunk is composed
     * of 16 sections. (0-15)</p>
     */
    int getSectionDataCount();

    /**
     * Get the index position of a chunk section relative to its chunk
     * based on the section index position in the available data.
     *
     * @param sectionDataIndex  The chunk section index position within the data.
     */
    int getSectionChunkIndex(int sectionDataIndex);

    /**
     * Get the starting index position of block data for the specified
     * chunk section using the chunk sections index position within the data.
     *
     * @param sectionDataIndex  The chunk section index position within the data.
     */
    int getBlockStart(int sectionDataIndex);

    /**
     * Get the starting index position of block meta data for the specified
     * chunk section using the chunk sections index position within the data.
     *
     * @param sectionDataIndex  The chunk section index position within the data.
     */
    int getBlockMetaStart(int sectionDataIndex);

    /**
     * Get the starting index position of block light data for the specified
     * chunk section using the chunk sections index position within the data.
     *
     * @param sectionDataIndex  The chunk section index position within the data.
     */
    int getBlockLightStart(int sectionDataIndex);

    /**
     * Get the starting index position of block skylight data for the specified
     * chunk section using the chunk sections index position within the data.
     *
     * <p>Should call {@code hasSkylight} first to determine if there is
     * skylight data available.</p>
     *
     * @param sectionDataIndex  The chunk section index position within the data.
     */
    int getSkylightStart(int sectionDataIndex);

    /**
     * Determine if the chunk data contains data for the specified chunk section.
     *
     * @param sectionChunkIndex  The index position of the chunk section within the chunk.
     */
    boolean hasChunkSection(int sectionChunkIndex);
}
