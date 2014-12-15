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

package com.jcwhatever.bukkit.phantom.nms.v1_8_R1;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.generic.utils.PreCon;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangeFactory;

import org.bukkit.Material;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.IBlockData;


public class BlockChangeFactory_v1_8_R1 implements IBlockChangeFactory {

    private final int _x;
    private final int _y;
    private final int _z;
    private final Material _material;
    private final byte _meta;

    public BlockChangeFactory_v1_8_R1(int x, int y, int z, Material material, byte meta) {
        PreCon.notNull(material);

        _x = x;
        _y = y;
        _z = z;
        _material = material;
        _meta = meta;
    }

    @Override
    public PacketContainer createPacket() {

        PacketContainer packet = new PacketContainer(Server.BLOCK_CHANGE);
        packet.getModifier().writeDefaults();

        BlockPosition position = new BlockPosition(_x, _y, _z);

        StructureModifier<Object> objects = packet.getModifier();
        objects.write(0, position);

        int id = Utils.getCombinedId(_material, _meta);
        IBlockData blockData = Block.getByCombinedId(id);
        if (blockData == null)
            throw new IllegalArgumentException("Failed to create block data.");

        objects.write(1, blockData);

        return packet;
    }
}
