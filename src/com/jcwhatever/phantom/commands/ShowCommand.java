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

package com.jcwhatever.phantom.commands;

import com.jcwhatever.nucleus.managed.commands.CommandInfo;
import com.jcwhatever.nucleus.managed.commands.arguments.ICommandArguments;
import com.jcwhatever.nucleus.managed.commands.exceptions.CommandException;
import com.jcwhatever.nucleus.managed.commands.mixins.IExecutableCommand;
import com.jcwhatever.nucleus.managed.commands.utils.AbstractCommand;
import com.jcwhatever.nucleus.managed.language.Localizable;
import com.jcwhatever.nucleus.utils.player.PlayerUtils;
import com.jcwhatever.phantom.Lang;
import com.jcwhatever.phantom.PhantomPackets;
import com.jcwhatever.phantom.blocks.regions.PhantomRegion;
import com.jcwhatever.phantom.blocks.regions.PhantomRegionManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(
        command="show",
        staticParams={ "regionName", "playerName=$self" },
        description="Shows the specified region to the command sender or a specified player.")

public class ShowCommand extends AbstractCommand implements IExecutableCommand {

    @Localizable static final String _REGION_NOT_FOUND =
            "A phantom region named '{0}' was not found.";

    @Localizable static final String _PLAYER_NOT_FOUND =
            "Player '{0}' not found.";

    @Localizable static final String _SUCCESS =
            "Phantom region named '{0}' is now visible to you.";

    @Override
    public void execute(CommandSender sender, ICommandArguments args) throws CommandException {

        String regionName = args.getName("regionName", 32);

        PhantomRegionManager manager = PhantomPackets.getRegionManager();

        PhantomRegion region = manager.get(regionName);
        if (region == null)
            throw new CommandException(Lang.get(_REGION_NOT_FOUND, regionName));

        Player player;

        if (args.getString("playerName").equals("$self")) {

            CommandException.checkNotConsole(getPlugin(), this, sender);

            player = (Player)sender;
        }
        else {

            String playerName = args.getName("playerName");

            player = PlayerUtils.getPlayer(playerName);
            if (player == null)
                throw new CommandException(Lang.get(_PLAYER_NOT_FOUND, playerName));
        }

        switch (region.getViewPolicy()) {
            case WHITELIST:
                region.addViewer(player);
                break;
            case BLACKLIST:
                region.removeViewer(player);
                break;
            default:
                throw new AssertionError();
        }

        tellSuccess(sender, Lang.get(_SUCCESS, regionName));
    }
}
