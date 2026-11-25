
package com.doom.godemperor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public final class DoomClientNetwork {

    private DoomClientNetwork() {}

    private static void send(CustomPacketPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getConnection() == null) {
            return;
        }

        minecraft.getConnection().send(
                new ServerboundCustomPayloadPacket(payload)
        );
    }

    public static void requestUnlock(String skillId) {
        if (skillId == null || skillId.length() > 64) {
            return;
        }

        send(new DoomNetwork.UnlockSkill(skillId));
    }

    public static void requestAssign(int slot, String skillId) {
        if (slot < 0 ||
                slot >= DoomLoadout.SLOT_COUNT ||
                skillId == null ||
                skillId.length() > 64) {
            return;
        }

        send(new DoomNetwork.AssignSkill(slot, skillId));
    }

    public static void requestActivate(int slot) {
        if (slot < 0 || slot >= DoomLoadout.SLOT_COUNT) {
            return;
        }

        send(new DoomNetwork.ActivateAbility(slot));
    }

    public static void sendOpenMultiverseHub() {
        send(new DoomNetwork.OpenMultiverseHub());
    }
}
