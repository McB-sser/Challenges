package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.model.ChallengePeriod;
import de.mcbesser.challenges.service.MainMenuService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MainMenuListener implements Listener {

    private final MainMenuService mainMenuService;

    public MainMenuListener(MainMenuService mainMenuService) {
        this.mainMenuService = mainMenuService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(MainMenuService.MAIN_MENU_TITLE) && !title.equals(MainMenuService.CHALLENGE_MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        String action = mainMenuService.readAction(current);
        if (action == null) {
            return;
        }

        if (title.equals(MainMenuService.MAIN_MENU_TITLE)) {
            if (action.equals(MainMenuService.ACTION_OPEN_SHOP)) {
                mainMenuService.getShopService().openShop(player);
            } else if (action.equals(MainMenuService.ACTION_OPEN_MODULES)) {
                mainMenuService.getShopService().openModules(player);
            } else if (action.equals(MainMenuService.ACTION_OPEN_CHALLENGES)) {
                mainMenuService.openChallengesMenu(player);
            }
            return;
        }

        if (action.equals(MainMenuService.ACTION_BACK)) {
            mainMenuService.openMainMenu(player);
            return;
        }

        if (!action.equals(MainMenuService.ACTION_CHALLENGE)) {
            return;
        }

        if (event.getClick() != ClickType.RIGHT) {
            return;
        }

        ChallengePeriod period = mainMenuService.readPeriod(current);
        Integer index = mainMenuService.readIndex(current);
        if (period == null || index == null) {
            return;
        }

        boolean skipped = mainMenuService.getChallengeService().skipChallenge(player, period, index);
        if (skipped) {
            player.sendMessage(ChatColor.YELLOW + "Challenge übersprungen.");
        } else {
            player.sendMessage(ChatColor.RED + "Skip nicht möglich.");
        }
        mainMenuService.openChallengesMenu(player);
    }
}
