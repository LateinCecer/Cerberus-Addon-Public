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

package com.cerberustek;

import com.cerberustek.addon.Addon;
import com.cerberustek.addon.AddonInfo;
import com.cerberustek.addon.AddonManager;
import com.cerberustek.commands.AddonCommand;
import com.cerberustek.events.ExceptionEvent;
import com.cerberustek.exception.AddonLoadException;
import com.cerberustek.service.CerberusService;
import com.cerberustek.settings.Settings;
import com.cerberustek.settings.impl.SettingsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CerberusAddon implements CerberusService {

    public static final String PERMISSION_ADDON = "de.cerberus.addon";
    public static final String PERMISSION_MANAGER = PERMISSION_ADDON + ".manager";
    public static final String PERMISSION_ADDON_LIST = PERMISSION_ADDON + ".list";
    public static final String PERMISSION_ADDON_ENABLE = PERMISSION_ADDON + ".enable";
    public static final String PERMISSION_ADDON_DISABLE = PERMISSION_ADDON + ".disable";
    public static final String PERMISSION_ADDON_STATUS = PERMISSION_ADDON + ".status";
    public static final String PERMISSION_ADDON_UNLOAD = PERMISSION_ADDON + ".unload";
    public static final String PERMISSION_ADDON_RELOAD = PERMISSION_ADDON + ".reload";

    public static final String SETTINGS_PATH = "config/addon.properties";

    private final Settings settings;
    private final HashMap<Class<? extends AddonManager>, AddonManager> managers = new HashMap<>();
    private final AddonCommand addonCommand;

    public CerberusAddon() {
        settings = new SettingsImpl(new File(SETTINGS_PATH), false);
        addonCommand = new AddonCommand();
    }

    @Override
    public void start() {
        // load settings
        settings.init();

        List<Object> defClasses = new ArrayList<>();
        defClasses.add("de.cerberus.addon.java.JavaAddonManager");

        Object obj = settings.getObject("managers", defClasses);
        if (obj instanceof List) {
            for (Object o : (List<?>) obj) {
                if (o instanceof String) {
                    Class<? extends AddonManager> clazz = loadClass((String) o);
                    if (clazz != null)
                        getManager(clazz);
                }
            }
        } else if (obj instanceof String) {
            Class<? extends AddonManager> clazz = loadClass((String) obj);
            if (clazz != null)
                getManager(clazz);
        } else
            CerberusRegistry.getInstance().warning("Failed to load default addon managers");

        // register comments
        CerberusRegistry.getInstance().registerTerminalCommand(addonCommand);
    }

    private Class<? extends AddonManager> loadClass(String loadClass) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Class<?> clazz = classLoader.loadClass(loadClass);

            if (AddonManager.class.isAssignableFrom(clazz))
                //noinspection unchecked
                return (Class<? extends AddonManager>) clazz;
            CerberusRegistry.getInstance().warning("specified addon manager class " + loadClass + " is not" +
                    " assignable to AddonManager.class");
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void stop() {
        // unload comments
        CerberusRegistry.getInstance().unregisterTerminalCommand(addonCommand);

        // unload addon managers
        managers.values().forEach(AddonManager::destroy);

        // save settings
        settings.destroy();
    }

    @Override
    public Class<? extends CerberusService> serviceClass() {
        return CerberusAddon.class;
    }

    @Override
    public Collection<Thread> getThreads() {
        HashSet<Thread> threads = new HashSet<>();
        for (AddonManager manager : managers.values()) {
            for (AddonInfo info : manager.getAddonInfo()) {
                Addon addon = manager.getAddon(info);
                if (addon != null && addon.isActive())
                    threads.addAll(addon.getThreadList());
            }
        }
        return threads;
    }

    /**
     * Returns the central settings file for the cerberus addon
     * service.
     * @return setting
     */
    public @NotNull Settings getSettings() {
        return settings;
    }

    /**
     * Returns the addon manager based on the manager class
     * @param clazz manager class
     * @param <T> addon type
     * @return addon manager
     */
    public <T extends AddonManager> @NotNull T getManager(@NotNull Class<T> clazz) {
        AddonManager manager = managers.get(clazz);
        if (manager == null) {
            try {
                Constructor<T> constructor = clazz.getConstructor();
                manager = constructor.newInstance();
                managers.put(clazz, manager);
                manager.init();
            } catch (NoSuchMethodException | IllegalAccessException
                    | InstantiationException | InvocationTargetException e) {
                CerberusRegistry.getInstance().warning("Failed to create addon manager");
                CerberusRegistry.getInstance().getService(CerberusEvent.class)
                        .executeFullEIF(new ExceptionEvent(CerberusAddon.class, e));
                throw new IllegalArgumentException("Addon manager class " + clazz.getName() + " can not be initiated");
            }
        }

        try {
            return clazz.cast(manager);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Addon manager has the wrong class");
        }
    }

    /**
     * Will remove and destroy the addon manager associated with the manager
     * class.
     * @param clazz manager class
     * @param <T> manager type
     */
    public <T extends AddonManager> void removeManager(@NotNull Class<T> clazz) {
        AddonManager manager = managers.remove(clazz);
        if (manager != null)
            manager.destroy();
    }

    /**
     * Returns true, if the addon manager is currently loaded.
     * @param addonManager addon manager
     * @return is loaded
     */
    public boolean hasManager(@NotNull Class<? extends AddonManager> addonManager) {
        return managers.containsKey(addonManager);
    }

    /**
     * Returns a collection of the addon manager classes of the managers that
     * are currently loaded.
     * @return currently loaded addon managers
     */
    public @NotNull Collection<Class<? extends AddonManager>> getManagers() {
        return managers.keySet();
    }

    /**
     * Returns the addon for an addon info object.
     *
     * If the addon is currently not loaded, this method will
     * return null.
     *
     * @param info addon info
     * @return addon
     */
    public @Nullable Addon getAddon(@NotNull AddonInfo info) {
        AddonManager manager = managers.get(info.managerClass());
        if (manager == null)
            return null;

        return manager.getAddon(info);
    }

    /**
     * Will load the addon for the addon info object.
     *
     * If the addon is already loaded, this method will just return
     * the loaded addon instance.
     *
     * @param info addon info to load
     * @return addon
     */
    public @Nullable Addon loadAddon(@NotNull AddonInfo info) {
        try {
            return getManager(info.managerClass()).loadAddon(info);
        } catch (AddonLoadException e) {
            return null;
        }
    }

    /**
     * Will unload an addon.
     * @param info addon info
     */
    public void unloadAddon(@NotNull AddonInfo info) {
        AddonManager manager = managers.get(info.managerClass());
        if (manager == null)
            return;

        manager.unload(info);
    }
}
