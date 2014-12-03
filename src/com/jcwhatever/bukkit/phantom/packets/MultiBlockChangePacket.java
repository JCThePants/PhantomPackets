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
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket.PacketBlock;

import org.bukkit.Material;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R1.IBlockData;
import net.minecraft.server.v1_8_R1.MultiBlockChangeInfo;
import net.minecraft.server.v1_8_R1.PacketPlayOutMultiBlockChange;

import java.util.Iterator;

/*
 * 
 */
public class MultiBlockChangePacket extends AbstractPacket implements Iterable<PacketBlock> {

    private ChunkCoordIntPair _nmsCoords;
    private MultiBlockChangeInfo[] _nmsBlockChanges;
    private PacketBlock[] _packetBlocks;

    public MultiBlockChangePacket(PacketContainer packet) {
        super(packet);

        StructureModifier<Object> objects = packet.getModifier();

        _nmsCoords = (ChunkCoordIntPair)objects.read(0);
        _nmsBlockChanges = (MultiBlockChangeInfo[])objects.read(1);
    }

    public int getChunkX() {
        return _nmsCoords.x;
    }

    public int getChunkZ() {
        return _nmsCoords.z;
    }

    @Override
    public void saveChanges() {

        if (_packetBlocks == null)
            return;

        for (int i=0; i < _packetBlocks.length; i++) {

            short blockPosition = _nmsBlockChanges[i].b();

            PacketBlock packetBlock = _packetBlocks[i];

            int id = Utils.getCombinedId(packetBlock.getMaterial(), packetBlock.getMeta());

            IBlockData nmsBlockData = Block.getByCombinedId(id);

            _nmsBlockChanges[i] = new MultiBlockChangeInfo(
                    (PacketPlayOutMultiBlockChange)_packet.getHandle(), blockPosition, nmsBlockData);
        }
    }

    @Override
    public MultiBlockChangePacket clonePacket() {

        PacketContainer clone = Utils.clonePacket(_packet);

        StructureModifier<Object> cloneObj = clone.getModifier();

        MultiBlockChangeInfo[] info = new MultiBlockChangeInfo[_nmsBlockChanges.length];

        for (int i=0; i < info.length; i++) {

            short blockPosition = _nmsBlockChanges[i].b();
            IBlockData nmsBlockData = _nmsBlockChanges[i].c();

            info[i] = new MultiBlockChangeInfo(
                    (PacketPlayOutMultiBlockChange)clone.getHandle(), blockPosition, nmsBlockData);

        }

        cloneObj.write(1, info);

        return new MultiBlockChangePacket(clone);
    }

    @Override
    public Iterator<PacketBlock> iterator() {

        if (_packetBlocks == null) {
            _packetBlocks = new PacketBlock[_nmsBlockChanges.length];

            for (int i = 0; i < _packetBlocks.length; i++) {
                _packetBlocks[i] =  new PacketBlock(_nmsBlockChanges[i]);
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


    public static class PacketBlock {
        private int _x;
        private int _y;
        private int _z;
        private Material _material;
        private byte _meta;

        public PacketBlock(MultiBlockChangeInfo info) {
            BlockPosition nmsPosition = info.a();

            _x = nmsPosition.getX();
            _y = nmsPosition.getY();
            _z = nmsPosition.getZ();

            IBlockData nmsBlockData = info.c();

            int combinedId = Block.getCombinedId(nmsBlockData);

            _material = Utils.getMaterialFromCombinedId(combinedId);
            _meta = Utils.getMetaFromCombinedId(combinedId);
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
