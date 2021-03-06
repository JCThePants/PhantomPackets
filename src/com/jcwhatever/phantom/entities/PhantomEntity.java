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

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.jcwhatever.nucleus.collections.players.PlayerSet;
import com.jcwhatever.nucleus.managed.entity.ITrackedEntity;
import com.jcwhatever.nucleus.providers.npc.Npcs;
import com.jcwhatever.nucleus.utils.entity.EntityUtils;
import com.jcwhatever.phantom.IViewable;
import com.jcwhatever.phantom.PhantomPackets;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single entity whose visibility is
 * being manipulated.
 */
public class PhantomEntity implements IViewable {

    private ITrackedEntity _trackedEntity;
    private PhantomEntitiesManager _manager;
    private PlayerSet _viewers;
    private ViewPolicy _viewPolicy = ViewPolicy.WHITELIST;

    /**
     * Constructor.
     *
     * @param manager  The owning manager.
     * @param entity   The entity to encapsulate.
     */
    public PhantomEntity (PhantomEntitiesManager manager, Entity entity) {
        _manager = manager;

        _trackedEntity = EntityUtils.trackEntity(entity);

        updateLocalPlayers();
    }

    /**
     * Get the owning manager.
     */
    public PhantomEntitiesManager getManager() {
        return _manager;
    }

    /**
     * Get the encapsulated entity.
     */
    @Nullable
    public Entity getEntity() {
        return _trackedEntity.getEntity();
    }

    /**
     * Get the tracked entity.
     */
    public ITrackedEntity getTrackedEntity() {
        return _trackedEntity;
    }

    /**
     * Get the entity ID.
     */
    public int getId() {
        Entity entity = getEntity();
        if (entity == null)
            return -1;

        return entity.getEntityId();
    }

    @Override
    public ViewPolicy getViewPolicy() {
        return _viewPolicy;
    }

    @Override
    public void setViewPolicy(ViewPolicy viewPolicy) {
        if (_viewPolicy == viewPolicy)
            return;

        _viewPolicy = viewPolicy;

        switch (viewPolicy) {
            case WHITELIST:
                showToViewers(_viewers);
                break;
            case BLACKLIST:
                hideFromViewers(_viewers);
                break;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean canSee(Player player) {
        boolean hasViewer = hasViewer(player);
        return (_viewPolicy == ViewPolicy.BLACKLIST && !hasViewer) ||
                (_viewPolicy == ViewPolicy.WHITELIST && hasViewer);
    }

    @Override
    public boolean hasViewer(Player player) {
        return _viewers != null &&
                _viewers.contains(player);
    }

    @Override
    public boolean addViewer(Player player) {
        initViewers();

        boolean add = _viewers.add(player);

        switch (_viewPolicy) {
            case WHITELIST:
                showTo(player);
                break;
            case BLACKLIST:
                hideFrom(player);
                break;
            default:
                throw new AssertionError();
        }

        return add;
    }

    @Override
    public boolean removeViewer(Player player) {
        if (_viewers == null)
            return false;

        if (!_viewers.remove(player)) {
            return false;
        }

        switch (_viewPolicy) {
            case WHITELIST:
                hideFrom(player);
                break;
            case BLACKLIST:
                showTo(player);
                break;
            default:
                throw new AssertionError();
        }

        return true;
    }

    @Override
    public void clearViewers() {
        if (_viewers == null)
            return;

        List<Player> viewers = new ArrayList<>(_viewers);

        _viewers.clear();

        refreshView(viewers);
    }

    @Override
    public List<Player> getViewers() {
        if (_viewers == null)
            return new ArrayList<>(0);

        return new ArrayList<>(_viewers);
    }

    @Override
    public void refreshView() {
        refreshView(new ArrayList<Player>(_viewers));
    }

    public void refreshView(List<Player> viewers) {

        if (_viewers == null)
            return;

        switch (_viewPolicy) {
            case WHITELIST:
                hideFromViewers(viewers);
                break;
            case BLACKLIST:
                showToViewers(viewers);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void initViewers() {
        if (_viewers != null)
            return;

        _viewers = new PlayerSet(PhantomPackets.getPlugin());
    }

    private void showTo(Player player) {
        Entity entity = getEntity();

        if (entity == null || !entity.isValid())
            return;

        if (!player.getWorld().equals(entity.getWorld()))
            return;

        ProtocolLibrary.getProtocolManager()
                .updateEntity(entity, Arrays.asList(player));
    }

    private void showToViewers(Collection<Player> viewers) {

        Entity entity = getEntity();

        if (entity == null || !entity.isValid())
            return;

        if (viewers == null || viewers.isEmpty())
            return;

        List<Player> players = new ArrayList<>(viewers.size());
        for (Player player : viewers) {

            if (!player.getWorld().equals(entity.getWorld()))
                continue;

            players.add(player);
        }

        if (players.isEmpty())
            return;

        ProtocolLibrary.getProtocolManager()
                .updateEntity(entity, players);
    }

    private void hideFrom(Player player) {

        Entity entity = getEntity();
        if (entity == null || entity.isDead())
            return;

        PacketContainer destroyPacket = getDestroyPacket();

        try {
            ProtocolLibrary.getProtocolManager()
                    .sendServerPacket(player, destroyPacket);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void hideFromViewers(Collection<Player> viewers) {
        Entity entity = getEntity();

        if (entity == null || entity.isDead())
            return;

        if (viewers == null || viewers.isEmpty())
            return;

        sendDestroyPacket(viewers);
    }

    private void updateLocalPlayers() {

        Entity entity = getEntity();

        if (entity == null || entity.isDead())
            return;

        World world = _trackedEntity.getWorld();

        for (Player player : world.getPlayers()) {

            if (Npcs.isNpc(player))
                continue;

            if (canSee(player)) {
                showTo(player);
            }
            else {
                hideFrom(player);
            }
        }
    }

    void sendDestroyPacket(Collection<Player> viewers) {
        PacketContainer destroyPacket = getDestroyPacket();

        try {
            for (Player player : viewers) {
                ProtocolLibrary.getProtocolManager()
                        .sendServerPacket(player, destroyPacket);
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    void sendDestroyPacket(Player viewer) {
        PacketContainer destroyPacket = getDestroyPacket();

        try {
            ProtocolLibrary.getProtocolManager()
                    .sendServerPacket(viewer, destroyPacket);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private PacketContainer getDestroyPacket() {

        Entity entity = getEntity();

        if (entity == null || entity.isDead())
            return null;

        PacketContainer destroyEntity = new PacketContainer(Server.ENTITY_DESTROY);
        destroyEntity.getIntegerArrays().write(0, new int[] { entity.getEntityId() });

        return destroyEntity;
    }
}
