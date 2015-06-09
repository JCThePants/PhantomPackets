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
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.jcwhatever.nucleus.utils.ArrayUtils;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.coords.ICoords2Di;
import com.jcwhatever.phantom.IPhantomBlock;
import com.jcwhatever.phantom.IPhantomChunk;
import com.jcwhatever.phantom.Utils;
import com.jcwhatever.phantom.nms.factory.IMultiBlockChangeFactory;
import com.jcwhatever.phantom.nms.v1_8_R3.MultiBlockChangeInfoUtil;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange.MultiBlockChangeInfo;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.Iterator;

/*
 * 
 */
public class MultiBlockChangeFactory_v1_8_R3 implements IMultiBlockChangeFactory {

    private final World _world;
    private final ICoords2Di _coords;
    private short[] _blockPositions;
    private IPhantomBlock[] _blockInfo;
    private IBlockData[] _blockData;

    public MultiBlockChangeFactory_v1_8_R3(World world, ICoords2Di coords, IPhantomChunk chunkData) {
        PreCon.notNull(world);
        PreCon.notNull(coords);
        PreCon.notNull(chunkData);

        _world = world;
        _coords = coords;

        Iterator<IPhantomBlock> iterator = chunkData.iterator();

        _blockPositions = new short[chunkData.totalBlocks()];
        _blockData = new IBlockData[chunkData.totalBlocks()];
        _blockInfo = new IPhantomBlock[chunkData.totalBlocks()];

        for (int i=0; i < chunkData.totalBlocks(); i++) {
            IPhantomBlock block = iterator.next();

            int x = block.getX();
            int z = block.getZ();
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
        packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(_coords.getX(), _coords.getZ()));

        StructureModifier<Object> objects = packet.getModifier();

        MultiBlockChangeInfo[] infoArray = new MultiBlockChangeInfo[totalBlocks];

        for(int i=0; i < totalBlocks; i++) {

            if (_blockData[i].getBlock().getMaterial() == Material.AIR && ignoreAir)
                continue;

            MultiBlockChangeInfo multiBlockChangeInfo = MultiBlockChangeInfoUtil.create(
                    (PacketPlayOutMultiBlockChange) packet.getHandle(), _blockPositions[i], _blockData[i]
            );

            infoArray[i] = multiBlockChangeInfo;
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
        packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(_coords.getX(), _coords.getZ()));

        StructureModifier<Object> objects = packet.getModifier();

        MultiBlockChangeInfo[] infoArray = new MultiBlockChangeInfo[totalBlocks];

        for(int i=0; i < totalBlocks; i++) {

            IPhantomBlock blockInfo = _blockInfo[i];

            org.bukkit.block.Block block = chunk.getBlock(blockInfo.getX(), blockInfo.getY(), blockInfo.getZ());
            int data = Utils.getCombinedId(block.getType().getId(), block.getData());

            MultiBlockChangeInfo multiBlockChangeInfo = MultiBlockChangeInfoUtil.create(
                    (PacketPlayOutMultiBlockChange) packet.getHandle(), _blockPositions[i], Block.getByCombinedId(data)
            );

            infoArray[i] = multiBlockChangeInfo;
        }

        objects.write(1, ArrayUtils.removeNull(infoArray));

        return packet;
    }

    private int setValue(int input, int value, int leftShift, int updateMask) {
        return ((value << leftShift) & updateMask) | (input & ~updateMask);
    }
}
