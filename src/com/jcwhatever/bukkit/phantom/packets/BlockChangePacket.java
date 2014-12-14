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
import com.jcwhatever.bukkit.generic.utils.reflection.Fields;
import com.jcwhatever.bukkit.generic.utils.reflection.ReflectedInstance;
import com.jcwhatever.bukkit.phantom.NmsTypes;
import com.jcwhatever.bukkit.phantom.Utils;

import org.bukkit.Material;

/*
 * 
 */
public class BlockChangePacket extends AbstractPacket {

    private int _x;
    private int _y;
    private int _z;
    private ReflectedInstance<?> _nmsBlockData; // IBlockData
    private StructureModifier<Object> _objects;

    public BlockChangePacket(PacketContainer packet) {
        super(packet);

         _objects = packet.getModifier();

        ReflectedInstance<?> nmsBlockPosition = NmsTypes.BASE_BLOCK_POSITION.reflect(_objects.read(0));
        Fields intFields = nmsBlockPosition.getFields(int.class);

        _x = intFields.get(0);
        _y = intFields.get(1);
        _z = intFields.get(2);
    }

    @Override
    public void saveChanges() {

        StructureModifier<Object> objects = _packet.getModifier();
        objects.write(1, _nmsBlockData.getHandle());
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
        //noinspection ConstantConditions
        int id = NmsTypes.BLOCK.call("getCombinedId", getNmsBlockData().getHandle());

        return Utils.getMaterialFromCombinedId(id);
    }

    public byte getMeta() {
        //noinspection ConstantConditions
        int id = NmsTypes.BLOCK.call("getCombinedId", getNmsBlockData().getHandle());

        return Utils.getMetaFromCombinedId(id);
    }

    public void setBlock(Material material, byte meta) {
        int id = Utils.getCombinedId(material, meta);

        Object data = NmsTypes.BLOCK.call("getByCombinedId", id);
        if (data == null)
            throw new IllegalArgumentException("Failed to create block data.");

        _nmsBlockData = NmsTypes.IBLOCK_DATA.reflect(data);
    }

    @Override
    public BlockChangePacket clonePacket() {
        PacketContainer clone = Utils.clonePacket(_packet);
        PacketContainer source = _packet;

        StructureModifier<Object> cloneObj = clone.getModifier();
        StructureModifier<Object> sourceObj = source.getModifier();

        /*IBlockData*/ ReflectedInstance<?> nmsBlockData = NmsTypes.IBLOCK_DATA.reflect(sourceObj.read(1));

        //noinspection ConstantConditions
        int id = NmsTypes.BLOCK.call("getCombinedId", nmsBlockData.getHandle());

        /*IBlockData*/ Object nmsCloneData = NmsTypes.BLOCK.call("getByCombinedId", id);

        cloneObj.write(1, nmsCloneData);

        return new BlockChangePacket(clone);
    }

    private ReflectedInstance<?> getNmsBlockData() {
        if (_nmsBlockData == null) {
            _nmsBlockData = NmsTypes.IBLOCK_DATA.reflect(_objects.read(1));
        }
        return _nmsBlockData;
    }

}
