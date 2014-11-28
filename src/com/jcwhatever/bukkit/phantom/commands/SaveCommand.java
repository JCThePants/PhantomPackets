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
import com.jcwhatever.bukkit.generic.commands.exceptions.InvalidValueException;
import com.jcwhatever.bukkit.generic.internal.Lang;
import com.jcwhatever.bukkit.generic.language.Localizable;
import com.jcwhatever.bukkit.generic.performance.queued.QueueResult.CancelHandler;
import com.jcwhatever.bukkit.generic.performance.queued.QueueResult.FailHandler;
import com.jcwhatever.bukkit.generic.performance.queued.QueueResult.Future;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegionManager;
import com.jcwhatever.bukkit.phantom.PhantomPackets;

import org.bukkit.command.CommandSender;

import java.io.IOException;
import javax.annotation.Nullable;

@CommandInfo(
        command="save",
        staticParams={ "regionName" },
        usage="/{plugin-command} save <regionName>",
        description="Save the region to disk. The disk file is used to display the region disguise.")

public class SaveCommand extends AbstractCommand {

    @Localizable static final String _REGION_NOT_FOUND = "A disguise region named '{0}' was not found.";
    @Localizable static final String _SAVING = "Saving disguise region '{0}'...";
    @Localizable static final String _SUCCESS = "Disguise region named '{0}' saved.";

    @Override
    public void execute(final CommandSender sender, CommandArguments args)
            throws InvalidValueException, InvalidCommandSenderException {

        InvalidCommandSenderException.check(sender, CommandSenderType.PLAYER, "Console cannot select region.");

        final String regionName = args.getName("regionName", 32);

        PhantomRegionManager manager = PhantomPackets.getPlugin().getRegionManager();

        PhantomRegion region = manager.getRegion(regionName);
        if (region == null) {
            tellError(sender, Lang.get(_REGION_NOT_FOUND, regionName));
            return; // finish
        }

        try {
            Future future = region.saveData();

            future.onFail(new FailHandler() {
                @Override
                public void run(@Nullable String reason) {
                    if (reason != null)
                        tellError(sender, reason);

                }
            });

            future.onCancel(new CancelHandler() {
                @Override
                public void run(@Nullable String reason) {

                    if (reason != null)
                        tellError(sender, reason);
                }
            });

            future.onComplete(new Runnable() {
                @Override
                public void run() {
                    tellSuccess(sender, Lang.get(_SUCCESS, regionName));
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
            return; // finished
        }

        tell(sender, Lang.get(_SAVING, regionName));
    }
}
