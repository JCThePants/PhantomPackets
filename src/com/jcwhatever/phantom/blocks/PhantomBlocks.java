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
import com.jcwhatever.nucleus.managed.scheduler.Scheduler;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.ThreadSingletons;
import com.jcwhatever.nucleus.utils.coords.Coords2Di;
import com.jcwhatever.nucleus.utils.coords.ICoords2Di;
import com.jcwhatever.nucleus.utils.coords.MutableCoords2Di;
import com.jcwhatever.nucleus.utils.performance.pool.IPoolElementFactory;
import com.jcwhatever.nucleus.utils.performance.pool.SimplePool;
import com.jcwhatever.nucleus.utils.player.PlayerUtils;
import com.jcwhatever.phantom.*;
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

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Simple phantom block context.
 */
public class PhantomBlocks implements IPhantomBlockContext {

    private static final ThreadSingletons<Location> PLAYER_LOCATIONS = new ThreadSingletons<>(
            new ThreadSingletons.ISingletonFactory<Location>() {
                @Override
                public Location create(Thread thread) {
                    return new Location(null, 0, 0, 0);
                }
            });

    private static final ThreadSingletons<MutableCoords2Di> COORDS = new ThreadSingletons<>(
            new ThreadSingletons.ISingletonFactory<MutableCoords2Di>() {
                @Override
                public MutableCoords2Di create(Thread thread) {
                    return new MutableCoords2Di();
                }
            });

    private final IBlockContextManager _manager;
    private final World _world;
    private final String _name;
    private final String _searchName;
    private final Map<ICoords2Di, PhantomChunk> _chunks = new HashMap<>(25);

    private Set<Player> _viewers;
    private boolean _ignoresAir;
    private ViewPolicy _viewPolicy = ViewPolicy.WHITELIST;
    private boolean _isDisposed;

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
    public void translateMultiBlock(Player player, IMultiBlockChangePacket packet) {

        if (_chunks.isEmpty())
            return;

        PhantomChunk chunk = _chunks.get(matcher(packet.getChunkX(), packet.getChunkZ()));

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
    public void translateMapChunk(Player player, PacketContainer packet) {
        if (_chunks.isEmpty())
            return;

        ChunkData data = PhantomPackets.getNms()
                .getChunkData(packet, _world);

        if (data.getData() == null)
            return;

        if (data.getBlockSize() > data.getData().length)
            return;

        if (data.getSectionMask() == 0) {

            PhantomChunk chunk = _chunks.get(matcher(data.getX(), data.getZ()));
            if (chunk != null) {

                ChunkResender resender = CHUNK_SENDER_POOL.retrieve();
                assert resender != null;

                resender.init(this, player);
                resender.toResend.add(chunk);

                Scheduler.runTaskLater(PhantomPackets.getPlugin(), 5, resender);
            }
        }
        else {
            translateChunkData(data);
        }
    }

    @Override
    public void translateMapChunkBulk(Player player, PacketContainer packet) {
        if (_chunks.isEmpty())
            return;

        ChunkBulkData bulkData = PhantomPackets.getNms()
                .getChunkBulkData(packet, _world);

        IChunkData[] dataArray = bulkData.getChunkData();

        for (IChunkData data : dataArray) {

            if (data.getSectionMask() == 0) {

                PhantomChunk chunk = _chunks.get(matcher(data.getX(), data.getZ()));
                if (chunk != null) {

                    ChunkResender resender = CHUNK_SENDER_POOL.retrieve();
                    assert resender != null;

                    resender.init(this, player);
                    resender.toResend.add(chunk);

                    Scheduler.runTaskLater(PhantomPackets.getPlugin(), 5, resender);
                }
            }
            else {
                translateChunkData(data);
            }
        }
    }

    private static final SimplePool<ChunkResender> CHUNK_SENDER_POOL = new SimplePool<ChunkResender>(30,
            new IPoolElementFactory<ChunkResender>() {
                @Override
                public ChunkResender create() {
                    return new ChunkResender();
                }
            });

    private static class ChunkResender implements Runnable {

        PhantomBlocks blocks;
        Player player;
        List<PhantomChunk> toResend = new ArrayList<>(10);

        void init(PhantomBlocks blocks, Player player) {
            this.blocks = blocks;
            this.player = player;
        }

        @Override
        public void run() {
            for (PhantomChunk chunk : toResend) {
                blocks.resendChunk(player, chunk);
            }
            toResend.clear();
            player = null;
            blocks = null;
            CHUNK_SENDER_POOL.recycle(this);
        }
    }

    @Override
    public IPhantomBlock getBlock(int x, int y, int z) {

        int chunkX = getChunkCoord(x);
        int chunkZ = getChunkCoord(z);

        PhantomChunk chunkContext = _chunks.get(matcher(chunkX, chunkZ));
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

        PhantomChunk chunk = _chunks.get(matcher(getChunkCoord(x), getChunkCoord(z)));
        if (chunk == null)
            return null;

        int relativeX = getRelativeCoord(x, chunk.getX());
        int relativeZ = getRelativeCoord(z, chunk.getZ());

        return chunk.getRelativeBlock(relativeX, y, relativeZ);
    }

    @Nullable
    @Override
    public IPhantomChunk getPhantomChunk(int x, int z) {
        return _chunks.get(matcher(x, z));
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
    private ICoords2Di matcher(int x, int z) {
        MutableCoords2Di coords = COORDS.get();
        coords.setX(x);
        coords.setZ(z);
        return coords;
    }

    /*
     * Translate the contexts data into a chunk data instance.
     */
    private void translateChunkData(IChunkData chunkData) {

        PhantomChunk chunk = _chunks.get(matcher(chunkData.getX(), chunkData.getZ()));
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
        return new PhantomBlock(x, y, z, block.getType(), block.getData());
    }

    /*
     * Resend the contexts block data to a player.
     */
    private void resendChunks(Player player) {

        if (!player.getWorld().equals(getWorld()))
            return;

        for (PhantomChunk chunk : _chunks.values()) {

            resendChunk(player, chunk);
        }
    }

    private void resendChunk(final Player player, final PhantomChunk chunk) {

        final Location location = player.getLocation(PLAYER_LOCATIONS.get());

        if (!Utils.isChunkNearby(chunk.getX(), chunk.getZ(), location))
            return;

        IMultiBlockChangeFactory factory = chunk.getMultiBlockPacketFactory();

        PacketContainer packet = canSee(player)
                ? factory.createPacket(ignoresAir())
                : factory.createPacket(chunk.coords.getChunk(_world));

        if (PlayerUtils.getWorldSessionTime(player) < 1500 ||
                !location.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) {

            Scheduler.runTaskLater(PhantomPackets.getPlugin(), 31, new Runnable() {
                @Override
                public void run() {
                    if (location.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) {
                        resendChunk(player, chunk);
                    }
                }
            });
        }
        else {
            sendPacket(player, packet);
        }
    }

    private void sendPacket(Player player, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager()
                    .sendServerPacket(player, packet);

        } catch (InvocationTargetException e) {
            e.printStackTrace();
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

            if (block != null && (block.relativeX != relativeX || block.y != y || block.relativeZ != relativeZ))
                throw new IllegalStateException("Retrieved block mismatch.");

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

        PhantomBlock(int x, int y, int z, Material material, int data) {
            this.chunkX = getChunkCoord(x);
            this.chunkZ = getChunkCoord(z);
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
            return _chunks.get(matcher(chunkX, chunkZ));
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
            PhantomChunk chunk = _chunks.get(matcher(chunkX, chunkZ));
            if (chunk == null) {
                chunk = new PhantomChunk(chunkX, chunkZ);
                _chunks.put(chunk.coords, chunk);
            }
            return chunk;
        }
    }
}
