package de.mcbesser.challenges.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffectType;

public record ShopReward(
        String id,
        ShopCategory category,
        ShopUsageType usageType,
        Material icon,
        String name,
        String description,
        int cost,
        int requiredLevel,
        int durationSeconds,
        int chargesPerPurchase,
        PotionEffectType effectType,
        int effectAmplifier,
        Particle particle
) {
}
