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

package com.jcwhatever.phantom.nms.v1_8_R3.factory;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.phantom.Utils;
import com.jcwhatever.phantom.packets.factory.IBlockChangeFactory;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;


public class BlockChangeFactory_v1_8_R3 implements IBlockChangeFactory {

    private final int _x;
    private final int _y;
    private final int _z;
    private final Material _material;
    private final byte _meta;

    public BlockChangeFactory_v1_8_R3(int x, int y, int z, Material material, byte meta) {
        PreCon.notNull(material);

        _x = x;
        _y = y;
        _z = z;
        _material = material;
        _meta = meta;
    }

    @Override
    public PacketContainer createPacket(World world) {
        PreCon.notNull(world);

        net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld)world).getHandle();

        BlockPosition position = new BlockPosition(_x, _y, _z);

        int id = Utils.getCombinedId(_material, _meta);
        IBlockData blockData = Block.getByCombinedId(id);
        if (blockData == null)
            throw new IllegalArgumentException("Failed to create block data.");

        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(nmsWorld, position);
        packet.block = blockData;

        return PacketContainer.fromPacket(packet);
    }
}
