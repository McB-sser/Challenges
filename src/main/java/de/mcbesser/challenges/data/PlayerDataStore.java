package de.mcbesser.challenges.data;

import de.mcbesser.challenges.model.ChallengePeriod;
import de.mcbesser.challenges.model.ChallengeType;
import de.mcbesser.challenges.model.PlayerChallenge;
import de.mcbesser.challenges.model.PlayerProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataStore {

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerProgress> progressMap = new HashMap<>();
    private File file;

    public PlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            PlayerProgress progress = new PlayerProgress();
            progress.setTokens(section.getInt("tokens", 0));
            progress.setGroupTier(section.getInt("groupTier", 1));
            progress.setCurrentGroup(section.getInt("currentGroup", 1));

            String expiresAt = section.getString("expiresAt");
            if (expiresAt != null) {
                try {
                    progress.setExpiresAt(LocalDateTime.parse(expiresAt));
                } catch (Exception ignored) {
                }
            }

            progress.getUnlockedRewards().addAll(section.getStringList("shop.unlocked"));
            progress.getActiveToggles().addAll(section.getStringList("shop.activeToggles"));
            ConfigurationSection moduleSeconds = section.getConfigurationSection("shop.moduleSeconds");
            if (moduleSeconds != null) {
                for (String idKey : moduleSeconds.getKeys(false)) {
                    progress.getModuleSeconds().put(idKey, Math.max(0, moduleSeconds.getInt(idKey, 0)));
                }
            }
            ConfigurationSection moduleCharges = section.getConfigurationSection("shop.moduleCharges");
            if (moduleCharges != null) {
                for (String idKey : moduleCharges.getKeys(false)) {
                    progress.getModuleCharges().put(idKey, Math.max(0, moduleCharges.getInt(idKey, 0)));
                }
            }

            for (ChallengePeriod period : ChallengePeriod.values()) {
                progress.setRemainingSkips(period, section.getInt("remainingSkips." + period.name(), 0));
                List<Map<?, ?>> rows = section.getMapList("challenges." + period.name());
                List<PlayerChallenge> loaded = new ArrayList<>();
                for (Map<?, ?> row : rows) {
                    try {
                        PlayerChallenge challenge = new PlayerChallenge(
                                asString(row.get("templateId")),
                                asString(row.get("title")),
                                asString(row.get("description")),
                                ChallengeType.valueOf(asString(row.get("type"))),
                                period,
                                asInt(row.get("target"), 1),
                                asInt(row.get("tokenReward"), 1),
                                asBool(row.get("skippable"), true)
                        );
                        challenge.setProgress(asInt(row.get("progress"), 0));
                        challenge.setCompleted(asBool(row.get("completed"), false));
                        challenge.setSkipped(asBool(row.get("skipped"), false));
                        loaded.add(challenge);
                    } catch (Exception ignored) {
                    }
                }
                progress.getActiveChallenges().put(period, loaded);
            }
            progressMap.put(id, progress);
        }
    }

    public void save() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "players.yml");
        }

        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerProgress> entry : progressMap.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerProgress progress = entry.getValue();

            cfg.set(base + ".tokens", progress.getTokens());
            cfg.set(base + ".groupTier", progress.getGroupTier());
            cfg.set(base + ".currentGroup", progress.getCurrentGroup());
            cfg.set(base + ".expiresAt", progress.getExpiresAt().toString());
            cfg.set(base + ".shop.unlocked", new ArrayList<>(progress.getUnlockedRewards()));
            cfg.set(base + ".shop.activeToggles", new ArrayList<>(progress.getActiveToggles()));
            cfg.set(base + ".shop.moduleSeconds", progress.getModuleSeconds());
            cfg.set(base + ".shop.moduleCharges", progress.getModuleCharges());

            for (ChallengePeriod period : ChallengePeriod.values()) {
                cfg.set(base + ".remainingSkips." + period.name(), progress.getRemainingSkips(period));

                List<Map<String, Object>> serialized = new ArrayList<>();
                for (PlayerChallenge challenge : progress.getActiveChallenges().getOrDefault(period, List.of())) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("templateId", challenge.getTemplateId());
                    row.put("title", challenge.getTitle());
                    row.put("description", challenge.getDescription());
                    row.put("type", challenge.getType().name());
                    row.put("target", challenge.getTarget());
                    row.put("tokenReward", challenge.getTokenReward());
                    row.put("skippable", challenge.isSkippable());
                    row.put("progress", challenge.getProgress());
                    row.put("completed", challenge.isCompleted());
                    row.put("skipped", challenge.isSkipped());
                    serialized.add(row);
                }
                cfg.set(base + ".challenges." + period.name(), serialized);
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }

    public PlayerProgress getOrCreate(UUID playerId) {
        return progressMap.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }

    private boolean asBool(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return fallback;
    }
}
