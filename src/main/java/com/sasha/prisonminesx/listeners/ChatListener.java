package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.commands.MineCommand;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
            String rawInput = event.getMessage().trim();

            String[] data = promptMap.get(uuid).split(":");
            String mineName = data[0];
            String type = data[1];
            String arg = data[2];

            Mine mine = plugin.getMineManager().getMine(mineName);

            if (rawInput.equalsIgnoreCase("cancel")) {
                promptMap.remove(uuid);
                player.sendMessage(getMsg("prefix") + getMsg("prompts.cancelled"));
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

            if (type.equals("block")) {
                double chance;
                try {
                    chance = Double.parseDouble(rawInput);
                } catch (NumberFormatException e) {
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-percent"));
                    return;
                }

                if (chance < 0 || chance > 100) {
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-percent"));
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

                    double newTotal = currentTotal + chance;
                    if (newTotal > 100.0) {
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

                    boolean autoReset = plugin.getConfig().getBoolean("settings.auto-reset-on-set", false);
                    boolean onlyWhenFull = plugin.getConfig().getBoolean("settings.only-reset-when-full", true);
                    boolean isFull = Math.abs(newTotal - 100.0) < 0.001;

                    if (autoReset && (!onlyWhenFull || isFull)) {
                        // Schedules the reset safely back onto the Main Server Thread!
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getMineManager().resetMine(mineName, false));
                        player.sendMessage(getMsg("prefix") + "§7Mine automatically reset due to composition update.");
                    } else if (isFull) {
                        TextComponent tc = new TextComponent(getMsg("prefix") + getMsg("prompts.mine-full-reset"));
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pmines reset " + mineName));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to instantly force reset!").create()));
                        player.spigot().sendMessage(tc);
                    }
                }
                success = true;
            }
            else if (type.equals("flag")) {
                switch (arg) {
                    case "delay":
                        int seconds = TimeUtil.parseTimeToSeconds(rawInput);
                        if (seconds <= 0) {
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-delay"));
                        } else {
                            mine.setResetDelay(seconds);
                            mine.setTimeUntilReset(seconds);
                            mine.getResetWarnings().clear();
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.delay-set").replace("%time%", TimeUtil.formatTime(seconds)));
                            if (mine.isHologramEnabled()) {
                                plugin.getHologramManager().updateHologram(mine);
                            }
                            success = true;
                        }
                        break;

                    case "warnings":
                        if (rawInput.equalsIgnoreCase("clear") || rawInput.equalsIgnoreCase("none")) {
                            mine.getResetWarnings().clear();
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.warnings-set"));
                            success = true;
                            break;
                        }

                        List<Integer> parsedWarnings = new ArrayList<>();
                        boolean valid = true;
                        for (String w : rawInput.split(",")) {
                            int warnSeconds = TimeUtil.parseTimeToSeconds(w.trim());
                            if (warnSeconds <= 0) {
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-format"));
                                valid = false;
                                break;
                            }
                            if (warnSeconds > mine.getResetDelay()) {
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-warning").replace("%val%", TimeUtil.formatTime(warnSeconds)));
                                valid = false;
                                break;
                            }
                            parsedWarnings.add(warnSeconds);
                        }
                        if (valid) {
                            mine.setResetWarnings(parsedWarnings);
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.warnings-set"));
                            success = true;
                        }
                        break;

                    case "percent":
                        try {
                            double pChance = Double.parseDouble(rawInput);
                            if (pChance < 0 || pChance > 100) {
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-percent"));
                            } else {
                                mine.setResetPercentage(pChance);
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.percent-set").replace("%chance%", String.valueOf(pChance)));
                                success = true;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-percent"));
                        }
                        break;

                    case "schedule":
                        if (rawInput.equalsIgnoreCase("clear")) {
                            mine.getResetSchedules().clear();
                            player.sendMessage(getMsg("prefix") + "§7Reset schedule cleared for §3" + mine.getName() + "§7.");
                            success = true;
                        } else {
                            List<String> validTimes = new ArrayList<>();
                            String[] times = rawInput.split(",");
                            boolean isValid = true;
                            for (String time : times) {
                                if (time.trim().matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) {
                                    validTimes.add(time.trim());
                                } else {
                                    isValid = false;
                                    break;
                                }
                            }
                            if (isValid) {
                                mine.setResetSchedules(validTimes);
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.schedule-set").replace("%mine%", mine.getName()));
                                success = true;
                            } else {
                                player.sendMessage(getMsg("prefix") + getMsg("prompts.invalid-schedule-format"));
                            }
                        }
                        break;
                }
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