package de.mcbesser.challenges.model;

public class PlayerChallenge {

    private final String templateId;
    private final String title;
    private final String description;
    private final ChallengeType type;
    private final ChallengePeriod period;
    private final int target;
    private final int tokenReward;
    private final boolean skippable;
    private int progress;
    private boolean completed;
    private boolean skipped;

    public PlayerChallenge(String templateId, String title, String description, ChallengeType type,
                           ChallengePeriod period, int target, int tokenReward, boolean skippable) {
        this.templateId = templateId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.period = period;
        this.target = target;
        this.tokenReward = tokenReward;
        this.skippable = skippable;
    }

    public boolean addProgress(int amount) {
        if (completed || skipped) {
            return false;
        }
        progress = Math.min(target, progress + Math.max(0, amount));
        if (progress >= target) {
            completed = true;
            return true;
        }
        return false;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ChallengeType getType() {
        return type;
    }

    public ChallengePeriod getPeriod() {
        return period;
    }

    public int getTarget() {
        return target;
    }

    public int getTokenReward() {
        return tokenReward;
    }

    public boolean isSkippable() {
        return skippable;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}
