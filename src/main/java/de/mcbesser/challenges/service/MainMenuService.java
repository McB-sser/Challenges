package de.mcbesser.challenges.service;

import de.mcbesser.challenges.model.ChallengePeriod;
import de.mcbesser.challenges.model.PlayerChallenge;
import de.mcbesser.challenges.model.PlayerProgress;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainMenuService {

    public static final String MAIN_MENU_TITLE = ChatColor.DARK_AQUA + "Herausforderungs-Hub";
    public static final String CHALLENGE_MENU_TITLE = ChatColor.DARK_BLUE + "Aufgaben";
    public static final String ACTION_OPEN_SHOP = "open_shop";
    public static final String ACTION_OPEN_CHALLENGES = "open_challenges";
    public static final String ACTION_OPEN_MODULES = "open_modules";
    public static final String ACTION_BACK = "back";
    public static final String ACTION_CHALLENGE = "challenge";

    private static final DateTimeFormatter ENDS_AT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final ChallengeService challengeService;
    private final ShopService shopService;
    private final NamespacedKey actionKey;
    private final NamespacedKey periodKey;
    private final NamespacedKey indexKey;

    public MainMenuService(JavaPlugin plugin, ChallengeService challengeService, ShopService shopService) {
        this.challengeService = challengeService;
        this.shopService = shopService;
        this.actionKey = new NamespacedKey(plugin, "main_menu_action");
        this.periodKey = new NamespacedKey(plugin, "challenge_period");
        this.indexKey = new NamespacedKey(plugin, "challenge_index");
    }

    public void openMainMenu(Player player) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE);
        String endLabel = progress.getExpiresAt() == null
                ? "-"
                : progress.getExpiresAt().minusSeconds(1).format(ENDS_AT_FORMAT);

        inv.setItem(10, actionItem(Material.BOOK, ChatColor.AQUA + "Aufgaben", List.of(
                ChatColor.GRAY + "Sichtbar ist nur die aktuelle Gruppe",
                ChatColor.YELLOW + "Aktuelle Stufe: " + progress.getGroupTier(),
                ChatColor.DARK_GRAY + "Gruppen-Reset endet: " + endLabel,
                ChatColor.DARK_GRAY + "Kontingent endet: " + endLabel,
                ChatColor.DARK_GRAY + "Basiszeit pro Durchlauf: 2 Tage für Gruppe 1-3",
                ChatColor.DARK_GRAY + "Aufbau: G1 +1 Tag, G2 +2 Tage, G3 +5 Tage",
                ChatColor.DARK_GRAY + "Ohne Kontingent sinkt täglich 1 Stufe"
        ), ACTION_OPEN_CHALLENGES));

        inv.setItem(13, actionItem(Material.SUNFLOWER, ChatColor.GOLD + "Token-Shop", List.of(
                ChatColor.GRAY + "Module mit Token- und Stufenvorgaben",
                ChatColor.YELLOW + "Tokens: " + progress.getTokens() + " Token"
        ), ACTION_OPEN_SHOP));

        inv.setItem(16, actionItem(Material.ENDER_CHEST, ChatColor.LIGHT_PURPLE + "Meine Module", List.of(
                ChatColor.GRAY + "Erworbene Module ein-/ausschalten",
                ChatColor.GRAY + "Pausieren spart Zeitguthaben"
        ), ACTION_OPEN_MODULES));

        player.openInventory(inv);
    }

    public void openChallengesMenu(Player player) {
        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        ChallengePeriod visible = challengeService.visibleGroup(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, CHALLENGE_MENU_TITLE);

        fillGroupSection(inv, player, progress, visible, 0);
        inv.setItem(53, actionItem(Material.ARROW, ChatColor.GRAY + "Zurück", List.of(
                ChatColor.DARK_GRAY + "Zum Hauptmenü"
        ), ACTION_BACK));
        player.openInventory(inv);
    }

    private void fillGroupSection(Inventory inv, Player player, PlayerProgress progress, ChallengePeriod period, int startSlot) {
        LocalDateTime endTime = challengeService.getEndTime(player.getUniqueId(), period);
        String endLabel = endTime == null ? "-" : endTime.minusSeconds(1).format(ENDS_AT_FORMAT);

        inv.setItem(startSlot, plainItem(Material.OAK_SIGN,
                ChatColor.AQUA + challengeService.periodName(period) + ChatColor.GRAY + " (Skips: "
                        + progress.getRemainingSkips(period) + ")",
                List.of(
                        ChatColor.DARK_GRAY + "Gruppen-Reset endet: " + endLabel,
                        ChatColor.DARK_GRAY + "Kontingent endet: " + endLabel,
                        ChatColor.DARK_GRAY + "Basiszeit pro Durchlauf: 2 Tage für Gruppe 1-3",
                        ChatColor.DARK_GRAY + "Aufbau: G1 +1 Tag, G2 +2 Tage, G3 +5 Tage",
                        ChatColor.DARK_GRAY + "Ohne Kontingent sinkt täglich 1 Stufe",
                        ChatColor.DARK_GRAY + "Alle Aufgaben sind überspringbar (solange Skips übrig sind)",
                        ChatColor.DARK_GRAY + "Rechtsklick auf Aufgabe zum Überspringen"
                )
        ));

        List<PlayerChallenge> challenges = progress.getActiveChallenges().getOrDefault(period, List.of());
        for (int i = 0; i < challenges.size() && i < 44; i++) {
            int slot = startSlot + 1 + i;
            PlayerChallenge challenge = challenges.get(i);
            Material icon = challenge.isCompleted() ? Material.LIME_DYE
                    : challenge.isSkipped() ? Material.GRAY_DYE : Material.PAPER;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + challenge.getDescription());
            lore.add(ChatColor.DARK_GRAY + "So geht's: " + challengeService.challengeHowTo(challenge));
            lore.add(ChatColor.DARK_GRAY + "Fortschritt: " + challenge.getProgress() + "/" + challenge.getTarget());
            lore.add(ChatColor.DARK_GRAY + "Gruppen-Reset endet: " + endLabel);
            lore.add(ChatColor.DARK_GRAY + "Kontingent endet: " + endLabel);
            lore.add(ChatColor.GOLD + "Belohnung: " + challenge.getTokenReward() + " Token");
            if (challenge.isCompleted()) {
                lore.add(ChatColor.GREEN + "Erledigt");
            } else if (challenge.isSkipped()) {
                lore.add(ChatColor.GRAY + "Übersprungen");
            } else {
                lore.add(ChatColor.YELLOW + "Rechtsklick: überspringen (wenn Skips übrig)");
            }

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + "" + (i + 1) + ". " + challenge.getTitle());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_CHALLENGE);
            meta.getPersistentDataContainer().set(periodKey, PersistentDataType.STRING, period.name());
            meta.getPersistentDataContainer().set(indexKey, PersistentDataType.INTEGER, i + 1);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }
    }

    private ItemStack actionItem(Material material, String title, List<String> lore, String action) {
        ItemStack item = plainItem(material, title, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack plainItem(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String readAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public ChallengePeriod readPeriod(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String period = item.getItemMeta().getPersistentDataContainer().get(periodKey, PersistentDataType.STRING);
        if (period == null) {
            return null;
        }
        try {
            return ChallengePeriod.valueOf(period);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Integer readIndex(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
    }

    public ShopService getShopService() {
        return shopService;
    }

    public ChallengeService getChallengeService() {
        return challengeService;
    }
}
