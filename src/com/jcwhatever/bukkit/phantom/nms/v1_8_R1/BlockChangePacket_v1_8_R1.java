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

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.nms.INmsHandler;
import com.jcwhatever.bukkit.phantom.packets.AbstractPacket;
import com.jcwhatever.bukkit.phantom.packets.IBlockChangePacket;

import org.bukkit.Material;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.IBlockData;


/*
 * 
 */
public class BlockChangePacket_v1_8_R1 extends AbstractPacket implements IBlockChangePacket {

    private final INmsHandler _nms;
    private final StructureModifier<Object> _objects;
    private final int _x;
    private final int _y;
    private final int _z;
    private IBlockData _nmsBlockData;



    public BlockChangePacket_v1_8_R1(INmsHandler handler, PacketContainer packet, int x, int y, int z) {
        super(packet);

        _nms = handler;

         _objects = packet.getModifier();

        _x = x;
        _y = y;
        _z = z;
    }

    @Override
    public void saveChanges() {

        if (_nmsBlockData == null)
            return;

        _objects.write(1, _nmsBlockData);
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
    public Material getMaterial() {
        int id = Block.getCombinedId(getNmsBlockData());

        return Utils.getMaterialFromCombinedId(id);
    }

    @Override
    public byte getMeta() {
        int id = Block.getCombinedId(getNmsBlockData());

        return Utils.getMetaFromCombinedId(id);
    }

    @Override
    public void setBlock(Material material, byte meta) {
        int id = Utils.getCombinedId(material, meta);

        IBlockData data = Block.getByCombinedId(id);
        if (data == null)
            throw new IllegalArgumentException("Failed to create block data.");

        _nmsBlockData = data;
    }

    @Override
    public BlockChangePacket_v1_8_R1 clonePacket() {
        PacketContainer clone = Utils.clonePacket(_packet);
        PacketContainer source = _packet;

        StructureModifier<Object> cloneObj = clone.getModifier();
        StructureModifier<Object> sourceObj = source.getModifier();

        IBlockData nmsBlockData = (IBlockData)sourceObj.read(1);

        int id = Block.getCombinedId(nmsBlockData);

        IBlockData nmsCloneData = Block.getByCombinedId(id);

        cloneObj.write(1, nmsCloneData);

        return new BlockChangePacket_v1_8_R1(_nms, clone, _x, _y, _z);
    }

    private IBlockData getNmsBlockData() {
        if (_nmsBlockData == null) {
            _nmsBlockData = (IBlockData)_objects.read(1);
        }
        return _nmsBlockData;
    }

}
