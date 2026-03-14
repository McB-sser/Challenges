package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.service.MainMenuService;
import de.mcbesser.challenges.service.MenuItemService;
import de.mcbesser.challenges.service.ChallengeScoreboardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MenuItemListener implements Listener {

    private final MenuItemService menuItemService;
    private final MainMenuService mainMenuService;
    private final ChallengeScoreboardService scoreboardService;

    public MenuItemListener(MenuItemService menuItemService, MainMenuService mainMenuService,
                            ChallengeScoreboardService scoreboardService) {
        this.menuItemService = menuItemService;
        this.mainMenuService = mainMenuService;
        this.scoreboardService = scoreboardService;
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
        scoreboardService.refresh(player);
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        scoreboardService.refresh(event.getPlayer());
    }
}
