package de.mcbesser.challenges.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class PlayerProgress {

    private int tokens;
    private int groupTier = 1;
    private int currentGroup = 1;
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
    private final Map<ChallengePeriod, Integer> remainingSkips = new EnumMap<>(ChallengePeriod.class);
    private final Map<ChallengePeriod, List<PlayerChallenge>> activeChallenges = new EnumMap<>(ChallengePeriod.class);
    private final Set<String> unlockedRewards = new HashSet<>();
    private final Set<String> activeToggles = new HashSet<>();
    private final Map<String, Integer> moduleSeconds = new HashMap<>();
    private final Map<String, Integer> moduleCharges = new HashMap<>();

    public PlayerProgress() {
        for (ChallengePeriod period : ChallengePeriod.values()) {
            remainingSkips.put(period, 0);
            activeChallenges.put(period, new ArrayList<>());
        }
    }

    public int getTokens() {
        return tokens;
    }

    public void addTokens(int amount) {
        tokens += Math.max(0, amount);
    }

    public boolean removeTokens(int amount) {
        if (tokens < amount) {
            return false;
        }
        tokens -= amount;
        return true;
    }

    public void setTokens(int tokens) {
        this.tokens = Math.max(0, tokens);
    }

    public int getGroupTier() {
        return groupTier;
    }

    public void setGroupTier(int groupTier) {
        this.groupTier = Math.max(1, groupTier);
    }

    public int getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(int currentGroup) {
        this.currentGroup = Math.max(1, Math.min(3, currentGroup));
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getRemainingSkips(ChallengePeriod period) {
        return remainingSkips.getOrDefault(period, 0);
    }

    public void setRemainingSkips(ChallengePeriod period, int value) {
        remainingSkips.put(period, Math.max(0, value));
    }

    public Map<ChallengePeriod, List<PlayerChallenge>> getActiveChallenges() {
        return activeChallenges;
    }

    public Set<String> getUnlockedRewards() {
        return unlockedRewards;
    }

    public Set<String> getActiveToggles() {
        return activeToggles;
    }

    public Map<String, Integer> getModuleSeconds() {
        return moduleSeconds;
    }

    public Map<String, Integer> getModuleCharges() {
        return moduleCharges;
    }
}
