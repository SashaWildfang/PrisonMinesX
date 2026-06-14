package com.sasha.prisonminesx.commands;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MineTabCompleter implements TabCompleter {

    private final PrisonMinesX plugin;
    private final List<String> commands = Arrays.asList(
            "help", "gui", "create", "delete", "reset",
            "setspawn", "list", "info", "tp", "set", "unset",
            "timers", "redefine", "start", "stop", "reload", "flags"
    );

    public MineTabCompleter(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("prisonmines.admin")) return completions;

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (Arrays.asList("delete", "reset", "setspawn", "info", "tp", "set", "unset", "redefine", "start", "stop", "flags", "gui").contains(subCmd)) {
                List<String> mineNames = new ArrayList<>();
                for (Mine mine : plugin.getMineManager().getMines()) {
                    mineNames.add(mine.getName());
                }

                if (subCmd.equals("start") || subCmd.equals("stop")) {
                    mineNames.add("all");
                }

                StringUtil.copyPartialMatches(args[1], mineNames, completions);
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
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
        }
        else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            completions.add("<percentage>");
        }

        return completions;
    }
}