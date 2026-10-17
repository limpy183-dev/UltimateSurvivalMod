package com.ultimatesurvival.mob;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.mob.attack.Attack;
import com.ultimatesurvival.mob.attack.Attacks;
import com.ultimatesurvival.mob.attack.Hazard;
import com.ultimatesurvival.mob.attack.Run;
import com.ultimatesurvival.util.Fx;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Every Ultimate Survival mob. Stats, size, attacks and loot come from the content manifest ({@link MobDef});
 * the entity runs a small attack scheduler on top of vanilla goals, and keeps its animation state in
 * synched data so the client rig renderer can play it.
 */
public class UsmMob extends Monster {
	public static final int F_VULN = 1;
	public static final int F_ENRAGED = 2;
	public static final int F_SHIELD = 4;
	public static final int F_TRANSITION = 8;
	public static final int F_HOT = 16;
	public static final int F_DYING = 32;
	public static final int F_SPAWNING = 64;

	protected static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(UsmMob.class, EntityDataSerializers.STRING);
	protected static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(UsmMob.class, EntityDataSerializers.INT);
	protected static final EntityDataAccessor<Integer> ANIM_LEN = SynchedEntityData.defineId(UsmMob.class, EntityDataSerializers.INT);
	protected static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(UsmMob.class, EntityDataSerializers.INT);
	protected static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(UsmMob.class, EntityDataSerializers.INT);

	protected final MobDef def;
	protected Run run;
	protected int attackCd = 50;
	protected final List<Hazard> hazards = new ArrayList<>();
	protected final List<UsmMob> minions = new ArrayList<>();
	protected final List<Attack> queued = new ArrayList<>();
	protected UsmMob master;
	protected double vulnMul = 1.0;
	protected double hotSpeed = 1.0;
	private int vulnLeft;
	/** Tests / debugging: stop the chase AI from swinging so special attacks can be measured alone. */
	public boolean noMelee;
	/** Invasion scaling: multiplies the damage of every special attack (melee scales through its attribute). */
	public double damageScale = 1.0;
	/** Tests / debugging: never start attacks on its own (forced ones still run). */
	public boolean passive;
	/** Fliers drop to the ground while a "perch" attack runs. */
	public boolean perching;
	/** +1 / -1: which way a ranged mob circles its target. */
	public int strafe = 1;
	private int shieldLeft;
	private double reflectPct;

	/** Number of hurt attempts this mob made against victims (tests measure attacks with it; works on invulnerable players). */
	public int hits;
	private Attack lastAttack;
	private final Map<MobDef.AttackSpec, Attack> attackCache = new IdentityHashMap<>();

	public UsmMob(EntityType<? extends UsmMob> type, Level level) {
		super(type, level);
		this.def = Mobs.defOf(type);
		this.xpReward = def.xp;
		if (def.fly) {
			this.moveControl = new FlyingMoveControl<>(this, 20, true);
			this.setNoGravity(true);
		}
		if (def.boss) {
			this.setPersistenceRequired();
		}
		if (def.scale != 1.0f) {
			this.refreshDimensions();                        // the SCALE attribute is set from the registry: size the hitbox to match the model
		}
	}

	public static AttributeSupplier.Builder attributes(MobDef d) {
		UsmConfig c = UsmConfig.get();
		AttributeSupplier.Builder b = Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, d.hp * (d.boss ? c.bossHealthMultiplier : 1.0))
				.add(Attributes.ARMOR, d.armor)
				.add(Attributes.ATTACK_DAMAGE, d.dmg * (d.boss ? 0.7 : 1.0))
				.add(Attributes.MOVEMENT_SPEED, d.speed)
				.add(Attributes.FOLLOW_RANGE, d.follow)
				.add(Attributes.KNOCKBACK_RESISTANCE, d.kb)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0)
				.add(Attributes.SCALE, d.scale)
				.add(Attributes.STEP_HEIGHT, d.boss ? 1.5 : 0.7);
		if (d.fly) {
			b.add(Attributes.FLYING_SPEED, d.speed * 1.2);
		}
		return b;
	}

	public MobDef def() {
		return def;
	}

	/** Natural-spawn test: right dimension, enough progress, night / height rules, then the usual ground and light checks. */
	public static boolean canSpawnNaturally(MobDef d, net.minecraft.world.level.ServerLevelAccessor level, EntitySpawnReason reason, net.minecraft.core.BlockPos pos,
			net.minecraft.util.RandomSource random) {
		MobDef.SpawnRule r = d.spawn;
		if (r == null) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.COMMAND || reason == EntitySpawnReason.SPAWN_ITEM_USE
				|| reason == EntitySpawnReason.TRIGGERED || reason == EntitySpawnReason.MOB_SUMMONED) {
			return true;
		}
		ServerLevel sl = level.getLevel();
		String dim = sl.dimension().identifier().getPath();
		boolean dimOk = switch (r.dim()) {
			case "nether" -> dim.equals("the_nether");
			case "end" -> dim.equals("the_end");
			default -> dim.equals("overworld");
		};
		if (!dimOk || pos.getY() < r.yMin() || pos.getY() > r.yMax() || com.ultimatesurvival.progress.Gates.currentTier() < r.minTier()
				|| (!r.gate().isEmpty() && !com.ultimatesurvival.progress.Gates.isCleared(r.gate()))) {
			return false;
		}
		if (r.dim().equals("overworld") && r.night() && !Monster.isDarkEnoughToSpawn(level, pos, random)) {
			return false;
		}
		return Mob.checkMobSpawnRules(Mobs.TYPES.get(d.id), level, reason, pos, random);
	}

	// ---- synched state (read by the client renderer) -------------------------------------------------------------------
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ANIM, "");
		builder.define(ANIM_START, 0);
		builder.define(ANIM_LEN, 1);
		builder.define(FLAGS, 0);
		builder.define(PHASE, 0);
	}

	public String anim() {
		return entityData.get(ANIM);
	}

	/** Progress 0..1 of the playing animation, or -1 when nothing is playing (client + server). */
	public float animProgress(float partial) {
		if (entityData.get(ANIM).isEmpty()) {
			return -1;
		}
		float p = (level().getGameTime() + partial - entityData.get(ANIM_START)) / entityData.get(ANIM_LEN);
		return p > 1.0f ? -1 : Math.max(0, p);
	}

	public void startAnim(String id, int length) {
		entityData.set(ANIM, id);
		entityData.set(ANIM_START, (int) level().getGameTime());
		entityData.set(ANIM_LEN, Math.max(1, length));
	}

	public void clearAnim() {
		entityData.set(ANIM, "");
	}

	public int phaseIndex() {
		return Math.min(entityData.get(PHASE), def.phases.size() - 1);
	}

	public MobDef.Phase phase() {
		return def.phases.get(phaseIndex());
	}

	public int flagBits() {
		return entityData.get(FLAGS);
	}

	public boolean hasFlag(int f) {
		return (entityData.get(FLAGS) & f) != 0;
	}

	public void setFlag(int f, boolean on) {
		int cur = entityData.get(FLAGS);
		int next = on ? cur | f : cur & ~f;
		if (next != cur) {
			entityData.set(FLAGS, next);
		}
	}

	// ---- damage numbers --------------------------------------------------------------------------------------------------------
	public double rawDmg(double mult) {
		return def.dmg * mult * phase().dmg() * (hasFlag(F_ENRAGED) ? 1.25 : 1.0) * damageScale;
	}

	/** Damage for a direct hit: the content multiple x phase x enrage x the boss-damage config. */
	public double dmg(double mult) {
		return rawDmg(mult) * UsmConfig.get().bossDamageMultiplier;
	}

	// ---- goals ----------------------------------------------------------------------------------------------------------------------
	@Override
	protected void registerGoals() {
		MobDef d = Mobs.defOf(getType());
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new FreezeGoal(this));
		goalSelector.addGoal(2, new ChaseGoal(this));
		if (d == null || !d.fly) {
			goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		}
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, d == null || !d.boss));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		MobDef d = Mobs.defOf(getType());
		if (d != null && d.fly) {
			FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
			nav.setCanOpenDoors(false);
			nav.setCanFloat(true);
			return nav;
		}
		return super.createNavigation(level);
	}

	/** True while an attack owns the body (movement AI stands down). */
	public boolean busy() {
		return run != null && !(run.movable && run.strikeStarted());
	}

	public boolean attacking() {
		return run != null;
	}

	/** Tick of the running attack, or -1 (tests / debugging). */
	public int runTick() {
		return run == null ? -1 : run.t;
	}

	public String runType() {
		return run == null ? "" : run.attack.type;
	}

	public void setAttackCooldown(int ticks) {
		attackCd = ticks;
	}

	/** While an attack owns the body nothing else may move it (otherwise the stroll goal wanders off mid-windup). */
	static final class FreezeGoal extends Goal {
		private final UsmMob m;

		FreezeGoal(UsmMob m) {
			this.m = m;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return m.busy();
		}

		@Override
		public boolean canContinueToUse() {
			return m.busy();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			m.getNavigation().stop();
		}
	}

	/** Chases the target and swings when in reach; stands down while an attack runs. */
	static final class ChaseGoal extends Goal {
		private final UsmMob m;
		private int repath;
		private int swingCd;
		private double flyX;
		private double flyY;
		private double flyZ;
		private boolean flying;

		ChaseGoal(UsmMob m) {
			this.m = m;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = m.getTarget();
			return t != null && t.isAlive() && !m.busy() && !m.isDeadOrDying();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			m.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity t = m.getTarget();
			if (t == null) {
				return;
			}
			m.getLookControl().setLookAt(t, 30f, 30f);
			if (m.noMelee) {
				return;                                        // debug "hold position" mode
			}
			if (swingCd > 0) {
				swingCd--;
			}
			double reach = m.getBbWidth() * 0.5 + 1.7 + t.getBbWidth() * 0.5;
			double dx = t.getX() - m.getX();
			double dz = t.getZ() - m.getZ();
			double flat2 = dx * dx + dz * dz;
			if (--repath <= 0) {
				repath = 4 + m.getRandom().nextInt(7);
				double dist = Math.sqrt(flat2);
				double wx = t.getX();
				double wz = t.getZ();
				boolean move = true;
				if (m.def.keepMax > 0) {                       // ranged mobs hold a distance band and circle the target
					Vec3 away = new Vec3(-dx, 0, -dz).normalize();
					if (dist < m.def.keepMin) {
						wx = m.getX() + away.x * 5;
						wz = m.getZ() + away.z * 5;
					} else if (dist <= m.def.keepMax) {
						double mid = (m.def.keepMin + m.def.keepMax) * 0.5;
						Vec3 spun = com.ultimatesurvival.mob.attack.Attacks.rotateY(away.scale(mid), 0.5 * m.strafe);
						wx = t.getX() + spun.x;
						wz = t.getZ() + spun.z;
						if (m.getRandom().nextInt(6) == 0) {
							m.strafe = -m.strafe;
						}
					}
				} else if (!m.def.fly && flat2 <= reach * reach * 0.6) {
					move = false;
				}
				if (move) {
					if (m.def.fly) {
						flyX = wx;
						flyY = t.getY() + 1.0 + m.hoverNow();
						flyZ = wz;
						flying = true;
					} else {
						m.getNavigation().moveTo(wx, t.getY(), wz, 1.0);
					}
				} else {
					flying = false;
				}
			}
			if (flying && m.def.fly) {                         // a flying move control only moves for the tick it was asked: ask every tick
				m.getMoveControl().setWantedPosition(flyX, flyY, flyZ, 1.0);
			}
			if (swingCd <= 0 && m.def.melee && !m.noMelee && flat2 <= reach * reach && Math.abs(t.getY() - m.getY()) < m.getBbHeight() + 0.5
					&& m.level() instanceof ServerLevel sl) {
				swingCd = 20;
				m.swing(InteractionHand.MAIN_HAND);
				if (m.animProgress(0) < 0) {
					m.startAnim("melee", 14);
				}
				m.doHurtTarget(sl, t);
			}
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!(target instanceof LivingEntity v)) {
			return false;
		}
		hits++;
		float amount = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * phase().dmg() * (hasFlag(F_ENRAGED) ? 1.25 : 1.0)
				* UsmConfig.get().bossDamageMultiplier);
		boolean ok = v.hurtServer(level, damageSources().mobAttack(this), amount);
		if (ok) {
			Vec3 away = v.position().subtract(position());
			Vec3 h = new Vec3(away.x, 0, away.z);
			h = h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : h.normalize();
			double resist = 1.0 - v.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			v.setDeltaMovement(v.getDeltaMovement().add(h.x * 0.9 * resist, 0.32 * resist, h.z * 0.9 * resist));
			v.hurtMarked = true;
			if (def.fire) {
				v.igniteForSeconds(4);
			}
		}
		return ok;
	}

	// ---- per-tick server logic ----------------------------------------------------------------------------------------------------
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (isDeadOrDying()) {
			return;
		}
		if (vulnLeft > 0 && --vulnLeft == 0) {
			clearVuln();
		}
		if (shieldLeft > 0 && --shieldLeft == 0) {
			clearShield();
		} else if (shieldLeft > 0 && tickCount % 3 == 0) {
			com.ultimatesurvival.mob.attack.Styles.rim(level, position().add(0, getBbHeight() * 0.35, 0), getBbWidth() * 0.9, def.style, 14);
		}
		tickHazards(level);
		tickAttacks(level);
	}

	protected void tickHazards(ServerLevel level) {
		for (Iterator<Hazard> it = hazards.iterator(); it.hasNext();) {
			if (!it.next().tick(this, level)) {
				it.remove();
			}
		}
	}

	protected void tickAttacks(ServerLevel level) {
		if (run != null) {
			if (!run.tick()) {
				endRun();
			}
			return;
		}
		LivingEntity t = getTarget();
		if (!queued.isEmpty()) {
			startRun(queued.remove(0), t);
			return;
		}
		if (attackCd > 0) {
			attackCd--;
		}
		if (passive || attackCd > 0 || t == null || !t.isAlive() || hasFlag(F_SPAWNING) || hasFlag(F_TRANSITION)) {
			return;
		}
		Attack a = pick(t);
		if (a == null) {
			attackCd = 8;
			return;
		}
		startRun(a, t);
	}

	protected Attack attackFor(MobDef.AttackSpec spec) {
		return attackCache.computeIfAbsent(spec, Attacks::create);
	}

	protected Attack pick(LivingEntity target) {
		List<Attack> pool = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (MobDef.AttackSpec s : phase().attacks()) {
			Attack a = attackFor(s);
			if (!a.usable(this, target)) {
				continue;
			}
			int w = a == lastAttack ? Math.max(1, a.weight / 3) : a.weight;
			pool.add(a);
			weights.add(w);
			total += w;
		}
		if (pool.isEmpty()) {
			return null;
		}
		int roll = getRandom().nextInt(total);
		for (int i = 0; i < pool.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) {
				return pool.get(i);
			}
		}
		return pool.get(0);
	}

	public void startRun(Attack a, LivingEntity target) {
		if (!(level() instanceof ServerLevel sl)) {
			return;
		}
		cancelRun();
		run = new Run(this, sl, a, target);
		a.begin(run);
		lastAttack = a;
		startAnim(a.anim(), run.total);
		getNavigation().stop();
	}

	protected void endRun() {
		if (run != null) {
			run.end();
		}
		run = null;
		clearAnim();
		MobDef.Phase ph = phase();
		double cdMul = hasFlag(F_ENRAGED) ? 0.6 : 1.0;
		attackCd = (int) ((ph.cdMin() + getRandom().nextInt(Math.max(1, ph.cdMax() - ph.cdMin() + 1))) * cdMul);
	}

	public void cancelRun() {
		if (run != null) {
			run.cancelled = true;
			run.end();
			run = null;
			clearAnim();
		}
	}

	/** Test / debug hook: queue the first attack of the given type from the current phase (or a default one). */
	public boolean forceAttack(String type) {
		List<MobDef.Phase> order = new ArrayList<>();
		order.add(phase());                              // the current phase's version of an attack wins
		order.addAll(def.phases);
		for (MobDef.Phase ph : order) {
			for (MobDef.AttackSpec s : ph.attacks()) {
				if (s.type().equals(type)) {
					queued.add(attackFor(s));
					return true;
				}
			}
			for (MobDef.AttackSpec s : ph.enter()) {
				if (s.type().equals(type)) {
					queued.add(attackFor(s));
					return true;
				}
			}
		}
		return false;
	}

	// ---- helpers used by attacks ----------------------------------------------------------------------------------------------------
	public void addHazard(Hazard h) {
		hazards.add(h);
	}

	public List<Hazard> hazards() {
		return hazards;
	}

	public void setVuln(int ticks, double mult) {
		vulnMul = mult;
		vulnLeft = ticks;
		setFlag(F_VULN, true);
		getNavigation().stop();
	}

	public void clearVuln() {
		vulnLeft = 0;
		vulnMul = 1.0;
		setFlag(F_VULN, false);
	}

	public void setHot(boolean on, double dmgTakenMul, double speedMul) {
		setFlag(F_HOT, on);
		vulnMul = on ? dmgTakenMul : 1.0;
		hotSpeed = speedMul;
		refreshSpeed();
	}

	protected void refreshSpeed() {
		double s = def.speed * phase().speed() * hotSpeed * (hasFlag(F_ENRAGED) ? 1.15 : 1.0);
		var inst = getAttribute(Attributes.MOVEMENT_SPEED);
		if (inst != null && Math.abs(inst.getBaseValue() - s) > 1e-6) {
			inst.setBaseValue(s);
		}
		var fly = getAttribute(Attributes.FLYING_SPEED);
		if (fly != null && def.fly) {
			fly.setBaseValue(s * 1.2);
		}
	}

	/** Instant teleport with a flash at both ends. */
	public void blinkTo(Vec3 at) {
		if (!(level() instanceof ServerLevel sl)) {
			return;
		}
		com.ultimatesurvival.mob.attack.Styles.burst(sl, position().add(0, getBbHeight() * 0.5, 0), def.style, getBbWidth() * 0.6, 30);
		snapTo(at.x, at.y, at.z, getYRot(), getXRot());
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		com.ultimatesurvival.mob.attack.Styles.burst(sl, at.add(0, getBbHeight() * 0.5, 0), def.style, getBbWidth() * 0.6, 30);
		Fx.sound(sl, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3f, 0.7f);
	}

	public void faceYaw(Vec3 dir) {
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
		setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	public int minionCount() {
		minions.removeIf(m -> m.isRemoved() || !m.isAlive());
		return minions.size();
	}

	public UsmMob spawnMinion(String id, Vec3 at) {
		EntityType<? extends UsmMob> type = Mobs.TYPES.get(id);
		if (type == null || !(level() instanceof ServerLevel sl)) {
			return null;
		}
		UsmMob m = type.create(sl, EntitySpawnReason.MOB_SUMMONED);
		if (m == null) {
			return null;
		}
		m.snapTo(at.x, at.y, at.z, getRandom().nextFloat() * 360f, 0f);
		m.master = this;
		m.setTarget(getTarget());
		sl.addFreshEntity(m);
		minions.add(m);
		com.ultimatesurvival.mob.attack.Styles.burst(sl, at.add(0, 0.5, 0), def.style, 0.5, 40);
		Fx.sound(sl, at, com.ultimatesurvival.mob.attack.Styles.warn(def.style), SoundSource.HOSTILE, 1.0f, 0.8f);
		return m;
	}

	public UsmMob master() {
		return master;
	}

	// ---- damage rules ------------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (hasFlag(F_SHIELD) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			if (reflectPct > 0 && source.getEntity() instanceof LivingEntity attacker && attacker != this && !(attacker instanceof UsmMob)) {
				attacker.hurtServer(level, level.damageSources().thorns(this), (float) (amount * reflectPct));
			}
			Fx.burst(level, position().add(0, getBbHeight() * 0.5, 0), com.ultimatesurvival.mob.attack.Styles.dust(def.style, 1.6f), 10, getBbWidth() * 0.6, 0.05);
			Fx.sound(level, position(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.0f, 0.7f);
			return false;
		}
		if ((hasFlag(F_VULN) || hasFlag(F_HOT)) && vulnMul != 1.0) {
			amount *= (float) vulnMul;
		}
		amount *= (float) phase().taken();
		return super.hurtServer(level, source, amount);
	}

	public double hoverNow() {
		return perching ? 0 : def.hover;
	}

	/** Reflective shield: damage is refused (and {@code pct} of it thrown back at the attacker) for {@code ticks}. */
	public void setShield(int ticks, double pct) {
		shieldLeft = ticks;
		reflectPct = pct;
		setFlag(F_SHIELD, true);
	}

	public void clearShield() {
		shieldLeft = 0;
		reflectPct = 0;
		if (!hasNodes()) {
			setFlag(F_SHIELD, false);
		}
	}

	/** Overridden by bosses that are shielded by pylons. */
	public boolean hasNodes() {
		return false;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		Mobs.rollDrops(this, level, def.drops);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !def.boss && super.removeWhenFarAway(distSqr);
	}

	// ---- sounds -----------------------------------------------------------------------------------------------------------------------------
	@Override
	protected SoundEvent getAmbientSound() {
		return Mobs.sound(def, "ambient", null);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return Mobs.sound(def, "hurt", SoundEvents.HOSTILE_HURT);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return Mobs.sound(def, "death", SoundEvents.HOSTILE_DEATH);
	}

	@Override
	public float getVoicePitch() {
		return (float) (Mobs.soundPitch(def) + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.12);
	}

	// ---- persistence --------------------------------------------------------------------------------------------------------------------------
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("UsmPhase", entityData.get(PHASE));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(PHASE, input.getIntOr("UsmPhase", 0));
		refreshSpeed();
	}
}
