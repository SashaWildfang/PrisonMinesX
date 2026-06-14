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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MineCommand implements CommandExecutor {

    private final PrisonMinesX plugin;

    public MineCommand(PrisonMinesX plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prisonminesx.cmd.pmine") && !player.hasPermission("prisonminesx.admin")) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (player.hasPermission("prisonminesx.admin") || player.hasPermission("prisonminesx.cmd.help")) {
                sendHelp(player);
            } else {
                player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (!player.hasPermission("prisonminesx.admin") && !player.hasPermission("prisonminesx.cmd." + subCommand)) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-permission"));
            return true;
        }

        switch (subCommand) {
            case "help":
                sendHelp(player);
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
                player.sendMessage(getMsg("formats.stats-header"));
                player.sendMessage(getMsg("formats.stats-title"));

                for (Mine m : plugin.getMineManager().getMines()) {
                    long totalMined = m.getLifetimeMinedBlocks();
                    int totalResets = m.getLifetimeResets();
                    double avgPerReset = totalResets == 0 ? totalMined : (double) totalMined / totalResets;

                    player.sendMessage(getMsg("formats.stats-entry")
                            .replace("%mine%", m.getName())
                            .replace("%mined%", String.valueOf(totalMined))
                            .replace("%resets%", String.valueOf(totalResets))
                            .replace("%avg%", String.format("%.1f", avgPerReset)));
                }
                player.sendMessage(getMsg("formats.stats-footer"));
                break;

            case "reload":
                plugin.reloadPlugin();
                player.sendMessage(getMsg("prefix") + getMsg("admin.reloaded"));
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
                    startMine.setPaused(false);
                    plugin.getMineManager().addMine(startMine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.started").replace("%mine%", startMine.getName()));
                }
                break;

            case "stop":
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
                    org.bukkit.Location loc = tpMine.getTpLocation();
                    if (loc == null) {
                        org.bukkit.World w = org.bukkit.Bukkit.getWorld(tpMine.getWorldName());
                        if (w != null) {
                            loc = new org.bukkit.Location(w,
                                    tpMine.getMinX() + (tpMine.getMaxX() - tpMine.getMinX()) / 2.0,
                                    tpMine.getMaxY() + 1.0,
                                    tpMine.getMinZ() + (tpMine.getMaxZ() - tpMine.getMinZ()) / 2.0);
                        }
                    }
                    if (loc != null) {
                        player.teleport(loc);
                        player.sendMessage(getMsg("prefix") + getMsg("mine.teleported").replace("%mine%", tpMine.getName()));
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.tp-failed"));
                    }
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
                    if (currentTotal + chance > 100.0) {
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
                            .replace("%free%", String.format("%.2f", 100.0 - (currentTotal + chance))));

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
                createMine(player, args[1]);
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

    private int parsePage(String arg) {
        try { return Math.max(1, Integer.parseInt(arg)); } catch (NumberFormatException e) { return 1; }
    }

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

        if (page < totalPages) {
            player.sendMessage("");
            player.sendMessage(getMsg("formats.list-next-page").replace("%next%", String.valueOf(page + 1)));
        }
        player.sendMessage(getMsg("formats.list-footer"));
    }

    private void handleTimers(Player player, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        if (mines.isEmpty()) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.no-mines"));
            return;
        }
        mines.sort(Comparator.comparingInt(Mine::getTimeUntilReset));

        int totalPages = (int) Math.ceil(mines.size() / 10.0);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-page").replace("%max%", String.valueOf(totalPages)));
            return;
        }

        player.sendMessage(getMsg("formats.timers-header"));
        player.sendMessage(getMsg("formats.timers-title").replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(totalPages)));

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, mines.size());

        for (int i = start; i < end; i++) {
            Mine m = mines.get(i);
            player.sendMessage(getMsg("formats.timers-entry").replace("%mine%", m.getName()).replace("%time%", TimeUtil.formatTime(m.getTimeUntilReset())));
        }
        player.sendMessage(getMsg("formats.timers-footer"));
    }

    private void sendHelp(Player player) {
        for (String line : plugin.getMessages().getStringList("help")) player.sendMessage(line.replace("&", "§"));
    }

    private void createMine(Player player, String mineName) {
        if (plugin.getMineManager().getMine(mineName) != null) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.mine-exists"));
            return;
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(getMsg("prefix") + getMsg("commands.we-selection"));
            return;
        }

        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();

        Mine tempCheck = new Mine("temp", player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
        for (Mine existing : plugin.getMineManager().getMines()) {
            if (existing.intersects(tempCheck)) {
                player.sendMessage(getMsg("prefix") + getMsg("commands.intersects").replace("%mine%", existing.getName()));
                return;
            }
        }

        Mine newMine = new Mine(mineName, player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());

        plugin.getMineManager().addMine(newMine);
        newMine.calculateTotalBlocks();
        player.sendMessage(getMsg("prefix") + getMsg("admin.created").replace("%mine%", mineName));
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