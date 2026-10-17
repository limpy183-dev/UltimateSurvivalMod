"""Tier 3 models: Ignarok, the Cinder Warden (boss) and the Ember Imp (its minion)."""
from .rig import Rig, PI

M = lambda o, s: ((-o[0] - s[0], o[1], o[2]), s)          # x-mirror of a cube (o = min corner, s = size)


def ignarok():
    R = Rig("ignarok", dict(rock="#3b2f33", plate="#5d3a36", ember="#e0561f", horn="#2a2226", lava="#ff8a2a"), glow="#ff7a1f")

    # ---- torso ----------------------------------------------------------------------------------------------
    body = R.bone("body", None, pivot=(0, 28, 0))
    body.cube((-12, 28, -8), (24, 22, 16), paint="plate", pal="plate", glow=0.35)                # chest
    body.cube((-9, 22, -6), (18, 7, 12), paint="rock", pal="rock", glow=0.2)                     # pelvis
    body.cube((-6, 33, -9), (12, 12, 1), paint="lava", pal="lava")                               # furnace core window
    body.cube((-10, 26, -7), (20, 3, 14), paint="plate", pal="rock")                             # belt
    body.cube((-9, 38, 8), (5, 16, 5), paint="rock", pal="rock", glow=0.3)                       # exhaust stacks
    body.cube((4, 38, 8), (5, 16, 5), paint="rock", pal="rock", glow=0.3)
    body.cube((-9, 52, 8), (5, 2, 5), paint="lava", pal="lava")                                  # glowing stack mouths
    body.cube((4, 52, 8), (5, 2, 5), paint="lava", pal="lava")
    body.cube((-5, 31, 8), (10, 8, 1), paint="lava", pal="lava")                                 # back furnace grate

    # ---- head ------------------------------------------------------------------------------------------------
    head = R.bone("head", "body", pivot=(0, 51, 0))
    head.cube((-6, 50, -6), (12, 12, 12), paint="plate", pal="plate", glow=0.15,
              features=[("front", 2, 5, 3, 2, "#ffd25a", True), ("front", 7, 5, 3, 2, "#ffd25a", True),
                        ("front", 3, 9, 6, 1, "#ff7a1f", True)])
    head.cube((-7, 59, -7), (14, 2, 3), paint="rock", pal="rock")                                # brow ridge
    head.cube((-5, 47, -7), (10, 3, 3), paint="rock", pal="rock", glow=0.5)                      # jaw plate
    head.cube((-1, 62, -2), (2, 5, 4), paint="rock", pal="horn")                                 # crest
    horn_r = R.bone("horn_r", "head", pivot=(-5, 60, 0), rot=(0, 0, 0.55))
    horn_r.cube((-7, 60, -2), (3, 9, 3), paint="rock", pal="horn")
    horn_r.cube((-7, 69, -1), (2, 5, 2), paint="lava", pal="lava")
    horn_l = R.bone("horn_l", "head", pivot=(5, 60, 0), rot=(0, 0, -0.55))
    horn_l.cube((4, 60, -2), (3, 9, 3), paint="rock", pal="horn")
    horn_l.cube((5, 69, -1), (2, 5, 2), paint="lava", pal="lava")

    # ---- arms ------------------------------------------------------------------------------------------------
    for side, sx in (("r", -1), ("l", 1)):
        def X(o, s):                                                       # authored for the right side, mirrored for the left
            return o if sx < 0 else M(o, s)[0]
        arm = R.bone("arm_" + side, "body", pivot=(17 * sx, 46, 0))
        arm.cube(X((-23, 45, -7), (12, 7, 14)), (12, 7, 14), paint="plate", pal="plate", glow=0.2)       # pauldron
        arm.cube(X((-22, 52, -5), (3, 7, 3)), (3, 7, 3), paint="rock", pal="horn")                       # spikes
        arm.cube(X((-22, 52, 2), (3, 7, 3)), (3, 7, 3), paint="rock", pal="horn")
        arm.cube(X((-18, 52, -1), (3, 9, 3)), (3, 9, 3), paint="rock", pal="horn")
        arm.cube(X((-22, 30, -5), (10, 15, 10)), (10, 15, 10), paint="rock", pal="rock", glow=0.4)       # upper arm
        fore = R.bone("forearm_" + side, "arm_" + side, pivot=(17 * sx, 30, 0))
        fore.cube(X((-23, 15, -6), (12, 15, 12)), (12, 15, 12), paint="plate", pal="plate", glow=0.55)   # forearm
        fore.cube(X((-24, 6, -7), (14, 9, 14)), (14, 9, 14), paint="rock", pal="rock", glow=0.5)         # fist

    # ---- legs ------------------------------------------------------------------------------------------------
    for side, sx in (("r", -1), ("l", 1)):
        def X(o, s):
            return o if sx < 0 else M(o, s)[0]
        leg = R.bone("leg_" + side, None, pivot=(7 * sx, 24, 0))
        leg.cube(X((-12, 11, -6), (10, 13, 12)), (10, 13, 12), paint="rock", pal="rock", glow=0.3)
        leg.cube(X((-12, 13, -8), (10, 6, 2)), (10, 6, 2), paint="plate", pal="plate")
        shin = R.bone("shin_" + side, "leg_" + side, pivot=(7 * sx, 11, 0))
        shin.cube(X((-12, 3, -5), (10, 8, 10)), (10, 8, 10), paint="plate", pal="plate", glow=0.3)
        shin.cube(X((-13, 0, -9), (12, 4, 15)), (12, 4, 15), paint="rock", pal="rock")

    # ---- orbiting cinders -----------------------------------------------------------------------------------------
    orbit = R.bone("orbit", None, pivot=(0, 40, 0))
    for (x, y, z) in ((30, 44, -2), (-32, 34, 4), (4, 52, -31), (-5, 30, 32), (22, 56, 22), (-24, 58, -22)):
        orbit.cube((x, y, z), (5, 5, 5), paint="lava", pal="lava")

    # ---- animation -----------------------------------------------------------------------------------------------
    R.humanoid_walk(amp=0.85, arm_amp=0.45)
    R.walk("shin_r", "rx", 0.4, 1.57)
    R.walk("shin_l", "rx", 0.4, 1.57 + PI)
    R.walk("body", "rz", 0.04)
    R.sin("body", "idle", "py", 0.09, 0.7)
    R.sin("arm_r", "idle", "rx", 0.07, 0.05)
    R.sin("arm_l", "idle", "rx", 0.07, 0.05, PI)
    R.look("head")
    R.spin("orbit", 0.06)
    R.sin("orbit", "always", "py", 0.05, 2.0)

    # melee: overhead right-hand hook
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.6), (1, 0))
    R.kf("forearm_r", "attack:melee", "rx", (0, 0), (0.35, -0.9), (0.55, -0.1), (1, 0))
    R.kf("body", "attack:melee", "ry", (0, 0), (0.35, 0.4), (0.55, -0.45), (1, 0))

    # slam (windup 0..0.6, strike at 0.6): both fists overhead, then down
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:slam", "rx", (0, 0), (0.55, -2.95), (0.62, -0.45), (0.8, -0.35), (1, 0))
        R.kf("forearm_" + s, "attack:slam", "rx", (0, 0), (0.55, -0.6), (0.62, -0.05), (1, 0))
        R.kf("arm_" + s, "attack:slam", "rz", (0, 0), (0.55, 0.25 if s == "r" else -0.25), (1, 0))
    R.kf("body", "attack:slam", "rx", (0, 0), (0.5, 0.22), (0.62, -0.32), (0.85, -0.15), (1, 0))
    R.kf("body", "attack:slam", "py", (0, 0), (0.5, 3), (0.62, -5), (0.85, -2), (1, 0))
    R.kf("head", "attack:slam", "rx", (0, 0), (0.5, 0.3), (0.62, -0.4), (1, 0))

    # fan / volley: wind back, thrust both hands forward, keep them out while firing
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:fan", "rx", (0, 0), (0.30, 0.55), (0.38, -1.55), (0.80, -1.45), (1, 0))
        R.kf("forearm_" + s, "attack:fan", "rx", (0, 0), (0.30, 0.1), (0.38, -0.45), (0.8, -0.4), (1, 0))
    R.kf("body", "attack:fan", "rx", (0, 0), (0.30, 0.2), (0.38, -0.2), (0.8, -0.15), (1, 0))

    # spiral: arms spread wide, body twirls
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:spiral", "rz", (0, 0), (0.18, 1.35 * sg), (0.82, 1.35 * sg), (1, 0))
        R.kf("arm_" + s, "attack:spiral", "rx", (0, 0), (0.18, -0.25), (0.82, -0.25), (1, 0))
    R.kf("body", "attack:spiral", "ry", (0, 0), (0.18, 0.0), (0.82, 6.28), (1, 6.28))
    R.kf("head", "attack:spiral", "rx", (0, 0), (0.18, -0.35), (0.82, -0.35), (1, 0))

    # pools: stomp (right leg up, then down)
    R.kf("leg_r", "attack:pools", "rx", (0, 0), (0.45, -1.15), (0.52, 0.1), (0.7, 0), (1, 0))
    R.kf("shin_r", "attack:pools", "rx", (0, 0), (0.45, 1.2), (0.52, 0), (1, 0))
    R.kf("body", "attack:pools", "py", (0, 0), (0.45, 2), (0.52, -6), (0.75, -2), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:pools", "rx", (0, 0), (0.45, -0.9), (0.52, 0.2), (1, 0))

    # pillars: hands raised high, trembling
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:pillars", "rx", (0, 0), (0.25, -3.0), (0.85, -3.0), (1, 0))
        R.kf("arm_" + s, "attack:pillars", "rz", (0, 0), (0.25, 0.3 * sg), (0.85, 0.3 * sg), (1, 0))
    R.kf("head", "attack:pillars", "rx", (0, 0), (0.25, -0.5), (0.85, -0.5), (1, 0))
    R.sin("body", "attack:pillars", "px", 1.9, 0.9)

    # summon: arms spread, head thrown back
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:summon", "rz", (0, 0), (0.5, 1.1 * sg), (0.75, 1.1 * sg), (1, 0))
        R.kf("arm_" + s, "attack:summon", "rx", (0, 0), (0.5, -0.9), (0.75, -0.9), (1, 0))
    R.kf("head", "attack:summon", "rx", (0, 0), (0.5, -0.6), (0.75, -0.6), (1, 0))
    R.kf("body", "attack:summon", "rx", (0, 0), (0.5, 0.18), (0.75, 0.18), (1, 0))

    # charge: lean in, arms swept back, legs hammering
    R.kf("body", "attack:charge", "rx", (0, 0), (0.4, -0.55), (0.9, -0.5), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:charge", "rx", (0, 0), (0.4, 1.3), (0.9, 1.3), (1, 0))
    R.sin("leg_r", "attack:charge", "rx", 1.1, 1.0)
    R.sin("leg_l", "attack:charge", "rx", 1.1, 1.0, PI)

    # meteors: arms high, face to the sky
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:meteors", "rx", (0, 0), (0.18, -3.1), (0.88, -3.1), (1, 0))
        R.kf("arm_" + s, "attack:meteors", "rz", (0, 0), (0.18, 0.45 * sg), (0.88, 0.45 * sg), (1, 0))
    R.kf("head", "attack:meteors", "rx", (0, 0), (0.18, -0.75), (0.88, -0.75), (1, 0))
    R.kf("body", "attack:meteors", "rx", (0, 0), (0.18, 0.2), (0.88, 0.2), (1, 0))

    # ring: fists driven into the floor
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:ring", "rx", (0, 0), (0.5, -2.7), (0.58, -0.1), (0.8, -0.1), (1, 0))
    R.kf("body", "attack:ring", "py", (0, 0), (0.5, 4), (0.58, -8), (0.85, -6), (1, 0))
    R.kf("body", "attack:ring", "rx", (0, 0), (0.5, 0.15), (0.58, -0.5), (0.85, -0.45), (1, 0))

    # overheat: crouch, arms crossed over the chest, shaking
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:overheat", "rx", (0, 0), (0.12, -1.1), (0.88, -1.1), (1, 0))
        R.kf("arm_" + s, "attack:overheat", "rz", (0, 0), (0.12, -0.85 * sg), (0.88, -0.85 * sg), (1, 0))
    R.kf("body", "attack:overheat", "py", (0, 0), (0.12, -6), (0.88, -6), (1, 0))
    R.kf("head", "attack:overheat", "rx", (0, 0), (0.12, 0.35), (0.88, 0.35), (1, 0))
    R.sin("body", "attack:overheat", "px", 2.3, 0.7)

    # roar (phase change): chest out, head back, fists wide
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "attack:roar", "rz", (0, 0), (0.2, 0.9 * sg), (0.8, 0.9 * sg), (1, 0))
        R.kf("arm_" + s, "attack:roar", "rx", (0, 0), (0.2, -0.35), (0.8, -0.35), (1, 0))
    R.kf("head", "attack:roar", "rx", (0, 0), (0.15, -0.8), (0.85, -0.8), (1, 0))
    R.kf("body", "attack:roar", "rx", (0, 0), (0.15, 0.25), (0.85, 0.25), (1, 0))
    R.sin("body", "attack:roar", "px", 2.0, 0.6)

    # vulnerable (stunned): slumped
    R.const("head", "vuln", "rx", 0.55)
    R.const("body", "vuln", "rx", -0.3)
    R.const("arm_r", "vuln", "rx", 0.25)
    R.const("arm_l", "vuln", "rx", 0.25)
    R.const("body", "vuln", "py", -3)

    # spawn: climbs out of the ground
    R.kf("root", "spawn", "py", (0, -66), (0.25, -66), (0.85, -4), (1, 0))
    R.kf("head", "spawn", "rx", (0, 0.8), (0.85, 0.8), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "spawn", "rx", (0, -2.4), (0.6, -2.4), (1, 0))

    # death: kneels and sinks into its own slag
    R.kf("root", "dead", "py", (0, 0), (0.5, -4), (0.85, -16), (1, -34))
    R.kf("body", "dead", "rx", (0, 0), (0.4, 0.5), (1, 0.85))
    R.kf("head", "dead", "rx", (0, 0), (0.4, 0.7), (1, 1.05))
    for s, sg in (("r", 1), ("l", -1)):
        R.kf("arm_" + s, "dead", "rx", (0, 0), (0.4, 0.6), (1, 0.3))
        R.kf("arm_" + s, "dead", "rz", (0, 0), (0.4, 0.4 * sg), (1, 0.7 * sg))
        R.kf("leg_" + s, "dead", "rx", (0, 0), (0.5, -0.9), (1, -1.3))
        R.kf("shin_" + s, "dead", "rx", (0, 0), (0.5, 1.2), (1, 1.6))
    return R


def ember_imp():
    R = Rig("ember_imp", dict(skin="#c2401f", horn="#2a2226", wing="#ff7a2a", ember="#ffb03a"), glow="#ffb03a")
    body = R.bone("body", None, pivot=(0, 7, 0))
    body.cube((-3, 5, -2), (6, 7, 4), paint="skin", pal="skin", glow=0.5)
    body.cube((-1, 2, 1), (2, 3, 2), paint="skin", pal="skin")                                  # tail root
    tail = R.bone("tail", "body", pivot=(0, 3, 2))
    tail.cube((-1, 1, 2), (2, 2, 6), paint="skin", pal="skin")
    tail.cube((-1, 0, 7), (2, 3, 3), paint="lava", pal="ember")                                # burning tip
    head = R.bone("head", "body", pivot=(0, 12, 0))
    head.cube((-4, 12, -3), (8, 7, 6), paint="skin", pal="skin",
              features=[("front", 1, 3, 2, 2, "#ffe27a", True), ("front", 5, 3, 2, 2, "#ffe27a", True),
                        ("front", 2, 5, 4, 1, "#ff7a1f", True)])
    head.cube((-3, 19, -1), (1, 3, 1), paint="rock", pal="horn")
    head.cube((2, 19, -1), (1, 3, 1), paint="rock", pal="horn")
    for s, sg in (("r", -1), ("l", 1)):
        arm = R.bone("arm_" + s, "body", pivot=(4 * sg, 11, 0))
        arm.cube(((-6 if sg < 0 else 4), 5, -1), (2, 7, 2), paint="skin", pal="skin")
        leg = R.bone("leg_" + s, "body", pivot=(2 * sg, 5, 0))
        leg.cube(((-3 if sg < 0 else 1), 0, -1), (2, 5, 2), paint="skin", pal="skin")
        wing = R.bone("wing_" + s, "body", pivot=(2 * sg, 10, 2))
        wing.cube(((-12 if sg < 0 else 2), 7, 2), (10, 9, 1), paint="skin", pal="wing", glow=0.8)
    R.look("head")
    R.sin("wing_r", "always", "rz", 0.9, 0.7, 0.0, 0.25)
    R.sin("wing_l", "always", "rz", 0.9, -0.7, 0.0, -0.25)
    R.sin("body", "always", "py", 0.18, 0.8)
    R.sin("tail", "always", "ry", 0.15, 0.5)
    R.sin("leg_r", "always", "rx", 0.12, 0.2)
    R.sin("leg_l", "always", "rx", 0.12, 0.2, PI)
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.4, -2.0), (0.6, -0.4), (1, 0))
    R.kf("arm_l", "attack:melee", "rx", (0, 0), (0.4, -1.6), (0.6, -0.4), (1, 0))
    R.kf("body", "attack:melee", "rx", (0, 0), (0.4, 0.35), (0.6, -0.4), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:fan", "rx", (0, 0), (0.45, -1.7), (0.8, -1.7), (1, 0))
    R.kf("head", "attack:fan", "rx", (0, 0), (0.45, -0.3), (0.8, -0.3), (1, 0))
    R.kf("root", "spawn", "py", (0, -10), (1, 0))
    R.kf("root", "dead", "py", (0, 0), (1, -6))
    R.kf("body", "dead", "rx", (0, 0), (1, 1.2))
    return R
