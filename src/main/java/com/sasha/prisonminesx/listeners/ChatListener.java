package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.commands.MineCommand;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final PrisonMinesX plugin;
    public static final Map<UUID, String> promptMap = new HashMap<>();

    public ChatListener(PrisonMinesX plugin) { this.plugin = plugin; }

    private String getMsg(String path) {
        return plugin.getMessages().getString(path, "").replace("&", "§");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (promptMap.containsKey(uuid)) {
            event.setCancelled(true);
            String rawInput = event.getMessage();

            String[] data = promptMap.get(uuid).split(":");
            String mineName = data[0];
            String type = data[1];
            String arg = data[2];

            Mine mine = plugin.getMineManager().getMine(mineName);

            if (rawInput.equalsIgnoreCase("cancel")) {
                promptMap.remove(uuid);
                player.sendMessage(getMsg("prompts.cancelled"));
                if (mine != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (type.equals("flag")) MineGUI.openFlagsMenu(player, mine, plugin);
                        else if (type.equals("block")) MineGUI.openBlockEditor(player, mine, plugin);
                    });
                }
                return;
            }

            if (mine == null) {
                promptMap.remove(uuid);
                return;
            }

            boolean success = false;

            try {
                if (type.equals("block")) {
                    double chance = Double.parseDouble(rawInput);
                    if (chance < 0 || chance > 100) {
                        player.sendMessage(getMsg("prompts.invalid-percent"));
                        return;
                    }

                    String niceName = MineCommand.formatName(arg);

                    if (chance == 0) {
                        mine.getComposition().remove(arg);
                        player.sendMessage(getMsg("prefix") + getMsg("commands.block-removed").replace("%block%", niceName).replace("%mine%", mineName));
                    } else {
                        double currentTotal = 0;
                        for (Map.Entry<String, Double> entry : mine.getComposition().entrySet()) {
                            if (!entry.getKey().equals(arg)) currentTotal += entry.getValue();
                        }

                        if (currentTotal + chance > 100.0) {
                            double maxAllowed = 100.0 - currentTotal;
                            if (maxAllowed <= 0.001) {
                                player.sendMessage(getMsg("prefix") + getMsg("commands.mine-full"));
                            } else {
                                player.sendMessage(getMsg("prefix") + getMsg("commands.exceeds-100").replace("%chance%", String.valueOf(chance)));
                                player.sendMessage(getMsg("prefix") + getMsg("commands.max-allowed").replace("%max%", String.format("%.2f", maxAllowed)));
                            }
                            return;
                        }
                        mine.addComposition(arg, chance);
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.block-set").replace("%block%", niceName).replace("%chance%", String.valueOf(chance)).replace("%mine%", mineName));
                    }
                    success = true;
                }
                else if (type.equals("flag")) {
                    switch (arg) {
                        case "delay":
                            int seconds = TimeUtil.parseTimeToSeconds(rawInput);
                            if (seconds <= 0) {
                                player.sendMessage(getMsg("prompts.invalid-delay"));
                            } else {
                                mine.setResetDelay(seconds);
                                player.sendMessage(getMsg("prompts.delay-set").replace("%time%", TimeUtil.formatTime(seconds)));
                                success = true;
                            }
                            break;

                        case "warnings":
                            List<Integer> parsedWarnings = new ArrayList<>();
                            for (String w : rawInput.split(",")) {
                                int warnSeconds = Integer.parseInt(w.trim()) * 60;
                                if (warnSeconds >= mine.getResetDelay()) {
                                    player.sendMessage(getMsg("prompts.invalid-warning").replace("%val%", w.trim()));
                                    return;
                                }
                                parsedWarnings.add(warnSeconds);
                            }
                            mine.setResetWarnings(parsedWarnings);
                            player.sendMessage(getMsg("prompts.warnings-set"));
                            success = true;
                            break;

                        case "surface":
                            if (rawInput.equalsIgnoreCase("air")) {
                                mine.setSurface(null);
                                player.sendMessage(getMsg("prompts.surface-cleared"));
                                success = true;
                            } else {
                                Material mat = Material.matchMaterial(rawInput.toUpperCase());
                                if (mat == null || !mat.isBlock()) {
                                    player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-block"));
                                } else {
                                    mine.setSurface(mat.name());
                                    player.sendMessage(getMsg("prompts.surface-set").replace("%block%", MineCommand.formatName(mat.name())));
                                    success = true;
                                }
                            }
                            break;

                        case "percent":
                            double chance = Double.parseDouble(rawInput);
                            if (chance < 0 || chance > 100) {
                                player.sendMessage(getMsg("prompts.invalid-percent"));
                            } else {
                                mine.setResetPercentage(chance);
                                player.sendMessage(getMsg("prompts.percent-set").replace("%chance%", String.valueOf(chance)));
                                success = true;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                player.sendMessage(getMsg("prompts.invalid-format"));
            }

            if (success) {
                promptMap.remove(uuid);
                plugin.getMineManager().addMine(mine);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (type.equals("flag")) MineGUI.openFlagsMenu(player, mine, plugin);
                    else if (type.equals("block")) MineGUI.openBlockEditor(player, mine, plugin);
                });
            }
        }
    }
}