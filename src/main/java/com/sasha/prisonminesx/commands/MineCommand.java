package com.sasha.prisonminesx.commands;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Handles all structural routing for the core /prisonmines (/pmines) command tree.
 * Interacts heavily with FastAsyncWorldEdit for region selections and GUI Engine for menus.
 */
public class MineCommand implements CommandExecutor {

    private final PrisonMinesX plugin;

    public MineCommand(PrisonMinesX plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Enforce player-only execution as commands rely on Player Location / Inventories
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prisonminesx.cmd.pmine") && !player.hasPermission("prisonminesx.admin")) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            return true;
        }

        // Base command executes Help Menu
        if (args.length == 0) {
            if (player.hasPermission("prisonminesx.admin") || player.hasPermission("prisonminesx.cmd.help")) {
                handleHelp(player, 1);
            } else {
                player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Strict Sub-Command Permission Validation
        if (!player.hasPermission("prisonminesx.admin") && !player.hasPermission("prisonminesx.cmd." + subCommand)) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            return true;
        }

        // Command Routing Switch
        switch (subCommand) {
            case "help":
                handleHelp(player, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "warp":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.tp"));
                    return true;
                }
                Mine wMine = plugin.getMineManager().getMine(args[1]);
                if (wMine != null) {
                    if (player.hasPermission("prisonminesx.admin") || player.hasPermission("prisonminesx.mine.all") || player.hasPermission("prisonminesx.mine." + wMine.getName().toLowerCase())) {
                        plugin.getMineManager().teleportToMine(player, wMine);
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
                    }
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "warpgui":
                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
                    return true;
                }
                List<Mine> accessibleMines = new ArrayList<>();
                for (Mine m : plugin.getMineManager().getMines()) {
                    if (player.hasPermission("prisonminesx.admin") || player.hasPermission("prisonminesx.mine.all") || player.hasPermission("prisonminesx.mine." + m.getName().toLowerCase())) {
                        accessibleMines.add(m);
                    }
                }
                if (accessibleMines.isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-access"));
                    return true;
                }
                MineGUI.openWarpsMenu(player, plugin, args.length > 1 ? parsePage(args[1]) : 1, accessibleMines);
                break;

            case "gui":
                if (args.length > 1) {
                    Mine specificMine = plugin.getMineManager().getMine(args[1]);
                    if (specificMine != null) {
                        MineGUI.openEditMenu(player, specificMine, plugin);
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    }
                    return true;
                }

                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
                    return true;
                }
                MineGUI.openMainMenu(player, plugin, 1);
                break;

            case "list":
                handleList(player, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "timers":
                handleTimers(player, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "stats":
                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
                    return true;
                }
                MineGUI.openStatsMenu(player, plugin, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "reload":
                plugin.reloadPlugin();
                player.sendMessage(getMsg("prefix") + getMsg("admin.reloaded"));
                break;

            case "rename":
                if (args.length < 3) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines rename <old> <new>");
                    return true;
                }
                Mine renameMine = plugin.getMineManager().getMine(args[1]);
                if (renameMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (plugin.getMineManager().getMine(args[2]) != null) {
                    player.sendMessage(getMsg("prefix") + "§cA mine with that name already exists!");
                    return true;
                }
                plugin.getMineManager().renameMine(args[1], args[2]);
                player.sendMessage(getMsg("prefix") + "§7Successfully renamed mine §b" + args[1] + " §7to §b" + args[2] + "§7.");
                break;

            case "setdesc":
                if (args.length < 3) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines setdesc <mine> <description/none>");
                    return true;
                }
                Mine descMine = plugin.getMineManager().getMine(args[1]);
                if (descMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }

                String desc = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (desc.equalsIgnoreCase("none")) {
                    descMine.setDescription(null);
                    plugin.getMineManager().addMine(descMine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.setdesc-cleared").replace("%mine%", descMine.getName()));
                } else {
                    descMine.setDescription(desc);
                    plugin.getMineManager().addMine(descMine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.setdesc-success").replace("%mine%", descMine.getName()));
                }
                break;

            case "start":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.start"));
                    return true;
                }
                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    int startedCount = 0;
                    for (Mine m : plugin.getMineManager().getMines()) {
                        if (m.isPaused()) {
                            com.sasha.prisonminesx.api.events.MineStateChangeEvent stateEvent = new com.sasha.prisonminesx.api.events.MineStateChangeEvent(m, false);
                            org.bukkit.Bukkit.getPluginManager().callEvent(stateEvent);
                            if (stateEvent.isCancelled()) continue;

                            m.setPaused(false);
                            plugin.getMineManager().addMine(m);
                            startedCount++;
                        }
                    }
                    player.sendMessage(getMsg("prefix") + getMsg("admin.started-all").replace("%count%", String.valueOf(startedCount)));
                    return true;
                }

                Mine startMine = plugin.getMineManager().getMine(args[1]);
                if (startMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (!startMine.isPaused()) {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.already-started").replace("%mine%", startMine.getName()));
                } else {
                    com.sasha.prisonminesx.api.events.MineStateChangeEvent stateEvent = new com.sasha.prisonminesx.api.events.MineStateChangeEvent(startMine, false);
                    org.bukkit.Bukkit.getPluginManager().callEvent(stateEvent);
                    if (stateEvent.isCancelled()) return true;

                    startMine.setPaused(false);
                    plugin.getMineManager().addMine(startMine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.started").replace("%mine%", startMine.getName()));
                }
                break;

            case "stop":
                // Logic inverted from 'start' above to pause mine timers
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.stop"));
                    return true;
                }
                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("all")) {
                    int stoppedCount = 0;
                    for (Mine m : plugin.getMineManager().getMines()) {
                        if (!m.isPaused()) {
                            com.sasha.prisonminesx.api.events.MineStateChangeEvent stateEvent = new com.sasha.prisonminesx.api.events.MineStateChangeEvent(m, true);
                            org.bukkit.Bukkit.getPluginManager().callEvent(stateEvent);
                            if (stateEvent.isCancelled()) continue;

                            m.setPaused(true);
                            plugin.getMineManager().addMine(m);
                            stoppedCount++;
                        }
                    }
                    player.sendMessage(getMsg("prefix") + getMsg("admin.stopped-all").replace("%count%", String.valueOf(stoppedCount)));
                    return true;
                }

                Mine stopMine = plugin.getMineManager().getMine(args[1]);
                if (stopMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (stopMine.isPaused()) {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.already-stopped").replace("%mine%", stopMine.getName()));
                } else {
                    com.sasha.prisonminesx.api.events.MineStateChangeEvent stateEvent = new com.sasha.prisonminesx.api.events.MineStateChangeEvent(stopMine, true);
                    org.bukkit.Bukkit.getPluginManager().callEvent(stateEvent);
                    if (stateEvent.isCancelled()) return true;

                    stopMine.setPaused(true);
                    plugin.getMineManager().addMine(stopMine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.stopped").replace("%mine%", stopMine.getName()));
                }
                break;

            case "flags":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.flags"));
                    return true;
                }
                Mine flagsMine = plugin.getMineManager().getMine(args[1]);
                if (flagsMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                MineGUI.openFlagsMenu(player, flagsMine, plugin);
                break;

            case "setschem":
                if (args.length < 3) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.setschem"));
                    return true;
                }
                Mine setSchemMine = plugin.getMineManager().getMine(args[1]);
                if (setSchemMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }

                String schemName = args[2];
                if (!schemName.endsWith(".schem")) schemName += ".schem";

                File schemFile = new File("plugins/FastAsyncWorldEdit/schematics/" + schemName);
                if (!schemFile.exists()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.schematic-not-found").replace("%schem%", schemName));
                    return true;
                }

                setSchemMine.setSchematic(schemName);
                plugin.getMineManager().addMine(setSchemMine);
                player.sendMessage(getMsg("prefix") + getMsg("admin.schematic-set").replace("%schem%", schemName).replace("%mine%", setSchemMine.getName()));

                plugin.getMineManager().resetMine(setSchemMine.getName(), true);
                player.sendMessage(getMsg("prefix") + "§7Mine automatically reset to apply the schematic.");
                break;

            case "clearschem":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.clearschem"));
                    return true;
                }
                Mine clearSchemMine = plugin.getMineManager().getMine(args[1]);
                if (clearSchemMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (clearSchemMine.getSchematic() == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.no-schematic"));
                    return true;
                }

                clearSchemMine.setSchematic(null);
                plugin.getMineManager().addMine(clearSchemMine);
                player.sendMessage(getMsg("prefix") + getMsg("admin.schematic-cleared").replace("%mine%", clearSchemMine.getName()));
                break;

            case "moveholo":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.moveholo"));
                    return true;
                }
                Mine holoMine = plugin.getMineManager().getMine(args[1]);
                if (holoMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (!holoMine.isHologramEnabled()) {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.no-hologram"));
                    return true;
                }

                org.bukkit.Location holoLoc;
                if (args.length >= 5) {
                    try {
                        double x = Double.parseDouble(args[2]);
                        double y = Double.parseDouble(args[3]);
                        double z = Double.parseDouble(args[4]);
                        if (x == 0 && y == 0 && z == 0) {
                            holoLoc = null; // Auto-center reset
                        } else {
                            holoLoc = new org.bukkit.Location(player.getWorld(), x, y, z);
                        }
                    } catch(NumberFormatException e) {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-coords"));
                        return true;
                    }
                } else {
                    holoLoc = player.getLocation();
                }

                holoMine.setHologramLocation(holoLoc);
                plugin.getMineManager().addMine(holoMine);
                plugin.getHologramManager().updateHologram(holoMine);

                if (holoLoc == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.holo-reset").replace("%mine%", holoMine.getName()));
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("admin.holo-moved").replace("%mine%", holoMine.getName()));
                }
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.info"));
                    return true;
                }
                Mine infoMine = plugin.getMineManager().getMine(args[1]);
                if (infoMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                for (String line : plugin.getMessages().getStringList("info-format")) {
                    String status = infoMine.isPaused() ? getMsg("formats.paused") : getMsg("formats.running");
                    String fill = infoMine.isFillMode() ? getMsg("formats.enabled") : getMsg("formats.disabled");
                    String holo = infoMine.isHologramEnabled() ? getMsg("formats.enabled") : getMsg("formats.disabled");
                    String pct = infoMine.getResetPercentage() == -1.0 ? getMsg("formats.default") : infoMine.getResetPercentage() + "%";
                    String linkedSchem = infoMine.getSchematic() == null ? "None" : infoMine.getSchematic();

                    if (line.contains("%composition%")) {
                        if (infoMine.getComposition().isEmpty()) {
                            player.sendMessage(getMsg("formats.comp-empty"));
                        } else {
                            for (Map.Entry<String, Double> entry : infoMine.getComposition().entrySet()) {
                                player.sendMessage(getMsg("formats.comp-entry")
                                        .replace("%block%", formatName(entry.getKey()))
                                        .replace("%chance%", String.valueOf(entry.getValue())));
                            }
                        }
                        continue;
                    }

                    player.sendMessage(line.replace("&", "§")
                            .replace("%mine%", infoMine.getName())
                            .replace("%status%", status)
                            .replace("%world%", infoMine.getWorldName())
                            .replace("%players%", String.valueOf(infoMine.getActivePlayers().size()))
                            .replace("%schematic%", linkedSchem)
                            .replace("%blocks_left%", String.format("%.1f", infoMine.getPercentRemaining()))
                            .replace("%delay%", TimeUtil.formatTime(infoMine.getResetDelay()))
                            .replace("%percent%", pct)
                            .replace("%fill%", fill)
                            .replace("%hologram%", holo));
                }
                break;

            case "tp":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.tp"));
                    return true;
                }
                Mine tpMine = plugin.getMineManager().getMine(args[1]);
                if (tpMine != null) {
                    plugin.getMineManager().teleportToMine(player, tpMine);
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "set":
                if (args.length < 4) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.set"));
                    return true;
                }
                Mine setMine = plugin.getMineManager().getMine(args[1]);
                if (setMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                Material mat = Material.matchMaterial(args[2].toUpperCase());
                if (mat == null || !mat.isBlock()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-block"));
                    return true;
                }
                if (setMine.getComposition().containsKey(mat.name())) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.block-exists").replace("%block%", formatName(mat.name())));
                    return true;
                }

                try {
                    double chance = Double.parseDouble(args[3]);
                    if (chance == 0) {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.cannot-add-zero"));
                        return true;
                    }

                    double currentTotal = setMine.getComposition().entrySet().stream().filter(e -> !e.getKey().equals(mat.name())).mapToDouble(Map.Entry::getValue).sum();
                    double newTotal = currentTotal + chance;

                    if (newTotal > 100.0) {
                        double maxAllowed = 100.0 - currentTotal;
                        if (maxAllowed <= 0.001) {
                            player.sendMessage(getMsg("prefix") + getMsg("commands.mine-full"));
                        } else {
                            player.sendMessage(getMsg("prefix") + getMsg("commands.exceeds-100").replace("%chance%", String.valueOf(chance)));
                            player.sendMessage(getMsg("prefix") + getMsg("commands.max-allowed").replace("%max%", String.format("%.2f", maxAllowed)));
                        }
                        return true;
                    }

                    setMine.addComposition(mat.name(), chance);
                    plugin.getMineManager().addMine(setMine);
                    player.sendMessage(getMsg("prefix") + getMsg("commands.block-added")
                            .replace("%block%", formatName(mat.name()))
                            .replace("%chance%", String.valueOf(chance))
                            .replace("%mine%", setMine.getName()));
                    player.sendMessage(getMsg("prefix") + getMsg("commands.free-space")
                            .replace("%free%", String.format("%.2f", 100.0 - newTotal)));

                    boolean autoReset = plugin.getConfig().getBoolean("settings.auto-reset-on-set", false);
                    boolean onlyWhenFull = plugin.getConfig().getBoolean("settings.only-reset-when-full", true);
                    boolean isFull = Math.abs(newTotal - 100.0) < 0.001;

                    if (autoReset && (!onlyWhenFull || isFull)) {
                        plugin.getMineManager().resetMine(setMine.getName(), false);
                        player.sendMessage(getMsg("prefix") + getMsg("mine.auto-reset"));
                    } else if (isFull) {
                        TextComponent tc = new TextComponent(getMsg("prefix") + getMsg("prompts.mine-full-reset"));
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines reset " + setMine.getName()));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to instantly force reset!").create()));
                        player.spigot().sendMessage(tc);
                    }

                } catch (NumberFormatException e) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-number"));
                }
                break;

            case "unset":
                if (args.length < 3) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.unset"));
                    return true;
                }
                Mine unsetMine = plugin.getMineManager().getMine(args[1]);
                if (unsetMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (args[2].equalsIgnoreCase("all")) {
                    unsetMine.getComposition().clear();
                    plugin.getMineManager().addMine(unsetMine);
                    player.sendMessage(getMsg("prefix") + getMsg("commands.cleared-all").replace("%mine%", unsetMine.getName()));

                    if (plugin.getConfig().getBoolean("settings.auto-reset-on-unset-all", false)) {
                        plugin.getMineManager().resetMine(unsetMine.getName(), false);
                        player.sendMessage(getMsg("prefix") + getMsg("mine.auto-reset"));
                    }
                } else {
                    String targetMat = args[2].toUpperCase();
                    unsetMine.getComposition().remove(targetMat);
                    plugin.getMineManager().addMine(unsetMine);
                    player.sendMessage(getMsg("prefix") + getMsg("commands.block-removed").replace("%block%", formatName(targetMat)).replace("%mine%", unsetMine.getName()));
                }
                break;

            case "create":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.create"));
                    return true;
                }
                if (args[1].length() > 16) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.name-too-long"));
                    return true;
                }

                if (plugin.getMineManager().getMine(args[1]) != null) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.mine-exists"));
                    return true;
                }

                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                Region selection;
                try {
                    selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
                } catch (IncompleteRegionException e) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.we-selection"));
                    return true;
                }

                BlockVector3 min = selection.getMinimumPoint();
                BlockVector3 max = selection.getMaximumPoint();

                Mine tempCheck = new Mine("temp", player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
                for (Mine existing : plugin.getMineManager().getMines()) {
                    if (existing.intersects(tempCheck)) {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.intersects").replace("%mine%", existing.getName()));
                        return true;
                    }
                }

                // Inject Default Flags upon creation
                Mine newMine = new Mine(args[1], player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());

                newMine.setResetDelay(plugin.getConfig().getInt("settings.default-flags.reset-delay", 600));
                newMine.setResetWarnings(plugin.getConfig().getIntegerList("settings.default-flags.reset-warnings"));
                newMine.setResetPercentage(plugin.getConfig().getDouble("settings.default-flags.reset-percentage", 20.0));
                newMine.setResetStyle(plugin.getConfig().getString("settings.default-flags.reset-style", "BOTTOM_UP").toUpperCase());

                String defSurface = plugin.getConfig().getString("settings.default-flags.surface-block", "NONE");
                newMine.setSurface(defSurface.equalsIgnoreCase("NONE") ? null : defSurface);

                newMine.setDisplayItem(plugin.getConfig().getString("settings.default-flags.display-item", "DIAMOND_PICKAXE"));
                newMine.setFillMode(plugin.getConfig().getBoolean("settings.default-flags.fill-mode", false));
                newMine.setSilent(plugin.getConfig().getBoolean("settings.default-flags.silent-reset", false));

                String defWarnMode = plugin.getConfig().getString("settings.default-flags.warn-mode", "GLOBAL").toUpperCase();
                newMine.setWarnMode(defWarnMode);

                newMine.setTeleportOnReset(plugin.getConfig().getBoolean("settings.default-flags.teleport-on-reset", true));
                newMine.setResetSchedules(plugin.getConfig().getStringList("settings.default-flags.daily-reset-schedules"));
                newMine.setHologramEnabled(plugin.getConfig().getBoolean("settings.default-flags.hologram", false));
                newMine.setActionbarEnabled(plugin.getConfig().getBoolean("settings.default-flags.actionbar", false));
                newMine.setPaused(plugin.getConfig().getBoolean("settings.default-flags.paused", false));

                newMine.setMineFly(plugin.getConfig().getBoolean("settings.default-flags.mine-fly", false));
                newMine.setHunger(plugin.getConfig().getBoolean("settings.default-flags.hunger-drain", false));
                newMine.setFallDamage(plugin.getConfig().getBoolean("settings.default-flags.fall-damage", false));
                newMine.setPvp(plugin.getConfig().getBoolean("settings.default-flags.pvp", false));
                newMine.setPlaceBlocks(plugin.getConfig().getBoolean("settings.default-flags.place-blocks", false));

                com.sasha.prisonminesx.api.events.MineCreateEvent createEvent = new com.sasha.prisonminesx.api.events.MineCreateEvent(newMine, player);
                org.bukkit.Bukkit.getPluginManager().callEvent(createEvent);
                if (createEvent.isCancelled()) {
                    player.sendMessage(getMsg("prefix") + "&cMine creation was cancelled by a third-party plugin.");
                    return true;
                }

                plugin.getMineManager().addMine(newMine);
                newMine.calculateTotalBlocks();
                player.sendMessage(getMsg("prefix") + getMsg("admin.created").replace("%mine%", args[1]));
                break;

            case "redefine":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.redefine"));
                    return true;
                }
                Mine redefMine = plugin.getMineManager().getMine(args[1]);
                if (redefMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }

                LocalSession rs = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                Region rSel;
                try {
                    rSel = rs.getSelection(BukkitAdapter.adapt(player.getWorld()));
                } catch (IncompleteRegionException e) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.we-selection"));
                    return true;
                }

                BlockVector3 rMin = rSel.getMinimumPoint();
                BlockVector3 rMax = rSel.getMaximumPoint();

                Mine tempRedef = new Mine("temp", player.getWorld().getName(), rMin.getBlockX(), rMin.getBlockY(), rMin.getBlockZ(), rMax.getBlockX(), rMax.getBlockY(), rMax.getBlockZ());
                for (Mine existing : plugin.getMineManager().getMines()) {
                    if (!existing.getName().equalsIgnoreCase(redefMine.getName()) && existing.intersects(tempRedef)) {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.intersects").replace("%mine%", existing.getName()));
                        return true;
                    }
                }

                com.sasha.prisonminesx.api.events.MineRedefineEvent redefEvent = new com.sasha.prisonminesx.api.events.MineRedefineEvent(redefMine);
                org.bukkit.Bukkit.getPluginManager().callEvent(redefEvent);
                if (redefEvent.isCancelled()) return true;

                // Save local backup file for safety
                File historyFolder = new File(plugin.getDataFolder(), "history");
                if (!historyFolder.exists()) historyFolder.mkdirs();
                File backupFile = new File(historyFolder, redefMine.getName() + "_" + System.currentTimeMillis() + ".yml");
                YamlConfiguration backupConfig = new YamlConfiguration();
                com.sasha.prisonminesx.utils.MineSerializer.serializeToYaml(redefMine, backupConfig);
                try { backupConfig.save(backupFile); } catch (Exception ignored) {}

                redefMine.savePreviousBounds();

                redefMine.setWorldName(player.getWorld().getName());
                redefMine.setMinX(rMin.getBlockX());
                redefMine.setMinY(rMin.getBlockY());
                redefMine.setMinZ(rMin.getBlockZ());
                redefMine.setMaxX(rMax.getBlockX());
                redefMine.setMaxY(rMax.getBlockY());
                redefMine.setMaxZ(rMax.getBlockZ());

                plugin.getHologramManager().removeHologram(redefMine.getName(), redefMine);
                redefMine.calculateTotalBlocks();
                plugin.getMineManager().addMine(redefMine);
                plugin.getMineManager().resetMine(redefMine.getName(), true);

                player.sendMessage(getMsg("prefix") + getMsg("admin.redefined").replace("%mine%", redefMine.getName()));
                break;

            case "undo":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.undo"));
                    return true;
                }
                Mine undoMine = plugin.getMineManager().getMine(args[1]);
                if (undoMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                if (!undoMine.hasPreviousBounds()) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.no-undo"));
                    return true;
                }

                plugin.getHologramManager().removeHologram(undoMine.getName(), undoMine);
                undoMine.restorePreviousBounds();
                undoMine.calculateTotalBlocks();
                plugin.getMineManager().addMine(undoMine);
                plugin.getMineManager().resetMine(undoMine.getName(), true);

                player.sendMessage(getMsg("prefix") + getMsg("admin.undone").replace("%mine%", undoMine.getName()));
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.delete"));
                    return true;
                }
                Mine delMine = plugin.getMineManager().getMine(args[1]);
                if (delMine != null) {
                    com.sasha.prisonminesx.api.events.MineDeleteEvent delEvent = new com.sasha.prisonminesx.api.events.MineDeleteEvent(delMine);
                    org.bukkit.Bukkit.getPluginManager().callEvent(delEvent);
                    if (delEvent.isCancelled()) return true;

                    delMine.getComposition().clear();
                    plugin.getMineManager().addMine(delMine);
                    plugin.getMineManager().resetMine(delMine.getName(), false);
                    plugin.getMineManager().deleteMine(args[1]);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.deleted").replace("%mine%", args[1]));
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "reset":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.reset"));
                    return true;
                }
                if (plugin.getMineManager().getMine(args[1]) != null) {
                    plugin.getMineManager().resetMine(args[1], true);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.forced-reset").replace("%mine%", args[1]));
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "setspawn":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + getMsg("commands.usage.setspawn"));
                    return true;
                }
                Mine mineToTp = plugin.getMineManager().getMine(args[1]);
                if (mineToTp != null) {
                    mineToTp.setTpLocation(player.getLocation());
                    plugin.getMineManager().addMine(mineToTp);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.tp-set").replace("%mine%", args[1]));
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            default:
                player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-args"));
                break;
        }
        return true;
    }

    /**
     * Attempts to parse a string integer for GUI pagination routing.
     * Defaults to 1 if missing or malformed.
     */
    private int parsePage(String arg) {
        try { return Math.max(1, Integer.parseInt(arg)); } catch (NumberFormatException e) { return 1; }
    }

    /**
     * Sends the deeply paginated help text directly to the executor's chat buffer.
     */
    private void handleHelp(Player player, int page) {
        List<String> lines = new ArrayList<>(plugin.getMessages().getStringList("help"));
        lines.sort(String::compareToIgnoreCase);

        int perPage = plugin.getConfig().getInt("settings.help-per-page", 10);
        int totalPages = (int) Math.ceil((double) lines.size() / perPage);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-page").replace("%max%", String.valueOf(totalPages)));
            return;
        }

        player.sendMessage(getMsg("formats.help-header"));
        player.sendMessage(getMsg("formats.help-title").replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(totalPages)));
        player.sendMessage("§7Developed by: §bSashaTheSnep");
        player.sendMessage("§7Discord: §bSashaTheSnep");
        player.sendMessage("");

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, lines.size());

        for (int i = start; i < end; i++) {
            player.sendMessage(lines.get(i).replace("&", "§"));
        }

        TextComponent pagination = new TextComponent("");
        if (page > 1) {
            TextComponent prev = new TextComponent("§8[§c< Previous Page§8] ");
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines help " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for previous page").create()));
            pagination.addExtra(prev);
        }
        if (page < totalPages) {
            TextComponent next = new TextComponent("§8[§aNext Page >§8]");
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines help " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for next page").create()));
            pagination.addExtra(next);
        }

        if (page > 1 || page < totalPages) {
            player.sendMessage("");
            player.spigot().sendMessage(pagination);
        }

        player.sendMessage(getMsg("formats.help-footer"));
    }

    /**
     * Sends the text-based list of active mines mapping to their world locations.
     */
    private void handleList(Player player, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        if (mines.isEmpty()) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
            return;
        }
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalPages = (int) Math.ceil(mines.size() / 10.0);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-page").replace("%max%", String.valueOf(totalPages)));
            return;
        }

        player.sendMessage(getMsg("formats.list-header"));
        player.sendMessage(getMsg("formats.list-title").replace("%count%", String.valueOf(mines.size())).replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(totalPages)));
        player.sendMessage("");

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, mines.size());

        for (int i = start; i < end; i++) {
            Mine m = mines.get(i);
            TextComponent mineComp = new TextComponent(getMsg("formats.list-entry").replace("%mine%", m.getName()).replace("%world%", m.getWorldName()));

            StringBuilder hoverText = new StringBuilder(getMsg("formats.hover-title").replace("%mine%", m.getName()) + "\n");
            hoverText.append(getMsg("formats.hover-status").replace("%status%", m.isPaused() ? getMsg("formats.paused") : getMsg("formats.running"))).append("\n");
            hoverText.append(getMsg("formats.hover-reset").replace("%time%", TimeUtil.formatTime(m.getTimeUntilReset()))).append("\n\n");
            hoverText.append(getMsg("formats.hover-comp-title")).append("\n");

            if (m.getComposition().isEmpty()) hoverText.append(getMsg("formats.comp-empty")).append("\n");
            else {
                for (Map.Entry<String, Double> entry : m.getComposition().entrySet()) {
                    hoverText.append(getMsg("formats.comp-entry").replace("%block%", formatName(entry.getKey())).replace("%chance%", String.valueOf(entry.getValue()))).append("\n");
                }
            }
            hoverText.append("\n").append(getMsg("formats.hover-click"));

            mineComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText.toString()).create()));
            mineComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines tp " + m.getName()));
            player.spigot().sendMessage(mineComp);
        }

        TextComponent pagination = new TextComponent("");
        if (page > 1) {
            TextComponent prev = new TextComponent("§8[§c< Previous Page§8] ");
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines list " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for previous page").create()));
            pagination.addExtra(prev);
        }
        if (page < totalPages) {
            TextComponent next = new TextComponent("§8[§aNext Page >§8]");
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines list " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for next page").create()));
            pagination.addExtra(next);
        }

        if (page > 1 || page < totalPages) {
            player.sendMessage("");
            player.spigot().sendMessage(pagination);
        }

        player.sendMessage(getMsg("formats.list-footer"));
    }

    /**
     * Sends the text-based breakdown of all active mine timers and forces a live countdown snapshot.
     */
    private void handleTimers(Player player, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        if (mines.isEmpty()) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
            return;
        }
        mines.sort(Comparator.comparingInt(Mine::getTimeUntilReset));

        int perPage = plugin.getConfig().getInt("settings.timers-per-page", 10);
        int totalPages = (int) Math.ceil((double) mines.size() / perPage);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-page").replace("%max%", String.valueOf(totalPages)));
            return;
        }

        player.sendMessage(getMsg("formats.timers-header"));
        player.sendMessage(getMsg("formats.timers-title").replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(totalPages)));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, mines.size());

        for (int i = start; i < end; i++) {
            Mine m = mines.get(i);
            player.sendMessage(getMsg("formats.timers-entry").replace("%mine%", m.getName()).replace("%time%", TimeUtil.formatTime(m.getTimeUntilReset())));
        }

        TextComponent pagination = new TextComponent("");
        if (page > 1) {
            TextComponent prev = new TextComponent("§8[§c< Previous Page§8] ");
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines timers " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for previous page").create()));
            pagination.addExtra(prev);
        }
        if (page < totalPages) {
            TextComponent next = new TextComponent("§8[§aNext Page >§8]");
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines timers " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click for next page").create()));
            pagination.addExtra(next);
        }

        if (page > 1 || page < totalPages) {
            player.sendMessage("");
            player.spigot().sendMessage(pagination);
        }

        player.sendMessage(getMsg("formats.timers-footer"));
    }

    private String getMsg(String path) { return plugin.getMessages().getString(path, "&cMissing: " + path).replace("&", "§"); }

    public static String formatName(String name) {
        if (name == null) return "None";
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}