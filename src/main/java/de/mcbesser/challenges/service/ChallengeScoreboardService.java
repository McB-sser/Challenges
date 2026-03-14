package de.mcbesser.challenges.service;

import de.mcbesser.challenges.model.ChallengePeriod;
import de.mcbesser.challenges.model.PlayerChallenge;
import de.mcbesser.challenges.model.PlayerProgress;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChallengeScoreboardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final String OBJECTIVE_NAME = "challenges";

    private final JavaPlugin plugin;
    private final ChallengeService challengeService;
    private final MenuItemService menuItemService;
    private BukkitTask refreshTask;

    public ChallengeScoreboardService(JavaPlugin plugin, ChallengeService challengeService, MenuItemService menuItemService) {
        this.plugin = plugin;
        this.challengeService = challengeService;
        this.menuItemService = menuItemService;
    }

    public void start() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refresh(player);
            }
        }, 1L, 20L);
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            hide(player);
        }
    }

    public void refresh(Player player) {
        if (shouldShow(player)) {
            show(player);
            return;
        }
        hide(player);
    }

    public void hide(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && isShowingOwnBoard(player)) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    private boolean shouldShow(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return menuItemService.isMenuItem(mainHand);
    }

    private void show(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        PlayerProgress progress = challengeService.getProgress(player.getUniqueId());
        ChallengePeriod period = challengeService.visibleGroup(player.getUniqueId());
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Challenges");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = ensureUnique(buildLines(progress, period));
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }

        player.setScoreboard(scoreboard);
    }

    private boolean isShowingOwnBoard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        return scoreboard != null && scoreboard.getObjective(OBJECTIVE_NAME) != null;
    }

    private List<String> buildLines(PlayerProgress progress, ChallengePeriod period) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + challengeService.periodName(period));
        lines.add(ChatColor.GRAY + "Stufe: " + ChatColor.WHITE + progress.getGroupTier());
        lines.add(ChatColor.GRAY + "Token: " + ChatColor.WHITE + progress.getTokens());
        lines.add(ChatColor.GRAY + "Skips: " + ChatColor.WHITE + progress.getRemainingSkips(period));
        lines.add(ChatColor.DARK_GRAY + " ");
        lines.add(ChatColor.AQUA + "Ablauf");

        LocalDateTime expiresAt = progress.getExpiresAt();
        lines.add(ChatColor.GRAY + (expiresAt == null ? "-" : expiresAt.minusSeconds(1).format(TIME_FORMAT)));
        lines.add(ChatColor.DARK_GRAY + "  ");
        lines.add(ChatColor.AQUA + "Aufgaben");

        List<PlayerChallenge> challenges = new ArrayList<>(progress.getActiveChallenges().getOrDefault(period, List.of()));
        challenges.sort(Comparator
                .comparing(PlayerChallenge::isCompleted)
                .thenComparing(PlayerChallenge::isSkipped)
                .thenComparing(PlayerChallenge::getTitle));
        int shown = Math.min(5, challenges.size());
        for (int i = 0; i < shown; i++) {
            lines.add(formatChallengeLine(i + 1, challenges.get(i)));
        }

        if (challenges.size() > shown) {
            lines.add(ChatColor.DARK_GRAY + "+" + (challenges.size() - shown) + " weitere");
        }

        return lines;
    }

    private String formatChallengeLine(int index, PlayerChallenge challenge) {
        String prefix = challenge.isCompleted() ? ChatColor.GREEN + "[OK] "
                : challenge.isSkipped() ? ChatColor.GRAY + "[X] "
                : ChatColor.YELLOW + "" + index + ". ";
        ChatColor color = challenge.isCompleted() ? ChatColor.GREEN
                : challenge.isSkipped() ? ChatColor.GRAY
                : ChatColor.WHITE;
        return trim(prefix + color + challenge.getProgress() + "/" + challenge.getTarget()
                + " " + challenge.getTitle(), 40);
    }

    private List<String> ensureUnique(List<String> lines) {
        List<String> unique = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = trim(lines.get(i), 40);
            if (unique.contains(line)) {
                line = trim(line + ChatColor.COLOR_CHAR + Integer.toHexString(i % 10), 40);
            }
            unique.add(line);
        }
        return unique;
    }

    private String trim(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }
}
