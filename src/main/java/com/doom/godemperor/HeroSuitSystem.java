package com.doom.godemperor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeroSuitSystem {
    private HeroSuitSystem() {}

    public record Suit(
            String id,
            String displayName,
            DeferredItem<Item> helmet,
            DeferredItem<Item> chest,
            DeferredItem<Item> legs,
            DeferredItem<Item> boots,
            int color
    ) {}

    public static final Suit ANTI_MONITOR = register(
            "anti_monitor", "Anti-Monitor", 0x8C35FF, true);
    public static final Suit MONITOR = register(
            "monitor", "The Monitor", 0x36D9FF, false);
    public static final Suit MARTIAN = register(
            "martian_manhunter", "Martian Manhunter", 0x20D5A0, false);
    public static final Suit SPECTRE = register(
            "spectre", "Spectre", 0xE9F4FF, true);
    public static final Suit HARBINGER = register(
            "harbinger", "Harbinger", 0xD9B5FF, false);

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private static Suit register(String id, String name, int color, boolean heavy) {
        var material = heavy
                ? GodEmperorDoom.GOD_EMPEROR_ARMOR_MATERIAL
                : GodEmperorDoom.DOOM_ARMOR_MATERIAL;
        return new Suit(
                id, name,
                armor(id + "_helmet", material, ArmorType.HELMET),
                armor(id + "_chestplate", material, ArmorType.CHESTPLATE),
                armor(id + "_leggings", material, ArmorType.LEGGINGS),
                armor(id + "_boots", material, ArmorType.BOOTS),
                color
        );
    }

    private static DeferredItem<Item> armor(
            String id,
            net.minecraft.world.item.equipment.ArmorMaterial material,
            ArmorType type
    ) {
        return GodEmperorDoom.ITEMS.registerItem(
                id, properties -> new Item(properties.humanoidArmor(material, type)));
    }

    public static Suit[] allSuits() { return new Suit[]{ANTI_MONITOR, MONITOR, MARTIAN, SPECTRE, HARBINGER}; }

    public static void register() {
        // Forces suit item registration before the shared item registry is attached.
    }

    public static boolean activate(ServerPlayer player, int slot) {
        Suit suit = equippedSuit(player);
        if (suit == null) return false;
        if (slot < 0 || slot >= 9) return true;

        String[] powers = powersFor(suit);
        String power = powers[slot];
        if (power == null) {
            player.displayClientMessage(Component.literal("That suit has no power in this slot."), true);
            return true;
        }

        long now = player.level().getGameTime();
        long readyAt = COOLDOWNS.getOrDefault(player.getUUID(), Map.of())
                .getOrDefault(power, 0L);
        if (now < readyAt) {
            player.displayClientMessage(Component.literal("Power recharging: " + (readyAt - now) / 20 + "s"), true);
            return true;
        }

        boolean used = execute(player, suit, power);
        if (used) {
            COOLDOWNS.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
                    .put(power, now + cooldown(power));
        }
        return true;
    }

    private static Suit equippedSuit(ServerPlayer player) {
        for (Suit suit : new Suit[]{ANTI_MONITOR, MONITOR, MARTIAN, SPECTRE, HARBINGER}) {
            if (player.getItemBySlot(EquipmentSlot.HEAD).is(suit.helmet().get())
                    && player.getItemBySlot(EquipmentSlot.CHEST).is(suit.chest().get())
                    && player.getItemBySlot(EquipmentSlot.LEGS).is(suit.legs().get())
                    && player.getItemBySlot(EquipmentSlot.FEET).is(suit.boots().get())) {
                return suit;
            }
        }
        return null;
    }

    private static String[] powersFor(Suit suit) {
        return switch (suit.id()) {
            case "anti_monitor" -> new String[]{"anti_blast","anti_shield","anti_shadow","anti_wave","anti_crush","anti_surge","anti_pulse","anti_aegis","anti_cosmic"};
            case "monitor" -> new String[]{"monitor_blast","monitor_lift","monitor_blink","monitor_matter","monitor_barrier","monitor_pulse","monitor_restore","monitor_surge","monitor_dimension"};
            case "martian_manhunter" -> new String[]{"martian_vision","martian_strength","martian_invisibility","martian_phase","martian_telepathy","martian_flight","martian_regen","martian_speed","martian_sense"};
            case "spectre" -> new String[]{"spectre_blast","spectre_teleport","spectre_phase","spectre_barrier","spectre_wrath","spectre_bind","spectre_restore","spectre_surge","spectre_judgment"};
            case "harbinger" -> new String[]{"harbinger_gravity","harbinger_blast","harbinger_flight","harbinger_regen","harbinger_duplicate","harbinger_crush","harbinger_shield","harbinger_surge","harbinger_pulse"};
            default -> new String[9];
        };
    }

    private static int cooldown(String power) {
        if (power.contains("blast") || power.contains("vision")) return 50;
        if (power.contains("teleport") || power.contains("blink") || power.contains("flight")) return 100;
        if (power.contains("wave") || power.contains("judgment") || power.contains("crush")) return 140;
        return 80;
    }

    private static boolean execute(ServerPlayer player, Suit suit, String power) {
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = findTarget(player, 30.0, 2.0);
        int color = suit.color();
        net.minecraft.server.level.ServerLevel level =
                (net.minecraft.server.level.ServerLevel) player.level();

        switch (power) {
            case "anti_blast", "monitor_blast", "martian_vision", "spectre_blast",
                    "harbinger_blast", "anti_cosmic", "anti_pulse", "monitor_pulse", "spectre_wrath",
                    "spectre_judgment", "harbinger_pulse" -> {
                beam(player, color, 30);
                if (target != null) hit(player, target, power.contains("judgment") ? 24 : 16, 1.4);
            }
            case "anti_shield", "anti_aegis", "monitor_barrier", "spectre_barrier",
                    "harbinger_shield" -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 3));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 3));
                burst(player, color, 45);
            }
            case "anti_shadow", "martian_invisibility", "spectre_phase", "martian_phase" -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
                burst(player, color, 30);
            }
            case "anti_wave", "anti_crush", "harbinger_crush", "spectre_bind",
                    "monitor_lift", "harbinger_gravity" -> {
                var nearby = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(9), e -> e != player && e.isAlive());
                for (LivingEntity entity : nearby) {
                    if (power.contains("lift") || power.contains("gravity")) {
                        entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1));
                    } else {
                        entity.hurt(player.damageSources().playerAttack(player), power.contains("wave") ? 18 : 12);
                        entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 2));
                        entity.knockback(1.2, player.getX() - entity.getX(), player.getZ() - entity.getZ());
                    }
                }
                burst(player, color, 64);
            }
            case "anti_surge", "monitor_surge", "martian_speed", "spectre_surge",
                    "harbinger_surge" -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 3));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 2));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 240, 2));
                burst(player, color, 30);
            }
            case "monitor_blink", "spectre_teleport" -> {
                Vec3 destination = player.position().add(look.scale(9));
                boolean moved = player.randomTeleport(destination.x, destination.y, destination.z, true);
                if (moved) burst(player, color, 35);
            }
            case "monitor_matter", "monitor_restore", "martian_regen", "spectre_restore",
                    "harbinger_regen" -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 2));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                player.heal(8.0F);
                burst(player, color, 30);
            }
            case "monitor_dimension", "martian_flight", "harbinger_flight" -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 260, 0));
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 260, 1));
                burst(player, color, 36);
            }
            case "martian_telepathy", "martian_sense" -> {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(32), e -> e != player && e.isAlive())) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0));
                }
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0));
                burst(player, color, 32);
            }
            case "martian_strength" -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 3));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
            }
            case "harbinger_duplicate" -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 70, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 2));
                burst(player, color, 72);
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(),
                        70, 1.5, 1.0, 1.5, 0.06);
            }
            default -> {
                return false;
            }
        }

        player.displayClientMessage(Component.literal(suit.displayName().toUpperCase() + " — " + label(power)), true);
        return true;
    }

    private static String label(String id) {
        return id.replaceFirst("^[a-z]+_", "").replace('_', ' ').toUpperCase();
    }

    private static LivingEntity findTarget(ServerPlayer player, double range, double radius) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && player.hasLineOfSight(entity))
                .stream()
                .filter(entity -> {
                    Vec3 offset = entity.getBoundingBox().getCenter().subtract(start);
                    double along = offset.dot(direction);
                    return along >= 0 && along <= range
                            && offset.subtract(direction.scale(along)).lengthSqr() <= radius * radius;
                })
                .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }

    private static void hit(ServerPlayer player, LivingEntity target, float damage, double knockback) {
        Vec3 direction = player.getLookAngle().normalize();
        target.hurt(player.damageSources().playerAttack(player), damage);
        target.knockback(knockback, -direction.x, -direction.z);
    }

    private static void beam(ServerPlayer player, int color, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        var particle = new net.minecraft.core.particles.DustParticleOptions(color, 1.8F);
        for (double d = 0.8; d <= range; d += 0.65) {
            Vec3 point = start.add(direction.scale(d));
            player.level().sendParticles(particle, point.x, point.y, point.z, 4, .07, .07, .07, .01);
        }
    }

    private static void burst(ServerPlayer player, int color, int count) {
        player.level().sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(color, 1.6F),
                player.getX(), player.getY() + 1.0, player.getZ(),
                count, 1.0, 1.1, 1.0, .08);
    }
}
