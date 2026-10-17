package com.ultimatesurvival.mob.attack;

import com.ultimatesurvival.entity.Ally;
import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The attack library. Each type is a small class that schedules timed events on a {@link Run}
 * (telegraph first, damage after) so every attack can be dodged by reading the floor.
 * Damage numbers are multipliers of the attacker's base damage; the parameters come from the content tables.
 */
public final class Attacks {
	private static final Map<String, Function<MobDef.AttackSpec, Attack>> TYPES = new LinkedHashMap<>();

	static {
		TYPES.put("slam", Slam::new);
		TYPES.put("fan", Fan::new);
		TYPES.put("spiral", Spiral::new);
		TYPES.put("pools", Pools::new);
		TYPES.put("pillars", Pillars::new);
		TYPES.put("summon", Summon::new);
		TYPES.put("charge", Charge::new);
		TYPES.put("meteors", Meteors::new);
		TYPES.put("ring", Ring::new);
		TYPES.put("overheat", Overheat::new);
		TYPES.put("roar", Roar::new);
		CommonAttacks.register(TYPES);
		ThemedAttacks.register(TYPES);
		VoidAttacks.register(TYPES);
		LightAttacks.register(TYPES);
		FrostAttacks.register(TYPES);
		PlagueAttacks.register(TYPES);
		CosmicAttacks.register(TYPES);
	}

	private Attacks() {
	}

	public static Attack create(MobDef.AttackSpec spec) {
		Function<MobDef.AttackSpec, Attack> f = TYPES.get(spec.type());
		if (f == null) {
			throw new IllegalArgumentException("unknown attack type '" + spec.type() + "' - known: " + TYPES.keySet());
		}
		return f.apply(spec);
	}

	public static boolean known(String type) {
		return TYPES.containsKey(type);
	}

	// ---- shared helpers ----------------------------------------------------------------------------------------

	/** Things a boss attack may hurt: survival players and player allies, in a flat disc with a vertical window. */
	public static List<LivingEntity> disc(ServerLevel sl, Vec3 c, double r, double below, double above) {
		AABB box = new AABB(c.x - r, c.y - below, c.z - r, c.x + r, c.y + above, c.z + r);
		return sl.getEntitiesOfClass(LivingEntity.class, box, e -> isVictim(e) && horizontalDistSqr(e.position(), c) <= r * r);
	}

	public static boolean isVictim(LivingEntity e) {
		if (e instanceof Player p) {
			return p.isAlive() && !p.isCreative() && !p.isSpectator();
		}
		return e instanceof Ally && e.isAlive();
	}

	public static double horizontalDistSqr(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return dx * dx + dz * dz;
	}

	/** Direct damage from {@code m}: armor and enchantments apply, fire themes also ignite. */
	public static boolean hurt(UsmMob m, LivingEntity victim, double mult, String style) {
		ServerLevel sl = (ServerLevel) m.level();
		m.hits++;
		boolean ok = victim.hurtServer(sl, sl.damageSources().mobAttack(m), (float) m.dmg(mult));
		if (ok && style.equals("ember")) {
			victim.igniteForSeconds(4);
		}
		return ok;
	}

	public static Vec3 horizontal(Vec3 from, Vec3 to) {
		Vec3 d = new Vec3(to.x - from.x, 0, to.z - from.z);
		return d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
	}

	public static Vec3 rotateY(Vec3 v, double a) {
		double c = Math.cos(a);
		double s = Math.sin(a);
		return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
	}

	/** The floor under (or just above) a point: scans a few blocks down, then up, for the first walkable surface. */
	public static Vec3 ground(ServerLevel sl, Vec3 p) {
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos(Math.floor(p.x), Math.floor(p.y) + 2, Math.floor(p.z));
		for (int i = 0; i < 48; i++) {                                        // far enough for a boss hovering high above the arena
			if (!sl.getBlockState(bp).getCollisionShape(sl, bp).isEmpty()) {
				return new Vec3(p.x, bp.getY() + 1.0, p.z);
			}
			bp.move(0, -1, 0);
		}
		return p;
	}

	/** Unit vector from the middle of {@code m} to {@code to}. */
	public static Vec3 aim3(UsmMob m, Vec3 to) {
		Vec3 d = to.subtract(m.position().add(0, m.getBbHeight() * 0.5, 0));
		return d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
	}

	/** Where bolts leave the attacker: chest height, a little in front. */
	public static Vec3 muzzle(UsmMob m) {
		Vec3 f = Vec3.directionFromRotation(0, m.yBodyRot);
		return m.position().add(0, m.getBbHeight() * 0.62, 0).add(f.scale(m.getBbWidth() * 0.5));
	}

	public static Bolt fireBolt(UsmMob m, Vec3 from, Vec3 dir, String style, double mult, double speed, int life, double aoe,
			double size, String trail) {
		BoltSpec spec = new BoltSpec(style, (float) m.rawDmg(mult), (float) speed, life, 0, false, 0.0, false, (float) aoe, trail, (float) size);
		return Bolt.fire((ServerLevel) m.level(), m, from, dir, spec);
	}

	private static RandomSource rnd(Run r) {
		return r.level.getRandom();
	}

	// ---- slam: a red ring closes in, then the floor erupts ---------------------------------------------------------
	static final class Slam extends Attack {
		Slam(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 18);
			double radius = p.d("radius", 7);
			double kb = p.d("kb", 1.2);
			boolean jumpable = p.b("jump", true);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + 2 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.6f));
			r.every(0, windup - 2, 2, i -> {
				double f = Math.min(1.0, (i * 2.0 + 2) / windup);
				Styles.rim(sl, m.position(), radius * f, style, (int) (14 + 20 * f));
				if (i % 3 == 2) {
					Styles.rim(sl, m.position(), radius, style, 28);
				}
			});
			r.at(windup, () -> {
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 5, Styles.dust(style, 1.8f), 44);
				Styles.burst(sl, c.add(0, 0.3, 0), style, radius * 0.25, 40);
				Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 1.6f, 0.7f);
				Fx.sound(sl, c, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, 0.5f);
				Shake.at(sl, c, radius * 3, 0.8f, 14);
				for (LivingEntity v : disc(sl, c, radius, 1.0, 2.5)) {
					if (jumpable && !v.onGround() && v.getY() > m.getY() + 0.9) {
						continue;
					}
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, kb, 0.55);
				}
			});
		}
	}

	// ---- fan: a spread of bolts (optionally several volleys) ----------------------------------------------------------
	static final class Fan extends Attack {
		Fan(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 22);
			int recover = p.i("recover", 14);
			int volleys = p.i("volleys", 1);
			int gap = p.i("interval", 8);
			int count = p.i("count", 5);
			double spread = Math.toRadians(p.d("spread", 40));
			double speed = p.d("speed", 1.3);
			double aoe = p.d("aoe", 1.6);
			double size = p.d("size", 1.3);
			String style = style();
			String trail = p.s("trail", "dust");
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + (volleys - 1) * gap + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.6f));
			r.every(0, windup - 1, 3, i -> {
				Vec3 mz = muzzle(m);
				Fx.burst(sl, mz, Styles.accent(style), 4, 0.35, 0.02);
				Fx.burst(sl, mz, Styles.dust(style, 1.3f), 3, 0.3, 0.01);
			});
			for (int v = 0; v < volleys; v++) {
				r.at(windup + v * gap, () -> {
					Vec3 from = muzzle(m);
					LivingEntity t = r.target;
					Vec3 at = t != null && t.isAlive() ? t.getBoundingBox().getCenter() : r.aim.add(0, 1, 0);
					Vec3 dir = at.subtract(from).normalize();
					for (int k = 0; k < count; k++) {
						double off = count == 1 ? 0 : spread * (k / (double) (count - 1) - 0.5);
						fireBolt(m, from, rotateY(dir, off), style, mult(), speed, p.i("life", 70), aoe, size, trail);
					}
					Fx.sound(sl, from, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 0.7f);
				});
			}
		}
	}

	// ---- spiral: a rotating bullet-hell pattern -----------------------------------------------------------------------------
	static final class Spiral extends Attack {
		Spiral(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 20);
			int dur = p.i("duration", 60);
			int step = p.i("interval", 4);
			int arms = p.i("arms", 4);
			double turn = Math.toRadians(p.d("turn", 14));
			double speed = p.d("speed", 0.6);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + dur + p.i("recover", 20);
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.5f));
			r.every(0, windup - 1, 4, i -> Styles.rim(sl, m.position(), 2.5 + i * 0.4, style, 20));
			r.every(windup, windup + dur - 1, step, i -> {
				double base = i * turn;
				Vec3 from = m.position().add(0, m.getBbHeight() * 0.55, 0);
				for (int a = 0; a < arms; a++) {
					double ang = base + a * Math.PI * 2 / arms;
					fireBolt(m, from, new Vec3(Math.cos(ang), 0.03, Math.sin(ang)), style, mult(), speed, p.i("life", 90),
							p.d("aoe", 0.0), p.d("size", 1.0), p.s("trail", "dust"));
				}
				if (i % 3 == 0) {
					Fx.sound(sl, from, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.6f, 1.2f);
				}
			});
		}
	}

	// ---- pools: telegraphed circles that erupt and then keep burning ------------------------------------------------------------
	static final class Pools extends Attack {
		Pools(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 26);
			int recover = p.i("recover", 24);
			int count = p.i("count", 3);
			int life = p.i("life", 200);
			double radius = p.d("radius", 3.2);
			double near = p.d("near", 6.5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<Vec3> pts = new ArrayList<>();
			pts.add(ground(sl, r.aim));
			for (int i = 1; i < count; i++) {
				for (int tries = 0; tries < 12; tries++) {
					double a = rnd(r).nextDouble() * Math.PI * 2;
					double d = radius * 1.6 + rnd(r).nextDouble() * near;
					Vec3 cand = ground(sl, r.aim.add(Math.cos(a) * d, 0, Math.sin(a) * d));
					boolean ok = true;
					for (Vec3 o : pts) {
						ok &= horizontalDistSqr(o, cand) >= radius * radius * 2.25;
					}
					if (ok || tries == 11) {
						pts.add(cand);
						break;
					}
				}
			}
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.2f, 0.5f));
			r.every(0, windup - 1, 3, i -> {
				for (Vec3 pt : pts) {
					Styles.rim(sl, pt, radius, style, 18);
					Styles.fill(sl, pt, radius * 0.7, style, 4);
				}
			});
			r.at(windup, () -> {
				for (Vec3 pt : pts) {
					Styles.burst(sl, pt.add(0, 0.4, 0), style, radius * 0.4, 30);
					Fx.sound(sl, pt, Styles.impact(style), SoundSource.HOSTILE, 0.9f, 0.9f);
					for (LivingEntity v : disc(sl, pt, radius, 1.2, 2.5)) {
						hurt(m, v, mult(), style);
						Dmg.shove(v, pt, 0.5, 0.45);
					}
					m.addHazard(new Hazard(pt, radius, life, p.d("tick", 0.3), p.i("tick_every", 10), style, p.s("effect", ""), p.i("effect_amp", 1)));
				}
			});
		}
	}

	// ---- pillars: columns erupt where you are about to be ------------------------------------------------------------------------------
	static final class Pillars extends Attack {
		Pillars(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			int recover = p.i("recover", 20);
			int count = p.i("count", 6);
			int delay = p.i("delay", 24);
			int gap = p.i("interval", 7);
			double radius = p.d("radius", 2.1);
			double lift = p.d("lift", 0.85);
			double spread = p.d("spread", 6);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + (count - 1) * gap + delay + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.5f));
			r.every(0, windup - 1, 4, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight(), 0), Styles.accent(style), 6, 1.4, 0.05));
			for (int k = 0; k < count; k++) {
				final int idx = k;
				r.at(windup + k * gap, () -> {
					LivingEntity t = r.target;
					Vec3 base = t != null && t.isAlive() ? t.position().add(t.getDeltaMovement().multiply(1, 0, 1).scale(delay * 0.8)) : r.aim;
					if (idx >= 2) {
						double a = rnd(r).nextDouble() * Math.PI * 2;
						double d = rnd(r).nextDouble() * spread;
						base = base.add(Math.cos(a) * d, 0, Math.sin(a) * d);
					}
					final Vec3 at = ground(sl, base);
					r.every(r.t, r.t + delay - 1, 3, i -> {
						Styles.rim(sl, at, radius, style, 16);
						Styles.beam(sl, at, 0.8 + i * 0.5, style, 4);
					});
					r.at(r.t + delay, () -> {
						Styles.beam(sl, at, 7, style, 40);
						Styles.burst(sl, at.add(0, 0.5, 0), style, radius * 0.5, 36);
						Fx.sound(sl, at, Styles.impact(style), SoundSource.HOSTILE, 0.9f, 1.1f);
						for (LivingEntity v : disc(sl, at, radius, 1.0, 5.0)) {
							hurt(m, v, mult(), style);
							Dmg.shove(v, at, 0.15, lift);
						}
					});
				});
			}
		}
	}

	// ---- summon: call the boss's minions ------------------------------------------------------------------------------------------------
	static final class Summon extends Attack {
		Summon(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public boolean usable(UsmMob self, LivingEntity target) {
			return super.usable(self, target) && self.minionCount() < p.i("cap", 4);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			int recover = p.i("recover", 24);
			int count = p.i("count", 2);
			int cap = p.i("cap", 4);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			String mob = p.s("mob", m.def().minion == null ? "" : m.def().minion);
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.5f));
			r.every(0, windup - 1, 3, i -> {
				Styles.rim(sl, m.position(), 3.5 - i * 0.1, style, 20);
				Fx.burst(sl, m.position().add(0, 0.2, 0), Styles.accent(style), 6, 1.2, 0.2);
			});
			r.at(windup, () -> {
				int n = Math.min(count, cap - m.minionCount());
				for (int i = 0; i < n; i++) {
					double a = rnd(r).nextDouble() * Math.PI * 2;
					Vec3 at = ground(sl, m.position().add(Math.cos(a) * 4.5, 0, Math.sin(a) * 4.5));
					m.spawnMinion(mob, at.add(0, m.def().fly ? 1.5 : 0, 0));
				}
			});
		}
	}

	// ---- charge: line telegraph, a dash, then a stunned window ---------------------------------------------------------------------------------
	static final class Charge extends Attack {
		Charge(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			double dist = p.d("dist", 22);
			double speed = p.d("speed", 1.05);
			int stun = p.i("stun", 50);
			int wallStun = p.i("wall_stun", 40);
			double vuln = p.d("vuln", 1.6);
			double width = p.d("width", 1.6);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			int dash = (int) Math.ceil(dist / speed);
			boolean fly = m.def().fly;                                    // fliers dive along the full 3D line to the target
			Vec3[] dir = {fly ? aim3(m, r.aim.add(0, 1, 0)) : horizontal(m.position(), r.aim)};
			boolean[] running = {false};
			boolean[] done = {false};
			Set<Integer> hit = new HashSet<>();
			r.windup = windup;
			r.total = windup + dash + stun;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.7f));
			r.every(0, windup - 17, 2, i -> {
				LivingEntity t = r.target;
				if (t != null && t.isAlive()) {
					m.getLookControl().setLookAt(t, 40f, 40f);
				}
			});
			r.at(windup - 16, () -> {
				LivingEntity t = r.target;
				Vec3 at = t != null && t.isAlive() ? t.position() : r.aim;
				dir[0] = fly ? aim3(m, at.add(0, 1, 0)) : horizontal(m.position(), at);
			});
			r.every(windup - 16, windup - 1, 2, i -> {
				Vec3 a = m.position().add(0, fly ? m.getBbHeight() * 0.5 : 0.2, 0);
				Vec3 b = a.add(dir[0].scale(dist));
				Fx.line(sl, a, b, Styles.dust(style, 1.4f), 0.7);
				Fx.line(sl, a.add(-dir[0].z * width, 0, dir[0].x * width), b.add(-dir[0].z * width, 0, dir[0].x * width), Styles.accent(style), 1.4);
				Fx.line(sl, a.add(dir[0].z * width, 0, -dir[0].x * width), b.add(dir[0].z * width, 0, -dir[0].x * width), Styles.accent(style), 1.4);
			});
			r.at(windup, () -> {
				running[0] = true;
				Fx.sound(sl, m.position(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.6f);
			});
			r.every(windup, windup + dash - 1, 1, i -> {
				if (!running[0]) {
					return;
				}
				m.setDeltaMovement(dir[0].x * speed, fly ? dir[0].y * speed : Math.min(0.0, m.getDeltaMovement().y), dir[0].z * speed);
				m.faceYaw(dir[0]);
				Fx.burst(sl, m.position().add(0, 0.3, 0), Styles.accent(style), 3, 0.5, 0.02);
				Fx.burst(sl, m.position().add(0, 0.3, 0), Styles.haze(style), 2, 0.5, 0.02);
				AABB box = m.getBoundingBox().expandTowards(dir[0].scale(speed)).inflate(width * 0.5, 0.3, width * 0.5);     // sweep the step it is about to take
				for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, box, Attacks::isVictim)) {
					if (hit.add(v.getId())) {
						hurt(m, v, mult(), style);
						Dmg.shove(v, m.position(), 1.8, 0.5);
						Fx.sound(sl, v.position(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0f, 0.7f);
					}
				}
				if ((m.horizontalCollision || (fly && m.verticalCollision)) && i > 3 && !done[0]) {
					running[0] = false;
					done[0] = true;
					Styles.burst(sl, m.position().add(dir[0].scale(1.5)).add(0, 1.2, 0), style, 1.2, 40);
					Fx.sound(sl, m.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.5f);
					Shake.at(sl, m.position(), 24, 0.7f, 12);
					r.total += wallStun;
					m.setVuln(stun + wallStun, vuln);
				}
			});
			r.at(windup + dash, () -> {
				running[0] = false;
				if (!done[0]) {
					done[0] = true;
					m.setVuln(stun, vuln);
				}
			});
			r.onEnd(m::clearVuln);
		}
	}

	// ---- meteors: telegraphed impacts raining around the target -----------------------------------------------------------------------------------
	static final class Meteors extends Attack {
		Meteors(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 20);
			int count = p.i("count", 8);
			int delay = p.i("delay", 26);
			int gap = p.i("interval", 7);
			double radius = p.d("radius", 3.0);
			double area = p.d("area", 8);
			int aimed = p.i("aimed", 3);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + (count - 1) * gap + delay + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.4f, 0.5f));
			r.every(0, windup - 1, 3, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() + 0.5, 0), Styles.accent(style), 8, 1.2, 0.15));
			for (int k = 0; k < count; k++) {
				final int idx = k;
				r.at(windup + k * gap, () -> {
					LivingEntity t = r.target;
					Vec3 base = t != null && t.isAlive() ? t.position().add(t.getDeltaMovement().multiply(1, 0, 1).scale(delay * 0.6)) : r.aim;
					if (idx >= aimed) {
						double a = rnd(r).nextDouble() * Math.PI * 2;
						double d = rnd(r).nextDouble() * area;
						base = (t != null ? t.position() : r.aim).add(Math.cos(a) * d, 0, Math.sin(a) * d);
					}
					final Vec3 at = ground(sl, base);
					r.every(r.t, r.t + delay - 7, 3, i -> {
						double f = 1.0 - (double) i * 3 / delay;
						Styles.rim(sl, at, radius, style, 16);
						Styles.rim(sl, at, radius * Math.max(0.2, f), style, 10);
					});
					r.every(r.t + delay - 6, r.t + delay - 1, 1, i -> {
						Vec3 top = at.add(5.0 - i, 24.0 - i * 4.0, 2.5 - i * 0.5);
						Fx.line(sl, top, top.add(-1.0, -4.0, -0.5), Styles.accent(style), 0.5);
						sl.sendParticles(ParticleTypes.LARGE_SMOKE, top.x, top.y, top.z, 2, 0.2, 0.2, 0.2, 0.0);
					});
					r.at(r.t + delay, () -> {
						Styles.burst(sl, at.add(0, 0.6, 0), style, radius * 0.5, 50);
						Fx.shockwave(sl, at, radius, 3, Styles.dust(style, 1.6f), 24);
						Fx.sound(sl, at, Styles.impact(style), SoundSource.HOSTILE, 1.1f, 0.9f);
						Shake.at(sl, at, 14, 0.4f, 6);
						for (LivingEntity v : disc(sl, at, radius, 1.2, 3.0)) {
							hurt(m, v, mult(), style);
							Dmg.shove(v, at, 0.8, 0.5);
						}
						if (p.b("scorch", true)) {
							m.addHazard(new Hazard(at, radius * 0.6, p.i("scorch_life", 80), p.d("tick", 0.25), 10, style));
						}
					});
				});
			}
		}
	}

	// ---- ring: an expanding shockwave you have to jump ------------------------------------------------------------------------------------------------
	static final class Ring extends Attack {
		Ring(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 22);
			double speed = p.d("speed", 0.6);
			double maxR = p.d("max_radius", 22);
			double band = p.d("band", 1.8);
			boolean jumpable = p.b("jump", true);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			int ticks = (int) (maxR / speed);
			Set<Integer> hit = new HashSet<>();
			Vec3[] origin = {m.position()};
			r.windup = windup;
			r.total = windup + ticks + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.6f));
			r.every(0, windup - 1, 3, i -> Styles.rim(sl, m.position(), 1.5 + i * 0.35, style, 20));
			r.at(windup, () -> {
				origin[0] = m.position();
				Fx.sound(sl, origin[0], SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.5f);
				Shake.at(sl, origin[0], 30, 0.7f, 12);
			});
			r.every(windup, windup + ticks, 1, i -> {
				double rad = speed * (i + 1);
				Vec3 c = origin[0];
				int dots = (int) Math.min(120, Math.max(16, rad * 7));
				Fx.ring(sl, c, rad, dots, Styles.dust(style, 1.7f), 0.2);
				Fx.ring(sl, c, rad - 0.6, dots / 2, Styles.accent(style), 0.35);
				if (i % 2 == 0) {
					Fx.ring(sl, c, rad, dots / 3, Styles.haze(style), 0.7);
				}
				for (LivingEntity v : disc(sl, c, rad + 0.5, 1.0, 2.5)) {
					double d = Math.sqrt(horizontalDistSqr(v.position(), c));
					if (d >= rad - band && d <= rad + 0.5 && !hit.contains(v.getId())) {
						if (jumpable && !v.onGround() && v.getY() > c.y + 0.9) {
							continue;
						}
						hit.add(v.getId());
						hurt(m, v, mult(), style);
						Dmg.shove(v, c, 0.9, 0.55);
					}
				}
			});
		}
	}

	// ---- overheat: the boss vents - it hits harder and faster, but takes more damage ----------------------------------------------------------------
	static final class Overheat extends Attack {
		Overheat(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 20);
			int dur = p.i("duration", 120);
			int recover = p.i("recover", 20);
			double vuln = p.d("vuln", 1.4);
			double speedMul = p.d("speed", 1.35);
			double pulseR = p.d("pulse_radius", 5.5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + dur + recover;
			r.movable = true;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 1.6f, 0.5f));
			r.every(0, windup - 1, 2, i -> Fx.burst(sl, m.position().add(0, 1.0, 0), Styles.haze(style), 5, 0.8, 0.05));
			r.at(windup, () -> {
				m.setHot(true, vuln, speedMul);
				Styles.burst(sl, m.position().add(0, 1.5, 0), style, 1.5, 60);
				Fx.sound(sl, m.position(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 0.8f);
			});
			r.every(windup, windup + dur - 1, 2, i -> {
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.7, 0), Styles.haze(style), 3, m.getBbWidth() * 0.5, 0.04);
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.5, 0), Styles.accent(style), 2, m.getBbWidth() * 0.5, 0.06);
			});
			r.every(windup + 10, windup + dur - 1, 20, i -> {
				Styles.rim(sl, m.position(), pulseR, style, 30);
				Fx.shockwave(sl, m.position(), pulseR, 3, Styles.dust(style, 1.4f), 20);
				Fx.sound(sl, m.position(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 0.5f);
				for (LivingEntity v : disc(sl, m.position(), pulseR, 1.0, 2.5)) {
					hurt(m, v, p.d("pulse", 0.5), style);
					Dmg.shove(v, m.position(), 0.7, 0.35);
				}
			});
			r.onEnd(() -> m.setHot(false, 1.0, 1.0));
		}
	}

	// ---- roar: a shockwave that clears the area (also the phase-change cry) -------------------------------------------------------------------------
	static final class Roar extends Attack {
		Roar(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 24);
			int length = p.i("length", 60);
			double radius = p.d("radius", 14);
			double kb = p.d("kb", 1.6);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = length;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.roar(style), SoundSource.HOSTILE, 2.0f, 1.0f));
			r.every(0, windup - 1, 2, i -> {
				Styles.rim(sl, m.position(), radius * (i * 2.0 + 2) / windup, style, 28);
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.8, 0), Styles.accent(style), 6, 1.2, 0.1);
			});
			r.at(windup, () -> {
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 8, Styles.dust(style, 2.0f), 56);
				Styles.burst(sl, c.add(0, 1, 0), style, 2.0, 80);
				Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 2.0f, 0.6f);
				Shake.at(sl, c, radius * 3, 1.0f, 24);
				for (LivingEntity v : disc(sl, c, radius, 1.5, 4.0)) {
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, kb, 0.6);
				}
			});
		}
	}
}
