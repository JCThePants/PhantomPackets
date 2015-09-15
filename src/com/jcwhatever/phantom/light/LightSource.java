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

package com.jcwhatever.phantom.light;

import com.jcwhatever.nucleus.mixins.IDisposable;
import com.jcwhatever.nucleus.mixins.INamedInsensitive;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.coords.LocationUtils;
import com.jcwhatever.phantom.PhantomPackets;

import org.bukkit.Location;

/**
 * A named light source.
 */
public class LightSource implements INamedInsensitive, IDisposable {

    private final String _name;
    private final String _searchName;
    private final Location _location = new Location(null, 0, 0, 0);
    private final LightManager _manager;

    private int _intensity;
    private boolean _isDisposed;

    /**
     * Constructor.
     *
     * @param name       The name of the light source.
     * @param location   The light source location.
     * @param intensity  The light source intensity.
     * @param manager    The owning light manager.
     */
    public LightSource(String name, Location location, int intensity, LightManager manager) {
        PreCon.notNullOrEmpty(name);
        PreCon.notNull(location);
        PreCon.notNull(intensity);

        _name = name;
        _searchName = name.toLowerCase();
        LocationUtils.copy(location, _location);
        _intensity = intensity;
        _manager = manager;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getSearchName() {
        return _searchName;
    }

    public Location getLocation() {
        return LocationUtils.copy(_location);
    }

    public Location getLocation(Location output) {
        PreCon.notNull(output);

        return LocationUtils.copy(_location, output);
    }

    public int getIntensity() {
        return _intensity;
    }

    public void setIntensity(int intensity) {
        PreCon.positiveNumber(intensity);

        _intensity = intensity;
        PhantomPackets.getNms().setLightSource(_location, intensity, true);
    }

    @Override
    public boolean isDisposed() {
        return _isDisposed;
    }

    @Override
    public void dispose() {
        _isDisposed = true;
        _manager.remove(_searchName);
    }
}
