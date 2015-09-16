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

import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.nucleus.mixins.IDisposable;
import com.jcwhatever.nucleus.mixins.INamedInsensitive;
import com.jcwhatever.phantom.packets.IMultiBlockChangePacket;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Interface for a block context.
 */
public interface IPhantomBlockContext extends IViewable, INamedInsensitive, IDisposable {

    /**
     * Get the world the context is for.
     */
    World getWorld();

    /**
     * Get the owning context manager.
     */
    IBlockContextManager getManager();

    /**
     * Determine if the context ignores phantom air blocks.
     */
    boolean ignoresAir();

    /**
     * Set the flag for ignoring phantom air blocks.
     *
     * @param ignoresAir  True to ignore phantom air blocks, otherwise false.
     */
    void setIgnoresAir(boolean ignoresAir);

    /**
     * Translate phantom block data into a multi block packet.
     *
     * @param player  The player the packet is being sent to.
     * @param packet  The packet.
     */
    void translateMultiBlock(Player player, IMultiBlockChangePacket packet);

    /**
     * Translate phantom block data into a chunk packet.
     *
     * @param player  The player the packet is being sent to.
     * @param packet  The packet.
     */
    void translateMapChunk(Player player, PacketContainer packet);

    /**
     * Translate phantom block data into a bulk chunk packet.
     *
     * @param player  The player the packet is being sent to.
     * @param packet  The packet.
     */
    void translateMapChunkBulk(Player player, PacketContainer packet);

    /**
     * Get a phantom block from the specified coordinates.
     *
     * <p>If a phantom block has already been created, it is returned. Otherwise a new
     * block is created using the actual block properties.</p>
     *
     * <p>When a new phantom block is created, it is not stored into the context until
     * its {@link IPhantomBlock#set} method is invoked.</p>
     *
     * @param x  The world X coordinates.
     * @param y  The world Y coordinates.
     * @param z  The world Z coordinates.
     */
    IPhantomBlock getBlock(int x, int y, int z);

    /**
     * Get a phantom block from the specified coordinates.
     *
     * @param x  The world X coordinates.
     * @param y  The world Y coordinates.
     * @param z  The world Z coordinates.
     *
     * @return  The phantom block or null if one is not set.
     */
    @Nullable
    IPhantomBlock getPhantomBlock(int x, int y, int z);

    /**
     * Get a phantom chunk from the context.
     *
     * @param x  The chunk X coordinates.
     * @param z  The chunk Z coordinates.
     *
     * @return  The phantom chunk or null if a phantom block whose coordinates
     * are within the chunk has not been set.
     */
    @Nullable
    IPhantomChunk getPhantomChunk(int x, int z);
}
