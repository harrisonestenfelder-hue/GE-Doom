
package com.doom.godemperor;

import java.util.List;
import java.util.Optional;

public final class DoomSkillTree {

    private DoomSkillTree() {}

    public enum Stage {
        DOCTOR_DOOM,
        GOD_EMPEROR
    }

    public enum Branch {
        TECHNOLOGY,
        SORCERY,
        COMBAT,
        DOMINION,
        REALITY,
        COSMIC,
        CREATION,
        ABSOLUTE_POWER
    }

    public record Skill(
            String id,
            String name,
            String description,
            Stage stage,
            Branch branch,
            int pointCost,
            int tier,
            List<String> prerequisites
    ) {}

    public static final List<Skill> SKILLS = List.of(

            // DOCTOR DOOM: TECHNOLOGY

            new Skill("repulsor_blast", "Repulsor Blast",
                    "Fire a green energy blast.",
                    Stage.DOCTOR_DOOM, Branch.TECHNOLOGY, 0, 1, List.of()),

            new Skill("doombot", "Doombot",
                    "Summon a mechanical ally.",
                    Stage.DOCTOR_DOOM, Branch.TECHNOLOGY, 2, 2,
                    List.of("repulsor_blast")),

            new Skill("tactical_scan", "Tactical Scan",
                    "Analyze nearby creatures and surroundings.",
                    Stage.DOCTOR_DOOM, Branch.TECHNOLOGY, 1, 2,
                    List.of("repulsor_blast")),

            // DOCTOR DOOM: SORCERY

            new Skill("arcane_bolt", "Arcane Bolt",
                    "Launch a bolt of green mystical energy.",
                    Stage.DOCTOR_DOOM, Branch.SORCERY, 0, 1, List.of()),

            new Skill("mystic_barrier", "Mystic Barrier",
                    "Create a protective magical barrier.",
                    Stage.DOCTOR_DOOM, Branch.SORCERY, 2, 2,
                    List.of("arcane_bolt")),

            new Skill("energy_drain", "Energy Drain",
                    "Restore Doom Energy.",
                    Stage.DOCTOR_DOOM, Branch.SORCERY, 2, 2,
                    List.of("arcane_bolt")),

            // DOCTOR DOOM: COMBAT

            new Skill("doom_dash", "Doom Dash",
                    "Burst forward at incredible speed.",
                    Stage.DOCTOR_DOOM, Branch.COMBAT, 0, 1, List.of()),

            new Skill("enhanced_strength", "Enhanced Strength",
                    "Temporarily enhance physical capabilities.",
                    Stage.DOCTOR_DOOM, Branch.COMBAT, 2, 2,
                    List.of("doom_dash")),

            new Skill("counterpulse", "Counterpulse",
                    "Release a defensive energy pulse.",
                    Stage.DOCTOR_DOOM, Branch.COMBAT, 3, 3,
                    List.of("enhanced_strength")),

            // DOCTOR DOOM: DOMINION

            new Skill("flight", "Flight",
                    "Take to the skies using Doom technology.",
                    Stage.DOCTOR_DOOM, Branch.DOMINION, 2, 2,
                    List.of("tactical_scan")),

            new Skill("teleport", "Teleportation",
                    "Instantly relocate a short distance.",
                    Stage.DOCTOR_DOOM, Branch.DOMINION, 3, 3,
                    List.of("flight")),

            new Skill("mind_dominion", "Mind Dominion",
                    "Temporarily influence nearby creatures.",
                    Stage.DOCTOR_DOOM, Branch.DOMINION, 5, 4,
                    List.of("teleport")),

            // GOD EMPEROR DOOM: REALITY

            new Skill("reality_blast", "Reality Blast",
                    "Unleash white reality energy.",
                    Stage.GOD_EMPEROR, Branch.REALITY, 1, 1, List.of()),

            new Skill("reality_shift", "Reality Shift",
                    "Alter the properties of your surroundings.",
                    Stage.GOD_EMPEROR, Branch.REALITY, 3, 2,
                    List.of("reality_blast")),

            new Skill("matter_creation", "Matter Creation",
                    "Create selected structures and materials.",
                    Stage.GOD_EMPEROR, Branch.CREATION, 3, 2,
                    List.of("reality_blast")),

            // GOD EMPEROR DOOM: COSMIC

            new Skill("cosmic_blast", "Cosmic Blast",
                    "Release overwhelming white cosmic energy.",
                    Stage.GOD_EMPEROR, Branch.COSMIC, 1, 1, List.of()),

            new Skill("cosmic_aegis", "Cosmic Aegis",
                    "Create an immensely powerful protective field.",
                    Stage.GOD_EMPEROR, Branch.COSMIC, 3, 2,
                    List.of("cosmic_blast")),

            new Skill("cosmic_regeneration", "Cosmic Regeneration",
                    "Restore health through cosmic power.",
                    Stage.GOD_EMPEROR, Branch.COSMIC, 3, 3,
                    List.of("cosmic_aegis")),

            // GOD EMPEROR DOOM: CREATION

            new Skill("terrain_rewrite", "Terrain Rewrite",
                    "Reshape selected terrain.",
                    Stage.GOD_EMPEROR, Branch.CREATION, 4, 3,
                    List.of("matter_creation")),

            // GOD EMPEROR DOOM: ABSOLUTE POWER

            new Skill("absolute_dominion", "Absolute Dominion",
                    "Unlock an ultimate reality-control ability.",
                    Stage.GOD_EMPEROR, Branch.ABSOLUTE_POWER, 5, 4,
                    List.of("reality_shift", "cosmic_aegis")),

            new Skill("time_fracture", "Time Fracture",
                    "Temporarily alter the flow of time.",
                    Stage.GOD_EMPEROR, Branch.ABSOLUTE_POWER, 5, 4,
                    List.of("reality_shift", "cosmic_aegis"))
    );

    public static Optional<Skill> getSkill(String id) {
        return SKILLS.stream()
                .filter(skill -> skill.id().equals(id))
                .findFirst();
    }

    public static List<Skill> getBranch(
            Stage stage,
            Branch branch
    ) {
        return SKILLS.stream()
                .filter(skill -> skill.stage() == stage)
                .filter(skill -> skill.branch() == branch)
                .toList();
    }

    public static List<Skill> getStage(Stage stage) {
        return SKILLS.stream()
                .filter(skill -> skill.stage() == stage)
                .toList();
    }
}
