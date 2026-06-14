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
                player.sendMessage("§cAction cancelled.");
                if (mine != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (type.equals("flag")) MineGUI.openFlagsMenu(player, mine);
                        else if (type.equals("block")) MineGUI.openBlockEditor(player, mine);
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
                        player.sendMessage("§cPlease enter a valid number between 0 and 100. Try again or type 'cancel'.");
                        return;
                    }

                    String niceName = MineCommand.formatName(arg);

                    if (chance == 0) {
                        mine.getComposition().remove(arg);
                        player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§cRemoved " + niceName + " from " + mineName + ".");
                    } else {
                        double currentTotal = 0;
                        for (Map.Entry<String, Double> entry : mine.getComposition().entrySet()) {
                            if (!entry.getKey().equals(arg)) currentTotal += entry.getValue();
                        }

                        if (currentTotal + chance > 100.0) {
                            double maxAllowed = 100.0 - currentTotal;
                            if (maxAllowed <= 0.001) {
                                player.sendMessage("§cThe mine is full! You must unset or lower other percentages first.");
                            } else {
                                player.sendMessage("§cCannot set to " + chance + "%. The mine would exceed 100%.");
                                player.sendMessage("§eMaximum available percentage to allocate is §a" + String.format("%.2f", maxAllowed) + "%§e.");
                            }
                            return;
                        }
                        mine.addComposition(arg, chance);
                        player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§aSet " + niceName + " to " + chance + "% in " + mineName + ".");
                    }
                    success = true;
                }
                else if (type.equals("flag")) {
                    switch (arg) {
                        case "delay":
                            int seconds = TimeUtil.parseTimeToSeconds(rawInput);
                            if (seconds <= 0) {
                                player.sendMessage("§cInvalid format! Use like '10m'. Try again or type 'cancel'.");
                            } else {
                                mine.setResetDelay(seconds);
                                player.sendMessage("§aReset delay set to " + TimeUtil.formatTime(seconds) + ".");
                                success = true;
                            }
                            break;

                        case "warnings":
                            List<Integer> parsedWarnings = new ArrayList<>();
                            for (String w : rawInput.split(",")) {
                                int warnSeconds = Integer.parseInt(w.trim()) * 60;
                                if (warnSeconds >= mine.getResetDelay()) {
                                    player.sendMessage("§cError: Warning (" + w.trim() + "m) cannot be longer than the Reset Delay! Try again or type 'cancel'.");
                                    return;
                                }
                                parsedWarnings.add(warnSeconds);
                            }
                            mine.setResetWarnings(parsedWarnings);
                            player.sendMessage("§aWarnings configured successfully.");
                            success = true;
                            break;

                        case "surface":
                            if (rawInput.equalsIgnoreCase("air")) {
                                mine.setSurface(null);
                                player.sendMessage("§aSurface block cleared.");
                                success = true;
                            } else {
                                Material mat = Material.matchMaterial(rawInput.toUpperCase());
                                if (mat == null || !mat.isBlock()) {
                                    player.sendMessage("§cInvalid block material! Try again or type 'cancel'.");
                                } else {
                                    mine.setSurface(mat.name());
                                    player.sendMessage("§aSurface block set to " + MineCommand.formatName(mat.name()) + ".");
                                    success = true;
                                }
                            }
                            break;

                        case "percent":
                            double chance = Double.parseDouble(rawInput);
                            if (chance < 0 || chance > 100) {
                                player.sendMessage("§cPlease enter a number between 0 and 100. Try again or type 'cancel'.");
                            } else {
                                mine.setResetPercentage(chance);
                                player.sendMessage("§aPercent reset set to " + chance + "%.");
                                success = true;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                player.sendMessage("§cInvalid input format! Please try again or type 'cancel'.");
            }

            if (success) {
                promptMap.remove(uuid);
                plugin.getMineManager().addMine(mine);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (type.equals("flag")) MineGUI.openFlagsMenu(player, mine);
                    else if (type.equals("block")) MineGUI.openBlockEditor(player, mine);
                });
            }
        }
    }
}