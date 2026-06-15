package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles all clicks inside the PrisonMinesX GUI engine.
 * Relies on MineGUI.getBaseTitle() which has been updated to use O(1) Cached lookups
 * rather than heavy string translation on every click event.
 */
public class GUIListener implements Listener {

    private final PrisonMinesX plugin;

    public GUIListener(PrisonMinesX plugin) { this.plugin = plugin; }

    private String getMsg(String path) {
        return plugin.getMessages().getString(path, "").replace("&", "§");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String cleanTitle = ChatColor.stripColor(title);

        // Fetch O(1) cached titles to evaluate context
        String mainBase = MineGUI.getBaseTitle(plugin, "gui.main.title");
        String editBase = MineGUI.getBaseTitle(plugin, "gui.edit.title");
        String flagsBase = MineGUI.getBaseTitle(plugin, "gui.flags.title");
        String blocksBase = MineGUI.getBaseTitle(plugin, "gui.blocks.title");
        String activeBase = MineGUI.getBaseTitle(plugin, "gui.active-players.title");
        String warpsBase = MineGUI.getBaseTitle(plugin, "gui.warps.title");
        String statsBase = MineGUI.getBaseTitle(plugin, "gui.stats.title");

        // Fast escape if inventory doesn't match our cache
        if (cleanTitle.startsWith(mainBase) || cleanTitle.startsWith(editBase) || cleanTitle.startsWith(flagsBase) || cleanTitle.startsWith(blocksBase) || cleanTitle.startsWith(activeBase) || cleanTitle.startsWith(warpsBase) || cleanTitle.startsWith(statsBase)) {

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            // Handling Player Inventory interactions for specific config prompts
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (event.getCurrentItem().getType() == Material.AIR) return;

                if (cleanTitle.startsWith(flagsBase) && ChatListener.promptMap.containsKey(player.getUniqueId())) {
                    String[] data = ChatListener.promptMap.get(player.getUniqueId()).split(":");

                    if (data.length > 1 && data[2].equals("displayitem")) {
                        Mine mine = plugin.getMineManager().getMine(data[0]);
                        if (mine != null) {
                            mine.setDisplayItem(event.getCurrentItem().getType().name());
                            plugin.getMineManager().addMine(mine);
                            ChatListener.promptMap.remove(player.getUniqueId());
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.icon-set"));
                            MineGUI.openFlagsMenu(player, mine, plugin);
                        }
                    }
                    else if (data.length > 1 && data[2].equals("surface")) {
                        Material clickedMat = event.getCurrentItem().getType();
                        if (!clickedMat.isBlock()) {
                            player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-block"));
                            return;
                        }
                        Mine mine = plugin.getMineManager().getMine(data[0]);
                        if (mine != null) {
                            mine.setSurface(clickedMat.name());
                            plugin.getMineManager().addMine(mine);
                            ChatListener.promptMap.remove(player.getUniqueId());
                            player.sendMessage(getMsg("prefix") + getMsg("prompts.surface-set").replace("%block%", com.sasha.prisonminesx.commands.MineCommand.formatName(clickedMat.name())));
                            MineGUI.openFlagsMenu(player, mine, plugin);
                        }
                    }
                    return;
                }

                if (cleanTitle.startsWith(blocksBase)) {
                    if (event.getCurrentItem().getType().isBlock()) {
                        String mineName = cleanTitle.replace(blocksBase, "").trim();
                        Mine mine = plugin.getMineManager().getMine(mineName);
                        if (mine == null) return;

                        String material = event.getCurrentItem().getType().name();
                        String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(material);

                        if (mine.getComposition().containsKey(material)) {
                            player.sendMessage(getMsg("prefix") + getMsg("commands.block-exists").replace("%block%", niceName));
                            return;
                        }

                        player.closeInventory();
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":block:" + material);
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-percent").replace("%block%", niceName));
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.invalid-block"));
                    }
                    return;
                }
                return;
            }

            // Target interactions inside the top GUI section
            if (!event.getCurrentItem().hasItemMeta() || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
            String cleanItemName = ChatColor.stripColor(itemName).trim();

            if (cleanTitle.startsWith(warpsBase)) {
                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.next-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    player.performCommand("pmines warpgui " + (page + 1));
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.prev-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    player.performCommand("pmines warpgui " + (page - 1));
                } else {
                    String cleanMinePrefix = ChatColor.stripColor(getMsg("gui.main.mine-name").split("%mine%")[0]).trim();
                    if (cleanItemName.startsWith(cleanMinePrefix)) {
                        String targetMine = cleanItemName.replace(cleanMinePrefix, "").trim();
                        player.performCommand("pmines warp " + targetMine);
                        player.closeInventory();
                    }
                }
            }
            else if (cleanTitle.startsWith(statsBase)) {
                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.next-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openStatsMenu(player, plugin, page + 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.prev-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openStatsMenu(player, plugin, page - 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.sort-title")).trim())) {
                    MineGUI.SortType current = MineGUI.playerSorts.getOrDefault(player.getUniqueId(), MineGUI.SortType.ALPHABETICAL_AZ);
                    MineGUI.SortType[] vals = MineGUI.SortType.values();
                    int nextIdx;
                    if (event.isRightClick()) {
                        nextIdx = (current.ordinal() - 1 + vals.length) % vals.length;
                    } else {
                        nextIdx = (current.ordinal() + 1) % vals.length;
                    }
                    MineGUI.playerSorts.put(player.getUniqueId(), vals[nextIdx]);

                    int page = 1;
                    if (cleanTitle.contains("Pg ")) {
                        page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    }
                    MineGUI.openStatsMenu(player, plugin, page);
                }
            }
            else if (cleanTitle.startsWith(mainBase)) {
                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.next-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page + 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.prev-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page - 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.sort-title")).trim())) {
                    MineGUI.SortType current = MineGUI.playerSorts.getOrDefault(player.getUniqueId(), MineGUI.SortType.ALPHABETICAL_AZ);
                    MineGUI.SortType[] vals = MineGUI.SortType.values();
                    int nextIdx;
                    if (event.isRightClick()) {
                        nextIdx = (current.ordinal() - 1 + vals.length) % vals.length;
                    } else {
                        nextIdx = (current.ordinal() + 1) % vals.length;
                    }
                    MineGUI.playerSorts.put(player.getUniqueId(), vals[nextIdx]);

                    int page = 1;
                    if (cleanTitle.contains("Pg ")) {
                        page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    }
                    MineGUI.openMainMenu(player, plugin, page);
                } else {
                    String cleanMinePrefix = ChatColor.stripColor(getMsg("gui.main.mine-name").split("%mine%")[0]).trim();
                    if (cleanItemName.startsWith(cleanMinePrefix)) {
                        String mineName = cleanItemName.replace(cleanMinePrefix, "").trim();
                        Mine mine = plugin.getMineManager().getMine(mineName);
                        if (mine != null) MineGUI.openEditMenu(player, mine, plugin);
                    }
                }
            }
            else if (cleanTitle.startsWith(editBase)) {
                String mineName = cleanTitle.replace(editBase, "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) { player.closeInventory(); return; }

                String viewPlayersBase = ChatColor.stripColor(getMsg("gui.edit.view-players")).trim();

                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.edit.flags")).trim())) {
                    MineGUI.openFlagsMenu(player, mine, plugin);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.edit.back")).trim())) {
                    MineGUI.openMainMenu(player, plugin, 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.edit.teleport")).trim())) {
                    mine.setTpLocation(player.getLocation());
                    plugin.getMineManager().addMine(mine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.tp-set").replace("%mine%", mineName));
                    player.closeInventory();
                } else if (cleanItemName.equals("Teleport to Mine")) {
                    player.performCommand("pmines warp " + mineName);
                    player.closeInventory();
                } else if (cleanItemName.startsWith(viewPlayersBase)) {
                    if (mine.getActivePlayers().isEmpty()) {
                        player.sendMessage(getMsg("prefix") + getMsg("admin.no-players"));
                        player.closeInventory();
                    } else {
                        MineGUI.openActivePlayersMenu(player, mine, plugin, 1);
                    }
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.edit.delete")).trim())) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        com.sasha.prisonminesx.api.events.MineDeleteEvent delEvent = new com.sasha.prisonminesx.api.events.MineDeleteEvent(mine);
                        org.bukkit.Bukkit.getPluginManager().callEvent(delEvent);
                        if (delEvent.isCancelled()) return;

                        mine.getComposition().clear();
                        plugin.getMineManager().addMine(mine);
                        plugin.getMineManager().resetMine(mineName, false);
                        plugin.getMineManager().deleteMine(mineName);
                        player.sendMessage(getMsg("prefix") + getMsg("admin.deleted").replace("%mine%", mineName));
                        player.closeInventory();
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.shift-right-delete"));
                    }
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.edit.blocks")).trim())) {
                    MineGUI.openBlockEditor(player, mine, plugin);
                }
            }
            else if (cleanTitle.startsWith(activeBase)) {
                String mineName = cleanTitle.replace(activeBase, "").split("\\|")[0].trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.back")).trim())) {
                    MineGUI.openEditMenu(player, mine, plugin);
                    return;
                }
                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.next-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openActivePlayersMenu(player, mine, plugin, page + 1);
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.main.prev-page")).trim())) {
                    int page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openActivePlayersMenu(player, mine, plugin, page - 1);
                } else if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    Player target = Bukkit.getPlayer(cleanItemName);
                    if (target != null && target.isOnline()) {
                        player.teleport(target);
                        player.sendMessage(getMsg("prefix") + "§7Teleported to §b" + target.getName() + "§7.");
                        player.closeInventory();
                    }
                }
            }
            else if (cleanTitle.startsWith(flagsBase)) {
                String mineName = cleanTitle.replace(flagsBase, "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.back")).trim())) { MineGUI.openEditMenu(player, mine, plugin); return; }

                boolean needsRefresh = false;

                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.style")).trim())) {
                    String[] styles = {"BOTTOM_UP", "INSTANT", "VERTICAL_SLICES", "CENTER_OUT", "OUTSIDE_IN", "CORNER_SWEEP", "WAVE", "DIAMOND", "SPIRAL"};
                    String current = mine.getResetStyle();
                    int currentIdx = 0;
                    for (int i = 0; i < styles.length; i++) {
                        if (styles[i].equalsIgnoreCase(current)) {
                            currentIdx = i;
                            break;
                        }
                    }
                    int nextIdx;
                    if (event.isRightClick()) {
                        nextIdx = (currentIdx - 1 + styles.length) % styles.length;
                    } else {
                        nextIdx = (currentIdx + 1) % styles.length;
                    }
                    mine.setResetStyle(styles[nextIdx]);
                    needsRefresh = true;
                }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.warn-global")).trim())) {
                    String[] modes = {"GLOBAL", "NEARBY"};
                    int currentIdx = mine.getWarnMode().equalsIgnoreCase("GLOBAL") ? 0 : 1;
                    int nextIdx = event.isRightClick() ? (currentIdx - 1 + modes.length) % modes.length : (currentIdx + 1) % modes.length;
                    mine.setWarnMode(modes[nextIdx]);
                    needsRefresh = true;
                }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.fillmode")).trim())) { mine.setFillMode(!mine.isFillMode()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.silent")).trim())) { mine.setSilent(!mine.isSilent()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.tp-reset")).trim())) { mine.setTeleportOnReset(!mine.isTeleportOnReset()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.actionbar")).trim())) { mine.setActionbarEnabled(!mine.isActionbarEnabled()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.pause")).trim())) { mine.setPaused(!mine.isPaused()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.mine-fly")).trim())) { mine.setMineFly(!mine.isMineFly()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.hunger")).trim())) { mine.setHunger(!mine.isHunger()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.fall-damage")).trim())) { mine.setFallDamage(!mine.isFallDamage()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.pvp")).trim())) { mine.setPvp(!mine.isPvp()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.place-blocks")).trim())) { mine.setPlaceBlocks(!mine.isPlaceBlocks()); needsRefresh = true; }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.hologram")).trim())) {
                    mine.setHologramEnabled(!mine.isHologramEnabled());
                    plugin.getHologramManager().updateHologram(mine);
                    needsRefresh = true;
                }

                if (needsRefresh) {
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine, plugin);
                    return;
                }

                if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.icon")).trim())) {
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:displayitem");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.click-inv-icon"));
                }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.surface")).trim())) {
                    if (event.isRightClick()) {
                        mine.setSurface(null);
                        plugin.getMineManager().addMine(mine);
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.surface-cleared"));
                        MineGUI.openFlagsMenu(player, mine, plugin);
                    } else {
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:surface");
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.click-inv-surface"));
                    }
                }
                else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.delay")).trim())) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:delay");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-delay"));
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.warnings")).trim())) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:warnings");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-warnings"));
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.percent")).trim())) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:percent");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-percent-reset"));
                } else if (cleanItemName.equals(ChatColor.stripColor(getMsg("gui.flags.schedule")).trim())) {
                    if (event.isRightClick()) {
                        mine.getResetSchedules().clear();
                        plugin.getMineManager().addMine(mine);
                        player.sendMessage(getMsg("prefix") + "&7Reset schedule cleared for &3" + mine.getName() + "&7.");
                        MineGUI.openFlagsMenu(player, mine, plugin);
                    } else {
                        player.closeInventory();
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:schedule");
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-schedule"));
                    }
                }
            }
            else if (cleanTitle.startsWith(blocksBase)) {
                String mineName = cleanTitle.replace(blocksBase, "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                int guiSize = event.getInventory().getSize();

                if (event.getRawSlot() == guiSize - 9 && cleanItemName.equals(ChatColor.stripColor(getMsg("gui.blocks.back")).trim())) {
                    MineGUI.openEditMenu(player, mine, plugin);
                    return;
                }
                if (event.getRawSlot() == guiSize - 1 && cleanItemName.equals(ChatColor.stripColor(getMsg("gui.blocks.force-reset")).trim())) {
                    plugin.getMineManager().resetMine(mineName, true);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.forced-reset").replace("%mine%", mineName));
                    return;
                }

                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory()) && event.getRawSlot() < guiSize - 9) {
                    String material = event.getCurrentItem().getType().name();
                    String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(material);

                    if (event.isRightClick()) {
                        mine.getComposition().remove(material);
                        plugin.getMineManager().addMine(mine);
                        player.sendMessage(getMsg("prefix") + getMsg("commands.block-removed").replace("%block%", niceName).replace("%mine%", mineName));
                        MineGUI.openBlockEditor(player, mine, plugin);
                    } else if (event.isLeftClick()) {
                        player.closeInventory();
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":block:" + material);
                        player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-percent").replace("%block%", niceName));
                    }
                }
            }
        }
    }
}