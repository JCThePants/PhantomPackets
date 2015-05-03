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

package com.jcwhatever.phantom;

import org.bukkit.Material;

/**
 * Interface for a phantom block.
 */
public interface IPhantomBlock extends IViewable {

    /**
     * Get the blocks owning context.
     */
    IPhantomBlockContext getContext();

    /**
     * Get the blocks owning phantom chunk.
     */
    IPhantomChunk getChunk();

    /**
     * Get the blocks world X coordinates.
     */
    int getX();

    /**
     * Get the blocks world Y coordinates.
     */
    int getY();

    /**
     * Get the blocks world Z coordinates.
     */
    int getZ();

    /**
     * Get the blocks X coordinates relative to the chunk it is in.
     */
    int getRelativeX();

    /**
     * Get the blocks Z coordinates relative to the chunk it is in.
     */
    int getRelativeZ();

    /**
     * Get the block material.
     */
    Material getMaterial();

    /**
     * Get the block data.
     */
    byte getData();

    /**
     * Set the block material and data.
     *
     * @param material  The block material.
     * @param data      The block data.
     *
     * @return  True if set, otherwise false.
     */
    boolean set(Material material, int data);
}
