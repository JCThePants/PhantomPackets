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

package com.jcwhatever.phantom.blocks;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.jcwhatever.nucleus.collections.ElementCounter;
import com.jcwhatever.nucleus.collections.ElementCounter.RemovalPolicy;
import com.jcwhatever.nucleus.utils.CollectionUtils;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.managers.NamedInsensitiveManager;
import com.jcwhatever.phantom.IBlockContextManager;
import com.jcwhatever.phantom.IPhantomBlock;
import com.jcwhatever.phantom.IPhantomBlockContext;
import com.jcwhatever.phantom.IPhantomChunk;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Context manager.
 */
public class BlockContextManager extends NamedInsensitiveManager<IPhantomBlockContext>
        implements IBlockContextManager {

    private final ElementCounter<World> _worlds = new ElementCounter<World>(RemovalPolicy.REMOVE);
    private final Multimap<World, IPhantomBlockContext> _worldContexts =
            MultimapBuilder.hashKeys().arrayListValues().build();

    @Override
    public boolean add(IPhantomBlockContext context) {
        PreCon.notNull(context);

        if (super.add(context)) {
            _worlds.add(context.getWorld());
            _worldContexts.put(context.getWorld(), context);
            return true;
        }

        return false;
    }

    @Override
    public boolean remove(String name) {
        PreCon.notNullOrEmpty(name);

        IPhantomBlockContext context = get(name);
        if (context == null)
            return false;

        if (super.remove(name)) {
            _worlds.subtract(context.getWorld());
            _worldContexts.remove(context.getWorld(), context);
            return true;
        }

        return false;
    }

    public PhantomBlocks addBlocksContext(World world, String name) {
        PreCon.notNull(world);
        PreCon.notNullOrEmpty(name);

        PhantomBlocks blocks = new PhantomBlocks(this, world, name);
        add(blocks);
        return blocks;
    }

    public boolean removeBlocksContext(String name) {
        PreCon.notNullOrEmpty(name);

        if (!contains(name))
            return false;

        IPhantomBlockContext context = get(name);
        return context instanceof PhantomBlocks && remove(name);
    }

    @Override
    public boolean hasPhantomBlocksInWorld(World world) {
        PreCon.notNull(world);

        return _worlds.contains(world);
    }

    @Nullable
    @Override
    public IPhantomBlock getBlockAt(World world, int x, int y, int z) {
        PreCon.notNull(world);

        Collection<IPhantomBlockContext> contexts = _worldContexts.get(world);
        if (contexts.isEmpty())
            return null;

        for (IPhantomBlockContext context : contexts) {
            IPhantomBlock block = context.getPhantomBlock(x, y, z);
            if (block == null)
                continue;

            return block;
        }

        return null;
    }

    @Override
    public Collection<IPhantomBlockContext> getChunkContexts(World world, int chunkX, int chunkZ) {
        PreCon.notNull(world);

        Collection<IPhantomBlockContext> contexts = _worldContexts.get(world);
        if (contexts.isEmpty())
            return CollectionUtils.unmodifiableList(IPhantomBlockContext.class);

        List<IPhantomBlockContext> results = new ArrayList<>(5);

        for (IPhantomBlockContext context : contexts) {
            IPhantomChunk chunk = context.getPhantomChunk(chunkX, chunkZ);
            if (chunk == null)
                continue;

            results.add(context);
        }

        return results;
    }
}
