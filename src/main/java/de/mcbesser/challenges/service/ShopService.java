
package de.mcbesser.challenges.service;

import de.mcbesser.challenges.model.PlayerProgress;
import de.mcbesser.challenges.model.ShopCategory;
import de.mcbesser.challenges.model.ShopReward;
import de.mcbesser.challenges.model.ShopUsageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ShopService {

    private static final String SHOP_TITLE = ChatColor.GOLD + "Token-Shop";
    private static final String MODULES_TITLE = ChatColor.LIGHT_PURPLE + "Meine Module";
    private static final String EFFECTS_TITLE = ChatColor.AQUA + "Effekte";
    private static final String COSMETIC_TITLE = ChatColor.LIGHT_PURPLE + "Kosmetik";
    private static final String CRAFTING_TITLE = ChatColor.YELLOW + "Werkbänke & Verzauberung";
    private static final String MOBS_TITLE = ChatColor.DARK_GREEN + "Spawn-Eier";

    private static final String ACTION_BUY = "buy";
    private static final String ACTION_TOGGLE = "toggle";
    private static final String ACTION_USE = "use";
    private static final String ACTION_OPEN_CATEGORY = "open_category";
    private static final String ACTION_BACK_HUB = "back_hub";
    private static final String ACTION_BACK_SHOP = "back_shop";

    private final JavaPlugin plugin;
    private final ChallengeService challengeService;
    private final List<ShopReward> rewards;
    private final Map<ShopCategory, String> titlesByCategory = new EnumMap<>(ShopCategory.class);
    private final Map<String, Material> spawnEggByRewardId;

    private final NamespacedKey actionKey;
    private final NamespacedKey payloadKey;
    private BukkitTask moduleTickTask;

    public ShopService(JavaPlugin plugin, ChallengeService challengeService) {
        this.plugin = plugin;
        this.challengeService = challengeService;
        this.actionKey = new NamespacedKey(plugin, "shop_action");
        this.payloadKey = new NamespacedKey(plugin, "shop_payload");
        this.rewards = buildRewards();
        this.spawnEggByRewardId = buildSpawnEggMap();

        titlesByCategory.put(ShopCategory.EFFECTS, EFFECTS_TITLE);
        titlesByCategory.put(ShopCategory.COSMETIC, COSMETIC_TITLE);
        titlesByCategory.put(ShopCategory.CRAFTING, CRAFTING_TITLE);
        titlesByCategory.put(ShopCategory.MOBS, MOBS_TITLE);

        startModuleTicker();
    }

    public void openShop(Player player) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, SHOP_TITLE);

        // Gleichmäßige Verteilung der Kategorien
        inv.setItem(9, actionItem(Material.POTION, ChatColor.AQUA + "Effekte", List.of(
                ChatColor.GRAY + "Positive Trank- und Beacon-Effekte"
        ), ACTION_OPEN_CATEGORY, ShopCategory.EFFECTS.name()));
        inv.setItem(11, actionItem(Material.FIREWORK_STAR, ChatColor.LIGHT_PURPLE + "Kosmetik", List.of(
                ChatColor.GRAY + "Allgemein, Teleport, Abbau, Laufen, Bogen, Elytra"
        ), ACTION_OPEN_CATEGORY, ShopCategory.COSMETIC.name()));
        inv.setItem(15, actionItem(Material.ENCHANTING_TABLE, ChatColor.YELLOW + "Werkbänke", List.of(
                ChatColor.GRAY + "Mobile Blöcke inkl. Enderkiste und Zaubertisch 0-15"
        ), ACTION_OPEN_CATEGORY, ShopCategory.CRAFTING.name()));
        inv.setItem(17, actionItem(Material.EGG, ChatColor.DARK_GREEN + "Spawn-Eier", List.of(
                ChatColor.GRAY + "Friedliche Mobs vollständig"
        ), ACTION_OPEN_CATEGORY, ShopCategory.MOBS.name()));

        inv.setItem(22, plainItem(Material.SUNFLOWER, ChatColor.GOLD + "Token-Kontostand", List.of(
                ChatColor.YELLOW + "Verfügbar: " + progress.getTokens() + " Token",
                ChatColor.GRAY + "Aktuelle Stufe: " + progress.getGroupTier()
        )));
        inv.setItem(26, actionItem(Material.ARROW, ChatColor.GRAY + "Zurück", List.of(
                ChatColor.DARK_GRAY + "Zum Herausforderungs-Hub"
        ), ACTION_BACK_HUB, ""));

        player.openInventory(inv);
    }

    public void openModules(Player player) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, MODULES_TITLE);

        List<ShopReward> unlocked = rewards.stream()
                .filter(reward -> progress.getUnlockedRewards().contains(reward.id()))
                .sorted(rewardComparator())
                .toList();

        int slot = 0;
        for (ShopReward reward : unlocked) {
            if (slot >= 53) {
                break;
            }
            if (slot == 53) {
                break;
            }
            inv.setItem(slot, moduleItem(progress, reward));
            slot++;
            if (slot == 53) {
                break;
            }
        }

        if (unlocked.isEmpty()) {
            inv.setItem(22, plainItem(Material.BARRIER, ChatColor.RED + "Noch keine Module", List.of(
                    ChatColor.GRAY + "Kaufe Module zuerst im Token-Shop."
            )));
        }

        inv.setItem(53, actionItem(Material.ARROW, ChatColor.GRAY + "Zurück", List.of(
                ChatColor.DARK_GRAY + "Zum Herausforderungs-Hub"
        ), ACTION_BACK_HUB, ""));
        player.openInventory(inv);
    }

    public boolean isShopTitle(String title) {
        return Objects.equals(title, SHOP_TITLE)
                || Objects.equals(title, MODULES_TITLE)
                || Objects.equals(title, EFFECTS_TITLE)
                || Objects.equals(title, COSMETIC_TITLE)
                || Objects.equals(title, CRAFTING_TITLE)
                || Objects.equals(title, MOBS_TITLE);
    }

    public void handleClick(Player player, String title, ItemStack item, MainMenuService mainMenuService) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String action = readAction(item);
        String payload = readPayload(item);
        if (action == null) {
            return;
        }

        if (ACTION_BACK_HUB.equals(action)) {
            mainMenuService.openMainMenu(player);
            return;
        }
        if (ACTION_BACK_SHOP.equals(action)) {
            openShop(player);
            return;
        }

        if (SHOP_TITLE.equals(title) && ACTION_OPEN_CATEGORY.equals(action)) {
            openCategory(player, payload);
            return;
        }

        if (ACTION_BUY.equals(action)) {
            findReward(payload).ifPresent(reward -> buyReward(player, reward));
            return;
        }

        if (MODULES_TITLE.equals(title)) {
            Optional<ShopReward> rewardOpt = findReward(payload);
            if (rewardOpt.isEmpty()) {
                return;
            }
            ShopReward reward = rewardOpt.get();
            if (ACTION_TOGGLE.equals(action)) {
                toggleModule(player, reward);
            } else if (ACTION_USE.equals(action)) {
                useChargeModule(player, reward);
            }
            openModules(player);
        }
    }

    private void openCategory(Player player, String categoryPayload) {
        ShopCategory category;
        try {
            category = ShopCategory.valueOf(categoryPayload);
        } catch (Exception ignored) {
            openShop(player);
            return;
        }

        String title = titlesByCategory.getOrDefault(category, SHOP_TITLE);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        List<ShopReward> list = rewards.stream()
                .filter(reward -> reward.category() == category)
                .sorted(rewardComparator())
                .toList();

        int slot = 0;
        for (ShopReward reward : list) {
            if (slot >= 53) {
                break;
            }
            inv.setItem(slot, shopEntry(progress, reward));
            slot++;
        }

        inv.setItem(53, actionItem(Material.ARROW, ChatColor.GRAY + "Zurück", List.of(
                ChatColor.DARK_GRAY + "Zum Token-Shop"
        ), ACTION_BACK_SHOP, ""));
        player.openInventory(inv);
    }

    private void buyReward(Player player, ShopReward reward) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        int level = progress.getGroupTier();
        if (level < reward.requiredLevel()) {
            player.sendMessage(ChatColor.RED + "Benötigte Stufe: " + reward.requiredLevel());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
            return;
        }
        if (!progress.removeTokens(reward.cost())) {
            player.sendMessage(ChatColor.RED + "Nicht genug Token.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
            return;
        }

        progress.getUnlockedRewards().add(reward.id());
        if (reward.usageType() == ShopUsageType.TOGGLE_TIME) {
            progress.getModuleSeconds().merge(reward.id(), Math.max(1, reward.durationSeconds()), Integer::sum);
        } else {
            progress.getModuleCharges().merge(reward.id(), Math.max(1, reward.chargesPerPurchase()), Integer::sum);
        }

        player.sendMessage(ChatColor.GREEN + "Gekauft: " + reward.name()
                + ChatColor.DARK_GRAY + " (-" + reward.cost() + " Token)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        openCategory(player, reward.category().name());
    }

    private void toggleModule(Player player, ShopReward reward) {
        if (reward.usageType() != ShopUsageType.TOGGLE_TIME) {
            return;
        }
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        int seconds = progress.getModuleSeconds().getOrDefault(reward.id(), 0);
        if (seconds <= 0) {
            progress.getActiveToggles().remove(reward.id());
            clearEffect(player, reward);
            player.sendMessage(ChatColor.RED + "Kein Zeitguthaben mehr für " + reward.name() + ".");
            return;
        }

        if (progress.getActiveToggles().contains(reward.id())) {
            progress.getActiveToggles().remove(reward.id());
            clearEffect(player, reward);
            player.sendMessage(ChatColor.YELLOW + reward.name() + " pausiert.");
        } else {
            progress.getActiveToggles().add(reward.id());
            player.sendMessage(ChatColor.GREEN + reward.name() + " aktiviert.");
            applyTickEffect(player, reward);
        }
    }

    private void useChargeModule(Player player, ShopReward reward) {
        if (reward.usageType() != ShopUsageType.CHARGE_USE) {
            return;
        }
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        int charges = progress.getModuleCharges().getOrDefault(reward.id(), 0);
        if (charges <= 0) {
            player.sendMessage(ChatColor.RED + "Keine Aufladungen übrig.");
            return;
        }

        if (executeCharge(player, reward.id())) {
            progress.getModuleCharges().put(reward.id(), charges - 1);
            player.sendMessage(ChatColor.GREEN + reward.name() + " benutzt. Rest: " + (charges - 1) + " Aufladung(en).");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.3f);
        } else {
            player.sendMessage(ChatColor.RED + "Dieses Modul kann hier gerade nicht genutzt werden.");
        }
    }

    private boolean executeCharge(Player player, String id) {
        if (id.startsWith("enchant_mobile_")) {
            player.openEnchanting((Location) null, true);
            return true;
        }

        return switch (id) {
            case "crafting_table_mobile" -> {
                player.openWorkbench((Location) null, true);
                yield true;
            }
            case "stonecutter_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.STONECUTTER, ChatColor.GRAY + "Mobiler Steinmetz"));
                yield true;
            }
            case "loom_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.LOOM, ChatColor.GRAY + "Mobiler Webstuhl"));
                yield true;
            }
            case "smithing_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.SMITHING, ChatColor.GRAY + "Mobiler Schmiedetisch"));
                yield true;
            }
            case "anvil_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.ANVIL, ChatColor.GRAY + "Mobiler Amboss"));
                yield true;
            }
            case "grindstone_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.GRINDSTONE, ChatColor.GRAY + "Mobiler Schleifstein"));
                yield true;
            }
            case "cartography_mobile" -> {
                player.openInventory(plugin.getServer().createInventory(player, InventoryType.CARTOGRAPHY, ChatColor.GRAY + "Mobiler Kartentisch"));
                yield true;
            }
            case "enderchest_mobile" -> {
                player.openInventory(player.getEnderChest());
                yield true;
            }
            default -> giveSpawnEgg(player, id);
        };
    }

    private boolean giveSpawnEgg(Player player, String id) {
        Material egg = spawnEggByRewardId.get(id);
        if (egg == null) {
            return false;
        }
        player.getInventory().addItem(new ItemStack(egg, 1));
        return true;
    }

    public void handleTeleportCosmetic(Player player, Location from, Location to) {
        if (from == null || to == null || !isCosmeticActive(player, "cos_tp")) {
            return;
        }
        from.getWorld().spawnParticle(Particle.PORTAL, from, 24, 0.4, 0.7, 0.4, 0.2);
        to.getWorld().spawnParticle(Particle.REVERSE_PORTAL, to, 24, 0.4, 0.7, 0.4, 0.0);
    }

    public void handleBreakCosmetic(Player player, Location at) {
        if (at == null || !isCosmeticActive(player, "cos_break")) {
            return;
        }
        Location loc = at.clone().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.25, 0.25, 0.25, 0.01);
    }

    public void handleWalkCosmetic(Player player, Location at) {
        if (at == null || !isCosmeticActive(player, "cos_walk")) {
            return;
        }
        // Wichtig: nie die Event-Location mutieren, sonst kann Bewegung verfälscht werden.
        Location loc = at.clone().add(0.0, 0.05, 0.0);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 4, 0.2, 0.03, 0.2, 0.01);
    }

    public void handleBowCosmetic(Player player, Location at) {
        if (at == null || !isCosmeticActive(player, "cos_bow")) {
            return;
        }
        Location loc = at.clone().add(0.0, 1.2, 0.0);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 14, 0.3, 0.2, 0.3, 0.02);
    }

    public void handleElytraCosmetic(Player player, Location at) {
        if (at == null || !isCosmeticActive(player, "cos_elytra")) {
            return;
        }
        Location loc = at.clone().add(0.0, 0.7, 0.0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.3, 0.2, 0.3, 0.01);
    }

    private boolean isCosmeticActive(Player player, String rewardId) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        return progress.getActiveToggles().contains(rewardId)
                && progress.getModuleSeconds().getOrDefault(rewardId, 0) > 0;
    }

    private ItemStack shopEntry(PlayerProgress progress, ShopReward reward) {
        boolean unlocked = progress.getUnlockedRewards().contains(reward.id());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + reward.description());
        lore.add(ChatColor.DARK_GRAY + "Stufe: " + reward.requiredLevel());
        lore.add(ChatColor.DARK_GRAY + "Preis: " + reward.cost() + " Token");

        if (reward.usageType() == ShopUsageType.TOGGLE_TIME) {
            lore.add(ChatColor.DARK_GRAY + "Je Kauf: +" + formatSeconds(reward.durationSeconds()));
            lore.add(ChatColor.DARK_GRAY + "Guthaben: " + formatSeconds(progress.getModuleSeconds().getOrDefault(reward.id(), 0)));
        } else {
            lore.add(ChatColor.DARK_GRAY + "Je Kauf: +" + reward.chargesPerPurchase() + " Aufladung(en)");
            lore.add(ChatColor.DARK_GRAY + "Guthaben: " + progress.getModuleCharges().getOrDefault(reward.id(), 0));
        }
        lore.add(unlocked ? ChatColor.GREEN + "Bereits freigeschaltet" : ChatColor.YELLOW + "Klicken zum Kaufen");

        return actionItem(reward.icon(), ChatColor.WHITE + reward.name(), lore, ACTION_BUY, reward.id());
    }

    private ItemStack moduleItem(PlayerProgress progress, ShopReward reward) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + reward.description());

        if (reward.usageType() == ShopUsageType.TOGGLE_TIME) {
            int remaining = progress.getModuleSeconds().getOrDefault(reward.id(), 0);
            boolean active = progress.getActiveToggles().contains(reward.id());
            lore.add(ChatColor.DARK_GRAY + "Restzeit: " + formatSeconds(remaining));
            lore.add(active ? ChatColor.GREEN + "Aktiv" : ChatColor.YELLOW + "Pausiert");
            lore.add(ChatColor.AQUA + "Linksklick: Ein/Aus");
            return actionItem(reward.icon(), ChatColor.WHITE + reward.name(), lore, ACTION_TOGGLE, reward.id());
        }

        int charges = progress.getModuleCharges().getOrDefault(reward.id(), 0);
        lore.add(ChatColor.DARK_GRAY + "Aufladungen: " + charges);
        lore.add(ChatColor.AQUA + "Linksklick: Verwenden");
        return actionItem(reward.icon(), ChatColor.WHITE + reward.name(), lore, ACTION_USE, reward.id());
    }

    private void startModuleTicker() {
        moduleTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
                List<String> active = new ArrayList<>(progress.getActiveToggles());
                for (String rewardId : active) {
                    Optional<ShopReward> rewardOpt = findReward(rewardId);
                    if (rewardOpt.isEmpty()) {
                        progress.getActiveToggles().remove(rewardId);
                        continue;
                    }
                    ShopReward reward = rewardOpt.get();
                    int left = progress.getModuleSeconds().getOrDefault(reward.id(), 0);
                    if (left <= 0) {
                        progress.getActiveToggles().remove(reward.id());
                        clearEffect(player, reward);
                        continue;
                    }

                    progress.getModuleSeconds().put(reward.id(), left - 1);
                    applyTickEffect(player, reward);
                }
            }
        }, 20L, 20L);
    }

    private void applyTickEffect(Player player, ShopReward reward) {
        if (reward.effectType() != null) {
            int amplifier = Math.max(0, reward.effectAmplifier());
            player.addPotionEffect(new PotionEffect(reward.effectType(), 220, amplifier, true, false, true));
        }
        if (reward.particle() != null) {
            Location loc = player.getLocation().add(0.0, 1.0, 0.0);
            player.getWorld().spawnParticle(reward.particle(), loc, 8, 0.35, 0.5, 0.35, 0.01);
        }
    }

    private void clearEffect(Player player, ShopReward reward) {
        if (reward.effectType() != null) {
            player.removePotionEffect(reward.effectType());
        }
    }

    private Optional<ShopReward> findReward(String id) {
        return rewards.stream().filter(reward -> reward.id().equals(id)).findFirst();
    }

    private Comparator<ShopReward> rewardComparator() {
        return Comparator.comparingInt(ShopReward::requiredLevel)
                .thenComparingInt(ShopReward::cost)
                .thenComparing(ShopReward::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ShopReward::id);
    }

    private List<ShopReward> buildRewards() {
        List<ShopReward> list = new ArrayList<>();

        // Positive Effekte
        list.add(toggleEffect("eff_speed_1", Material.SUGAR, "Tempo I", "Schneller laufen", 6, 1, 1800, PotionEffectType.SPEED, 0));
        list.add(toggleEffect("eff_haste_1", Material.GOLDEN_PICKAXE, "Eile I", "Schneller abbauen", 8, 1, 1800, PotionEffectType.FAST_DIGGING, 0));
        list.add(toggleEffect("eff_jump_1", Material.RABBIT_FOOT, "Sprungkraft I", "Höher springen", 8, 1, 1800, PotionEffectType.JUMP, 0));
        list.add(toggleEffect("eff_night", Material.ENDER_EYE, "Nachtsicht", "Klares Sehen im Dunkeln", 9, 1, 2400, PotionEffectType.NIGHT_VISION, 0));
        list.add(toggleEffect("eff_water", Material.TURTLE_HELMET, "Wasseratmung", "Länger unter Wasser", 10, 2, 1800, PotionEffectType.WATER_BREATHING, 0));
        list.add(toggleEffect("eff_fire", Material.MAGMA_CREAM, "Feuerresistenz", "Schutz vor Feuer und Lava", 12, 2, 1500, PotionEffectType.FIRE_RESISTANCE, 0));
        list.add(toggleEffect("eff_luck", Material.RABBIT_FOOT, "Glück I", "Mehr Glück bei Beute", 11, 2, 1500, PotionEffectType.LUCK, 0));
        list.add(toggleEffect("eff_regen", Material.GLISTERING_MELON_SLICE, "Regeneration I", "Lebensregeneration", 14, 3, 900, PotionEffectType.REGENERATION, 0));
        list.add(toggleEffect("eff_strength", Material.BLAZE_POWDER, "Stärke I", "Mehr Nahkampfschaden", 14, 3, 900, PotionEffectType.INCREASE_DAMAGE, 0));
        list.add(toggleEffect("eff_res", Material.SHIELD, "Resistenz I", "Weniger Schaden", 18, 3, 1200, PotionEffectType.DAMAGE_RESISTANCE, 0));
        list.add(toggleEffect("eff_dolphin", Material.HEART_OF_THE_SEA, "Delfins Gnade", "Schneller schwimmen", 18, 4, 1200, PotionEffectType.DOLPHINS_GRACE, 0));
        list.add(toggleEffect("eff_slowfall", Material.PHANTOM_MEMBRANE, "Langsamer Fall", "Sicheres Fallen", 16, 4, 1200, PotionEffectType.SLOW_FALLING, 0));
        list.add(toggleEffect("eff_conduit", Material.CONDUIT, "Meereskraft", "Unterwasser-Boost", 24, 5, 900, PotionEffectType.CONDUIT_POWER, 0));
        list.add(toggleEffect("eff_speed_2", Material.SUGAR, "Tempo II", "Sehr schnelles Laufen", 22, 4, 1200, PotionEffectType.SPEED, 1));
        list.add(toggleEffect("eff_haste_2", Material.NETHERITE_PICKAXE, "Eile II", "Sehr schnelles Abbauen", 28, 5, 900, PotionEffectType.FAST_DIGGING, 1));

        // Kosmetik allgemein + spezielle Trigger
        list.add(toggleParticle("cos_heart", Material.POPPY, "Herz-Aura", "Herzpartikel um dich", 5, 1, 2400, Particle.HEART));
        list.add(toggleParticle("cos_happy", Material.LIME_DYE, "Freude-Aura", "Fröhliche Partikel", 7, 1, 2400, Particle.VILLAGER_HAPPY));
        list.add(toggleParticle("cos_magic", Material.ENCHANTED_BOOK, "Magie-Aura", "Magische Partikel", 12, 2, 1800, Particle.ENCHANTMENT_TABLE));
        list.add(toggleParticle("cos_tp", Material.ENDER_PEARL, "Teleport-Effekt", "Effekt bei Teleport", 16, 2, 1800, Particle.PORTAL));
        list.add(toggleParticle("cos_break", Material.IRON_PICKAXE, "Abbau-Effekt", "Effekt beim Abbauen", 16, 2, 1800, Particle.CRIT));
        list.add(toggleParticle("cos_walk", Material.LEATHER_BOOTS, "Lauf-Effekt", "Effekt beim Laufen", 14, 2, 1800, Particle.CLOUD));
        list.add(toggleParticle("cos_bow", Material.BOW, "Bogen-Effekt", "Effekt beim Schießen", 16, 3, 1800, Particle.ELECTRIC_SPARK));
        list.add(toggleParticle("cos_elytra", Material.ELYTRA, "Elytra-Effekt", "Effekt beim Fliegen", 20, 4, 1500, Particle.END_ROD));

        // Werkbänke + Enderkiste
        list.add(charge("crafting_table_mobile", Material.CRAFTING_TABLE, "Mobile Werkbank", "Öffnet eine Werkbank", 4, 1, 3));
        list.add(charge("stonecutter_mobile", Material.STONECUTTER, "Mobiler Steinmetz", "Öffnet einen Steinmetz", 5, 1, 3));
        list.add(charge("loom_mobile", Material.LOOM, "Mobiler Webstuhl", "Öffnet einen Webstuhl", 5, 1, 3));
        list.add(charge("cartography_mobile", Material.CARTOGRAPHY_TABLE, "Mobiler Kartentisch", "Öffnet einen Kartentisch", 6, 2, 3));
        list.add(charge("smithing_mobile", Material.SMITHING_TABLE, "Mobiler Schmiedetisch", "Öffnet einen Schmiedetisch", 8, 2, 2));
        list.add(charge("grindstone_mobile", Material.GRINDSTONE, "Mobiler Schleifstein", "Öffnet einen Schleifstein", 8, 2, 2));
        list.add(charge("anvil_mobile", Material.ANVIL, "Mobiler Amboss", "Öffnet einen Amboss", 10, 3, 2));
        list.add(charge("enderchest_mobile", Material.ENDER_CHEST, "Mobile Enderkiste", "Öffnet deine Enderkiste", 9, 2, 2));

        for (int shelves = 0; shelves <= 15; shelves++) {
            int levelReq = Math.min(6, 1 + (shelves / 3));
            int price = 8 + shelves;
            list.add(charge(
                    "enchant_mobile_" + shelves,
                    Material.ENCHANTING_TABLE,
                    "Mobiler Zaubertisch (" + shelves + " Regale)",
                    "Verzaubern mit gedacht " + shelves + " Bücherregalen",
                    price,
                    levelReq,
                    shelves >= 12 ? 1 : 2
            ));
        }

        // Friedliche Mobs vollständig
        list.add(chargeEgg("spawn_allay", Material.ALLAY_SPAWN_EGG, "Allay-Spawn-Ei", 26, 5));
        list.add(chargeEgg("spawn_axolotl", Material.AXOLOTL_SPAWN_EGG, "Axolotl-Spawn-Ei", 18, 3));
        list.add(chargeEgg("spawn_bat", Material.BAT_SPAWN_EGG, "Fledermaus-Spawn-Ei", 10, 2));
        list.add(chargeEgg("spawn_bee", Material.BEE_SPAWN_EGG, "Bienen-Spawn-Ei", 12, 2));
        list.add(chargeEgg("spawn_camel", Material.CAMEL_SPAWN_EGG, "Kamel-Spawn-Ei", 18, 3));
        list.add(chargeEgg("spawn_cat", Material.CAT_SPAWN_EGG, "Katzen-Spawn-Ei", 10, 1));
        list.add(chargeEgg("spawn_chicken", Material.CHICKEN_SPAWN_EGG, "Huhn-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_cod", Material.COD_SPAWN_EGG, "Kabeljau-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_cow", Material.COW_SPAWN_EGG, "Kuh-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_dolphin", Material.DOLPHIN_SPAWN_EGG, "Delfin-Spawn-Ei", 16, 3));
        list.add(chargeEgg("spawn_donkey", Material.DONKEY_SPAWN_EGG, "Esel-Spawn-Ei", 12, 2));
        list.add(chargeEgg("spawn_fox", Material.FOX_SPAWN_EGG, "Fuchs-Spawn-Ei", 14, 2));
        list.add(chargeEgg("spawn_frog", Material.FROG_SPAWN_EGG, "Frosch-Spawn-Ei", 14, 2));
        list.add(chargeEgg("spawn_glow_squid", Material.GLOW_SQUID_SPAWN_EGG, "Leuchttintenfisch-Spawn-Ei", 14, 2));
        list.add(chargeEgg("spawn_goat", Material.GOAT_SPAWN_EGG, "Ziegen-Spawn-Ei", 12, 2));
        list.add(chargeEgg("spawn_horse", Material.HORSE_SPAWN_EGG, "Pferde-Spawn-Ei", 14, 2));
        list.add(chargeEgg("spawn_iron_golem", Material.IRON_GOLEM_SPAWN_EGG, "Eisengolem-Spawn-Ei", 28, 5));
        list.add(chargeEgg("spawn_llama", Material.LLAMA_SPAWN_EGG, "Lama-Spawn-Ei", 13, 2));
        list.add(chargeEgg("spawn_mooshroom", Material.MOOSHROOM_SPAWN_EGG, "Mooshroom-Spawn-Ei", 22, 4));
        list.add(chargeEgg("spawn_mule", Material.MULE_SPAWN_EGG, "Maultier-Spawn-Ei", 12, 2));
        list.add(chargeEgg("spawn_ocelot", Material.OCELOT_SPAWN_EGG, "Ozelot-Spawn-Ei", 15, 3));
        list.add(chargeEgg("spawn_panda", Material.PANDA_SPAWN_EGG, "Panda-Spawn-Ei", 24, 4));
        list.add(chargeEgg("spawn_parrot", Material.PARROT_SPAWN_EGG, "Papagei-Spawn-Ei", 15, 3));
        list.add(chargeEgg("spawn_pig", Material.PIG_SPAWN_EGG, "Schwein-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_polar_bear", Material.POLAR_BEAR_SPAWN_EGG, "Eisbär-Spawn-Ei", 20, 4));
        list.add(chargeEgg("spawn_pufferfish", Material.PUFFERFISH_SPAWN_EGG, "Kugelfisch-Spawn-Ei", 12, 2));
        list.add(chargeEgg("spawn_rabbit", Material.RABBIT_SPAWN_EGG, "Hasen-Spawn-Ei", 10, 1));
        list.add(chargeEgg("spawn_salmon", Material.SALMON_SPAWN_EGG, "Lachs-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_sheep", Material.SHEEP_SPAWN_EGG, "Schaf-Spawn-Ei", 8, 1));
        list.add(chargeEgg("spawn_skeleton_horse", Material.SKELETON_HORSE_SPAWN_EGG, "Skelettpferd-Spawn-Ei", 28, 5));
        list.add(chargeEgg("spawn_sniffer", Material.SNIFFER_SPAWN_EGG, "Sniffer-Spawn-Ei", 32, 6));
        list.add(chargeEgg("spawn_snow_golem", Material.SNOW_GOLEM_SPAWN_EGG, "Schneegolem-Spawn-Ei", 18, 3));
        list.add(chargeEgg("spawn_squid", Material.SQUID_SPAWN_EGG, "Tintenfisch-Spawn-Ei", 10, 1));
        list.add(chargeEgg("spawn_strider", Material.STRIDER_SPAWN_EGG, "Schreiter-Spawn-Ei", 18, 3));
        list.add(chargeEgg("spawn_tadpole", Material.TADPOLE_SPAWN_EGG, "Kaulquappen-Spawn-Ei", 10, 1));
        list.add(chargeEgg("spawn_trader_llama", Material.TRADER_LLAMA_SPAWN_EGG, "Händlerlama-Spawn-Ei", 16, 3));
        list.add(chargeEgg("spawn_tropical_fish", Material.TROPICAL_FISH_SPAWN_EGG, "Tropenfisch-Spawn-Ei", 10, 1));
        list.add(chargeEgg("spawn_turtle", Material.TURTLE_SPAWN_EGG, "Schildkröten-Spawn-Ei", 16, 3));
        list.add(chargeEgg("spawn_villager", Material.VILLAGER_SPAWN_EGG, "Dorfbewohner-Spawn-Ei", 25, 5));
        list.add(chargeEgg("spawn_wandering_trader", Material.WANDERING_TRADER_SPAWN_EGG, "Fahrender-Händler-Spawn-Ei", 24, 5));

        return list;
    }

    private ShopReward toggleEffect(String id, Material icon, String name, String description, int cost, int requiredLevel,
                                    int seconds, PotionEffectType type, int amplifier) {
        return new ShopReward(id, ShopCategory.EFFECTS, ShopUsageType.TOGGLE_TIME, icon, name, description, cost,
                requiredLevel, seconds, 0, type, amplifier, null);
    }

    private ShopReward toggleParticle(String id, Material icon, String name, String description,
                                      int cost, int requiredLevel, int seconds, Particle particle) {
        return new ShopReward(id, ShopCategory.COSMETIC, ShopUsageType.TOGGLE_TIME, icon, name, description, cost,
                requiredLevel, seconds, 0, null, 0, particle);
    }

    private ShopReward charge(String id, Material icon, String name, String description,
                              int cost, int requiredLevel, int charges) {
        return new ShopReward(id, ShopCategory.CRAFTING, ShopUsageType.CHARGE_USE, icon, name, description, cost,
                requiredLevel, 0, charges, null, 0, null);
    }

    private ShopReward chargeEgg(String id, Material icon, String name, int cost, int requiredLevel) {
        return new ShopReward(id, ShopCategory.MOBS, ShopUsageType.CHARGE_USE, icon, name,
                "Gibt 1 Spawn-Ei pro Nutzung", cost, requiredLevel, 0, 1, null, 0, null);
    }

    private Map<String, Material> buildSpawnEggMap() {
        Map<String, Material> map = new HashMap<>();
        for (ShopReward reward : rewards == null ? List.<ShopReward>of() : rewards) {
            if (reward.category() == ShopCategory.MOBS) {
                map.put(reward.id(), reward.icon());
            }
        }
        return map;
    }

    private ItemStack actionItem(Material type, String name, List<String> lore, String action, String payload) {
        ItemStack item = plainItem(type, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(payloadKey, PersistentDataType.STRING, payload);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack plainItem(Material type, String name, List<String> lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String readAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private String readPayload(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(payloadKey, PersistentDataType.STRING);
    }

    private String formatSeconds(int seconds) {
        int clamped = Math.max(0, seconds);
        int h = clamped / 3600;
        int m = (clamped % 3600) / 60;
        int s = clamped % 60;
        if (h > 0) {
            return String.format(Locale.ROOT, "%dh %02dm %02ds", h, m, s);
        }
        return String.format(Locale.ROOT, "%dm %02ds", m, s);
    }
}
