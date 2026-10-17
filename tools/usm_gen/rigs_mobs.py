"""Archetype builders for the regular (non-boss) mobs: beasts, brutes (golems / thralls) and floaters (wisps / wraiths / raptors).
Each builder returns a finished Rig with idle / walk / melee / attack animations, so a new mob is one call."""
import math
from .rig import Rig, PI


def _x(o, size, side):
    """Origin of a cube authored for the right side (x < 0), mirrored for the left (side = +1)."""
    return o if side < 0 else (-o[0] - size[0], o[1], o[2])


# =====================================================================================================================
def beast(rid, pals, glow, body=(10, 8, 16), leg_h=7, head=(7, 7, 7), legs=4, spikes=0, horns=0, tail=0, eyes="#ffd25a", fangs=False,
          paint="rock", spike_pal="spike", bright=0.0):
    """Four- or six-legged animal. pals needs 'body'; optionally 'spike', 'head'."""
    w, h, l = body
    hw, hh, hl = head
    R = Rig(rid, pals, glow=glow)
    top = leg_h + h
    b = R.bone("body", None, pivot=(0, leg_h + h / 2.0, 0))
    b.cube((-w // 2, leg_h, -l // 2), (w, h, l), paint=paint, pal="body", glow=0.15 + bright)
    for i in range(spikes):
        z = -l // 2 + 2 + (i * (l - 6)) // max(1, spikes - 1)
        b.cube((-1, top, z), (2, 4 + (i % 2) * 2, 2), paint="rock", pal=spike_pal if spike_pal in pals else "body", glow=bright)
    hy = leg_h + h - hh - 1
    head_b = R.bone("head", "body", pivot=(0, hy + hh / 2.0, -l // 2))
    head_b.cube((-hw // 2, hy, -l // 2 - hl + 1), (hw, hh, hl), paint=paint, pal=pals.get("head") and "head" or "body",
                features=[("front", 1, hh // 3, 2, 1, eyes, True), ("front", hw - 3, hh // 3, 2, 1, eyes, True)])
    if fangs:
        head_b.cube((-hw // 2 + 1, hy - 2, -l // 2 - hl + 1), (1, 3, 1), paint="rock", pal="body")
        head_b.cube((hw // 2 - 2, hy - 2, -l // 2 - hl + 1), (1, 3, 1), paint="rock", pal="body")
    for k in range(horns):
        sx = -1 if k % 2 == 0 else 1
        head_b.cube(_x((-hw // 2 - 1, hy + hh - 1, -l // 2 - hl // 2), (2, 5, 2), sx), (2, 5, 2), paint="rock", pal=spike_pal if spike_pal in pals else "body", glow=bright)
    if tail:
        t = R.bone("tail", "body", pivot=(0, leg_h + h * 0.8, l // 2))
        t.cube((-1, leg_h + h * 0.6, l // 2), (2, 2, 5 + tail), paint="rock", pal="body", glow=bright)
        R.sin("tail", "always", "ry", 0.18, 0.35)
    rows = [(-l // 2 + 3, 0), (l // 2 - 3, 1)] if legs == 4 else [(-l // 2 + 2, 0), (0, 1), (l // 2 - 2, 0)]
    for ri, (z, ph) in enumerate(rows):
        for s, sx in (("r", -1), ("l", 1)):
            x = (w // 2 - 1) * sx
            leg = R.bone("leg_%s%d" % (s, ri), None, pivot=(x, leg_h, z))
            leg.cube((x - 1.5, 0, z - 1.5), (3, leg_h, 3), paint="rock", pal="body", glow=bright * 0.5)
            R.walk("leg_%s%d" % (s, ri), "rx", 1.0, (0.0 if s == "r" else PI) + ph * PI)
    R.look("head")
    R.sin("body", "idle", "py", 0.1, 0.4)
    R.kf("head", "attack:melee", "rx", (0, 0), (0.35, 0.5), (0.55, -0.55), (1, 0))
    R.kf("body", "attack:melee", "pz", (0, 0), (0.35, -1.0), (0.55, 2.5), (1, 0))
    for a in ("charge", "leap"):
        R.kf("body", "attack:" + a, "py", (0, 0), (0.45, -3), (0.55, 1), (1, 0))
        R.kf("head", "attack:" + a, "rx", (0, 0), (0.45, 0.4), (0.6, -0.3), (1, 0))
    R.kf("head", "attack:fan", "rx", (0, 0), (0.4, -0.7), (0.8, -0.7), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (0.7, -0.1), (1, -0.9))
    R.kf("body", "dead", "rz", (0, 0), (1, 1.4))
    return R


# =====================================================================================================================
def brute(rid, pals, glow, h=40, bulk=1.0, head=(10, 10, 10), shoulders=True, core=True, spikes=0, eyes="#ffd25a", paint="plate",
          cracks=0.3, fist=None):
    """Hulking humanoid (golem / thrall). pals: 'body' (+ 'accent', 'core')."""
    k = bulk
    tw, td = int(round(16 * k)), int(round(10 * k))
    leg_h = int(round(h * 0.32))
    torso_h = int(round(h * 0.38))
    R = Rig(rid, pals, glow=glow)
    ty = leg_h
    body = R.bone("body", None, pivot=(0, ty + torso_h // 2, 0))
    body.cube((-tw // 2, ty, -td // 2), (tw, torso_h, td), paint=paint, pal="body", glow=cracks)
    if core:
        body.cube((-3, ty + torso_h // 3, -td // 2 - 1), (6, max(4, torso_h // 3), 1), paint="lava", pal="core" if "core" in pals else "body")
    for i in range(spikes):
        body.cube((-tw // 2 + 2 + i * ((tw - 4) // max(1, spikes - 1)), ty + torso_h, 0), (2, 5, 2), paint="rock", pal="accent" if "accent" in pals else "body", glow=0.2)
    hw, hh, hd = head
    hb = R.bone("head", "body", pivot=(0, ty + torso_h, 0))
    hb.cube((-hw // 2, ty + torso_h, -hd // 2), (hw, hh, hd), paint=paint, pal="body", glow=cracks * 0.5,
            features=[("front", 2, hh // 2 - 1, 2, 2, eyes, True), ("front", hw - 4, hh // 2 - 1, 2, 2, eyes, True)])
    aw = int(round(6 * k))
    arm_h = int(round(h * 0.34))
    for s, sx in (("r", -1), ("l", 1)):
        px = (tw // 2 + aw // 2) * sx
        arm = R.bone("arm_" + s, "body", pivot=(px, ty + torso_h - 2, 0))
        o = (px - aw // 2, ty + torso_h - 2 - arm_h, -aw // 2)
        arm.cube(o, (aw, arm_h, aw), paint="rock", pal="body", glow=cracks)
        if shoulders:
            arm.cube((px - aw // 2 - 1, ty + torso_h - 4, -aw // 2 - 1), (aw + 2, 4, aw + 2), paint=paint, pal="accent" if "accent" in pals else "body")
        f = fist or (aw + 2)
        arm.cube((px - f // 2, ty + torso_h - 2 - arm_h - f + 2, -f // 2), (f, f, f), paint="rock", pal="body", glow=cracks * 1.3)
        leg = R.bone("leg_" + s, None, pivot=(sx * (tw // 4), leg_h, 0))
        leg.cube((sx * (tw // 4) - aw // 2 - (1 if sx < 0 else 0), 0, -aw // 2 - 1), (aw + 1, leg_h, aw + 2), paint="rock", pal="body", glow=cracks * 0.6)
    R.humanoid_walk(amp=0.8, arm_amp=0.45)
    R.look("head")
    R.sin("body", "idle", "py", 0.08, 0.5)
    R.kf("arm_r", "attack:melee", "rx", (0, 0), (0.35, -2.2), (0.55, -0.5), (1, 0))
    R.kf("body", "attack:melee", "ry", (0, 0), (0.35, 0.4), (0.55, -0.4), (1, 0))
    for a in ("slam", "ring"):
        for s in ("r", "l"):
            R.kf("arm_" + s, "attack:" + a, "rx", (0, 0), (0.55, -2.9), (0.62, -0.4), (0.8, -0.35), (1, 0))
        R.kf("body", "attack:" + a, "py", (0, 0), (0.5, 2), (0.62, -4), (0.85, -2), (1, 0))
    for s in ("r", "l"):
        R.kf("arm_" + s, "attack:fan", "rx", (0, 0), (0.4, -1.5), (0.8, -1.5), (1, 0))
        R.kf("arm_" + s, "attack:charge", "rx", (0, 0), (0.4, 1.2), (0.9, 1.2), (1, 0))
    R.kf("body", "attack:charge", "rx", (0, 0), (0.4, -0.5), (0.9, -0.45), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (0.7, -0.1), (1, -0.9))
    R.kf("body", "dead", "rx", (0, 0), (1, 0.8))
    return R


# =====================================================================================================================
def floater(rid, pals, glow, core=(6, 6, 6), y=10, wings=0, tail=0, halo=0, eyes="#ffd25a", body_paint="rock", puffs=0, head=None):
    """A hovering creature: a core body, optional wings (flapping), tail, and orbiting bits."""
    cw, ch, cd = core
    R = Rig(rid, pals, glow=glow)
    b = R.bone("body", None, pivot=(0, y, 0))
    b.cube((-cw // 2, y - ch // 2, -cd // 2), (cw, ch, cd), paint=body_paint, pal="body", glow=0.3)
    hb = R.bone("head", "body", pivot=(0, y + ch // 2, 0))
    hw, hh, hd = head or (max(4, cw - 1), max(4, ch - 2), max(4, cd - 1))
    hb.cube((-hw // 2, y + ch // 2, -hd // 2 - 1), (hw, hh, hd), paint=body_paint, pal="body", glow=0.2,
            features=[("front", 1, hh // 2, 1, 1, eyes, True), ("front", hw - 2, hh // 2, 1, 1, eyes, True)])
    for k in range(wings):
        for s, sx in (("r", -1), ("l", 1)):
            wb = R.bone("wing_%s%d" % (s, k), "body", pivot=(sx * (cw // 2), y + 1, 1 - k * 4))
            ww = 9 + k * 2
            wb.cube(_x((-cw // 2 - ww, y - 2, 0 - k * 4), (ww, 5, 1), sx), (ww, 5, 1), paint="rock", pal="accent" if "accent" in pals else "body", glow=0.35)
            R.sin("wing_%s%d" % (s, k), "always", "rz", 0.75, 0.8 * (1 if s == "r" else -1), 0.4 * k)
    if tail:
        tb = R.bone("tail", "body", pivot=(0, y - 1, cd // 2))
        tb.cube((-1, y - 2, cd // 2), (2, 2, tail), paint="rock", pal="body", glow=0.3)
        R.sin("tail", "always", "ry", 0.2, 0.5)
    if halo:
        hl = R.bone("halo", None, pivot=(0, y, 0))
        for i in range(halo):
            a = i * 2 * PI / halo
            hl.cube((int(round(math.cos(a) * (cw + 3))) - 1, y - 1, int(round(math.sin(a) * (cw + 3))) - 1), (2, 2, 2), paint="lava", pal="accent" if "accent" in pals else "body")
        R.spin("halo", 0.1)
    for i in range(puffs):
        a = i * 2 * PI / puffs
        pb = R.bone("puff%d" % i, "body", pivot=(0, y, 0))
        pb.cube((int(round(math.cos(a) * 5)) - 2, y - 3 + (i % 2) * 2, int(round(math.sin(a) * 5)) - 2), (4, 4, 4), paint="rock", pal="accent" if "accent" in pals else "body", glow=0.1)
        R.sin("puff%d" % i, "always", "ry", 0.1, 0.4, i)
    R.look("head")
    R.sin("root", "always", "py", 0.14, 0.9)
    R.walk("body", "rx", 0.2, PI / 2, speed=0.0)
    R.kf("body", "attack:melee", "rx", (0, 0), (0.4, 0.6), (0.6, -0.6), (1, 0))
    for a in ("fan", "orbs", "charge", "lightning"):
        R.kf("head", "attack:" + a, "rx", (0, 0), (0.4, -0.5), (0.8, -0.5), (1, 0))
        for ax in ("sx", "sy", "sz"):
            R.kf("body", "attack:" + a, ax, (0, 0), (0.5, 0.25), (0.65, 0), (1, 0))
    for ax in ("sx", "sy", "sz"):
        R.kf("root", "spawn", ax, (0, -0.9), (1, 0))
        R.kf("root", "dead", ax, (0, 0), (0.7, -0.1), (1, -0.9))
    R.kf("root", "dead", "py", (0, 0), (1, -6))
    return R
