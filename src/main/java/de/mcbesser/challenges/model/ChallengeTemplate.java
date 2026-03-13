package de.mcbesser.challenges.model;

public record ChallengeTemplate(
        String id,
        String title,
        String description,
        ChallengeType type,
        int baseTarget,
        int targetPerTier,
        int baseTokens,
        int tokenPerTier,
        boolean skippable
) {
}
