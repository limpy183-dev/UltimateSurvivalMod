package com.ultimatesurvival.mob.attack;

import com.ultimatesurvival.mob.UsmMob;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * One in-flight attack. An {@link Attack#begin(Run)} schedules everything it does as timed events
 * ({@link #at}, {@link #every}); the owning mob ticks the run once per server tick. Because events are
 * stored here (not in a global queue) they vanish with the mob - nothing fires after a boss dies.
 */
public final class Run {
	private record Event(int at, Runnable run) {
	}

	public final UsmMob self;
	public final ServerLevel level;
	public final Attack attack;
	public final LivingEntity target;
	/** Where the target stood when the attack began - attacks aim here so they stay dodgeable. */
	public final Vec3 aim;
	/** Current tick of the run (0 = first tick). */
	public int t;
	/** Tick at which the "strike" happens (end of the telegraph). */
	public int windup;
	/** Total length in ticks; attacks may extend it while running (e.g. a wall hit stuns longer). */
	public int total;
	/** When true the mob may keep walking after the strike (overheat). */
	public boolean movable;
	public boolean cancelled;
	private final List<Event> events = new ArrayList<>();
	private final List<Runnable> endHooks = new ArrayList<>();

	public Run(UsmMob self, ServerLevel level, Attack attack, LivingEntity target) {
		this.self = self;
		this.level = level;
		this.attack = attack;
		this.target = target;
		this.aim = target == null ? self.position() : target.position();
	}

	public Run at(int tick, Runnable run) {
		events.add(new Event(tick, run));
		return this;
	}

	/** Runs {@code run(i)} on ticks {@code from..to} (inclusive) every {@code step} ticks; i counts from 0. */
	public Run every(int from, int to, int step, IntConsumer run) {
		int i = 0;
		for (int k = from; k <= to; k += step) {
			final int idx = i++;
			at(k, () -> run.accept(idx));
		}
		return this;
	}

	/** Cleanup that must happen however the run ends (flags, buffs). */
	public Run onEnd(Runnable run) {
		endHooks.add(run);
		return this;
	}

	public boolean strikeStarted() {
		return t >= windup;
	}

	/** @return true while the run is still going. */
	public boolean tick() {
		if (cancelled || !self.isAlive()) {
			return false;
		}
		for (int i = 0; i < events.size(); i++) {
			Event e = events.get(i);
			if (e.at == t) {
				e.run.run();
			}
		}
		t++;
		return t < total;
	}

	public void end() {
		for (Runnable r : endHooks) {
			r.run();
		}
		endHooks.clear();
		events.clear();
	}
}
