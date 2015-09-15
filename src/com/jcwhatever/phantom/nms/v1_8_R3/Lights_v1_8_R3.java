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

package com.jcwhatever.phantom.nms.v1_8_R3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Iterables;
import com.jcwhatever.nucleus.providers.npc.Npcs;
import com.jcwhatever.nucleus.utils.BlockUtils;
import com.jcwhatever.nucleus.utils.coords.ChunkUtils;
import com.jcwhatever.nucleus.utils.coords.IChunkCoords;
import com.jcwhatever.nucleus.utils.materials.MaterialProperty;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumSkyBlock;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;
import net.minecraft.server.v1_8_R3.WorldServer;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Entity;

/*
 * 
 */
public class Lights_v1_8_R3 {

    public void setLightSource(Location location, int intensity, boolean updateChunks) {

        WorldServer world = ((CraftWorld)location.getWorld()).getHandle();

        BlockPosition position = new BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (intensity > 0) {
            // set light source
            world.a(EnumSkyBlock.BLOCK, position, intensity);

            Block block = location.getBlock();
            Block trans = BlockUtils.getFirstAdjacent(block, MaterialProperty.TRANSPARENT);
            if (trans == null)
                trans = block;

            position = new BlockPosition(trans.getX(), trans.getY(), trans.getZ());
        }

        // recalculate lighting
        world.c(EnumSkyBlock.BLOCK, position);

        if (updateChunks) {
            Collection<IChunkCoords> localChunks = ChunkUtils.getChunksInRadius(location, 8);
            if (localChunks.size() == 1) {
                updateChunk(location.getWorld(), Iterables.getFirst(localChunks, null));
            } else {
                updateChunks(location.getWorld(), localChunks);
            }
        }
    }

    private void updateChunks(org.bukkit.World world, Collection<IChunkCoords> coords) {

        List<Chunk> chunks = new ArrayList<>(coords.size());
        for (IChunkCoords coord : coords) {
            Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(coord.getX(), coord.getZ());
            chunks.add(chunk);
        }

        PacketPlayOutMapChunkBulk bulkPacket = new PacketPlayOutMapChunkBulk(chunks);
        List<EntityHuman> players = ((CraftWorld)world).getHandle().players;

        for (Chunk chunk : chunks) {
            for (EntityHuman human : players) {
                if (human instanceof EntityPlayer) {

                    Entity entity = human.getBukkitEntity();
                    if (Npcs.isNpc(entity))
                        continue;

                    EntityPlayer player = (EntityPlayer) human;
                    if (distance(player, chunk) <= 6) {
                        player.playerConnection.sendPacket(bulkPacket);
                    }
                }
            }
        }
    }

    private void updateChunk(org.bukkit.World world, IChunkCoords coords) {

        Chunk chunk = ((CraftWorld) world).getHandle().getChunkAt(coords.getX(), coords.getZ());
        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, 65535);
        List<EntityHuman> players = ((CraftWorld)world).getHandle().players;

        for (EntityHuman human : players) {
            if (human instanceof EntityPlayer) {

                Entity entity = human.getBukkitEntity();
                if (Npcs.isNpc(entity))
                    continue;

                EntityPlayer player = (EntityPlayer) human;
                if (distance(player, chunk) <= 6) {
                    player.playerConnection.sendPacket(packet);
                }
            }
        }
    }

    private int distance(EntityPlayer player, Chunk to) {
        int fromX = (int)player.locX >> 4;
        int fromZ = (int)player.locZ >> 4;

        int deltaX = to.locX - fromX;
        int deltaZ = to.locZ - fromZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }
}
