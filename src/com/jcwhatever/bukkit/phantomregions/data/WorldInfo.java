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

package com.jcwhatever.bukkit.phantomregions.data;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;

import javax.annotation.Nullable;

public class WorldInfo {

    private String _worldName;
    private Environment _environment;

    public WorldInfo (World world) {
        _worldName = world.getName();
        _environment = world.getEnvironment();
    }

    public String getName() {
        return _worldName;
    }

    public Environment getEnvironment() {
        return _environment;
    }

    public World getBukkitWorld() {
        return Bukkit.getWorld(_worldName);
    }

    public boolean equalsBukkitWorld(@Nullable World world) {
        return world != null && world.getName().equalsIgnoreCase(_worldName);
    }

    @Override
    public int hashCode() {
        return _worldName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WorldInfo &&
                ((WorldInfo) obj)._worldName.equalsIgnoreCase(_worldName);
    }
}
