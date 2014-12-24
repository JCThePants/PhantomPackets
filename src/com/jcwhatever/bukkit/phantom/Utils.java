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

package com.jcwhatever.bukkit.phantom;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

/*
 * 
 */
public class Utils {

    private static final ProtocolManager _protocolManager = ProtocolLibrary.getProtocolManager();

    private Utils() {}

    public static int getLegacyId(Material material, byte meta) {
        return (material.getId() << 4) | meta;
    }

    public static Material getMaterialFromLegacyId(int id) {
        int materialId = id >> 4;
        return Material.getMaterial(materialId);
    }

    public static byte getMetaFromLegacyId(int id) {
        return (byte)(id & 0xF);
    }

    public static int getCombinedId(Material material, byte meta) {
        return (material.getId() & 0xFFF) | ((meta & 0xF) << 12);
    }

    public static Material getMaterialFromCombinedId(int id) {
        int materialId = id & 0xFFF;
        return Material.getMaterial(materialId);
    }

    public static byte getMetaFromCombinedId(int id) {
        return (byte)(id >> 12);
    }

    public static boolean isChunkNearby(int chunkX, int chunkZ, Location location) {
        Chunk chunk = location.getChunk();

        return Math.abs(chunkX - (chunk.getX() >> 4)) == 0 &&
               Math.abs(chunkZ - (chunk.getZ() >> 4)) == 0;
    }

    public static PacketContainer clonePacket(PacketContainer packet) {
        PacketContainer clone = _protocolManager.createPacket(packet.getType());

        StructureModifier<Object> source = packet.getModifier();
        StructureModifier<Object> dest = clone.getModifier();

        for (int i=0; i < source.size(); i++) {
            dest.write(i, source.read(i));
        }

        return clone;
    }
}
