package com.ultimatesurvival.mob;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.mob.attack.Attack;
import com.ultimatesurvival.mob.attack.Attacks;
import com.ultimatesurvival.mob.attack.Styles;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A progression boss: a boss bar, HP-threshold phases with an invulnerable transition, a ritual spawn animation,
 * a long death sequence, and the rules that keep the fight honest - it never despawns, cannot be suffocated,
 * drowned or dropped, resets when abandoned, and teleports to a target it cannot reach.
 */
public class UsmBoss extends UsmMob {
	private final ServerBossEvent bar;
	private Vec3 home;
	private int combatTicks;
	private int idleTicks;
	private int spawnLeft;
	private int transition;
	private int stuckTicks;
	private Vec3 lastSample = Vec3.ZERO;
	private int inWallTicks;

	public UsmBoss(EntityType<? extends UsmBoss> type, Level level) {
		super(type, level);
		this.bar = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(), color(def.barColor), overlay(def.barOverlay));
		this.bar.setPlayBossMusic(false);
		this.setHealth(this.getMaxHealth());
	}

	private static BossEvent.BossBarColor color(String s) {
		return switch (s) {
			case "pink" -> BossEvent.BossBarColor.PINK;
			case "blue" -> BossEvent.BossBarColor.BLUE;
			case "green" -> BossEvent.BossBarColor.GREEN;
			case "yellow" -> BossEvent.BossBarColor.YELLOW;
			case "purple" -> BossEvent.BossBarColor.PURPLE;
			case "white" -> BossEvent.BossBarColor.WHITE;
			default -> BossEvent.BossBarColor.RED;
		};
	}

	private static BossEvent.BossBarOverlay overlay(String s) {
		return switch (s) {
			case "notched_6" -> BossEvent.BossBarOverlay.NOTCHED_6;
			case "notched_10" -> BossEvent.BossBarOverlay.NOTCHED_10;
			case "notched_12" -> BossEvent.BossBarOverlay.NOTCHED_12;
			case "notched_20" -> BossEvent.BossBarOverlay.NOTCHED_20;
			default -> BossEvent.BossBarOverlay.PROGRESS;
		};
	}

	// ---- ritual spawn ---------------------------------------------------------------------------------------------------------
	/** Called by the ritual right after the boss is added to the world. */
	public void beginSpawn() {
		spawnLeft = Math.max(1, def.spawnTicks);
		setFlag(F_SPAWNING, true);
		startAnim("spawn", spawnLeft);
		home = position();
		bar.setProgress(0f);
		refreshSpeed();
	}

	public ServerBossEvent bossBar() {
		return bar;
	}

	public boolean isSpawning() {
		return spawnLeft > 0;
	}

	// ---- server tick ---------------------------------------------------------------------------------------------------------------
	@Override
	protected void customServerAiStep(ServerLevel level) {
		if (home == null) {
			home = position();
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL && !UsmConfig.get().bossesOnPeaceful) {
			discard();
			return;
		}
		if (spawnLeft > 0) {
			tickSpawn(level);
			return;
		}
		super.customServerAiStep(level);
		if (isDeadOrDying()) {
			return;
		}
		if (transition > 0 && --transition == 0) {
			setFlag(F_TRANSITION, false);
		}
		bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0f, 1f));
		if (nodesWereUp && !hasNodes()) {                         // the last pylon fell: the shield shatters and the boss is staggered
			nodesWereUp = false;
			setFlag(F_SHIELD, false);
			setVuln(100, 1.5);
			Styles.burst(level, position().add(0, getBbHeight() * 0.5, 0), def.style, getBbWidth(), 80);
			Fx.sound(level, position(), SoundEvents.SHIELD_BREAK.value(), SoundSource.HOSTILE, 2.0f, 0.6f);
			announce(Component.translatable("ultimatesurvival.boss.unshielded", getName()).withStyle(ChatFormatting.GREEN));
		}
		checkPhase(level);
		tickCombat(level);
		safety(level);
		if (tickCount % 20 == 7) {
			BossMusic.tick(this, level);
		}
	}

	private void tickSpawn(ServerLevel level) {
		int total = Math.max(1, def.spawnTicks);
		spawnLeft--;
		bar.setProgress(1f - (float) spawnLeft / total);
		getNavigation().stop();
		if (spawnLeft % 3 == 0) {
			Fx.burst(level, position().add(0, 0.2, 0), ParticleTypes.LARGE_SMOKE, 6, getBbWidth() * 0.6, 0.05);
			Styles.fill(level, position(), getBbWidth() * 1.4, def.style, 8);
		}
		if (spawnLeft == total - 2) {
			Fx.sound(level, position(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.6f);
		}
		if (spawnLeft % 12 == 0) {
			Shake.at(level, position(), 40, 0.35f, 8);
		}
		if (spawnLeft <= 0) {
			setFlag(F_SPAWNING, false);
			clearAnim();
			bar.setProgress(1f);
			Fx.sound(level, position(), Styles.roar(def.style), SoundSource.HOSTILE, 2.0f, 1.0f);
			Shake.at(level, position(), 40, 0.8f, 16);
			ScreenFlash.at(level, position(), 50, Styles.color(def.style), 0.45f, 16);
			refreshBar();
			attackCd = 40;
		}
	}

	// ---- phases ---------------------------------------------------------------------------------------------------------------------------
	private void checkPhase(ServerLevel level) {
		int cur = phaseIndex();
		if (cur + 1 < def.phases.size() && getHealth() <= getMaxHealth() * def.phases.get(cur + 1).at() + 1e-3 && transition == 0
				&& (run == null || run.attack != roarAttack)) {
			enterPhase(level, cur + 1);
		}
	}

	private Attack roarAttack;
	private final java.util.List<UsmMob> nodes = new java.util.ArrayList<>();
	private boolean nodesWereUp;

	@Override
	public boolean hasNodes() {
		nodes.removeIf(n -> n.isRemoved() || !n.isAlive());
		return !nodes.isEmpty();
	}

	/** Shield pylons: the boss is immune until every one of them has been destroyed. */
	private void spawnNodes(ServerLevel level, com.google.gson.JsonObject spec) {
		String id = spec.get("mob").getAsString();
		int n = spec.has("count") ? spec.get("count").getAsInt() : 3;
		double radius = spec.has("radius") ? spec.get("radius").getAsDouble() : 9;
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n + random.nextDouble() * 0.5;
			Vec3 at = Attacks.ground(level, position().add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
			UsmMob node = spawnMinion(id, at);
			if (node != null) {
				nodes.add(node);
			}
		}
		if (!nodes.isEmpty()) {
			nodesWereUp = true;
			setFlag(F_SHIELD, true);
			announce(Component.translatable("ultimatesurvival.boss.shielded", getName(), nodes.size()).withStyle(ChatFormatting.AQUA));
		}
	}

	private void enterPhase(ServerLevel level, int idx) {
		cancelRun();
		queued.clear();
		hazards.clear();
		clearVuln();
		setHot(false, 1.0, 1.0);
		entityData.set(PHASE, idx);
		MobDef.Phase ph = def.phases.get(idx);
		ScreenFlash.at(level, position(), 50, Styles.color(def.style), 0.35f, 14);
		refreshSpeed();
		refreshBar();
		heal((float) (getMaxHealth() * 0.04));
		MobDef.AttackSpec spec = new MobDef.AttackSpec("roar", 1, new com.ultimatesurvival.content.AbilityRef("roar", roarParams()));
		roarAttack = Attacks.create(spec);
		transition = 70;
		setFlag(F_TRANSITION, true);
		startRun(roarAttack, getTarget());
		for (MobDef.AttackSpec s : ph.enter()) {
			queued.add(attackFor(s));
		}
		if (ph.nodes() != null) {
			spawnNodes(level, ph.nodes());
		}
		announce(Component.translatable("ultimatesurvival.boss.phase", getName(), Component.literal(ph.name())).withStyle(ChatFormatting.GOLD));
		UltimateSurvival.LOGGER.info("{} enters phase {} ({})", def.id, idx + 1, ph.name());
	}

	private com.google.gson.JsonObject roarParams() {
		com.google.gson.JsonObject o = new com.google.gson.JsonObject();
		o.addProperty("type", "roar");
		o.addProperty("style", def.style);
		o.addProperty("dmg", 0.5);
		o.addProperty("radius", 12);
		o.addProperty("length", 60);
		return o;
	}

	private void refreshBar() {
		MobDef.Phase ph = phase();
		bar.setName(Component.empty().append(getDisplayName()).append(Component.literal("  " + roman(phaseIndex() + 1)).withStyle(ChatFormatting.GRAY)));
		bar.setColor(color(ph.color() != null ? ph.color() : def.barColor));
	}

	private static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			default -> "V";
		};
	}

	private void announce(Component msg) {
		if (level() instanceof ServerLevel sl) {
			for (ServerPlayer p : sl.players()) {
				if (p.distanceToSqr(this) < 96 * 96) {
					p.sendOverlayMessage(msg);
				}
			}
		}
	}

	/** A hit may never carry the boss past the next phase threshold: it stops there and the transition plays. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if ((spawnLeft > 0 || transition > 0) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {      // /kill and creative still work
			return false;
		}
		boolean hit = super.hurtServer(level, source, amount);
		int cur = phaseIndex();
		if (hit && cur + 1 < def.phases.size() && isAlive()) {
			float floor = (float) (getMaxHealth() * def.phases.get(cur + 1).at());
			if (getHealth() < floor) {
				setHealth(floor);
			}
		}
		return hit;
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.DROWN)
				|| source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.CACTUS)
				|| source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.FALLING_BLOCK)
				|| source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FREEZE)) {
			return true;
		}
		return super.isInvulnerableTo(level, source);
	}

	@Override
	public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	public void checkDespawn() {
		this.noActionTime = 0;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	// ---- combat bookkeeping, enrage, reset -----------------------------------------------------------------------------------------------------
	private void tickCombat(ServerLevel level) {
		LivingEntity t = getTarget();
		if (t != null && t.isAlive()) {
			combatTicks++;
			idleTicks = 0;
			if (def.enrageSeconds > 0 && !hasFlag(F_ENRAGED) && combatTicks > def.enrageSeconds * 20) {
				setFlag(F_ENRAGED, true);
				refreshSpeed();
				announce(Component.translatable("ultimatesurvival.boss.enraged", getName()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
				Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0f, 0.5f);
			}
		} else {
			idleTicks++;
			if (idleTicks > 200) {
				combatTicks = 0;
				if (hasFlag(F_ENRAGED)) {
					setFlag(F_ENRAGED, false);
					refreshSpeed();
				}
				if (getHealth() < getMaxHealth() && tickCount % 4 == 0) {
					heal((float) (getMaxHealth() * 0.01));
				}
				if (home != null && position().distanceToSqr(home) > 16 && !busy()) {
					getNavigation().moveTo(home.x, home.y, home.z, 1.0);
				}
			}
		}
	}

	/** Anti-cheese: stuck in blocks, fallen out of the world, dragged far from home, or unable to reach the target. */
	private void safety(ServerLevel level) {
		if (isInWall()) {
			if (++inWallTicks > 20) {
				teleportTo(level, home != null ? home : position().add(0, 3, 0));
				inWallTicks = 0;
			}
		} else {
			inWallTicks = 0;
		}
		if (getY() < level.getMinY() - 8 || (home != null && position().distanceToSqr(home) > UsmConfig.get().bossLeashRange * UsmConfig.get().bossLeashRange)) {
			teleportTo(level, home != null ? home : position());
			setTarget(null);
		}
		LivingEntity t = getTarget();
		if (t != null && !busy() && !def.fly && tickCount % 20 == 0) {
			Vec3 now = position();
			boolean slowMover = now.distanceToSqr(lastSample) < 0.25;
			lastSample = now;
			if (slowMover && distanceToSqr(t) > 9 * 9) {
				stuckTicks += 20;
			} else {
				stuckTicks = 0;
			}
			if (stuckTicks >= 60) {
				stuckTicks = 0;
				teleportNear(level, t);
			}
		}
	}

	public void teleportTo(ServerLevel level, Vec3 to) {
		Styles.burst(level, position().add(0, getBbHeight() * 0.5, 0), def.style, 1.2, 40);
		snapTo(to.x, to.y, to.z, getYRot(), getXRot());
		getNavigation().stop();
		Styles.burst(level, to.add(0, getBbHeight() * 0.5, 0), def.style, 1.2, 40);
		Fx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.5f);
	}

	private void teleportNear(ServerLevel level, LivingEntity t) {
		double a = random.nextDouble() * Math.PI * 2;
		Vec3 want = Attacks.ground(level, t.position().add(Math.cos(a) * 5.0, 0, Math.sin(a) * 5.0));
		BlockPos bp = BlockPos.containing(want);
		if (level.noCollision(this, getBoundingBox().move(want.subtract(position())))) {
			teleportTo(level, want);
		} else {
			teleportTo(level, t.position().add(0, 0.5, 0));
		}
		UltimateSurvival.LOGGER.debug("{} could not reach its target and teleported to {}", def.id, bp);
	}

	// ---- seen by players: boss bar --------------------------------------------------------------------------------------------------------------------
	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bar.removePlayer(player);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		bar.setName(getDisplayName());
	}

	// ---- death -----------------------------------------------------------------------------------------------------------------------------------------
	@Override
	public void die(DamageSource source) {
		cancelRun();
		hazards.clear();
		killMinions();
		if (level() instanceof ServerLevel sl0) {
			BossMusic.end(this, sl0);
		}
		setFlag(F_DYING, true);
		setFlag(F_VULN, false);
		setFlag(F_HOT, false);
		bar.setProgress(0f);
		super.die(source);
		if (level() instanceof ServerLevel sl) {
			Fx.sound(sl, position(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0f, 0.55f);
			Shake.at(sl, position(), 60, 1.0f, 30);
			ScreenFlash.at(sl, position(), 60, 0xFFFFFF, 0.6f, 24);
		}
	}

	private void killMinions() {
		for (UsmMob m : new java.util.ArrayList<>(minions)) {
			if (m.isAlive() && level() instanceof ServerLevel sl) {
				Fx.burst(sl, m.position().add(0, 0.5, 0), ParticleTypes.POOF, 14, 0.3, 0.05);
				m.discard();
			}
		}
		minions.clear();
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (deathTime == 1) {
			startAnim("dead", def.deathTicks);
		}
		if (level() instanceof ServerLevel sl && !isRemoved()) {
			float f = (float) deathTime / def.deathTicks;
			if (deathTime % 2 == 0) {
				Fx.burst(sl, position().add(0, getBbHeight() * (1.0 - f * 0.6) * random.nextDouble(), 0), Styles.accent(def.style), 5, getBbWidth() * 0.5, 0.08);
				Fx.burst(sl, position().add(0, getBbHeight() * random.nextDouble() * 0.8, 0), ParticleTypes.LARGE_SMOKE, 3, getBbWidth() * 0.5, 0.04);
			}
			if (deathTime % 12 == 0) {
				Fx.sound(sl, position(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.6f + f * 0.4f);
				Shake.at(sl, position(), 40, 0.4f, 8);
			}
			if (deathTime >= def.deathTicks) {
				Styles.burst(sl, position().add(0, getBbHeight() * 0.5, 0), def.style, getBbWidth(), 140);
				Fx.sound(sl, position(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
				sl.broadcastEntityEvent(this, (byte) 60);
				remove(Entity.RemovalReason.KILLED);
			}
		}
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		bar.removeAllPlayers();
		if (level() instanceof ServerLevel sl0 && !isDeadOrDying()) {
			BossMusic.end(this, sl0);
		}
		if (!isDeadOrDying()) {
			killMinions();
		}
		super.remove(reason);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		var bag = Mobs.stack(def.id + "_bag", 1);
		if (!bag.isEmpty()) {
			var e = spawnAtLocation(level, bag);
			if (e != null) {
				e.setGlowingTag(true);
				e.setExtendedLifetime();
				e.setInvulnerable(true);
				LootBeacon.mark(e, def.style);
			}
		}
		Player killer = level.getNearestPlayer(this, 64.0);
		if (killer instanceof ServerPlayer sp) {
			announce(Component.translatable("ultimatesurvival.boss.defeated", getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		}
	}

	// ---- persistence ---------------------------------------------------------------------------------------------------------------------------------------
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (home != null) {
			output.putDouble("HomeX", home.x);
			output.putDouble("HomeY", home.y);
			output.putDouble("HomeZ", home.z);
		}
		output.putInt("CombatTicks", combatTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		double hx = input.getDoubleOr("HomeX", Double.NaN);
		if (!Double.isNaN(hx)) {
			home = new Vec3(hx, input.getDoubleOr("HomeY", getY()), input.getDoubleOr("HomeZ", getZ()));
		}
		combatTicks = input.getIntOr("CombatTicks", 0);
		if (hasCustomName()) {
			bar.setName(getDisplayName());
		}
		refreshBar();
	}
}
