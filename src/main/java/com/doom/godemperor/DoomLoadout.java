
package com.doom.godemperor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class DoomLoadout {

    private DoomLoadout() {}

    public static final int SLOT_COUNT = 9;

    public record Loadout(List<String> slots) {

        public Loadout {
            slots = List.copyOf(slots);

            if (slots.size() != SLOT_COUNT) {
                throw new IllegalArgumentException(
                        "A Doom loadout must contain exactly nine slots."
                );
            }
        }

        public static final MapCodec<Loadout> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                Codec.STRING.listOf()
                                        .fieldOf("slots")
                                        .forGetter(Loadout::slots)
                        ).apply(instance, Loadout::new)
                );

        public static Loadout starting() {
            return new Loadout(
                    List.of(
                            "", "", "",
                            "", "", "",
                            "", "", ""
                    )
            );
        }
    }

    public static final Supplier<AttachmentType<Loadout>> PLAYER_LOADOUT =
            GodEmperorDoom.ATTACHMENTS.register(
                    "ability_loadout",
                    () -> AttachmentType.builder(Loadout::starting)
                            .serialize(Loadout.CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static void register() {
        // Initializes the player loadout attachment.
    }

    public static Loadout get(ServerPlayer player) {
        return player.getData(PLAYER_LOADOUT);
    }

    public static boolean assign(
            ServerPlayer player,
            int slot,
            String skillId
    ) {
        if (slot < 0 || slot >= SLOT_COUNT || skillId == null) {
            return false;
        }

        DoomProgression.Progress progress =
                DoomProgression.get(player);

        if (!progress.unlockedSkills().contains(skillId)) {
            return false;
        }

        if (DoomSkillTree.getSkill(skillId).isEmpty()) {
            return false;
        }

        Loadout current = get(player);

        // Do not put the same power in multiple slots.
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != slot &&
                    current.slots().get(i).equals(skillId)) {
                return false;
            }
        }

        List<String> updated =
                new ArrayList<>(current.slots());

        updated.set(slot, skillId);

        player.setData(
                PLAYER_LOADOUT,
                new Loadout(updated)
        );

        return true;
    }

    public static boolean clear(
            ServerPlayer player,
            int slot
    ) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return false;
        }

        Loadout current = get(player);

        List<String> updated =
                new ArrayList<>(current.slots());

        updated.set(slot, "");

        player.setData(
                PLAYER_LOADOUT,
                new Loadout(updated)
        );

        return true;
    }

    public static Optional<String> getAssignedSkill(
            ServerPlayer player,
            int slot
    ) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return Optional.empty();
        }

        String skillId = get(player).slots().get(slot);

        if (skillId.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(skillId);
    }
}
