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
import com.cerberustek.Initable;
import com.cerberustek.addon.*;
import com.cerberustek.events.AddonDisableEvent;
import com.cerberustek.events.AddonEnableEvent;
import com.cerberustek.events.ExceptionEvent;
import com.cerberustek.settings.Settings;
import com.cerberustek.settings.impl.SettingsImpl;
import com.cerberustek.CerberusAddon;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

public class JavaAddon implements Addon, Initable {

    private final Object instance;
    private final JavaAddonInfo info;
    private final File dir;
    private final Settings settings;

    private long activationTime;
    private boolean active;

    public JavaAddon(Object instance, JavaAddonInfo info, File dir) {
        this.instance = instance;
        this.info = info;
        this.dir = dir;

        active = false;
        settings = new SettingsImpl(dir.toPath().resolve("settings.properties").toFile(), false);
    }

    @Override
    public void init() {
        settings.init();

        Field infoField = findField(AddonField.INFO);
        Field settingsField = findField(AddonField.SETTINGS);
        Field dirField = findField(AddonField.DIRECTORY);

        if (infoField != null) {
            try {
                infoField.set(instance, info);
            } catch (IllegalAccessException e) {
                CerberusRegistry.getInstance().warning("Unable to set info field for addon " + info.getSimpleName());
            }
        }

        if (settingsField != null) {
            try {
                settingsField.set(instance, settings);
            } catch (IllegalAccessException e) {
                CerberusRegistry.getInstance().warning("Unable to set settings field for addon " + info.getSimpleName());
            }
        }

        if (dirField != null) {
            try {
                dirField.set(instance, dir);
            } catch (IllegalAccessException e) {
                CerberusRegistry.getInstance().warning("Unable to set directory field for addon " + info.getSimpleName());
            }
        }
    }

    private Method findMethod(AddonMethod handle) {
        Class<?> clazz = info.getJavaClass();
        Method[] methods = clazz.getMethods();

        for (Method m : methods) {
            if (m.isAnnotationPresent(AddonHandler.class)) {
                AddonHandler handler = m.getAnnotation(AddonHandler.class);
                if (handler.value() == handle)
                    return m;
            }
        }
        return null;
    }

    private Field findField(AddonField handle) {
        Class<?> clazz = info.getJavaClass();
        Field[] fields = clazz.getFields();

        for (Field f : fields) {
            if (f.isAnnotationPresent(FieldHandler.class) && f.getType().equals(handle.getReturnType())) {
                FieldHandler handler = f.getAnnotation(FieldHandler.class);
                if (handler.value() == handle)
                    return f;
            }
        }
        return null;
    }

    private boolean invokeMethod(AddonMethod handle) {
        Method m = findMethod(handle);
        if (m == null)
            return false;

        try {
            m.invoke(instance);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            CerberusRegistry.getInstance().getService(CerberusEvent.class)
                    .executeFullEIF(new ExceptionEvent(CerberusAddon.class, e));
        }
        return false;
    }

    @Override
    public boolean enable() {
        if (!CerberusRegistry.getInstance().getService(CerberusEvent.class)
                .executeShortEIF(new AddonEnableEvent(info)))
            return false;

        active = true;
        activationTime = System.currentTimeMillis();
        return invokeMethod(AddonMethod.ENABLE);
    }

    @Override
    public boolean disable() {
        if (!CerberusRegistry.getInstance().getService(CerberusEvent.class)
                .executeShortEIF(new AddonDisableEvent(info)))
            return false;

        active = false;
        activationTime = -1;
        return invokeMethod(AddonMethod.DISABLE);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public @NotNull Collection<Thread> getThreadList() {
        Method m = findMethod(AddonMethod.THREADS);
        if (m == null || !m.getReturnType().isAssignableFrom(Collection.class))
            return new HashSet<>();

        try {
            Object obj = m.invoke(instance);
            if (obj instanceof Collection)
                //noinspection unchecked
                return (Collection<Thread>) obj;
            else {
                CerberusRegistry.getInstance().warning("Thread list method has the wrong return type");
                return new HashSet<>();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            CerberusRegistry.getInstance().getService(CerberusEvent.class)
                    .executeFullEIF(new ExceptionEvent(CerberusAddon.class, e));
        }
        return new HashSet<>();
    }

    @Override
    public @NotNull AddonInfo getInfo() {
        return info;
    }

    @Override
    public @NotNull Settings getSettings() {
        return settings;
    }

    @Override
    public @NotNull File getDir() {
        return dir;
    }

    @Override
    public long getActivationTime() {
        return activationTime;
    }

    @Override
    public void destroy() {
        if (isActive())
            disable();
        settings.destroy();
    }
}
