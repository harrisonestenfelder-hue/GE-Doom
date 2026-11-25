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
        MinecraftServer server = player.getServer();
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
                        player.serverLevel().dimension(),
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
                FLOOR_Y + 1.0D,
                8.5D,
                Set.of(),
                180.0F,
                0.0F
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

        double x = point == null ? destination.getSharedSpawnPos().getX() + 0.5D : point.x();
        double y = point == null ? destination.getSharedSpawnPos().getY() : point.y();
        double z = point == null ? destination.getSharedSpawnPos().getZ() + 0.5D : point.z();
        float yaw = point == null ? 0.0F : point.yaw();
        float pitch = point == null ? 0.0F : point.pitch();

        player.teleportTo(destination, x, y, z, Set.of(), yaw, pitch);
        player.fallDistance = 0.0F;
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("RETURNING TO YOUR PREVIOUS REALITY"),
                true
        );
    }

    private static void buildHubIfNeeded(ServerLevel level) {
        BlockPos marker = new BlockPos(0, FLOOR_Y, 0);
        if (level.getBlockState(marker).is(Blocks.GOLD_BLOCK)) {
            return;
        }

        level.getChunkAt(marker);

        BlockState floor = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState wall = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState silver = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState silverStone = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();

        // A broad quartz floor and ceiling form the main chamber.
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                put(level, new BlockPos(x, FLOOR_Y, z), floor);
                put(level, new BlockPos(x, FLOOR_Y + 15, z), wall);

                if (Math.abs(x) == 23 || Math.abs(z) == 23) {
                    put(level, new BlockPos(x, FLOOR_Y + 1, z), gold);
                }
            }
        }

        // Four walls, each with a clear, walk-through gateway opening.
        for (int y = FLOOR_Y + 1; y < FLOOR_Y + 15; y++) {
            for (int offset = -24; offset <= 24; offset++) {
                boolean doorway = Math.abs(offset) <= 3 && y <= FLOOR_Y + 11;

                if (!doorway) {
                    put(level, new BlockPos(offset, y, -24), wall);
                    put(level, new BlockPos(offset, y, 24), wall);
                    put(level, new BlockPos(-24, y, offset), wall);
                    put(level, new BlockPos(24, y, offset), wall);

            }
        }

        // Tall silver corner columns with gold caps.
        for (int x : new int[]{-22, 22}) {
            for (int z : new int[]{-22, 22}) {
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 14; y++) {
                    put(level, new BlockPos(x, y, z), silver);
                }
                put(level, new BlockPos(x, FLOOR_Y + 14, z), gold);
            }
        }

        // Gold floor inlays lead from the central seal toward each gateway.
        for (int i = -18; i <= 18; i++) {
            put(level, new BlockPos(i, FLOOR_Y + 1, 0), gold);
            put(level, new BlockPos(0, FLOOR_Y + 1, i), gold);
        }

        // Four automatically-built silver-and-gold gateway arches.
        buildGate(level, 0, -23, true, silverStone, silver, gold);
        buildGate(level, 0, 23, true, silverStone, silver, gold);
        buildGate(level, -23, 0, false, silverStone, silver, gold);
        buildGate(level, 23, 0, false, silverStone, silver, gold);

        // Ceiling lighting and the central God Emperor seal.
        for (int x : new int[]{-12, 0, 12}) {
            for (int z : new int[]{-12, 0, 12}) {
                put(level, new BlockPos(x, FLOOR_Y + 14, z), light);
            }
        }

        put(level, marker, gold);
        put(level, new BlockPos(0, FLOOR_Y + 1, 0), Blocks.SEA_LANTERN.defaultBlockState());
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
        for (int side : new int[]{-4, 4}) {
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 11; y++) {
                BlockPos pos = alongX
                        ? new BlockPos(centerX + side, y, centerZ)
                        : new BlockPos(centerX, y, centerZ + side);
                put(level, pos, y == FLOOR_Y + 11 ? gold : stone);
            }
        }

        for (int offset = -4; offset <= 4; offset++) {
            BlockPos top = alongX
                    ? new BlockPos(centerX + offset, FLOOR_Y + 11, centerZ)
                    : new BlockPos(centerX, FLOOR_Y + 11, centerZ + offset);
            put(level, top, Math.abs(offset) == 0 ? gold : silver);
        }

        for (int offset = -2; offset <= 2; offset++) {
            BlockPos threshold = alongX
                    ? new BlockPos(centerX + offset, FLOOR_Y, centerZ)
                    : new BlockPos(centerX, FLOOR_Y, centerZ + offset);
            put(level, threshold, silver);
        }
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }
}
