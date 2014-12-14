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
import com.jcwhatever.bukkit.generic.utils.reflection.ReflectedArray;
import com.jcwhatever.bukkit.generic.utils.reflection.ReflectedInstance;
import com.jcwhatever.bukkit.phantom.NmsTypes;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket.PacketBlock;

import org.bukkit.Material;

import java.util.Iterator;

/*
 * 
 */
public class MultiBlockChangePacket extends AbstractPacket implements Iterable<PacketBlock> {

    private int _chunkX;
    private int _chunkZ;
    private ReflectedArray<?> _nmsBlockChanges;
    private PacketBlock[] _packetBlocks;
    private StructureModifier<Object> _objects;

    public MultiBlockChangePacket(PacketContainer packet) {
        super(packet);

        _objects = packet.getModifier();

        ReflectedInstance<?> nmsCoords = NmsTypes.CHUNK_COORD_INT_PAIR.reflect(_objects.read(0));
        if (nmsCoords == null)
            throw new RuntimeException("Failed to ChunkCoordIntPair for MultiBlockChangePacket");

        Fields fields = nmsCoords.getFields(int.class);

        _chunkX = fields.get(0);
        _chunkZ = fields.get(1);
    }

    public int getChunkX() {
        return _chunkX;
    }

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

            Object nmsBlockData = NmsTypes.BLOCK.call("getByCombinedId", id);//Block.getByCombinedId(id);

            Object blockChangeInfo = NmsTypes.MULTI_BLOCK_CHANGE_INFO
                    .newInstance(_packet.getHandle(), blockPosition, nmsBlockData);

            getNmsBlockChanges().set(i, blockChangeInfo);
        }
    }

    @Override
    public MultiBlockChangePacket clonePacket() {

        PacketContainer clone = Utils.clonePacket(_packet);

        StructureModifier<Object> cloneObj = clone.getModifier();

        ReflectedArray newArray = NmsTypes.MULTI_BLOCK_CHANGE_INFO.newArray(getNmsBlockChanges().length());

        for (int i=0; i < newArray.length(); i++) {

            ReflectedInstance<?> info = getNmsBlockChanges().getReflected(i);

            short blockPosition = info.getFields(short.class).get(0);//.b();
            Object nmsBlockData = info.getFields().get(1); //(IBlockData)_nmsBlockChanges[i].c();

            Object multiBlockChangeInfo = NmsTypes.MULTI_BLOCK_CHANGE_INFO.newInstance(
                    clone.getHandle(), blockPosition, nmsBlockData
            );

            newArray.set(i, multiBlockChangeInfo);
        }

        cloneObj.write(1, newArray.getHandle());

        return new MultiBlockChangePacket(clone);
    }

    @Override
    public Iterator<PacketBlock> iterator() {

        if (_packetBlocks == null) {
            _packetBlocks = new PacketBlock[getNmsBlockChanges().length()];

            for (int i = 0; i < _packetBlocks.length; i++) {
                _packetBlocks[i] =  new PacketBlock(getNmsBlockChanges().get(i), _chunkX, _chunkZ);
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
            _nmsBlockChanges = new ReflectedArray<>(NmsTypes.MULTI_BLOCK_CHANGE_INFO, blockChangeArray);
        }
        return _nmsBlockChanges;
    }

    public static class PacketBlock {
        private int _x;
        private int _y;
        private int _z;
        private Material _material;
        private byte _meta;

        public PacketBlock(Object objectInfo, int chunkX, int chunkZ) { // MultiBlockChangeInfo info) {

            ReflectedInstance<?> info = new ReflectedInstance<>(NmsTypes.MULTI_BLOCK_CHANGE_INFO, objectInfo);

            Fields fields = info.getFields();

            short position = fields.get(0);// .a(); block position
            Object nmsBlockData = fields.get(1); //.c(); // IBlockData

            _x = (chunkX << 4) + (position >> 12 & 15);
            _y = position & 255;
            _z = (chunkZ << 4) + (position >> 8 & 15);

            Integer combinedId = NmsTypes.BLOCK.call("getCombinedId", nmsBlockData); // Block.getCombinedId(nmsBlockData);
            if (combinedId != null) {
                _material = Utils.getMaterialFromCombinedId(combinedId);
                _meta = Utils.getMetaFromCombinedId(combinedId);
            }
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

        public byte getMeta() {
            return _meta;
        }

        public void setBlock(Material material, int meta) {
            _material = material;
            _meta = (byte)meta;
        }
    }
}
