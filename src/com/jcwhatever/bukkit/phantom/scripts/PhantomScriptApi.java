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

package com.jcwhatever.bukkit.phantom.scripts;

import com.jcwhatever.bukkit.generic.utils.PlayerUtils;
import com.jcwhatever.bukkit.generic.scripting.IEvaluatedScript;
import com.jcwhatever.bukkit.generic.scripting.ScriptApiInfo;
import com.jcwhatever.bukkit.generic.scripting.api.GenericsScriptApi;
import com.jcwhatever.bukkit.generic.scripting.api.IScriptApiObject;
import com.jcwhatever.bukkit.generic.utils.PreCon;
import com.jcwhatever.bukkit.phantom.entities.PhantomEntity;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

@ScriptApiInfo(
        variableName = "phantom",
        description = "Gives script api access to Phantom regions and entities.")
public class PhantomScriptApi extends GenericsScriptApi {

    private static ApiObject _api = new ApiObject();

    /**
     * Constructor.
     *
     * @param plugin The owning plugin
     */
    public PhantomScriptApi(Plugin plugin) {
        super(plugin);
    }

    @Override
    public IScriptApiObject getApiObject(IEvaluatedScript script) {
        return _api;
    }

    public static class ApiObject implements IScriptApiObject {

        Map<Player, Set<PhantomRegion>> _regionMap = new WeakHashMap<>(30);
        Map<Player, Set<PhantomEntity>> _entityMap = new WeakHashMap<>(30);
        private boolean _isDisposed;

        @Override
        public boolean isDisposed() {
            return _isDisposed;
        }

        @Override
        public void dispose() {
            for (Entry<Player, Set<PhantomRegion>> playerSetEntry : _regionMap.entrySet()) {

                Set<PhantomRegion> regionList = playerSetEntry.getValue();

                for (PhantomRegion region : regionList) {
                    region.removeViewer(playerSetEntry.getKey());
                }
            }
            _regionMap.clear();

            for (Entry<Player, Set<PhantomEntity>> playerSetEntry : _entityMap.entrySet()) {

                Set<PhantomEntity> entityList = playerSetEntry.getValue();

                for (PhantomEntity entity : entityList) {
                    entity.removeViewer(playerSetEntry.getKey());
                }
            }

            _entityMap.clear();

            _isDisposed = true;
        }

        public final PhantomRegionsAPI regions = new PhantomRegionsAPI();
        public final PhantomEntityAPI entity = new PhantomEntityAPI();

        public class PhantomRegionsAPI {

            /**
             * Determine if a region has a viewer.
             *
             * @param player      The player to check.
             * @param regionName  The region to check.
             */
            public boolean hasViewer(Object player, String regionName) {
                PreCon.notNull(player);
                PreCon.notNull(entity);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomRegion region = PhantomPackets.getPlugin().getRegionManager().getRegion(regionName);
                return region != null && region.hasViewer(p);
            }

            /**
             * Add a player to a phantom region as a whitelisted viewer.
             *
             * @param player      The player to show the region to.
             * @param regionName  The phantom region name.
             *
             * @return  True if the region was found and shown to the player.
             */
            public boolean addViewer(Object player, String regionName) {
                PreCon.notNull(player);
                PreCon.notNullOrEmpty(regionName);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomRegion region = PhantomPackets.getPlugin().getRegionManager().getRegion(regionName);
                if (region == null)
                    return false;

                region.addViewer(p);

                Set<PhantomRegion> regionList = _regionMap.get(p);
                if (regionList == null) {
                    regionList = new HashSet<>(10);
                    _regionMap.put(p, regionList);
                }

                regionList.add(region);

                return true;
            }

            /**
             * Remove a player from a phantom region as a whitelisted viewer.
             *
             * @param player      The player to hide the region from.
             * @param regionName  The disguise region name.
             *
             * @return  True if the region was found and hidden.
             */
            public boolean removeViewer(Object player, String regionName) {
                PreCon.notNull(player);
                PreCon.notNullOrEmpty(regionName);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomRegion region = PhantomPackets.getPlugin().getRegionManager().getRegion(regionName);
                if (region == null)
                    return false;

                region.removeViewer(p);

                Set<PhantomRegion> regionList = _regionMap.get(p);
                if (regionList != null) {
                    regionList.remove(region);
                }

                return true;
            }
        }

        public class PhantomEntityAPI {

            /**
             * Make the specified entity a phantom entity.
             *
             * @param entity  The entity.
             */
            public void addEntity(Entity entity) {
                PreCon.notNull(entity);

                PhantomPackets.getPlugin().getEntitiesManager().addEntity(entity);
            }

            /**
             * Remove the specified entity as a phantom entity.
             *
             * @param entity  The entity to remove.
             */
            public void removeEntity(Entity entity) {
                PreCon.notNull(entity);

                PhantomPackets.getPlugin().getEntitiesManager().removeEntity(entity);
            }

            /**
             * Determine if an entity has a viewer.
             *
             * <p>If the entity is not a phantom entity then false
             * is returned.</p>
             *
             * @param player  The player to check.
             * @param entity  The entity to check.
             */
            public boolean hasViewer(Object player, Entity entity) {
                PreCon.notNull(player);
                PreCon.notNull(entity);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomEntity phantomEntity = PhantomPackets.getPlugin()
                        .getEntitiesManager().getEntity(entity);

                return phantomEntity != null && phantomEntity.hasViewer(p);
            }

            /**
             * Add a player to a phantom entity as a whitelisted viewer.
             *
             * @param player  The player to show the entity to.
             * @param entity  The entity to show.
             *
             * @return  True if the viewer was added.
             */
            public boolean addViewer(Object player, Entity entity) {
                PreCon.notNull(player);
                PreCon.notNull(entity);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomEntity phantomEntity = PhantomPackets.getPlugin()
                        .getEntitiesManager().addEntity(entity);

                if (phantomEntity.addViewer(p)) {

                    Set<PhantomEntity> entityList = _entityMap.get(p);
                    if (entityList == null) {
                        entityList = new HashSet<>(10);
                        _entityMap.put(p, entityList);
                    }

                    entityList.add(phantomEntity);

                    return true;
                }

                return false;
            }

            /**
             * Remove a player from a phantom entity as a whitelisted viewer.
             *
             * @param player  The player to hide the entity from.
             * @param entity  The entity to hide.
             *
             * @return  True if the viewer was removed.
             */
            public boolean removeViewer(Object player, Entity entity) {
                PreCon.notNull(player);
                PreCon.notNull(entity);

                Player p = PlayerUtils.getPlayer(player);
                PreCon.notNull(p);

                PhantomEntity phantomEntity = PhantomPackets.getPlugin()
                        .getEntitiesManager().addEntity(entity);

                if (phantomEntity.removeViewer(p)) {

                    Set<PhantomEntity> entityList = _entityMap.get(p);
                    if (entityList != null) {
                        entityList.remove(phantomEntity);
                    }

                    return true;
                }

                return false;
            }
        }

    }
}
