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

package com.jcwhatever.phantom.nms.v1_8_R1.packets;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.phantom.nms.packets.IBlockDigPacket;

import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.EnumDirection;
import net.minecraft.server.v1_8_R1.EnumPlayerDigType;

/*
 * 
 */
public class BlockDigPacket_v1_8_R1 implements IBlockDigPacket {

    private final StructureModifier<Object> _objects;

    private int _x;
    private int _y;
    private int _z;

    private Direction _direction;
    private DigType _digType;


    public BlockDigPacket_v1_8_R1(PacketContainer packet) {
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
    public Direction getDirection() {
        if (_direction == null) {
            EnumDirection direction = (EnumDirection)_objects.read(1);

            switch (direction) {
                case UP:
                    _direction = Direction.UP;
                    break;
                case DOWN:
                    _direction = Direction.DOWN;
                    break;
                case NORTH:
                    _direction = Direction.NORTH;
                    break;
                case SOUTH:
                    _direction = Direction.SOUTH;
                    break;
                case WEST:
                    _direction = Direction.WEST;
                    break;
                case EAST:
                    _direction = Direction.EAST;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        return _direction;
    }

    @Override
    public DigType getDigType() {

        if (_digType == null) {
            EnumPlayerDigType digType = (EnumPlayerDigType) _objects.read(2);
            switch (digType) {
                case START_DESTROY_BLOCK:
                    _digType = DigType.START_DESTROY_BLOCK;
                    break;
                case ABORT_DESTROY_BLOCK:
                    _digType = DigType.ABORT_DESTROY_BLOCK;
                    break;
                case STOP_DESTROY_BLOCK:
                    _digType = DigType.STOP_DESTROY_BLOCK;
                    break;
                case DROP_ALL_ITEMS:
                    _digType = DigType.DROP_ALL_ITEMS;
                    break;
                case DROP_ITEM:
                    _digType = DigType.DROP_ITEM;
                    break;
                case RELEASE_USE_ITEM:
                    _digType = DigType.RELEASE_USE_ITEM;
                    break;
                default:
                    throw new AssertionError();
            }
        }

        return _digType;
    }
}
