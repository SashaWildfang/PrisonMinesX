package com.sasha.prisonminesx.commands;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MineTabCompleter implements TabCompleter {

    private final PrisonMinesX plugin;
    private final List<String> commands = Arrays.asList(
            "help", "gui", "create", "delete", "reset", "undo", "warpgui", "warp", "setdesc",
            "setspawn", "list", "info", "tp", "set", "unset", "setschem", "clearschem",
            "timers", "redefine", "start", "stop", "reload", "flags", "stats", "rename", "moveholo"
    );

    public MineTabCompleter(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("prisonminesx.cmd.pmine") && !sender.hasPermission("prisonminesx.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> allowedCommands = new ArrayList<>();
            for (String cmd : commands) {
                if (sender.hasPermission("prisonminesx.admin") || sender.hasPermission("prisonminesx.cmd." + cmd)) {
                    allowedCommands.add(cmd);
                }
            }
            StringUtil.copyPartialMatches(args[0], allowedCommands, completions);
        }
        else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();

            if (subCmd.equals("help") || subCmd.equals("list") || subCmd.equals("timers") || subCmd.equals("warpgui") || subCmd.equals("stats")) {
                int totalItems = 1;
                int perPage = 10;

                if (subCmd.equals("help")) {
                    totalItems = plugin.getMessages().getStringList("help").size();
                    perPage = plugin.getConfig().getInt("settings.help-per-page", 10);
                } else if (subCmd.equals("list") || subCmd.equals("timers") || subCmd.equals("stats")) {
                    totalItems = plugin.getMineManager().getMines().size();
                    if (subCmd.equals("timers")) perPage = plugin.getConfig().getInt("settings.timers-per-page", 10);
                    else if (subCmd.equals("stats")) perPage = 45;
                    else perPage = 10;
                } else if (subCmd.equals("warpgui")) {
                    totalItems = plugin.getMineManager().getMines().size();
                    perPage = plugin.getConfig().getInt("settings.warps-per-page", 45);
                }

                int totalPages = (int) Math.ceil((double) totalItems / perPage);
                if (totalPages == 0) totalPages = 1;

                List<String> pages = new ArrayList<>();
                for (int i = 1; i <= totalPages; i++) {
                    pages.add(String.valueOf(i));
                }
                StringUtil.copyPartialMatches(args[1], pages, completions);
            }
            else if (Arrays.asList("delete", "reset", "setspawn", "info", "tp", "set", "unset", "redefine", "start", "stop", "flags", "gui", "undo", "setschem", "clearschem", "rename", "moveholo", "setdesc").contains(subCmd)) {
                List<String> mineNames = new ArrayList<>();
                for (Mine mine : plugin.getMineManager().getMines()) {
                    mineNames.add(mine.getName());
                }

                if (subCmd.equals("start") || subCmd.equals("stop")) {
                    mineNames.add("all");
                }

                StringUtil.copyPartialMatches(args[1], mineNames, completions);
            }
            else if (subCmd.equals("warp") && sender instanceof Player) {
                Player p = (Player) sender;
                List<String> accessibleMines = new ArrayList<>();
                for (Mine mine : plugin.getMineManager().getMines()) {
                    if (p.hasPermission("prisonminesx.admin") || p.hasPermission("prisonminesx.mine.all") || p.hasPermission("prisonminesx.mine." + mine.getName().toLowerCase())) {
                        accessibleMines.add(mine.getName());
                    }
                }
                StringUtil.copyPartialMatches(args[1], accessibleMines, completions);
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rename")) {
                completions.add("<NewName>");
            }
            else if (args[0].equalsIgnoreCase("setdesc")) {
                completions.add("<description/none>");
            }
            else if (args[0].equalsIgnoreCase("moveholo")) {
                if (sender instanceof Player) completions.add(String.valueOf(((Player) sender).getLocation().getBlockX()));
                completions.add("0");
            }
            else if (args[0].equalsIgnoreCase("set")) {
                List<String> materials = new ArrayList<>();
                for (Material mat : Material.values()) {
                    if (mat.isBlock() && !mat.isLegacy()) materials.add(mat.name());
                }
                StringUtil.copyPartialMatches(args[2], materials, completions);
            }
            else if (args[0].equalsIgnoreCase("unset")) {
                Mine mine = plugin.getMineManager().getMine(args[1]);
                List<String> options = new ArrayList<>();
                options.add("all");
                if (mine != null) {
                    options.addAll(mine.getComposition().keySet());
                }
                StringUtil.copyPartialMatches(args[2], options, completions);
            }
            else if (args[0].equalsIgnoreCase("setschem")) {
                completions.add("<filename.schem>");
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("set")) completions.add("<percentage>");
            else if (args[0].equalsIgnoreCase("moveholo") && sender instanceof Player) completions.add(String.valueOf(((Player) sender).getLocation().getBlockY()));
            else if (args[0].equalsIgnoreCase("moveholo")) completions.add("0");
        }
        else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("moveholo") && sender instanceof Player) completions.add(String.valueOf(((Player) sender).getLocation().getBlockZ()));
            else if (args[0].equalsIgnoreCase("moveholo")) completions.add("0");
        }

        return completions;
    }
}