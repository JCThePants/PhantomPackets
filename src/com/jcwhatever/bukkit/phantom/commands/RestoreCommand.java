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

import com.jcwhatever.bukkit.phantom.Lang;
import com.jcwhatever.bukkit.phantom.PhantomPackets;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegion;
import com.jcwhatever.bukkit.phantom.regions.PhantomRegionManager;
import com.jcwhatever.nucleus.managed.commands.CommandInfo;
import com.jcwhatever.nucleus.managed.commands.arguments.ICommandArguments;
import com.jcwhatever.nucleus.managed.commands.exceptions.CommandException;
import com.jcwhatever.nucleus.managed.commands.mixins.IExecutableCommand;
import com.jcwhatever.nucleus.managed.commands.utils.AbstractCommand;
import com.jcwhatever.nucleus.managed.language.Localizable;
import com.jcwhatever.nucleus.regions.BuildMethod;
import com.jcwhatever.nucleus.utils.observer.future.FutureSubscriber;
import com.jcwhatever.nucleus.utils.observer.future.IFuture;
import com.jcwhatever.nucleus.utils.observer.future.IFuture.FutureStatus;

import org.bukkit.command.CommandSender;

import java.io.IOException;
import javax.annotation.Nullable;

@CommandInfo(
        command="restore",
        staticParams={ "regionName" },
        description="Restore the region from disk.")

public class RestoreCommand extends AbstractCommand implements IExecutableCommand {

    @Localizable static final String _REGION_NOT_FOUND = "A phantom region named '{0}' was not found.";
    @Localizable static final String _SAVING = "Phantom disguise region '{0}'...";
    @Localizable static final String _SUCCESS = "Phantom region named '{0}' restored.";

    @Override
    public void execute(final CommandSender sender, final ICommandArguments args) throws CommandException{

        CommandException.checkNotConsole(getPlugin(), this, sender);

        final String regionName = args.getName("regionName", 32);

        PhantomRegionManager manager = PhantomPackets.getPlugin().getRegionManager();

        PhantomRegion region = manager.getRegion(regionName);
        if (region == null)
            throw new CommandException(Lang.get(_REGION_NOT_FOUND, regionName));

        try {
            IFuture future = region.restoreData(BuildMethod.BALANCED);

            future.onError(new FutureSubscriber() {
                @Override
                public void on(FutureStatus status, @Nullable String message) {
                    if (message != null)
                        tellError(sender, message);
                }
            });

            future.onCancel(new FutureSubscriber() {
                @Override
                public void on(FutureStatus status, @Nullable String message) {
                    if (message != null)
                        tellError(sender, message);
                }
            });

            future.onSuccess(new FutureSubscriber() {
                @Override
                public void on(FutureStatus status, @Nullable String message) {
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
