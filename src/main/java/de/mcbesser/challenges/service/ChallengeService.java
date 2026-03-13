package de.mcbesser.challenges.service;

import de.mcbesser.challenges.data.PlayerDataStore;
import de.mcbesser.challenges.model.ChallengePeriod;
import de.mcbesser.challenges.model.ChallengeTemplate;
import de.mcbesser.challenges.model.ChallengeType;
import de.mcbesser.challenges.model.PlayerChallenge;
import de.mcbesser.challenges.model.PlayerProgress;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChallengeService {

    private final JavaPlugin plugin;
    private final PlayerDataStore dataStore;
    private final Map<ChallengePeriod, Integer> challengeCountByGroup = new EnumMap<>(ChallengePeriod.class);
    private final Map<ChallengePeriod, Integer> skipByGroup = new EnumMap<>(ChallengePeriod.class);
    private final Map<ChallengePeriod, List<ChallengeTemplate>> templates = new EnumMap<>(ChallengePeriod.class);
    private final Map<ChallengePeriod, Integer> baseTokensByGroup = new EnumMap<>(ChallengePeriod.class);
    private final Map<ChallengePeriod, Integer> quotaDaysByGroup = new EnumMap<>(ChallengePeriod.class);
    private final Map<UUID, Map<String, BossBar>> progressBars = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, BukkitTask>> progressBarHideTasks = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private BukkitTask scheduler;

    public ChallengeService(JavaPlugin plugin, PlayerDataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        loadTemplates();
    }

    public void initializeOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureInitialized(player.getUniqueId());
        }
    }

    public void startSchedulers() {
        scheduler = Bukkit.getScheduler().runTaskTimer(plugin, this::tickState, 20L * 10, 20L * 60);
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.cancel();
        }
        for (Map<String, BossBar> bars : progressBars.values()) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        progressBars.clear();
        for (Map<String, BukkitTask> tasks : progressBarHideTasks.values()) {
            tasks.values().forEach(BukkitTask::cancel);
        }
        progressBarHideTasks.clear();
        dataStore.save();
    }

    private void tickState() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processExpiry(player.getUniqueId(), true);
        }
        dataStore.save();
    }

    private void loadTemplates() {
        challengeCountByGroup.put(ChallengePeriod.GROUP_1, plugin.getConfig().getInt("challengeCounts.group1", 5));
        challengeCountByGroup.put(ChallengePeriod.GROUP_2, plugin.getConfig().getInt("challengeCounts.group2", 7));
        challengeCountByGroup.put(ChallengePeriod.GROUP_3, plugin.getConfig().getInt("challengeCounts.group3", 10));

        skipByGroup.put(ChallengePeriod.GROUP_1, plugin.getConfig().getInt("skipLimits.group1", 1));
        skipByGroup.put(ChallengePeriod.GROUP_2, plugin.getConfig().getInt("skipLimits.group2", 2));
        skipByGroup.put(ChallengePeriod.GROUP_3, plugin.getConfig().getInt("skipLimits.group3", 3));

        baseTokensByGroup.put(ChallengePeriod.GROUP_1, 1);
        baseTokensByGroup.put(ChallengePeriod.GROUP_2, 2);
        baseTokensByGroup.put(ChallengePeriod.GROUP_3, 4);

        quotaDaysByGroup.put(ChallengePeriod.GROUP_1, 1);
        quotaDaysByGroup.put(ChallengePeriod.GROUP_2, 2);
        quotaDaysByGroup.put(ChallengePeriod.GROUP_3, 5);

        for (ChallengePeriod group : ChallengePeriod.values()) {
            String key = switch (group) {
                case GROUP_1 -> "group1";
                case GROUP_2 -> "group2";
                case GROUP_3 -> "group3";
            };
            List<ChallengeTemplate> loaded = new ArrayList<>();
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("templates." + key);
            if (section != null) {
                for (String id : section.getKeys(false)) {
                    ConfigurationSection row = section.getConfigurationSection(id);
                    if (row == null) {
                        continue;
                    }
                    loaded.add(new ChallengeTemplate(
                            id,
                            ChatColor.translateAlternateColorCodes('&', row.getString("title", id)),
                            ChatColor.translateAlternateColorCodes('&', row.getString("description", "")),
                            ChallengeType.valueOf(row.getString("type", "BREAK_BLOCK")),
                            row.getInt("baseTarget", 10),
                            row.getInt("targetPerTier", 5),
                            0,
                            0,
                            true
                    ));
                }
            }
            templates.put(group, loaded);
        }
    }

    public void ensureInitialized(UUID playerId) {
        PlayerProgress progress = dataStore.getOrCreate(playerId);
        if (progress.getExpiresAt() == null) {
            progress.setExpiresAt(LocalDateTime.now().plusDays(1));
        }
        processExpiry(playerId, false);
        for (ChallengePeriod group : ChallengePeriod.values()) {
            if (progress.getActiveChallenges().get(group).isEmpty()) {
                regenerateChallenges(playerId, group);
            }
            if (progress.getRemainingSkips(group) <= 0) {
                progress.setRemainingSkips(group, skipByGroup.getOrDefault(group, 1));
            }
        }
    }

    private void processExpiry(UUID playerId, boolean notify) {
        PlayerProgress progress = dataStore.getOrCreate(playerId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = progress.getExpiresAt();
        if (expiry == null || !now.isAfter(expiry)) {
            return;
        }

        long overdueDays = ChronoUnit.DAYS.between(expiry, now);
        overdueDays = Math.max(1, overdueDays + 1);
        int loss = (int) Math.min(overdueDays, Math.max(0, progress.getGroupTier() - 1));
        if (loss > 0) {
            progress.setGroupTier(progress.getGroupTier() - loss);
        }
        resetGroups(playerId, progress);

        progress.setExpiresAt(expiry.plusDays(overdueDays));
        if (progress.getExpiresAt().isBefore(now)) {
            progress.setExpiresAt(now.plusDays(1));
        }

        Player player = Bukkit.getPlayer(playerId);
        if (notify && player != null && player.isOnline()) {
            if (loss > 0) {
                player.sendMessage(ChatColor.RED + "Kontingent abgelaufen. Stufe -" + loss
                        + ", neue Stufe: " + progress.getGroupTier() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Kontingent abgelaufen. Gruppenfortschritt zurückgesetzt.");
            }
        }
    }

    private void regenerateChallenges(UUID playerId, ChallengePeriod group) {
        PlayerProgress progress = dataStore.getOrCreate(playerId);
        int level = progress.getGroupTier();

        List<ChallengeTemplate> pool = new ArrayList<>(templates.getOrDefault(group, List.of()));
        pool.sort(Comparator.comparing(ChallengeTemplate::id));
        List<ChallengeTemplate> selected = new ArrayList<>();
        int count = Math.min(challengeCountByGroup.getOrDefault(group, 5), pool.size());
        while (!pool.isEmpty() && selected.size() < count) {
            selected.add(pool.remove(random.nextInt(pool.size())));
        }

        double levelFactor = 1.0 + ((Math.max(1, level) - 1) * 0.25);
        List<PlayerChallenge> generated = selected.stream().map(template -> {
            int spread = Math.max(2, template.targetPerTier());
            int baseTarget = template.baseTarget() + random.nextInt(spread);
            int target = Math.max(1, (int) Math.ceil(baseTarget * levelFactor));
            int tokens = baseTokensByGroup.getOrDefault(group, 1) * Math.max(1, level);
            return new PlayerChallenge(
                    template.id(),
                    template.title(),
                    template.description(),
                    template.type(),
                    group,
                    target,
                    tokens,
                    true
            );
        }).collect(Collectors.toCollection(ArrayList::new));
        progress.getActiveChallenges().put(group, generated);
    }

    public void addProgress(Player player, ChallengeType type, int amount) {
        UUID playerId = player.getUniqueId();
        ensureInitialized(playerId);
        processExpiry(playerId, false);

        PlayerProgress progress = dataStore.getOrCreate(playerId);
        ChallengePeriod current = currentGroup(progress);
        boolean anyCompleted = false;

        for (PlayerChallenge challenge : progress.getActiveChallenges().getOrDefault(current, List.of())) {
            if (challenge.getType() != type || challenge.isCompleted() || challenge.isSkipped()) {
                continue;
            }
            if (challenge.addProgress(amount)) {
                progress.addTokens(challenge.getTokenReward());
                player.sendMessage(ChatColor.GREEN + "Challenge geschafft: " + challenge.getTitle()
                        + ChatColor.DARK_GRAY + " (+" + challenge.getTokenReward() + " Token)");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.1f);
                anyCompleted = true;
            }
            showProgressBossBar(player, challenge, current);
        }

        if (anyCompleted && isGroupDone(progress, current)) {
            completeGroup(player, progress, current);
        }
        dataStore.save();
    }

    private void completeGroup(Player player, PlayerProgress progress, ChallengePeriod group) {
        int quotaDays = quotaDaysByGroup.getOrDefault(group, 1);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = progress.getExpiresAt() == null ? now : progress.getExpiresAt();
        if (base.isBefore(now)) {
            base = now;
        }
        progress.setExpiresAt(base.plusDays(quotaDays));

        if (group == ChallengePeriod.GROUP_3) {
            progress.setGroupTier(progress.getGroupTier() + 1);
            resetGroups(player.getUniqueId(), progress);
            player.sendMessage(ChatColor.AQUA + "Alle Gruppen abgeschlossen. Neue Stufe: " + progress.getGroupTier());
        } else {
            progress.setCurrentGroup(progress.getCurrentGroup() + 1);
            player.sendMessage(ChatColor.GOLD + "Gruppe abgeschlossen: " + periodName(group)
                    + ChatColor.GRAY + " | Nächste Gruppe freigeschaltet.");
        }
    }

    private void resetGroups(UUID playerId, PlayerProgress progress) {
        progress.setCurrentGroup(1);
        for (ChallengePeriod group : ChallengePeriod.values()) {
            regenerateChallenges(playerId, group);
            progress.setRemainingSkips(group, skipByGroup.getOrDefault(group, 1));
        }
    }

    private boolean isGroupDone(PlayerProgress progress, ChallengePeriod group) {
        for (PlayerChallenge challenge : progress.getActiveChallenges().getOrDefault(group, List.of())) {
            if (!challenge.isCompleted() && !challenge.isSkipped()) {
                return false;
            }
        }
        return true;
    }

    private void showProgressBossBar(Player player, PlayerChallenge challenge, ChallengePeriod group) {
        UUID uuid = player.getUniqueId();
        String barId = group.name() + ":" + challenge.getTemplateId();
        Map<String, BossBar> playerBars = progressBars.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>());
        Map<String, BukkitTask> playerTasks = progressBarHideTasks.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>());
        BossBar bar = playerBars.computeIfAbsent(barId, ignored -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));

        int remaining = Math.max(0, challenge.getTarget() - challenge.getProgress());
        double value = challenge.getTarget() <= 0 ? 0.0 : (double) challenge.getProgress() / (double) challenge.getTarget();
        bar.setProgress(Math.max(0.0, Math.min(1.0, value)));
        bar.setColor(challenge.isCompleted() ? BarColor.GREEN : BarColor.BLUE);
        bar.setTitle(ChatColor.AQUA + periodName(group) + ": " + ChatColor.WHITE + challenge.getTitle()
                + ChatColor.GRAY + " | " + challenge.getProgress() + "/" + challenge.getTarget()
                + ChatColor.DARK_GRAY + " (offen: " + remaining + ")");
        bar.addPlayer(player);

        BukkitTask oldTask = playerTasks.remove(barId);
        if (oldTask != null) {
            oldTask.cancel();
        }
        BukkitTask hideTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<String, BossBar> activeBars = progressBars.get(uuid);
            BossBar activeBar = activeBars == null ? null : activeBars.remove(barId);
            if (activeBar != null) {
                activeBar.removePlayer(player);
            }
            Map<String, BukkitTask> tasks = progressBarHideTasks.get(uuid);
            if (tasks != null) {
                tasks.remove(barId);
                if (tasks.isEmpty()) {
                    progressBarHideTasks.remove(uuid);
                }
            }
            if (activeBars != null && activeBars.isEmpty()) {
                progressBars.remove(uuid);
            }
        }, 20L * 60L);
        playerTasks.put(barId, hideTask);
    }

    public PlayerProgress getProgress(UUID playerId) {
        ensureInitialized(playerId);
        processExpiry(playerId, false);
        return dataStore.getOrCreate(playerId);
    }

    public boolean skipChallenge(Player player, ChallengePeriod group, int index) {
        PlayerProgress progress = getProgress(player.getUniqueId());
        if (group != currentGroup(progress)) {
            return false;
        }
        List<PlayerChallenge> list = progress.getActiveChallenges().getOrDefault(group, List.of());
        if (index < 1 || index > list.size()) {
            return false;
        }
        PlayerChallenge challenge = list.get(index - 1);
        if (challenge.isCompleted() || challenge.isSkipped()) {
            return false;
        }
        if (progress.getRemainingSkips(group) <= 0) {
            return false;
        }
        progress.setRemainingSkips(group, progress.getRemainingSkips(group) - 1);
        challenge.setSkipped(true);
        if (isGroupDone(progress, group)) {
            completeGroup(player, progress, group);
        }
        dataStore.save();
        return true;
    }

    private ChallengePeriod currentGroup(PlayerProgress progress) {
        return switch (progress.getCurrentGroup()) {
            case 2 -> ChallengePeriod.GROUP_2;
            case 3 -> ChallengePeriod.GROUP_3;
            default -> ChallengePeriod.GROUP_1;
        };
    }

    public ChallengePeriod visibleGroup(UUID playerId) {
        return currentGroup(getProgress(playerId));
    }

    public String periodName(ChallengePeriod group) {
        return switch (group) {
            case GROUP_1 -> "Gruppe 1 von 3";
            case GROUP_2 -> "Gruppe 2 von 3";
            case GROUP_3 -> "Gruppe 3 von 3";
        };
    }

    public LocalDateTime getEndTime(UUID playerId, ChallengePeriod group) {
        return getProgress(playerId).getExpiresAt();
    }

    public String challengeHowTo(PlayerChallenge challenge) {
        return switch (challenge.getType()) {
            case BREAK_BLOCK -> "Baue beliebige Blöcke in Survival ab.";
            case MINE_ORE -> "Baue echte Erzblöcke wie Kohle, Eisen, Diamant.";
            case KILL_MOB -> "Besiege feindliche Mobs mit dem finalen Treffer.";
            case FISH -> "Fange mit der Angel (nur gefangene Fische zählen).";
            case CRAFT -> "Stelle echte Items an Werkbank oder Inventar her.";
            case BREED -> "Züchte Tiere mit passendem Futter.";
            case SMELT -> "Entnimm geschmolzene Items aus dem Ofen.";
            case ENCHANT -> "Verzaubere Gegenstände am Zaubertisch.";
            case WALK_DISTANCE -> "Bewege dich zu Fuß, schwimmend oder fliegend durch die Welt.";
            case TRADE_VILLAGER -> "Schließe echte Villager-Handel ab.";
        };
    }
}
