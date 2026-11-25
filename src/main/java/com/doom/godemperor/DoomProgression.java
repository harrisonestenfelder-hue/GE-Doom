
package com.doom.godemperor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class DoomProgression {

    private DoomProgression() {}

    public static final int MAX_LEVEL = 100;

    private static final Codec<Set<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(
                    Set::copyOf,
                    List::copyOf
            );

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

        public static final MapCodec<Progress> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                Codec.INT.fieldOf("level")
                                        .forGetter(Progress::level),
                                Codec.INT.fieldOf("experience")
                                        .forGetter(Progress::experience),
                                Codec.INT.fieldOf("skill_points")
                                        .forGetter(Progress::skillPoints),
                                STRING_SET_CODEC.fieldOf("unlocked_skills")
                                        .forGetter(Progress::unlockedSkills),
                                Codec.BOOL.fieldOf("god_emperor_unlocked")
                                        .forGetter(Progress::godEmperorUnlocked)
                        ).apply(instance, Progress::new)
                );

        public static Progress starting() {
            return new Progress(
                    1,
                    0,
                    0,
                    Set.of(
                            "repulsor_blast",
                            "arcane_bolt",
                            "doom_dash"
                    ),
                    false
            );
        }
    }

    public static final Supplier<AttachmentType<Progress>> PLAYER_PROGRESS =
            GodEmperorDoom.ATTACHMENTS.register(
                    "progression",
                    () -> AttachmentType.builder(Progress::starting)
                            .serialize(Progress.CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static void register() {
        // Calling this initializes the progression attachment.
    }

    public static int experienceRequired(int level) {
        return 100 + (level - 1) * 50;
    }

    public static Progress get(ServerPlayer player) {
        return player.getData(PLAYER_PROGRESS);
    }

    public static void awardExperience(
            ServerPlayer player,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }

        Progress progress = get(player);

        if (progress.level() >= MAX_LEVEL) {
            return;
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

        player.setData(
                PLAYER_PROGRESS,
                new Progress(
                        level,
                        xp,
                        points,
                        progress.unlockedSkills(),
                        progress.godEmperorUnlocked()
                )
        );
    }

    public static boolean unlockSkill(
            ServerPlayer player,
            String skillId
    ) {
        Progress progress = get(player);

        DoomSkillTree.Skill skill =
                DoomSkillTree.getSkill(skillId).orElse(null);

        if (skill == null ||
                progress.unlockedSkills().contains(skillId)) {
            return false;
        }

        if (skill.stage() ==
                DoomSkillTree.Stage.GOD_EMPEROR &&
                !progress.godEmperorUnlocked()) {
            return false;
        }

        if (progress.skillPoints() < skill.pointCost()) {
            return false;
        }

        boolean prerequisitesMet =
                skill.prerequisites().stream()
                        .allMatch(progress.unlockedSkills()::contains);

        if (!prerequisitesMet) {
            return false;
        }

        Set<String> unlocked =
                new HashSet<>(progress.unlockedSkills());

        unlocked.add(skillId);

        player.setData(
                PLAYER_PROGRESS,
                new Progress(
                        progress.level(),
                        progress.experience(),
                        progress.skillPoints() - skill.pointCost(),
                        unlocked,
                        progress.godEmperorUnlocked()
                )
        );

        return true;
    }

    public static void unlockGodEmperor(ServerPlayer player) {
        Progress progress = get(player);

        player.setData(
                PLAYER_PROGRESS,
                new Progress(
                        progress.level(),
                        progress.experience(),
                        progress.skillPoints(),
                        progress.unlockedSkills(),
                        true
                )
        );
    }
}
