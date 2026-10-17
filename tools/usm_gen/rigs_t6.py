"""Tier 6 models: Boreus (frost), Morbane (plague), Astraeus (cosmic), Eternus (every colour at once) and their creatures.

Conventions (see rig.py): pixels, feet-up, front = -z, right side = -x. Arm rz > 0 swings the RIGHT arm outward, rz < 0 the left."""
import math
from .rig import Rig, PI
from .rigs_t4 import hold, arms, mirror_cube, spawn_dead_dissolve
from .rigs_t5 import _ring, _breathe
from .rigs_mobs import beast, brute, floater


def _giant_pose(R, extra_slam=()):
    """The attack poses every two-armed walking boss shares (slam / icicles / fan / summon / charge / exposed ...)."""
    for anim in ("slam", "ring", "icicles", "glaciate") + tuple(extra_slam):
        for s in ("r", "l"):
            R.kf("arm_" + s, "attack:" + anim, "rx", (0, 0), (0.55, -3.0), (0.64, -0.3), (0.85, -0.3), (1, 0))
        R.kf("torso", "attack:" + anim, "rx", (0, 0), (0.5, -0.25), (0.64, 0.5), (0.9, 0.2), (1, 0))
        R.kf("root", "attack:" + anim, "py", (0, 0), (0.55, 4), (0.64, -3), (1, 0))
    for anim in ("fan", "orbs", "spiral", "beam", "leech", "spores", "tether", "flash"):
        arms(R, anim, rx=-1.5, a=0.2, b=0.9)
    R.kf("torso", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    for anim in ("summon", "roar", "meteors", "pools", "whiteout", "infest", "constellation", "planets", "supernova", "lightning", "pillars", "gust", "stop"):
        arms(R, anim, rx=-2.7, rz=0.6, a=0.25, b=0.85)
        hold(R, "head", anim, "rx", -0.5, 0.25, 0.85)
        hold(R, "root", anim, "py", 5, 0.25, 0.85)
    arms(R, "cage", rx=-0.4, rz=1.2, a=0.2, b=0.85)
    arms(R, "reflect", rx=-1.1, rz=-0.7, a=0.15, b=0.9)
    arms(R, "exposed", rx=0.35, rz=0.3, a=0.15, b=0.9)
    hold(R, "head", "exposed", "rx", 0.6, 0.15, 0.9)
    hold(R, "root", "exposed", "py", -5, 0.15, 0.9)
    hold(R, "root", "perch", "py", -4, 0.15, 0.9)
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.5), (1, 0))
    R.kf("torso", "attack:melee", "ry", (0, 0), (0.35, 0.45), (0.55, -0.5), (1, 0))
    R.kf("torso", "attack:charge", "rx", (0, 0), (0.4, 0.55), (0.9, 0.5), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    for anim in ("leap",):
        R.kf("root", "attack:" + anim, "py", (0, 0), (0.4, -3), (0.55, 5), (0.8, 1), (1, 0))
    # spawn: climbs out of the altar; death: comes apart
    R.kf("root", "spawn", "py", (0, -90), (0.25, -90), (0.9, -4), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "spawn", "rx", (0, -2.4), (0.6, -2.4), (1, 0))
    R.kf("root", "dead", "py", (0, 0), (0.6, -6), (1, -46))
    R.kf("torso", "dead", "rx", (0, 0), (1, 0.7))
    R.kf("head", "dead", "rx", (0, 0), (1, 1.0))


def _walker(R, torso_y):
    R.look("head")
    R.sin("root", "always", "py", 0.05, 0.8)
    R.humanoid_walk(leg_l="leg_l", leg_r="leg_r", arm_l="arm_l", arm_r="arm_r", amp=0.65, arm_amp=0.35)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.06, 0.05, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.05 * sg)


# =====================================================================================================================
def boreus():
    R = Rig("boreus", dict(ice="#cfeeff", blue="#3d86c4", deep="#12365c", white="#ffffff", rime="#7fe8ff", dark="#0a1626"), glow="#8ff0ff")
    torso = R.bone("torso", None, pivot=(0, 36, 0))
    torso.cube((-10, 24, -6), (20, 20, 12), paint="rock", pal="ice", glow=0.3)
    torso.cube((-8, 27, -7), (16, 13, 1), paint="plate", pal="blue")                                     # breastplate of frozen steel
    torso.cube((-3, 31, -8), (6, 6, 1), paint="lava", pal="rime")                                        # the frozen heart
    torso.cube((-12, 41, -8), (24, 4, 16), paint="plate", pal="deep", glow=0.15)                          # gorget
    torso.cube((-9, 22, -5), (18, 3, 10), paint="plate", pal="deep")                                     # belt
    for x in (-8, -3, 2, 7):
        torso.cube((x, 40, 5), (2, 9, 2), paint="lava", pal="white")                                     # a ridge of ice down the back
    cape = R.bone("cape", "torso", pivot=(0, 42, 7))
    cape.cube((-11, 8, 7), (22, 34, 1), paint="rock", pal="white", glow=0.1)
    head = R.bone("head", "torso", pivot=(0, 46, 0))
    head.cube((-5, 46, -5), (10, 10, 10), paint="rock", pal="ice", glow=0.15,
              features=[("front", 1, 4, 3, 2, "#ffffff", True), ("front", 6, 4, 3, 2, "#ffffff", True)])
    head.cube((-4, 39, -6), (8, 7, 3), paint="rock", pal="white")                                       # a beard of icicles
    head.cube((-3, 34, -6), (2, 6, 2), paint="lava", pal="white")
    head.cube((1, 34, -6), (2, 6, 2), paint="lava", pal="white")
    for k, (x, h) in enumerate(((-6, 8), (-3, 12), (0, 15), (3, 12), (6, 8))):
        head.cube((x - 1, 56, -1), (2, h, 2), paint="lava", pal="white" if k % 2 else "rime")             # the crown
    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(14 * sx, 42, 0))
        arm.cube(mirror_cube((-22, 38, -6), (12, 8, 12), sx), (12, 8, 12), paint="plate", pal="blue", glow=0.2)       # pauldron
        arm.cube(mirror_cube((-19, 46, -2), (4, 11, 4), sx), (4, 11, 4), paint="lava", pal="white")                   # a spike
        arm.cube(mirror_cube((-20, 24, -4), (8, 14, 8), sx), (8, 14, 8), paint="rock", pal="ice", glow=0.25)
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(14 * sx, 24, 0))
        fore.cube(mirror_cube((-20, 12, -4), (8, 12, 8), sx), (8, 12, 8), paint="rock", pal="ice", glow=0.25)
        fore.cube(mirror_cube((-22, 2, -5), (12, 10, 10), sx), (12, 10, 10), paint="lava", pal="rime")             # a fist of ice
        leg = R.bone("leg_" + s, None, pivot=(6 * sx, 24, 0))
        leg.cube(mirror_cube((-10, 8, -4), (8, 16, 8), sx), (8, 16, 8), paint="rock", pal="ice", glow=0.2)
        leg.cube(mirror_cube((-11, 0, -6), (10, 8, 12), sx), (10, 8, 12), paint="plate", pal="blue")
    shards = R.bone("shards", None, pivot=(0, 36, 0))                                                   # icicles circling the waist
    for k in range(12):
        a = k * 2 * PI / 12
        shards.cube((int(round(22 * math.cos(a))) - 1, 30 + (k % 3) * 6, int(round(22 * math.sin(a))) - 1), (2, 9, 2), paint="lava", pal="rime" if k % 2 else "white")
    flakes = R.bone("flakes", None, pivot=(0, 40, 0))
    _ring(flakes, 10, 30, 36, (3, 3, 3), "lava", "white", phase=0.3)

    _walker(R, 36)
    R.spin("shards", 0.04)
    R.spin("flakes", -0.025)
    R.sin("cape", "always", "rx", 0.09, 0.05)
    _breathe(R, "torso", 0.012)
    _giant_pose(R)
    R.spin("shards", 0.3, when="attack:icicles")
    R.spin("shards", 0.3, when="attack:glaciate")
    for ax in ("sx", "sy", "sz"):
        hold(R, "shards", "whiteout", ax, 0.5, 0.25, 0.85)
    return R


def frost_wraith():
    return floater("rime_wraith", dict(body="#cfeeff", accent="#7fe8ff"), "#9fe8ff", core=(6, 9, 6), y=15, halo=6, puffs=4, eyes="#ffffff")


def glacial_brute():
    return brute("glacial_brute", dict(body="#cfeeff", accent="#3d86c4", core="#7fe8ff"), "#8ff0ff", h=48, bulk=1.25, spikes=5, eyes="#ffffff", cracks=0.3)


def _node(rid, pals, glow, crystal_pal, ring_pal, spin=0.1):
    """A shield pylon: a plinth, a hovering crystal and two orbiting rings (Boreus's totems, Morbane's hearts, Astraeus's stars)."""
    R = Rig(rid, pals, glow=glow)
    base = R.bone("base", None, pivot=(0, 0, 0))
    base.cube((-7, 0, -7), (14, 4, 14), paint="rock", pal="body", glow=0.2)
    base.cube((-4, 4, -4), (8, 24, 8), paint="plate", pal="body", glow=0.5)
    crystal = R.bone("crystal", None, pivot=(0, 36, 0))
    crystal.cube((-6, 28, -6), (12, 14, 12), paint="lava", pal=crystal_pal)
    crystal.cube((-2, 42, -2), (4, 8, 4), paint="lava", pal=ring_pal)
    r1 = R.bone("ring1", None, pivot=(0, 18, 0))
    for (x, z) in ((11, 0), (-14, 0), (0, 11), (0, -14)):
        r1.cube((x, 17, z), (3, 3, 3), paint="lava", pal=ring_pal)
    r2 = R.bone("ring2", None, pivot=(0, 30, 0))
    for (x, z) in ((9, 9), (-12, -12), (9, -12), (-12, 9)):
        r2.cube((x, 29, z), (3, 3, 3), paint="lava", pal=crystal_pal)
    R.spin("ring1", spin)
    R.spin("ring2", -spin * 1.5)
    R.spin("crystal", 0.03)
    R.sin("crystal", "always", "py", 0.1, 1.5)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (0.7, -0.1), (1, -0.95))
    for anim in ("beam", "orbs", "fan"):
        for ax in ("sx", "sy", "sz"):
            R.kf("crystal", "attack:" + anim, ax, (0, 0), (0.4, 0.3), (0.6, 0), (1, 0))
    return R


def rime_anchor():
    return _node("rime_anchor", dict(body="#3d86c4", rime="#7fe8ff", white="#ffffff"), "#8ff0ff", "rime", "white", spin=0.09)


# =====================================================================================================================
def morbane():
    R = Rig("morbane", dict(flesh="#7d9a36", moss="#56702a", cap="#b04d72", spore="#c6ff3a", bone="#e6e0c4", dark="#14180a", glow="#a6e22e"), glow="#b6f03a")
    torso = R.bone("torso", None, pivot=(0, 30, 0))
    torso.cube((-13, 16, -8), (26, 26, 16), paint="skin", pal="flesh", glow=0.1)                           # a mound of rot
    torso.cube((-9, 24, -9), (18, 10, 1), paint="skin", pal="moss")
    torso.cube((-5, 27, -10), (10, 8, 1), paint="lava", pal="spore")                                      # a swollen sac in the chest
    for k, (x, y, z) in enumerate(((-9, 40, 2), (7, 40, -3), (-2, 41, 5), (9, 36, 6), (-11, 35, -5))):
        torso.cube((x - 2, y, z - 2), (5, 4, 5), paint="skin", pal="cap", glow=0.0)                         # shelf fungi
        torso.cube((x - 1, y + 4, z - 1), (3, 2, 3), paint="lava", pal="spore")
    head = R.bone("head", "torso", pivot=(0, 42, 0))
    head.cube((-5, 40, -5), (10, 9, 10), paint="skin", pal="flesh",
              features=[("front", 1, 4, 3, 2, "#c6ff3a", True), ("front", 6, 4, 3, 2, "#c6ff3a", True), ("front", 2, 1, 6, 1, "#14180a", False)])
    head.cube((-3, 49, -3), (6, 6, 6), paint="skin", pal="moss")                                        # stalk
    head.cube((-14, 54, -14), (28, 5, 28), paint="skin", pal="cap")                                     # the cap: a great flat mushroom hat
    head.cube((-11, 59, -11), (22, 4, 22), paint="skin", pal="cap")
    head.cube((-7, 63, -7), (14, 3, 14), paint="skin", pal="cap")
    for (x, z) in ((-10, -10), (8, -10), (-10, 8), (8, 8), (-1, -12), (-1, 11), (-12, -1), (11, -1)):
        head.cube((x, 62, z), (2, 2, 2), paint="lava", pal="bone")                                      # pale warts on the cap
    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(17 * sx, 38, 0))
        arm.cube(mirror_cube((-24, 36, -6), (11, 8, 12), sx), (11, 8, 12), paint="skin", pal="cap")                    # a shoulder cap
        arm.cube(mirror_cube((-22, 22, -4), (8, 15, 8), sx), (8, 15, 8), paint="skin", pal="flesh")
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(17 * sx, 22, 0))
        fore.cube(mirror_cube((-22, 6, -4), (8, 16, 8), sx), (8, 16, 8), paint="skin", pal="moss")
        fore.cube(mirror_cube((-24, -3, -6), (12, 10, 12), sx), (12, 10, 12), paint="lava", pal="spore")           # a bloated fist of spores
        leg = R.bone("leg_" + s, None, pivot=(7 * sx, 18, 0))
        leg.cube(mirror_cube((-11, 4, -5), (9, 14, 10), sx), (9, 14, 10), paint="skin", pal="moss")
        leg.cube(mirror_cube((-13, 0, -7), (13, 5, 14), sx), (13, 5, 14), paint="skin", pal="flesh")
    spores = R.bone("spores", None, pivot=(0, 36, 0))
    for k in range(14):
        a = k * 2 * PI / 14
        spores.cube((int(round(26 * math.cos(a))) - 1, 24 + (k % 4) * 7, int(round(26 * math.sin(a))) - 1), (3, 3, 3), paint="lava", pal="spore" if k % 3 else "bone")

    _walker(R, 30)
    R.spin("spores", 0.03)
    _breathe(R, "torso", 0.025, 0.1)
    _giant_pose(R)
    for ax in ("sx", "sy", "sz"):
        hold(R, "torso", "spores", ax, 0.18, 0.3, 0.8)
        hold(R, "torso", "infest", ax, 0.22, 0.3, 0.9)
    return R


def spore_pod():
    return beast("spore_pod", dict(body="#6a8a2a", spike="#c6ff3a", head="#8a2f4f"), "#c6ff3a", body=(7, 7, 8), leg_h=3, head=(4, 4, 4), legs=4, spikes=3, eyes="#ffffff", bright=0.35)


def blight_crawler():
    return beast("blight_crawler", dict(body="#4a5e22", spike="#c6ff3a", head="#8a2f4f"), "#c6ff3a", body=(12, 8, 18), leg_h=6, head=(7, 6, 7), legs=6, spikes=5, fangs=True,
                 eyes="#c6ff3a", bright=0.3)


def mycelid():
    return floater("mycelid", dict(body="#8a2f4f", accent="#c6ff3a"), "#c6ff3a", core=(8, 7, 8), y=14, puffs=6, halo=5, eyes="#ffffff")


def blight_heart():
    return _node("blight_heart", dict(body="#5b6e2a", spore="#c6ff3a", bone="#d9d4b8"), "#c6ff3a", "spore", "bone", spin=0.07)


# =====================================================================================================================
def astraeus():
    R = Rig("astraeus", dict(robe="#161a52", star="#7b7bff", gold="#ffe9a0", white="#ffffff", void="#05061c", nebula="#b86bff", sky="#7fe8ff"), glow="#a8b4ff")
    torso = R.bone("torso", None, pivot=(0, 38, 0))
    torso.cube((-8, 24, -5), (16, 20, 10), paint="rock", pal="robe", glow=0.45)
    torso.cube((-4, 30, -6), (8, 9, 1), paint="lava", pal="star")                                         # a galaxy where the heart should be
    torso.cube((-2, 33, -7), (4, 3, 1), paint="lava", pal="white")
    torso.cube((-10, 42, -6), (20, 3, 12), paint="plate", pal="gold")                                     # mantle
    robe = R.bone("robe", "torso", pivot=(0, 26, 0))
    robe.cube((-10, 4, -6), (20, 22, 12), paint="rock", pal="robe", glow=0.35)                           # no legs: the robe ends in a tatter of night
    for k in range(5):
        robe.cube((-9 + k * 4, -4, -5 + (k % 2) * 4), (4, 9, 3), paint="rock", pal="void", glow=0.4)
    head = R.bone("head", "torso", pivot=(0, 45, 0))
    head.cube((-4, 45, -4), (8, 9, 8), paint="plate", pal="void",
              features=[("front", 1, 4, 2, 1, "#ffffff", True), ("front", 5, 4, 2, 1, "#ffffff", True)])
    for k, (x, h) in enumerate(((-5, 5), (-2, 9), (1, 12), (4, 9), (7, 5))):
        head.cube((x - 2, 54, -1), (2, h, 2), paint="lava", pal="gold" if k % 2 else "white")             # a crown of stars
    for s, sx in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "torso", pivot=(12 * sx, 42, 0))
        arm.cube(mirror_cube((-17, 38, -4), (8, 8, 8), sx), (8, 8, 8), paint="plate", pal="gold", glow=0.2)
        arm.cube(mirror_cube((-16, 26, -3), (6, 13, 6), sx), (6, 13, 6), paint="rock", pal="robe", glow=0.35)
        fore = R.bone("forearm_" + s, "arm_" + s, pivot=(12 * sx, 26, 0))
        fore.cube(mirror_cube((-16, 14, -3), (6, 13, 6), sx), (6, 13, 6), paint="rock", pal="robe", glow=0.35)
        fore.cube(mirror_cube((-17, 8, -4), (8, 7, 8), sx), (8, 7, 8), paint="lava", pal="star")           # a hand holding a small sun
    # the orrery: three tilted rings, each carrying a planet
    ring1 = R.bone("ring1", None, pivot=(0, 40, 0))
    _ring(ring1, 18, 26, 38, (3, 2, 3), "lava", "star")
    ring1.cube((24, 36, -3), (6, 6, 6), paint="lava", pal="sky")
    ring2 = R.bone("ring2", None, pivot=(0, 40, 0), rot=(0.9, 0, 0))
    _ring(ring2, 16, 20, 38, (2, 2, 3), "lava", "nebula")
    ring2.cube((-23, 36, -3), (5, 5, 5), paint="lava", pal="gold")
    ring3 = R.bone("ring3", None, pivot=(0, 40, 0), rot=(0, 0, 0.9))
    _ring(ring3, 14, 33, 38, (2, 3, 2), "lava", "white")
    ring3.cube((0, 36, 31), (7, 7, 7), paint="lava", pal="nebula")
    sparks = R.bone("sparks", None, pivot=(0, 44, 0))
    for (x, y, z) in ((15, 62, 8), (-18, 58, -6), (6, 70, -12), (-8, 66, 14), (22, 50, -14), (-22, 46, 12)):
        sparks.cube((x - 1, y, z - 1), (2, 2, 2), paint="lava", pal="white")

    R.look("head")
    R.sin("root", "always", "py", 0.05, 2.0)
    R.spin("ring1", 0.03)
    R.spin("ring2", -0.045)
    R.spin("ring3", 0.06)
    R.spin("sparks", 0.015)
    R.sin("robe", "always", "rx", 0.08, 0.05)
    for s, sg in (("r", 1), ("l", -1)):
        R.sin("arm_" + s, "idle", "rx", 0.06, 0.06, 0.0 if s == "r" else PI)
        R.sin("arm_" + s, "idle", "rz", 0.05, 0.06 * sg)
    # planets: the rings spin up and the arms rise
    for r_ in ("ring1", "ring2", "ring3"):
        R.spin(r_, 0.4, when="attack:planets")
        R.spin(r_, 0.2, when="attack:constellation")
    arms(R, "planets", rx=-2.4, rz=0.5, a=0.25, b=0.85)
    arms(R, "constellation", rx=-2.7, rz=0.8, a=0.25, b=0.85)
    hold(R, "root", "constellation", "py", 7, 0.25, 0.85)
    # meteors / supernova / roar / summon / pools: arms to the sky
    for anim in ("meteors", "supernova", "roar", "summon", "pools", "lightning", "tether"):
        arms(R, anim, rx=-2.8, rz=0.5, a=0.25, b=0.85)
        hold(R, "head", anim, "rx", -0.5, 0.25, 0.85)
        hold(R, "root", anim, "py", 6, 0.25, 0.85)
    for ax in ("sx", "sy"):
        for r_ in ("ring1", "ring2", "ring3"):
            hold(R, r_, "supernova", ax, 0.4, 0.3, 0.85)
    for anim in ("orbs", "fan", "beam", "sunder", "flash", "spiral"):
        arms(R, anim, rx=-1.6, a=0.2, b=0.9)
    R.kf("torso", "attack:spiral", "ry", (0, 0), (0.18, 0), (0.82, 6.28), (1, 6.28))
    hold(R, "arm_l", "blackhole", "rx", -2.4, 0.25, 0.9)
    hold(R, "arm_r", "blackhole", "rx", 0.4, 0.25, 0.9)
    arms(R, "exposed", rx=0.3, rz=0.3, a=0.15, b=0.9)
    hold(R, "head", "exposed", "rx", 0.6, 0.15, 0.9)
    hold(R, "root", "exposed", "py", -5, 0.15, 0.9)
    arms(R, "reflect", rx=-1.1, rz=-0.7, a=0.15, b=0.9)
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.4), (1, 0))
    spawn_dead_dissolve(R)
    return R


def star_sprite():
    return floater("star_sprite", dict(body="#161a52", accent="#7b7bff"), "#a8b4ff", core=(5, 5, 5), y=13, halo=5, puffs=3, eyes="#ffffff")


def comet_hound():
    return beast("comet_hound", dict(body="#161a52", spike="#a8b4ff", head="#2a2f8a"), "#a8b4ff", body=(10, 8, 16), leg_h=8, head=(7, 7, 8), legs=4, spikes=4, tail=6, eyes="#ffffff",
                 bright=0.3)


def astral_node():
    return _node("astral_node", dict(body="#161a52", star="#7b7bff", white="#ffffff"), "#a8b4ff", "star", "white", spin=0.12)


# =====================================================================================================================
def eternus():
    pals = dict(red="#ff4d6d", orange="#ff9f1c", yellow="#ffe14d", green="#4dff9f", blue="#4dc9ff", violet="#9b6bff", white="#ffffff", dark="#14101f",
                gold="#ffe9a8")
    R = Rig("eternus", pals, glow="#ffffff")
    names = ("red", "orange", "yellow", "green", "blue", "violet")
    # the Final Thread: a serpent that has swallowed its own tail, a wheel of light standing upright
    wheel = R.bone("wheel", None, pivot=(0, 50, 0))
    n = 24
    for k in range(n):
        a = k * 2 * PI / n
        x, y = int(round(26 * math.cos(a))), int(round(26 * math.sin(a)))
        wheel.cube((x - 3, 50 + y - 3, -3), (6, 6, 6), paint="lava", pal=names[k % 6])
    for k in range(n):
        a = (k + 0.5) * 2 * PI / n
        x, y = int(round(22 * math.cos(a))), int(round(22 * math.sin(a)))
        wheel.cube((x - 2, 50 + y - 2, -5), (4, 4, 3), paint="plate", pal="dark", glow=0.3)               # scales on the inner rim
    head = R.bone("head", "wheel", pivot=(0, 78, 0))                                                      # the head sits at the top, biting its own tail
    head.cube((-7, 74, -8), (14, 11, 12), paint="lava", pal="gold",
              features=[("front", 2, 3, 3, 3, "#ff4d6d", True), ("front", 9, 3, 3, 3, "#ff4d6d", True)])
    head.cube((-5, 70, -9), (10, 5, 4), paint="plate", pal="dark")                                        # the jaw
    for x in (-4, -1, 2):
        head.cube((x, 67, -9), (2, 4, 2), paint="lava", pal="white")                                      # fangs
    head.cube((-2, 85, -2), (4, 9, 4), paint="lava", pal="white")                                         # a crest
    core = R.bone("core", None, pivot=(0, 50, 0))
    core.cube((-6, 44, -6), (12, 12, 12), paint="lava", pal="white")                                      # the loom: a crystal where every thread meets
    core.cube((-10, 47, -2), (20, 6, 4), paint="lava", pal="blue")
    core.cube((-2, 40, -10), (4, 20, 20), paint="lava", pal="violet")
    rays = R.bone("rays", None, pivot=(0, 50, 0))
    for k in range(8):
        a = k * PI / 4
        rays.cube((int(round(14 * math.cos(a))) - 1, 50 + int(round(14 * math.sin(a))) - 1, -1), (2, 2, 2), paint="lava", pal=names[k % 6])
    threads = R.bone("threads", None, pivot=(0, 50, 0))                                                   # eight loose threads hanging below the wheel
    for k in range(8):
        x = -21 + k * 6
        threads.cube((x, 8, -1), (2, 10, 2), paint="lava", pal=names[k % 6])
        threads.cube((x, -1, -1), (2, 10, 2), paint="lava", pal=names[(k + 3) % 6])

    R.look("head", 0.3)
    R.sin("root", "always", "py", 0.045, 1.6)
    R.spin("wheel", 0.02, axis="rz")
    R.spin("rays", -0.06, axis="rz")
    R.spin("core", 0.05, axis="ry")
    R.sin("threads", "always", "rx", 0.1, 0.08)
    for ax in ("sx", "sy", "sz"):
        R.sin("core", "always", ax, 0.12, 0.05)
    # attacks: the wheel races, the core swells, the head snaps
    for anim in ("meteors", "lightning", "supernova", "constellation", "planets", "summon", "roar", "pools", "gust", "tether", "stop"):
        R.spin("wheel", 0.2, axis="rz", when="attack:" + anim)
        for ax in ("sx", "sy", "sz"):
            hold(R, "core", anim, ax, 0.35, 0.3, 0.85)
        hold(R, "root", anim, "py", 6, 0.25, 0.85)
    for anim in ("fan", "orbs", "beam", "spiral", "flash", "spores", "icicles", "glaciate", "sunder", "slam", "ring", "charge", "leech", "blackhole", "rifts", "cage"):
        R.spin("wheel", 0.1, axis="rz", when="attack:" + anim)
        hold(R, "head", anim, "rx", -0.5, 0.2, 0.85)
        for ax in ("sx", "sy", "sz"):
            hold(R, "core", anim, ax, 0.2, 0.3, 0.85)
    for anim in ("exposed", "perch"):
        hold(R, "root", anim, "py", -5, 0.15, 0.9)
        hold(R, "head", anim, "rx", 0.7, 0.15, 0.9)
    R.kf("head", "attack:melee", "rx", (0, 0), (0.35, -0.8), (0.55, 0.5), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "attack:blink", ax, (0, 0), (0.35, -0.9), (0.5, -0.9), (0.65, 0.1), (1, 0))
    spawn_dead_dissolve(R)
    return R


def loom_spindle():
    return _node("loom_spindle", dict(body="#14101f", red="#ff4d6d", blue="#4dc9ff", white="#ffffff"), "#ffffff", "white", "red", spin=0.14)
