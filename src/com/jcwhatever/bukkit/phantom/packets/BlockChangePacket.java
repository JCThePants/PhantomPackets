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

package com.jcwhatever.bukkit.phantom.packets;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.phantom.Utils;

import org.bukkit.Material;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.IBlockData;

/*
 * 
 */
public class BlockChangePacket extends AbstractPacket {

    private final BlockPosition _nmsBlockPosition;
    private IBlockData _nmsBlockData;

    public BlockChangePacket(PacketContainer packet) {
        super(packet);

        StructureModifier<Object> objects = packet.getModifier();

        _nmsBlockPosition = (BlockPosition)objects.read(0);
        _nmsBlockData = (IBlockData)objects.read(1);
    }

    @Override
    public void saveChanges() {

        StructureModifier<Object> objects = _packet.getModifier();
        objects.write(1, _nmsBlockData);
    }

    public int getX() {
        return _nmsBlockPosition.getX();
    }

    public int getY() {
        return _nmsBlockPosition.getY();
    }

    public int getZ() {
        return _nmsBlockPosition.getZ();
    }

    public Material getMaterial() {
        int id = Block.getCombinedId(_nmsBlockData);

        return Utils.getMaterialFromCombinedId(id);
    }

    public byte getMeta() {
        int id = Block.getCombinedId(_nmsBlockData);

        return Utils.getMetaFromCombinedId(id);
    }

    public void setBlock(Material material, byte meta) {
        int id = Utils.getCombinedId(material, meta);

        IBlockData data = Block.getByCombinedId(id);
        if (data == null)
            throw new IllegalArgumentException("Failed to create block data.");

        _nmsBlockData = data;
    }

    @Override
    public BlockChangePacket clonePacket() {
        PacketContainer clone = Utils.clonePacket(_packet);
        PacketContainer source = _packet;

        StructureModifier<Object> cloneObj = clone.getModifier();
        StructureModifier<Object> sourceObj = source.getModifier();

        IBlockData nmsBlockData = (IBlockData)sourceObj.read(1);

        int id = Block.getCombinedId(nmsBlockData);

        IBlockData nmsCloneData = Block.getByCombinedId(id);

        cloneObj.write(1, nmsCloneData);

        return new BlockChangePacket(clone);
    }
}
