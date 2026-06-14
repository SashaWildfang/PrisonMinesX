package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final PrisonMinesX plugin;

    public GUIListener(PrisonMinesX plugin) { this.plugin = plugin; }

    private String getMsg(String path) {
        return plugin.getMessages().getString(path, "").replace("&", "§");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        String mainBase = getMsg("gui.main.title").split("%count%")[0];
        String editBase = getMsg("gui.edit.title").split("%mine%")[0];
        String flagsBase = getMsg("gui.flags.title").split("%mine%")[0];
        String blocksBase = getMsg("gui.blocks.title").split("%mine%")[0];

        if (title.startsWith(mainBase) || title.startsWith(editBase) || title.startsWith(flagsBase) || title.startsWith(blocksBase)) {

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (event.getCurrentItem().getType() == Material.AIR) return;

                if (title.startsWith(flagsBase) && ChatListener.promptMap.containsKey(player.getUniqueId())) {
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

                if (title.startsWith(blocksBase)) {
                    if (event.getCurrentItem().getType().isBlock()) {
                        String mineName = ChatColor.stripColor(title).replace(ChatColor.stripColor(blocksBase), "").trim();
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

            if (!event.getCurrentItem().hasItemMeta() || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (title.startsWith(mainBase)) {
                if (itemName.equals(getMsg("gui.main.next-page"))) {
                    int page = Integer.parseInt(ChatColor.stripColor(title).split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page + 1);
                } else if (itemName.equals(getMsg("gui.main.prev-page"))) {
                    int page = Integer.parseInt(ChatColor.stripColor(title).split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page - 1);
                } else if (itemName.startsWith(getMsg("gui.main.mine-name").split("%mine%")[0])) {
                    String cleanTitle = ChatColor.stripColor(itemName);
                    String cleanPrefix = ChatColor.stripColor(getMsg("gui.main.mine-name").split("%mine%")[0]);
                    String mineName = cleanTitle.replace(cleanPrefix, "").trim();

                    Mine mine = plugin.getMineManager().getMine(mineName);
                    if (mine != null) MineGUI.openEditMenu(player, mine, plugin);
                }
            }
            else if (title.startsWith(editBase)) {
                String mineName = ChatColor.stripColor(title).replace(ChatColor.stripColor(editBase), "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) { player.closeInventory(); return; }

                if (itemName.equals(getMsg("gui.edit.flags"))) {
                    MineGUI.openFlagsMenu(player, mine, plugin);
                } else if (itemName.equals(getMsg("gui.edit.back"))) {
                    MineGUI.openMainMenu(player, plugin, 1);
                } else if (itemName.equals(getMsg("gui.edit.teleport"))) {
                    mine.setTpLocation(player.getLocation());
                    plugin.getMineManager().addMine(mine);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.tp-set").replace("%mine%", mineName));
                    player.closeInventory();
                } else if (itemName.equals(getMsg("gui.edit.delete"))) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        mine.getComposition().clear();
                        plugin.getMineManager().addMine(mine);
                        plugin.getMineManager().resetMine(mineName, false);
                        plugin.getMineManager().deleteMine(mineName);
                        player.sendMessage(getMsg("prefix") + getMsg("admin.deleted").replace("%mine%", mineName));
                        player.closeInventory();
                    } else {
                        player.sendMessage(getMsg("prefix") + getMsg("commands.shift-right-delete"));
                    }
                } else if (itemName.equals(getMsg("gui.edit.blocks"))) {
                    MineGUI.openBlockEditor(player, mine, plugin);
                }
            }
            else if (title.startsWith(flagsBase)) {
                String mineName = ChatColor.stripColor(title).replace(ChatColor.stripColor(flagsBase), "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (itemName.equals(getMsg("gui.flags.back"))) { MineGUI.openEditMenu(player, mine, plugin); return; }

                if (itemName.equals(getMsg("gui.flags.fillmode"))) { mine.setFillMode(!mine.isFillMode()); }
                else if (itemName.equals(getMsg("gui.flags.silent"))) { mine.setSilent(!mine.isSilent()); }
                else if (itemName.equals(getMsg("gui.flags.tp-reset"))) { mine.setTeleportOnReset(!mine.isTeleportOnReset()); }
                else if (itemName.equals(getMsg("gui.flags.actionbar"))) { mine.setActionbarEnabled(!mine.isActionbarEnabled()); }
                else if (itemName.equals(getMsg("gui.flags.warn-global"))) { mine.setWarnGlobal(!mine.isWarnGlobal()); }
                else if (itemName.equals(getMsg("gui.flags.pause"))) { mine.setPaused(!mine.isPaused()); }
                else if (itemName.equals(getMsg("gui.flags.hologram"))) {
                    mine.setHologramEnabled(!mine.isHologramEnabled());
                    plugin.getHologramManager().updateHologram(mine);
                }

                if (itemName.startsWith(getMsg("gui.flags.fillmode")) || itemName.startsWith(getMsg("gui.flags.silent")) ||
                        itemName.startsWith(getMsg("gui.flags.tp-reset")) || itemName.startsWith(getMsg("gui.flags.actionbar")) ||
                        itemName.startsWith(getMsg("gui.flags.warn-global")) || itemName.startsWith(getMsg("gui.flags.pause")) ||
                        itemName.startsWith(getMsg("gui.flags.hologram"))) {
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine, plugin);
                    return;
                }

                if (itemName.equals(getMsg("gui.flags.icon"))) {
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:displayitem");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.click-inv-icon"));
                }
                else if (itemName.equals(getMsg("gui.flags.surface"))) {
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
                else if (itemName.equals(getMsg("gui.flags.delay"))) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:delay");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-delay"));
                } else if (itemName.equals(getMsg("gui.flags.warnings"))) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:warnings");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-warnings"));
                } else if (itemName.equals(getMsg("gui.flags.percent"))) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:percent");
                    player.sendMessage(getMsg("prefix") + getMsg("prompts.enter-percent-reset"));
                }
            }
            else if (title.startsWith(blocksBase)) {
                String mineName = ChatColor.stripColor(title).replace(ChatColor.stripColor(blocksBase), "").trim();
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (event.getRawSlot() == 45 && itemName.equals(getMsg("gui.blocks.back"))) {
                    MineGUI.openEditMenu(player, mine, plugin);
                    return;
                }
                if (event.getRawSlot() == 53 && itemName.equals(getMsg("gui.blocks.force-reset"))) {
                    plugin.getMineManager().resetMine(mineName, true);
                    player.sendMessage(getMsg("prefix") + getMsg("admin.forced-reset").replace("%mine%", mineName));
                    return;
                }

                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory()) && event.getRawSlot() < 45) {
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