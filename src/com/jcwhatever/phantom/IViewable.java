
package com.jcwhatever.phantom;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Mixin defines an implementation that determines
 * which players can see whatever object it represents
 * in the world.
 */
public interface IViewable {

    /**
     * Get the view policy.
     */
    ViewPolicy getViewPolicy();

    /**
     * Set the view policy.
     *
     * @param viewPolicy  The view policy.
     */
    void setViewPolicy(ViewPolicy viewPolicy);

    /**
     * Determine if the player can see the object.
     *
     * @param player The player to check.
     */
    boolean canSee(Player player);

    /**
     * Determine if the specified player is in the collection of viewers.
     *
     * @param player  The player to check.
     */
    boolean hasViewer(Player player);

    /**
     * Add a player to the collection of viewers.
     *
     * @param player  The player to add.
     *
     * @return True if the player was added.
     */
    boolean addViewer(Player player);

    /**
     * Remove a player from the collection of viewers.
     *
     * @param player  The player to remove.
     *
     * @return True if the player was removed.
     */
    boolean removeViewer(Player player);

    /**
     * Clear all players from the collection of
     * viewers.
     */
    void clearViewers();

    /**
     * Get all players in the view collection.
     */
    List<Player> getViewers();

    /**
     * Resend view to viewers.
     */
    void refreshView();

    /**
     * Define how the viewer collection is treated.
     */
    enum ViewPolicy {
        /**
         * All viewers in the collection cannot
         * see the object in the world.
         */
        BLACKLIST,

        /**
         * Only viewers in the collection can
         * see the object in the world.
         */
        WHITELIST
    }
}
