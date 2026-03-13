package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.service.ShopService;
import de.mcbesser.challenges.service.MainMenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopListener implements Listener {

    private final ShopService shopService;
    private final MainMenuService mainMenuService;

    public ShopListener(ShopService shopService, MainMenuService mainMenuService) {
        this.shopService = shopService;
        this.mainMenuService = mainMenuService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!shopService.isShopTitle(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            shopService.handleClick(player, title, event.getCurrentItem(), mainMenuService);
        }
    }
}
