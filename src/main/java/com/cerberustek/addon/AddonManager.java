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
import com.cerberustek.Initable;
import com.cerberustek.exception.AddonInfoLoadException;
import com.cerberustek.exception.AddonLoadException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

public interface AddonManager extends Initable, Destroyable {

    /**
     * Returns the file in with the addon files are stored for this addon
     * manager.
     * @return addon directory
     */
    @NotNull File addonDir();

    /**
     * Will load an addon from an addon info object.
     * @param info addon info
     * @return addon
     * @throws AddonLoadException exception thrown, if loading the
     *          addon failed.
     */
    @NotNull Addon loadAddon(@NotNull AddonInfo info) throws AddonLoadException;

    /**
     * Returns the addon associated with the addon info.
     *
     * If the appropriate addon is currently not loaded, this method will
     * return null.
     *
     * @param info addon info
     * @return addon associated with the addon info
     */
    @Nullable Addon getAddon(@NotNull AddonInfo info);

    /**
     * Will load the addon info file from an addon file.
     * @param file addon file
     * @return addon info
     * @throws AddonInfoLoadException exception thrown, if the info
     *          file could not be loaded.
     */
    @NotNull AddonInfo loadInfo(@NotNull File file) throws AddonInfoLoadException;

    /**
     * Will unload an addon based in the addon info file.
     *
     * If the addon is currently enabled, the addon will be disabled and
     * unloaded.
     *
     * @param info info to unload
     */
    void unload(@NotNull AddonInfo info);

    /**
     * Will unload all currently loaded addons.
     */
    void unloadAll();

    /**
     * Will reload all addons from the main addon directory.
     */
    void reload();

    /**
     * Will reload the specified addon
     * @param info addon
     */
    void reload(@NotNull AddonInfo info);

    /**
     * Returns the addon info collection
     * @return addon info
     */
    Collection<AddonInfo> getAddonInfo();
}
