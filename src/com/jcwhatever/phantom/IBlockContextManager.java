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

import com.jcwhatever.nucleus.utils.managers.INamedManager;

import org.bukkit.World;

import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Manager for block contexts.
 */
public interface IBlockContextManager extends INamedManager<IPhantomBlockContext> {

    /**
     * Determine if a world contains phantom block contexts.
     *
     * @param world  The world to check.
     */
    boolean hasPhantomBlocksInWorld(World world);

    /**
     * Get phantom block at a location.
     *
     * @param world  The world to check in.
     * @param x      The X coordinates of the block.
     * @param y      The Y coordinates of the block.
     * @param z      The Z coordinates of the block.
     *
     * @return  The phantom block or null if there is no phantom block.
     */
    @Nullable
    IPhantomBlock getBlockAt(World world, int x, int y, int z);

    /**
     * Get all phantom block contexts that have blocks inside the specified chunk.
     *
     * @param world   The world.
     * @param chunkX  The chunk X coordinates.
     * @param chunkZ  The chunk Z coordinates.
     */
    Collection<IPhantomBlockContext> getChunkContexts(World world, int chunkX, int chunkZ);
}
