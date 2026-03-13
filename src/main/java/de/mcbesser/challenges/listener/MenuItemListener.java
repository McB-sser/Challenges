package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.service.MainMenuService;
import de.mcbesser.challenges.service.MenuItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MenuItemListener implements Listener {

    private final MenuItemService menuItemService;
    private final MainMenuService mainMenuService;

    public MenuItemListener(MenuItemService menuItemService, MainMenuService mainMenuService) {
        this.menuItemService = menuItemService;
        this.mainMenuService = mainMenuService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!menuItemService.isMenuItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        mainMenuService.openMainMenu(player);
    }
}
