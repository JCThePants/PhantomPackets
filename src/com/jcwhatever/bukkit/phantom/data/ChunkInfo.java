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

import org.bukkit.Chunk;

/*
 * 
 */
public class ChunkInfo implements IChunkCoordinates {

    private final WorldInfo _world;
    private final int _x;
    private final int _z;

    public ChunkInfo (WorldInfo world, int x, int z) {
        _world = world;
        _x = x;
        _z = z;
    }

    public ChunkInfo (Chunk chunk) {
        _world = new WorldInfo(chunk.getWorld());
        _x = chunk.getX();
        _z = chunk.getZ();
    }

    @Override
    public WorldInfo getWorld() {
        return _world;
    }

    @Override
    public int getX() {
        return _x;
    }

    @Override
    public int getZ() {
        return _z;
    }

    @Override
    public int hashCode() {
        return _world.getName().hashCode() ^ _x ^ _z;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ChunkInfo) {
            ChunkInfo other = (ChunkInfo)obj;

            return other._world.getName().equals(_world.getName()) &&
                    other._x == _x && other._z == _z;
        }

        return false;
    }

}
