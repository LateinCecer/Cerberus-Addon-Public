/*
 * Cerberus-Addon is a simple addon management library
 * Visit https://cerberustek.com for more details
 * Copyright (c)  2020  Adrian Paskert
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. See the file LICENSE included with this
 * distribution for more information.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.cerberustek.addon;

import com.cerberustek.Destroyable;
import com.cerberustek.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

/**
 * Main addon interface
 */
public interface Addon extends Destroyable {

    /**
     * Will call the enable method in the addons main class
     * if present.
     *
     * This method will return true, if the enable method does
     * exist in the main class and could be called without any
     * exceptions being thrown.
     *
     * @return success
     */
    boolean enable();

    /**
     * Will call the disable method in the addons main class
     * if present.
     *
     * This method will return true, if the enable method does
     * exist in the main class and could be called without any
     * exception being thrown.
     *
     * @return success
     */
    boolean disable();

    /**
     * Returns true, if the addon is currently active.
     * @return is active
     */
    boolean isActive();

    /**
     * Returns a list of all threads owned by the addon.
     * @return threads owned by the addon
     */
    @NotNull Collection<Thread> getThreadList();

    /**
     * Returns the addon info.
     * @return addon info
     */
    @NotNull
    AddonInfo getInfo();

    /**
     * Returns the settings file of the addon.
     * @return settings file
     */
    @NotNull
    Settings getSettings();

    /**
     * Returns the execution directory of the addon.
     * @return execution directory
     */
    @NotNull File getDir();

    /**
     * Returns the time at which the addon was started.
     *
     * If the addon is currently not active, this method
     * will return -1.
     *
     * @return current activation time
     */
    long getActivationTime();
}
