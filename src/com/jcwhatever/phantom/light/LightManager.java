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

import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.managers.NamedInsensitiveManager;
import com.jcwhatever.phantom.PhantomPackets;

import org.bukkit.Location;

/**
 * Manage light sources.
 */
public class LightManager extends NamedInsensitiveManager<LightSource> {

    /**
     * Create a new light source.
     *
     * @param name       The name of the light source.
     * @param location   The light source location.
     * @param intensity  The intensity of the light.
     *
     * @return  The light source instance.
     */
    public LightSource create(String name, Location location, int intensity) {
        PreCon.notNullOrEmpty(name);
        PreCon.notNull(location);
        PreCon.positiveNumber(intensity);

        LightSource lightSource = new LightSource(name, location, intensity, this);
        add(lightSource);

        PhantomPackets.getNms().setLightSource(location, intensity, true);

        return lightSource;
    }
}
