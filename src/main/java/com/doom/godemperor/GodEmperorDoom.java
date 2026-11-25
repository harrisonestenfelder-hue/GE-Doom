
package com.doom.godemperor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import net.minecraft.server.level.ServerPlayer;

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
import java.util.function.Supplier;

@Mod(GodEmperorDoom.MOD_ID)
public class GodEmperorDoom {

    public static final String MOD_ID = "godemperor_doom";

    // ==========================================
    // REGISTRIES
    // ==========================================

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    MOD_ID
            );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    MOD_ID
            );

    // ==========================================
    // DOCTOR DOOM - DIAMOND ARMOR
    // ==========================================

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
                    ItemTags.REPAIRS_DIAMOND_ARMOR,
                    DOOM_ARMOR_ASSET
            );

    // ==========================================
    // GOD EMPEROR DOOM - NETHERITE ARMOR
    // ==========================================

    public static final ResourceKey<EquipmentAsset> GOD_EMPEROR_ARMOR_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(MOD_ID, "god_emperor")
            );

    public static final ArmorMaterial GOD_EMPEROR_ARMOR_MATERIAL =
            new ArmorMaterial(
                    37,
                    Map.of(
                            ArmorType.HELMET, 3,
                            ArmorType.CHESTPLATE, 8,
                            ArmorType.LEGGINGS, 6,
                            ArmorType.BOOTS, 3
                    ),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    3.0F,
                    0.1F,
                    ItemTags.REPAIRS_NETHERITE_ARMOR,
                    GOD_EMPEROR_ARMOR_ASSET
            );

    // ==========================================
    // DOOM SIGIL
    // ==========================================

    public static final DeferredItem<Item> DOOM_SIGIL =
            ITEMS.registerSimpleItem("doom_sigil");

    // ==========================================
    // DOCTOR DOOM ARMOR ITEMS
    // ==========================================

    public static final DeferredItem<Item> DOOM_HELMET =
            ITEMS.registerItem(
                    "doom_helmet",
                    p -> new Item(
                            p.humanoidArmor(
                                    DOOM_ARMOR_MATERIAL,
                                    ArmorType.HELMET
                            )
                    )
            );

    public static final DeferredItem<Item> DOOM_CHESTPLATE =
            ITEMS.registerItem(
                    "doom_chestplate",
                    p -> new Item(
                            p.humanoidArmor(
                                    DOOM_ARMOR_MATERIAL,
                                    ArmorType.CHESTPLATE
                            )
                    )
            );

    public static final DeferredItem<Item> DOOM_LEGGINGS =
            ITEMS.registerItem(
                    "doom_leggings",
                    p -> new Item(
                            p.humanoidArmor(
                                    DOOM_ARMOR_MATERIAL,
                                    ArmorType.LEGGINGS
                            )
                    )
            );

    public static final DeferredItem<Item> DOOM_BOOTS =
            ITEMS.registerItem(
                    "doom_boots",
                    p -> new Item(
                            p.humanoidArmor(
                                    DOOM_ARMOR_MATERIAL,
                                    ArmorType.BOOTS
                            )
                    )
            );

    // ==========================================
    // GOD EMPEROR DOOM ARMOR ITEMS
    // ==========================================

    public static final DeferredItem<Item> GOD_HELMET =
            ITEMS.registerItem(
                    "god_helmet",
                    p -> new Item(
                            p.humanoidArmor(
                                    GOD_EMPEROR_ARMOR_MATERIAL,
                                    ArmorType.HELMET
                            )
                    )
            );

    public static final DeferredItem<Item> GOD_CHESTPLATE =
            ITEMS.registerItem(
                    "god_chestplate",
                    p -> new Item(
                            p.humanoidArmor(
                                    GOD_EMPEROR_ARMOR_MATERIAL,
                                    ArmorType.CHESTPLATE
                            )
                    )
            );

    public static final DeferredItem<Item> GOD_LEGGINGS =
            ITEMS.registerItem(
                    "god_leggings",
                    p -> new Item(
                            p.humanoidArmor(
                                    GOD_EMPEROR_ARMOR_MATERIAL,
                                    ArmorType.LEGGINGS
                            )
                    )
            );

    public static final DeferredItem<Item> GOD_BOOTS =
            ITEMS.registerItem(
                    "god_boots",
                    p -> new Item(
                            p.humanoidArmor(
                                    GOD_EMPEROR_ARMOR_MATERIAL,
                                    ArmorType.BOOTS
                            )
                    )
            );

    // ==========================================
    // CREATIVE TAB
    // ==========================================

    public static final Supplier<CreativeModeTab> DOOM_TAB =
            TABS.register(
                    "godemperor_doom",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.godemperor_doom"
                                    )
                            )
                            .icon(
                                    () -> DOOM_SIGIL.get().getDefaultInstance()
                            )
                            .displayItems((parameters, output) -> {

                                output.accept(DOOM_SIGIL.get());

                                // Doctor Doom
                                output.accept(DOOM_HELMET.get());
                                output.accept(DOOM_CHESTPLATE.get());
                                output.accept(DOOM_LEGGINGS.get());
                                output.accept(DOOM_BOOTS.get());

                                // God Emperor Doom
                                output.accept(GOD_HELMET.get());
                                output.accept(GOD_CHESTPLATE.get());
                                output.accept(GOD_LEGGINGS.get());
                                output.accept(GOD_BOOTS.get());

                            })
                            .build()
            );

    // ==========================================
    // PLAYER DATA
    // ==========================================

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
                            () -> new DoomPlayerData(
                                    100,
                                    false,
                                    0,
                                    0
                            )
                    )
                            .serialize(DoomPlayerData.CODEC)
                            .copyOnDeath()
                            .build()
            );

    // ==========================================
    // MOD INITIALIZATION
    // ==========================================

    public GodEmperorDoom(IEventBus modBus) {

        ITEMS.register(modBus);
        TABS.register(modBus);
        ATTACHMENTS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    // ==========================================
    // ENERGY REGENERATION
    // ==========================================

    private void onPlayerTick(PlayerTickEvent.Post event) {

        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        DoomPlayerData data = player.getData(PLAYER_DATA);

        if (data.energy() < 100) {

            player.setData(
                    PLAYER_DATA,
                    new DoomPlayerData(
                            Math.min(100, data.energy() + 1),
                            data.awakened(),
                            data.aegisReadyAt(),
                            data.surgeReadyAt()
                    )
            );
        }
    }

    // ==========================================
    // COMMANDS
    // ==========================================

    private void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("doom")

                        // STATUS
                        .then(Commands.literal("status")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    DoomPlayerData data =
                                            player.getData(PLAYER_DATA);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Doom Energy: "
                                                            + data.energy()
                                                            + "/100 | Awakened: "
                                                            + data.awakened()
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        // AWAKEN
                        .then(Commands.literal("awaken")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    DoomPlayerData data =
                                            player.getData(PLAYER_DATA);

                                    if (data.awakened()) {

                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Doom is already awakened."
                                                )
                                        );

                                        return 0;
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

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "The power of Doom awakens."
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        // AEGIS
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

                        // SURGE
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
