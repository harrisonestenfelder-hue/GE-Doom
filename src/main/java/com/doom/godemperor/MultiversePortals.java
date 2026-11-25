package com.doom.godemperor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiversePortals {
    private static final int HUB_FLOOR = 60;
    private static final double GATE_RADIUS = 36.0D;
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    private record Destination(String dimension, int color, boolean locked) {
        ResourceKey<Level> key() {
            return ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(GodEmperorDoom.MOD_ID, dimension)
            );
        }
    }

    // Eleven conquerable realities and one final reality reserved for the Absolute.
    private static final List<Destination> DESTINATIONS = List.of(
            new Destination("universe_one", 0x55FFAA, false),
            new Destination("universe_two", 0xFF5533, false),
            new Destination("universe_three", 0xAA66FF, false),
            new Destination("frozen_wastes", 0x8BE8FF, false),
            new Destination("skylands", 0xFFE9A3, false),
            new Destination("ancient_overgrowth", 0x4C9B45, false),
            new Destination("cyber_city", 0x00E5FF, false),
            new Destination("oceanic_realm", 0x176BCE, false),
            new Destination("volcanic_wastes", 0xFF6A00, false),
            new Destination("crystal_dunes", 0xE6A8FF, false),
            new Destination("corrupted_realm", 0x8A35C7, false),
            new Destination("final_universe", 0xFFD700, true)
    );

    private MultiversePortals() {}

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        animatePortals(level, player.tickCount);

        Integer until = COOLDOWNS.get(player.getUUID());
        if (until != null) {
            if (player.tickCount < until) return;
            COOLDOWNS.remove(player.getUUID());
        }

        if (level.dimension().equals(MultiverseHub.HUB_DIMENSION)) {
            for (int i = 0; i < DESTINATIONS.size(); i++) {
                Destination destination = DESTINATIONS.get(i);
                double angle = Math.PI * 2.0D * i / DESTINATIONS.size();
                if (insideGate(player, angle, HUB_FLOOR)) {
                    enterUniverse(player, level, destination);
                    return;
                }
            }
        } else if (isUniverse(level.dimension())
                && Math.abs(player.getX()) <= 3.2D
                && Math.abs(player.getZ()) <= 1.8D
                && player.getY() >= 8.0D && player.getY() <= 20.0D) {
            returnToHub(player, level);
        }
    }

    private static boolean insideGate(ServerPlayer player, double angle, int floor) {
        double nx = Math.cos(angle);
        double nz = Math.sin(angle);
        double tx = -nz;
        double tz = nx;
        double dx = player.getX() - nx * GATE_RADIUS;
        double dz = player.getZ() - nz * GATE_RADIUS;
        double normal = dx * nx + dz * nz;
        double tangent = dx * tx + dz * tz;
        return Math.abs(normal) <= 1.8D && Math.abs(tangent) <= 3.2D
                && player.getY() >= floor + 1.0D
                && player.getY() <= floor + 15.0D;
    }

    private static void enterUniverse(
            ServerPlayer player, ServerLevel hub, Destination destinationInfo
    ) {
        if (destinationInfo.locked()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "THIS REALITY IS SEALED. THE ABSOLUTE STILL STANDS IN YOUR WAY."
                    ), true
            );
            COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
            return;
        }

        MinecraftServer server = hub.getServer();
        ServerLevel destination = server.getLevel(destinationInfo.key());
        if (destination == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "THIS GATEWAY HAS NOT YET STABILIZED."
                    ), true
            );
            COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
            return;
        }

        buildReturnGateway(destination);
        player.teleportTo(destination, 0.5D, 9.0D, 8.5D,
                Set.of(), 180.0F, 0.0F, true);
        player.fallDistance = 0.0F;
        COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("REALITY TRANSIT"), true
        );
        destination.playSound(null, BlockPos.ZERO, SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static boolean isUniverse(ResourceKey<Level> dimension) {
        return DESTINATIONS.stream().anyMatch(destination -> destination.key().equals(dimension));
    }

    private static void returnToHub(ServerPlayer player, ServerLevel currentLevel) {
        MinecraftServer server = currentLevel.getServer();
        ServerLevel hub = server.getLevel(MultiverseHub.HUB_DIMENSION);
        if (hub == null) return;

        player.teleportTo(hub, 0.5D, HUB_FLOOR + 2.0D, 8.5D,
                Set.of(), 180.0F, 0.0F, true);
        player.fallDistance = 0.0F;
        COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("RETURNING TO THE MULTIVERSE HUB"), true
        );
        hub.playSound(null, new BlockPos(0, HUB_FLOOR, 8),
                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.1F);
    }

    private static void buildReturnGateway(ServerLevel level) {
        BlockPos marker = new BlockPos(0, 7, 0);
        if (level.getBlockState(marker).is(Blocks.EMERALD_BLOCK)) return;

        BlockState floor = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState frame = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState trim = Blocks.GOLD_BLOCK.defaultBlockState();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                level.setBlock(new BlockPos(x, 7, z), floor, 3);
            }
        }
        for (int side : new int[]{-5, 5}) {
            for (int y = 8; y <= 18; y++) {
                level.setBlock(new BlockPos(side, y, 0), frame, 3);
            }
        }
        for (int x = -5; x <= 5; x++) {
            level.setBlock(new BlockPos(x, 18, 0), trim, 3);
        }
        level.setBlock(marker, Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
    }

    private static void animatePortals(ServerLevel level, int tick) {
        if (tick % 3 != 0) return;
        if (level.dimension().equals(MultiverseHub.HUB_DIMENSION)) {
            for (int i = 0; i < DESTINATIONS.size(); i++) {
                Destination destination = DESTINATIONS.get(i);
                double angle = Math.PI * 2.0D * i / DESTINATIONS.size();
                animateGate(level, Math.cos(angle) * GATE_RADIUS,
                        Math.sin(angle) * GATE_RADIUS, angle, destination.color(), tick, HUB_FLOOR);
            }
        } else if (isUniverse(level.dimension())) {
            animateGate(level, 0.0D, 0.0D, 0.0D, 0xFFFFFF, tick, 7);
        }
    }

    private static void animateGate(
            ServerLevel level, double centerX, double centerZ,
            double angle, int color, int tick, int floor
    ) {
        double tx = -Math.sin(angle);
        double tz = Math.cos(angle);
        for (int i = 0; i < 9; i++) {
            double sway = Math.sin(tick * 0.11D + i * 0.7D) * 0.18D;
            double offset = (i - 4) * 0.72D;
            double x = centerX + tx * offset + Math.cos(angle) * sway;
            double z = centerZ + tz * offset + Math.sin(angle) * sway;
            double y = floor + 2.0D + i;
            level.sendParticles(new DustParticleOptions(color, 1.35F),
                    x, y, z, 2, 0.04D, 0.10D, 0.04D, 0.01D);
            level.sendParticles(ParticleTypes.PORTAL,
                    x, y, z, 2, 0.08D, 0.12D, 0.08D, 0.02D);
        }
    }
}
