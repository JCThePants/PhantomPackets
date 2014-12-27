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
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.jcwhatever.nucleus.regions.data.ChunkBlockInfo;
import com.jcwhatever.nucleus.regions.data.ChunkInfo;
import com.jcwhatever.nucleus.utils.ArrayUtils;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangeFactory;

import org.bukkit.Chunk;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.IBlockData;
import net.minecraft.server.v1_8_R1.Material;
import net.minecraft.server.v1_8_R1.MultiBlockChangeInfo;
import net.minecraft.server.v1_8_R1.PacketPlayOutMultiBlockChange;

import java.util.Iterator;
import java.util.List;

/*
 * 
 */
public class MultiBlockChangeFactory_v1_8_R1 implements IMultiBlockChangeFactory {

    private final ChunkInfo _chunkInfo;
    private short[] _blockPositions;
    private ChunkBlockInfo[] _blockInfo;
    private IBlockData[] _blockData;

    public MultiBlockChangeFactory_v1_8_R1(ChunkInfo chunkInfo, List<ChunkBlockInfo> blocks) {

        _chunkInfo = chunkInfo;

        Iterator<ChunkBlockInfo> iterator = blocks.iterator();

        _blockPositions = new short[blocks.size()];
        _blockData = new IBlockData[blocks.size()];
        _blockInfo = new ChunkBlockInfo[blocks.size()];

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
            _blockInfo[i] = block;

            // create block info integer
            int data = 0;
            data = setValue(data, b,  0, 0x00000FFF);
            data = setValue(data, m, 12, 0x0000F000);

            _blockData[i] = Block.getByCombinedId(data);
        }
    }


    @Override
    public PacketContainer createPacket(boolean ignoreAir) {

        int totalBlocks = _blockData.length;

        PacketContainer packet = new PacketContainer(Server.MULTI_BLOCK_CHANGE);
        packet.getModifier().writeDefaults();

        // chunk coordinates
        packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(_chunkInfo.getX(), _chunkInfo.getZ()));

        StructureModifier<Object> objects = packet.getModifier();

        MultiBlockChangeInfo[] infoArray = new MultiBlockChangeInfo[totalBlocks];

        for(int i=0; i < totalBlocks; i++) {

            if (_blockData[i].getBlock().getMaterial() == Material.AIR && ignoreAir)
                continue;

            MultiBlockChangeInfo info = new MultiBlockChangeInfo(
                    (PacketPlayOutMultiBlockChange)packet.getHandle(), _blockPositions[i], _blockData[i]);

            infoArray[i] = info;
        }

        objects.write(1, ArrayUtils.removeNull(infoArray));

        return packet;
    }

    @Override
    public PacketContainer createPacket(Chunk chunk) {
        int totalBlocks = _blockData.length;

        PacketContainer packet = new PacketContainer(Server.MULTI_BLOCK_CHANGE);
        packet.getModifier().writeDefaults();

        // chunk coordinates
        packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(_chunkInfo.getX(), _chunkInfo.getZ()));

        StructureModifier<Object> objects = packet.getModifier();

        MultiBlockChangeInfo[] infoArray = new MultiBlockChangeInfo[totalBlocks];

        for(int i=0; i < totalBlocks; i++) {

            ChunkBlockInfo blockInfo = _blockInfo[i];

            org.bukkit.block.Block block = chunk.getBlock(blockInfo.getChunkBlockX(), blockInfo.getY(), blockInfo.getChunkBlockZ());
            int data = Utils.getCombinedId(block.getType().getId(), block.getData());

            MultiBlockChangeInfo info = new MultiBlockChangeInfo(
                    (PacketPlayOutMultiBlockChange)packet.getHandle(), _blockPositions[i], Block.getByCombinedId(data));

            infoArray[i] = info;
        }

        objects.write(1, ArrayUtils.removeNull(infoArray));

        return packet;
    }

    private int setValue(int input, int value, int leftShift, int updateMask) {
        return ((value << leftShift) & updateMask) | (input & ~updateMask);
    }
}
