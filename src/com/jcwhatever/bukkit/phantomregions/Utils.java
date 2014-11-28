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

package com.jcwhatever.bukkit.phantomregions;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/*
 * 
 */
public class Utils {

    private static final ProtocolManager _protocolManager = ProtocolLibrary.getProtocolManager();

    private Utils() {}

    public static boolean isChunkNearby(int chunkX, int chunkZ, Location location) {
        Chunk chunk = location.getChunk();

        return Math.abs(chunkX - (chunk.getX() >> 4)) == 0 &&
               Math.abs(chunkZ - (chunk.getZ() >> 4)) == 0;
    }

    public static PacketContainer cloneBlockChangePacket(PacketContainer packet) {

        PacketContainer clone = clonePacket(packet);

        StructureModifier<Integer> ints = packet.getIntegers();
        StructureModifier<Integer> cloneInts = clone.getIntegers();

        for (int i=0; i < ints.size(); i++) {
            cloneInts.write(i, ints.read(i));
        }

        StructureModifier<Material> blocks = packet.getBlocks();
        StructureModifier<Material> cloneBlocks = packet.getBlocks();

        for (int i=0; i < ints.size(); i++) {
            cloneBlocks.write(i, blocks.read(i));
        }

        return clone;
    }

    public static PacketContainer cloneMultiBlockPacket(PacketContainer packet) {
        PacketContainer clone = clonePacket(packet);

        byte[] source = packet.getByteArrays().read(0);
        byte[] dest = new byte[source.length];

        System.arraycopy(source, 0, dest, 0, source.length);

        clone.getByteArrays().write(0, dest);

        return clone;
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

    public static void refreshChunk(Player player, int chunkX, int chunkZ) {

        BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        Object entityPlayer = unwrapper.unwrapItem(player);

        Class<?> chunkCoordClass = MinecraftReflection.getMinecraftClass("ChunkCoordIntPair");

        FuzzyReflection playerReflect = FuzzyReflection.fromObject(entityPlayer);

        Field chunkCoordIntPairQueue = playerReflect.getFieldByName("chunkCoordIntPairQueue");


        // Get the ChunkCoordIntPair queue
        List<Object> chunkQueue;
        try {

            @SuppressWarnings("unchecked")
            List<Object> result =(List<Object>)chunkCoordIntPairQueue.get(entityPlayer);

            chunkQueue = result;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        // Get ChunkCoordIntPair constructor
        Constructor<?> constructor;
        try {
            constructor = chunkCoordClass.getConstructor(int.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        // Construct new instance of ChunkCoordIntPair
        Object chunkCoords;
        try {
            chunkCoords = constructor.newInstance(chunkX, chunkZ);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return;
        }

        chunkQueue.add(chunkCoords);
    }
}
