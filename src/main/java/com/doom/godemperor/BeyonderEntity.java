package com.doom.godemperor;

import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private int phaseAttackCooldown = 180;
    private int currentPhase = 1;
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

        this.updateCombatPhase(serverLevel);

        // Gold energy constantly surrounds the Beyonder.
        if (this.tickCount % 5 == 0) {
            float auraScale = this.currentPhase == 3 ? 2.2F : this.currentPhase == 2 ? 1.8F : 1.5F;
            serverLevel.sendParticles(
                    new DustParticleOptions(0xFFD700, auraScale),
                    this.getX(),
                    this.getY() + 1.5D,
                    this.getZ(),
                    this.currentPhase == 3 ? 9 : 5,
                    0.6D,
                    0.9D,
                    0.6D,
                    0.02D
            );
        }

        if (--this.cosmicPulseCooldown <= 0) {
            this.cosmicPulse(serverLevel);
            this.cosmicPulseCooldown = this.currentPhase == 3 ? 65 : this.currentPhase == 2 ? 80 : 100;
        }

        if (this.currentPhase >= 2 && --this.phaseAttackCooldown <= 0) {
            this.cosmicNova(serverLevel);
            this.phaseAttackCooldown = this.currentPhase == 3 ? 115 : 175;
        }
    }

    private void updateCombatPhase(ServerLevel serverLevel) {
        float healthRatio = this.getHealth() / this.getMaxHealth();
        int nextPhase = healthRatio <= 0.33F ? 3 : healthRatio <= 0.66F ? 2 : 1;

        if (nextPhase == this.currentPhase) {
            return;
        }

        this.currentPhase = nextPhase;
        this.bossEvent.setColor(
                nextPhase == 3 ? BossEvent.BossBarColor.RED
                        : nextPhase == 2 ? BossEvent.BossBarColor.PURPLE
                        : BossEvent.BossBarColor.YELLOW
        );

        String announcement = nextPhase == 3
                ? "THE BEYONDER UNLEASHES HIS TRUE POWER!"
                : "THE BEYONDER'S COSMIC POWER INTENSIFIES!";

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                this.getBoundingBox().inflate(64.0D)
        )) {
            player.displayClientMessage(Component.literal(announcement), false);
        }

        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE,
                2.0F,
                nextPhase == 3 ? 0.55F : 0.8F
        );

        serverLevel.sendParticles(
                new DustParticleOptions(0xFFD700, 3.0F),
                this.getX(),
                this.getY() + 1.5D,
                this.getZ(),
                180,
                2.0D,
                2.0D,
                2.0D,
                0.16D
        );
    }

    private void cosmicPulse(ServerLevel serverLevel) {
        double radius = this.currentPhase == 3 ? 11.0D : this.currentPhase == 2 ? 9.0D : 7.0D;
        float damage = this.currentPhase == 3 ? 14.0F : this.currentPhase == 2 ? 11.0F : 8.0F;

        serverLevel.sendParticles(
                new DustParticleOptions(0xFFD700, this.currentPhase == 3 ? 3.2F : 2.5F),
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                this.currentPhase == 3 ? 150 : 90,
                radius * 0.55D,
                1.5D,
                radius * 0.55D,
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
                this.getBoundingBox().inflate(radius)
        );

        for (ServerPlayer player : nearbyPlayers) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));

            player.hurtServer(serverLevel, serverLevel.damageSources().magic(), damage);
            player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            dx / distance * (this.currentPhase == 3 ? 1.15D : 0.9D),
                            this.currentPhase == 3 ? 0.6D : 0.45D,
                            dz / distance * (this.currentPhase == 3 ? 1.15D : 0.9D)
                    )
            );
        }
    }

    private void cosmicNova(ServerLevel serverLevel) {
        double radius = this.currentPhase == 3 ? 13.0D : 10.0D;
        float damage = this.currentPhase == 3 ? 12.0F : 8.0F;

        // A short visual wind-up gives players a readable warning before the nova.
        serverLevel.sendParticles(
                new DustParticleOptions(0xFFD700, 2.8F),
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                110,
                1.5D,
                1.0D,
                1.5D,
                0.04D
        );

        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.HOSTILE,
                1.8F,
                0.65F
        );

        List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                this.getBoundingBox().inflate(radius)
        );

        for (ServerPlayer player : nearbyPlayers) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));

            player.hurtServer(serverLevel, serverLevel.damageSources().magic(), damage);
            player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            dx / distance * 1.35D,
                            0.8D,
                            dz / distance * 1.35D
                    )
            );
        }

        serverLevel.sendParticles(
                new DustParticleOptions(0xFFD700, 3.5F),
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                220,
                radius * 0.65D,
                1.4D,
                radius * 0.65D,
                0.18D
        );
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
