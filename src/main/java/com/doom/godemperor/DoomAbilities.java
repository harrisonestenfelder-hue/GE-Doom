
package com.doom.godemperor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.Comparator;
import java.util.function.Supplier;

public final class DoomAbilities {

    private DoomAbilities() {}

    public record AbilityCooldowns(
            long repulsorReadyAt,
            long arcaneReadyAt,
            long dashReadyAt
    ) {
        public static final MapCodec<AbilityCooldowns> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                com.mojang.serialization.Codec.LONG
                                        .fieldOf("repulsor_ready_at")
                                        .forGetter(AbilityCooldowns::repulsorReadyAt),
                                com.mojang.serialization.Codec.LONG
                                        .fieldOf("arcane_ready_at")
                                        .forGetter(AbilityCooldowns::arcaneReadyAt),
                                com.mojang.serialization.Codec.LONG
                                        .fieldOf("dash_ready_at")
                                        .forGetter(AbilityCooldowns::dashReadyAt)
                        ).apply(instance, AbilityCooldowns::new)
                );

        public static AbilityCooldowns starting() {
            return new AbilityCooldowns(0, 0, 0);
        }
    }

    public static final Supplier<AttachmentType<AbilityCooldowns>>
            PLAYER_ABILITY_COOLDOWNS =
            GodEmperorDoom.ATTACHMENTS.register(
                    "ability_cooldowns",
                    () -> AttachmentType.builder(AbilityCooldowns::starting)
                            .serialize(AbilityCooldowns.CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static void register() {
        // Initializes the ability cooldown attachment.
    }

    public static void activate(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= DoomLoadout.SLOT_COUNT) {
            return;
        }

        String skillId = DoomLoadout.getAssignedSkill(player, slot)
                .orElse(null);

        if (skillId == null) {
            player.displayClientMessage(
                    Component.literal("No power assigned to that slot."),
                    true
            );
            return;
        }

        if (!DoomProgression.get(player).unlockedSkills().contains(skillId)) {
            return;
        }

        switch (skillId) {
            case "repulsor_blast" -> repulsorBlast(player);
            case "arcane_bolt" -> arcaneBolt(player);
            case "doom_dash" -> doomDash(player);

            default -> player.displayClientMessage(
                    Component.literal(
                            "That power has not been implemented yet."
                    ),
                    true
            );
        }
    }

    private static boolean canActivate(
            ServerPlayer player,
            String ability,
            int energyCost
    ) {
        AbilityCooldowns cooldowns =
                player.getData(PLAYER_ABILITY_COOLDOWNS);

        long now = player.level().getGameTime();
        long readyAt = getReadyAt(cooldowns, ability);

        if (now < readyAt) {
            player.displayClientMessage(
                    Component.literal("That power is recharging."),
                    true
            );
            return false;
        }

        GodEmperorDoom.DoomPlayerData data =
                player.getData(GodEmperorDoom.PLAYER_DATA);

        if (data.energy() < energyCost) {
            player.displayClientMessage(
                    Component.literal("Not enough Doom Energy."),
                    true
            );
            return false;
        }

        return true;
    }

    private static long getReadyAt(
            AbilityCooldowns cooldowns,
            String ability
    ) {
        return switch (ability) {
            case "repulsor_blast" -> cooldowns.repulsorReadyAt();
            case "arcane_bolt" -> cooldowns.arcaneReadyAt();
            case "doom_dash" -> cooldowns.dashReadyAt();
            default -> 0;
        };
    }

    private static void spendEnergy(
            ServerPlayer player,
            int cost
    ) {
        GodEmperorDoom.DoomPlayerData data =
                player.getData(GodEmperorDoom.PLAYER_DATA);

        player.setData(
                GodEmperorDoom.PLAYER_DATA,
                new GodEmperorDoom.DoomPlayerData(
                        data.energy() - cost,
                        data.awakened(),
                        data.aegisReadyAt(),
                        data.surgeReadyAt()
                )
        );
    }

    private static void setCooldown(
            ServerPlayer player,
            String ability,
            long duration
    ) {
        AbilityCooldowns old =
                player.getData(PLAYER_ABILITY_COOLDOWNS);

        long readyAt = player.level().getGameTime() + duration;

        AbilityCooldowns updated = switch (ability) {
            case "repulsor_blast" -> new AbilityCooldowns(
                    readyAt,
                    old.arcaneReadyAt(),
                    old.dashReadyAt()
            );

            case "arcane_bolt" -> new AbilityCooldowns(
                    old.repulsorReadyAt(),
                    readyAt,
                    old.dashReadyAt()
            );

            case "doom_dash" -> new AbilityCooldowns(
                    old.repulsorReadyAt(),
                    old.arcaneReadyAt(),
                    readyAt
            );

            default -> old;
        };

        player.setData(PLAYER_ABILITY_COOLDOWNS, updated);
    }

    // REPULSOR BLAST

    private static void repulsorBlast(ServerPlayer player) {
        if (!canActivate(player, "repulsor_blast", 15)) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();

        LivingEntity target = findTarget(player, 24.0, 1.5);

        sendBeam(
                player,
                ParticleTypes.ELECTRIC_SPARK,
                24.0,
                direction
        );

        if (target != null) {
            target.hurt(
                    player.damageSources().playerAttack(player),
                    7.0F
            );

            target.knockback(
                    0.8,
                    -direction.x,
                    -direction.z
            );
        }

        spendEnergy(player, 15);
        setCooldown(player, "repulsor_blast", 20);

        player.displayClientMessage(
                Component.literal("REPULSOR BLAST"),
                true
        );
    }

    // ARCANE BOLT

    private static void arcaneBolt(ServerPlayer player) {
        if (!canActivate(player, "arcane_bolt", 20)) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();

        LivingEntity target = findTarget(player, 28.0, 1.3);

        sendBeam(
                player,
                ParticleTypes.ENCHANT,
                28.0,
                direction
        );

        if (target != null) {
            target.hurt(
                    player.damageSources().playerAttack(player),
                    9.0F
            );

            target.addEffect(
                    new MobEffectInstance(
                            MobEffects.SLOWNESS,
                            60,
                            1
                    )
            );
        }

        spendEnergy(player, 20);
        setCooldown(player, "arcane_bolt", 35);

        player.displayClientMessage(
                Component.literal("ARCANE BOLT"),
                true
        );
    }

    // DOOM DASH

    private static void doomDash(ServerPlayer player) {
        if (!canActivate(player, "doom_dash", 20)) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();

        Vec3 movement = new Vec3(
                direction.x * 1.55,
                Math.max(player.getDeltaMovement().y, 0.25),
                direction.z * 1.55
        );

        player.setDeltaMovement(movement);
        player.hasImpulse = true;

        player.level().sendParticles(
                ParticleTypes.CLOUD,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                18,
                0.35,
                0.25,
                0.35,
                0.08
        );

        spendEnergy(player, 20);
        setCooldown(player, "doom_dash", 40);

        player.displayClientMessage(
                Component.literal("DOOM DASH"),
                true
        );
    }

    // FIND A LIVING TARGET ALONG THE PLAYER'S AIM

    private static LivingEntity findTarget(
            ServerPlayer player,
            double range,
            double radius
    ) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        AABB searchArea = player.getBoundingBox()
                .expandTowards(direction.scale(range))
                .inflate(radius);

        return player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        searchArea,
                        target -> target != player && target.isAlive()
                )
                .stream()
                .filter(player::hasLineOfSight)
                .filter(target -> {
                    Vec3 offset = target.getBoundingBox()
                            .getCenter()
                            .subtract(start);

                    double distanceAlongAim = offset.dot(direction);

                    if (distanceAlongAim < 0 ||
                            distanceAlongAim > range) {
                        return false;
                    }

                    Vec3 closestPoint = direction.scale(distanceAlongAim);

                    return offset.subtract(closestPoint).lengthSqr()
                            <= radius * radius;
                })
                .min(Comparator.comparingDouble(
                        target -> target.distanceToSqr(player)
                ))
                .orElse(null);
    }

    // VISUAL ENERGY BEAM

    private static void sendBeam(
            ServerPlayer player,
            net.minecraft.core.particles.ParticleOptions particle,
            double range,
            Vec3 direction
    ) {
        Vec3 start = player.getEyePosition();

        for (double distance = 1.0; distance <= range; distance += 0.8) {
            Vec3 position = start.add(direction.scale(distance));

            player.level().sendParticles(
                    particle,
                    position.x,
                    position.y,
                    position.z,
                    2,
                    0.06,
                    0.06,
                    0.06,
                    0.01
            );
        }
    }
}
