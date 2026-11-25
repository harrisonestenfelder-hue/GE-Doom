
package com.doom.godemperor;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(GodEmperorDoom.MOD_ID)
public final class GodEmperorDoom {
    public static final String MOD_ID = "godemperor_doom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public record DoomPlayerData(
            int energy,
            boolean awakened,
            long aegisReadyAt,
            long surgeReadyAt
    ) {
        public static final DoomPlayerData DEFAULT =
                new DoomPlayerData(100, false, 0L, 0L);

        public static final Codec<DoomPlayerData> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("energy").forGetter(DoomPlayerData::energy),
                        Codec.BOOL.fieldOf("awakened").forGetter(DoomPlayerData::awakened),
                        Codec.LONG.fieldOf("aegis_ready_at").forGetter(DoomPlayerData::aegisReadyAt),
                        Codec.LONG.fieldOf("surge_ready_at").forGetter(DoomPlayerData::surgeReadyAt)
                ).apply(instance, DoomPlayerData::new));
    }

    public static final Supplier<AttachmentType<DoomPlayerData>> PLAYER_DATA =
            ATTACHMENTS.register("player_data",
                    () -> AttachmentType.builder(() -> DoomPlayerData.DEFAULT)
                            .serialize(DoomPlayerData.CODEC)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<Item, Item> DOOM_SIGIL =
            ITEMS.registerSimpleItem("doom_sigil",
                    new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DOOM_TAB =
            CREATIVE_TABS.register("doom_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.godemperor_doom"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> DOOM_SIGIL.get().getDefaultInstance())
                            .displayItems((parameters, output) ->
                                    output.accept(DOOM_SIGIL.get()))
                            .build());

    public GodEmperorDoom(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ATTACHMENTS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(GodEmperorDoom::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GodEmperorDoom::registerCommands);

        LOGGER.info("God Emperor Doom systems initialized.");
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player entity = event.getEntity();

        if (!(entity instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }

        DoomPlayerData data = player.getData(PLAYER_DATA.get());

        if (data.energy() < 100) {
            player.setData(PLAYER_DATA.get(), new DoomPlayerData(
                    Math.min(100, data.energy() + 1),
                    data.awakened(),
                    data.aegisReadyAt(),
                    data.surgeReadyAt()
            ));
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("doom")
                        .then(Commands.literal("status")
                                .executes(context ->
                                        showStatus(context.getSource())))
                        .then(Commands.literal("awaken")
                                .executes(context ->
                                        awaken(context.getSource())))
                        .then(Commands.literal("aegis")
                                .executes(context ->
                                        activateAegis(context.getSource())))
            .then(Commands.literal("surge")
        .executes(context ->
                activateSurge(context.getSource()))
));
    private static int showStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DoomPlayerData data = player.getData(PLAYER_DATA.get());

        source.sendSuccess(() -> Component.literal(
                "Doom Systems | Energy: " + data.energy()
                        + "/100 | State: "
                        + (data.awakened()
                        ? "Awakened Doctor Doom" : "Unawakened")
        ), false);

        return 1;
    }

    private static int awaken(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DoomPlayerData data = player.getData(PLAYER_DATA.get());

        if (data.awakened()) {
            source.sendSuccess(() ->
                    Component.literal("Doom's power is already awakened."),
                    false);
            return 1;
        }

        player.setData(PLAYER_DATA.get(), new DoomPlayerData(
                data.energy(), true,
                data.aegisReadyAt(), data.surgeReadyAt()
        ));

        source.sendSuccess(() -> Component.literal(
                "Victor von Doom has awakened. Green-energy systems online."
        ), false);

        return 1;
    }

    private static int activateAegis(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DoomPlayerData data = player.getData(PLAYER_DATA.get());
        long now = player.level().getGameTime();

        if (!data.awakened()) {
            source.sendFailure(Component.literal(
                    "Use /doom awaken first."));
            return 0;
        }

        if (data.energy() < 30) {
            source.sendFailure(Component.literal(
                    "You need 30 Doom energy."));
            return 0;
        }

        if (now < data.aegisReadyAt()) {
            source.sendFailure(Component.literal(
                    "Aegis is recharging."));
            return 0;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, 20 * 8, 1));

        player.setData(PLAYER_DATA.get(), new DoomPlayerData(
                data.energy() - 30,
                true,
                now + 20 * 15L,
                data.surgeReadyAt()
        ));

        source.sendSuccess(() -> Component.literal(
                "GREEN AEGIS ACTIVATED!"
        ), false);

        return 1;
    }

    private static int activateSurge(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DoomPlayerData data = player.getData(PLAYER_DATA.get());
        long now = player.level().getGameTime();

        if (!data.awakened()) {
            source.sendFailure(Component.literal(
                    "Use /doom awaken first."));
            return 0;
        }

        if (data.energy() < 20) {
            source.sendFailure(Component.literal(
                    "You need 20 Doom energy."));
            return 0;
        }

        if (now < data.surgeReadyAt()) {
            source.sendFailure(Component.literal(
                    "Surge is recharging."));
            return 0;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, 20 * 6, 1));

        player.setData(PLAYER_DATA.get(), new DoomPlayerData(
                data.energy() - 20,
                true,
                data.aegisReadyAt(),
                now + 20 * 10L
        ));

        source.sendSuccess(() -> Component.literal(
                "GREEN SURGE ACTIVATED!"
        ), false);

        return 1;
    }
}
