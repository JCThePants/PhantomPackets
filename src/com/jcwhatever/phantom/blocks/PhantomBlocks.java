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

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.nucleus.collections.players.PlayerSet;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.coords.Coords2Di;
import com.jcwhatever.nucleus.utils.coords.ICoords2Di;
import com.jcwhatever.nucleus.utils.coords.MutableCoords2Di;
import com.jcwhatever.phantom.IPhantomBlock;
import com.jcwhatever.phantom.IPhantomBlockContext;
import com.jcwhatever.phantom.IPhantomChunk;
import com.jcwhatever.phantom.IBlockContextManager;
import com.jcwhatever.phantom.PhantomPackets;
import com.jcwhatever.phantom.Utils;
import com.jcwhatever.phantom.data.ChunkBulkData;
import com.jcwhatever.phantom.data.ChunkData;
import com.jcwhatever.phantom.data.IChunkData;
import com.jcwhatever.phantom.nms.factory.IMultiBlockChangeFactory;
import com.jcwhatever.phantom.nms.packets.IMultiBlockChangePacket;
import com.jcwhatever.phantom.nms.packets.PacketBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Simple phantom block context.
 */
public class PhantomBlocks implements IPhantomBlockContext {

    private final IBlockContextManager _manager;
    private final World _world;
    private final String _name;
    private final String _searchName;
    private final Map<ICoords2Di, PhantomChunk> _chunks = new HashMap<>(25);
    private final MutableCoords2Di _chunkMatcher = new MutableCoords2Di();

    private Set<Player> _viewers;
    private boolean _ignoresAir;
    private ViewPolicy _viewPolicy = ViewPolicy.WHITELIST;
    private boolean _isDisposed;

    private final Location PLAYER_LOCATION = new Location(null, 0, 0, 0);

    /**
     * Constructor.
     *
     * @param manager  The owning context manager.
     * @param world    The world the context is for.
     * @param name     The name of the context.
     */
    public PhantomBlocks(IBlockContextManager manager, World world, String name) {
        PreCon.notNull(manager);
        PreCon.notNull(world);
        PreCon.notNullOrEmpty(name);

        _manager = manager;
        _world = world;
        _name = name;
        _searchName = name.toLowerCase();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getSearchName() {
        return _searchName;
    }

    @Override
    public World getWorld() {
        return _world;
    }

    @Override
    public IBlockContextManager getManager() {
        return _manager;
    }

    @Override
    public boolean ignoresAir() {
        return _ignoresAir;
    }

    @Override
    public void setIgnoresAir(boolean ignoresAir) {

        if (_ignoresAir == ignoresAir)
            return;

        _ignoresAir = true;
        refreshView();
    }

    @Override
    public void translateMultiBlock(IMultiBlockChangePacket packet) {

        if (_chunks.isEmpty())
            return;

        _chunkMatcher.setX(packet.getChunkX());
        _chunkMatcher.setZ(packet.getChunkZ());

        PhantomChunk chunk = _chunks.get(_chunkMatcher);

        for (PacketBlock block : packet) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            int relativeX = getRelativeCoord(x, packet.getChunkX());
            int relativeZ = getRelativeCoord(z, packet.getChunkZ());

            PhantomBlock phantomBlock = chunk.getRelativeBlock(relativeX, y, relativeZ);
            if (phantomBlock == null)
                continue;

            block.setBlock(phantomBlock.getMaterial(), phantomBlock.getData());
        }
    }

    @Override
    public void translateMapChunk(PacketContainer packet) {
        if (_chunks.isEmpty())
            return;

        ChunkData data = PhantomPackets.getNms()
                .getChunkData(packet, _world);

        if (data.getData() == null)
            return;

        if (data.getBlockSize() > data.getData().length)
            return;

        translateChunkData(data);
    }

    @Override
    public void translateMapChunkBulk(PacketContainer packet) {
        if (_chunks.isEmpty())
            return;

        ChunkBulkData data = PhantomPackets.getNms()
                .getChunkBulkData(packet, _world);

        IChunkData[] dataArray = data.getChunkData();

        for (IChunkData chunkData : dataArray) {
            translateChunkData(chunkData);
        }
    }

    @Override
    public IPhantomBlock getBlock(int x, int y, int z) {

        int chunkX = getChunkCoord(x);
        int chunkZ = getChunkCoord(z);

        PhantomChunk chunkContext = _chunks.get(chunkMatcher(chunkX, chunkZ));
        if (chunkContext == null)
            return getBlockFromWorld(x, y, z);

        int relativeX = getRelativeCoord(x, chunkX);
        int relativeZ = getRelativeCoord(z, chunkZ);

        IPhantomBlock block = chunkContext.getRelativeBlock(relativeX, y, relativeZ);
        if (block == null)
            return getBlockFromWorld(x, y, z);

        return block;
    }

    @Nullable
    @Override
    public IPhantomBlock getPhantomBlock(int x, int y, int z) {

        ICoords2Di matcher = chunkMatcher(getChunkCoord(x), getChunkCoord(z));

        PhantomChunk chunk = _chunks.get(matcher);
        if (chunk == null)
            return null;

        int relativeX = getRelativeCoord(x, chunk.getX());
        int relativeZ = getRelativeCoord(z, chunk.getZ());

        return chunk.getRelativeBlock(relativeX, y, relativeZ);
    }

    @Nullable
    @Override
    public IPhantomChunk getPhantomChunk(int x, int z) {
        return _chunks.get(chunkMatcher(x, z));
    }

    @Override
    public ViewPolicy getViewPolicy() {
        return _viewPolicy;
    }

    @Override
    public void setViewPolicy(ViewPolicy viewPolicy) {
        PreCon.notNull(viewPolicy);

        if (_viewPolicy == viewPolicy)
            return;

        _viewPolicy = viewPolicy;

        refreshChunks();
    }

    @Override
    public boolean canSee(Player player) {
        PreCon.notNull(player);

        if (_isDisposed)
            return false;

        switch (_viewPolicy) {
            case WHITELIST:
                return _viewers != null && _viewers.contains(player);
            case BLACKLIST:
                return _viewers == null || !_viewers.contains(player);
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean hasViewer(Player player) {
        PreCon.notNull(player);

        return _viewers != null && _viewers.contains(player);
    }

    @Override
    public boolean addViewer(Player player) {
        PreCon.notNull(player);

        if (_viewers == null)
            _viewers = new PlayerSet(PhantomPackets.getPlugin());

        if (_viewers.add(player)) {
            resendChunks(player);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeViewer(Player player) {
        PreCon.notNull(player);

        if (_viewers != null && _viewers.remove(player)) {
            resendChunks(player);
            return true;
        }

        return false;
    }

    @Override
    public void clearViewers() {
        if (_viewers == null)
            return;

        List<Player> players = getViewers();

        _viewers.clear();

        switch (_viewPolicy) {
            case WHITELIST:
                for (Player player : players) {
                    resendChunks(player);
                }
                break;

            case BLACKLIST:
                refreshChunks();
                break;

            default:
                throw new AssertionError();
        }
    }

    @Override
    public List<Player> getViewers() {
        return _viewers != null
                ? new ArrayList<>(_viewers)
                : new ArrayList<Player>(0);
    }

    @Override
    public void refreshView() {
        if (_viewers == null)
            return;

        //noinspection SynchronizeOnNonFinalField
        synchronized (_viewers) {
            for (Player player : _viewers) {
                resendChunks(player);
            }
        }
    }

    public final void refreshChunks() {
        for (ICoords2Di coords : _chunks.keySet()) {
            _world.refreshChunk(coords.getX(), coords.getZ());
        }
    }

    @Override
    public boolean isDisposed() {
        return _isDisposed;
    }

    @Override
    public void dispose() {
        _manager.remove(_name);

        _isDisposed = true;

        refreshView();

        if (_viewers != null)
            _viewers.clear();
    }

    /*
     * Convert world coordinate to coordinate relative to chunk.
     */
    private int getRelativeCoord(int worldCoord, int chunkCoord) {
        return worldCoord - (chunkCoord * 16);
    }

    /*
     * Convert world coordinate to chunk coordinate.
     */
    private int getChunkCoord(int worldCoord) {
        return (int)Math.floor(worldCoord / 16.0D);
    }

    /*
     * Set and get the chunk matcher instance.
     */
    private ICoords2Di chunkMatcher(int x, int z) {
        _chunkMatcher.setX(x);
        _chunkMatcher.setZ(z);

        return _chunkMatcher;
    }

    /*
     * Translate the contexts data into a chunk data instance.
     */
    private void translateChunkData(IChunkData chunkData) {

        _chunkMatcher.setX(chunkData.getX());
        _chunkMatcher.setZ(chunkData.getZ());

        PhantomChunk chunk = _chunks.get(_chunkMatcher);
        if (chunk == null || chunk.totalBlocks == 0)
            return;

        for (int i=0; i < chunk.blocks.length; i++) {

            IPhantomBlock[] blocks = chunk.blocks[i];
            if (blocks == null)
                continue;

            for (IPhantomBlock block : blocks) {

                if (block == null || block.getMaterial() == Material.AIR && ignoresAir())
                    continue;

                chunkData.setBlock(
                        block.getRelativeX(), block.getY(), block.getRelativeZ(),
                        block.getMaterial(), block.getData());
            }
        }
    }

    /*
     * Create a new phantom block from a world block.
     */
    private IPhantomBlock getBlockFromWorld(int x, int y, int z) {

        Block block = _world.getBlockAt(x, y, z);

        int chunkX = getChunkCoord(x);
        int chunkZ = getChunkCoord(z);

        return new PhantomBlock(chunkX, chunkZ, x, y, z, block.getType(), block.getData());
    }

    /*
     * Resend the contexts block data to a player.
     */
    private void resendChunks(Player player) {

        if (!player.getWorld().equals(getWorld()))
            return;

        Location location = player.getLocation(PLAYER_LOCATION);

        for (PhantomChunk chunk : _chunks.values()) {

            if (!Utils.isChunkNearby(chunk.getX(), chunk.getZ(), location))
                continue;

            IMultiBlockChangeFactory factory = chunk.getMultiBlockPacketFactory();

            PacketContainer packet = canSee(player)
                    ? factory.createPacket(ignoresAir())
                    : factory.createPacket(chunk.coords.getChunk(_world));

            try {
                ProtocolLibrary.getProtocolManager()
                        .sendServerPacket(player, packet);

            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Implementation of {@link IPhantomChunk}.
     */
    private class PhantomChunk implements IPhantomChunk {

        final int x;
        final int z;
        final Coords2Di coords;
        IMultiBlockChangeFactory factory;

        // chunks are divided into 64 sections, each 4 blocks tall,
        // containing 1024 blocks per section.
        final IPhantomBlock[][] blocks = new IPhantomBlock[64][];

        int totalBlocks;
        int totalNonAirBlocks;

        PhantomChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.coords = new Coords2Di(x, z);
        }

        IMultiBlockChangeFactory getMultiBlockPacketFactory() {
            if (factory == null) {
                factory = PhantomPackets.getNms()
                        .getMultiBlockChangeFactory(_world, coords, this);
            }
            return factory;
        }

        @Override
        public IPhantomBlockContext getContext() {
            return PhantomBlocks.this;
        }

        @Override
        public World getWorld() {
            return _world;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public int totalBlocks() {
            return ignoresAir() ? totalNonAirBlocks : totalBlocks;
        }

        @Nullable
        @Override
        public PhantomBlock getRelativeBlock(int relativeX, int y, int relativeZ) {

            int sectionIndex = (int)Math.floor(y / 4.0D);

            IPhantomBlock[] section = blocks[sectionIndex];
            if (section == null)
                return null;

            int index = ((y % 4) * 16 * 16) + ((relativeZ * 16) + relativeX + 1);

            PhantomBlock block = (PhantomBlock)section[index];
            if (block != null && ignoresAir() && block.getMaterial() == Material.AIR)
                return null;

            return block;
        }

        void addBlock(PhantomBlock block) {

            int sectionIndex = (int)Math.floor(block.getY() / 4.0D);

            IPhantomBlock[] section = blocks[sectionIndex];
            if (section == null) {
                section = new IPhantomBlock[1024];
                blocks[sectionIndex] = section;
            }

            int index = ((block.getY() % 4) * 16 * 16) +
                    ((block.getRelativeZ() * 16) + block.getRelativeX() + 1);

            if (section[index] == null) {
                totalBlocks++;

                if (block.getMaterial() != Material.AIR)
                    totalNonAirBlocks++;
            }

            section[index] = block;
            factory = null;
        }

        @Override
        public Iterator<IPhantomBlock> iterator() {
            return new Iterator<IPhantomBlock>() {

                boolean checkedHasNext;
                int sectionIndex = 0;
                int blockIndex = 0;
                IPhantomBlock current;

                @Override
                public boolean hasNext() {

                    checkedHasNext = true;

                    while (sectionIndex < 64) {
                        IPhantomBlock[] section = blocks[sectionIndex];

                        if (section != null) {

                            while (blockIndex < 1024) {

                                current = section[blockIndex];
                                blockIndex++;

                                if (current != null) {

                                    if (ignoresAir() && current.getMaterial() == Material.AIR)
                                        continue;

                                    return true;
                                }
                            }
                        }

                        sectionIndex++;
                        blockIndex = 0;
                    }

                    return false;
                }

                @Override
                public IPhantomBlock next() {

                    if (!checkedHasNext)
                        hasNext();

                    checkedHasNext = false;

                    if (current == null)
                        throw new NoSuchElementException();

                    return current;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Implementation of {@link IPhantomBlock}.
     */
    private class PhantomBlock implements IPhantomBlock {

        final int chunkX, chunkZ, x, y, z, relativeX, relativeZ;
        Material material;
        byte data;

        PhantomBlock(int chunkX, int chunkZ, int x, int y, int z, Material material, int data) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.relativeX = getRelativeCoord(x, chunkX);
            this.relativeZ = getRelativeCoord(z, chunkZ);
            this.material = material;
            this.data = (byte)data;
        }

        @Override
        public IPhantomBlockContext getContext() {
            return PhantomBlocks.this;
        }

        @Override
        public IPhantomChunk getChunk() {
            return _chunks.get(chunkMatcher(chunkX, chunkZ));
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public int getRelativeX() {
            return relativeX;
        }

        @Override
        public int getRelativeZ() {
            return relativeZ;
        }

        @Override
        public Material getMaterial() {
            return material;
        }

        @Override
        public byte getData() {
            return data;
        }

        @Override
        public boolean set(Material material, int data) {

            this.material = material;
            this.data = (byte)data;

            PhantomChunk chunk = chunk();
            chunk.addBlock(this);

            return true;
        }

        @Override
        public ViewPolicy getViewPolicy() {
            return PhantomBlocks.this.getViewPolicy();
        }

        @Override
        public void setViewPolicy(ViewPolicy viewPolicy) {
            PhantomBlocks.this.setViewPolicy(viewPolicy);
        }

        @Override
        public boolean canSee(Player player) {
            return PhantomBlocks.this.canSee(player);
        }

        @Override
        public boolean hasViewer(Player player) {
            return PhantomBlocks.this.hasViewer(player);
        }

        @Override
        public boolean addViewer(Player player) {
            return PhantomBlocks.this.addViewer(player);
        }

        @Override
        public boolean removeViewer(Player player) {
            return PhantomBlocks.this.removeViewer(player);
        }

        @Override
        public void clearViewers() {
            PhantomBlocks.this.clearViewers();
        }

        @Override
        public List<Player> getViewers() {
            return PhantomBlocks.this.getViewers();
        }

        @Override
        public void refreshView() {
            PhantomBlocks.this.refreshView();
        }

        private PhantomChunk chunk() {
            PhantomChunk chunk = _chunks.get(chunkMatcher(chunkX, chunkZ));
            if (chunk == null) {
                chunk = new PhantomChunk(chunkX, chunkZ);
                _chunks.put(chunk.coords, chunk);
            }
            return chunk;
        }
    }
}
