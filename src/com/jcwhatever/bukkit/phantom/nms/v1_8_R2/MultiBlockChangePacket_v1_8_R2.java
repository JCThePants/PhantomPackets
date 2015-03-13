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

package com.jcwhatever.bukkit.phantom.nms.v1_8_R2;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.nms.INmsHandler;
import com.jcwhatever.bukkit.phantom.packets.AbstractPacket;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.PacketBlock;

import org.bukkit.Material;

import net.minecraft.server.v1_8_R2.Block;
import net.minecraft.server.v1_8_R2.BlockPosition;
import net.minecraft.server.v1_8_R2.IBlockData;
import net.minecraft.server.v1_8_R2.PacketPlayOutMultiBlockChange.MultiBlockChangeInfo;
import net.minecraft.server.v1_8_R2.PacketPlayOutMultiBlockChange;

import java.util.Iterator;

/*
 * 
 */
public class MultiBlockChangePacket_v1_8_R2 extends AbstractPacket implements IMultiBlockChangePacket, Iterable<PacketBlock> {

    private final INmsHandler _nms;
    private int _chunkX;
    private int _chunkZ;
    private MultiBlockChangeInfo[] _nmsBlockChanges;
    private PacketBlock[] _packetBlocks;
    private StructureModifier<Object> _objects;

    public MultiBlockChangePacket_v1_8_R2(INmsHandler handler, PacketContainer packet, int chunkX, int chunkZ) {
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

        MultiBlockChangeInfo[] array = getNmsBlockChanges();

        for (int i=0; i < _packetBlocks.length; i++) {

            short blockPosition = array[i].b();

            PacketBlock packetBlock = _packetBlocks[i];

            int id = Utils.getCombinedId(packetBlock.getMaterial(), packetBlock.getMeta());

            IBlockData nmsBlockData = Block.getByCombinedId(id);

            MultiBlockChangeInfo blockChangeInfo = MultiBlockChangeInfoUtil.create(
                    (PacketPlayOutMultiBlockChange)_packet.getHandle(), blockPosition, nmsBlockData
            );

            array[i] = blockChangeInfo;
        }
    }

    @Override
    public MultiBlockChangePacket_v1_8_R2 clonePacket() {

        PacketContainer clone = Utils.clonePacket(_packet);

        StructureModifier<Object> cloneObj = clone.getModifier();

        MultiBlockChangeInfo[] array = getNmsBlockChanges();
        MultiBlockChangeInfo[] newArray = new MultiBlockChangeInfo[array.length];

        for (int i=0; i < newArray.length; i++) {

            MultiBlockChangeInfo info = array[i];

            short blockPosition = info.b();
            IBlockData nmsBlockData = info.c();

            MultiBlockChangeInfo multiBlockChangeInfo = MultiBlockChangeInfoUtil.create(
                    (PacketPlayOutMultiBlockChange)clone.getHandle(), blockPosition, nmsBlockData
            );

            newArray[i] = multiBlockChangeInfo;
        }

        cloneObj.write(1, newArray);

        return new MultiBlockChangePacket_v1_8_R2(_nms, clone, _chunkX, _chunkZ);
    }

    @Override
    public Iterator<PacketBlock> iterator() {

        if (_packetBlocks == null) {

            MultiBlockChangeInfo[] array = getNmsBlockChanges();

            _packetBlocks = new PacketBlock[array.length];

            for (int i = 0; i < _packetBlocks.length; i++) {

                MultiBlockChangeInfo info = array[i];

                BlockPosition nmsPosition = info.a(); //block position
                IBlockData nmsBlockData = info.c(); // IBlockData

                int x = nmsPosition.getX();
                int y = nmsPosition.getY();
                int z = nmsPosition.getZ();

                int combinedId = Block.getCombinedId(nmsBlockData);

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

    private MultiBlockChangeInfo[] getNmsBlockChanges() {
        if (_nmsBlockChanges == null) {
            _nmsBlockChanges = (MultiBlockChangeInfo[])_objects.read(1);
        }
        return _nmsBlockChanges;
    }
}
