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
import com.jcwhatever.nucleus.utils.coords.ICoords2Di;
import com.jcwhatever.phantom.IPhantomChunk;
import com.jcwhatever.phantom.data.ChunkBulkData;
import com.jcwhatever.phantom.data.ChunkData;
import com.jcwhatever.phantom.packets.factory.IBlockChangeFactory;
import com.jcwhatever.phantom.packets.factory.IMultiBlockChangeFactory;
import com.jcwhatever.phantom.packets.IBlockChangePacket;
import com.jcwhatever.phantom.packets.IBlockDigPacket;
import com.jcwhatever.phantom.packets.IBlockPlacePacket;
import com.jcwhatever.phantom.packets.IMultiBlockChangePacket;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/*
 * 
 */
public interface INmsHandler {

    IBlockDigPacket getBlockDigPacket(PacketContainer packet);

    IBlockPlacePacket getBlockPlacePacket(PacketContainer packet);

    IBlockChangeFactory getBlockChangeFactory(int x, int y, int z, Material material, byte meta);

    IBlockChangePacket getBlockChangePacket(PacketContainer packet);

    IMultiBlockChangeFactory getMultiBlockChangeFactory(World world, ICoords2Di coords, IPhantomChunk chunkData);

    IMultiBlockChangePacket getMultiBlockChangePacket(PacketContainer packet);

    ChunkBulkData getChunkBulkData(PacketContainer packet, World world);

    ChunkData getChunkData(PacketContainer packet, World world);

    void setLightSource(Location location, int intensity, boolean updateChunks);
}
