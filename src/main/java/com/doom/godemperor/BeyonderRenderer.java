package com.doom.godemperor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class BeyonderRenderer extends HumanoidMobRenderer<
        BeyonderEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>
        > {

    public BeyonderRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                1.1F
        );
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return Identifier.fromNamespaceAndPath(
                "minecraft",
                "textures/entity/zombie/zombie.png"
        );
    }
}
