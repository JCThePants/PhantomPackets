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
import com.jcwhatever.bukkit.generic.reflection.Fields;
import com.jcwhatever.bukkit.generic.reflection.ReflectedArray;
import com.jcwhatever.bukkit.generic.reflection.ReflectedInstance;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.nms.INmsHandler;
import com.jcwhatever.bukkit.phantom.nms.NmsTypes;
import com.jcwhatever.bukkit.phantom.packets.AbstractPacket;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.PacketBlock;

import org.bukkit.Material;

import java.util.Iterator;

/*
 * 
 */
public class MultiBlockChangePacket extends AbstractPacket implements IMultiBlockChangePacket, Iterable<PacketBlock> {

    private final INmsHandler _nms;
    private int _chunkX;
    private int _chunkZ;
    private ReflectedArray<?> _nmsBlockChanges;
    private PacketBlock[] _packetBlocks;
    private StructureModifier<Object> _objects;

    public MultiBlockChangePacket(INmsHandler handler, PacketContainer packet, int chunkX, int chunkZ) {
        super(packet);

        _nms = handler;

        _objects = packet.getModifier();

        _chunkX = chunkX;
        _chunkZ = chunkZ;
    }

    @Override
    public int getChunkX() {
        return _chunkX;
    }

    @Override
    public int getChunkZ() {
        return _chunkZ;
    }

    @Override
    public void saveChanges() {

        if (_packetBlocks == null)
            return;

        for (int i=0; i < _packetBlocks.length; i++) {

            short blockPosition = getNmsBlockChanges().getReflected(i).getFields(short.class).get(0); //b();

            PacketBlock packetBlock = _packetBlocks[i];

            int id = Utils.getCombinedId(packetBlock.getMaterial(), packetBlock.getMeta());

            Object nmsBlockData = _nms.getBlockByCombinedId(id);//Block.getByCombinedId(id);

            Object blockChangeInfo = _nms.getReflectedType(NmsTypes.MULTI_BLOCK_CHANGE_INFO)
                    .newInstance(_packet.getHandle(), blockPosition, nmsBlockData);

            getNmsBlockChanges().set(i, blockChangeInfo);
        }
    }

    @Override
    public MultiBlockChangePacket clonePacket() {

        PacketContainer clone = Utils.clonePacket(_packet);

        StructureModifier<Object> cloneObj = clone.getModifier();

        ReflectedArray newArray = _nms.getReflectedType(NmsTypes.MULTI_BLOCK_CHANGE_INFO)
                .newArray(getNmsBlockChanges().length());

        for (int i=0; i < newArray.length(); i++) {

            ReflectedInstance<?> info = getNmsBlockChanges().getReflected(i);

            short blockPosition = info.getFields(short.class).get(0);//.b();
            Object nmsBlockData = info.getFields().get(1); //(IBlockData)_nmsBlockChanges[i].c();

            Object multiBlockChangeInfo = _nms.getReflectedType(NmsTypes.MULTI_BLOCK_CHANGE_INFO).newInstance(
                    clone.getHandle(), blockPosition, nmsBlockData
            );

            newArray.set(i, multiBlockChangeInfo);
        }

        cloneObj.write(1, newArray.getHandle());

        return new MultiBlockChangePacket(_nms, clone, _chunkX, _chunkZ);
    }

    @Override
    public Iterator<PacketBlock> iterator() {

        if (_packetBlocks == null) {
            _packetBlocks = new PacketBlock[getNmsBlockChanges().length()];

            for (int i = 0; i < _packetBlocks.length; i++) {

                Object objectInfo = getNmsBlockChanges().get(i);

                ReflectedInstance<?> info = _nms.reflect(NmsTypes.MULTI_BLOCK_CHANGE_INFO, objectInfo);

                Fields fields = info.getFields();

                short position = fields.get(0);// .a(); block position
                Object nmsBlockData = fields.get(1); //.c(); // IBlockData

                int x = (_chunkX << 4) + (position >> 12 & 15);
                int y = position & 255;
                int z = (_chunkZ << 4) + (position >> 8 & 15);

                int combinedId = _nms.getBlockCombinedId(nmsBlockData); // Block.getCombinedId(nmsBlockData);

                Material material = Utils.getMaterialFromCombinedId(combinedId);
                byte meta = Utils.getMetaFromCombinedId(combinedId);

                _packetBlocks[i] =  new PacketBlock(x, y, z, material, meta);
            }
        }

        return new Iterator<PacketBlock>() {

            int index = 0;

            @Override
            public boolean hasNext() {
                return index < _packetBlocks.length;
            }

            @Override
            public PacketBlock next() {
                PacketBlock result = _packetBlocks[index];
                index++;

                return result;
            }

            @Override
            public void remove() {
                _packetBlocks[index].setBlock(Material.AIR, 0);
            }
        };
    }

    private ReflectedArray<?> getNmsBlockChanges() {
        if (_nmsBlockChanges == null) {
            Object blockChangeArray = _objects.read(1); // MultiBlockChangeInfo[]
            _nmsBlockChanges =  _nms.reflectArray(NmsTypes.MULTI_BLOCK_CHANGE_INFO, blockChangeArray);
        }
        return _nmsBlockChanges;
    }


}
