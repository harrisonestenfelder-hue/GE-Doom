
package com.doom.godemperor;

import java.util.HashSet;
import java.util.Set;

public final class DoomProgression {

    private DoomProgression() {}

    public static final int MAX_LEVEL = 100;

    public record Progress(
            int level,
            int experience,
            int skillPoints,
            Set<String> unlockedSkills,
            boolean godEmperorUnlocked
    ) {
        public Progress {
            unlockedSkills = Set.copyOf(unlockedSkills);
        }

        public static Progress starting() {
            return new Progress(
                    1, 0, 0,
                    Set.of("repulsor_blast", "arcane_bolt", "doom_dash"),
                    false
            );
        }
    }

    public static int experienceRequired(int level) {
        return 100 + (level - 1) * 50;
    }

    public static Progress awardExperience(
            Progress progress,
            int amount
    ) {
        if (amount <= 0 || progress.level() >= MAX_LEVEL) {
            return progress;
        }

        int level = progress.level();
        int xp = progress.experience() + amount;
        int points = progress.skillPoints();

        while (level < MAX_LEVEL &&
                xp >= experienceRequired(level)) {

            xp -= experienceRequired(level);
            level++;
            points++;
        }

        return new Progress(
                level,
                xp,
                points,
                progress.unlockedSkills(),
                progress.godEmperorUnlocked()
        );
    }

    public static Progress unlockSkill(
            Progress progress,
            String skillId
    ) {
        DoomSkillTree.Skill skill =
                DoomSkillTree.getSkill(skillId).orElse(null);

        if (skill == null ||
                progress.unlockedSkills().contains(skillId)) {
            return progress;
        }

        if (skill.stage() ==
                DoomSkillTree.Stage.GOD_EMPEROR &&
                !progress.godEmperorUnlocked()) {
            return progress;
        }

        if (progress.skillPoints() < skill.pointCost()) {
            return progress;
        }

        boolean prerequisitesMet =
                skill.prerequisites().stream()
                        .allMatch(progress.unlockedSkills()::contains);

        if (!prerequisitesMet) {
            return progress;
        }

        Set<String> unlocked =
                new HashSet<>(progress.unlockedSkills());

        unlocked.add(skillId);

        return new Progress(
                progress.level(),
                progress.experience(),
                progress.skillPoints() - skill.pointCost(),
                unlocked,
                progress.godEmperorUnlocked()
        );
    }

    public static Progress unlockGodEmperor(
            Progress progress
    ) {
        return new Progress(
                progress.level(),
                progress.experience(),
                progress.skillPoints(),
                progress.unlockedSkills(),
                true
        );
    }
}
