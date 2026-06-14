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

    public GUIListener(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("§8PrisonMinesX") || title.startsWith("§8Editing:") ||
                title.startsWith("§8Flags:") || title.startsWith("§8Blocks:") || title.startsWith("§8Mines - ")) {

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (event.getCurrentItem().getType() == Material.AIR) return;

                if (title.startsWith("§8Flags: ") && ChatListener.promptMap.containsKey(player.getUniqueId())) {
                    String[] data = ChatListener.promptMap.get(player.getUniqueId()).split(":");
                    if (data.length > 1 && data[2].equals("displayitem")) {
                        Mine mine = plugin.getMineManager().getMine(data[0]);
                        if (mine != null) {
                            mine.setDisplayItem(event.getCurrentItem().getType().name());
                            plugin.getMineManager().addMine(mine);
                            ChatListener.promptMap.remove(player.getUniqueId());
                            player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§aDisplay item set!");
                            MineGUI.openFlagsMenu(player, mine);
                        }
                    }
                    return;
                }

                if (title.startsWith("§8Blocks: ")) {
                    if (event.getCurrentItem().getType().isBlock()) {
                        String mineName = title.replace("§8Blocks: §b", "");
                        Mine mine = plugin.getMineManager().getMine(mineName);
                        if (mine == null) return;

                        String material = event.getCurrentItem().getType().name();
                        String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(material);

                        if (mine.getComposition().containsKey(material)) {
                            player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§c" + niceName + " is already in the mine!");
                            player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§eIf you want to edit its percentage, please click it in the top GUI menu.");
                            return;
                        }

                        player.closeInventory();
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":block:" + material);
                        player.sendMessage("§eType the percentage to set for §a" + niceName + " §e(0-100) in chat, or type 'cancel'.");
                    } else {
                        player.sendMessage("§cYou can only add solid blocks to a mine!");
                    }
                    return;
                }
                return;
            }

            if (!event.getCurrentItem().hasItemMeta() || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (title.startsWith("§8Mines - ")) {
                if (itemName.equals("§aNext Page")) {
                    int page = Integer.parseInt(title.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page + 1);
                } else if (itemName.equals("§cPrevious Page")) {
                    int page = Integer.parseInt(title.split("Pg ")[1].replace(")", "").trim());
                    MineGUI.openMainMenu(player, plugin, page - 1);
                } else if (itemName.startsWith("§b§lMine: §f")) {
                    String mineName = ChatColor.stripColor(itemName).replace("Mine: ", "");
                    Mine mine = plugin.getMineManager().getMine(mineName);
                    if (mine != null) {
                        MineGUI.openEditMenu(player, mine, plugin);
                    }
                }
            }
            else if (title.startsWith("§8Editing: ")) {
                String mineName = title.replace("§8Editing: §b", "");
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) {
                    player.closeInventory();
                    return;
                }

                if (itemName.equals("§6Edit Flags")) {
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§cBack to Mines")) {
                    MineGUI.openMainMenu(player, plugin, 1);
                } else if (itemName.equals("§aSet Teleport Location")) {
                    mine.setTpLocation(player.getLocation());
                    plugin.getMineManager().addMine(mine);
                    player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§aTeleport location set.");
                    player.closeInventory();
                } else if (itemName.equals("§eForce Reset")) {
                    plugin.getMineManager().resetMine(mineName, true); // Triggers forced flash
                    player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§aMine reset forced.");
                } else if (itemName.equals("§cDelete Mine")) {
                    if (event.isShiftClick() && event.isRightClick()) {

                        // FIX: Safe Deletion Wipe!
                        mine.getComposition().clear();
                        plugin.getMineManager().addMine(mine);
                        plugin.getMineManager().resetMine(mineName, false);

                        plugin.getMineManager().deleteMine(mineName);

                        player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§cMine deleted.");
                        player.closeInventory();
                    } else {
                        player.sendMessage("§cYou must Shift-Right-Click to delete a mine!");
                    }
                } else if (itemName.equals("§bEdit Block Composition")) {
                    MineGUI.openBlockEditor(player, mine);
                }
            }
            else if (title.startsWith("§8Flags: ")) {
                String mineName = title.replace("§8Flags: §b", "");
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (itemName.equals("§cGo Back")) {
                    MineGUI.openEditMenu(player, mine, plugin);
                    return;
                }

                if (itemName.equals("§6Fill Mode")) {
                    mine.setFillMode(!mine.isFillMode());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Silent Reset")) {
                    mine.setSilent(!mine.isSilent());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Teleport on Reset")) {
                    mine.setTeleportOnReset(!mine.isTeleportOnReset());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Hologram")) {
                    mine.setHologramEnabled(!mine.isHologramEnabled());
                    plugin.getMineManager().addMine(mine);
                    plugin.getHologramManager().updateHologram(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Actionbar Notifs")) {
                    mine.setActionbarEnabled(!mine.isActionbarEnabled());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Warning Broadcast")) {
                    mine.setWarnGlobal(!mine.isWarnGlobal());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                } else if (itemName.equals("§6Stop Resetting")) {
                    mine.setPaused(!mine.isPaused());
                    plugin.getMineManager().addMine(mine);
                    MineGUI.openFlagsMenu(player, mine);
                }

                else if (itemName.equals("§dSet Display Item")) {
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:displayitem");
                    player.sendMessage("§ePlease click an item in your inventory below to set it as the display item.");
                } else if (itemName.equals("§eReset Delay")) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:delay");
                    player.sendMessage("§eType the new Reset Delay (e.g. 10m or 1h 20m 4s), or type 'cancel'.");
                } else if (itemName.equals("§eReset Warnings")) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:warnings");
                    player.sendMessage("§eType the warnings in minutes separated by commas (e.g. 10,5,1), or type 'cancel'.");
                } else if (itemName.equals("§eSurface Block")) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:surface");
                    player.sendMessage("§eType the Material name for the surface (or 'air' to clear), or type 'cancel'.");
                } else if (itemName.equals("§ePercent Reset")) {
                    player.closeInventory();
                    ChatListener.promptMap.put(player.getUniqueId(), mineName + ":flag:percent");
                    player.sendMessage("§eType the percentage for auto-reset (0-100), or type 'cancel'.");
                }
            }
            else if (title.startsWith("§8Blocks: ")) {
                String mineName = title.replace("§8Blocks: §b", "");
                Mine mine = plugin.getMineManager().getMine(mineName);
                if (mine == null) return;

                if (event.getRawSlot() == 45 && event.getCurrentItem().getType() == Material.ARROW) {
                    MineGUI.openEditMenu(player, mine, plugin);
                    return;
                }

                if (event.getRawSlot() == 53 && event.getCurrentItem().getType() == Material.GOLDEN_PICKAXE) {
                    plugin.getMineManager().resetMine(mineName, true); // Triggers forced flash
                    player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§aMine reset forced.");
                    return;
                }

                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory()) && event.getRawSlot() < 45) {
                    String material = event.getCurrentItem().getType().name();
                    String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(material);

                    if (event.isRightClick()) {
                        mine.getComposition().remove(material);
                        plugin.getMineManager().addMine(mine);
                        player.sendMessage(plugin.getMessages().getString("prefix").replace("&", "§") + "§cRemoved " + niceName + " from " + mineName + ".");
                        MineGUI.openBlockEditor(player, mine);
                    } else if (event.isLeftClick()) {
                        double currentPercent = mine.getComposition().getOrDefault(material, 0.0);
                        player.closeInventory();
                        ChatListener.promptMap.put(player.getUniqueId(), mineName + ":block:" + material);
                        player.sendMessage("§eMine §b" + mineName + " §ecurrently contains §a" + currentPercent + "% " + niceName + "§e.");
                        player.sendMessage("§eType the new percentage (0-100) in chat for " + niceName + ", or type 'cancel'.");
                    }
                }
            }
        }
    }
}