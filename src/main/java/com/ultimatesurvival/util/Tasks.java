package com.ultimatesurvival.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny server-thread scheduler: "do this N ticks from now". Used for staggered shockwaves, delayed
 * lightning strikes, telegraphed boss attacks, expanding rings ... Driven by END_SERVER_TICK.
 */
public final class Tasks {
	private record Task(long runAt, Runnable run) {
	}

	private static final List<Task> QUEUE = new ArrayList<>();
	private static long now;

	private Tasks() {
	}

	public static long now() {
		return now;
	}

	public static void later(int delayTicks, Runnable run) {
		if (delayTicks <= 0) {
			run.run();
			return;
		}
		QUEUE.add(new Task(now + delayTicks, run));
	}

	/** Runs {@code run} every tick for {@code ticks} ticks; the int is the 0-based step index. */
	public static void repeat(int ticks, java.util.function.IntConsumer run) {
		for (int i = 0; i < ticks; i++) {
			final int step = i;
			later(i, () -> run.accept(step));
		}
	}

	public static void tick() {
		now++;
		if (QUEUE.isEmpty()) {
			return;
		}
		List<Task> due = null;
		for (int i = QUEUE.size() - 1; i >= 0; i--) {
			if (QUEUE.get(i).runAt <= now) {
				if (due == null) {
					due = new ArrayList<>();
				}
				due.add(QUEUE.remove(i));
			}
		}
		if (due != null) {
			due.sort((a, b) -> Long.compare(a.runAt, b.runAt));
			for (Task t : due) {
				try {
					t.run.run();
				} catch (RuntimeException e) {
					com.ultimatesurvival.UltimateSurvival.LOGGER.error("scheduled task failed", e);
				}
			}
		}
	}

	public static void clear() {
		QUEUE.clear();
	}
}
