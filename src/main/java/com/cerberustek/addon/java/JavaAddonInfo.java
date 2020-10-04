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

package com.cerberustek.addon.java;

import com.cerberustek.CerberusEvent;
import com.cerberustek.CerberusRegistry;
import com.cerberustek.events.ExceptionEvent;
import com.cerberustek.CerberusAddon;
import com.cerberustek.addon.AddonInfo;
import com.cerberustek.addon.AddonManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class JavaAddonInfo implements AddonInfo {

    private final File file;
    private final Class<?> clazz;
    private final String version;
    private final String[] author;
    private final String name;

    public JavaAddonInfo(File file, Class<?> clazz, String name, String version, String... author) {
        this.file = file;
        this.clazz = clazz;
        this.version = version;
        this.author = author;
        this.name = name;
    }

    @Override
    public @NotNull String getMainClass() {
        return clazz.getName();
    }

    public @NotNull Class<?> getJavaClass() {
        return clazz;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public @NotNull String[] getAuthor() {
        return author;
    }

    @Override
    public @NotNull String getSimpleName() {
        return name;
    }

    @Override
    public File jarFile() {
        return file;
    }

    @Override
    public Class<? extends AddonManager> managerClass() {
        return JavaAddonManager.class;
    }

    /**
     * Will attempt to read the info file from the specified input stream.
     * @param inputStream input stream
     * @return into file
     */
    public static @Nullable JavaAddonInfo readInfo(File file, InputStream inputStream) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String classTag = null;
            String name = null;
            String version = null;
            ArrayList<String> authors = new ArrayList<>();

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                switch (count) {
                    case 0:
                        classTag = line;
                        break;
                    case 1:
                        name = line;
                        break;
                    case 2:
                        version = line;
                        break;
                    default:
                        authors.add(line);
                }
                count++;
            }

            // check if enough arguments are present
            if (count < 4)
                return null;

            // load class
            URL url = file.toURI().toURL();
            URLClassLoader cl = new URLClassLoader(new URL[]{url});
            Class<?> clazz = cl.loadClass(classTag);

            String[] authorArray = new String[authors.size()];
            authors.toArray(authorArray);

            return new JavaAddonInfo(file, clazz, name, version, authorArray);
        } catch (IOException | ClassNotFoundException e) {
            CerberusRegistry.getInstance().getService(CerberusEvent.class)
                    .executeFullEIF(new ExceptionEvent(CerberusAddon.class, e));
        }
        return null;
    }
}
