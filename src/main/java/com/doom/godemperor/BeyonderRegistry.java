package com.doom.godemperor;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BeyonderRegistry {
    private BeyonderRegistry() {}

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(GodEmperorDoom.MOD_ID);

    public static final Supplier<EntityType<BeyonderEntity>> BEYONDER =
            ENTITY_TYPES.registerEntityType(
                    "beyonder",
                    BeyonderEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.4F, 3.2F)
                            .eyeHeight(2.8F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
            );

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
