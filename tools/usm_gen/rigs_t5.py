"""Tier 5 models: Nihilus (void), Solareon (light), the Omnarch (everything) and their creatures.

Conventions (see rig.py): pixels, feet-up, front = -z, right side = -x. Arm rz > 0 swings the RIGHT arm outward, rz < 0 the left."""
import math
from .rig import Rig, PI
from .rigs_t4 import hold, arms, mirror_cube, spawn_dead_dissolve
from .rigs_mobs import beast, brute, floater


def _ring(bone, n, radius, y, size, paint, pal, phase=0.0, glow=0.0, plane="xz", z0=0):
    """n small cubes on a circle (xz = flat halo, xy = upright disc behind the body)."""
    for k in range(n):
        a = k * 2 * PI / n + phase
        c, s = int(round(radius * math.cos(a))), int(round(radius * math.sin(a)))
        if plane == "xz":
            bone.cube((c - size[0] // 2, y, s - size[2] // 2), size, paint=paint, pal=pal, glow=glow)
        else:
            bone.cube((c - size[0] // 2, y + s - size[1] // 2, z0), size, paint=paint, pal=pal, glow=glow)


def _breathe(R, bone, amp=0.03, speed=0.12):
    for ax in ("sx", "sy", "sz"):
        R.sin(bone, "always", ax, speed, amp)


# =====================================================================================================================
def nihilus():
    R = Rig("nihilus", dict(void="#1c0f2e", crust="#3a1d63", violet="#b04bff", light="#fff3b0", bone="#d8cdb8", dark="#07000d"), glow="#d64bff")
    core = R.bone("core", None, pivot=(0, 34, 0))
    core.cube((-10, 24, -10), (20, 20, 20), paint="rock", pal="void", glow=0.45)
    core.cube((-12, 26, -8), (24, 16, 16), paint="rock", pal="crust", glow=0.4)
    core.cube((-8, 22, -8), (16, 24, 16), paint="rock", pal="crust", glow=0.4)
    core.cube((-8, 26, -12), (16, 16, 24), paint="rock", pal="void", glow=0.3)
    for (x, z, h) in ((0, 0, 11), (-6, -4, 7), (6, -4, 7), (-6, 4, 7), (6, 4, 7)):
        core.cube((x - 1, 46, z - 1), (2, h, 2), paint="rock", pal="crust", glow=0.3)               # crown of spikes
    eye = R.bone("eye", "core", pivot=(0, 34, -13))
    eye.cube((-5, 30, -13), (10, 8, 1), paint="lava", pal="light")                                  # a single white-hot eye
    eye.cube((-1, 31, -14), (2, 6, 1), paint="plate", pal="dark")                                   # slit pupil
    maw = R.bone("maw", "core", pivot=(0, 27, -13))
    maw.cube((-7, 24, -13), (14, 4, 1), paint="plate", pal="dark")
    for i in range(5):
        maw.cube((-6 + i * 3, 27, -14), (2, 3, 1), paint="bone", pal="bone")

    ring = R.bone("ring", None, pivot=(0, 34, 0))
    _ring(ring, 16, 24, 32, (6, 4, 6), "plate", "crust", glow=0.25)
    ring2 = R.bone("ring2", None, pivot=(0, 34, 0))
    _ring(ring2, 12, 17, 33, (3, 3, 3), "lava", "violet", phase=0.26)
    shards = R.bone("shards", None, pivot=(0, 36, 0))                                               # light it has swallowed, still shining
    for (x, y, z) in ((32, 30, 0), (-32, 40, 0), (0, 34, 32), (0, 44, -32), (22, 36, 22)):
        shards.cube((x - 2, y, z - 2), (4, 9, 4), paint="lava", pal="light")

    for i in range(6):
        a = i * 2 * PI / 6
        px, pz = int(round(9 * math.cos(a))), int(round(9 * math.sin(a)))
        t1 = R.bone("t%da" % i, None, pivot=(px, 25, pz))
        t1.cube((px - 2, 15, pz - 2), (4, 10, 4), paint="rock", pal="void", glow=0.3)
        t2 = R.bone("t%db" % i, "t%da" % i, pivot=(px, 15, pz))
        t2.cube((px - 2, 6, pz - 2), (4, 9, 4), paint="rock", pal="crust", glow=0.3)
        t3 = R.bone("t%dc" % i, "t%db" % i, pivot=(px, 6, pz))
        t3.cube((px - 1, -3, pz - 1), (2, 9, 2), paint="lava", pal="violet")

    R.sin("root", "always", "py", 0.06, 1.8)
    R.look("core", 0.6)
    R.spin("ring", 0.05)
    R.spin("ring2", -0.09)
    R.spin("shards", 0.035)
    _breathe(R, "core")
    for i in range(6):
        R.sin("t%da" % i, "always", "rx", 0.07, 0.18, i * 1.1)
        R.sin("t%da" % i, "always", "rz", 0.06, 0.15, i * 0.9)
        R.sin("t%db" % i, "always", "rx", 0.09, 0.3, i * 0.7 + 1)
        R.sin("t%dc" % i, "always", "rx", 0.11, 0.35, i * 0.5 + 2)

    tent = ["t%da" % i for i in range(6)]
    # blackhole: the body folds in on itself, the ring whirls, every tendril reaches up and in
    for ax in ("sx", "sy", "sz"):
        R.kf("core", "attack:blackhole", ax, (0, 0), (0.5, -0.32), (0.64, 0.25), (1, 0))
    R.spin("ring", 0.45, when="attack:blackhole")
    for t in tent:
        R.kf(t, "attack:blackhole", "rx", (0, 0), (0.4, -1.1), (0.9, -1.1), (1, 0))
    # rifts: the tendrils stab into the floor
    for i, t in enumerate(tent):
        R.kf(t, "attack:rifts", "rx", (0, 0), (0.3, -0.9), (0.52, 1.2), (0.85, 0.6), (1, 0))
    R.kf("core", "attack:rifts", "py", (0, 0), (0.3, 3), (0.52, -5), (1, 0))
    # gloom: it swells, the ring flares, the eye shuts
    for ax in ("sx", "sy", "sz"):
        hold(R, "core", "gloom", ax, 0.3, 0.25, 0.85)
    for ax in ("sx", "sz"):
        hold(R, "ring", "gloom", ax, 0.45, 0.25, 0.85)
    hold(R, "eye", "gloom", "sy", -0.85, 0.2, 0.85)
    # cage: tendrils flare outward like bars
    for i, t in enumerate(tent):
        sg = 1 if i % 2 == 0 else -1
        R.kf(t, "attack:cage", "rz", (0, 0), (0.3, 0.9 * sg), (0.9, 0.9 * sg), (1, 0))
        R.kf(t, "attack:cage", "rx", (0, 0), (0.3, -0.4), (0.9, -0.4), (1, 0))
    # projectile attacks: lunge and flare
    for anim in ("orbs", "fan", "gust", "summon", "roar", "spiral"):
        R.kf("core", "attack:" + anim, "pz", (0, 0), (0.4, 3), (0.55, -4), (0.85, -2), (1, 0))
        for ax in ("sx", "sy", "sz"):
            R.kf("core", "attack:" + anim, ax, (0, 0), (0.4, -0.12), (0.55, 0.18), (1, 0))
    R.kf("core", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    for t in tent:
        hold(R, t, "roar", "rx", -0.8, 0.2, 0.85)
    # perch / exposed: it sinks and the tendrils go slack
    for anim in ("perch", "exposed"):
        for t in tent:
            hold(R, t, anim, "rx", 0.9, 0.3, 0.9)
        hold(R, "core", anim, "rx", 0.3, 0.3, 0.9)
    hold(R, "eye", "exposed", "sx", 0.5, 0.2, 0.9)
    hold(R, "eye", "exposed", "sy", 0.5, 0.2, 0.9)
    R.kf("core", "attack:charge", "rx", (0, 0), (0.4, -0.6), (0.9, -0.6), (1, 0))
    R.kf("core", "attack:melee", "pz", (0, 0), (0.35, 3), (0.55, -7), (1, 0))
    spawn_dead_dissolve(R)
    return R


# =====================================================================================================================
def solareon():
    R = Rig("solareon", dict(gold="#ffd45e", plate="#fff0b8", white="#fff2c4", dark="#8a6a20", ray="#ffe27a", light="#ffffff", sky="#8fd9ff"),
            glow="#fff6c8")
    torso = R.bone("torso", None, pivot=(0, 34, 0))
    torso.cube((-7, 22, -4), (14, 20, 8), paint="lava", pal="plate")
    torso.cube((-3, 32, -5), (6, 6, 1), paint="lava", pal="light")                                   # chest sun
    torso.cube((-9, 40, -5), (18, 3, 10), paint="plate", pal="gold")                                 # gorget
    torso.cube((-6, 20, -3), (12, 3, 6), paint="plate", pal="gold")                                  # belt
    head = R.bone("head", "torso", pivot=(0, 43, 0))
    head.cube((-4, 43, -4), (8, 9, 8), paint="lava", pal="plate",
              features=[("front", 1, 4, 2, 1, "#ffffff", True), ("front", 5, 4, 2, 1, "#ffffff", True)])
    head.cube((-1, 52, -4), (2, 4, 8), paint="plate", pal="gold")                                    # helm crest
    halo = R.bone("halo", "head", pivot=(0, 58, 0))
    _ring(halo, 10, 8, 58, (3, 2, 3), "lava", "light")
    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(10 * sx, 40, 0))
        arm.cube(mirror_cube((-15, 37, -4), (7, 5, 8), sx), (7, 5, 8), paint="plate", pal="gold")                        # pauldron
        arm.cube(mirror_cube((-14, 27, -2), (4, 12, 4), sx), (4, 12, 4), paint="lava", pal="plate")
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(10 * sx, 27, 0))
        fore.cube(mirror_cube((-14, 15, -2), (4, 12, 4), sx), (4, 12, 4), paint="lava", pal="plate")
        fore.cube(mirror_cube((-15, 10, -3), (6, 6, 6), sx), (6, 6, 6), paint="lava", pal="ray")                         # glowing gauntlet
        leg = R.bone("leg_" + s, None, pivot=(4 * sx, 22, 0))
        leg.cube(mirror_cube((-6, 8, -2), (4, 14, 4), sx), (4, 14, 4), paint="lava", pal="plate")
        leg.cube(mirror_cube((-7, 2, -3), (6, 6, 6), sx), (6, 6, 6), paint="plate", pal="gold")                          # greave
        for k, (w, y, rz) in enumerate(((26, 40, 0.55), (23, 36, 0.12), (20, 32, -0.3))):
            wing = R.bone("wing_%s%d" % (s, k), "torso", pivot=(5 * sx, y, 4), rot=(0, 0, rz * (1 if s == "r" else -1)))
            wing.cube(mirror_cube((-5 - w, y - 1, 3), (w, 3, 1), sx), (w, 3, 1), paint="lava", pal="white")
            wing.cube(mirror_cube((-5 - w + 3, y - 3, 3), (w - 6, 2, 1), sx), (w - 6, 2, 1), paint="lava", pal="ray")
    sun = R.bone("sun", None, pivot=(0, 38, 9))
    sun.cube((-1, 21, 8), (2, 34, 1), paint="lava", pal="ray")
    sun.cube((-17, 37, 9), (34, 2, 1), paint="lava", pal="ray")
    for (x, y) in ((-10, 28), (10, 28), (-10, 46), (10, 46), (-14, 24), (14, 24), (-14, 50), (14, 50)):
        sun.cube((x - 2, y - 2, 8), (4, 4, 1), paint="lava", pal="ray")
    _ring(sun, 16, 22, 38, (3, 3, 1), "lava", "light", plane="xy", z0=10)

    R.sin("root", "always", "py", 0.06, 1.6)
    R.look("head")
    R.spin("halo", 0.08)
    R.spin("sun", 0.02, axis="rz")
    R.walk("torso", "rx", 0.16, PI / 2, speed=0.0)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.06, 0.06, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.05 * sg)
        R.sin("leg_" + s, "always", "rx", 0.07, 0.08, 0.0 if s == "r" else PI)
        for k in range(3):
            R.sin("wing_%s%d" % (s, k), "always", "rz", 0.11, 0.1 * sg, k * 0.5)

    def wings(anim, rz, a=0.2, b=0.85):
        for s, sg in (("r", 1), ("l", -1)):
            for k in range(3):
                hold(R, "wing_%s%d" % (s, k), anim, "rz", rz * sg, a, b)

    # beam: arms thrust forward, the sun disc spins up
    arms(R, "beam", rx=-1.55, a=0.2, b=0.92)
    R.spin("sun", 0.5, axis="rz", when="attack:beam")
    wings("beam", 0.4)
    # supernova: arms to the sky, the body rises, the disc swells
    arms(R, "supernova", rx=-2.9, rz=0.4, a=0.35, b=0.85)
    hold(R, "root", "supernova", "py", 9, 0.35, 0.85)
    hold(R, "head", "supernova", "rx", -0.5, 0.35, 0.85)
    for ax in ("sx", "sy"):
        hold(R, "sun", "supernova", ax, 0.5, 0.35, 0.85)
    wings("supernova", 0.8, 0.35, 0.85)
    R.spin("sun", 0.35, axis="rz", when="attack:supernova")
    # flash: arms cross over the face, the head snaps back
    arms(R, "flash", rx=-2.0, rz=-0.9, a=0.3, b=0.7, fore=-1.2)
    hold(R, "head", "flash", "rx", -0.6, 0.55, 0.8)
    # sunder: both fists up, then down hard
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:sunder", "rx", (0, 0), (0.55, -3.0), (0.64, -0.3), (0.85, -0.3), (1, 0))
    R.kf("torso", "attack:sunder", "rx", (0, 0), (0.5, -0.25), (0.64, 0.45), (0.9, 0.2), (1, 0))
    R.kf("root", "attack:sunder", "py", (0, 0), (0.55, 5), (0.64, -3), (1, 0))
    # reflect: wings wrap forward, arms crossed
    for s, sg in (("r", 1), ("l", -1)):
        for k in range(3):
            hold(R, "wing_%s%d" % (s, k), "reflect", "rz", -1.1 * sg, 0.15, 0.9)
    arms(R, "reflect", rx=-1.1, rz=-0.7, a=0.15, b=0.9)
    # projectile / summon / roar: arms spread, wings up
    for anim in ("fan", "orbs", "summon", "roar", "spiral", "lightning", "pillars", "pools", "gust"):
        arms(R, anim, rx=-0.7, rz=1.0, a=0.2, b=0.85)
        hold(R, "head", anim, "rx", -0.4, 0.2, 0.85)
        wings(anim, 0.5)
    R.kf("torso", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    # blink: shrinks and returns; exposed: kneels with wings drooping
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    wings("exposed", -0.7, 0.15, 0.9)
    hold(R, "head", "exposed", "rx", 0.6, 0.15, 0.9)
    arms(R, "exposed", rx=0.3, rz=0.3, a=0.15, b=0.9)
    hold(R, "root", "exposed", "py", -5, 0.15, 0.9)
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.4), (1, 0))
    R.kf("torso", "attack:melee", "ry", (0, 0), (0.35, 0.4), (0.55, -0.45), (1, 0))
    spawn_dead_dissolve(R)
    return R


# =====================================================================================================================
def omnarch():
    R = Rig("omnarch", dict(vd="#2b1b3d", vp="#7a3fc0", lt="#fff1c4", lw="#ffffff", dark="#0b0614", mag="#ff6ad5", cy="#7fe8ff"), glow="#ff6ad5")
    # the body is cut down the middle: the right half (x < 0) is light, the left half (x > 0) is void
    torso = R.bone("torso", None, pivot=(0, 36, 0))
    torso.cube((-11, 22, -6), (11, 22, 12), paint="lava", pal="lt")
    torso.cube((0, 22, -6), (11, 22, 12), paint="rock", pal="vd", glow=0.45)
    torso.cube((-5, 32, -7), (5, 6, 1), paint="lava", pal="cy")
    torso.cube((0, 32, -7), (5, 6, 1), paint="lava", pal="mag")
    torso.cube((-13, 42, -7), (13, 3, 14), paint="lava", pal="lw")
    torso.cube((0, 42, -7), (13, 3, 14), paint="plate", pal="dark", glow=0.2)
    head = R.bone("head", "torso", pivot=(0, 45, 0))
    head.cube((-6, 45, -5), (6, 11, 10), paint="lava", pal="lt", features=[("front", 1, 5, 3, 2, "#7fe8ff", True)])
    head.cube((0, 45, -5), (6, 11, 10), paint="rock", pal="vd", glow=0.3, features=[("front", 2, 5, 3, 2, "#ff6ad5", True)])
    head.cube((-9, 55, -1), (3, 12, 3), paint="lava", pal="lw")                                      # one horn of each kind
    head.cube((-10, 67, -1), (2, 6, 2), paint="lava", pal="cy")
    head.cube((6, 55, -1), (3, 12, 3), paint="rock", pal="dark", glow=0.3)
    head.cube((8, 67, -1), (2, 6, 2), paint="lava", pal="mag")
    for s, sx, pal_up, pal_lo in (("r", -1, "lt", "lw"), ("l", 1, "vd", "dark")):
        kind = "lava" if s == "r" else "rock"
        gl = 0.55 if s == "r" else 0.4
        arm = R.bone("arm_" + s, "torso", pivot=(15 * sx, 42, 0))
        arm.cube(mirror_cube((-22, 40, -6), (12, 7, 12), sx), (12, 7, 12), paint="plate", pal=pal_lo, glow=gl * 0.5)         # pauldron
        arm.cube(mirror_cube((-20, 25, -4), (8, 15, 8), sx), (8, 15, 8), paint=kind, pal=pal_up, glow=gl)
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(15 * sx, 25, 0))
        fore.cube(mirror_cube((-20, 10, -4), (8, 15, 8), sx), (8, 15, 8), paint=kind, pal=pal_up, glow=gl)
        fore.cube(mirror_cube((-21, 1, -5), (10, 9, 10), sx), (10, 9, 10), paint="lava", pal="cy" if s == "r" else "mag")      # a fist of pure element
        leg = R.bone("leg_" + s, None, pivot=(6 * sx, 22, 0))
        leg.cube(mirror_cube((-10, 8, -4), (8, 14, 8), sx), (8, 14, 8), paint=kind, pal=pal_up, glow=gl)
        leg.cube(mirror_cube((-11, 0, -5), (10, 8, 10), sx), (10, 8, 10), paint="plate", pal=pal_lo, glow=gl * 0.5)
    cape = R.bone("cape", "torso", pivot=(0, 44, 7))
    cape.cube((-12, 6, 7), (12, 38, 1), paint="lava", pal="lw")
    cape.cube((0, 6, 7), (12, 38, 1), paint="rock", pal="dark", glow=0.3)
    halo = R.bone("halo", None, pivot=(0, 48, 12))                                                   # a broken wheel behind it: light segments and void segments
    for k in range(20):
        a = k * 2 * PI / 20
        c, s = int(round(30 * math.cos(a))), int(round(30 * math.sin(a)))
        halo.cube((c - 2, 48 + s - 2, 12), (4, 4, 2), paint="lava" if c < 0 else "rock", pal="cy" if c < 0 else "mag", glow=0.3)
    orbs = R.bone("orbs", None, pivot=(0, 40, 0))
    for (x, y, z, pal) in ((34, 48, 0, "mag"), (-34, 34, 0, "cy"), (0, 52, 34, "mag"), (0, 30, -34, "cy")):
        orbs.cube((x - 3, y, z - 3), (6, 6, 6), paint="lava", pal=pal)

    R.look("head")
    R.sin("root", "always", "py", 0.05, 1.0)
    R.humanoid_walk(leg_l="leg_l", leg_r="leg_r", arm_l="arm_l", arm_r="arm_r", amp=0.7, arm_amp=0.4)
    R.spin("halo", 0.02, axis="rz")
    R.spin("orbs", 0.04)
    R.sin("cape", "always", "rx", 0.1, 0.07)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.06, 0.05, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.05 * sg)

    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.5), (1, 0))
    R.kf("torso", "attack:melee", "ry", (0, 0), (0.35, 0.45), (0.55, -0.5), (1, 0))
    # slam / ring / sunder / charge: both fists high, then down
    for anim in ("slam", "ring", "sunder"):
        for s in ("r", "l"):
            R.kf("arm_" + s, "attack:" + anim, "rx", (0, 0), (0.55, -3.0), (0.64, -0.3), (0.85, -0.3), (1, 0))
        R.kf("torso", "attack:" + anim, "rx", (0, 0), (0.5, -0.25), (0.64, 0.5), (0.9, 0.2), (1, 0))
        R.kf("root", "attack:" + anim, "py", (0, 0), (0.55, 4), (0.64, -3), (1, 0))
    # beam / flash / leech / fan / orbs: arms thrust forward
    for anim in ("beam", "flash", "leech", "fan", "orbs", "spiral"):
        arms(R, anim, rx=-1.5, a=0.2, b=0.9)
    R.spin("halo", 0.4, axis="rz", when="attack:beam")
    R.kf("torso", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    # supernova / lightning / meteors / pillars / pools / summon / roar / gloom / rifts: arms to the sky, body rises
    for anim in ("supernova", "lightning", "meteors", "pillars", "pools", "summon", "roar", "gloom", "rifts"):
        arms(R, anim, rx=-2.7, rz=0.5, a=0.25, b=0.85)
        hold(R, "head", anim, "rx", -0.5, 0.25, 0.85)
        hold(R, "root", anim, "py", 6, 0.25, 0.85)
    for ax in ("sx", "sy"):
        hold(R, "halo", "supernova", ax, 0.35, 0.3, 0.85)
    # blackhole: the void hand reaches, the light hand holds back
    hold(R, "arm_l", "blackhole", "rx", -2.4, 0.25, 0.9)
    hold(R, "arm_l", "blackhole", "rz", -0.3, 0.25, 0.9)
    hold(R, "arm_r", "blackhole", "rx", 0.4, 0.25, 0.9)
    # cage / gust / stop / rewind / reflect / exposed / blink
    arms(R, "cage", rx=-0.4, rz=1.2, a=0.2, b=0.85)
    arms(R, "gust", rx=-1.8, a=0.4, b=0.8)
    hold(R, "arm_r", "stop", "rx", -1.7, 0.2, 0.9)
    hold(R, "arm_l", "stop", "rx", -2.8, 0.2, 0.9)
    for s, ph in (("r", 0.0), ("l", PI)):
        R.sin("arm_" + s, "attack:rewind", "rx", 0.5, 1.3, ph, -1.2)
    arms(R, "reflect", rx=-1.1, rz=-0.7, a=0.15, b=0.9)
    arms(R, "exposed", rx=0.35, rz=0.3, a=0.15, b=0.9)
    hold(R, "head", "exposed", "rx", 0.6, 0.15, 0.9)
    hold(R, "root", "exposed", "py", -4, 0.15, 0.9)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    # spawn: climbs out of the altar; death: comes apart
    R.kf("root", "spawn", "py", (0, -90), (0.25, -90), (0.9, -4), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "spawn", "rx", (0, -2.4), (0.6, -2.4), (1, 0))
    R.kf("root", "dead", "py", (0, 0), (0.6, -6), (1, -40))
    R.kf("torso", "dead", "rx", (0, 0), (1, 0.7))
    R.kf("head", "dead", "rx", (0, 0), (1, 1.0))
    for ax in ("sx", "sy", "sz"):
        R.kf("halo", "dead", ax, (0, 0), (0.7, 0.4), (1, -0.95))
    return R


# =====================================================================================================================
def void_spawn():
    return beast("void_spawn", dict(body="#1c0f2e", spike="#b04bff", head="#2a1642"), "#d64bff", body=(7, 6, 11), leg_h=5, head=(5, 5, 6), legs=6, spikes=3,
                 fangs=True, eyes="#ff6ad5", bright=0.25)


def null_anchor():
    R = Rig("null_anchor", dict(rock="#1c0f2e", crust="#3a1d63", violet="#b04bff", light="#fff3b0"), glow="#d64bff")
    base = R.bone("base", None, pivot=(0, 0, 0))
    base.cube((-6, 0, -6), (12, 4, 12), paint="rock", pal="rock", glow=0.2)
    base.cube((-4, 4, -4), (8, 22, 8), paint="plate", pal="crust", glow=0.5)
    crystal = R.bone("crystal", None, pivot=(0, 33, 0))
    crystal.cube((-5, 27, -5), (10, 12, 10), paint="lava", pal="violet")
    crystal.cube((-2, 39, -2), (4, 7, 4), paint="lava", pal="light")
    r1 = R.bone("ring1", None, pivot=(0, 16, 0))
    for (x, z) in ((10, 0), (-13, 0), (0, 10), (0, -13)):
        r1.cube((x, 15, z), (3, 3, 3), paint="lava", pal="violet")
    r2 = R.bone("ring2", None, pivot=(0, 26, 0))
    for (x, z) in ((8, 8), (-11, -11), (8, -11), (-11, 8)):
        r2.cube((x, 25, z), (3, 3, 3), paint="lava", pal="light")
    R.spin("ring1", 0.09)
    R.spin("ring2", -0.14)
    R.spin("crystal", 0.03)
    R.sin("crystal", "always", "py", 0.1, 1.5)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.95), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.95))
    for a in ("fan", "orbs"):
        R.kf("crystal", "attack:" + a, "py", (0, 0), (0.3, 4), (0.9, 4), (1, 0))
    return R


def apex_pylon():
    R = Rig("apex_pylon", dict(vd="#2b1b3d", lt="#f3e6b0", mag="#ff6ad5", cy="#7fe8ff", dark="#0b0614"), glow="#ff6ad5")
    base = R.bone("base", None, pivot=(0, 0, 0))
    base.cube((-8, 0, -8), (8, 5, 16), paint="plate", pal="lt")
    base.cube((0, 0, -8), (8, 5, 16), paint="rock", pal="vd", glow=0.3)
    base.cube((-5, 5, -5), (5, 28, 10), paint="plate", pal="lt")
    base.cube((0, 5, -5), (5, 28, 10), paint="rock", pal="vd", glow=0.5)
    crystal = R.bone("crystal", None, pivot=(0, 40, 0))
    crystal.cube((-6, 33, -6), (6, 13, 12), paint="lava", pal="cy")
    crystal.cube((0, 33, -6), (6, 13, 12), paint="lava", pal="mag")
    r1 = R.bone("ring1", None, pivot=(0, 20, 0))
    for k in range(6):
        a = k * PI / 3
        r1.cube((int(round(13 * math.cos(a))) - 2, 19, int(round(13 * math.sin(a))) - 2), (4, 4, 4), paint="lava", pal="cy" if k % 2 == 0 else "mag")
    r2 = R.bone("ring2", None, pivot=(0, 30, 0))
    for k in range(4):
        a = k * PI / 2 + 0.5
        r2.cube((int(round(9 * math.cos(a))) - 2, 29, int(round(9 * math.sin(a))) - 2), (4, 4, 4), paint="lava", pal="mag" if k % 2 == 0 else "cy")
    R.spin("ring1", 0.08)
    R.spin("ring2", -0.12)
    R.spin("crystal", 0.04)
    R.sin("crystal", "always", "py", 0.1, 1.5)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.95), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.95))
    for a in ("beam", "orbs"):
        R.kf("crystal", "attack:" + a, "py", (0, 0), (0.3, 4), (0.9, 4), (1, 0))
    return R


def halo_blade():
    R = Rig("halo_blade", dict(steel="#fffbe0", gold="#e8c25a", ray="#ffd25a", light="#ffffff"), glow="#fff3b0")
    blade = R.bone("blade", None, pivot=(0, 14, 0))
    blade.cube((-1, 13, -17), (2, 4, 24), paint="plate", pal="steel", glow=0.1)                       # the blade, pointing forward (-z)
    blade.cube((-1, 14, -21), (2, 2, 4), paint="lava", pal="light")
    blade.cube((-5, 12, 7), (10, 6, 2), paint="plate", pal="gold")                                    # crossguard
    blade.cube((-1, 13, 9), (2, 4, 7), paint="plate", pal="gold")
    blade.cube((-2, 12, 16), (4, 6, 3), paint="lava", pal="ray")
    halo = R.bone("halo", None, pivot=(0, 15, 0))
    _ring(halo, 6, 8, 20, (2, 2, 2), "lava", "ray")
    R.sin("root", "always", "py", 0.12, 1.2)
    R.sin("blade", "always", "rz", 0.1, 0.12)
    R.spin("halo", 0.12)
    R.kf("blade", "attack:charge", "rz", (0, 0), (0.4, 6.28), (1, 6.28))
    R.kf("blade", "attack:melee", "rx", (0, 0), (0.4, 0.7), (0.55, -0.7), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.9))
    return R


def void_stalker():
    R = Rig("void_stalker", dict(skin="#120a1c", acc="#3a1d63", eye="#ff6ad5"), glow="#d64bff")
    body = R.bone("body", None, pivot=(0, 30, 0))
    body.cube((-4, 26, -2), (8, 12, 4), paint="rock", pal="skin", glow=0.2)
    body.cube((-5, 36, -3), (10, 2, 6), paint="rock", pal="acc", glow=0.3)
    head = R.bone("head", "body", pivot=(0, 38, 0))
    head.cube((-4, 38, -4), (8, 8, 8), paint="rock", pal="skin", glow=0.1,
              features=[("front", 0, 3, 3, 1, "#ff6ad5", True), ("front", 5, 3, 3, 1, "#ff6ad5", True)])
    head.cube((-3, 46, -3), (6, 3, 6), paint="rock", pal="acc", glow=0.4)
    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "body", pivot=(6 * sx, 36, 0))
        arm.cube((6 * sx - 1, 10, -1), (2, 26, 2), paint="rock", pal="skin", glow=0.15)
        arm.cube((6 * sx - 1, 6, -1), (2, 4, 2), paint="lava", pal="acc")
        leg = R.bone("leg_" + s, None, pivot=(2 * sx, 26, 0))
        leg.cube((2 * sx - 1, 0, -1), (2, 26, 2), paint="rock", pal="skin")
    R.humanoid_walk(amp=0.7, arm_amp=0.4)
    R.look("head")
    R.sin("body", "idle", "py", 0.08, 0.4)
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.3), (0.55, -0.4), (1, 0))
    R.kf("arm_l", "attack:melee", "rx", (0, 0), (0.45, -2.0), (0.65, -0.3), (1, 0))
    for a in ("blink", "leech"):
        for s in ("r", "l"):
            R.kf("arm_" + s, "attack:" + a, "rx", (0, 0), (0.3, -1.5), (0.85, -1.5), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.9))
    return R


def rift_weaver():
    R = Rig("rift_weaver", dict(skin="#1c0f2e", acc="#b04bff", light="#ffffff"), glow="#d64bff")
    body = R.bone("body", None, pivot=(0, 16, 0))
    body.cube((-4, 12, -2), (8, 8, 10), paint="rock", pal="skin", glow=0.3)
    head = R.bone("head", "body", pivot=(0, 16, -2))
    head.cube((-3, 13, -8), (6, 6, 6), paint="rock", pal="skin", glow=0.1,
              features=[("front", 0, 2, 2, 1, "#ffffff", True), ("front", 4, 2, 2, 1, "#ffffff", True)])
    for k in range(3):
        for s, sx in (("r", -1), ("l", 1)):
            leg = R.bone("leg_%s%d" % (s, k), "body", pivot=(4 * sx, 14, -1 + k * 3), rot=(0, 0, 0.6 * (1 if s == "r" else -1)))
            leg.cube((sx * 4 - 1 + (-11 if sx < 0 else 0), 4, -2 + k * 3), (12, 2, 2), paint="rock", pal="skin", glow=0.2)
    rift = R.bone("rift", None, pivot=(0, 17, 9))
    _ring(rift, 10, 8, 17, (2, 2, 1), "lava", "acc", plane="xy", z0=8)
    R.sin("root", "always", "py", 0.12, 1.0)
    R.look("head")
    R.spin("rift", 0.06, axis="rz")
    for k in range(3):
        for s, sg in (("r", 1), ("l", -1)):
            R.sin("leg_%s%d" % (s, k), "always", "rz", 0.1, 0.15 * sg, k * 0.8)
    R.kf("body", "attack:melee", "rx", (0, 0), (0.4, 0.6), (0.6, -0.6), (1, 0))
    for a in ("orbs", "rifts", "fan"):
        R.kf("head", "attack:" + a, "rx", (0, 0), (0.4, -0.5), (0.8, -0.5), (1, 0))
        for ax in ("sx", "sy", "sz"):
            R.kf("rift", "attack:" + a, ax, (0, 0), (0.5, 0.5), (0.7, 0), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (1, -0.9))
    return R


def radiant_sentinel():
    return brute("radiant_sentinel", dict(body="#d8c890", accent="#fff3b0", core="#8fd9ff"), "#fff3b0", h=52, bulk=1.25, spikes=3, eyes="#ffffff", cracks=0.2)


def prism_drone():
    return floater("prism_drone", dict(body="#e8d890", accent="#8fd9ff"), "#fff3b0", core=(6, 6, 6), y=12, halo=6, eyes="#ffffff")
