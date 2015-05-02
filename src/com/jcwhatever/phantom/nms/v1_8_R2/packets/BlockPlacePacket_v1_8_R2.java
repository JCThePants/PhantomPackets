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

package com.jcwhatever.phantom.nms.v1_8_R2.packets;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.phantom.nms.packets.IBlockPlacePacket;

import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_8_R2.BlockPosition;

/*
 * 
 */
public class BlockPlacePacket_v1_8_R2 implements IBlockPlacePacket {

    private final StructureModifier<Object> _objects;

    private final int _x;
    private final int _y;
    private final int _z;

    private ItemStack _itemStack;
    private long _timeStamp = -1;

    public BlockPlacePacket_v1_8_R2(PacketContainer packet) {

        _objects = packet.getModifier();

        BlockPosition blockPosition = (BlockPosition)_objects.read(0);

        _x = blockPosition.getX();
        _y = blockPosition.getY();
        _z = blockPosition.getZ();
    }

    @Override
    public int getX() {
        return _x;
    }

    @Override
    public int getY() {
        return _y;
    }

    @Override
    public int getZ() {
        return _z;
    }

    @Override
    public ItemStack getItemStack() {
        if (_itemStack == null) {
            net.minecraft.server.v1_8_R2.ItemStack itemStack =
                    (net.minecraft.server.v1_8_R2.ItemStack)_objects.read(2);
            _itemStack = CraftItemStack.asCraftMirror(itemStack);
        }
        return _itemStack;
    }

    @Override
    public long timeStamp() {
        if (_timeStamp == -1) {
            _timeStamp = (long)_objects.read(6);
        }
        return _timeStamp;
    }
}
