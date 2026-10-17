"""Rig DSL for mobs and bosses.

A rig is a tree of bones; each bone holds boxes. `Rig.build()` packs the boxes into a texture, paints a diffuse
texture and an emissive ("glow") texture, and returns the JSON the Java RigModel loads at runtime.

Coordinates are authored in pixels (16 px = 1 block), feet-up and centred on the entity:
  x  : model x, like vanilla -> negative x is the entity's RIGHT side (right_arm lives at x < 0)
  y  : up from the feet
  z  : negative z is the FRONT of the mob
All box sizes are integers (they map 1:1 to texture pixels, vanilla box-UV layout).

Animation rules (evaluated every frame by RigModel, values are ADDED to the bone's rest pose):
  axis rx ry rz   rotation in radians, vanilla sign convention (arms: negative rx lifts them forward / up)
  axis px py pz   offset in pixels: px = model +x (entity's left), py = UP, pz = FORWARD
  axis sx sy sz   additive scale factor
  bone "root" is the model root (move/scale the whole mob).
  when: always | idle | walk | attack:<anim> | spawn | dead | vuln | enraged | phase:<n>
  fn  : sin(speed, amp, phase, off) | walk(speed, amp, phase) | kf(keys=[[progress, value], ...]) | const(value) |
        look(mul) | spin(speed) | noise(speed, amp)
  attack:/spawn/dead rules play over the animation's own 0..1 progress, so keyframes are authored as fractions."""
import math
import random
import zlib
from PIL import Image
from .color import ramp, as_rgba, lighten, darken, mix

PI = math.pi

# baked "ambient light" per face so the model reads even in flat lighting: index shift into the 5-step ramp
FACE_BIAS = {"top": 1, "bottom": -2, "front": 0, "back": -1, "left": -1, "right": -1}


def face_rects(u, v, w, h, d):
    """Vanilla box-UV layout for a w x h x d box with its origin at (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Cube:
    def __init__(self, o, s, paint="plate", pal="body", inflate=0.0, mirror=False, features=None, glow=0.0, seed=0, tint=None):
        self.o, self.s = tuple(o), tuple(s)
        if any(int(v) != v for v in self.s):
            raise ValueError("cube sizes must be integers: %r" % (s,))
        self.paint, self.pal = paint, pal
        self.inflate, self.mirror = inflate, mirror
        self.features = features or []        # [(face, x, y, w, h, colour, emit?)]
        self.glow = glow                      # 0..1 how much of the surface is emissive crack
        self.seed = seed
        self.tint = tint
        self.uv = (0, 0)

    @property
    def footprint(self):
        w, h, d = (int(v) for v in self.s)
        return 2 * (d + w), d + h


class Bone:
    def __init__(self, rig, name, parent, pivot, rot):
        self.rig, self.name, self.parent = rig, name, parent
        self.pivot, self.rot = tuple(pivot), tuple(rot)
        self.cubes = []

    def cube(self, o, s, **kw):
        self.cubes.append(Cube(o, s, **kw))
        return self

    def mirror_x(self, o, s, **kw):
        """Add a cube and its x-mirror image into the same bone (symmetric horns, spikes ...)."""
        self.cube(o, s, **kw)
        self.cube((-o[0] - s[0], o[1], o[2]), s, mirror=True, **kw)
        return self


class Rig:
    def __init__(self, id, palettes, glow="#ff8a2a", scale=1.0):
        self.id = id
        self.palettes = {k: (ramp(v) if isinstance(v, str) else v) for k, v in palettes.items()}
        self.glow = as_rgba(glow)
        self.bones = {}
        self.order = []
        self.rules = []
        self.scale = scale
        self.tex = (64, 64)

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        if parent is not None and parent not in self.bones:
            raise ValueError("parent %s must be declared before %s" % (parent, name))
        b = Bone(self, name, parent, pivot, rot)
        self.bones[name] = b
        self.order.append(name)
        return b

    # ---- animation rules -------------------------------------------------------------------------------------
    def rule(self, bone, when, axis, fn, **kw):
        if bone != "root" and bone not in self.bones:
            raise ValueError("animation rule for unknown bone " + bone)
        r = dict(bone=bone, when=when, axis=axis, fn=fn)
        r.update(kw)
        self.rules.append(r)
        return self

    def kf(self, bone, when, axis, *keys):
        """Keyframes [(progress, value), ...] over the animation's 0..1 progress, smoothly interpolated."""
        return self.rule(bone, when, axis, "kf", keys=[list(k) for k in keys])

    def sin(self, bone, when, axis, speed, amp, phase=0.0, off=0.0):
        return self.rule(bone, when, axis, "sin", speed=speed, amp=amp, phase=phase, off=off)

    def walk(self, bone, axis, amp, phase=0.0, speed=0.6662):
        return self.rule(bone, "walk", axis, "walk", speed=speed, amp=amp, phase=phase)

    def spin(self, bone, speed, axis="ry", when="always"):
        return self.rule(bone, when, axis, "spin", speed=speed)

    def look(self, bone="head", mul=1.0):
        self.rule(bone, "always", "ry", "look", mul=mul)
        self.rule(bone, "always", "rx", "look", mul=mul)
        return self

    def const(self, bone, when, axis, value):
        return self.rule(bone, when, axis, "const", value=value)

    def humanoid_walk(self, leg_l="leg_l", leg_r="leg_r", arm_l="arm_l", arm_r="arm_r", amp=1.0, arm_amp=0.6, speed=0.6662):
        self.walk(leg_r, "rx", amp, 0.0, speed)
        self.walk(leg_l, "rx", amp, PI, speed)
        if arm_l:
            self.walk(arm_l, "rx", arm_amp, 0.0, speed)
        if arm_r:
            self.walk(arm_r, "rx", arm_amp, PI, speed)
        return self

    # ---- packing -------------------------------------------------------------------------------------------
    def _pack(self):
        cubes = [c for b in self.order for c in self.bones[b].cubes]
        order = sorted(cubes, key=lambda c: -c.footprint[1])
        for size in (32, 64, 128, 256, 512):
            x = y = shelf = 0
            ok = True
            for c in order:
                fw, fh = c.footprint
                if fw > size:
                    ok = False
                    break
                if x + fw > size:
                    x, y, shelf = 0, y + shelf, 0
                if y + fh > size:
                    ok = False
                    break
                c.uv = (x, y)
                x += fw
                shelf = max(shelf, fh)
            if ok:
                self.tex = (size, max(16, -(-(y + shelf) // 8) * 8))      # width = shelf width, height trimmed to what is used
                return
        raise ValueError("rig too large")

    # ---- painting ----------------------------------------------------------------------------------------------
    def _shade(self, r, i, bias):
        return r[max(0, min(len(r) - 1, i + bias))]

    def _surface(self, px, gx, rect, r, rnd, glow, kind, bias):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                n = rnd.random()
                if kind == "lava":
                    c = r[3 + (1 if n > 0.55 else 0)] if n > 0.15 else r[2]
                    px[x0 + x, y0 + y] = c
                    gx[x0 + x, y0 + y] = c
                    continue
                if kind == "rock":
                    i = 1 + (1 if n > 0.55 else 0) + (1 if n > 0.92 else 0) - (1 if n < 0.14 else 0)
                elif kind == "skin":
                    i = 2 + (1 if n > 0.8 else -1 if n < 0.15 else 0)
                elif kind == "bone":
                    i = 3 + (1 if n > 0.85 else -1 if n < 0.2 else 0)
                elif kind == "scales":
                    off = (y // 2) % 2
                    cx, cy = (x + off * 2) % 4, y % 2
                    i = 3 if cy == 0 and cx in (1, 2) else 2 if cy == 0 else 1
                    if n < 0.06:
                        i -= 1
                else:                                                                  # plate / metal
                    i = 2 + (1 if n > 0.93 else -1 if n < 0.06 else 0)
                    if y == 0:
                        i += 2
                    elif x == 0:
                        i += 1
                    elif y == h - 1 or x == w - 1:
                        i -= 2
                px[x0 + x, y0 + y] = self._shade(r, i, bias)
        if kind in ("plate", "metal") and w >= 6 and h >= 6:
            m = w // 2
            for y in range(1, h - 1):
                px[x0 + m, y0 + y] = self._shade(r, 1, bias)
            if h >= 8:
                for x in range(1, w - 1):
                    px[x0 + x, y0 + h // 2] = self._shade(r, 1, bias)
            for (rx, ry) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):          # rivets
                px[x0 + rx, y0 + ry] = self._shade(r, 4, bias)
        if glow > 0 and kind != "lava":                                               # random-walk cracks in both textures
            walks = max(1, int(glow * (w + h) / 3))
            for _ in range(walks):
                cx, cy = rnd.randrange(w), rnd.randrange(h)
                for _ in range(rnd.randint(3, 3 + max(w, h) // 2)):
                    px[x0 + cx, y0 + cy] = self.glow
                    gx[x0 + cx, y0 + cy] = self.glow
                    d = rnd.choice(((1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)))
                    cx = max(0, min(w - 1, cx + d[0]))
                    cy = max(0, min(h - 1, cy + d[1]))

    def _clock(self, px, gx, rect, dial):
        """A round clock face on a front rect: dial, twelve ticks and two hands (ticks and hands are emissive)."""
        x0, y0, w, h = rect
        cx, cy, R = (w - 1) / 2.0, (h - 1) / 2.0, min(w, h) / 2.0 - 0.6
        for y in range(h):
            for x in range(w):
                d = math.hypot(x - cx, y - cy)
                if d <= R:
                    px[x0 + x, y0 + y] = dial[3] if d < R - 0.9 else dial[1]
        for k in range(12):
            a = k * PI / 6
            for rr in ((R - 1.6, R - 0.8) if k % 3 else (R - 2.6, R - 0.8)):
                xx, yy = int(round(cx + math.sin(a) * rr)), int(round(cy - math.cos(a) * rr))
                if 0 <= xx < w and 0 <= yy < h:
                    px[x0 + xx, y0 + yy] = self.glow
                    gx[x0 + xx, y0 + yy] = self.glow
        for ang, ln in ((PI * 0.17, R * 0.62), (PI * 1.12, R * 0.82)):
            for t in range(int(ln * 2)):
                xx, yy = int(round(cx + math.sin(ang) * t / 2.0)), int(round(cy - math.cos(ang) * t / 2.0))
                if 0 <= xx < w and 0 <= yy < h:
                    px[x0 + xx, y0 + yy] = self.glow
                    gx[x0 + xx, y0 + yy] = self.glow

    def paint(self):
        self._pack()
        W, H = self.tex
        base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        bp, gp = base.load(), glow.load()
        for bname in self.order:
            for c in self.bones[bname].cubes:
                rnd = random.Random(zlib.crc32(("%s|%s|%s|%s|%s" % (self.id, bname, c.o, c.s, c.seed)).encode()))
                r = self.palettes[c.pal]
                w, h, d = (int(v) for v in c.s)
                rects = face_rects(c.uv[0], c.uv[1], w, h, d)
                for face, rect in rects.items():
                    kind = "plate" if c.paint == "clock" else c.paint
                    self._surface(bp, gp, rect, r, rnd, c.glow if face != "bottom" else 0, kind, FACE_BIAS[face])
                    if c.paint == "clock" and face == "front":
                        self._clock(bp, gp, rect, self.palettes["dial"])
                for (face, x, y, fw, fh, colour, emit) in c.features:
                    fx, fy, _, _ = rects[face]
                    col = as_rgba(colour)
                    for yy in range(fh):
                        for xx in range(fw):
                            bp[fx + x + xx, fy + y + yy] = col
                            if emit:
                                gp[fx + x + xx, fy + y + yy] = col
        return base, glow

    # ---- export ------------------------------------------------------------------------------------------------------
    def to_json(self):
        bones = []
        for n in self.order:
            b = self.bones[n]
            bones.append(dict(name=b.name, parent=b.parent, pivot=list(b.pivot), rot=list(b.rot),
                              cubes=[dict(uv=list(c.uv), o=list(c.o), s=[int(v) for v in c.s], inflate=c.inflate, mirror=c.mirror)
                                     for c in b.cubes]))
        return dict(id=self.id, tex=list(self.tex), scale=self.scale, bones=bones, rules=self.rules)

    def build(self):
        base, glow = self.paint()
        return self.to_json(), base, glow
