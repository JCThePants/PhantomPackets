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

import com.jcwhatever.nucleus.collections.ElementCounter;
import com.jcwhatever.nucleus.collections.ElementCounter.RemovalPolicy;
import com.jcwhatever.nucleus.storage.IDataNode;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.phantom.PhantomPackets;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Manager for phantom regions.
 */
public class PhantomRegionManager {

    private final IDataNode _dataNode;
    private final Map<String, PhantomRegion> _regions = new HashMap<>(25);
    private final ElementCounter<World> _worlds = new ElementCounter<World>(RemovalPolicy.REMOVE);

    /**
     * Constructor.
     */
    public PhantomRegionManager() {
        _dataNode = PhantomPackets.getPlugin().getDataNode().getNode("regions");

        loadRegions();
    }

    /**
     * Get worlds that contain phantom regions.
     */
    public Set<World> getWorlds() {
        return _worlds.getElements();
    }

    /**
     * Determine if a world contains a phantom region.
     *
     * @param world  The world to check.
     */
    public boolean hasRegionInWorld(World world) {
        return _worlds.contains(world);
    }

    /**
     * Get a phantom region by case insensitive name.
     *
     * @param name  The name of the region.
     *
     * @return  The phantom region or null if not found.
     */
    @Nullable
    public PhantomRegion get(String name) {
        PreCon.notNullOrEmpty(name);

        return _regions.get(name.toLowerCase());
    }

    /**
     * Get all phantom regions.
     */
    public List<PhantomRegion> getAll() {
        return new ArrayList<>(_regions.values());
    }

    /**
     * Add a new phantom region.
     *
     * @param name  The name of the region.
     * @param p1    The 1st cuboid point.
     * @param p2    The 2nd cuboid point.
     */
    @Nullable
    public PhantomRegion add(String name, Location p1, Location p2) {
        PreCon.notNullOrEmpty(name);
        PreCon.notNull(p1);
        PreCon.notNull(p2);

        if (PhantomPackets.getBlockContexts().contains(name))
            return null;

        PhantomRegion region = get(name);
        if (region != null)
            return null;

        region = new PhantomRegion(PhantomPackets.getBlockContexts(), name, _dataNode.getNode(name));
        region.setCoords(p1, p2);

        _worlds.add(region.getWorld());

        try {
            region.saveData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _regions.put(region.getSearchName(), region);
        PhantomPackets.getBlockContexts().add(region);

        return region;
    }

    /**
     * Remove a phantom region by case-insensitive name.
     *
     * @param name  The name of the phantom region.
     *
     * @return  True if found and removed, otherwise false.
     */
    public boolean remove(String name) {
        PreCon.notNullOrEmpty(name);

        PhantomRegion region = _regions.remove(name.toLowerCase());
        if (region == null)
            return false;

        if (region.isDefined() && region.isWorldLoaded()) {
            _worlds.subtract(region.getWorld());
        }

        IDataNode dataNode = region.getDataNode();
        if (dataNode != null) {
            dataNode.remove();
            dataNode.save();
        }

        region.dispose();

        return true;
    }

    /**
     * Dispose the manager and regions.
     */
    public void dispose() {
        for (PhantomRegion region : _regions.values()) {
            region.dispose();
        }
    }

    private void loadRegions() {

        for (IDataNode regionNode : _dataNode) {

            PhantomRegion region = new PhantomRegion(
                    PhantomPackets.getBlockContexts(), regionNode.getName(), regionNode);

            _regions.put(region.getSearchName(), region);

            if (region.isDefined() && region.getWorld() != null) {
                _worlds.add(region.getWorld());
            }

            PhantomPackets.getBlockContexts().add(region);
        }
    }
}
