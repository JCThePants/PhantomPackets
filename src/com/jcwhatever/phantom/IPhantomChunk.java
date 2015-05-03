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

import org.bukkit.World;

import javax.annotation.Nullable;

/**
 * Interface for a phantom chunk.
 */
public interface IPhantomChunk extends Iterable<IPhantomBlock> {

    /**
     * Get the owning block context.
     */
    IPhantomBlockContext getContext();

    /**
     * Get the world the chunk is in.
     */
    World getWorld();

    /**
     * Get the chunk X coordinates.
     */
    int getX();

    /**
     * Get the chunk Z coordinates.
     */
    int getZ();

    /**
     * Get the total number of phantom blocks within the chunk that have
     * been set.
     */
    int totalBlocks();

    /**
     * Get a phantom block from within the chunk.
     *
     * @param relativeX  The X coordinates relative to the chunk.
     * @param y          The Y coordinates.
     * @param relativeZ  The Z coordinates relative to the chunk.
     *
     * @return  The phantom block or null if it has not been set.
     */
    @Nullable
    IPhantomBlock getRelativeBlock(int relativeX, int y, int relativeZ);
}
