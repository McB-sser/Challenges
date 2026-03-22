package de.mcbesser.challenges.listener;

import de.mcbesser.challenges.model.ChallengeType;
import de.mcbesser.challenges.service.ChallengeScoreboardService;
import de.mcbesser.challenges.service.ChallengeService;
import de.mcbesser.challenges.service.MenuItemService;
import de.mcbesser.challenges.service.ShopService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeProgressListener implements Listener {

    private static final Set<Material> STONE_BLOCKS = EnumSet.of(
            Material.STONE, Material.COBBLESTONE,
            Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.TUFF, Material.CALCITE,
            Material.BLACKSTONE, Material.BASALT
    );

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> LANDSCAPING_BLOCKS = EnumSet.of(
            Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
            Material.GRASS_BLOCK, Material.PODZOL, Material.MYCELIUM,
            Material.DIRT_PATH, Material.FARMLAND,
            Material.MUD, Material.CLAY,
            Material.SAND, Material.RED_SAND, Material.GRAVEL
    );

    private static final Set<Material> PLANTABLE_BLOCKS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP,
            Material.MELON_STEM, Material.ATTACHED_MELON_STEM,
            Material.PUMPKIN_STEM, Material.ATTACHED_PUMPKIN_STEM,
            Material.SUGAR_CANE, Material.CACTUS, Material.BAMBOO,
            Material.SWEET_BERRY_BUSH, Material.COCOA,
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
    );

    private final ChallengeService challengeService;
    private final ChallengeScoreboardService scoreboardService;
    private final MenuItemService menuItemService;
    private final ShopService shopService;
    private final JavaPlugin plugin;
    private final Map<UUID, Double> walkedDistanceBuffer = new ConcurrentHashMap<>();

    public ChallengeProgressListener(JavaPlugin plugin, ChallengeService challengeService,
                                     ChallengeScoreboardService scoreboardService,
                                     MenuItemService menuItemService, ShopService shopService) {
        this.plugin = plugin;
        this.challengeService = challengeService;
        this.scoreboardService = scoreboardService;
        this.menuItemService = menuItemService;
        this.shopService = shopService;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        shopService.handleBreakCosmetic(player, event.getBlock().getLocation());
        if (!menuItemService.hasMenuItemInInventory(player)) {
            return;
        }

        boolean updated = false;
        Material blockType = event.getBlock().getType();
        if (STONE_BLOCKS.contains(blockType)) {
            challengeService.addProgress(player, ChallengeType.MINE_STONE, 1);
            updated = true;
        }
        if (ORES.contains(blockType)) {
            challengeService.addProgress(player, ChallengeType.MINE_ORE, 1);
            updated = true;
        }
        if (LANDSCAPING_BLOCKS.contains(blockType) || isHarvestedPlant(event)) {
            challengeService.addProgress(player, ChallengeType.LANDSCAPING, 1);
            updated = true;
        }
        if (updated) {
            scoreboardService.refresh(player);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!menuItemService.hasMenuItemInInventory(player)) {
            return;
        }
        Material blockType = event.getBlockPlaced().getType();
        if (!LANDSCAPING_BLOCKS.contains(blockType) && !PLANTABLE_BLOCKS.contains(blockType)) {
            return;
        }
        challengeService.addProgress(player, ChallengeType.LANDSCAPING, 1);
        scoreboardService.refresh(player);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        Player player = event.getEntity().getKiller();
        if (event.getEntityType() == EntityType.ARMOR_STAND) {
            return;
        }
        if (!menuItemService.hasMenuItemInInventory(player)) {
            return;
        }
        challengeService.addProgress(player, ChallengeType.KILL_MOB, 1);
        scoreboardService.refresh(player);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (!menuItemService.hasMenuItemInInventory(event.getPlayer())) {
                return;
            }
            challengeService.addProgress(event.getPlayer(), ChallengeType.FISH, 1);
            scoreboardService.refresh(event.getPlayer());
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!menuItemService.hasMenuItemInInventory(player)) {
                return;
            }
            if (event.isCancelled()) {
                return;
            }
            ItemStack result = event.getCurrentItem();
            if (result == null || result.getType() == Material.AIR || result.getAmount() <= 0) {
                return;
            }
            int before = countSimilar(player, result);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int after = countSimilar(player, result);
                int delta = Math.max(0, after - before);
                if (delta > 0) {
                    challengeService.addProgress(player, ChallengeType.CRAFT, delta);
                    scoreboardService.refresh(player);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player && event.getEntity() instanceof Animals) {
            if (!menuItemService.hasMenuItemInInventory(player)) {
                return;
            }
            challengeService.addProgress(player, ChallengeType.BREED, 1);
            scoreboardService.refresh(player);
        }
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent event) {
        if (!menuItemService.hasMenuItemInInventory(event.getPlayer())) {
            return;
        }
        challengeService.addProgress(event.getPlayer(), ChallengeType.SMELT, event.getItemAmount());
        scoreboardService.refresh(event.getPlayer());
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (!menuItemService.hasMenuItemInInventory(event.getEnchanter())) {
            return;
        }
        challengeService.addProgress(event.getEnchanter(), ChallengeType.ENCHANT, 1);
        scoreboardService.refresh(event.getEnchanter());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        shopService.handleWalkCosmetic(event.getPlayer(), event.getTo());
        if (event.getPlayer().isGliding()) {
            shopService.handleElytraCosmetic(event.getPlayer(), event.getTo());
        }

        if (!menuItemService.hasMenuItemInInventory(event.getPlayer())) {
            return;
        }
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            walkedDistanceBuffer.remove(event.getPlayer().getUniqueId());
            return;
        }

        double step = event.getFrom().distance(event.getTo());
        if (step <= 0.0) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        double total = walkedDistanceBuffer.getOrDefault(uuid, 0.0) + step;
        int wholeBlocks = (int) total;
        if (wholeBlocks > 0) {
            challengeService.addProgress(event.getPlayer(), ChallengeType.WALK_DISTANCE, wholeBlocks);
            scoreboardService.refresh(event.getPlayer());
            total -= wholeBlocks;
        }
        walkedDistanceBuffer.put(uuid, total);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        shopService.handleTeleportCosmetic(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            shopService.handleBowCosmetic(player, player.getLocation());
        }
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!menuItemService.hasMenuItemInInventory(player)) {
            return;
        }
        if (!(event.getView().getTopInventory() instanceof MerchantInventory)) {
            return;
        }
        MerchantInventory merchantInventory = (MerchantInventory) event.getView().getTopInventory();
        if (event.getSlotType() != InventoryType.SlotType.RESULT || event.getRawSlot() != 2) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        MerchantRecipe recipe = merchantInventory.getSelectedRecipe();
        if (recipe == null || recipe.getResult() == null || recipe.getResult().getType() == Material.AIR) {
            return;
        }

        ItemStack before0 = cloneOrAir(merchantInventory.getItem(0));
        ItemStack before1 = cloneOrAir(merchantInventory.getItem(1));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack after0 = cloneOrAir(merchantInventory.getItem(0));
            ItemStack after1 = cloneOrAir(merchantInventory.getItem(1));

            int consumed0 = consumedAmount(before0, after0);
            int consumed1 = consumedAmount(before1, after1);

            if (consumed0 <= 0 && consumed1 <= 0) {
                return;
            }

            int tradesDone = estimateTradesDone(recipe, consumed0, consumed1);
            int resultItems = Math.max(1, recipe.getResult().getAmount() * tradesDone);
            if (resultItems > 0) {
                challengeService.addProgress(player, ChallengeType.TRADE_VILLAGER, resultItems);
                scoreboardService.refresh(player);
            }
        }, 1L);
    }

    private int estimateTradesDone(MerchantRecipe recipe, int consumed0, int consumed1) {
        List<ItemStack> ingredients = recipe.getIngredients();
        int perTrade0 = ingredientAmount(ingredients, 0);
        int perTrade1 = ingredientAmount(ingredients, 1);

        int byFirst = perTrade0 <= 0 ? Integer.MAX_VALUE : consumed0 / perTrade0;
        int bySecond = perTrade1 <= 0 ? Integer.MAX_VALUE : consumed1 / perTrade1;
        int trades = Math.min(byFirst, bySecond);
        if (trades == Integer.MAX_VALUE || trades <= 0) {
            return 1;
        }
        return trades;
    }

    private int ingredientAmount(List<ItemStack> ingredients, int index) {
        if (index >= ingredients.size()) {
            return 0;
        }
        ItemStack stack = ingredients.get(index);
        if (stack == null || stack.getType() == Material.AIR) {
            return 0;
        }
        return Math.max(1, stack.getAmount());
    }

    private int consumedAmount(ItemStack before, ItemStack after) {
        if (before == null || before.getType() == Material.AIR) {
            return 0;
        }
        if (after == null || after.getType() == Material.AIR) {
            return before.getAmount();
        }
        if (!before.isSimilar(after)) {
            return before.getAmount();
        }
        return Math.max(0, before.getAmount() - after.getAmount());
    }

    private ItemStack cloneOrAir(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        return stack.clone();
    }

    private int countSimilar(Player player, ItemStack compare) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() != Material.AIR && stack.isSimilar(compare)) {
                total += stack.getAmount();
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR && cursor.isSimilar(compare)) {
            total += cursor.getAmount();
        }
        return total;
    }

    private boolean isHarvestedPlant(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (!PLANTABLE_BLOCKS.contains(type)) {
            return false;
        }
        if (!(event.getBlock().getBlockData() instanceof Ageable)) {
            return true;
        }
        Ageable ageable = (Ageable) event.getBlock().getBlockData();
        return ageable.getAge() >= ageable.getMaximumAge();
    }
}
