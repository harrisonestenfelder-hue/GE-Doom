
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

    // K KEY OPENS THE SKILL TREE
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        while (DoomKeyMappings.OPEN_SKILL_TREE.consumeClick()) {

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player != null &&
                    minecraft.screen == null) {

                minecraft.setScreen(
                        new DoomSkillTreeScreen()
                );
            }
        }
    }

    // /doom skills OPENS THE SAME SCREEN
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
