package com.doom.godemperor;

import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BeyonderEntity extends Monster {
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    Component.literal("THE BEYONDER"),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );

    private int cosmicPulseCooldown = 100;
    private boolean defeatRewarded = false;

    public BeyonderEntity(EntityType<? extends BeyonderEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 100;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(this, Player.class, true)
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.bossEvent.setProgress(
                Math.max(0.0F, this.getHealth() / this.getMaxHealth())
        );

        // Gold energy constantly surrounds the Beyonder.
        if (this.tickCount % 5 == 0) {
            serverLevel.sendParticles(
                    new DustParticleOptions(0xFFD700, 1.5F),
                    this.getX(),
                    this.getY() + 1.5D,
                    this.getZ(),
                    5,
                    0.6D,
                    0.9D,
                    0.6D,
                    0.02D
            );
        }

        // Periodic cosmic pulse.
        if (--this.cosmicPulseCooldown <= 0) {
            this.cosmicPulse(serverLevel);
            this.cosmicPulseCooldown = 100;
        }
    }

    private void cosmicPulse(ServerLevel serverLevel) {
        serverLevel.sendParticles(
                new DustParticleOptions(0xFFD700, 2.5F),
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                90,
                4.0D,
                1.5D,
                4.0D,
                0.12D
        );

        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                25,
                0.5D,
                0.8D,
                0.5D,
                0.08D
        );

        List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                this.getBoundingBox().inflate(7.0D)
        );

        for (ServerPlayer player : nearbyPlayers) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));

            player.hurtServer(
                    serverLevel,
                    serverLevel.damageSources().magic(),
                    8.0F
            );

            player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            dx / distance * 0.9D,
                            0.45D,
                            dz / distance * 0.9D
                    )
            );
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.defeatRewarded && this.level() instanceof ServerLevel serverLevel) {
            this.defeatRewarded = true;

            List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(
                    ServerPlayer.class,
                    this.getBoundingBox().inflate(64.0D)
            );

            for (ServerPlayer player : nearbyPlayers) {
                GodEmperorDoom.rewardBeyonderDefeat(player);
                GodEmperorDoom.unlockGodEmperorStory(player);
            }
        }

        super.die(damageSource);
    }
}
