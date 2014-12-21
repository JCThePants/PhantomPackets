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

package com.jcwhatever.bukkit.phantom.commands;

import com.jcwhatever.bukkit.generic.commands.AbstractCommand;
import com.jcwhatever.bukkit.generic.commands.CommandInfo;
import com.jcwhatever.bukkit.generic.commands.arguments.CommandArguments;
import com.jcwhatever.bukkit.generic.commands.exceptions.InvalidCommandSenderException;
import com.jcwhatever.bukkit.generic.commands.exceptions.InvalidCommandSenderException.CommandSenderType;
import com.jcwhatever.bukkit.generic.commands.exceptions.InvalidArgumentException;
import com.jcwhatever.bukkit.generic.internal.Lang;
import com.jcwhatever.bukkit.generic.language.Localizable;
import com.jcwhatever.bukkit.generic.utils.PlayerUtils;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegionManager;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(
        command="hide",
        staticParams={ "regionName", "playerName=$self" },
        usage="/{plugin-command} hide <regionName> [playerName]",
        description="Hide the specified region from the command sender or specified player.")

public class HideCommand extends AbstractCommand {

    @Localizable static final String _REGION_NOT_FOUND = "A phantom region named '{0}' was not found.";
    @Localizable static final String _PLAYER_NOT_FOUND = "Player '{0}' not found.";
    @Localizable static final String _SUCCESS = "Phantom region named '{0}' is hidden from you.";

    @Override
    public void execute(CommandSender sender, CommandArguments args)
            throws InvalidArgumentException, InvalidCommandSenderException {

        String regionName = args.getName("regionName", 32);

        PhantomRegionManager manager = PhantomPackets.getPlugin().getRegionManager();

        PhantomRegion region = manager.getRegion(regionName);
        if (region == null) {
            tellError(sender, Lang.get(_REGION_NOT_FOUND, regionName));
            return; // finish
        }

        Player player;

        if (args.getString("playerName").equals("$self")) {

            InvalidCommandSenderException.check(sender, CommandSenderType.PLAYER,
                    "Console cannot see disguise.");

            player = (Player)sender;
        }
        else {

            String playerName = args.getName("playerName");

            player = PlayerUtils.getPlayer(playerName);

            if (player == null) {
                tellError(sender, Lang.get(_PLAYER_NOT_FOUND, playerName));
                return; // finished
            }
        }

        switch (region.getViewPolicy()) {
            case WHITELIST:
                region.removeViewer(player);
                break;
            case BLACKLIST:
                region.addViewer(player);
                break;
            default:
                throw new AssertionError();
        }

        tellSuccess(sender, Lang.get(_SUCCESS, regionName));
    }
}

