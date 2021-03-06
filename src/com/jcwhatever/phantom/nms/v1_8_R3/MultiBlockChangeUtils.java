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

package com.jcwhatever.phantom.nms.v1_8_R3;

import com.jcwhatever.nucleus.managed.reflection.IReflectedInstance;
import com.jcwhatever.nucleus.managed.reflection.IReflectedType;
import com.jcwhatever.nucleus.managed.reflection.IReflection;
import com.jcwhatever.nucleus.managed.reflection.Reflection;

import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange.MultiBlockChangeInfo;

public class MultiBlockChangeUtils {

    static final IReflection reflection = Reflection.newContext();

    static final IReflectedType _PacketPlayOutMultiBlockChange =
            reflection.nmsType("PacketPlayOutMultiBlockChange")
                .fieldAlias("coords", "a")
                .fieldAlias("infoArray", "b");

    static final IReflectedType _MultiBlockChangeInfo =
            reflection.nmsType("PacketPlayOutMultiBlockChange$MultiBlockChangeInfo")
                    .constructorAlias("new",
                            PacketPlayOutMultiBlockChange.class,
                            short.class, IBlockData.class)
                    .fieldAlias("position", "b")
                    .fieldAlias("data", "c");


    static void modify(MultiBlockChangeInfo info, short position, IBlockData data) {

        IReflectedInstance instance = _MultiBlockChangeInfo.reflect(info);

        instance.set("position", position);
        instance.set("data", data);
    }

    public static MultiBlockChangeInfo create(
            PacketPlayOutMultiBlockChange packet, short position, IBlockData data) {
        return (MultiBlockChangeInfo)_MultiBlockChangeInfo.construct("new", packet, position, data);
    }

    public static void initPacket(PacketPlayOutMultiBlockChange packet,
                                  ChunkCoordIntPair coords,
                                  PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[] infoArray) {
        IReflectedInstance instance = _PacketPlayOutMultiBlockChange.reflect(packet);
        instance.set("coords", coords);
        instance.set("infoArray", infoArray);
    }
}
