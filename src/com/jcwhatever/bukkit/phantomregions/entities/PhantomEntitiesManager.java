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

package com.jcwhatever.bukkit.phantomregions.entities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class PhantomEntitiesManager {

    // Packets that update remote player entities
    private static final PacketType[] ENTITY_PACKETS = {
            Server.ENTITY_EQUIPMENT,       Server.BED,                   Server.ANIMATION,
            Server.NAMED_ENTITY_SPAWN,     Server.COLLECT,               Server.SPAWN_ENTITY,
            Server.SPAWN_ENTITY_LIVING,    Server.SPAWN_ENTITY_PAINTING, Server.SPAWN_ENTITY_EXPERIENCE_ORB,
            Server.ENTITY_VELOCITY,        Server.REL_ENTITY_MOVE,       Server.ENTITY_LOOK,
            Server.ENTITY_MOVE_LOOK,       Server.ENTITY_MOVE_LOOK,      Server.ENTITY_TELEPORT,
            Server.ENTITY_HEAD_ROTATION,   Server.ENTITY_STATUS,         Server.ATTACH_ENTITY,
            Server.ENTITY_METADATA,        Server.ENTITY_EFFECT,         Server.REMOVE_ENTITY_EFFECT,
            Server.BLOCK_BREAK_ANIMATION
    };

    private final Plugin _plugin;
    private Map<Integer, PhantomEntity> _entities = new HashMap<>(30);
    private ProtocolManager _manager;

    public PhantomEntitiesManager(Plugin plugin) {
        _plugin = plugin;
        _manager = ProtocolLibrary.getProtocolManager();

        _manager.addPacketListener(getPacketListener());

        Bukkit.getPluginManager().registerEvents(new BukkitListener(), _plugin);
    }

    public Plugin getPlugin() {
        return _plugin;
    }

    public ProtocolManager getProtocolManager() {
        return _manager;
    }

    public PhantomEntity addEntity(Entity entity) {

        PhantomEntity phantom = _entities.get(entity.getEntityId());
        if (phantom != null)
            return phantom;

        phantom = new PhantomEntity(this, entity);
        
        if (!entity.isDead()) {
            _entities.put(entity.getEntityId(), phantom);
        }

        return phantom;
    }

    public boolean removeEntity(Entity entity) {
        return _entities.remove(entity.getEntityId()) != null;
    }

    public PhantomEntity getEntity(Entity entity) {
        return _entities.get(entity.getEntityId());
    }

    public List<PhantomEntity> getEntities() {
        return new ArrayList<>(_entities.values());
    }

    private PacketAdapter getPacketListener() {
        return new PacketAdapter(_plugin, ENTITY_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int entityID = event.getPacket().getIntegers().read(0);

                PhantomEntity entity = _entities.get(entityID);
                if (entity == null)
                    return;

                if (!entity.canSee(event.getPlayer())) {
                    event.setCancelled(true);
                }
            }
        };
    }

    private class BukkitListener implements Listener {
        @EventHandler
        public void onEntityDeath(EntityDeathEvent e) {
            _entities.remove(e.getEntity().getEntityId());
        }
    }

}
