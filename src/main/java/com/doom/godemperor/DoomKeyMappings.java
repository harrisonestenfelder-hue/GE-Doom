
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

    // Default ability-slot controls

    public static final KeyMapping SLOT_1 =
            key("slot_1", GLFW.GLFW_KEY_Z);

    public static final KeyMapping SLOT_2 =
            key("slot_2", GLFW.GLFW_KEY_X);

    public static final KeyMapping SLOT_3 =
            key("slot_3", GLFW.GLFW_KEY_C);

    public static final KeyMapping SLOT_4 =
            key("slot_4", GLFW.GLFW_KEY_V);

    public static final KeyMapping SLOT_5 =
            key("slot_5", GLFW.GLFW_KEY_F);

    public static final KeyMapping SLOT_6 =
            key("slot_6", GLFW.GLFW_KEY_R);

    public static final KeyMapping SLOT_7 =
            key("slot_7", GLFW.GLFW_KEY_Q);

    public static final KeyMapping SLOT_8 =
            mouse("slot_8", GLFW.GLFW_MOUSE_BUTTON_4);

    public static final KeyMapping SLOT_9 =
            mouse("slot_9", GLFW.GLFW_MOUSE_BUTTON_5);

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

        event.register(SLOT_1);
        event.register(SLOT_2);
        event.register(SLOT_3);
        event.register(SLOT_4);
        event.register(SLOT_5);
        event.register(SLOT_6);
        event.register(SLOT_7);
        event.register(SLOT_8);
        event.register(SLOT_9);
    }
}
