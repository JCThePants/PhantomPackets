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

package com.jcwhatever.bukkit.phantom;

import com.jcwhatever.bukkit.phantom.commands.PhantomCommandDispatcher;
import com.jcwhatever.bukkit.phantom.entities.PhantomEntitiesManager;
import com.jcwhatever.bukkit.phantom.nms.INmsHandler;
import com.jcwhatever.bukkit.phantom.nms.v1_8_R1.NmsHandler_v1_8_R1;
import com.jcwhatever.bukkit.phantom.nms.v1_8_R2.NmsHandler_v1_8_R2;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegionManager;
import com.jcwhatever.bukkit.phantom.scripts.PhantomScriptApi;
import com.jcwhatever.nucleus.Nucleus;
import com.jcwhatever.nucleus.NucleusPlugin;
import com.jcwhatever.nucleus.mixins.IDisposable;
import com.jcwhatever.nucleus.scripting.IEvaluatedScript;
import com.jcwhatever.nucleus.scripting.IScriptApi;
import com.jcwhatever.nucleus.scripting.SimpleScriptApi;
import com.jcwhatever.nucleus.scripting.SimpleScriptApi.IApiObjectCreator;
import com.jcwhatever.nucleus.utils.nms.NmsManager;
import com.jcwhatever.nucleus.utils.text.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PhantomPackets extends NucleusPlugin {

    private static PhantomPackets _plugin;

    public static PhantomPackets getPlugin() {
        return _plugin;
    }

    private PhantomRegionManager _regionManager;
    private PhantomEntitiesManager _entitiesManager;
    private IScriptApi _scriptApi;

    private NmsManager _reflectionManager;
    private INmsHandler _reflectionHandler;

    public PhantomPackets() {
        super();

        _plugin = this;
    }

    @Override
    public String getChatPrefix() {
        return TextColor.WHITE.toString() + '[' + TextColor.GRAY + "PhantomPackets" + TextColor.WHITE + "] ";
    }

    @Override
    public String getConsolePrefix() {
        return "[PhantomPackets] ";
    }

    public PhantomRegionManager getRegionManager() {
        return _regionManager;
    }

    public PhantomEntitiesManager getEntitiesManager() {
        return _entitiesManager;
    }

    public NmsManager getReflectionManager() {
        return _reflectionManager;
    }

    public static INmsHandler getNms() {
        return _plugin._reflectionHandler;
    }

    @Override
    protected void onEnablePlugin() {

        _reflectionManager = new NmsManager(this, "v1_8_R1", "v1_8_R2");
        _reflectionManager.registerNmsHandler("v1_8_R1", "nms", NmsHandler_v1_8_R1.class);
        _reflectionManager.registerNmsHandler("v1_8_R2", "nms", NmsHandler_v1_8_R2.class);

        _reflectionHandler = _reflectionManager.getHandler("nms");
        if (_reflectionHandler == null) {
            Msg.warning("Failed to get an NMS handler. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        _regionManager = new PhantomRegionManager(this);
        _entitiesManager = new PhantomEntitiesManager(this);
        registerCommands(new PhantomCommandDispatcher(this));

        _scriptApi = new SimpleScriptApi(this, "phantom", new IApiObjectCreator() {
            @Override
            public IDisposable create(Plugin plugin, IEvaluatedScript script) {
                return new PhantomScriptApi();
            }
        });

        Nucleus.getScriptApiRepo().registerApi(_scriptApi);
    }

    @Override
    protected void onDisablePlugin() {

        if (_regionManager != null)
            _regionManager.dispose();

        Nucleus.getScriptApiRepo().unregisterApi(_scriptApi);
    }

}
