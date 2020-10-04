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

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * The addon info file contains general information about the addon
 * retrieved from the addon info file.
 */
public interface AddonInfo {

    /**
     * Returns main class of the addon.
     * @return main class
     */
    @NotNull
    String getMainClass();

    /**
     * Returns the formatted version specifier if the addon.
     * @return version
     */
    @NotNull
    String getVersion();

    /**
     * Returns the author(s) of the addon
     * @return author(s)
     */
    @NotNull
    String[] getAuthor();

    /**
     * Returns the name of the addon.
     * @return addon name
     */
    @NotNull
    String getSimpleName();

    /**
     * Returns the jar file of the addon.
     * @return jar file
     */
    File jarFile();

    /**
     * Returns the addon manager class
     * @return manager class
     */
    Class<? extends AddonManager> managerClass();
}
