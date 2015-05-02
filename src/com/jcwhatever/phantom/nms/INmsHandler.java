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

package com.jcwhatever.phantom.nms;

import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.phantom.data.ChunkBulkData;
import com.jcwhatever.phantom.data.ChunkData;
import com.jcwhatever.phantom.nms.factory.IBlockChangeFactory;
import com.jcwhatever.phantom.nms.packets.IBlockChangePacket;
import com.jcwhatever.phantom.nms.packets.IBlockDigPacket;
import com.jcwhatever.phantom.nms.packets.IBlockPlacePacket;
import com.jcwhatever.phantom.nms.factory.IMultiBlockChangeFactory;
import com.jcwhatever.phantom.nms.packets.IMultiBlockChangePacket;
import com.jcwhatever.nucleus.utils.coords.ChunkBlockInfo;
import com.jcwhatever.nucleus.utils.coords.IChunkCoords;
import com.jcwhatever.nucleus.utils.coords.WorldInfo;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/*
 * 
 */
public interface INmsHandler {

    void refreshChunk(Player player, int x, int z);

    IBlockDigPacket getBlockDigPacket(PacketContainer packet);

    IBlockPlacePacket getBlockPlacePacket(PacketContainer packet);

    IBlockChangeFactory getBlockChangeFactory(int x, int y, int z, Material material, byte meta);

    IBlockChangePacket getBlockChangePacket(PacketContainer packet);

    IMultiBlockChangeFactory getMultiBlockChangeFactory(IChunkCoords chunkInfo, List<ChunkBlockInfo> blocks);

    IMultiBlockChangePacket getMultiBlockChangePacket(PacketContainer packet);

    ChunkBulkData getChunkBulkData(PacketContainer packet, WorldInfo world);

    ChunkData getChunkData(PacketContainer packet, WorldInfo world);
}
