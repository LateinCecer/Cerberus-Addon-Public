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
import com.cerberustek.events.AddonLoadEvent;
import com.cerberustek.events.AddonUnloadEvent;
import com.cerberustek.exception.AddonInfoLoadException;
import com.cerberustek.exception.AddonLoadException;
import com.cerberustek.service.TerminalUtil;
import com.cerberustek.settings.Settings;
import com.cerberustek.CerberusAddon;
import com.cerberustek.addon.Addon;
import com.cerberustek.addon.AddonInfo;
import com.cerberustek.addon.AddonManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class JavaAddonManager implements AddonManager {

    private final HashMap<AddonInfo, JavaAddon> addons = new HashMap<>();

    private File dir;
    private File runDir;
    private String infoFileName;

    private CerberusAddon addon;
    private CerberusEvent event;

    public JavaAddonManager() {
        dir = new File("addons/java/");
        runDir = new File("addons/run/");
    }
    
    @Override
    public @NotNull File addonDir() {
        return dir;
    }

    @Override
    public @NotNull Addon loadAddon(@NotNull AddonInfo info) throws AddonLoadException {
        if (!(info instanceof JavaAddonInfo))
            throw new AddonLoadException(info);

        if (addons.containsKey(info))
            return addons.get(info);

        if (!getEventService().executeShortEIF(new AddonLoadEvent(info)))
            throw new AddonLoadException(info);

        try {
            Constructor<?> constructor = ((JavaAddonInfo) info).getJavaClass().getConstructor();
            Object instance = constructor.newInstance();

            File pluginDir = runDir.toPath().resolve(info.getSimpleName()).toFile();
            if (!pluginDir.exists() || !pluginDir.isDirectory()) {
                if (!pluginDir.mkdirs()) {
                    CerberusRegistry.getInstance().debug("Could not locate or initialize addon directory for addon " +
                            info.getSimpleName());
                } else {
                    CerberusRegistry.getInstance().debug("Successfully created addon directory for addon " +
                            info.getSimpleName());
                }
            }
            JavaAddon addon = new JavaAddon(instance, (JavaAddonInfo) info, pluginDir);
            addon.init();

            addons.put(info, addon);
            return addon;
        } catch (NoSuchMethodException e) {
            CerberusRegistry.getInstance().debug("Could not find fitting constructor of main class for addon "
                        + info.getSimpleName());
        } catch (IllegalAccessException e) {
            CerberusRegistry.getInstance().debug("Could not access constructor of main class for addon "
                        + info.getSimpleName() + "; constructor is not visible");
        } catch (InstantiationException e) {
            CerberusRegistry.getInstance().debug("Failed to create an instance of addon " + info.getSimpleName());
        } catch (InvocationTargetException e) {
            CerberusRegistry.getInstance().debug("Failed to invoke constructor of main class from addon "
                        + info.getSimpleName());
        }
        throw new AddonLoadException(info);
    }

    @Override
    public @Nullable Addon getAddon(@NotNull AddonInfo info) {
        return addons.get(info);
    }

    @Override
    public @NotNull AddonInfo loadInfo(@NotNull File file) throws AddonInfoLoadException {
        try (JarInputStream inputStream = new JarInputStream(new FileInputStream(file))) {

            ZipEntry current;
            JavaAddonInfo info = null;

            while ((current = inputStream.getNextEntry()) != null) {
                if (current.getName().equals(infoFileName)) {
                    info = JavaAddonInfo.readInfo(file, inputStream);
                    break;
                }
            }

            if (info == null) {
                CerberusRegistry.getInstance().warning("Unable to load addon info file from addon located at \""
                            + file.getAbsolutePath() + "\"");
                throw new AddonInfoLoadException(file);
            }
            return info;
        } catch (FileNotFoundException e) {
            CerberusRegistry.getInstance().warning("Addon file \"" + file.getAbsolutePath() + "\" does not exist");
        } catch (IOException e) {
            CerberusRegistry.getInstance().warning("Unable to access addon file \"" + file.getAbsolutePath() + "\"" +
                    ". Either the file is corrupted or currently used by an other program");
        }
        throw new AddonInfoLoadException(file);
    }

    @Override
    public void unload(@NotNull AddonInfo info) {
        JavaAddon addon = addons.get(info);
        if (addon != null && getEventService().executeShortEIF(new AddonUnloadEvent(info))) {
            addon.destroy();
            addons.remove(info);
        }
    }

    @Override
    public void unloadAll() {
        addons.values().forEach(JavaAddon::destroy);
        addons.clear();
    }

    @Override
    public void reload() {
        unloadAll();

        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file == null)
                continue;

            try {
                AddonInfo info = loadInfo(file);
                loadAddon(info).enable();
            } catch (AddonInfoLoadException | AddonLoadException ignore) {}
        }
    }

    @Override
    public void reload(@NotNull AddonInfo info) {
        JavaAddon addon = addons.get(info);
        if (addon == null) {
            try {
                addon = (JavaAddon) loadAddon(info);
                addon.enable();
            } catch (AddonLoadException e) {
                CerberusRegistry.getInstance().warning("Unable to load addon " + TerminalUtil.ANSI_BLUE
                        + info.getSimpleName() + TerminalUtil.ANSI_RESET);
            }
            return;
        }

        addon.destroy();
        addon.init();
        addon.enable();
    }

    @Override
    public Collection<AddonInfo> getAddonInfo() {
        return addons.keySet();
    }

    @Override
    public void destroy() {
        unloadAll();
    }

    @Override
    public void init() {
        Settings settings = getAddonService().getSettings();
        dir = new File(settings.getString("manager_java_jars", "addons/java/"));
        runDir = new File(settings.getString("manager_java_run", "addons/run/"));

        if (!dir.exists() || !dir.isDirectory()) {
            if (dir.mkdirs()) {
                CerberusRegistry.getInstance().debug("Successfully initialized addons jar directory in \""
                        + dir.getAbsolutePath() + "\"");
            } else {
                CerberusRegistry.getInstance().warning("Could not locate or initialize addons jar directory");
            }
        }

        if (!runDir.exists() || !runDir.isDirectory()) {
            if (runDir.mkdirs()) {
                CerberusRegistry.getInstance().debug("Successfully initialized addons run directory in \""
                        + runDir.getAbsolutePath() + "\"");
            } else {
                CerberusRegistry.getInstance().warning("Could not locate or initialize addons run directory");
            }
        }

        infoFileName = settings.getString("info_file", "addon.info");
        reload();
    }
    
    private CerberusAddon getAddonService() {
        if (addon == null)
            addon = CerberusRegistry.getInstance().getService(CerberusAddon.class);
        return addon;
    }

    private CerberusEvent getEventService() {
        if (event == null)
            event = CerberusRegistry.getInstance().getService(CerberusEvent.class);
        return event;
    }
}
