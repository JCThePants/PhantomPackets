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

package com.jcwhatever.phantom.blocks.regions;

import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.nucleus.regions.RestorableRegion;
import com.jcwhatever.nucleus.regions.file.IRegionFileData;
import com.jcwhatever.nucleus.regions.file.IRegionFileFactory;
import com.jcwhatever.nucleus.regions.file.IRegionFileLoader.LoadSpeed;
import com.jcwhatever.nucleus.regions.file.IRegionFileLoader.LoadType;
import com.jcwhatever.nucleus.regions.file.basic.BasicFileFactory;
import com.jcwhatever.nucleus.storage.IDataNode;
import com.jcwhatever.nucleus.utils.MetaKey;
import com.jcwhatever.nucleus.utils.coords.ChunkBlockInfo;
import com.jcwhatever.nucleus.utils.coords.ChunkCoords;
import com.jcwhatever.nucleus.utils.file.IAppliedSerializable;
import com.jcwhatever.nucleus.utils.observer.future.FutureSubscriber;
import com.jcwhatever.nucleus.utils.observer.future.IFuture;
import com.jcwhatever.nucleus.utils.observer.future.IFuture.FutureStatus;
import com.jcwhatever.nucleus.utils.performance.queued.QueueTask;
import com.jcwhatever.phantom.IPhantomBlock;
import com.jcwhatever.phantom.IPhantomChunk;
import com.jcwhatever.phantom.IPhantomBlockContext;
import com.jcwhatever.phantom.IBlockContextManager;
import com.jcwhatever.phantom.Msg;
import com.jcwhatever.phantom.PhantomPackets;
import com.jcwhatever.phantom.blocks.PhantomBlocks;
import com.jcwhatever.phantom.nms.packets.IMultiBlockChangePacket;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/*
 * Region that saves to disk and uses saved version to
 * display to specified players only.
 */
public class PhantomRegion extends RestorableRegion implements IPhantomBlockContext {

    public static final MetaKey<PhantomRegion> REGION_KEY = new MetaKey<>(PhantomRegion.class);

    private final IBlockContextManager _manager;
    private final BasicFileFactory _fileFactory = new BasicFileFactory("disguise");
    private PhantomBlocks _blocks;

    private boolean _isLoading;

    /**
     * Constructor.
     *
     * @param name      The name of the region.
     * @param dataNode  The regions data node.
     */
    public PhantomRegion(IBlockContextManager manager, String name, IDataNode dataNode) {
        super(PhantomPackets.getPlugin(), name, dataNode);

        _manager = manager;

        getMeta().set(REGION_KEY, this);

        if (isDefined()) {
            try {
                loadDisguise();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBlockContextManager getManager() {
        return _manager;
    }

    @Override
    public void translateMultiBlock(IMultiBlockChangePacket packet) {
        _blocks.translateMultiBlock(packet);
    }

    @Override
    public void translateMapChunk(PacketContainer packet) {
        _blocks.translateMapChunk(packet);
    }

    @Override
    public void translateMapChunkBulk(PacketContainer packet) {
        _blocks.translateMapChunkBulk(packet);
    }

    @Override
    public IPhantomBlock getBlock(int x, int y, int z) {
        return _blocks.getBlock(x, y, z);
    }

    @Nullable
    @Override
    public IPhantomBlock getPhantomBlock(int x, int y, int z) {
        return _blocks.getPhantomBlock(x, y, z);
    }

    @Nullable
    @Override
    public IPhantomChunk getPhantomChunk(int x, int z) {
        return _blocks.getPhantomChunk(x, z);
    }

    /**
     * Determine if the region is loading.
     */
    public boolean isLoading() {
        return _isLoading;
    }

    @Override
    public boolean ignoresAir() {
        return _blocks.ignoresAir();
    }

    @Override
    public void setIgnoresAir(boolean ignore) {

        _blocks.setIgnoresAir(ignore);

        IDataNode dataNode = getDataNode();
        if (dataNode != null) {
            dataNode.set("ignore-air", ignore);
            dataNode.save();
        }
    }

    @Override
    public ViewPolicy getViewPolicy() {
        return _blocks.getViewPolicy();
    }

    @Override
    public void setViewPolicy(ViewPolicy viewPolicy) {
        _blocks.setViewPolicy(viewPolicy);
    }

    @Override
    public boolean canSee(Player player) {
        return _blocks.canSee(player);
    }

    @Override
    public boolean hasViewer(Player player) {
        return _blocks.hasViewer(player);
    }

    @Override
    public boolean addViewer(Player player) {
        return _blocks.addViewer(player);
    }

    @Override
    public boolean removeViewer(Player player) {
        return _blocks.removeViewer(player);
    }

    @Override
    public void clearViewers() {
        _blocks.clearViewers();
    }

    @Override
    public List<Player> getViewers() {
        return _blocks.getViewers();
    }

    @Override
    public void refreshView() {
        _blocks.refreshView();
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
    protected void onDispose() {
        _blocks.dispose();
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
        _blocks = new PhantomBlocks(_manager, getWorld(), getName());

        IDataNode dataNode = getDataNode();
        assert dataNode != null;

        _blocks.setIgnoresAir(dataNode.getBoolean("ignore-air"));

        getFileFormat().getLoader(this, getFileFactory()).load(
                LoadType.ALL_BLOCKS, LoadSpeed.FAST, new RegionData())
                .onError(new FutureSubscriber() {
                    @Override
                    public void on(FutureStatus status, @Nullable String message) {
                        Msg.warning("Failed to load chunk data for phantom region named '{0}' because:",
                                getName(), message);
                    }
                });
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
                    x - (currentChunk.getX() * 16), y, z - (currentChunk.getZ() * 16),
                    material, data, light, skylight);

            blocks.add(info);
        }

        @Override
        public void addSerializable(IAppliedSerializable blockEntity) {
            // do nothing
        }

        @Override
        @Nullable
        public QueueTask commit() {

            int cwX = currentChunk.getX() * 16;
            int cWZ = currentChunk.getZ() * 16;

            while (!blocks.isEmpty()) {
                ChunkBlockInfo info = blocks.remove();

                IPhantomBlock block = _blocks.getBlock(cwX + info.getX(), info.getY(), cWZ + info.getZ());
                block.set(info.getMaterial(), info.getData());
            }

            currentChunk = null;

            return null;
        }
    }
}
