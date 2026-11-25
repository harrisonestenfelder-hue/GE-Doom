package com.doom.godemperor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiverseHub {

    public static final ResourceKey<Level> HUB_DIMENSION =
            ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(
                            GodEmperorDoom.MOD_ID,
                            "multiverse_hub"
                    )
            );

    private static final int FLOOR_Y = 60;
    private static final Map<UUID, ReturnPoint> RETURN_POINTS =
            new ConcurrentHashMap<>();

    private record ReturnPoint(
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {}

    private MultiverseHub() {}

    public static void toggle(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) {
            return;
        }

        ServerLevel hub = server.getLevel(HUB_DIMENSION);
        if (hub == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "The Multiverse Hub is unavailable. Check that the hub dimension data loaded."
                    ),
                    true
            );
            return;
        }

        if (player.level().dimension().equals(HUB_DIMENSION)) {
            returnPlayer(player, server);
            return;
        }

        RETURN_POINTS.put(
                player.getUUID(),
                new ReturnPoint(
                        ((ServerLevel) player.level()).dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                )
        );

        buildHubIfNeeded(hub);
        player.teleportTo(
                hub,
                0.5D,
                FLOOR_Y + 2.0D,
                8.5D,
                Set.of(),
                180.0F,
                0.0F,
                true
        );
        player.fallDistance = 0.0F;
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("WELCOME TO THE MULTIVERSE HUB"),
                true
        );
    }

    private static void returnPlayer(
            ServerPlayer player,
            MinecraftServer server
    ) {
        ReturnPoint point = RETURN_POINTS.remove(player.getUUID());
        ServerLevel destination = point == null
                ? server.overworld()
                : server.getLevel(point.dimension());

        if (destination == null) {
            destination = server.overworld();
            point = null;
        }

        BlockPos safeSpawn = destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.ZERO);
        double x = point == null ? safeSpawn.getX() + 0.5D : point.x();
        double y = point == null ? safeSpawn.getY() : point.y();
        double z = point == null ? safeSpawn.getZ() + 0.5D : point.z();
        float yaw = point == null ? 0.0F : point.yaw();
        float pitch = point == null ? 0.0F : point.pitch();

        player.teleportTo(destination, x, y, z, Set.of(), yaw, pitch, true);
        player.fallDistance = 0.0F;
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("RETURNING TO YOUR PREVIOUS REALITY"),
                true
        );
    }

    private static void buildHubIfNeeded(ServerLevel level) {
        BlockPos marker = new BlockPos(0, FLOOR_Y, 0);
        BlockPos generationMarker = new BlockPos(0, FLOOR_Y - 1, 0);
        if (level.getBlockState(generationMarker).is(Blocks.DIAMOND_BLOCK)) {
            return;
        }

        // Generate the full structure in a broad circular footprint.
        level.getChunkAt(marker);

        BlockState floor = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState polished = Blocks.POLISHED_DIORITE.defaultBlockState();
        BlockState wall = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState silver = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState silverTrim = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState goldTrim = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

        final int radius = 38;
        final int wallHeight = 22;

        // Vast circular floor with a layered quartz-and-silver border.
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distance = x * x + z * z;
                if (distance <= radius * radius) {
                    BlockState tile = ((Math.abs(x / 4) + Math.abs(z / 4)) % 2 == 0)
                            ? floor : polished;
                    put(level, new BlockPos(x, FLOOR_Y, z), tile);
                    if (distance >= 35 * 35) {
                        put(level, new BlockPos(x, FLOOR_Y, z), silverTrim);
                    }
                }
            }
        }

        // Concentric gold geometry makes the floor read as a celestial seal.
        for (int ringRadius : new int[]{7, 13, 21, 30, 36}) {
            buildFloorRing(level, ringRadius, gold, 0);
        }
        for (int axis = -34; axis <= 34; axis++) {
            put(level, new BlockPos(axis, FLOOR_Y, 0), gold);
            put(level, new BlockPos(0, FLOOR_Y, axis), gold);
            if (Math.abs(axis) % 2 == 0) {
                put(level, new BlockPos(axis, FLOOR_Y, 1), silverTrim);
                put(level, new BlockPos(1, FLOOR_Y, axis), silverTrim);
            }
        }

        // Tall circular outer wall. Four wide openings remain clear for portals.
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + wallHeight; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = x * x + z * z;
                    if (distance < (radius * radius) - 3 || distance > (radius * radius) + 3) {
                        continue;
                    }
                    boolean gateOpening = isGateOpening(x, z);
                    if (!gateOpening || y > FLOOR_Y + 14) {
                        put(level, new BlockPos(x, y, z), wall);
                    }
                }
            }
        }

        // Eight monumental silver pillars support the upper temple.
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            int x = (int) Math.round(Math.cos(angle) * 28.0D);
            int z = (int) Math.round(Math.sin(angle) * 28.0D);
            buildTemplePillar(level, x, z, FLOOR_Y + 1, FLOOR_Y + 21,
                    silver, silverTrim, gold, light);
        }

        // A second ring of smaller columns gives the chamber depth and scale.
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0D * i / 16.0D;
            int x = (int) Math.round(Math.cos(angle) * 34.0D);
            int z = (int) Math.round(Math.sin(angle) * 34.0D);
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 13; y++) {
                put(level, new BlockPos(x, y, z), silverTrim);
            }
            put(level, new BlockPos(x, FLOOR_Y + 13, z), gold);
        }

        // Layered domed ceiling: concentric rings rise toward a luminous crown.
        buildDomeRing(level, 36, FLOOR_Y + 18, silverTrim);
        buildDomeRing(level, 31, FLOOR_Y + 22, wall);
        buildDomeRing(level, 24, FLOOR_Y + 25, silver);
        buildDomeRing(level, 16, FLOOR_Y + 28, wall);
        buildDomeRing(level, 8, FLOOR_Y + 30, gold);
        for (int y = FLOOR_Y + 18; y <= FLOOR_Y + 30; y++) {
            int ringRadius = y < FLOOR_Y + 22 ? 35
                    : y < FLOOR_Y + 25 ? 30
                    : y < FLOOR_Y + 28 ? 23
                    : y < FLOOR_Y + 30 ? 15 : 7;
            for (int x = -ringRadius; x <= ringRadius; x++) {
                for (int z = -ringRadius; z <= ringRadius; z++) {
                    if (x * x + z * z <= ringRadius * ringRadius
                            && x * x + z * z >= (ringRadius - 1) * (ringRadius - 1)) {
                        put(level, new BlockPos(x, y, z),
                                (y % 2 == 0) ? wall : silverTrim);
                    }
                }
            }
        }

        // A brilliant central crown and suspended-looking light well.
        for (int y = FLOOR_Y + 27; y <= FLOOR_Y + 31; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (x * x + z * z <= 16) {
                        put(level, new BlockPos(x, y, z), y == FLOOR_Y + 31 ? gold : glass);
                    }
                }
            }
        }
        put(level, new BlockPos(0, FLOOR_Y + 30, 0), light);

        // Twelve monumental gateways encircle the celestial temple.
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            int gateX = (int) Math.round(Math.cos(angle) * 36.0D);
            int gateZ = (int) Math.round(Math.sin(angle) * 36.0D);
            buildRadialGate(level, gateX, gateZ, angle, silverTrim, silver, gold);
        }

        // Gold-and-sea-lantern threshold strips mark each portal approach.
        for (int distance = 9; distance <= 34; distance++) {
            for (int offset = -1; offset <= 1; offset++) {
                put(level, new BlockPos(offset, FLOOR_Y, -distance), gold);
                put(level, new BlockPos(offset, FLOOR_Y, distance), gold);
                put(level, new BlockPos(-distance, FLOOR_Y, offset), gold);
                put(level, new BlockPos(distance, FLOOR_Y, offset), gold);
            }
            if (distance % 5 == 0) {
                put(level, new BlockPos(0, FLOOR_Y + 1, -distance), light);
                put(level, new BlockPos(0, FLOOR_Y + 1, distance), light);
                put(level, new BlockPos(-distance, FLOOR_Y + 1, 0), light);
                put(level, new BlockPos(distance, FLOOR_Y + 1, 0), light);
            }
        }

        // Elevated throne dais at the rear of the chamber, facing the hub.
        buildThroneDais(level, 0, -19, wall, silver, silverTrim, gold, light);

        // Central seal: layered silver, quartz, and gold beneath Doom's arrival point.
        buildFloorRing(level, 5, silverTrim, 0);
        buildFloorRing(level, 3, gold, 0);
        put(level, marker, gold);
        put(level, generationMarker, Blocks.DIAMOND_BLOCK.defaultBlockState());
        put(level, new BlockPos(0, FLOOR_Y + 1, 0), light);
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 5; y++) {
            put(level, new BlockPos(-1, y, 0), glass);
            put(level, new BlockPos(1, y, 0), glass);
        }
    }

    private static boolean isGateOpening(int x, int z) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double nx = Math.cos(angle);
            double nz = Math.sin(angle);
            double tx = -nz;
            double tz = nx;
            double tangent = x * tx + z * tz;
            double radial = x * nx + z * nz;
            if (Math.abs(tangent) <= 5.2D && Math.abs(radial - 36.0D) <= 3.0D) {
                return true;
            }
        }
        return false;
    }

    private static void buildRadialGate(
            ServerLevel level,
            int centerX,
            int centerZ,
            double angle,
            BlockState stone,
            BlockState silver,
            BlockState gold
    ) {
        double tx = -Math.sin(angle);
        double tz = Math.cos(angle);

        for (int side : new int[]{-5, 5}) {
            int x = (int) Math.round(centerX + tx * side);
            int z = (int) Math.round(centerZ + tz * side);
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 13; y++) {
                put(level, new BlockPos(x, y, z),
                        y >= FLOOR_Y + 12 ? gold : stone);
            }
        }

        for (int offset = -5; offset <= 5; offset++) {
            int x = (int) Math.round(centerX + tx * offset);
            int z = (int) Math.round(centerZ + tz * offset);
            put(level, new BlockPos(x, FLOOR_Y + 13, z),
                    Math.abs(offset) <= 1 ? gold : silver);
            put(level, new BlockPos(x, FLOOR_Y, z),
                    Math.abs(offset) <= 1 ? gold : silver);
        }

        // Gold-lit ribs frame the walk-through opening without blocking it.
        for (int y = FLOOR_Y + 2; y <= FLOOR_Y + 11; y += 3) {
            for (int side : new int[]{-4, 4}) {
                int x = (int) Math.round(centerX + tx * side);
                int z = (int) Math.round(centerZ + tz * side);
                put(level, new BlockPos(x, y, z), silver);
            }
        }
    }

    private static void buildGate(
            ServerLevel level,
            int centerX,
            int centerZ,
            boolean alongX,
            BlockState stone,
            BlockState silver,
            BlockState gold
    ) {
        // Two monumental uprights and a crowned lintel frame each future portal.
        for (int side : new int[]{-5, 5}) {
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 13; y++) {
                BlockPos pos = alongX
                        ? new BlockPos(centerX + side, y, centerZ)
                        : new BlockPos(centerX, y, centerZ + side);
                put(level, pos, y >= FLOOR_Y + 12 ? gold : stone);
            }
        }

        for (int offset = -5; offset <= 5; offset++) {
            BlockPos top = alongX
                    ? new BlockPos(centerX + offset, FLOOR_Y + 13, centerZ)
                    : new BlockPos(centerX, FLOOR_Y + 13, centerZ + offset);
            put(level, top, Math.abs(offset) <= 1 ? gold : silver);
        }

        // A silver threshold and gold center mark the walk-through opening.
        for (int offset = -4; offset <= 4; offset++) {
            BlockPos threshold = alongX
                    ? new BlockPos(centerX + offset, FLOOR_Y, centerZ)
                    : new BlockPos(centerX, FLOOR_Y, centerZ + offset);
            put(level, threshold, Math.abs(offset) <= 1 ? gold : silver);
        }

        // Decorative inner ribs make the gateway read as a temple portal frame.
        for (int y = FLOOR_Y + 2; y <= FLOOR_Y + 11; y += 3) {
            for (int side : new int[]{-4, 4}) {
                BlockPos rib = alongX
                        ? new BlockPos(centerX + side, y, centerZ - 1)
                        : new BlockPos(centerX - 1, y, centerZ + side);
                put(level, rib, silver);
            }
        }
    }

    private static void buildFloorRing(
            ServerLevel level, int radius, BlockState state, int yOffset
    ) {
        int steps = Math.max(48, radius * 12);
        for (int i = 0; i < steps; i++) {
            double angle = Math.PI * 2.0D * i / steps;
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            put(level, new BlockPos(x, FLOOR_Y + yOffset, z), state);
        }
    }

    private static void buildTemplePillar(
            ServerLevel level, int x, int z, int bottom, int top,
            BlockState silver, BlockState trim, BlockState gold, BlockState light
    ) {
        for (int y = bottom; y <= top; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    put(level, new BlockPos(x + dx, y, z + dz),
                            dx == 0 && dz == 0 ? silver : trim);
                }
            }
            if ((y - bottom) % 5 == 0) {
                put(level, new BlockPos(x, y, z), light);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    put(level, new BlockPos(x + dx, top, z + dz), gold);
                }
            }
        }
    }

    private static void buildDomeRing(
            ServerLevel level, int radius, int y, BlockState state
    ) {
        int steps = radius * 16;
        for (int i = 0; i < steps; i++) {
            double angle = Math.PI * 2.0D * i / steps;
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            put(level, new BlockPos(x, y, z), state);
        }
    }

    private static void buildThroneDais(
            ServerLevel level, int centerX, int centerZ,
            BlockState quartz, BlockState silver, BlockState trim,
            BlockState gold, BlockState light
    ) {
        for (int layer = 0; layer < 4; layer++) {
            int half = 7 - layer;
            int y = FLOOR_Y + layer;
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    put(level, new BlockPos(centerX + x, y, centerZ + z),
                            layer == 3 ? quartz : trim);
                }
            }
        }

        // A tall, unmistakable throne silhouette, built from silver and gold.
        int seatY = FLOOR_Y + 4;
        for (int x = -3; x <= 3; x++) {
            put(level, new BlockPos(centerX + x, seatY, centerZ), gold);
            put(level, new BlockPos(centerX + x, seatY + 1, centerZ), silver);
        }
        for (int y = seatY; y <= seatY + 8; y++) {
            put(level, new BlockPos(centerX - 3, y, centerZ - 2), silver);
            put(level, new BlockPos(centerX + 3, y, centerZ - 2), silver);
            put(level, new BlockPos(centerX - 3, y, centerZ - 1), trim);
            put(level, new BlockPos(centerX + 3, y, centerZ - 1), trim);
        }
        for (int x = -3; x <= 3; x++) {
            put(level, new BlockPos(centerX + x, seatY + 8, centerZ - 2), gold);
        }
        put(level, new BlockPos(centerX, seatY + 5, centerZ - 2), light);
        put(level, new BlockPos(centerX, seatY + 9, centerZ - 2), gold);
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }
}
