
package com.doom.godemperor;

import com.mojang.brigadier.Command;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(
        modid = GodEmperorDoom.MOD_ID,
        value = Dist.CLIENT
)
public final class DoomClientEvents {

    private DoomClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (DoomKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new DoomSkillTreeScreen());
            }
        }

        // Ability keys only activate while the player is in-game.
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (DoomKeyMappings.SLOT_1.consumeClick()) {
            DoomNetwork.requestActivate(0);
        }

        while (DoomKeyMappings.SLOT_2.consumeClick()) {
            DoomNetwork.requestActivate(1);
        }

        while (DoomKeyMappings.SLOT_3.consumeClick()) {
            DoomNetwork.requestActivate(2);
        }

        while (DoomKeyMappings.SLOT_4.consumeClick()) {
            DoomNetwork.requestActivate(3);
        }

        while (DoomKeyMappings.SLOT_5.consumeClick()) {
            DoomNetwork.requestActivate(4);
        }

        while (DoomKeyMappings.SLOT_6.consumeClick()) {
            DoomNetwork.requestActivate(5);
        }

        while (DoomKeyMappings.SLOT_7.consumeClick()) {
            DoomNetwork.requestActivate(6);
        }

        while (DoomKeyMappings.SLOT_8.consumeClick()) {
            DoomNetwork.requestActivate(7);
        }

        while (DoomKeyMappings.SLOT_9.consumeClick()) {
            DoomNetwork.requestActivate(8);
        }
    }

    @SubscribeEvent
    public static void registerClientCommands(
            RegisterClientCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("doom")
                        .then(Commands.literal("skills")
                                .executes(context -> {
                                    Minecraft.getInstance().setScreen(
                                            new DoomSkillTreeScreen()
                                    );

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }
}
