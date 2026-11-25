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
import net.minecraft.world.level.block.Block;
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
        buildArrivalLandmark(destination);
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

    private static void buildArrivalLandmark(ServerLevel level) {
        BlockPos marker = new BlockPos(24, 7, 24);
        if (level.getBlockState(marker).is(Blocks.DIAMOND_BLOCK)) return;

        String id = DESTINATIONS.stream()
                .filter(destination -> destination.key().equals(level.dimension()))
                .map(Destination::dimension)
                .findFirst().orElse("");
        switch (id) {
            case "universe_one" -> buildLushSanctuary(level);
            case "universe_two" -> buildCrimsonObelisks(level);
            case "universe_three" -> buildEndMonolith(level);
            case "frozen_wastes" -> buildSpireRing(level, Blocks.PACKED_ICE, Blocks.BLUE_ICE, 0x8BE8FF);
            case "skylands" -> buildSkyPillars(level);
            case "ancient_overgrowth" -> buildAncientGrove(level);
            case "cyber_city" -> buildNeonTowers(level);
            case "oceanic_realm" -> buildOceanSpires(level);
            case "volcanic_wastes" -> buildVolcanicPillars(level);
            case "crystal_dunes" -> buildCrystalSpire(level);
            case "corrupted_realm" -> buildCorruptedShrine(level);
            case "final_universe" -> buildFinalMonument(level);
            default -> { return; }
        }
        level.setBlock(marker, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
    }

    private static void buildLushSanctuary(ServerLevel level) {
        pillar(level, 0, 9, 0, 18, Blocks.MOSS_BLOCK, Blocks.MOSSY_COBBLESTONE);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 12);
            int z = (int) Math.round(Math.sin(a) * 12);
            pillar(level, x, 9, z, 7 + (i % 3), Blocks.MOSSY_COBBLESTONE, Blocks.GLOWSTONE);
            level.setBlock(new BlockPos(x, 17 + (i % 3), z), Blocks.OAK_LEAVES.defaultBlockState(), 3);
        }
    }

    private static void buildCrimsonObelisks(ServerLevel level) {
        buildSpireRing(level, Blocks.CRIMSON_NYLIUM, Blocks.NETHER_BRICKS, 0xFF5533);
    }

    private static void buildEndMonolith(ServerLevel level) {
        pillar(level, 0, 9, 0, 24, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK);
        for (int i = 0; i < 4; i++) {
            int x = (i % 2 == 0 ? -1 : 1) * 13;
            int z = (i < 2 ? -1 : 1) * 13;
            pillar(level, x, 9, z, 12, Blocks.PURPUR_PILLAR, Blocks.END_ROD);
        }
    }

    private static void buildSpireRing(ServerLevel level, Block base, Block tip, int color) {
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            int x = (int) Math.round(Math.cos(a) * 17);
            int z = (int) Math.round(Math.sin(a) * 17);
            int height = 9 + (i * 7 % 15);
            pillar(level, x, 9, z, height, base.defaultBlockState(), tip.defaultBlockState());
            level.sendParticles(new DustParticleOptions(color, 1.0F),
                    x + 0.5D, 9 + height, z + 0.5D, 12, 0.4D, 0.8D, 0.4D, 0.01D);
        }
    }

    private static void buildSkyPillars(ServerLevel level) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 19);
            int z = (int) Math.round(Math.sin(a) * 19);
            pillar(level, x, 9, z, 16 + (i % 3) * 4,
                    Blocks.CALCITE, Blocks.GOLD_BLOCK);
        }
        pillar(level, 0, 9, 0, 12, Blocks.QUARTZ_BLOCK, Blocks.SEA_LANTERN);
    }

    private static void buildAncientGrove(ServerLevel level) {
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7;
            int x = (int) Math.round(Math.cos(a) * 16);
            int z = (int) Math.round(Math.sin(a) * 16);
            pillar(level, x, 9, z, 13 + (i % 3) * 3,
                    Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 3) {
                        level.setBlock(new BlockPos(x + dx, 22 + (i % 3) * 3, z + dz),
                                Blocks.MANGROVE_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void buildNeonTowers(ServerLevel level) {
        BlockState[] palette = {Blocks.CYAN_CONCRETE.defaultBlockState(),
                Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()};
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 19);
            int z = (int) Math.round(Math.sin(a) * 19);
            int height = 12 + (i * 11 % 20);
            pillar(level, x, 9, z, height, palette[i % palette.length], Blocks.SEA_LANTERN);
        }
    }

    private static void buildOceanSpires(ServerLevel level) {
        for (int i = 0; i < 9; i++) {
            double a = Math.PI * 2 * i / 9;
            int x = (int) Math.round(Math.cos(a) * 17);
            int z = (int) Math.round(Math.sin(a) * 17);
            pillar(level, x, 9, z, 8 + (i * 5 % 13),
                    Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN);
        }
        pillar(level, 0, 9, 0, 15, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE);
    }

    private static void buildVolcanicPillars(ServerLevel level) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 18);
            int z = (int) Math.round(Math.sin(a) * 18);
            pillar(level, x, 9, z, 10 + (i * 9 % 17),
                    Blocks.BASALT, Blocks.MAGMA_BLOCK);
            level.setBlock(new BlockPos(x, 9, z), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        }
    }

    private static void buildCrystalSpire(ServerLevel level) {
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            int x = (int) Math.round(Math.cos(a) * 15);
            int z = (int) Math.round(Math.sin(a) * 15);
            pillar(level, x, 9, z, 9 + (i % 3) * 5,
                    Blocks.CALCITE, Blocks.AMETHYST_BLOCK);
            level.setBlock(new BlockPos(x, 9 + 10 + (i % 3) * 5, z),
                    Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
        }
    }

    private static void buildCorruptedShrine(ServerLevel level) {
        pillar(level, 0, 9, 0, 20, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 14);
            int z = (int) Math.round(Math.sin(a) * 14);
            pillar(level, x, 9, z, 8 + (i % 4) * 3,
                    Blocks.WARPED_STEM, Blocks.SHROOMLIGHT);
        }
    }

    private static void buildFinalMonument(ServerLevel level) {
        pillar(level, 0, 9, 0, 30, Blocks.QUARTZ_PILLAR, Blocks.GOLD_BLOCK);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            int x = (int) Math.round(Math.cos(a) * 20);
            int z = (int) Math.round(Math.sin(a) * 20);
            pillar(level, x, 9, z, 18, Blocks.GOLD_BLOCK, Blocks.SEA_LANTERN);
        }
    }

    private static void pillar(
            ServerLevel level, int x, int baseY, int z, int height,
            Block body, Block crown
    ) {
        pillar(level, x, baseY, z, height, body.defaultBlockState(), crown.defaultBlockState());
    }

    private static void pillar(
            ServerLevel level, int x, int baseY, int z, int height,
            BlockState body, Block crown
    ) {
        pillar(level, x, baseY, z, height, body, crown.defaultBlockState());
    }

    private static void pillar(
            ServerLevel level, int x, int baseY, int z, int height,
            BlockState body, BlockState crown
    ) {
        for (int y = 0; y < height; y++) {
            BlockState state = y == height - 1 ? crown : body;
            level.setBlock(new BlockPos(x, baseY + y, z), state, 3);
        }
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
