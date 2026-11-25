
package com.doom.godemperor;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DoomNetwork {

    private DoomNetwork() {}

    public record UnlockSkill(String skillId)
            implements CustomPacketPayload {

        public static final Type<UnlockSkill> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(
                        GodEmperorDoom.MOD_ID,
                        "unlock_skill"
                ));

        public static final StreamCodec<ByteBuf, UnlockSkill> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        UnlockSkill::skillId,
                        UnlockSkill::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AssignSkill(int slot, String skillId)
            implements CustomPacketPayload {

        public static final Type<AssignSkill> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(
                        GodEmperorDoom.MOD_ID,
                        "assign_skill"
                ));

        public static final StreamCodec<ByteBuf, AssignSkill> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        AssignSkill::slot,
                        ByteBufCodecs.STRING_UTF8,
                        AssignSkill::skillId,
                        AssignSkill::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = GodEmperorDoom.MOD_ID)
    public static class Registration {

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {

            var registrar = event.registrar("1");

            registrar.playToServer(
                    UnlockSkill.TYPE,
                    UnlockSkill.STREAM_CODEC,
                    DoomNetwork::handleUnlock
            );

            registrar.playToServer(
                    AssignSkill.TYPE,
                    AssignSkill.STREAM_CODEC,
                    DoomNetwork::handleAssign
            );
        }
    }

    private static void handleUnlock(
            UnlockSkill packet,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (packet.skillId() == null ||
                packet.skillId().length() > 64) {
            return;
        }

        DoomProgression.unlockSkill(player, packet.skillId());
    }

    private static void handleAssign(
            AssignSkill packet,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (packet.skillId() == null ||
                packet.skillId().length() > 64) {
            return;
        }

        DoomLoadout.assign(
                player,
                packet.slot(),
                packet.skillId()
        );
    }

    public static void requestUnlock(String skillId) {
        ClientPacketDistributor.sendToServer(
                new UnlockSkill(skillId)
        );
    }

    public static void requestAssign(int slot, String skillId) {
        ClientPacketDistributor.sendToServer(
                new AssignSkill(slot, skillId)
        );
    }
}
