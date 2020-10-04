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

package com.cerberustek.commands;

import com.cerberustek.CerberusRegistry;
import com.cerberustek.service.TerminalUtil;
import com.cerberustek.service.terminal.TerminalCommand;
import com.cerberustek.usr.PermissionHolder;
import com.cerberustek.CerberusAddon;
import com.cerberustek.addon.Addon;
import com.cerberustek.addon.AddonInfo;
import com.cerberustek.addon.AddonManager;
import com.cerberustek.exception.AddonLoadException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class AddonCommand implements TerminalCommand {

    @Override
    public boolean execute(PermissionHolder permissionHolder, Scanner scanner, String... args) {
        if (args.length < 1)
            return false;

        String subCommand = args[0];
        CerberusRegistry registry = CerberusRegistry.getInstance();

        switch (subCommand.toLowerCase()) {
            case "manager":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_MANAGER))
                    return managerSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "list":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_LIST))
                    return listSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "enable":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_ENABLE))
                    return enableSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "disable":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_DISABLE))
                    return disableSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "reload":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_RELOAD))
                    return reloadSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "unload":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_UNLOAD))
                    return unloadSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            case "status":
                if (permissionHolder.hasPermission(CerberusAddon.PERMISSION_ADDON_STATUS))
                    return statusSubCommand(args);
                else
                    printInsufficientPermission();
                break;
            default:
                registry.warning("Invalid sub command " + args[0]);
        }
        return true;
    }

    private void printInsufficientPermission() {
        CerberusRegistry.getInstance().warning(TerminalUtil.ANSI_RED + "You do not have permission for this command");
    }

    private boolean managerSubCommand(String... args) {
        if (args.length < 2)
            return false;

        CerberusRegistry registry = CerberusRegistry.getInstance();
        CerberusAddon service = registry.getService(CerberusAddon.class);
        switch (args[1].toLowerCase()) {
            case "list":
                Collection<Class<? extends AddonManager>> managers = service.getManagers();
                registry.info("There is a total of " + TerminalUtil.ANSI_BLUE + managers.size()
                        + TerminalUtil.ANSI_RESET+ " addon manager registered. Here is a list:");

                for (Class<? extends AddonManager> m : managers) {
                    registry.info(TerminalUtil.ANSI_PURPLE + "# " + m.getSimpleName() + TerminalUtil.ANSI_RESET);
                }
                break;
            case "reload": {
                if (args.length < 3)
                    return false;

                AddonManager manager = findManager(args[2]);
                if (manager == null)
                    registry.warning("Could not find addon manager with name \"" + TerminalUtil.ANSI_BLUE
                            + args[2] + TerminalUtil.ANSI_RESET + "\". Try " + TerminalUtil.ANSI_BLUE
                            + "addon manager list" + TerminalUtil.ANSI_RESET + " for a list of all registered" +
                            " addon managers");
                else {
                    manager.reload();
                    registry.info("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                            + TerminalUtil.ANSI_RESET + " has been reloaded successfully");
                }
                break;
            }
            case "unload": {
                if (args.length < 3)
                    return false;

                AddonManager manager = findManager(args[2]);
                if (manager == null)
                    registry.warning("Could not find addon manager with name \"" + TerminalUtil.ANSI_BLUE
                            + args[2] + TerminalUtil.ANSI_RESET + "\". Try " + TerminalUtil.ANSI_BLUE
                            + "addon manager list" + TerminalUtil.ANSI_RESET + " for a list of all registered" +
                            " addon managers");
                else {
                    manager.unloadAll();
                    registry.info("Successfully unloaded all addons in addon manager " + TerminalUtil.ANSI_BLUE
                            + manager.getClass().getSimpleName() + TerminalUtil.ANSI_RESET);
                }
                break;
            }
            default:
                return false;
        }
        return true;
    }

    private AddonManager findManager(String simpleName) {
        CerberusAddon service = CerberusRegistry.getInstance().getService(CerberusAddon.class);
        Collection<Class<? extends AddonManager>> classes = service.getManagers();

        for (Class<? extends AddonManager> c : classes) {
            if (c.getName().toLowerCase().contains(simpleName.toLowerCase()))
                return service.getManager(c);
        }
        return null;
    }

    private AddonInfo findAddon(AddonManager manager, String addon) {
        Collection<AddonInfo> infos = manager.getAddonInfo();
        for (AddonInfo info : infos) {
            if (info.getSimpleName().toLowerCase().contains(addon.toLowerCase()))
                return info;
        }
        return null;
    }

    private AddonInfo findAddon(String name) {
        CerberusAddon service = CerberusRegistry.getInstance().getService(CerberusAddon.class);
        Collection<Class<? extends AddonManager>> classes = service.getManagers();

        for (Class<? extends AddonManager> c : classes) {
            AddonInfo info = findAddon(service.getManager(c), name);
            if (info != null)
                return info;
        }
        return null;
    }

    private boolean listSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        CerberusAddon service = registry.getService(CerberusAddon.class);
        Collection<Class<? extends AddonManager>> classes = service.getManagers();

        registry.info("Here is a list of all currently loaded addons:");
        int count = 0;
        for (Class<? extends AddonManager> c : classes) {
            AddonManager manager = service.getManager(c);
            Collection<AddonInfo> infos = manager.getAddonInfo();

            registry.info("\tManager: " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                    + TerminalUtil.ANSI_RESET + ">");
            for (AddonInfo info : infos) {
                Addon addon = manager.getAddon(info);
                registry.info("\t\t" + (addon == null ? TerminalUtil.ANSI_PURPLE :
                        (addon.isActive() ? TerminalUtil.ANSI_GREEN : TerminalUtil.ANSI_RED))
                        + info.getSimpleName() + TerminalUtil.ANSI_RESET + " -v " + TerminalUtil.ANSI_BLUE
                        + info.getVersion() + TerminalUtil.ANSI_RESET);
            }
            count += infos.size();
        }
        registry.info("In total: " + TerminalUtil.ANSI_BLUE + count + TerminalUtil.ANSI_RESET);
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean enableSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        if (args.length > 2) {
            AddonManager manager = findManager(args[1]);
            if (manager == null) {
                registry.warning("Addon manager with name " + TerminalUtil.ANSI_BLUE + args[1]
                        + TerminalUtil.ANSI_RESET + " could not be found");
                return true;
            }

            AddonInfo addon = findAddon(manager, args[2]);
            if (addon == null) {
                registry.warning("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " does not contain addon with name " + TerminalUtil.ANSI_BLUE
                        + args[2] + TerminalUtil.ANSI_RESET);
                return true;
            }

            try {
                Addon a = manager.loadAddon(addon);

                if (a.isActive()) {
                    registry.info("Addon " + TerminalUtil.ANSI_BLUE
                            + addon.getSimpleName() + TerminalUtil.ANSI_RESET + " is already enabled");
                } else {
                    a.enable();
                    registry.info("Successfully enabled addon " + TerminalUtil.ANSI_BLUE
                            + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
                }
            } catch (AddonLoadException e) {
                registry.warning("Failed to load addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET);
            }
        } else if (args.length > 1) {
            AddonInfo addon = findAddon(args[1]);
            if (addon == null) {
                registry.warning("Could not find addon with name " + TerminalUtil.ANSI_BLUE
                        + args[1] + TerminalUtil.ANSI_RESET);
                return true;
            }

            CerberusAddon service = registry.getService(CerberusAddon.class);
            Addon a = service.loadAddon(addon);
            if (a == null) {
                registry.warning("Failed to load addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET);
                return true;
            }

            if (a.isActive()) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET + " is already enabled");
            } else {
                a.enable();
                registry.info("Successfully enabled addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
            }
        } else
            return false;
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean disableSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        if (args.length > 2) {
            AddonManager manager = findManager(args[1]);
            if (manager == null) {
                registry.warning("Addon manager with name " + TerminalUtil.ANSI_BLUE + args[1]
                        + TerminalUtil.ANSI_RESET + " could not be found");
                return true;
            }

            AddonInfo addon = findAddon(manager, args[2]);
            if (addon == null) {
                registry.warning("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " does not contain addon with name " + TerminalUtil.ANSI_BLUE
                        + args[2] + TerminalUtil.ANSI_RESET);
                return true;
            }

            Addon a = manager.getAddon(addon);
            if (a == null || !a.isActive()) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET + " is already disabled");
            } else {
                a.disable();
                registry.info("Successfully disabled addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
            }
        } else if (args.length > 1) {
            AddonInfo addon = findAddon(args[1]);
            if (addon == null) {
                registry.warning("Could not find addon with name " + TerminalUtil.ANSI_BLUE
                        + args[1] + TerminalUtil.ANSI_RESET);
                return true;
            }

            CerberusAddon service = registry.getService(CerberusAddon.class);
            Addon a = service.getAddon(addon);
            if (a == null || !a.isActive()) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET + " is already disabled");
                return true;
            }

            a.disable();
            registry.info("Successfully disabled addon " + TerminalUtil.ANSI_BLUE
                    + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
        } else
            return false;
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean unloadSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        if (args.length > 2) {
            AddonManager manager = findManager(args[1]);
            if (manager == null) {
                registry.warning("Addon manager with name " + TerminalUtil.ANSI_BLUE + args[1]
                        + TerminalUtil.ANSI_RESET + " could not be found");
                return true;
            }

            AddonInfo addon = findAddon(manager, args[2]);
            if (addon == null) {
                registry.warning("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " does not contain addon with name " + TerminalUtil.ANSI_BLUE
                        + args[2] + TerminalUtil.ANSI_RESET);
                return true;
            }

            Addon a = manager.getAddon(addon);
            if (a == null || !a.isActive()) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET + " is already unloaded");
            } else {
                manager.unload(addon);
                registry.info("Successfully unloaded addon " + TerminalUtil.ANSI_BLUE
                        + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
            }
        } else if (args.length > 1) {
            AddonInfo addon = findAddon(args[1]);
            if (addon == null) {
                registry.warning("Could not find addon with name " + TerminalUtil.ANSI_BLUE
                        + args[1] + TerminalUtil.ANSI_RESET);
                return true;
            }

            CerberusAddon service = registry.getService(CerberusAddon.class);
            Addon a = service.getAddon(addon);
            if (a == null) {
                registry.warning("Addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET + " is already unloaded");
                return true;
            }

            AddonManager manager = service.getManager(addon.managerClass());
            manager.unload(addon);
            registry.info("Successfully unloaded addon " + TerminalUtil.ANSI_BLUE
                    + addon.getSimpleName() + TerminalUtil.ANSI_RESET);
        } else
            return false;
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean reloadSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        if (args.length > 2) {
            AddonManager manager = findManager(args[1]);
            if (manager == null) {
                registry.warning("Addon manager with name " + TerminalUtil.ANSI_BLUE + args[1]
                        + TerminalUtil.ANSI_RESET + " could not be found");
                return true;
            }

            AddonInfo addon = findAddon(manager, args[2]);
            if (addon == null) {
                registry.warning("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " does not contain addon with name " + TerminalUtil.ANSI_BLUE
                        + args[2] + TerminalUtil.ANSI_RESET);
                return true;
            }

            manager.reload(addon);
            registry.info("Successfully reloaded addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                    + TerminalUtil.ANSI_RESET);
        } else if (args.length > 1) {
            AddonInfo addon = findAddon(args[1]);
            if (addon == null) {
                registry.warning("Could not find addon with name " + TerminalUtil.ANSI_BLUE
                        + args[1] + TerminalUtil.ANSI_RESET);
                return true;
            }

            CerberusAddon service = registry.getService(CerberusAddon.class);
            AddonManager manager = service.getManager(addon.managerClass());
            manager.reload(addon);
            registry.info("Successfully reloaded addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                    + TerminalUtil.ANSI_RESET);
        } else {
            CerberusAddon service = registry.getService(CerberusAddon.class);
            Collection<Class<? extends AddonManager>> managers = service.getManagers();

            for (Class<? extends AddonManager> c : managers) {
                AddonManager manager = service.getManager(c);
                manager.reload();
                registry.info("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " has been reloaded successfully");
            }
            registry.info("Successfully reloaded all addon managers");
            return true;
        }
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean statusSubCommand(String... args) {
        CerberusRegistry registry = CerberusRegistry.getInstance();
        if (args.length > 2) {
            AddonManager manager = findManager(args[1]);
            if (manager == null) {
                registry.warning("Addon manager with name " + TerminalUtil.ANSI_BLUE + args[1]
                        + TerminalUtil.ANSI_RESET + " could not be found");
                return true;
            }

            AddonInfo addon = findAddon(manager, args[2]);
            if (addon == null) {
                registry.warning("Addon manager " + TerminalUtil.ANSI_BLUE + manager.getClass().getSimpleName()
                        + TerminalUtil.ANSI_RESET + " does not contain addon with name " + TerminalUtil.ANSI_BLUE
                        + args[2] + TerminalUtil.ANSI_RESET);
                return true;
            }

            Addon a = manager.getAddon(addon);
            if (a == null) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET + " is currently not loaded");
                return true;
            }

            registry.info("Here are some infos about the addon " + TerminalUtil.ANSI_BLUE
                    + addon.getSimpleName() + TerminalUtil.ANSI_RESET + ":");
            if (a.isActive())
                registry.info("\tStatus> " + TerminalUtil.ANSI_GREEN + "Active" + TerminalUtil.ANSI_RESET);
            else
                registry.info("\tStatus> " + TerminalUtil.ANSI_RED + "Deactivated" + TerminalUtil.ANSI_RESET);
            registry.info("\tVersion> " + TerminalUtil.ANSI_BLUE + addon.getVersion() + TerminalUtil.ANSI_RESET);
            registry.info("\tAuthors> " + TerminalUtil.ANSI_CYAN + Arrays.toString(addon.getAuthor())
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tThreads> " + TerminalUtil.ANSI_BLUE + a.getThreadList().size()
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tManager> " + TerminalUtil.ANSI_BLUE + addon.managerClass().getSimpleName()
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tDirectory> \"" + TerminalUtil.ANSI_BLUE + a.getDir().getPath() + TerminalUtil.ANSI_RESET
                    + "\"");
            if (a.isActive())
                registry.info("\tOnline since> " + TerminalUtil.getInstance().formatTime(a.getActivationTime()));

        } else if (args.length > 1) {
            AddonInfo addon = findAddon(args[1]);
            if (addon == null) {
                registry.warning("Could not find addon with name " + TerminalUtil.ANSI_BLUE
                        + args[1] + TerminalUtil.ANSI_RESET);
                return true;
            }

            CerberusAddon service = registry.getService(CerberusAddon.class);
            Addon a = service.getAddon(addon);
            if (a == null) {
                registry.info("Addon " + TerminalUtil.ANSI_BLUE + addon.getSimpleName()
                        + TerminalUtil.ANSI_RESET + " is currently not loaded");
                return true;
            }

            registry.info("Here are some infos about the addon " + TerminalUtil.ANSI_BLUE
                    + addon.getSimpleName() + TerminalUtil.ANSI_RESET + ":");
            if (a.isActive())
                registry.info("\tStatus> " + TerminalUtil.ANSI_GREEN + "Active" + TerminalUtil.ANSI_RESET);
            else
                registry.info("\tStatus> " + TerminalUtil.ANSI_RED + "Deactivated" + TerminalUtil.ANSI_RESET);
            registry.info("\tVersion> " + TerminalUtil.ANSI_BLUE + addon.getVersion() + TerminalUtil.ANSI_RESET);
            registry.info("\tAuthors> " + TerminalUtil.ANSI_CYAN + Arrays.toString(addon.getAuthor())
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tThreads> " + TerminalUtil.ANSI_BLUE + a.getThreadList().size()
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tManager> " + TerminalUtil.ANSI_BLUE + addon.managerClass().getSimpleName()
                    + TerminalUtil.ANSI_RESET);
            registry.info("\tDirectory> \"" + TerminalUtil.ANSI_BLUE + a.getDir().getPath() + TerminalUtil.ANSI_RESET
                    + "\"");
            if (a.isActive())
                registry.info("\tOnline since> " + TerminalUtil.getInstance().formatTime(a.getActivationTime()));
        } else
            return false;
        return true;
    }



    @Override
    public String executor() {
        return "addon";
    }

    @Override
    public String usage() {
        return "addon <list/load/unload/reload/addon>";
    }

    @Override
    public String requiredPermission() {
        return CerberusAddon.PERMISSION_ADDON;
    }
}
