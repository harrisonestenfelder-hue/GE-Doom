package com.doom.godemperor;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = GodEmperorDoom.MOD_ID)
public final class BeyonderEvents {
    private BeyonderEvents() {}

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .build();

        event.put(BeyonderRegistry.BEYONDER.get(), attributes);
    }
}
