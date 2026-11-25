package com.doom.godemperor;

import java.util.List;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.Heightmap;

public class BeyonderEntity extends Monster {
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    Component.literal("THE BEYONDER"),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );

    private int cosmicPulseCooldown = 100;
    private int phaseAttackCooldown = 180;
    private int teleportCooldown = 260;
    private int barrageCooldown = 210;
    private int gravityCooldown = 240;
    private int barrageWindup = 0;
    private int gravityDuration = 0;
    private double barrageX;
    private double barrageY;
    private double barrageZ;
    private int currentPhase = 1;
    private boolean enraged = false;
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
        this.updateEnrage(serverLevel);

        if (this.tickCount % 5 == 0) {
            float auraScale = this.currentPhase == 3 ? 2.2F : this.currentPhase == 2 ? 1.8F : 1.5F;
            serverLevel.sendParticles(
                    new DustParticleOptions(0xFFD700, auraScale),
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    this.currentPhase == 3 ? 9 : 5,
                    0.6D, 0.9D, 0.6D, 0.02D
            );
        }

        if (--this.cosmicPulseCooldown <= 0) {
            this.cosmicPulse(serverLevel);
            this.cosmicPulseCooldown = this.enraged ? 45
                    : this.currentPhase == 3 ? 65 : this.currentPhase == 2 ? 80 : 100;
        }

        if (this.currentPhase >= 2 && --this.phaseAttackCooldown <= 0) {
            this.cosmicNova(serverLevel);
            this.phaseAttackCooldown = this.enraged ? 75 : this.currentPhase == 3 ? 115 : 175;
        }

        if (this.currentPhase >= 2) {
            this.updateAdvancedAttacks(serverLevel);
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
                ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
            player.displayClientMessage(Component.literal(announcement), false);
        }

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 2.0F, nextPhase == 3 ? 0.55F : 0.8F);

        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 3.0F),
                this.getX(), this.getY() + 1.5D, this.getZ(),
                180, 2.0D, 2.0D, 2.0D, 0.16D);
    }

    private void updateEnrage(ServerLevel serverLevel) {
        if (this.enraged || this.getHealth() / this.getMaxHealth() > 0.10F) {
            return;
        }

        this.enraged = true;
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.42D);
        }

        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
            player.displayClientMessage(
                    Component.literal("THE BEYONDER IS ENRAGED!"),
                    false
            );
        }

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.0F, 0.8F);
        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 4.0F),
                this.getX(), this.getY() + 1.5D, this.getZ(),
                240, 2.5D, 2.0D, 2.5D, 0.2D);
    }

    private void updateAdvancedAttacks(ServerLevel serverLevel) {
        Player target = this.getTarget() instanceof Player player ? player : null;

        if (this.barrageWindup > 0) {
            this.barrageWindup--;
            serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 2.0F),
                    this.barrageX, this.barrageY + 0.2D, this.barrageZ,
                    14, 0.8D, 0.15D, 0.8D, 0.02D);
            if (this.barrageWindup == 0) {
                this.goldenBarrageImpact(serverLevel);
            }
        }

        if (this.gravityDuration > 0) {
            this.gravityDuration--;
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                    ServerPlayer.class, this.getBoundingBox().inflate(14.0D))) {
                double dx = this.getX() - player.getX();
                double dy = this.getY() + 1.0D - player.getY();
                double dz = this.getZ() - player.getZ();
                double distance = Math.max(1.0D, Math.sqrt(dx * dx + dy * dy + dz * dz));
                player.setDeltaMovement(player.getDeltaMovement().add(
                        dx / distance * 0.10D,
                        dy / distance * 0.035D,
                        dz / distance * 0.10D
                ));
            }
            if (this.gravityDuration == 0) {
                this.gravityCrushRelease(serverLevel);
            }
        }

        if (target == null || !target.isAlive()) {
            return;
        }

        if (--this.teleportCooldown <= 0) {
            this.cosmicTeleport(serverLevel, target);
            this.teleportCooldown = this.enraged ? 95 : this.currentPhase == 3 ? 145 : 210;
        }

        if (this.barrageWindup == 0 && --this.barrageCooldown <= 0) {
            this.barrageX = target.getX();
            this.barrageY = target.getY();
            this.barrageZ = target.getZ();
            this.barrageWindup = 24;
            this.barrageCooldown = this.enraged ? 100 : 165;
            target.displayClientMessage(Component.literal("THE BEYONDER MARKS YOUR POSITION!"), true);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 1.5F, 1.2F);
        }

        if (this.gravityDuration == 0 && --this.gravityCooldown <= 0) {
            this.gravityDuration = 35;
            this.gravityCooldown = this.enraged ? 125 : 205;
            serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 3.0F),
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    100, 3.0D, 1.5D, 3.0D, 0.08D);
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                    ServerPlayer.class, this.getBoundingBox().inflate(14.0D))) {
                player.displayClientMessage(Component.literal("GRAVITY CRUSH!"), true);
            }
        }
    }

    private void cosmicTeleport(ServerLevel serverLevel, Player target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int offsetX = this.random.nextInt(13) - 6;
            int offsetZ = this.random.nextInt(13) - 6;
            if (Math.abs(offsetX) + Math.abs(offsetZ) < 5) {
                continue;
            }

            int x = (int) Math.floor(target.getX()) + offsetX;
            int z = (int) Math.floor(target.getZ()) + offsetZ;
            int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            double newX = x + 0.5D;
            double newY = y;
            double newZ = z + 0.5D;

            if (serverLevel.noCollision(this,
                    this.getBoundingBox().move(newX - this.getX(), newY - this.getY(), newZ - this.getZ()))) {
                this.setPos(newX, newY, newZ);
                this.getNavigation().stop();
                serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 2.8F),
                        newX, newY + 1.0D, newZ, 75, 1.0D, 1.2D, 1.0D, 0.12D);
                serverLevel.playSound(null, BlockPos.containing(newX, newY, newZ),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.7F);
                return;
            }
        }
    }

    private void goldenBarrageImpact(ServerLevel serverLevel) {
        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 3.2F),
                this.barrageX, this.barrageY + 0.5D, this.barrageZ,
                95, 1.5D, 0.8D, 1.5D, 0.14D);

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                new net.minecraft.world.phys.AABB(
                        this.barrageX - 2.5D, this.barrageY - 1.0D, this.barrageZ - 2.5D,
                        this.barrageX + 2.5D, this.barrageY + 3.0D, this.barrageZ + 2.5D))) {
            player.hurtServer(serverLevel, serverLevel.damageSources().magic(),
                    this.enraged ? 13.0F : 9.0F);
            player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.65D, 0.0D));
        }
    }

    private void gravityCrushRelease(ServerLevel serverLevel) {
        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 3.8F),
                this.getX(), this.getY() + 1.0D, this.getZ(),
                180, 5.0D, 2.0D, 5.0D, 0.18D);

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class, this.getBoundingBox().inflate(14.0D))) {
            player.hurtServer(serverLevel, serverLevel.damageSources().magic(),
                    this.enraged ? 15.0F : 10.0F);
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            player.setDeltaMovement(player.getDeltaMovement().add(
                    dx / distance * 1.1D, 0.8D, dz / distance * 1.1D));
        }
    }

    private void cosmicPulse(ServerLevel serverLevel) {
        double radius = this.currentPhase == 3 ? 11.0D : this.currentPhase == 2 ? 9.0D : 7.0D;
        float damage = this.enraged ? 17.0F
                : this.currentPhase == 3 ? 14.0F : this.currentPhase == 2 ? 11.0F : 8.0F;

        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, this.currentPhase == 3 ? 3.2F : 2.5F),
                this.getX(), this.getY() + 1.0D, this.getZ(),
                this.currentPhase == 3 ? 150 : 90,
                radius * 0.55D, 1.5D, radius * 0.55D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                25, 0.5D, 0.8D, 0.5D, 0.08D);

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class, this.getBoundingBox().inflate(radius))) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            player.hurtServer(serverLevel, serverLevel.damageSources().magic(), damage);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    dx / distance * (this.currentPhase == 3 ? 1.15D : 0.9D),
                    this.currentPhase == 3 ? 0.6D : 0.45D,
                    dz / distance * (this.currentPhase == 3 ? 1.15D : 0.9D)));
        }
    }

    private void cosmicNova(ServerLevel serverLevel) {
        double radius = this.currentPhase == 3 ? 13.0D : 10.0D;
        float damage = this.enraged ? 16.0F : this.currentPhase == 3 ? 12.0F : 8.0F;

        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 2.8F),
                this.getX(), this.getY() + 1.0D, this.getZ(),
                110, 1.5D, 1.0D, 1.5D, 0.04D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.HOSTILE, 1.8F, 0.65F);

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class, this.getBoundingBox().inflate(radius))) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            player.hurtServer(serverLevel, serverLevel.damageSources().magic(), damage);
            player.setDeltaMovement(player.getDeltaMovement().add(
                    dx / distance * 1.35D, 0.8D, dz / distance * 1.35D));
        }

        serverLevel.sendParticles(new DustParticleOptions(0xFFD700, 3.5F),
                this.getX(), this.getY() + 1.0D, this.getZ(),
                220, radius * 0.65D, 1.4D, radius * 0.65D, 0.18D);
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
                    ServerPlayer.class, this.getBoundingBox().inflate(64.0D));
            for (ServerPlayer player : nearbyPlayers) {
                GodEmperorDoom.rewardBeyonderDefeat(player);
                GodEmperorDoom.unlockGodEmperorStory(player);
            }
        }
        super.die(damageSource);
    }
}
