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

package com.jcwhatever.bukkit.phantom.regions;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.jcwhatever.nucleus.internal.Msg;
import com.jcwhatever.nucleus.mixins.IViewable;
import com.jcwhatever.nucleus.utils.performance.queued.QueueProject;
import com.jcwhatever.nucleus.utils.performance.queued.QueueResult.FailHandler;
import com.jcwhatever.nucleus.utils.performance.queued.QueueResult.Future;
import com.jcwhatever.nucleus.collections.players.PlayerSet;
import com.jcwhatever.nucleus.regions.RegionChunkFileLoader;
import com.jcwhatever.nucleus.regions.RegionChunkFileLoader.LoadType;
import com.jcwhatever.nucleus.regions.RestorableRegion;
import com.jcwhatever.nucleus.regions.data.ChunkBlockInfo;
import com.jcwhatever.nucleus.regions.data.ChunkInfo;
import com.jcwhatever.nucleus.regions.data.IChunkInfo;
import com.jcwhatever.nucleus.regions.data.WorldInfo;
import com.jcwhatever.nucleus.storage.IDataNode;
import com.jcwhatever.nucleus.utils.CollectionUtils;
import com.jcwhatever.nucleus.utils.MetaKey;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.data.Coordinate;
import com.jcwhatever.bukkit.phantom.packets.IMultiBlockChangeFactory;
import com.jcwhatever.bukkit.phantom.translators.BlockTypeTranslator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

    private final BlockTypeTranslator _blockTranslator;

    private Map<Coordinate, ChunkBlockInfo> _blocks;
    private Multimap<IChunkInfo, ChunkBlockInfo> _chunkBlocks =
            MultimapBuilder.hashKeys(10).hashSetValues().build();

    private Map<ChunkInfo, IMultiBlockChangeFactory> _chunkBlockFactories = new HashMap<>(10);

    private Set<Player> _viewers;
    private ViewPolicy _viewPolicy = ViewPolicy.WHITELIST;

    private boolean _ignoreAir;
    private boolean _isLoading;

    /**
     * Constructor.
     *
     * @param plugin    The owning plugin.
     * @param name      The name of the region.
     * @param dataNode  The regions data node.
     */
    public PhantomRegion(Plugin plugin, String name, IDataNode dataNode) {
        super(plugin, name, dataNode);

        setMeta(REGION_KEY, this);

        if (isDefined()) {
            try {
                loadDisguise();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // initialize block type translator
        _blockTranslator = new BlockTypeTranslator() {
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

    public BlockTypeTranslator getBlockPacketTranslator() {
        return _blockTranslator;
    }

    public List<ChunkBlockInfo> getChunkBlocks(IChunkInfo chunkInfo) {
        return CollectionUtils.unmodifiableList(_chunkBlocks.get(chunkInfo));
    }

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
            dataNode.saveAsync(null);
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

        synchronized (_sync) {
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

        synchronized (_sync) {
            if (!_viewers.remove(player)) {
                return false;
            }
        }

        resendChunks(player);

        return true;
    }

    /**
     * Hide region from all players.
     */
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
    public Future saveData() throws IOException {
        return super.saveData().onComplete(new Runnable() {
            @Override
            public void run() {
                try {
                    loadDisguise();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected String getFilePrefix() {
        return "disguise." + getName();
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

        List<ChunkInfo> chunks = getChunks();

        _blocks = new HashMap<>((int)getVolume());


        QueueProject loadProject = new QueueProject(PhantomPackets.getPlugin());

        loadProject.getResult().onEnd(new Runnable() {
            @Override
            public void run() {
                _isLoading = false;
            }
        });

        for (final ChunkInfo chunk : chunks) {

            // create chunk loader
            final RegionChunkFileLoader loader = new RegionChunkFileLoader(this, chunk);

            // add load task to chunk project
            loader.loadInProject(getChunkFile(chunk, "", false), loadProject, LoadType.ALL_BLOCKS)
                    .onComplete(new Runnable() {

                        @Override
                        public void run() {

                            LinkedList<ChunkBlockInfo> blockInfos = loader.getBlockInfo();

                             IMultiBlockChangeFactory factory = PhantomPackets.getNms()
                                     .getMultiBlockChangeFactory(chunk, blockInfos);

                             _chunkBlockFactories.put(chunk, factory);

                            while (!blockInfos.isEmpty()) {
                                ChunkBlockInfo info = blockInfos.remove();

                                int x = (chunk.getX() * 16) + info.getChunkBlockX();
                                int z = (chunk.getZ() * 16) + info.getChunkBlockZ();

                                Coordinate coord = new Coordinate(x, info.getY(), z);

                                _blocks.put(coord, info);
                                _chunkBlocks.put(chunk, info);
                            }

                        }
                    })
                    .onFail(new FailHandler() {
                        @Override
                        public void run(@Nullable String reason) {
                            Msg.warning("Failed to load chunk data for phantom region named '{0}' because:", getName(), reason);
                        }
                    });
        }

        loadProject.run();
    }

    private void resendChunks(Player p) {
        if (!p.getWorld().equals(getWorld()))
            return;

        for (ChunkInfo chunk : getChunks()) {

            IMultiBlockChangeFactory factory = _chunkBlockFactories.get(chunk);
            if (factory == null)
                continue;

            PacketContainer packet;
            packet = canSee(p)
                    ? factory.createPacket(_ignoreAir)
                    : factory.createPacket(chunk.getChunk());

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
}
