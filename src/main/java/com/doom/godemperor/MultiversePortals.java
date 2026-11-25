package com.doom.godemperor;

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
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    private static final ResourceKey<Level> UNIVERSE_ONE = key("universe_one");
    private static final ResourceKey<Level> UNIVERSE_TWO = key("universe_two");
    private static final ResourceKey<Level> UNIVERSE_THREE = key("universe_three");
    private static final ResourceKey<Level> FINAL_UNIVERSE = key("final_universe");

    private MultiversePortals() {}

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(
                Registries.DIMENSION,
                Identifier.fromNamespaceAndPath(GodEmperorDoom.MOD_ID, path)
        );
    }

    /** Called on the server every player tick. Crossing a portal needs no key press. */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        animatePortals(level, player.tickCount);

        Integer until = COOLDOWNS.get(player.getUUID());
        if (until != null) {
            if (player.tickCount < until) {
                return;
            }
            COOLDOWNS.remove(player.getUUID());
        }

        if (level.dimension().equals(MultiverseHub.HUB_DIMENSION)) {
            checkHubPortal(player, level, 0.0D, -36.0D, UNIVERSE_ONE, false);
            checkHubPortal(player, level, 0.0D, 36.0D, UNIVERSE_TWO, false);
            checkHubPortal(player, level, -36.0D, 0.0D, UNIVERSE_THREE, false);
            checkHubPortal(player, level, 36.0D, 0.0D, FINAL_UNIVERSE, true);
        } else if (isUniverse(level.dimension())) {
            // The return gateway is centered at the origin of each universe.
            if (Math.abs(player.getX()) <= 3.2D
                    && Math.abs(player.getZ()) <= 1.8D
                    && player.getY() >= 8.0D
                    && player.getY() <= 20.0D) {
                returnToHub(player, level);
            }
        }
    }

    private static boolean isUniverse(ResourceKey<Level> dimension) {
        return dimension.equals(UNIVERSE_ONE)
                || dimension.equals(UNIVERSE_TWO)
                || dimension.equals(UNIVERSE_THREE)
                || dimension.equals(FINAL_UNIVERSE);
    }

    private static void checkHubPortal(
            ServerPlayer player,
            ServerLevel hub,
            double centerX,
            double centerZ,
            ResourceKey<Level> destinationKey,
            boolean locked
    ) {
        boolean inside;
        if (centerX == 0.0D) {
            inside = Math.abs(player.getX()) <= 3.2D
                    && Math.abs(player.getZ() - centerZ) <= 1.8D;
        } else {
            inside = Math.abs(player.getX() - centerX) <= 1.8D
                    && Math.abs(player.getZ()) <= 3.2D;
        }

        if (!inside || player.getY() < HUB_FLOOR + 1.0D
                || player.getY() > HUB_FLOOR + 15.0D) {
            return;
        }

        if (locked) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "THIS REALITY IS SEALED. THE ABSOLUTE STILL STANDS IN YOUR WAY."
                    ),
                    true
            );
            COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
            return;
        }

        MinecraftServer server = hub.getServer();
        ServerLevel destination = server.getLevel(destinationKey);
        if (destination == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "THIS GATEWAY HAS NOT YET STABILIZED."
                    ),
                    true
            );
            COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
            return;
        }

        buildReturnGateway(destination);
        player.teleportTo(
                destination, 0.5D, 9.0D, 8.5D,
                Set.of(), 180.0F, 0.0F, true
        );
        player.fallDistance = 0.0F;
        COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("REALITY TRANSIT"),
                true
        );
        destination.playSound(
                null, BlockPos.ZERO, SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 1.0F, 0.8F
        );
    }

    private static void returnToHub(ServerPlayer player, ServerLevel currentLevel) {
        MinecraftServer server = currentLevel.getServer();
        ServerLevel hub = server.getLevel(MultiverseHub.HUB_DIMENSION);
        if (hub == null) {
            return;
        }

        player.teleportTo(
                hub, 0.5D, HUB_FLOOR + 2.0D, 8.5D,
                Set.of(), 180.0F, 0.0F, true
        );
        player.fallDistance = 0.0F;
        COOLDOWNS.put(player.getUUID(), player.tickCount + 40);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("RETURNING TO THE MULTIVERSE HUB"),
                true
        );
        hub.playSound(
                null, new BlockPos(0, HUB_FLOOR, 8),
                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.1F
        );
    }

    private static void buildReturnGateway(ServerLevel level) {
        BlockPos marker = new BlockPos(0, 7, 0);
        if (level.getBlockState(marker).is(Blocks.EMERALD_BLOCK)) {
            return;
        }

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
        if (tick % 3 != 0) {
            return;
        }

        if (level.dimension().equals(MultiverseHub.HUB_DIMENSION)) {
            animateGate(level, 0.0D, -36.0D, true, 0x55FFAA, tick);
            animateGate(level, 0.0D, 36.0D, true, 0xFF5533, tick);
            animateGate(level, -36.0D, 0.0D, false, 0xAA66FF, tick);
            animateGate(level, 36.0D, 0.0D, false, 0xFFD700, tick);
        } else if (isUniverse(level.dimension())) {
            animateGate(level, 0.0D, 0.0D, true, 0xFFFFFF, tick);
        }
    }

    private static void animateGate(
            ServerLevel level,
            double centerX,
            double centerZ,
            boolean alongX,
            int color,
            int tick
    ) {
        int floor = level.dimension().equals(MultiverseHub.HUB_DIMENSION) ? HUB_FLOOR : 7;
        for (int i = 0; i < 9; i++) {
            double sway = Math.sin(tick * 0.11D + i * 0.7D) * 0.18D;
            double horizontal = (i - 4) * 0.72D;
            double x = alongX ? centerX + horizontal : centerX + sway;
            double z = alongX ? centerZ + sway : centerZ + horizontal;
            double y = floor + 2.0D + i;
            level.sendParticles(
                    new DustParticleOptions(color, 1.25F),
                    x, y, z, 2, 0.04D, 0.10D, 0.04D, 0.01D
            );
            level.sendParticles(
                    ParticleTypes.PORTAL,
                    x, y, z, 1, 0.08D, 0.12D, 0.08D, 0.02D
            );
        }
    }
}
