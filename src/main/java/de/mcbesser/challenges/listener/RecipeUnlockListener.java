package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.service.MenuItemService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RecipeUnlockListener implements Listener {

    private final MenuItemService menuItemService;

    public RecipeUnlockListener(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(menuItemService.getRecipeKey());
    }
}
