"""Tier 4 models: Voltaris (storm), Hemorath (blood), Chronos (time) and their helpers (storm wisp, storm totem, blood bat).

Conventions (see rig.py): pixels, feet-up, front = -z, right side = -x. Arm rz > 0 swings the RIGHT arm outward, rz < 0 the left."""
import math
from .rig import Rig, PI


def hold(R, bone, anim, axis, v, a=0.2, b=0.85):
    """Rise to v by progress a, hold until b, return to rest."""
    R.kf(bone, "attack:" + anim, axis, (0, 0), (a, v), (b, v), (1, 0))


def arms(R, anim, rx=0.0, rz=0.0, a=0.2, b=0.85, fore=0.0):
    """Both arms to a pose (rz is given for the right arm and mirrored for the left)."""
    for s, sg in (("r", 1), ("l", -1)):
        if rx:
            hold(R, "arm_" + s, anim, "rx", rx, a, b)
        if rz:
            hold(R, "arm_" + s, anim, "rz", rz * sg, a, b)
        if fore:
            hold(R, "forearm_" + s, anim, "rx", fore, a, b)


def mirror_cube(o, size, sx):
    """Authored for the right side (x < 0); returns the origin for the side sx (-1 right, +1 left)."""
    return o if sx < 0 else (-o[0] - size[0], o[1], o[2])


def spawn_dead_dissolve(R, rise=0.0):
    """Generic spawn (grows out of nothing, spinning) and death (shrinks away, spinning) for floating bosses."""
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.95), (0.7, -0.05), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (0.6, -0.15), (1, -0.95))
    R.kf("root", "spawn", "ry", (0, 0), (1, 6.28))
    R.kf("root", "dead", "ry", (0, 0), (1, 12.5))
    R.kf("root", "dead", "py", (0, 0), (1, -16))
    if rise:
        R.kf("root", "spawn", "py", (0, -rise), (1, 0))


# =====================================================================================================================
def voltaris():
    R = Rig("voltaris", dict(cloak="#27324f", plate="#7da7d9", dark="#1a2238", spark="#ffe94d", cloud="#c9d8f0", glass="#9fd8ff"),
            glow="#9fe3ff")
    # the funnel: stacked layers that each twist at their own speed
    layers = [(0, 4, 6), (4, 5, 10), (9, 6, 14), (15, 7, 18), (22, 8, 22)]
    for i, (y, h, w) in enumerate(layers):
        b = R.bone("cloak%d" % i, None, pivot=(0, y + h / 2.0, 0))
        b.cube((-w // 2, y, -w // 2), (w, h, w), paint="rock", pal="cloak", glow=0.35 if i % 2 else 0.12)

    torso = R.bone("torso", None, pivot=(0, 30, 0))
    torso.cube((-9, 30, -6), (18, 14, 12), paint="plate", pal="plate", glow=0.3)
    torso.cube((-4, 34, -7), (8, 8, 1), paint="lava", pal="spark")                                   # lightning core
    torso.cube((-10, 42, -7), (20, 3, 14), paint="plate", pal="dark")                                # collar

    head = R.bone("head", "torso", pivot=(0, 45, 0))
    head.cube((-5, 45, -5), (10, 10, 10), paint="plate", pal="plate", glow=0.15,
              features=[("front", 1, 4, 3, 2, "#ffe94d", True), ("front", 6, 4, 3, 2, "#ffe94d", True), ("front", 3, 7, 4, 1, "#9fe3ff", True)])
    head.cube((-1, 55, -1), (2, 8, 2), paint="lava", pal="spark")                                     # crown spire
    head.cube((-7, 52, -2), (2, 6, 4), paint="plate", pal="dark")
    head.cube((5, 52, -2), (2, 6, 4), paint="plate", pal="dark")

    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(12 * sx, 42, 0))
        arm.cube(mirror_cube((-17, 41, -5), (10, 6, 10), sx), (10, 6, 10), paint="plate", pal="plate", glow=0.2)      # pauldron
        arm.cube(mirror_cube((-13, 47, -1), (2, 6, 2), sx), (2, 6, 2), paint="lava", pal="spark")                    # spike
        arm.cube(mirror_cube((-15, 28, -3), (6, 13, 6), sx), (6, 13, 6), paint="rock", pal="cloak", glow=0.3)
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(12 * sx, 28, 0))
        fore.cube(mirror_cube((-15, 15, -3), (6, 13, 6), sx), (6, 13, 6), paint="plate", pal="plate", glow=0.3)
        fore.cube(mirror_cube((-16, 9, -4), (8, 6, 8), sx), (8, 6, 8), paint="lava", pal="glass")                    # crackling hand
        cloud = R.bone("cloud_" + s, None, pivot=(22 * sx, 38, 0))
        cloud.cube(mirror_cube((-27, 35, -5), (9, 6, 9), sx), (9, 6, 9), paint="rock", pal="cloud", glow=0.08)

    orbit = R.bone("orbit", None, pivot=(0, 34, 0))
    for (x, y, z) in ((24, 36, 0), (-24, 40, 0), (0, 32, 24), (0, 44, -24)):
        orbit.cube((x - 2, y, z - 2), (4, 9, 4), paint="lava", pal="glass")                          # lightning blades
    orbit2 = R.bone("orbit2", None, pivot=(0, 40, 0))
    for (x, z) in ((15, 15), (-15, -15), (15, -15), (-15, 15)):
        orbit2.cube((x - 2, 48, z - 2), (4, 4, 4), paint="lava", pal="spark")

    R.sin("root", "always", "py", 0.07, 1.6)
    R.look("head")
    for i in range(5):
        R.spin("cloak%d" % i, (0.06 if i % 2 == 0 else -0.05) * (1 + 0.3 * i))
    R.spin("orbit", 0.07)
    R.spin("orbit2", -0.1)
    R.walk("torso", "rx", 0.22, PI / 2, speed=0.0)                                                   # leans into movement
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.06, 0.07, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.05 * sg)
        R.sin("cloud_" + s, "always", "py", 0.09, 1.2, 0.0 if s == "r" else 1.5)

    # lightning: both arms to the sky, head thrown back, the whole body rises
    arms(R, "lightning", rx=-2.9, rz=0.3, a=0.2, b=0.88)
    hold(R, "head", "lightning", "rx", -0.55, 0.2, 0.88)
    hold(R, "root", "lightning", "py", 5, 0.2, 0.88)
    # gust: wind up, then both palms thrust forward
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:gust", "rx", (0, 0), (0.4, 0.5), (0.5, -1.7), (0.85, -1.7), (1, 0))
    R.kf("torso", "attack:gust", "rx", (0, 0), (0.45, 0.35), (0.5, -0.3), (0.8, -0.2), (1, 0))
    # orbs: hands cupped in front
    arms(R, "orbs", rx=-1.1, rz=-0.25, a=0.15, b=0.9, fore=-0.9)
    # perch: drops to the ground, arms hanging, head bowed
    arms(R, "perch", rx=0.35, a=0.3, b=0.9)
    hold(R, "head", "perch", "rx", 0.4, 0.3, 0.9)
    # exposed: slumped and open
    hold(R, "head", "exposed", "rx", 0.65, 0.15, 0.9)
    hold(R, "torso", "exposed", "rx", -0.2, 0.15, 0.9)
    arms(R, "exposed", rx=0.3, rz=0.5, a=0.15, b=0.9)
    # roar
    arms(R, "roar", rx=-0.4, rz=1.0, a=0.2, b=0.8)
    hold(R, "head", "roar", "rx", -0.7, 0.15, 0.85)
    # melee
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.0), (0.55, -0.4), (1, 0))
    R.kf("torso", "attack:melee", "ry", (0, 0), (0.35, 0.4), (0.55, -0.45), (1, 0))
    # shock ring / slam
    arms(R, "ring", rx=-2.7, a=0.5, b=0.6)
    R.kf("root", "attack:ring", "py", (0, 0), (0.5, 4), (0.58, -3), (1, 0))
    spawn_dead_dissolve(R, rise=0)
    return R


def storm_wisp():
    R = Rig("storm_wisp", dict(cloud="#b9cbea", spark="#ffe94d", glass="#9fd8ff"), glow="#9fe3ff")
    core = R.bone("core", None, pivot=(0, 8, 0))
    core.cube((-2, 6, -2), (4, 4, 4), paint="lava", pal="spark")
    puffs = R.bone("puffs", None, pivot=(0, 8, 0))
    for (x, y, z, w) in ((-5, 4, -3, 5), (1, 8, -4, 5), (-4, 8, 1, 5), (1, 3, 1, 5)):
        puffs.cube((x, y, z), (w, 4, w), paint="rock", pal="cloud", glow=0.05)
    sparks = R.bone("sparks", None, pivot=(0, 8, 0))
    for (x, z) in ((7, 0), (-8, 0), (0, 7), (0, -8)):
        sparks.cube((x, 7, z), (2, 2, 2), paint="lava", pal="glass")
    R.sin("root", "always", "py", 0.2, 1.0)
    R.spin("puffs", 0.05)
    R.spin("sparks", -0.18)
    R.sin("core", "always", "sx", 0.3, 0.12)
    R.sin("core", "always", "sy", 0.3, 0.12)
    R.sin("core", "always", "sz", 0.3, 0.12)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.9))
    R.kf("root", "attack:fan", "py", (0, 0), (0.5, 2.0), (0.7, -1), (1, 0))
    R.kf("core", "attack:fan", "sx", (0, 0), (0.6, 0.8), (1, 0))
    R.kf("core", "attack:fan", "sy", (0, 0), (0.6, 0.8), (1, 0))
    R.kf("core", "attack:fan", "sz", (0, 0), (0.6, 0.8), (1, 0))
    return R


def storm_totem():
    R = Rig("storm_totem", dict(rock="#394a66", plate="#7da7d9", glass="#9fd8ff", spark="#ffe94d"), glow="#9fe3ff")
    base = R.bone("base", None, pivot=(0, 0, 0))
    base.cube((-6, 0, -6), (12, 4, 12), paint="rock", pal="rock", glow=0.15)
    base.cube((-4, 4, -4), (8, 20, 8), paint="plate", pal="plate", glow=0.4)
    crystal = R.bone("crystal", None, pivot=(0, 30, 0))
    crystal.cube((-5, 25, -5), (10, 12, 10), paint="lava", pal="glass")
    crystal.cube((-2, 37, -2), (4, 6, 4), paint="lava", pal="spark")
    r1 = R.bone("ring1", None, pivot=(0, 16, 0))
    for (x, z) in ((9, 0), (-12, 0), (0, 9), (0, -12)):
        r1.cube((x, 15, z), (3, 3, 3), paint="lava", pal="glass")
    r2 = R.bone("ring2", None, pivot=(0, 24, 0))
    for (x, z) in ((7, 7), (-10, -10), (7, -10), (-10, 7)):
        r2.cube((x, 23, z), (3, 3, 3), paint="lava", pal="spark")
    R.spin("ring1", 0.09)
    R.spin("ring2", -0.14)
    R.spin("crystal", 0.03)
    R.sin("crystal", "always", "py", 0.1, 1.5)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.95), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.95))
    R.kf("crystal", "attack:lightning", "py", (0, 0), (0.3, 4), (0.9, 4), (1, 0))
    return R


# =====================================================================================================================
def hemorath():
    R = Rig("hemorath", dict(skin="#7a5568", bone="#d8cdb8", crimson="#a3122a", dark="#1a0a10", membrane="#6e1630", heart="#ff3355"),
            glow="#ff3355")
    body = R.bone("body", None, pivot=(0, 26, 0))
    body.cube((-6, 22, -4), (12, 6, 8), paint="plate", pal="dark")
    body.cube((-7, 28, -5), (14, 14, 10), paint="rock", pal="skin", glow=0.35)
    body.cube((-3, 33, -6), (6, 6, 1), paint="lava", pal="heart")                                     # the beating heart
    body.cube((-9, 41, -4), (18, 3, 8), paint="plate", pal="bone")                                    # collar bones
    for k in range(3):
        body.cube((-8, 29 + k * 4, -6), (16, 1, 1), paint="plate", pal="bone")                        # ribs

    head = R.bone("head", "body", pivot=(0, 43, 0))
    head.cube((-5, 43, -5), (10, 11, 10), paint="rock", pal="skin", glow=0.1,
              features=[("front", 1, 4, 3, 2, "#ff3355", True), ("front", 6, 4, 3, 2, "#ff3355", True), ("front", 3, 8, 4, 2, "#e8dcc8", False)])
    for s, sx, rz in (("r", -1, 0.4), ("l", 1, -0.4)):
        horn = R.bone("horn_" + s, "head", pivot=(4 * sx, 53, 0), rot=(0, 0, rz))
        horn.cube(mirror_cube((-6, 53, -1), (2, 9, 2), sx), (2, 9, 2), paint="plate", pal="bone")
        horn.cube(mirror_cube((-7, 62, -1), (2, 4, 2), sx), (2, 4, 2), paint="lava", pal="heart")
    hair = R.bone("hair", "head", pivot=(0, 53, 5))
    hair.cube((-4, 33, 5), (8, 20, 2), paint="plate", pal="dark")
    hair.cube((-7, 38, 5), (2, 14, 2), paint="plate", pal="dark")
    hair.cube((5, 38, 5), (2, 14, 2), paint="plate", pal="dark")

    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "body", pivot=(9 * sx, 40, 0))
        arm.cube(mirror_cube((-12, 27, -2), (4, 13, 4), sx), (4, 13, 4), paint="rock", pal="skin", glow=0.2)
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(9 * sx, 27, 0))
        fore.cube(mirror_cube((-12, 12, -2), (4, 15, 4), sx), (4, 15, 4), paint="rock", pal="skin", glow=0.3)
        fore.cube(mirror_cube((-13, 6, -3), (6, 6, 6), sx), (6, 6, 6), paint="plate", pal="dark")
        for k in range(3):
            fore.cube(mirror_cube((-13 + 2 * k, 0, -1), (2, 6, 2), sx), (2, 6, 2), paint="plate", pal="bone")
        wing = R.bone("wing_" + s, "body", pivot=(5 * sx, 38, 5))
        wing.cube(mirror_cube((-34, 36, 4), (30, 3, 3), sx), (30, 3, 3), paint="plate", pal="bone")
        wing.cube(mirror_cube((-34, 14, 5), (30, 22, 1), sx), (30, 22, 1), paint="rock", pal="membrane", glow=0.55)
        wing2 = R.bone("wing2_" + s, "wing_" + s, pivot=(34 * sx, 37, 5))
        wing2.cube(mirror_cube((-50, 34, 4), (16, 2, 2), sx), (16, 2, 2), paint="plate", pal="bone")
        wing2.cube(mirror_cube((-50, 18, 5), (16, 16, 1), sx), (16, 16, 1), paint="rock", pal="membrane", glow=0.5)

        leg = R.bone("leg_" + s, None, pivot=(4 * sx, 22, 0))
        leg.cube(mirror_cube((-6, 11, -2), (4, 12, 4), sx), (4, 12, 4), paint="rock", pal="skin")
        shin = R.bone("shin_" + s, "leg_" + s, pivot=(4 * sx, 11, 0))
        shin.cube(mirror_cube((-6, 2, -1), (4, 10, 4), sx), (4, 10, 4), paint="rock", pal="skin", glow=0.2)
        shin.cube(mirror_cube((-7, 0, -5), (6, 2, 8), sx), (6, 2, 8), paint="plate", pal="dark")
    cape = R.bone("cape", "body", pivot=(0, 41, 5))
    cape.cube((-7, 8, 5), (14, 33, 1), paint="rock", pal="crimson", glow=0.15)

    R.humanoid_walk(amp=0.8, arm_amp=0.5)
    R.walk("shin_r", "rx", 0.5, 1.57)
    R.walk("shin_l", "rx", 0.5, 1.57 + PI)
    R.look("head")
    R.sin("body", "idle", "py", 0.1, 0.5)
    R.sin("cape", "always", "rx", 0.11, 0.09)
    R.sin("hair", "always", "rx", 0.13, 0.14, 0.6)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("wing_" + s, "always", "rz", 0.12, 0.1 * sg)
        R.sin("wing2_" + s, "always", "rz", 0.12, 0.18 * sg, 0.8)
        R.sin("arm_" + s, "idle", "rx", 0.07, 0.05, 0.0 if s == "r" else PI)

    # leech: arms reaching forward, wings spread wide
    arms(R, "leech", rx=-1.5, a=0.2, b=0.92, fore=-0.3)
    for s, sg in (("r", 1), ("l", -1)):
        hold(R, "wing_" + s, "leech", "rz", 0.55 * sg, 0.2, 0.92)
    hold(R, "head", "leech", "rx", -0.15, 0.2, 0.92)
    # leap: crouch, spring, wings up
    R.kf("body", "attack:leap", "py", (0, 0), (0.45, -7), (0.55, 4), (0.85, 0), (1, 0))
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("leg_" + s, "attack:leap", "rx", (0, 0), (0.45, -0.9), (0.55, 0.3), (1, 0))
        R.kf("wing_" + s, "attack:leap", "rz", (0, 0), (0.45, -0.3 * sg), (0.6, 0.9 * sg), (1, 0))
        R.kf("arm_" + s, "attack:leap", "rx", (0, 0), (0.45, 0.6), (0.6, -2.4), (1, 0))
    # reflect: wings wrap around the body, arms crossed
    for s, sg in (("r", 1), ("l", -1)):
        hold(R, "wing_" + s, "reflect", "rz", -1.1 * sg, 0.15, 0.9)
        hold(R, "wing2_" + s, "reflect", "rz", -1.3 * sg, 0.15, 0.9)
    arms(R, "reflect", rx=-1.1, rz=-0.7, a=0.15, b=0.9)
    # summon / roar / pools: arms spread, head back, wings up
    for anim in ("summon", "roar", "pools"):
        arms(R, anim, rx=-0.6, rz=1.0, a=0.2, b=0.85)
        hold(R, "head", anim, "rx", -0.6, 0.2, 0.85)
        for s, sg in (("r", 1), ("l", -1)):
            hold(R, "wing_" + s, anim, "rz", 0.6 * sg, 0.2, 0.85)
    # melee
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.5), (1, 0))
    R.kf("arm_l", "attack:melee", "rx", (0, 0), (0.5, -1.8), (0.7, -0.3), (1, 0))
    R.kf("body", "attack:melee", "ry", (0, 0), (0.35, 0.4), (0.55, -0.45), (1, 0))
    # ring (blood nova) and slam-like attacks
    arms(R, "ring", rx=-2.7, a=0.5, b=0.6)
    R.kf("body", "attack:ring", "py", (0, 0), (0.5, 4), (0.58, -6), (0.85, -4), (1, 0))
    # exposed: crouched and panting
    hold(R, "body", "exposed", "py", -5, 0.15, 0.9)
    hold(R, "head", "exposed", "rx", 0.6, 0.15, 0.9)
    arms(R, "exposed", rx=0.3, rz=0.3, a=0.15, b=0.9)
    # spawn: rises out of the ground; death: collapses
    R.kf("root", "spawn", "py", (0, -70), (0.25, -70), (0.85, -4), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "spawn", "rx", (0, -2.4), (0.6, -2.4), (1, 0))
    R.kf("root", "dead", "py", (0, 0), (0.6, -6), (1, -30))
    R.kf("body", "dead", "rx", (0, 0), (1, 0.8))
    R.kf("head", "dead", "rx", (0, 0), (1, 1.0))
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("wing_" + s, "dead", "rz", (0, 0), (1, -1.2 * sg))
    return R


def blood_bat():
    R = Rig("blood_bat", dict(skin="#5a2a3a", membrane="#7a1630", dark="#1a0a10", heart="#ff3355"), glow="#ff3355")
    body = R.bone("body", None, pivot=(0, 5, 0))
    body.cube((-2, 3, -2), (4, 5, 4), paint="rock", pal="skin", glow=0.2)
    head = R.bone("head", "body", pivot=(0, 8, 0))
    head.cube((-2, 8, -2), (4, 4, 4), paint="rock", pal="skin", features=[("front", 0, 1, 1, 1, "#ff3355", True), ("front", 3, 1, 1, 1, "#ff3355", True)])
    head.cube((-3, 11, 0), (2, 3, 1), paint="plate", pal="dark")
    head.cube((1, 11, 0), (2, 3, 1), paint="plate", pal="dark")
    for s, sx in (("r", -1), ("l", 1)):
        wing = R.bone("wing_" + s, "body", pivot=(2 * sx, 7, 0))
        wing.cube(mirror_cube((-12, 2, 0), (10, 8, 1), sx), (10, 8, 1), paint="rock", pal="membrane", glow=0.3)
        wing.cube(mirror_cube((-12, 9, 0), (10, 1, 1), sx), (10, 1, 1), paint="plate", pal="dark")
    R.look("head")
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("wing_" + s, "always", "rz", 0.7, 0.8 * sg)
    R.sin("body", "always", "py", 0.35, 0.8)
    R.kf("body", "attack:melee", "rx", (0, 0), (0.4, 0.5), (0.6, -0.5), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.9))
    return R


# =====================================================================================================================
def chronos():
    R = Rig("chronos", dict(brass="#d9b24a", teal="#2a5a66", sand="#e8d9a8", dark="#14262c", dial="#e8d9a8", glass="#3ec6b0"),
            glow="#3ec6b0")
    layers = [(0, 5, 8), (5, 6, 12), (11, 7, 16), (18, 8, 20)]
    for i, (y, h, w) in enumerate(layers):
        b = R.bone("robe%d" % i, None, pivot=(0, y + h / 2.0, 0))
        b.cube((-w // 2, y, -w // 2), (w, h, w), paint="plate", pal="dark" if i % 2 == 0 else "teal", glow=0.1)
        b.cube((-w // 2 - 1, y + h - 1, -w // 2 - 1), (w + 2, 1, w + 2), paint="plate", pal="brass")      # brass trim

    torso = R.bone("torso", None, pivot=(0, 30, 0))
    torso.cube((-6, 26, -4), (12, 8, 8), paint="plate", pal="teal", features=[("front", 2, 2, 8, 4, "#e8d9a8", True)])   # lower bulb + sand
    torso.cube((-2, 34, -2), (4, 3, 4), paint="plate", pal="brass")                                                       # neck of the glass
    torso.cube((-6, 37, -4), (12, 8, 8), paint="plate", pal="teal", features=[("front", 2, 2, 8, 2, "#e8d9a8", True)])    # upper bulb
    for (x, z) in ((-8, -5), (6, -5), (-8, 3), (6, 3)):
        torso.cube((x, 26, z), (2, 19, 2), paint="plate", pal="brass")                                                   # cage posts
    torso.cube((-8, 45, -5), (16, 2, 10), paint="plate", pal="brass")
    torso.cube((-8, 24, -5), (16, 2, 10), paint="plate", pal="brass")

    head = R.bone("head", "torso", pivot=(0, 47, 0))
    head.cube((-6, 47, -3), (12, 12, 6), paint="clock", pal="brass")                                  # the clock-face head
    halo = R.bone("halo", "head", pivot=(0, 53, 0))
    for k in range(12):
        a = k * PI / 6
        halo.cube((int(round(14 * math.sin(a))) - 1, 53 + int(round(14 * math.cos(a))) - 1, 4), (3, 3, 2), paint="plate", pal="brass", glow=0.2)
    halo2 = R.bone("halo2", "head", pivot=(0, 53, 0))
    for k in range(8):
        a = k * PI / 4 + 0.4
        halo2.cube((int(round(9 * math.sin(a))) - 1, 53 + int(round(9 * math.cos(a))) - 1, 7), (3, 3, 1), paint="lava", pal="glass")

    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(10 * sx, 44, 0))
        arm.cube(mirror_cube((-15, 31, -3), (5, 13, 6), sx), (5, 13, 6), paint="plate", pal="dark", glow=0.15)
        arm.cube(mirror_cube((-16, 30, -4), (7, 3, 8), sx), (7, 3, 8), paint="plate", pal="brass")
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(10 * sx, 30, 0))
        fore.cube(mirror_cube((-15, 17, -3), (5, 13, 6), sx), (5, 13, 6), paint="plate", pal="teal", glow=0.2)
        fore.cube(mirror_cube((-15, 11, -3), (5, 6, 6), sx), (5, 6, 6), paint="lava", pal="glass")

    pend = R.bone("pendulum", None, pivot=(0, 26, 0))
    pend.cube((-1, 8, -1), (2, 18, 2), paint="plate", pal="brass")
    pend.cube((-4, 3, -4), (8, 6, 8), paint="lava", pal="glass")
    gears = R.bone("gears", None, pivot=(0, 40, 0))
    for (x, y, z) in ((22, 36, 0), (-24, 42, 0), (0, 38, 22)):
        gears.cube((x - 3, y, z - 3), (6, 6, 2), paint="plate", pal="brass", glow=0.2)
        gears.cube((x - 1, y + 2, z - 3), (2, 2, 3), paint="lava", pal="glass")

    R.sin("root", "always", "py", 0.06, 1.4)
    R.look("head")
    R.spin("halo", 0.03, axis="rz")
    R.spin("halo2", -0.06, axis="rz")
    R.spin("gears", 0.05)
    R.sin("pendulum", "always", "rz", 0.1, 0.5)
    for i in range(4):
        R.spin("robe%d" % i, 0.02 * (1 + i) * (1 if i % 2 == 0 else -1))
    R.walk("torso", "rx", 0.18, PI / 2, speed=0.0)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.05, 0.06, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.05 * sg)

    # rewind: arms sweep backwards in circles, the halo spins backwards
    for s, ph in (("r", 0.0), ("l", PI)):
        R.sin("arm_" + s, "attack:rewind", "rx", 0.5, 1.3, ph, -1.2)
    R.spin("halo", -0.5, axis="rz", when="attack:rewind")
    hold(R, "head", "rewind", "rx", -0.3, 0.15, 0.9)
    # stop: one palm forward, the other raised
    hold(R, "arm_r", "stop", "rx", -1.7, 0.2, 0.9)
    hold(R, "arm_l", "stop", "rx", -2.8, 0.2, 0.9)
    hold(R, "forearm_r", "stop", "rx", -0.3, 0.2, 0.9)
    # blink: shrinks to a point and returns
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    # orbs / spiral / ring / roar / exposed / melee
    arms(R, "orbs", rx=-1.1, rz=-0.2, a=0.15, b=0.9, fore=-0.8)
    arms(R, "spiral", rx=-0.3, rz=1.2, a=0.18, b=0.82)
    R.kf("torso", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    arms(R, "ring", rx=-2.7, a=0.5, b=0.6)
    R.kf("root", "attack:ring", "py", (0, 0), (0.5, 4), (0.58, -4), (1, 0))
    arms(R, "roar", rx=-0.4, rz=1.0, a=0.2, b=0.8)
    hold(R, "head", "roar", "rx", -0.6, 0.15, 0.85)
    arms(R, "exposed", rx=-0.5, rz=1.1, a=0.15, b=0.9)
    for ax in ("sx", "sz"):
        hold(R, "torso", "exposed", ax, 0.18, 0.15, 0.9)
    hold(R, "head", "exposed", "rx", -0.5, 0.15, 0.9)
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.0), (0.55, -0.4), (1, 0))
    spawn_dead_dissolve(R)
    return R
