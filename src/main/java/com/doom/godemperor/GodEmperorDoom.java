
package com.doom.godemperor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.Commands;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Mod(GodEmperorDoom.MOD_ID)
public class GodEmperorDoom {

    public static final String MOD_ID = "godemperor_doom";

    private record AwakeningScene(double sourceX, double sourceY, double sourceZ, int elapsedTicks) {}

    private static final Map<UUID, AwakeningScene> AWAKENING_SCENES = new ConcurrentHashMap<>();
    private static final int AWAKENING_SCENE_DURATION = 100;

    // REGISTRIES

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    MOD_ID
            );

    // CUSTOM REPAIR TAGS

    public static final TagKey<Item> REPAIRS_LATVERIAN_ARMOR =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "repairs_latverian_armor"
                    )
            );

    public static final TagKey<Item> REPAIRS_BEYONDER_ARMOR =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "repairs_beyonder_armor"
                    )
            );

    // CUSTOM INGOTS

    public static final DeferredItem<Item> LATVERIAN_STEEL_INGOT =
            ITEMS.registerSimpleItem("latverian_steel_ingot");

    public static final DeferredItem<Item> BEYONDER_INGOT =
            ITEMS.registerSimpleItem("beyonder_ingot");

    // DOCTOR DOOM ARMOR

    public static final ResourceKey<EquipmentAsset> DOOM_ARMOR_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(MOD_ID, "doom")
            );

    public static final ArmorMaterial DOOM_ARMOR_MATERIAL =
            new ArmorMaterial(
                    33,
                    Map.of(
                            ArmorType.HELMET, 3,
                            ArmorType.CHESTPLATE, 8,
                            ArmorType.LEGGINGS, 6,
                            ArmorType.BOOTS, 3
                    ),
                    10,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    2.0F,
                    0.0F,
                    REPAIRS_LATVERIAN_ARMOR,
                    DOOM_ARMOR_ASSET
            );

    // GOD EMPEROR DOOM ARMOR

    public static final ResourceKey<EquipmentAsset> GOD_EMPEROR_ARMOR_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(MOD_ID, "god_emperor")
            );

    public static final ArmorMaterial GOD_EMPEROR_ARMOR_MATERIAL =
            new ArmorMaterial(
                    41,
                    Map.of(
                            ArmorType.HELMET, 4,
                            ArmorType.CHESTPLATE, 9,
                            ArmorType.LEGGINGS, 7,
                            ArmorType.BOOTS, 4
                    ),
                    20,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    3.5F,
                    0.2F,
                    REPAIRS_BEYONDER_ARMOR,
                    GOD_EMPEROR_ARMOR_ASSET
            );

    // DOOM SIGIL

    public static final DeferredItem<Item> DOOM_SIGIL =
            ITEMS.registerSimpleItem("doom_sigil");

    // DOCTOR DOOM ARMOR ITEMS

    public static final DeferredItem<Item> DOOM_HELMET =
            ITEMS.registerItem(
                    "doom_helmet",
                    p -> new Item(p.humanoidArmor(
                            DOOM_ARMOR_MATERIAL,
                            ArmorType.HELMET
                    ))
            );

    public static final DeferredItem<Item> DOOM_CHESTPLATE =
            ITEMS.registerItem(
                    "doom_chestplate",
                    p -> new Item(p.humanoidArmor(
                            DOOM_ARMOR_MATERIAL,
                            ArmorType.CHESTPLATE
                    ))
            );

    public static final DeferredItem<Item> DOOM_LEGGINGS =
            ITEMS.registerItem(
                    "doom_leggings",
                    p -> new Item(p.humanoidArmor(
                            DOOM_ARMOR_MATERIAL,
                            ArmorType.LEGGINGS
                    ))
            );

    public static final DeferredItem<Item> DOOM_BOOTS =
            ITEMS.registerItem(
                    "doom_boots",
                    p -> new Item(p.humanoidArmor(
                            DOOM_ARMOR_MATERIAL,
                            ArmorType.BOOTS
                    ))
            );

    // GOD EMPEROR DOOM ARMOR ITEMS

    public static final DeferredItem<Item> GOD_HELMET =
            ITEMS.registerItem(
                    "god_helmet",
                    p -> new Item(p.humanoidArmor(
                            GOD_EMPEROR_ARMOR_MATERIAL,
                            ArmorType.HELMET
                    ))
            );

    public static final DeferredItem<Item> GOD_CHESTPLATE =
            ITEMS.registerItem(
                    "god_chestplate",
                    p -> new Item(p.humanoidArmor(
                            GOD_EMPEROR_ARMOR_MATERIAL,
                            ArmorType.CHESTPLATE
                    ))
            );

    public static final DeferredItem<Item> GOD_LEGGINGS =
            ITEMS.registerItem(
                    "god_leggings",
                    p -> new Item(p.humanoidArmor(
                            GOD_EMPEROR_ARMOR_MATERIAL,
                            ArmorType.LEGGINGS
                    ))
            );

    public static final DeferredItem<Item> GOD_BOOTS =
            ITEMS.registerItem(
                    "god_boots",
                    p -> new Item(p.humanoidArmor(
                            GOD_EMPEROR_ARMOR_MATERIAL,
                            ArmorType.BOOTS
                    ))
            );

    // CREATIVE TAB

    public static final Supplier<CreativeModeTab> DOOM_TAB =
            TABS.register(
                    "godemperor_doom",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "itemGroup.godemperor_doom"
                            ))
                            .icon(() -> DOOM_SIGIL.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(DOOM_SIGIL.get());

                                output.accept(LATVERIAN_STEEL_INGOT.get());
                                output.accept(BEYONDER_INGOT.get());

                                output.accept(DOOM_HELMET.get());
                                output.accept(DOOM_CHESTPLATE.get());
                                output.accept(DOOM_LEGGINGS.get());
                                output.accept(DOOM_BOOTS.get());

                                output.accept(GOD_HELMET.get());
                                output.accept(GOD_CHESTPLATE.get());
                                output.accept(GOD_LEGGINGS.get());
                                output.accept(GOD_BOOTS.get());
                            })
                            .build()
            );

    // PLAYER DATA

    public record DoomPlayerData(
            int energy,
            boolean awakened,
            long aegisReadyAt,
            long surgeReadyAt
    ) {
        public static final MapCodec<DoomPlayerData> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                com.mojang.serialization.Codec.INT
                                        .fieldOf("energy")
                                        .forGetter(DoomPlayerData::energy),

                                com.mojang.serialization.Codec.BOOL
                                        .fieldOf("awakened")
                                        .forGetter(DoomPlayerData::awakened),

                                com.mojang.serialization.Codec.LONG
                                        .fieldOf("aegis_ready_at")
                                        .forGetter(DoomPlayerData::aegisReadyAt),

                                com.mojang.serialization.Codec.LONG
                                        .fieldOf("surge_ready_at")
                                        .forGetter(DoomPlayerData::surgeReadyAt)
                        ).apply(instance, DoomPlayerData::new)
                );
    }

    public static final Supplier<AttachmentType<DoomPlayerData>> PLAYER_DATA =
            ATTACHMENTS.register(
                    "player_data",
                    () -> AttachmentType.builder(
                            () -> new DoomPlayerData(100, false, 0, 0)
                    )
                            .serialize(DoomPlayerData.CODEC)
                            .copyOnDeath()
                            .build()
            );

    // INITIALIZATION

    public GodEmperorDoom(IEventBus modBus) {
        BeyonderRegistry.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);

        DoomProgression.register();
        DoomLoadout.register();
        DoomAbilities.register();

        ATTACHMENTS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    // FULL ARMOR SET CHECKS

    private boolean hasDoctorDoomSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD)
                        .is(DOOM_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST)
                        .is(DOOM_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS)
                        .is(DOOM_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET)
                        .is(DOOM_BOOTS.get());
    }

    private boolean hasGodEmperorSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD)
                        .is(GOD_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST)
                        .is(GOD_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS)
                        .is(GOD_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET)
                        .is(GOD_BOOTS.get());
    }

    // PLAYER TICK: ENERGY AND ARMOR BONUSES

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MultiversePortals.tick(player);
        tickAwakeningScene(player);

        if (player.tickCount % 20 != 0) {
            return;
        }

        DoomPlayerData data = player.getData(PLAYER_DATA);

        boolean doctorSet = hasDoctorDoomSet(player);
        boolean emperorSet = hasGodEmperorSet(player);

        int energyGain = 1;

        if (doctorSet) {
            energyGain = 2;

            player.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    45,
                    0,
                    false,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.SPEED,
                    45,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (emperorSet) {
            energyGain = 3;

            player.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    45,
                    1,
                    false,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.SPEED,
                    45,
                    1,
                    false,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    45,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (data.energy() < 100) {
            player.setData(
                    PLAYER_DATA,
                    new DoomPlayerData(
                            Math.min(100, data.energy() + energyGain),
                            data.awakened(),
                            data.aegisReadyAt(),
                            data.surgeReadyAt()
                    )
            );
        }
    }

    // BEYONDER DEFEAT: WHITE-ENERGY TRANSFER AND AWAKENING

    public static void beginGodEmperorAwakening(
            ServerPlayer player,
            double sourceX,
            double sourceY,
            double sourceZ
    ) {
        DoomPlayerData data = player.getData(PLAYER_DATA);
        if (data.awakened() || DoomProgression.get(player).godEmperorUnlocked()
                || AWAKENING_SCENES.containsKey(player.getUUID())) {
            return;
        }

        AWAKENING_SCENES.put(
                player.getUUID(),
                new AwakeningScene(sourceX, sourceY, sourceZ, 0)
        );
        rewardBeyonderDefeat(player);

        player.displayClientMessage(
                Component.literal("THE BEYONDER'S POWER IS CALLING TO YOU..."),
                false
        );
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS,
                2.0F,
                0.55F
        );
    }

    private static void tickAwakeningScene(ServerPlayer player) {
        AwakeningScene scene = AWAKENING_SCENES.get(player.getUUID());
        if (scene == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            AWAKENING_SCENES.remove(player.getUUID());
            return;
        }

        int elapsed = scene.elapsedTicks() + 1;
        double progress = Math.min(1.0D, elapsed / (double) AWAKENING_SCENE_DURATION);
        double targetX = player.getX();
        double targetY = player.getY() + 1.0D;
        double targetZ = player.getZ();

        // A growing ribbon of white particles travels from the fallen Beyonder to Doom.
        for (int point = 1; point <= 10; point++) {
            double fraction = progress * point / 10.0D;
            double x = scene.sourceX() + (targetX - scene.sourceX()) * fraction;
            double y = scene.sourceY() + (targetY - scene.sourceY()) * fraction;
            double z = scene.sourceZ() + (targetZ - scene.sourceZ()) * fraction;
            level.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 1.15F + (float) progress),
                    x, y, z, 2,
                    0.10D, 0.10D, 0.10D, 0.015D
            );
        }

        // White energy begins circling the player as the transfer intensifies.
        double orbitRadius = 0.7D + progress * 0.8D;
        for (int point = 0; point < 8; point++) {
            double angle = (elapsed * 0.22D) + Math.PI * 2.0D * point / 8.0D;
            level.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 1.3F + (float) progress),
                    targetX + Math.cos(angle) * orbitRadius,
                    player.getY() + 0.45D + (point % 3) * 0.55D,
                    targetZ + Math.sin(angle) * orbitRadius,
                    1, 0.025D, 0.04D, 0.025D, 0.01D
            );
        }

        // A rising spiral and narrow pillar make the incoming power visible from a distance.
        for (int point = 0; point < 6; point++) {
            double angle = elapsed * 0.16D + Math.PI * 2.0D * point / 6.0D;
            double height = 0.25D + point * 0.42D;
            double spiralRadius = 0.35D + progress * 0.55D;
            level.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 1.5F + (float) progress),
                    targetX + Math.cos(angle) * spiralRadius,
                    player.getY() + height,
                    targetZ + Math.sin(angle) * spiralRadius,
                    1, 0.015D, 0.035D, 0.015D, 0.005D
            );
        }

        if (elapsed == 25 || elapsed == 75) {
            level.playSound(
                    null, player.blockPosition(),
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS, 1.2F, elapsed == 25 ? 0.7F : 0.9F
            );
        }

        if (elapsed == 50) {
            player.displayClientMessage(
                    Component.literal("THE COSMIC POWER FLOWS INTO YOU..."),
                    true
            );
            level.playSound(
                    null, player.blockPosition(),
                    SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1.8F, 0.65F
            );
        }

        if (elapsed >= AWAKENING_SCENE_DURATION) {
            AWAKENING_SCENES.remove(player.getUUID());
            level.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 3.5F),
                    targetX, targetY, targetZ,
                    180, 2.0D, 2.0D, 2.0D, 0.16D
            );

            // The transfer culminates in several expanding white rings.
            for (int ring = 1; ring <= 4; ring++) {
                double radius = ring * 2.25D;
                double ringY = player.getY() + 0.15D + ring * 0.35D;
                for (int point = 0; point < 48; point++) {
                    double angle = Math.PI * 2.0D * point / 48.0D;
                    level.sendParticles(
                            new DustParticleOptions(0xFFFFFF, 2.0F + ring * 0.35F),
                            targetX + Math.cos(angle) * radius,
                            ringY,
                            targetZ + Math.sin(angle) * radius,
                            1, 0.02D, 0.04D, 0.02D, 0.0D
                    );
                }
            }

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    targetX, targetY, targetZ,
                    90, 0.7D, 1.8D, 0.7D, 0.12D
            );
            player.displayClientMessage(
                    Component.literal("THE POWER OF THE BEYONDER IS NOW YOURS."),
                    false
            );
            unlockGodEmperorStory(player);
            awakenGodEmperor(player);
        } else {
            AWAKENING_SCENES.put(
                    player.getUUID(),
                    new AwakeningScene(
                            scene.sourceX(), scene.sourceY(), scene.sourceZ(), elapsed
                    )
            );
        }
    }

    // GOD EMPEROR STORY UNLOCK

    public static void unlockGodEmperorStory(ServerPlayer player) {
        DoomProgression.unlockGodEmperor(player);

        player.displayClientMessage(
                Component.literal(
                        "THE GOD EMPEROR SKILL TREE HAS BEEN UNLOCKED."
                ),
                false
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.5F,
                0.7F
        );
    }

    // GOD EMPEROR AWAKENING

    public static boolean awakenGodEmperor(ServerPlayer player) {
        DoomProgression.Progress progress =
                DoomProgression.get(player);

        if (!progress.godEmperorUnlocked()) {
            player.displayClientMessage(
                    Component.literal(
                            "You have not yet unlocked the God Emperor power."
                    ),
                    false
            );
            return false;
        }

        DoomPlayerData data = player.getData(PLAYER_DATA);

        if (data.awakened()) {
            player.displayClientMessage(
                    Component.literal(
                            "God Emperor Doom is already awakened."
                    ),
                    false
            );
            return false;
        }

        player.setData(
                PLAYER_DATA,
                new DoomPlayerData(
                        data.energy(),
                        true,
                        data.aegisReadyAt(),
                        data.surgeReadyAt()
                )
        );

        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                120,
                1.2,
                1.5,
                1.2,
                0.12
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                35,
                0.8,
                1.0,
                0.8,
                0.08
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.5F,
                0.7F
        );

        player.displayClientMessage(
                Component.literal(
                        "GOD EMPEROR DOOM AWAKENED."
                ),
                false
        );

        // Native Minecraft title sequence delivers the transformation reveal
        // without requiring a custom client-only camera system.
        player.connection.send(
                new ClientboundSetTitlesAnimationPacket(12, 90, 24)
        );
        player.connection.send(
                new ClientboundSetTitleTextPacket(
                        Component.literal("GOD EMPEROR DOOM")
                )
        );
        player.connection.send(
                new ClientboundSetSubtitleTextPacket(
                        Component.literal("THE POWER BEYOND REALITY IS MINE")
                )
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS,
                1.4F,
                0.8F
        );

        level.sendParticles(
                new DustParticleOptions(0xFFFFFF, 2.8F),
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                100,
                1.8D,
                1.6D,
                1.8D,
                0.08D
        );

        return true;
    }

    // BEYONDER BOSS REWARD

    public static void rewardBeyonderDefeat(ServerPlayer player) {
        player.getInventory().add(
                new ItemStack(BEYONDER_INGOT.get(), 24)
        );
    }

    // COMMANDS

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("doom")

                        .then(Commands.literal("status")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    DoomPlayerData data =
                                            player.getData(PLAYER_DATA);

                                    DoomProgression.Progress progress =
                                            DoomProgression.get(player);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Doom Energy: "
                                                            + data.energy()
                                                            + "/100"
                                                            + " | Awakened: "
                                                            + data.awakened()
                                                            + " | God Emperor Unlocked: "
                                                            + progress.godEmperorUnlocked()
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("awaken")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    return awakenGodEmperor(player) ? 1 : 0;
                                })
                        )

                        .then(Commands.literal("aegis")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    DoomPlayerData data =
                                            player.getData(PLAYER_DATA);

                                    long now =
                                            player.level().getGameTime();

                                    if (data.energy() < 30) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Not enough Doom Energy."
                                                )
                                        );
                                        return 0;
                                    }

                                    if (now < data.aegisReadyAt()) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Aegis is still recharging."
                                                )
                                        );
                                        return 0;
                                    }

                                    player.addEffect(
                                            new MobEffectInstance(
                                                    MobEffects.RESISTANCE,
                                                    160,
                                                    1
                                            )
                                    );

                                    player.setData(
                                            PLAYER_DATA,
                                            new DoomPlayerData(
                                                    data.energy() - 30,
                                                    data.awakened(),
                                                    now + 300,
                                                    data.surgeReadyAt()
                                            )
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "DOOM AEGIS ACTIVATED."
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("surge")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    DoomPlayerData data =
                                            player.getData(PLAYER_DATA);

                                    long now =
                                            player.level().getGameTime();

                                    if (data.energy() < 20) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Not enough Doom Energy."
                                                )
                                        );
                                        return 0;
                                    }

                                    if (now < data.surgeReadyAt()) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Surge is still recharging."
                                                )
                                        );
                                        return 0;
                                    }

                                    player.addEffect(
                                            new MobEffectInstance(
                                                    MobEffects.SPEED,
                                                    120,
                                                    1
                                            )
                                    );

                                    player.setData(
                                            PLAYER_DATA,
                                            new DoomPlayerData(
                                                    data.energy() - 20,
                                                    data.awakened(),
                                                    data.aegisReadyAt(),
                                                    now + 200
                                            )
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "DOOM SURGE ACTIVATED."
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
