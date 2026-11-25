package com.doom.godemperor;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/** Main entry point for God Emperor Doom: Rise of the God Emperor. */
@Mod(GodEmperorDoom.MOD_ID)
public final class GodEmperorDoom {
    public static final String MOD_ID = "godemperor_doom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** First in-game identity item; later milestones will add armor, abilities, and progression. */
    public static final DeferredHolder<Item, Item> DOOM_SIGIL =
            ITEMS.registerSimpleItem("doom_sigil", new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DOOM_TAB =
            CREATIVE_TABS.register("doom_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.godemperor_doom"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> DOOM_SIGIL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(DOOM_SIGIL.get()))
                    .build());

    public GodEmperorDoom(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        LOGGER.info("God Emperor Doom: Rise of the God Emperor is initializing.");
    }
}
