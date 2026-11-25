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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

        int returnFloor = buildReturnGateway(destination);
        buildArrivalLandmark(destination);
        player.teleportTo(destination, 0.5D, returnFloor + 1.0D, 0.5D,
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


    private static int buildReturnGateway(ServerLevel level) {
        // Force terrain generation before querying the surface height.
        level.getChunk(0, 0);
        int surfaceY = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        int floorY = Math.max(8, surfaceY - 1);
        BlockPos marker = new BlockPos(0, floorY + 12, 0);
        if (level.getBlockState(marker).is(Blocks.BEACON)) {
            return floorY;
        }

        // Remove the earlier Y=8 gateway if it was created before terrain generation.
        BlockPos legacyMarker = new BlockPos(0, 20, 0);
        if (level.getBlockState(legacyMarker).is(Blocks.SEA_LANTERN)) {
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    for (int y = 8; y <= 20; y++) {
                        BlockPos oldPos = new BlockPos(x, y, z);
                        BlockState oldState = level.getBlockState(oldPos);
                        if (oldState.is(Blocks.PURPUR_BLOCK)
                                || oldState.is(Blocks.QUARTZ_PILLAR)
                                || oldState.is(Blocks.GOLD_BLOCK)
                                || oldState.is(Blocks.SEA_LANTERN)) {
                            level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        BlockState floor = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState frame = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState trim = Blocks.GOLD_BLOCK.defaultBlockState();

        // A broad, solid arrival platform is placed at the actual terrain surface.
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                level.setBlock(new BlockPos(x, floorY, z), floor, 3);
                for (int y = floorY + 1; y <= floorY + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int side : new int[]{-5, 5}) {
            for (int y = floorY + 1; y <= floorY + 10; y++) {
                level.setBlock(new BlockPos(side, y, 0), frame, 3);
            }
        }
        for (int x = -5; x <= 5; x++) {
            level.setBlock(new BlockPos(x, floorY + 10, 0), trim, 3);
        }
        for (int x = -8; x <= 8; x++) {
            level.setBlock(new BlockPos(x, floorY, -8), trim, 3);
            leve    private static void buildArrivalLandmark(ServerLevel level) {
        // A vertical marker scan avoids rebuilding the fortress after its own towers
        // change the terrain heightmap.
        BlockPos markerColumn = new BlockPos(48, level.getMinY(), 48);
        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            if (level.getBlockState(new BlockPos(48, y, 48)).is(Blocks.BEACON)) {
                return;
            }
        }

        // Force all chunks used by the citadel and its approach to generate first.
        for (int cx = -1; cx <= 6; cx++) {
            for (int cz = -1; cz <= 6; cz++) {
                level.getChunk(cx, cz);
            }
        }

        int baseY = Math.max(level.getMinY() + 8, level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                48, 48) - 1);

        // Remove the old compact fortress marker and its recognizable block palette.
        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            BlockPos oldMarker = new BlockPos(24, y, 24);
            if (level.getBlockState(oldMarker).is(Blocks.BEACON)) {
                level.setBlock(oldMarker, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (int y = Math.max(level.getMinY(), baseY - 12);
             y <= Math.min(level.getMaxY() - 1, baseY + 52); y++) {
            for (int x = 3; x <= 45; x++) {
                for (int z = 3; z <= 45; z++) {
                    BlockPos oldPos = new BlockPos(x, y, z);
                    BlockState oldState = level.getBlockState(oldPos);
                    if (oldState.is(Blocks.MOSSY_STONE_BRICKS)
                            || oldState.is(Blocks.MOSS_BLOCK)
                            || oldState.is(Blocks.MOSSY_COBBLESTONE)
                            || oldState.is(Blocks.GREEN_CONCRETE)
                            || oldState.is(Blocks.RED_NETHER_BRICKS)
                            || oldState.is(Blocks.NETHER_BRICKS)
                            || oldState.is(Blocks.CRIMSON_PLANKS)
                            || oldState.is(Blocks.NETHER_WART_BLOCK)
                            || oldState.is(Blocks.END_STONE_BRICKS)
                            || oldState.is(Blocks.PURPUR_BLOCK)
                            || oldState.is(Blocks.PURPUR_PILLAR)
                            || oldState.is(Blocks.PACKED_ICE)
                            || oldState.is(Blocks.SNOW_BLOCK)
                            || oldState.is(Blocks.BLUE_ICE)
                            || oldState.is(Blocks.CALCITE)
                            || oldState.is(Blocks.QUARTZ_BLOCK)
                            || oldState.is(Blocks.GOLD_BLOCK)
                            || oldState.is(Blocks.MUD_BRICKS)
                            || oldState.is(Blocks.MANGROVE_PLANKS)
                            || oldState.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                            || oldState.is(Blocks.CYAN_CONCRETE)
                            || oldState.is(Blocks.PRISMARINE_BRICKS)
                            || oldState.is(Blocks.DARK_PRISMARINE)
                            || oldState.is(Blocks.BLACKSTONE)
                            || oldState.is(Blocks.BASALT)
                            || oldState.is(Blocks.DEEPSLATE_BRICKS)
                            || oldState.is(Blocks.CRYING_OBSIDIAN)
                            || oldState.is(Blocks.SEA_LANTERN)) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        String id = DESTINATIONS.stream()
                .filter(destination -> destination.key().equals(level.dimension()))
                .map(Destination::dimension)
                .findFirst().orElse("");

        BlockState foundation;
        BlockState wall;
        BlockState trim;
        BlockState roof;
        switch (id) {
            case "universe_one" -> {
                foundation = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                wall = Blocks.MOSS_BLOCK.defaultBlockState();
                trim = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                roof = Blocks.GREEN_CONCRETE.defaultBlockState();
            }
            case "universe_two" -> {
                foundation = Blocks.RED_NETHER_BRICKS.defaultBlockState();
                wall = Blocks.NETHER_BRICKS.defaultBlockState();
                trim = Blocks.CRIMSON_PLANKS.defaultBlockState();
                roof = Blocks.NETHER_WART_BLOCK.defaultBlockState();
            }
            case "universe_three" -> {
                foundation = Blocks.END_STONE_BRICKS.defaultBlockState();
                wall = Blocks.PURPUR_BLOCK.defaultBlockState();
                trim = Blocks.OBSIDIAN.defaultBlockState();
                roof = Blocks.PURPUR_PILLAR.defaultBlockState();
            }
            case "frozen_wastes" -> {
                foundation = Blocks.PACKED_ICE.defaultBlockState();
                wall = Blocks.SNOW_BLOCK.defaultBlockState();
                trim = Blocks.BLUE_ICE.defaultBlockState();
                roof = Blocks.ICE.defaultBlockState();
            }
            case "skylands" -> {
                foundation = Blocks.CALCITE.defaultBlockState();
                wall = Blocks.QUARTZ_BLOCK.defaultBlockState();
                trim = Blocks.GOLD_BLOCK.defaultBlockState();
                roof = Blocks.SMOOTH_QUARTZ.defaultBlockState();
            }
            case "ancient_overgrowth" -> {
                foundation = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                wall = Blocks.MUD_BRICKS.defaultBlockState();
                trim = Blocks.MANGROVE_PLANKS.defaultBlockState();
                roof = Blocks.MANGROVE_LEAVES.defaultBlockState();
            }
            case "cyber_city" -> {
                foundation = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                wall = Blocks.CYAN_CONCRETE.defaultBlockState();
                trim = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                roof = Blocks.SEA_LANTERN.defaultBlockState();
            }
            case "oceanic_realm" -> {
                foundation = Blocks.PRISMARINE_BRICKS.defaultBlockState();
                wall = Blocks.DARK_PRISMARINE.defaultBlockState();
                trim = Blocks.PRISMARINE.defaultBlockState();
                roof = Blocks.SEA_LANTERN.defaultBlockState();
            }
            case "volcanic_wastes" -> {
                foundation = Blocks.BLACKSTONE.defaultBlockState();
                wall = Blocks.BASALT.defaultBlockState();
                trim = Blocks.MAGMA_BLOCK.defaultBlockState();
                roof = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            case "crystal_dunes" -> {
                foundation = Blocks.CALCITE.defaultBlockState();
                wall = Blocks.AMETHYST_BLOCK.defaultBlockState();
                trim = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                roof = Blocks.BUDDING_AMETHYST.defaultBlockState();
            }
            case "corrupted_realm" -> {
                foundation = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                wall = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                trim = Blocks.WARPED_PLANKS.defaultBlockState();
                roof = Blocks.RESPAWN_ANCHOR.defaultBlockState();
            }
            default -> {
                foundation = Blocks.QUARTZ_BLOCK.defaultBlockState();
                wall = Blocks.QUARTZ_PILLAR.defaultBlockState();
                trim = Blocks.GOLD_BLOCK.defaultBlockState();
                roof = Blocks.SEA_LANTERN.defaultBlockState();
            }
        }

        // Flatten a 73x73 citadel plateau and clear a generous build volume.
        for (int x = 12; x <= 84; x++) {
            for (int z = 12; z <= 84; z++) {
                BlockState floor = ((x + z) % 9 == 0)
                        ? trim : foundation;
                if (x == 12 || x == 84 || z == 12 || z == 84) floor = trim;
                level.setBlock(new BlockPos(x, baseY, z), floor, 3);
                for (int y = baseY + 1; y <= baseY + 48; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Deep outer foundation and a dark perimeter moat-ring.
        for (int x = 9; x <= 87; x++) {
            for (int z = 9; z <= 87; z++) {
                if (x <= 11 || x >= 85 || z <= 11 || z >= 85) {
                    level.setBlock(new BlockPos(x, baseY, z),
                            Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }

        // Main curtain wall: thick, walkable ramparts with regularly spaced merlons.
        for (int y = baseY + 1; y <= baseY + 17; y++) {
            for (int p = 18; p <= 78; p++) {
                boolean gateOpening = p >= 43 && p <= 53;
                if (!gateOpening) {
                    for (int t = 0; t < 3; t++) {
                        level.setBlock(new BlockPos(p, y, 18 + t), wall, 3);
                    }
                }
                for (int t = 0; t < 3; t++) {
                    level.setBlock(new BlockPos(p, y, 78 - t), wall, 3);
                    level.setBlock(new BlockPos(18 + t, y, p), wall, 3);
                    level.setBlock(new BlockPos(78 - t, y, p), wall, 3);
                }
            }
        }
        for (int p = 18; p <= 78; p++) {
            for (int t = 0; t < 3; t++) {
                level.setBlock(new BlockPos(p, baseY + 18, 18 + t), trim, 3);
                level.setBlock(new BlockPos(p, baseY + 18, 78 - t), trim, 3);
                level.setBlock(new BlockPos(18 + t, baseY + 18, p), trim, 3);
                level.setBlock(new BlockPos(78 - t, baseY + 18, p), trim, 3);
            }
            if (p % 3 == 0) {
                for (int t = 0; t < 3; t++) {
                    if (p < 43 || p > 53) {
                        level.setBlock(new BlockPos(p, baseY + 20, 18 + t), roof, 3);
                    }
                    level.setBlock(new BlockPos(p, baseY + 20, 78 - t), roof, 3);
                    level.setBlock(new BlockPos(18 + t, baseY + 20, p), roof, 3);
                    level.setBlock(new BlockPos(78 - t, baseY + 20, p), roof, 3);
                }
            }
        }

        // Four colossal corner bastions with layered crowns and glowing cores.
        for (int cx : new int[]{18, 78}) {
            for (int cz : new int[]{18, 78}) {
                for (int y = baseY + 1; y <= baseY + 34; y++) {
                    for (int dx = -6; dx <= 6; dx++) {
                        for (int dz = -6; dz <= 6; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) >= 4) {
                                level.setBlock(new BlockPos(cx + dx, y, cz + dz), wall, 3);
                            }
                        }
                    }
                }
                for (int layer = 0; layer < 4; layer++) {
                    int radius = 7 - layer;
                    int yy = baseY + 35 + layer;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                                level.setBlock(new BlockPos(cx + dx, yy, cz + dz),
                                        layer % 2 == 0 ? trim : roof, 3);
                            }
                        }
                    }
                }
                for (int y = baseY + 27; y <= baseY + 34; y++) {
                    level.setBlock(new BlockPos(cx, y, cz), Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            level.setBlock(new BlockPos(cx + dx, baseY + 39, cz + dz), trim, 3);
                        }
                    }
                }
            }
        }

        // Gatehouse: twin keeps, a monumental arch and a raised portcullis.
        for (int side : new int[]{40, 56}) {
            for (int y = baseY + 1; y <= baseY + 27; y++) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
                            level.setBlock(new BlockPos(side + dx, y, 18 + dz), wall, 3);
                        }
                    }
                }
                level.setBlock(new BlockPos(side, y, 18), trim, 3);
            }
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) == 4 || Math.abs(dz) == 4) {
                        level.setBlock(new BlockPos(side + dx, baseY + 28, 18 + dz), trim, 3);
                    }
                }
            }
        }
        for (int y = baseY + 1; y <= baseY + 20; y++) {
            for (int x = 43; x <= 53; x++) {
                if (x <= 44 || x >= 52 || y >= baseY + 15) {
                    level.setBlock(new BlockPos(x, y, 18), trim, 3);
                }
            }
        }
        for (int x = 44; x <= 52; x++) {
            for (int y = baseY + 2; y <= baseY + 14; y++) {
                if ((x + y) % 2 == 0) {
                    level.setBlock(new BlockPos(x, y, 18), Blocks.IRON_BARS.defaultBlockState(), 3);
                }
            }
        }

        // Elevated processional bridge connects the arrival portal to the fortress gate.
        int arrivalY = Math.max(level.getMinY() + 8, level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                0, 0) - 1);
        for (int step = 0; step <= 40; step++) {
            int x = 8 + step;
            int z = 8 + (step * 10 / 40);
            int y = arrivalY + (baseY - arrivalY) * step / 40;
            for (int width = -3; width <= 3; width++) {
                level.setBlock(new BlockPos(x, y, z + width),
                        width == -3 || width == 3 ? trim : foundation, 3);
                if (width == -3 || width == 3) {
                    level.setBlock(new BlockPos(x, y + 1, z + width),
                            Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                }
            }
            if (step % 5 == 0) {
                for (int width : new int[]{-4, 4}) {
                    for (int pillarY = y - 1; pillarY >= Math.max(level.getMinY() + 1, y - 8); pillarY--) {
                        level.setBlock(new BlockPos(x, pillarY, z + width),
                                Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                    }
                    level.setBlock(new BlockPos(x, y + 1, z + width),
                            Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }
            }
        }

        // Inner courtyard: concentric command dais, four obelisks and a central power seal.
        for (int x = 31; x <= 65; x++) {
            for (int z = 31; z <= 65; z++) {
                int dx = x - 48;
                int dz = z - 48;
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                if (distance == 15 || distance == 14) {
                    level.setBlock(new BlockPos(x, baseY + 1, z), trim, 3);
                } else if (distance == 10 || distance == 9) {
                    level.setBlock(new BlockPos(x, baseY + 1, z), roof, 3);
                }
            }
        }
        for (int[] point : new int[][]{{32,32},{64,32},{32,64},{64,64}}) {
            for (int y = baseY + 1; y <= baseY + 22; y++) {
                level.setBlock(new BlockPos(point[0], y, point[1]), trim, 3);
                if (y % 3 == 0) {
                    level.setBlock(new BlockPos(point[0] + 1, y, point[1]), roof, 3);
                }
            }
            level.setBlock(new BlockPos(point[0], baseY + 23, point[1]),
                    Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }

        // Central royal keep: layered hall, buttresses, roof terraces and a 46-block spire.
        for (int y = baseY + 1; y <= baseY + 29; y++) {
            for (int p = 35; p <= 61; p++) {
                level.setBlock(new BlockPos(p, y, 35), wall, 3);
                level.setBlock(new BlockPos(p, y, 61), wall, 3);
                level.setBlock(new BlockPos(35, y, p), wall, 3);
                level.setBlock(new BlockPos(61, y, p), wall, 3);
            }
        }
        for (int y = baseY + 2; y <= baseY + 27; y++) {
            for (int p : new int[]{38, 58}) {
                level.setBlock(new BlockPos(p, y, 35), trim, 3);
                level.setBlock(new BlockPos(p, y, 61), trim, 3);
                level.setBlock(new BlockPos(35, y, p), trim, 3);
                level.setBlock(new BlockPos(61, y, p), trim, 3);
            }
        }
        for (int layer = 0; layer < 6; layer++) {
            int radius = 15 - layer * 2;
            int yy = baseY + 30 + layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        level.setBlock(new BlockPos(48 + dx, yy, 48 + dz),
                                layer % 2 == 0 ? trim : roof, 3);
                    }
                }
            }
        }
        for (int y = baseY + 36; y <= baseY + 46; y++) {
            int radius = Math.max(1, 5 - (y - (baseY + 36)) / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        level.setBlock(new BlockPos(48 + dx, y, 48 + dz), trim, 3);
                    }
                }
            }
            if (y % 2 == 0) {
                level.setBlock(new BlockPos(48, y, 48), roof, 3);
            }
        }
        level.setBlock(new BlockPos(48, baseY + 47, 48), Blocks.BEACON.defaultBlockState(), 3);

        // Throne platform and throne visible through the keep's central hall.
        for (int x = 43; x <= 53; x++) {
            for (int z = 43; z <= 53; z++) {
                level.setBlock(new BlockPos(x, baseY + 2, z), trim, 3);
            }
        }
        for (int y = baseY + 3; y <= baseY + 10; y++) {
            level.setBlock(new BlockPos(48, y, 51), roof, 3);
            level.setBlock(new BlockPos(48, y, 52), trim, 3);
            level.setBlock(new BlockPos(47, y, 52), trim, 3);
            level.setBlock(new BlockPos(49, y, 52), trim, 3);
        }
        for (int x = 45; x <= 51; x++) {
            level.setBlock(new BlockPos(x, baseY + 11, 52), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        }

        // Realm champions are deliberately formidable and have different combat roles.
        spawnElite(level, EntityType.RAVAGER, "THE COLOSSUS - Siege Beast",
                48, 28, ItemStack.EMPTY, 520.0D, 34.0D, 24.0D, 0.34D, 1);
        spawnElite(level, EntityType.VINDICATOR, "THE VOID WARLORD",
                43, 34, new ItemStack(Items.NETHERITE_AXE), 260.0D, 32.0D, 22.0D, 0.42D, 2);
        spawnElite(level, EntityType.EVOKER, "THE RIFT ARCANIST",
                53, 34, ItemStack.EMPTY, 220.0D, 18.0D, 18.0D, 0.34D, 1);
        spawnElite(level, EntityType.PILLAGER, "THE DREAD MARKSMAN",
                38, 48, new ItemStack(Items.CROSSBOW), 190.0D, 24.0D, 18.0D, 0.36D, 1);
        spawnElite(level, EntityType.WITHER_SKELETON, "THE INFERNAL REAPER",
                58, 48, new ItemStack(Items.NETHERITE_SWORD), 300.0D, 34.0D, 24.0D, 0.40D, 2);
        spawnElite(level, EntityType.PIGLIN_BRUTE, "THE BLACK IRON EXECUTIONER",
                43, 58, new ItemStack(Items.NETHERITE_AXE), 340.0D, 36.0D, 26.0D, 0.38D, 2);
        spawnElite(level, EntityType.WITCH, "THE PLAGUE SORCERESS",
                53, 58, ItemStack.EMPTY, 210.0D, 18.0D, 16.0D, 0.34D, 1);
        spawnElite(level, EntityType.BLAZE, "THE STARFIRE DESTROYER",
                48, 68, ItemStack.EMPTY, 240.0D, 26.0D, 18.0D, 0.36D, 1);

        level.playSound(null, new BlockPos(48, baseY + 24, 48),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.65F);
    }

    private static void spawnElite(
            ServerLevel level, EntityType<? extends Mob> type, String name,
            int x, int z, ItemStack weapon, double health, double damage,
            double armor, double speed, int strengthAmplifier
    ) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) return;

        setEliteAttribute(mob, Attributes.MAX_HEALTH, health);
        setEliteAttribute(mob, Attributes.ATTACK_DAMAGE, damage);
        setEliteAttribute(mob, Attributes.ARMOR, armor);
        setEliteAttribute(mob, Attributes.MOVEMENT_SPEED, speed);
        setEliteAttribute(mob, Attributes.KNOCKBACK_RESISTANCE, 0.85D);
        setEliteAttribute(mob, Attributes.FOLLOW_RANGE, 72.0D);
        mob.setHealth((float) health);
        mob.setPos(x + 0.5D, level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x, z), z + 0.5D);
        mob.setCustomName(net.minecraft.network.chat.Component.literal(name));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();

        if (!weapon.isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 30,
                strengthAmplifier, false, true, true));
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30,
                1, false, true, true));
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30,
                1, false, true, true));
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60 * 30,
                1, false, true, true));
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 30,
                0, false, true, true));
        level.addFreshEntity(mob);
    }

    private static void setEliteAttribute(Mob mob,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double value) {
        var instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

, new ItemStack(Items.IRON_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        level.addFreshEntity(mob);
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
            int returnFloor = Math.max(8, level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    0, 0) - 1);
            animateGate(level, 0.0D, 0.0D, 0.0D, 0xFFFFFF, tick, returnFloor);
        }
    }

    private static void animateGate(
            ServerLevel level, double centerX, double centerZ,
            double angle, int color, int tick, int floor
    ) {
        double tx = -Math.sin(angle);
        double tz = Math.cos(angle);
        double nx = Math.cos(angle);
        double nz = Math.sin(angle);

        // Fill the entire arch with a walk-through curtain of animated energy.
        // The effect is particles only, so it never blocks player movement.
        for (int row = 0; row < 9; row++) {
            double y = floor + 2.0D + row * 1.18D;
            for (int column = 0; column < 7; column++) {
                double across = (column - 3) * 1.35D;
                double ripple = Math.sin(tick * 0.12D + row * 0.72D
                        + column * 0.58D) * 0.28D;
                double depth = Math.cos(tick * 0.09D + row * 0.55D
                        - column * 0.43D) * 0.16D;

                double x = centerX + tx * (across + ripple) + nx * depth;
                double z = centerZ + tz * (across + ripple) + nz * depth;

                level.sendParticles(new DustParticleOptions(color, 1.65F),
                        x, y, z, 1, 0.035D, 0.045D, 0.035D, 0.005D);
                level.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 1, 0.06D, 0.08D, 0.06D, 0.015D);
            }
        }
    }
}
