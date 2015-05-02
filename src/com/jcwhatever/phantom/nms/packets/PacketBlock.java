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

package com.jcwhatever.phantom.nms.packets;

import org.bukkit.Material;

/**
 * Information about a block.
 */
public class PacketBlock {

    private int _x;
    private int _y;
    private int _z;
    private Material _material;
    private byte _data;

    /**
     * Constructor.
     *
     * @param x         The block X coordinates.
     * @param y         The block Y coordinates.
     * @param z         The block Z coordinates.
     * @param material  The material.
     * @param data      The data.
     */
    public PacketBlock(int x, int y, int z, Material material, byte data) {
        _x = x;
        _y = y;
        _z = z;
        _material = material;
        _data = data;
    }

    public int getX() {
        return _x;
    }

    public int getY() {
        return _y;
    }

    public int getZ() {
        return _z;
    }

    public Material getMaterial() {
        return _material;
    }

    public byte getData() {
        return _data;
    }

    public void setBlock(Material material, int meta) {
        _material = material;
        _data = (byte)meta;
    }
}
