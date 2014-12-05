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

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.jcwhatever.bukkit.generic.messaging.Messenger;
import com.jcwhatever.bukkit.generic.regions.data.ChunkBlockInfo;
import com.jcwhatever.bukkit.generic.regions.data.ChunkInfo;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.IBlockData;
import net.minecraft.server.v1_8_R1.MultiBlockChangeInfo;
import net.minecraft.server.v1_8_R1.PacketPlayOutMultiBlockChange;

import java.util.Iterator;
import java.util.List;

/*
 * 
 */
public class MultiBlockChangeFactory {

    private final ChunkInfo _chunkInfo;
    private short[] _blockPositions;
    private IBlockData[] _blockData;

    public MultiBlockChangeFactory(ChunkInfo chunkInfo, List<ChunkBlockInfo> blocks) {

        _chunkInfo = chunkInfo;

        Iterator<ChunkBlockInfo> iterator = blocks.iterator();

        _blockPositions = new short[blocks.size()];
        _blockData = new IBlockData[blocks.size()];

        for (int i=0; i < blocks.size(); i++) {
            ChunkBlockInfo block = iterator.next();

            int x = block.getChunkBlockX();
            int z = block.getChunkBlockZ();
            int y = block.getY();

            int b = block.getMaterial().getId();
            int m = block.getData();

            // create position integer
            int position = 0;
            position = setValue(position, y,  0, 0x000000FF);
            position = setValue(position, x, 12, 0x0000F000);
            position = setValue(position, z, 8,  0x00000F00);

            _blockPositions[i] = (short)position;

            // create block info integer
            int data = 0;
            data = setValue(data, b,  0, 0x00000FFF);
            data = setValue(data, m, 12, 0x0000F000);

            _blockData[i] = Block.getByCombinedId(data);
        }
    }


    public PacketContainer createPacket() {

        int totalBlocks = _blockData.length;

        Messenger.warning(PhantomPackets.getPlugin(), "Generating Multiblock packet for {0} blocks.", totalBlocks);

        PacketContainer packet = new PacketContainer(Server.MULTI_BLOCK_CHANGE);
        packet.getModifier().writeDefaults();

        // chunk coordinates
        packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(_chunkInfo.getX(), _chunkInfo.getZ()));

        StructureModifier<Object> objects = packet.getModifier();

        PacketPlayOutMultiBlockChange nmsPacket = (PacketPlayOutMultiBlockChange)packet.getHandle();

        MultiBlockChangeInfo[] info = new MultiBlockChangeInfo[totalBlocks];

        for(int i=0; i < totalBlocks; i++) {
            info[i] = new MultiBlockChangeInfo(nmsPacket, _blockPositions[i],  _blockData[i]);
        }

        objects.write(1, info);

        // number of blocks
        //packet.getIntegers().write(0, _blocks.length);

        // data
        //packet.getByteArrays().write(0, _blocks.clone());

        return packet;
    }

    private int setValue(int input, int value, int leftShift, int updateMask) {
        return ((value << leftShift) & updateMask) | (input & ~updateMask);
    }
}
