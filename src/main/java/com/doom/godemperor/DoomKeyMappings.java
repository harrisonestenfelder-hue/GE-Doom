
package com.doom.godemperor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = GodEmperorDoom.MOD_ID,
        @EventBusSubscriber(
        modid = GodEmperorDoom.MOD_ID,
        value = Dist.CLIENT
)
        value = Dist.CLIENT
)
public class DoomKeyMappings {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath(
                            GodEmperorDoom.MOD_ID,
                            "abilities"
                    )
            );

    public static final KeyMapping REPULSOR =
            key("repulsor", GLFW.GLFW_KEY_Z);

    public static final KeyMapping ARCANE_BOLT =
            key("arcane_bolt", GLFW.GLFW_KEY_X);

    public static final KeyMapping FORCE_FIELD =
            key("force_field", GLFW.GLFW_KEY_C);

    public static final KeyMapping DOOM_DASH =
            key("doom_dash", GLFW.GLFW_KEY_V);

    public static final KeyMapping ENERGY_DRAIN =
            key("energy_drain", GLFW.GLFW_KEY_F);

    public static final KeyMapping DOOMBOT =
            key("doombot", GLFW.GLFW_KEY_R);

    public static final KeyMapping FLIGHT =
            key("flight", GLFW.GLFW_KEY_Q);

    public static final KeyMapping TACTICAL_SCAN =
            mouse("tactical_scan", GLFW.GLFW_MOUSE_BUTTON_4);

    public static final KeyMapping TELEPORT =
            mouse("teleport", GLFW.GLFW_MOUSE_BUTTON_5);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(
                "key.godemperor_doom." + name,
                InputConstants.Type.KEYSYM,
                code,
                CATEGORY
        );
    }

    private static KeyMapping mouse(String name, int code) {
        return new KeyMapping(
                "key.godemperor_doom." + name,
                InputConstants.Type.MOUSE,
                code,
                CATEGORY
        );
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);

        event.register(REPULSOR);
        event.register(ARCANE_BOLT);
        event.register(FORCE_FIELD);
        event.register(DOOM_DASH);
        event.register(ENERGY_DRAIN);
        event.register(DOOMBOT);
        event.register(FLIGHT);
        event.register(TACTICAL_SCAN);
        event.register(TELEPORT);
    }
}
