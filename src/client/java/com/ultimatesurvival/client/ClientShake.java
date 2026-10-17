package com.ultimatesurvival.client;

/** The camera wobble driven by {@code Payloads.Shake}: a decaying rumble on yaw and pitch (applied by CameraMixin). */
public final class ClientShake {
	private static float strength;
	private static int total;
	private static int left;
	private static long age;

	private ClientShake() {
	}

	public static void start(float s, int ticks) {
		if (s >= strength * remaining() || left <= 0) {
			strength = s;
			total = Math.max(1, ticks);
			left = total;
		}
	}

	public static void tick() {
		age++;
		if (left > 0 && --left == 0) {
			strength = 0;
		}
	}

	private static float remaining() {
		return left <= 0 ? 0 : (float) left / total;
	}

	public static boolean active() {
		return left > 0 && strength > 0;
	}

	public static float amplitude(float partial) {
		return left <= 0 ? 0 : strength * Math.max(0f, (left - partial) / total);
	}

	/** Yaw offset in degrees for this frame. */
	public static float yaw(float partial) {
		double t = age + partial;
		return (float) ((Math.sin(t * 2.9) + 0.6 * Math.sin(t * 1.7 + 1.3)) * 1.6 * amplitude(partial));
	}

	/** Pitch offset in degrees for this frame. */
	public static float pitch(float partial) {
		double t = age + partial;
		return (float) ((Math.sin(t * 3.4 + 0.7) + 0.6 * Math.sin(t * 2.1)) * 1.3 * amplitude(partial));
	}

	public static void reset() {
		strength = 0;
		left = 0;
	}
}
