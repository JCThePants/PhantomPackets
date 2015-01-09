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
import com.comphenix.protocol.events.PacketAdapter;
import com.jcwhatever.nucleus.collections.EntryCounter;
import com.jcwhatever.nucleus.collections.EntryCounter.RemovalPolicy;
import com.jcwhatever.nucleus.storage.IDataNode;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/*
 * 
 */
public class PhantomRegionManager {

    private final PhantomPackets _plugin;
    private final IDataNode _dataNode;
    private final Map<String, PhantomRegion> _regions = new HashMap<>(25);
    private final EntryCounter<World> _worlds = new EntryCounter<World>(RemovalPolicy.REMOVE);

    public PhantomRegionManager(PhantomPackets plugin) {
        _plugin = plugin;
        _dataNode = plugin.getDataNode().getNode("regions");

        loadRegions();

        PacketAdapter packetListener = new RegionProtocolListener(plugin, this);
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }

    public Set<World> getWorlds() {
        return _worlds.getEntries();
    }

    public boolean hasRegionInWorld(World world) {
        return _worlds.contains(world);
    }

    @Nullable
    public PhantomRegion getRegion(String name) {
        PreCon.notNullOrEmpty(name);

        return _regions.get(name.toLowerCase());
    }

    public List<PhantomRegion> getRegions() {
        return new ArrayList<>(_regions.values());
    }

    @Nullable
    public PhantomRegion addRegion(String name, Location p1, Location p2) {
        PreCon.notNullOrEmpty(name);
        PreCon.notNull(p1);
        PreCon.notNull(p2);

        PhantomRegion region = getRegion(name);
        if (region != null)
            return null;

        region = new PhantomRegion(_plugin, name, _dataNode.getNode(name));
        region.setCoords(p1, p2);

        _worlds.add(region.getWorld());

        try {
            region.saveData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _regions.put(region.getSearchName(), region);

        return region;
    }

    public boolean removeRegion(String name) {
        PreCon.notNullOrEmpty(name);

        PhantomRegion region = _regions.remove(name.toLowerCase());
        if (region == null)
            return false;

        if (region.isDefined()) {
            _worlds.subtract(region.getWorld());
        }

        IDataNode dataNode = region.getDataNode();
        if (dataNode != null) {
            dataNode.remove();
            dataNode.saveAsync(null);
        }

        region.dispose();

        return true;
    }

    public void dispose() {
        for (PhantomRegion region : _regions.values()) {
            region.dispose();
        }
    }

    private void loadRegions() {

        for (IDataNode regionNode : _dataNode) {

            PhantomRegion region = new PhantomRegion(_plugin, regionNode.getName(), regionNode);

            _regions.put(region.getSearchName(), region);

            if (region.isDefined()) {
                _worlds.add(region.getWorld());
            }
        }
    }
}
