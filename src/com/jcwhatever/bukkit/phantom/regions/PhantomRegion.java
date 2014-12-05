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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.jcwhatever.bukkit.generic.messaging.Messenger;
import com.jcwhatever.bukkit.generic.mixins.IViewable;
import com.jcwhatever.bukkit.generic.performance.queued.QueueResult.Future;
import com.jcwhatever.bukkit.generic.player.collections.PlayerSet;
import com.jcwhatever.bukkit.generic.regions.RegionChunkFileLoader;
import com.jcwhatever.bukkit.generic.regions.RegionChunkFileLoader.LoadFileCallback;
import com.jcwhatever.bukkit.generic.regions.RegionChunkFileLoader.LoadType;
import com.jcwhatever.bukkit.generic.regions.RestorableRegion;
import com.jcwhatever.bukkit.generic.regions.data.ChunkBlockInfo;
import com.jcwhatever.bukkit.generic.regions.data.ChunkInfo;
import com.jcwhatever.bukkit.generic.regions.data.IChunkInfo;
import com.jcwhatever.bukkit.generic.regions.data.WorldInfo;
import com.jcwhatever.bukkit.generic.storage.IDataNode;
import com.jcwhatever.bukkit.generic.utils.EntryValidator;
import com.jcwhatever.bukkit.generic.utils.PreCon;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.Utils;
import com.jcwhatever.bukkit.phantom.data.Coordinate;
import com.jcwhatever.bukkit.phantom.packets.BlockChangePacket;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangeFactory;
import com.jcwhatever.bukkit.phantom.packets.MultiBlockChangePacket;
import com.jcwhatever.bukkit.phantom.translators.BlockPacketTranslator;
import com.jcwhatever.bukkit.phantom.translators.BlockTypeTranslator;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import java.util.WeakHashMap;

/*
 * Region that saves to disk and uses saved version to
 * display to specified players only.
 */
public class PhantomRegion extends RestorableRegion implements IViewable {

    private static Map<Object, Void> _sentPackets = new WeakHashMap<>(10);

    private final ProtocolManager _protocolManager = ProtocolLibrary.getProtocolManager();
    private final BlockPacketTranslator _packetTranslator;
    private final BlockTypeTranslator _blockTranslator;
    private final EntryValidator<IChunkInfo> _chunkValidator;

    private PacketAdapter _packetListener;
    private AsyncListenerHandler _asyncListener;
    private boolean _isAsyncListenerStarted;

    private Map<Coordinate, ChunkBlockInfo> _blocks;
    private Map<ChunkInfo, MultiBlockChangeFactory> _chunkBlocks = new HashMap<>(10);

    private Set<Player> _viewers;
    private ViewPolicy _viewMode = ViewPolicy.WHITELIST;

    private boolean _ignoreAir;

    /**
     * Constructor.
     *
     * @param plugin    The owning plugin.
     * @param name      The name of the region.
     * @param dataNode  The regions data node.
     */
    public PhantomRegion(Plugin plugin, String name, IDataNode dataNode) {
        super(plugin, name, dataNode);

        if (isDefined()) {
            try {
                loadDisguise();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        _packetTranslator = new BlockPacketTranslator();

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

        // initialize chunk validator
        _chunkValidator = new EntryValidator<IChunkInfo>() {
            @Override
            public boolean isValid(IChunkInfo entry) {
                return intersects(entry.getX(), entry.getZ());
            }
        };
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
        return _viewMode;
    }

    @Override
    public void setViewMode(ViewPolicy viewMode) {
        PreCon.notNull(viewMode);

        if (_viewMode == viewMode)
            return;

        _viewMode = viewMode;

        refreshChunks();
    }

    @Override
    public boolean canSee(Player player) {
        PreCon.notNull(player);

        boolean hasViewer = hasViewer(player);
        return (_viewMode == ViewPolicy.BLACKLIST && !hasViewer) ||
                (_viewMode == ViewPolicy.WHITELIST && hasViewer);
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

        if (_viewers.size() == 1) {
            loadPacketListener();
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

        switch (_viewMode) {
            case WHITELIST:
                for (Player p : players) {
                    resendChunks(p);
                }
                unloadPacketListener();
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

        unloadPacketListener();

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

        unloadPacketListener();

        if (_asyncListener != null) {
            _protocolManager.getAsynchronousManager()
                    .unregisterAsyncHandler(_asyncListener);

            if (!_asyncListener.isCancelled()) {
                _asyncListener.cancel();
            }
        }
    }

    /*
     * Load and register the packet listener.
     */
    private void loadPacketListener() {

        if (_viewers == null || _viewers.isEmpty())
            return;

        if (_packetListener == null) {
            _packetListener = getPacketListener();
            _protocolManager.addPacketListener(_packetListener);
        }

        if (_asyncListener == null) {
            _asyncListener = _protocolManager.getAsynchronousManager()
                    .registerAsyncHandler(getAsyncPacketListener());
        }

        if (!_isAsyncListenerStarted) {
            _asyncListener.start();
            _isAsyncListenerStarted = true;
        }

    }

    /*
     * Unload and de-register the packet listener.
     */
    private void unloadPacketListener() {
        if (_packetListener != null) {
            _protocolManager.removePacketListener(_packetListener);
            _packetListener = null;
        }

        if (_asyncListener != null) {
            _asyncListener.stop();
            _isAsyncListenerStarted = false;
            //_protocolManager.getAsynchronousManager().unregisterAsyncHandler(_asyncListener);
            //_asyncListener = null;
        }
    }

    /*
     * Get the Async packet listener.
     */
    private PacketAdapter getAsyncPacketListener() {

        return new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST,
                Server.MAP_CHUNK,
                Server.MAP_CHUNK_BULK,
                Server.UPDATE_SIGN,
                Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent event) {

                PacketType type = event.getPacketType();

                if (type == Server.UPDATE_SIGN || type == Server.TILE_ENTITY_DATA) {
                    // listening to update_sign and tile_entity_data to keep them
                    // from being sent out of order.
                    return;
                }

                WorldInfo worldInfo;

                synchronized (_sync) {

                    if (!canSee(event.getPlayer()))
                        return;

                    // don't process packets sent by a phantom region
                    if (_sentPackets.containsKey(event.getPacket().getHandle()))
                        return;

                    World world = event.getPlayer().getWorld();

                    if (!world.equals(getWorld()))
                        return;

                    worldInfo = new WorldInfo(world);
                }

                PacketContainer packet = event.getPacket();

                if (type == Server.MAP_CHUNK) {

                    _packetTranslator.translateMapChunk(packet, worldInfo, _blockTranslator, _chunkValidator);
                }
                else if (type == Server.MAP_CHUNK_BULK) {

                    _packetTranslator.translateMapChunkBulk(packet, worldInfo, _blockTranslator, _chunkValidator);
                }
            }
        };
    }

    private PacketAdapter getPacketListener() {
        return new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST,
                Server.MAP_CHUNK,
                Server.MAP_CHUNK_BULK,
                Server.BLOCK_CHANGE,
                Server.MULTI_BLOCK_CHANGE) {

            @Override
            public void onPacketSending(PacketEvent event) {

                PacketType type = event.getPacketType();

                WorldInfo worldInfo;

                synchronized (_sync) {

                    if (!canSee(event.getPlayer()))
                        return;

                    // don't process packets sent by a phantom region
                    if (_sentPackets.containsKey(event.getPacket().getHandle()))
                        return;
                }

                World world = event.getPlayer().getWorld();

                if (!world.equals(getWorld()))
                    return;

                worldInfo = new WorldInfo(world);


                PacketContainer packet = event.getPacket();

                if (type == Server.BLOCK_CHANGE) {

                    BlockChangePacket wrapper = new BlockChangePacket(packet);
                    if (_packetTranslator.translateBlockChange(wrapper, worldInfo, _blockTranslator)) {

                        wrapper.saveChanges();
                        event.setPacket(wrapper.clonePacket().getPacket());
                    }
                }
                else if (type == Server.MULTI_BLOCK_CHANGE) {

                    MultiBlockChangePacket wrapper = new MultiBlockChangePacket(packet);
                    if (_packetTranslator.translateMultiBlockChange(
                            wrapper, worldInfo, _blockTranslator, _chunkValidator)) {

                        wrapper.saveChanges();

                        event.setPacket(wrapper.clonePacket().getPacket());
                    }
                }
                else if (type == Server.MAP_CHUNK) {

                    StructureModifier<Integer> integers = packet.getSpecificModifier(int.class);
                    int chunkX = integers.read(0);
                    int chunkZ = integers.read(1);

                    if (intersects(chunkX, chunkZ)) {
                        Location location = event.getPlayer().getLocation();

                        if (Utils.isChunkNearby(chunkX, chunkZ, location)) {
                            event.getAsyncMarker().setNewSendingIndex(0);
                        }
                    }

                }
                else if (type == Server.MAP_CHUNK_BULK) {

                    StructureModifier<int[]> integerArrays = packet.getSpecificModifier(int[].class);
                    int[] chunkXArray = integerArrays.read(0);
                    int[] chunkZArray = integerArrays.read(1);
                    Location location = event.getPlayer().getLocation();

                    for (int i = 0; i < chunkXArray.length; i++) {

                        int chunkX = chunkXArray[i];
                        int chunkZ = chunkZArray[i];

                        if (!intersects(chunkX, chunkZ))
                            continue;

                        if (Utils.isChunkNearby(chunkX, chunkZ, location)) {
                            event.getAsyncMarker().setNewSendingIndex(0);
                            break;
                        }
                    }
                }

            }
        };
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

        // make sure packet listener unloaded to prevent
        // async issues while modifying _blocks field.
        unloadPacketListener();

        List<Chunk> chunks = getChunks();

        _blocks = new HashMap<>((int)getVolume());

        for (final Chunk chunk : chunks) {

            // create chunk loader
            final RegionChunkFileLoader loader = new RegionChunkFileLoader(this, chunk);

            // add load task to chunk project
            loader.load(getChunkFile(chunk, "", false), LoadType.ALL_BLOCKS, new LoadFileCallback() {
                @Override
                public void onFinish(boolean isLoadSuccess) {

                    if (isLoadSuccess) {

                        ChunkInfo chunkInfo = new ChunkInfo(chunk);

                        LinkedList<ChunkBlockInfo> blockInfos = loader.getBlockInfo();

                        MultiBlockChangeFactory factory = new MultiBlockChangeFactory(chunkInfo, blockInfos);

                        _chunkBlocks.put(chunkInfo, factory);

                        while (!blockInfos.isEmpty()) {
                            ChunkBlockInfo info = blockInfos.remove();

                            int x = (chunk.getX() * 16) + info.getChunkBlockX();
                            int z = (chunk.getZ() * 16) + info.getChunkBlockZ();

                            Coordinate coord = new Coordinate(x, info.getY(), z);

                            _blocks.put(coord, info);
                        }

                        loadPacketListener();
                    }
                    else {
                        Messenger.warning(PhantomPackets.getPlugin(),
                                "Failed to load chunk data for phantom region named '{0}'.", getName());
                    }
                }
            });
        }
    }


    private void resendChunks(Player p) {
        if (!p.getWorld().equals(getWorld()))
            return;

        Messenger.warning(PhantomPackets.getPlugin(), "Resending chunks to player {0}.", p.getName());

        for (Chunk chunk : getChunks()) {

            if (canSee(p)) {
                MultiBlockChangeFactory factory = _chunkBlocks.get(new ChunkInfo(chunk));
                if (factory == null)
                    continue;

                PacketContainer packet = factory.createPacket();

               // _sentPackets.put(packet.getHandle(), null);

                try {
                    _protocolManager.sendServerPacket(p, packet);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            else {
                Utils.refreshChunk(p, chunk.getX(), chunk.getZ());
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
