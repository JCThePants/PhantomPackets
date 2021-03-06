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

package com.jcwhatever.phantom.entities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.jcwhatever.nucleus.managed.entity.ITrackedEntity;
import com.jcwhatever.nucleus.utils.PreCon;
import com.jcwhatever.nucleus.utils.coords.ChunkCoords;
import com.jcwhatever.nucleus.utils.observer.update.UpdateSubscriber;
import com.jcwhatever.nucleus.utils.player.PlayerUtils;
import com.jcwhatever.phantom.PhantomPackets;

import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Manage phantom entities.
 *
 * <p>Allows adding and removing entities so viewers can be set.</p>
 */
public class PhantomEntitiesManager {

    // entity packets
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

    private Map<UUID, PhantomEntity> _entities = new HashMap<>(30);
    private Map<Integer, PhantomEntity> _loadedEntities = new HashMap<>(30);

    /**
     * Constructor.
     */
    public PhantomEntitiesManager() {
        ProtocolLibrary.getProtocolManager().addPacketListener(getPacketListener());
    }

    /**
     * Add an entity so viewers can be set.
     *
     * <p>If the entity is already added, the current
     * {@code PhantomEntity} is returned.</p>
     *
     * @param entity  The entity to add.
     */
    public PhantomEntity addEntity(Entity entity) {
        PreCon.notNull(entity);

        PhantomEntity phantom = _entities.get(entity.getUniqueId());
        if (phantom != null)
            return phantom;

        phantom = new PhantomEntity(this, entity);

        if (entity.isDead())
            return phantom;

        final ITrackedEntity trackedEntity = phantom.getTrackedEntity();

        trackedEntity.onUpdate(new UpdateSubscriber<Entity>() {

            @Override
            public void on(Entity event) {

                PhantomEntity phantom = _entities.get(trackedEntity.getUniqueId());
                if (phantom != null) {
                    _loadedEntities.put(trackedEntity.getEntity().getEntityId(), phantom);
                }
            }

        }).onUnload(new UpdateSubscriber<ChunkCoords>() {

            @Override
            public void on(ChunkCoords event) {
                _loadedEntities.remove(trackedEntity.getEntity().getEntityId());
            }

        }).onDeath(new UpdateSubscriber<Entity>() {

            @Override
            public void on(Entity event) {
                _loadedEntities.remove(trackedEntity.getEntity().getEntityId());
                _entities.remove(trackedEntity.getUniqueId());
            }
        });

        _entities.put(entity.getUniqueId(), phantom);

        if (entity.getLocation().getChunk().isLoaded()) {
            _loadedEntities.put(entity.getEntityId(), phantom);
        }

        return phantom;
    }

    /**
     * Remove an entity.
     *
     * @param entity  The entity to remove.
     */
    public boolean removeEntity(Entity entity) {
        PreCon.notNull(entity);

        PhantomEntity phantom = _entities.remove(entity.getUniqueId());
        if (phantom == null)
            return false;

        Entity phantomEntity = phantom.getEntity();
        if (phantomEntity != null) {
            _loadedEntities.remove(phantomEntity.getEntityId());
        }

        return true;
    }

    /**
     * Get a phantom entity that has already been
     * created from the specified entity.
     *
     * @param entity  The entity.
     *
     * @return  Null if there is no phantom entity created.
     */
    @Nullable
    public PhantomEntity getEntity(Entity entity) {
        PreCon.notNull(entity);

        return _entities.get(entity.getUniqueId());
    }

    /**
     * Get all phantom entities.
     */
    public List<PhantomEntity> getEntities() {
        return new ArrayList<>(_entities.values());
    }

    /*
     * Get the entity packet listener
     */
    private PacketAdapter getPacketListener() {
        return new PacketAdapter(PhantomPackets.getPlugin(), ENTITY_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int entityID = event.getPacket().getIntegers().read(0);

                PhantomEntity entity = _loadedEntities.get(entityID);
                if (entity == null)
                    return;

                if (!entity.canSee(event.getPlayer())) {
                    event.setCancelled(true);

                    // sometimes the client still receives an entity packet
                    // when the player first logs in or changes world, destroy it
                    if (PlayerUtils.getWorldSessionTime(event.getPlayer()) < 500) {
                        entity.sendDestroyPacket(event.getPlayer());
                    }
                }
            }
        };
    }
}
