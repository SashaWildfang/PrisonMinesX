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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MineCommand implements CommandExecutor {

    private final PrisonMinesX plugin;

    public MineCommand(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can manage mines.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("prisonmines.admin")) {
            player.sendMessage(getMsg("prefix") + getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

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
                    player.sendMessage(getMsg("prefix") + "§cThere are no active mines! Use /pmines create <name> to create one.");
                    return true;
                }
                MineGUI.openMainMenu(player, plugin, 1);
                break;

            case "list":
                int listPage = args.length > 1 ? parsePage(args[1]) : 1;
                handleList(player, listPage);
                break;

            case "timers":
                int timerPage = args.length > 1 ? parsePage(args[1]) : 1;
                handleTimers(player, timerPage);
                break;

            case "reload":
                plugin.reloadPlugin();
                player.sendMessage(getMsg("prefix") + getMsg("admin.reloaded"));
                break;

            case "start":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines start <name|all>");
                    return true;
                }

                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + "§cThere are no active mines to start!");
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
                    player.sendMessage(getMsg("prefix") + "§aSuccessfully started/unpaused §e" + startedCount + " §amines.");
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
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines stop <name|all>");
                    return true;
                }

                if (plugin.getMineManager().getMines().isEmpty()) {
                    player.sendMessage(getMsg("prefix") + "§cThere are no active mines to stop!");
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
                    player.sendMessage(getMsg("prefix") + "§cSuccessfully stopped/paused §e" + stoppedCount + " §cmines.");
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
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines flags <name>");
                    return true;
                }
                Mine flagsMine = plugin.getMineManager().getMine(args[1]);
                if (flagsMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                MineGUI.openFlagsMenu(player, flagsMine);
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines info <name>");
                    return true;
                }
                Mine infoMine = plugin.getMineManager().getMine(args[1]);
                if (infoMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                player.sendMessage("§8§m----------------------------------------");
                player.sendMessage("§b§lMine Info: §f" + infoMine.getName());
                player.sendMessage("§7Status: " + (infoMine.isPaused() ? "§cPaused" : "§aRunning"));
                player.sendMessage("§7World: §f" + infoMine.getWorldName());
                player.sendMessage("§7Blocks Remaining: §f" + String.format("%.1f", infoMine.getPercentRemaining()) + "%");

                player.sendMessage("§7Flags:");
                player.sendMessage("  §8- §7Reset Delay: §e" + TimeUtil.formatTime(infoMine.getResetDelay()));
                player.sendMessage("  §8- §7Percent Reset: §e" + (infoMine.getResetPercentage() == -1.0 ? "Default" : infoMine.getResetPercentage() + "%"));
                player.sendMessage("  §8- §7Fill Mode: " + (infoMine.isFillMode() ? "§aEnabled" : "§cDisabled"));
                player.sendMessage("  §8- §7Hologram: " + (infoMine.isHologramEnabled() ? "§aEnabled" : "§cDisabled"));

                player.sendMessage("§7Composition:");

                if (infoMine.getComposition().isEmpty()) {
                    player.sendMessage("  §8- §cNone (Mine is empty)");
                } else {
                    for (Map.Entry<String, Double> entry : infoMine.getComposition().entrySet()) {
                        player.sendMessage("  §8- §a" + formatName(entry.getKey()) + " §7(§e" + entry.getValue() + "%§7)");
                    }
                }
                player.sendMessage("§8§m----------------------------------------");
                break;

            case "tp":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines tp <name>");
                    return true;
                }
                Mine tpMine = plugin.getMineManager().getMine(args[1]);
                if (tpMine != null) {
                    org.bukkit.Location loc = tpMine.getTpLocation();

                    if (loc == null) {
                        org.bukkit.World w = org.bukkit.Bukkit.getWorld(tpMine.getWorldName());
                        if (w != null) {
                            double x = tpMine.getMinX() + (tpMine.getMaxX() - tpMine.getMinX()) / 2.0;
                            double y = tpMine.getMaxY() + 1.0;
                            double z = tpMine.getMinZ() + (tpMine.getMaxZ() - tpMine.getMinZ()) / 2.0;
                            loc = new org.bukkit.Location(w, x, y, z);
                        }
                    }

                    if (loc != null) {
                        player.teleport(loc);
                        player.sendMessage(getMsg("prefix") + "§7You were teleported to §e" + tpMine.getName() + "§7.");
                    } else {
                        player.sendMessage(getMsg("prefix") + "§cCannot teleport: World not found.");
                    }
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "set":
                if (args.length < 4) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines set <mine> <block> <percent>");
                    return true;
                }
                Mine setMine = plugin.getMineManager().getMine(args[1]);
                if (setMine == null) {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                    return true;
                }
                Material mat = Material.matchMaterial(args[2].toUpperCase());
                if (mat == null || !mat.isBlock()) {
                    player.sendMessage(getMsg("prefix") + "§cInvalid block material.");
                    return true;
                }

                if (setMine.getComposition().containsKey(mat.name())) {
                    player.sendMessage(getMsg("prefix") + "§c" + formatName(mat.name()) + " is already in the mine!");
                    player.sendMessage(getMsg("prefix") + "§eIf you want to edit its percentage, please use the GUI or unset it first.");
                    return true;
                }

                try {
                    double chance = Double.parseDouble(args[3]);
                    if (chance == 0) {
                        player.sendMessage(getMsg("prefix") + "§cYou cannot add a new block at 0%.");
                        return true;
                    }

                    double currentTotal = 0;
                    for (Map.Entry<String, Double> entry : setMine.getComposition().entrySet()) {
                        if (!entry.getKey().equals(mat.name())) currentTotal += entry.getValue();
                    }

                    if (currentTotal + chance > 100.0) {
                        double maxAllowed = 100.0 - currentTotal;
                        if (maxAllowed <= 0.001) {
                            player.sendMessage(getMsg("prefix") + "§cThe mine is full! You must unset or lower other percentages first.");
                        } else {
                            player.sendMessage(getMsg("prefix") + "§cCannot set to " + chance + "%. The mine would exceed 100%.");
                            player.sendMessage(getMsg("prefix") + "§eMaximum available percentage to allocate is §a" + String.format("%.2f", maxAllowed) + "%§e.");
                        }
                        return true;
                    }

                    setMine.addComposition(mat.name(), chance);
                    plugin.getMineManager().addMine(setMine);
                    double freePercent = 100.0 - (currentTotal + chance);
                    player.sendMessage(getMsg("prefix") + "§aAdded §e" + formatName(mat.name()) + " §aat §e" + chance + "% §ato §e" + setMine.getName() + "§a.");
                    player.sendMessage(getMsg("prefix") + "§7(Remaining free space: §a" + String.format("%.2f", freePercent) + "%§7)");

                } catch (NumberFormatException e) {
                    player.sendMessage(getMsg("prefix") + "§cPercentage must be a valid number.");
                }
                break;

            case "unset":
                if (args.length < 3) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines unset <mine> <block|all>");
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
                    player.sendMessage(getMsg("prefix") + "§aCleared all blocks from §e" + unsetMine.getName() + "§a.");
                } else {
                    String targetMat = args[2].toUpperCase();
                    unsetMine.getComposition().remove(targetMat);
                    plugin.getMineManager().addMine(unsetMine);
                    player.sendMessage(getMsg("prefix") + "§aRemoved §e" + formatName(targetMat) + " §afrom §e" + unsetMine.getName() + "§a.");
                }
                break;

            case "create":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines create <name>");
                    return true;
                }
                if (args[1].length() > 16) {
                    player.sendMessage(getMsg("prefix") + "§cMine names cannot exceed 16 characters!");
                    return true;
                }
                createMine(player, args[1]);
                break;

            case "redefine":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines redefine <name>");
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
                    player.sendMessage(getMsg("prefix") + "§cYou must make a complete WorldEdit selection first!");
                    return true;
                }

                BlockVector3 rMin = rSel.getMinimumPoint();
                BlockVector3 rMax = rSel.getMaximumPoint();

                Mine tempRedef = new Mine("temp", player.getWorld().getName(), rMin.getBlockX(), rMin.getBlockY(), rMin.getBlockZ(), rMax.getBlockX(), rMax.getBlockY(), rMax.getBlockZ());
                for (Mine existing : plugin.getMineManager().getMines()) {
                    if (!existing.getName().equalsIgnoreCase(redefMine.getName()) && existing.intersects(tempRedef)) {
                        player.sendMessage(getMsg("prefix") + "§cRedefine failed! This region intersects with existing mine: §e" + existing.getName());
                        return true;
                    }
                }

                redefMine.setWorldName(player.getWorld().getName());
                redefMine.setMinX(rMin.getBlockX());
                redefMine.setMinY(rMin.getBlockY());
                redefMine.setMinZ(rMin.getBlockZ());
                redefMine.setMaxX(rMax.getBlockX());
                redefMine.setMaxY(rMax.getBlockY());
                redefMine.setMaxZ(rMax.getBlockZ());

                redefMine.calculateTotalBlocks();
                plugin.getMineManager().addMine(redefMine);
                plugin.getMineManager().resetMine(redefMine.getName(), true);

                player.sendMessage(getMsg("prefix") + getMsg("admin.redefined").replace("%mine%", redefMine.getName()));
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines delete <name>");
                    return true;
                }
                Mine delMine = plugin.getMineManager().getMine(args[1]);
                if (delMine != null) {

                    // FIX: Safe Deletion Wipe! Clears composition and tells MineManager to reset it
                    delMine.getComposition().clear();
                    plugin.getMineManager().addMine(delMine); // Save empty state
                    plugin.getMineManager().resetMine(delMine.getName(), false); // Cleanly wipes to Air

                    // Proceed with standard deletion
                    plugin.getMineManager().deleteMine(args[1]);

                    player.sendMessage(getMsg("prefix") + getMsg("admin.deleted").replace("%mine%", args[1]));
                } else {
                    player.sendMessage(getMsg("prefix") + getMsg("mine.does-not-exist"));
                }
                break;

            case "reset":
                if (args.length < 2) {
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines reset <name>");
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
                    player.sendMessage(getMsg("prefix") + "§cUsage: /pmines setspawn <name>");
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
                player.sendMessage(getMsg("prefix") + getMsg("invalid-args"));
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
            player.sendMessage(getMsg("prefix") + "§cThere are no active mines!");
            return;
        }
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalPages = (int) Math.ceil(mines.size() / 10.0);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + "§cInvalid page number. Max pages is " + totalPages + ".");
            return;
        }

        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§a§lShowing " + mines.size() + " Mine(s) §7(Page " + page + "/" + totalPages + ")");

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, mines.size());

        for (int i = start; i < end; i++) {
            Mine m = mines.get(i);
            TextComponent mineComp = new TextComponent(" §8- §e" + m.getName() + " §7(World: " + m.getWorldName() + ")");

            StringBuilder hoverText = new StringBuilder("§b§l" + m.getName() + " Info:\n");
            hoverText.append("§7Status: ").append(m.isPaused() ? "§cPaused" : "§aRunning").append("\n");
            hoverText.append("§7Next Reset: §e").append(TimeUtil.formatTime(m.getTimeUntilReset())).append("\n\n");
            hoverText.append("§b§lComposition:\n");

            if (m.getComposition().isEmpty()) hoverText.append("§cNone (Mine is empty)\n");
            else {
                for (Map.Entry<String, Double> entry : m.getComposition().entrySet()) {
                    hoverText.append("§8- §a").append(formatName(entry.getKey())).append(" §7(§e").append(entry.getValue()).append("%§7)\n");
                }
            }
            hoverText.append("\n§eClick to teleport to mine.");

            mineComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText.toString()).create()));
            mineComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines tp " + m.getName()));
            player.spigot().sendMessage(mineComp);
        }

        if (page < totalPages) {
            player.sendMessage("");
            player.sendMessage("§eType /pmines list " + (page + 1) + " to go to next page");
        }

        player.sendMessage("§8§m----------------------------------------");
    }

    private void handleTimers(Player player, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        if (mines.isEmpty()) {
            player.sendMessage(getMsg("prefix") + "§cThere are no active mines!");
            return;
        }
        mines.sort(Comparator.comparingInt(Mine::getTimeUntilReset));

        int totalPages = (int) Math.ceil(mines.size() / 10.0);
        if (totalPages == 0) totalPages = 1;

        if (page > totalPages || page < 1) {
            player.sendMessage(getMsg("prefix") + "§cInvalid page number. Max pages is " + totalPages + ".");
            return;
        }

        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§b§lMine Timers §7(Page " + page + "/" + totalPages + ")");

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, mines.size());

        for (int i = start; i < end; i++) {
            Mine m = mines.get(i);
            player.sendMessage(" §8- §e" + m.getName() + " §7- Resets in §a" + TimeUtil.formatTime(m.getTimeUntilReset()));
        }
        player.sendMessage("§8§m----------------------------------------");
    }

    private void sendHelp(Player player) {
        for (String line : plugin.getMessages().getStringList("help")) player.sendMessage(line.replace("&", "§"));
    }

    private void createMine(Player player, String mineName) {
        if (plugin.getMineManager().getMine(mineName) != null) {
            player.sendMessage(getMsg("prefix") + "§cA mine with that name already exists!");
            return;
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(getMsg("prefix") + "§cYou must make a complete WorldEdit selection first!");
            return;
        }

        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();

        Mine tempCheck = new Mine("temp", player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
        for (Mine existing : plugin.getMineManager().getMines()) {
            if (existing.intersects(tempCheck)) {
                player.sendMessage(getMsg("prefix") + "§cCreation failed! This region intersects with the existing mine: §e" + existing.getName());
                return;
            }
        }

        Mine newMine = new Mine(mineName, player.getWorld().getName(), min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
        plugin.getMineManager().addMine(newMine);
        newMine.calculateTotalBlocks();
        player.sendMessage(getMsg("prefix") + getMsg("admin.created").replace("%mine%", mineName));
    }

    private String getMsg(String path) { return plugin.getMessages().getString(path, "").replace("&", "§"); }

    public static String formatName(String name) {
        if (name == null) return "";
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}