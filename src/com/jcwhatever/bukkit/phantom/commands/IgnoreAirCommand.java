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
import com.jcwhatever.bukkit.generic.commands.exceptions.InvalidValueException;
import com.jcwhatever.bukkit.generic.internal.Lang;
import com.jcwhatever.bukkit.generic.language.Localizable;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegionManager;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import org.bukkit.command.CommandSender;

@CommandInfo(
        command="ignoreair",
        staticParams={ "regionName", "on|off|info=info" },
        usage="/{plugin-command} ignoreair <regionName> [on|off]",
        description="View or set a phantom regions 'Ignore Air' setting.")

public class IgnoreAirCommand extends AbstractCommand {

    @Localizable static final String _REGION_NOT_FOUND = "A phantom region named '{0}' was not found.";
    @Localizable static final String _INFO_IGNORES = "Phantom region '{0}' ignores air blocks.";
    @Localizable static final String _INFO_NOT_IGNORES = "Phantom region '{0}' does NOT ignore air blocks.";
    @Localizable static final String _SET_IGNORES = "Phantom region '{0}' set to ignore air blocks.";
    @Localizable static final String _SET_NOT_IGNORES = "Phantom region '{0}' set to use air blocks.";

    @Override
    public void execute(CommandSender sender, CommandArguments args)
            throws InvalidValueException, InvalidCommandSenderException {

        String regionName = args.getName("regionName", 32);

        PhantomRegionManager manager = PhantomPackets.getPlugin().getRegionManager();

        PhantomRegion region = manager.getRegion(regionName);
        if (region == null) {
            tellError(sender, Lang.get(_REGION_NOT_FOUND, regionName));
            return; // finish
        }

        if (args.getString("on|off|info").equals("info")) {

            boolean ignoresAir = region.ignoresAir();

            if (ignoresAir) {
                tell(sender, Lang.get(_INFO_IGNORES, region.getName()));
            }
            else {
                tell(sender, Lang.get(_INFO_NOT_IGNORES, region.getName()));
            }


        } else {

            boolean ignoreAir = args.getBoolean("on|off|info");

            region.setIgnoresAir(ignoreAir);

            if (ignoreAir) {
                tellSuccess(sender, Lang.get(_SET_IGNORES, region.getName()));
            }
            else {
                tellSuccess(sender, Lang.get(_SET_NOT_IGNORES, region.getName()));
            }
        }
    }
}

