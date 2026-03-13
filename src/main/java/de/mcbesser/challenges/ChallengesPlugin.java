package de.mcbesser.challenges;

import de.mcbesser.challenges.data.PlayerDataStore;
import de.mcbesser.challenges.listener.ChallengeProgressListener;
import de.mcbesser.challenges.listener.MainMenuListener;
import de.mcbesser.challenges.listener.MenuItemListener;
import de.mcbesser.challenges.listener.RecipeUnlockListener;
import de.mcbesser.challenges.listener.ShopListener;
import de.mcbesser.challenges.service.ChallengeService;
import de.mcbesser.challenges.service.MainMenuService;
import de.mcbesser.challenges.service.MenuItemService;
import de.mcbesser.challenges.service.ShopService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChallengesPlugin extends JavaPlugin {

    private PlayerDataStore dataStore;
    private ChallengeService challengeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataStore = new PlayerDataStore(this);
        dataStore.load();

        challengeService = new ChallengeService(this, dataStore);
        ShopService shopService = new ShopService(this, challengeService);
        MainMenuService mainMenuService = new MainMenuService(this, challengeService, shopService);
        MenuItemService menuItemService = new MenuItemService(this);
        menuItemService.registerRecipe();

        getServer().getPluginManager().registerEvents(new ChallengeProgressListener(this, challengeService, menuItemService, shopService), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopService, mainMenuService), this);
        getServer().getPluginManager().registerEvents(new MainMenuListener(mainMenuService), this);
        getServer().getPluginManager().registerEvents(new MenuItemListener(menuItemService, mainMenuService), this);
        getServer().getPluginManager().registerEvents(new RecipeUnlockListener(menuItemService), this);

        getServer().getOnlinePlayers().forEach(player -> player.discoverRecipe(menuItemService.getRecipeKey()));

        challengeService.startSchedulers();
        challengeService.initializeOnlinePlayers();
    }

    @Override
    public void onDisable() {
        if (challengeService != null) {
            challengeService.shutdown();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }
}
