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

package com.jcwhatever.phantom.regions;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.jcwhatever.phantom.IBlockTypeTranslator;
import com.jcwhatever.phantom.IViewable;
import com.jcwhatever.phantom.Msg;
import com.jcwhatever.phantom.PhantomPackets;
import com.jcwhatever.phantom.data.Coordinate;
import com.jcwhatever.phantom.nms.factory.IMultiBlockChangeFactory;
import com.jcwhatever.nucleus.collections.players.PlayerSet;
import com.jcwhatever.nucleus.regions.RestorableRegion;
import com.jcwhatever.nucleus.regions.file.IRegionFileData;
import com.jcwhatever.nucleus.regions.file.IRegionFileFactory;
import com.jcwhatever.nucleus.regions.file.IRegionFileLoader.LoadSpeed;
import com.jcwhatever.nucleus.regions.file.IRegionFileLoader.LoadType;
import com.jcwhatever.nucleus.regions.file.basic.BasicFileFactory;
import com.jcwhatever.nucleus.storage.IDataNode;
import com.jcwhatever.nucleus.utils.CollectionUtils;
import com.jcwhatever.nucleus.utils.MetaKey;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.coords.ChunkBlockInfo;
import com.jcwhatever.nucleus.utils.coords.ChunkCoords;
import com.jcwhatever.nucleus.utils.coords.IChunkCoords;
import com.jcwhatever.nucleus.utils.coords.WorldInfo;
import com.jcwhatever.nucleus.utils.file.IAppliedSerializable;
import com.jcwhatever.nucleus.utils.observer.future.FutureSubscriber;
import com.jcwhatever.nucleus.utils.observer.future.IFuture;
import com.jcwhatever.nucleus.utils.observer.future.IFuture.FutureStatus;
import com.jcwhatever.nucleus.utils.performance.queued.QueueTask;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/*
 * Region that saves to disk and uses saved version to
 * display to specified players only.
 */
public class PhantomRegion extends RestorableRegion implements IViewable {

    public static final MetaKey<PhantomRegion> REGION_KEY = new MetaKey<>(PhantomRegion.class);
    private final ProtocolManager _protocolManager = ProtocolLibrary.getProtocolManager();

    private final IBlockTypeTranslator _blockTranslator;
    private final BasicFileFactory _fileFactory = new BasicFileFactory("disguise");

    private Map<Coordinate, ChunkBlockInfo> _blocks;
    private Multimap<IChunkCoords, ChunkBlockInfo> _chunkBlocks =
            MultimapBuilder.hashKeys(10).hashSetValues().build();

    private Map<IChunkCoords, IMultiBlockChangeFactory> _chunkBlockFactories = new HashMap<>(10);

    private Set<Player> _viewers;
    private ViewPolicy _viewPolicy = ViewPolicy.WHITELIST;

    private boolean _ignoreAir;
    private boolean _isLoading;

    /**
     * Constructor.
     *
     * @param name      The name of the region.
     * @param dataNode  The regions data node.
     */
    public PhantomRegion(String name, IDataNode dataNode) {
        super(PhantomPackets.getPlugin(), name, dataNode);

        getMeta().set(REGION_KEY, this);

        if (isDefined()) {
            try {
                loadDisguise();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // initialize block type translator
        _blockTranslator = new IBlockTypeTranslator() {
            @Override
            public ChunkBlockInfo translate(WorldInfo world,
                                            int x, int y, int z,
                                            Material material, byte meta) {
                if (!contains(x, y, z))
                    return null;

                ChunkBlockInfo blockInfo = _blocks.get(new Coordinate(x, y, z));
                return _ignoreAir && blockInfo.getMaterial() == Material.AIR
                       ? null
                       : blockInfo;

            }
        };
    }

    /**
     * Get the regions block packet translator.
     */
    public IBlockTypeTranslator getBlockPacketTranslator() {
        return _blockTranslator;
    }

    /**
     * Get block info for a specific chunk the region intersects with.
     *
     * @param chunkCoords  The chunk coordinates.
     */
    public List<ChunkBlockInfo> getChunkBlocks(IChunkCoords chunkCoords) {
        return CollectionUtils.unmodifiableList(_chunkBlocks.get(chunkCoords));
    }

    /**
     * Determine if the region is loading.
     */
    public boolean isLoading() {
        return _isLoading;
    }

    /**
     * Determine if the disguise ignores saved air blocks.
     */
    public boolean ignoresAir() {
        return _ignoreAir;
    }

    /**
     * Set if the disguise ignores saved air blocks.
     *
     * @param ignore  True to ignore air blocks.
     */
    public void setIgnoresAir(boolean ignore) {
        _ignoreAir = ignore;

        IDataNode dataNode = getDataNode();
        if (dataNode != null) {
            dataNode.set("ignore-air", ignore);
            dataNode.save();
        }
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

        boolean hasViewer = hasViewer(player);
        return (_viewPolicy == ViewPolicy.BLACKLIST && !hasViewer) ||
                (_viewPolicy == ViewPolicy.WHITELIST && hasViewer);
    }

    @Override
    public boolean hasViewer(Player player) {
        PreCon.notNull(player);

        return _viewers != null &&
                _viewers.contains(player);
    }

    @Override
    public boolean addViewer(Player player) {
        PreCon.notNull(player);

        if (!isDefined() || !initViewers()) {
            return false;
        }

        synchronized (getSync()) {
            _viewers.add(player);
        }

        resendChunks(player);

        return true;
    }

    @Override
    public boolean removeViewer(Player player) {
        PreCon.notNull(player);

        if (_viewers == null)
            return false;

        synchronized (getSync()) {
            if (!_viewers.remove(player)) {
                return false;
            }
        }

        resendChunks(player);

        return true;
    }

    @Override
    public void clearViewers() {
        if (_viewers == null || _viewers.isEmpty())
            return;

        List<Player> players = getViewers();

        _viewers.clear();

        switch (_viewPolicy) {
            case WHITELIST:
                for (Player p : players) {
                    resendChunks(p);
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
        return new ArrayList<>(_viewers);
    }

    @Override
    public IFuture saveData() throws IOException {
        return super.saveData().onSuccess(new FutureSubscriber() {
            @Override
            public void on(FutureStatus status, @Nullable String message) {
                try {
                    loadDisguise();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public IRegionFileFactory getFileFactory() {
        return _fileFactory;
    }

    @Override
    public IDataNode getDataNode() {
        return super.getDataNode();
    }

    @Override
    protected void onCoordsChanged(Location p1, Location p2) {

        if (isDefined()) {
            try {
                saveDisguise();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void loadSettings(IDataNode dataNode) {
        super.loadSettings(dataNode);

        _ignoreAir = dataNode.getBoolean("ignore-air", _ignoreAir);
    }

    @Override
    protected void onDispose() {

        if (_viewers != null) {
            _viewers.clear();
        }
    }

    /*
     * Save the disguise
     */
    private void saveDisguise() throws IOException {
        saveData();
    }

    /*
     * Load the regions disguise.
     */
    private void loadDisguise() throws IOException {

        if (!canRestore())
            return;

        _isLoading = true;
        _blocks = new HashMap<>((int)getVolume());

        getFileFormat().getLoader(this, getFileFactory()).load(
                LoadType.ALL_BLOCKS, LoadSpeed.FAST, new RegionData())
                .onSuccess(new FutureSubscriber() {
                    @Override
                    public void on(FutureStatus status, @Nullable String message) {

                    }
                })
                .onError(new FutureSubscriber() {
                    @Override
                    public void on(FutureStatus status, @Nullable String message) {
                        Msg.warning("Failed to load chunk data for phantom region named '{0}' because:",
                                getName(), message);
                    }
                });
    }

    private void resendChunks(Player p) {
        if (!p.getWorld().equals(getWorld()))
            return;

        for (IChunkCoords coord : getChunkCoords()) {

            IMultiBlockChangeFactory factory = _chunkBlockFactories.get(coord);
            if (factory == null)
                continue;

            PacketContainer packet;
            packet = canSee(p)
                    ? factory.createPacket(_ignoreAir)
                    : factory.createPacket(coord.getChunk());

            try {
                _protocolManager.sendServerPacket(p, packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean initViewers() {
        if (_viewers != null)
            return true;

        // make sure region is saved
        if (_blocks == null) {
            try {
                loadDisguise();
            } catch (IOException e) {
                return false;
            }
            if (_blocks == null) {
                return false;
            }
        }

        _viewers = new PlayerSet(getPlugin());
        return true;
    }

    private class RegionData implements IRegionFileData {

        private ChunkCoords currentChunk;
        private LinkedList<ChunkBlockInfo> blocks = new LinkedList<>();

        @Override
        public void addBlock(int x, int y, int z, Material material, int data, int light, int skylight) {

            if (currentChunk == null) {

                int chunkX = (int) Math.floor(x / 16.0D);
                int chunkZ = (int) Math.floor(z / 16.0D);
                currentChunk = new ChunkCoords(getWorld(), chunkX, chunkZ);
            }

            ChunkBlockInfo info = new ChunkBlockInfo(
                    x - (currentChunk.getX() * 16), y, z - (currentChunk.getZ() * 16), material, data, light, skylight);

            blocks.add(info);
        }

        @Override
        public void addSerializable(IAppliedSerializable blockEntity) {
            // do nothing
        }

        @Override
        @Nullable
        public QueueTask commit() {

            IMultiBlockChangeFactory factory = PhantomPackets.getNms()
                    .getMultiBlockChangeFactory(currentChunk, blocks);

            _chunkBlockFactories.put(currentChunk, factory);

            int cwX = currentChunk.getX() * 16;
            int cWZ = currentChunk.getZ() * 16;

            while (!blocks.isEmpty()) {
                ChunkBlockInfo info = blocks.remove();

                Coordinate coord = new Coordinate(cwX + info.getX(), info.getY(), cWZ + info.getZ());

                _blocks.put(coord, info);
                _chunkBlocks.put(currentChunk, info);
            }

            currentChunk = null;

            return null;
        }
    }
}
