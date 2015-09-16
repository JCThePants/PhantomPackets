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

package com.jcwhatever.phantom;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.nucleus.utils.PreCon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Static utilities.
 */
public class Utils {

    private Utils() {}

    private static final ProtocolManager _protocolManager = ProtocolLibrary.getProtocolManager();

    /**
     * Get legacy integer ID for a material and data.
     *
     * @param material  The material.
     * @param data      The material data.
     */
    public static int getLegacyId(Material material, byte data) {
        return (material.getId() << 4) | data;
    }

    /**
     * Extract {@link Material} from a legacy ID.
     *
     * @param id  The legacy ID.
     */
    public static Material getMaterialFromLegacyId(int id) {
        int materialId = id >> 4;
        return Material.getMaterial(materialId);
    }

    /**
     * Extract byte data from a legacy ID.
     *
     * @param id  The legacy ID.
     */
    public static byte getDataFromLegacyId(int id) {
        return (byte)(id & 0xF);
    }

    /**
     * Get a combined ID for a material and data.
     *
     * @param material  The material.
     * @param data      The material data.
     */
    public static int getCombinedId(Material material, byte data) {
        return (material.getId() & 0xFFF) | ((data & 0xF) << 12);
    }

    /**
     * Get a combined ID from a material type ID and data.
     *
     * @param typeId  The material type ID.
     * @param data    The material data.
     */
    public static int getCombinedId(int typeId, int data) {
        return (typeId & 0xFFF) | ((data & 0xF) << 12);
    }

    /**
     * Extract {@link Material} from a combined ID.
     *
     * @param id  The combined ID.
     */
    public static Material getMaterialFromCombinedId(int id) {
        int materialId = id & 0xFFF;
        return Material.getMaterial(materialId);
    }

    /**
     * Extract byte data from a combined ID.
     *
     * @param id  The combined ID.
     */
    public static byte getDataFromCombinedId(int id) {
        return (byte)(id >> 12);
    }

    /**
     * Determine if a chunk is within viewing distance of a location.
     *
     * @param chunkX    The chunk X coordinates.
     * @param chunkZ    The chunk Z coordinates.
     * @param location  The location.
     */
    public static boolean isChunkNearby(int chunkX, int chunkZ, Location location) {
        PreCon.notNull(location);

        int locChunkX = location.getBlockX() >> 4;
        int locChunkZ = location.getBlockZ() >> 4;

        int viewDistance = Bukkit.getServer().getViewDistance();

        return Math.abs(chunkX - locChunkX) <= viewDistance &&
               Math.abs(chunkZ - locChunkZ) <= viewDistance;
    }

    /**
     * Partially clone a packet.
     *
     * @param packet  The packet to clone.
     */
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
