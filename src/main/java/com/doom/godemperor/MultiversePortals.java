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
            level.setBlock(new BlockPos(x, floorY, 8), trim, 3);
        }
        level.setBlock(marker, Blocks.BEACON.defaultBlockState(), 3);
        return floorY;
    }

    private static void buildArrivalLandmark(ServerLevel level) {
        // Generate the fortress chunk first; otherwise an ungenerated chunk can report Y=0
        // and place the entire stronghold beneath the actual world.
        level.getChunk(1, 1);
        int baseY = Math.max(level.getMinY() + 8, level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                24, 24) - 1);
        BlockPos marker = new BlockPos(24, baseY + 26, 24);
        if (level.getBlockState(marker).is(Blocks.BEACON)) {
            return;
        }

        // Remove the previous underground fortress version without carving out natural terrain.
        for (int oldY = 8; oldY <= Math.min(80, baseY + 25); oldY++) {
            for (int x = 3; x <= 45; x++) {
                for (int z = 3; z <= 45; z++) {
                    BlockPos oldPos = new BlockPos(x, oldY, z);
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

        // Level a large fortress footprint and clear enough vertical space for its towers.
        for (int x = 3; x <= 45; x++) {
            for (int z = 3; z <= 45; z++) {
                level.setBlock(new BlockPos(x, baseY, z), foundation, 3);
                for (int y = baseY + 1; y <= baseY + 27; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Outer fortress walls with a guarded main gate.
        for (int y = baseY + 1; y <= baseY + 10; y++) {
            for (int p = 7; p <= 41; p++) {
                if (p < 21 || p > 27) {
                    level.setBlock(new BlockPos(p, y, 7), wall, 3);
                }
                level.setBlock(new BlockPos(p, y, 41), wall, 3);
                level.setBlock(new BlockPos(7, y, p), wall, 3);
                level.setBlock(new BlockPos(41, y, p), wall, 3);
            }
        }
        for (int p = 7; p <= 41; p++) {
            if (p < 21 || p > 27) {
                level.setBlock(new BlockPos(p, baseY + 11, 7), trim, 3);
            }
            level.setBlock(new BlockPos(p, baseY + 11, 41), trim, 3);
            level.setBlock(new BlockPos(7, baseY + 11, p), trim, 3);
            level.setBlock(new BlockPos(41, baseY + 11, p), trim, 3);
        }

        // Four corner towers, each with a lit crown and battlements.
        for (int cx : new int[]{7, 41}) {
            for (int cz : new int[]{7, 41}) {
                for (int y = baseY + 1; y <= baseY + 19; y++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                                level.setBlock(new BlockPos(cx + dx, y, cz + dz), wall, 3);
                            }
                        }
                    }
                }
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        if (Math.abs(dx) == 4 || Math.abs(dz) == 4) {
                            level.setBlock(new BlockPos(cx + dx, baseY + 20, cz + dz), trim, 3);
                        }
                    }
                }
                level.setBlock(new BlockPos(cx, baseY + 19, cz), roof, 3);
            }
        }

        // Central keep and its high command tower.
        for (int y = baseY + 1; y <= baseY + 16; y++) {
            for (int p = 17; p <= 31; p++) {
                level.setBlock(new BlockPos(p, y, 17), wall, 3);
                level.setBlock(new BlockPos(p, y, 31), wall, 3);
                level.setBlock(new BlockPos(17, y, p), wall, 3);
                level.setBlock(new BlockPos(31, y, p), wall, 3);
            }
        }
        for (int x = 20; x <= 28; x++) {
            for (int z = 20; z <= 28; z++) {
                level.setBlock(new BlockPos(x, baseY + 1, z), trim, 3);
            }
        }
        for (int y = baseY + 17; y <= baseY + 24; y++) {
            for (int p = 20; p <= 28; p++) {
                level.setBlock(new BlockPos(p, y, 20), wall, 3);
                level.setBlock(new BlockPos(p, y, 28), wall, 3);
                level.setBlock(new BlockPos(20, y, p), wall, 3);
                level.setBlock(new BlockPos(28, y, p), wall, 3);
            }
        }
        for (int p = 19; p <= 29; p++) {
            level.setBlock(new BlockPos(p, baseY + 25, 19), trim, 3);
            level.setBlock(new BlockPos(p, baseY + 25, 29), trim, 3);
            level.setBlock(new BlockPos(19, baseY + 25, p), trim, 3);
            level.setBlock(new BlockPos(29, baseY + 25, p), trim, 3);
        }
        level.setBlock(marker, Blocks.BEACON.defaultBlockState(), 3);

        // Realm champions and guards defend the stronghold.
        spawnDefender(level, EntityType.VINDICATOR, "Realm Champion", 24, 13,
                new ItemStack(Items.DIAMOND_AXE));
        spawnDefender(level, EntityType.VINDICATOR, "Royal Sentinel", 13, 24,
                new ItemStack(Items.IRON_AXE));
        spawnDefender(level, EntityType.VINDICATOR, "Royal Sentinel", 35, 24,
                new ItemStack(Items.IRON_AXE));
        spawnDefender(level, EntityType.PILLAGER, "Realm Marksman", 24, 36,
                new ItemStack(Items.CROSSBOW));
        spawnDefender(level, EntityType.PILLAGER, "Realm Marksman", 36, 36,
                new ItemStack(Items.CROSSBOW));
        spawnDefender(level, EntityType.EVOKER, "Realm Arcanist", 12, 36, ItemStack.EMPTY);
    }

    private static void spawnDefender(
            ServerLevel level, EntityType<? extends Mob> type, String name,
            int x, int z, ItemStack weapon
    ) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) {
            return;
        }
        int y = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        mob.setPos(x + 0.5D, y, z + 0.5D);
        mob.setCustomName(net.minecraft.network.chat.Component.literal(name));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        if (!weapon.isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
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
