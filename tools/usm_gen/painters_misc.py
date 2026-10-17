"""Materials, accessories, consumables, projectile orbs, block textures and GUI panels."""
import math
import random
from PIL import Image
from .sprite import Sprite
from .color import ramp, lighten, darken, mix, as_rgba
from .painters import painter

STONE = ramp("#7d7d7d")
DEEPSLATE = ramp("#4a4a50")
NETHERRACK = ramp("#6e3030")
END_STONE = ramp("#d8d6a0")
GLASS = ramp("#a9c4d6")


def _disc(s, cx, cy, r, fn):
    for y in range(s.h):
        for x in range(s.w):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                s.set(x, y, fn(x, y))


def _lit(r, cx, cy, rad):
    """Sphere-ish shading function lit from the upper-left."""
    def f(x, y):
        d = ((x + 0.5 - cx) * 0.7 + (y + 0.5 - cy) * 0.7) / max(rad, 1e-6)
        return r[4] if d < -0.55 else r[3] if d < -0.1 else r[2] if d < 0.4 else r[1] if d < 0.75 else r[0]
    return f


# ---------------------------------------------------------------------------------------------
# raw materials
# ---------------------------------------------------------------------------------------------
@painter("raw_ore")
def raw_ore(pal, seed=3, base=None):
    rnd = random.Random(seed)
    b = base or STONE
    s = Sprite()
    pts = []
    for i in range(10):
        a = math.pi * 2 * i / 10
        r = 5.6 + rnd.uniform(-1.3, 1.2)
        pts.append((8 + math.cos(a) * r, 8.4 + math.sin(a) * r * 0.9))
    s.fill_poly(pts, lambda x, y: b[4] if (x + y) < 12 else b[3] if (x + y) < 16 else b[2] if (x + y) < 19 else b[1])
    for _ in range(4):
        cx, cy = rnd.randint(4, 11), rnd.randint(4, 11)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if s.get(cx + dx, cy + dy) is not None and rnd.random() < 0.85:
                s.set(cx + dx, cy + dy, pal.p[3] if (dx + dy) < 1 else pal.p[1] if dx + dy == 2 else pal.p[2])
        if s.get(cx, cy) is not None:
            s.set(cx, cy, pal.p[4])
    return s.outline(strength=0.55)


@painter("gem")
def gem(pal, cut="diamond", tint=None):
    r = tint or pal.p
    s = Sprite()
    if cut == "shard":
        s.fill_poly([(8, 1.5), (11.5, 6), (10, 14.5), (6, 14.5), (4.5, 6)], _lit(r, 8, 8, 7))
        s.fill_poly([(8, 1.5), (9.5, 6), (8.5, 13.5), (7, 13.5), (6.5, 6)], lambda x, y: r[4] if x < 8 else r[3])
    elif cut == "round":
        _disc(s, 8, 8, 5.6, _lit(r, 8, 8, 5.6))
        s.fill_poly([(5, 5), (8, 3), (7, 7)], r[4])
    else:
        s.fill_poly([(3, 6.5), (5.2, 3.5), (10.8, 3.5), (13, 6.5), (8, 14)], _lit(r, 8, 8, 7))
        s.fill_poly([(3, 6.5), (13, 6.5), (8, 14)], lambda x, y: r[2] if x < 8 else r[1])
        s.fill_poly([(5.2, 3.5), (10.8, 3.5), (13, 6.5), (3, 6.5)], lambda x, y: r[4] if x < 9 else r[3])
        for x in range(6, 10):
            s.set(x, 6, lighten(r[4], 0.35))
    s.set(5, 5, (255, 255, 255, 255))
    return s.outline(strength=0.5)


@painter("scale")
def scale(pal, tilt=1.0):
    s = Sprite()
    body = [(8, 1.5), (13, 4.5), (13.5, 10), (8, 14.5), (2.5, 10), (3, 4.5)]
    s.fill_poly(body, _lit(pal.p, 8, 8, 7))
    for k, y in enumerate((5, 8, 11)):                       # overlapping ridges
        for x in range(4 + (k % 2), 13, 3):
            s.set(x, y, pal.p[4])
            s.set(x + 1, y, pal.p[3])
            s.set(x, y + 1, pal.p[1])
    s.fill_poly([(8, 2.5), (8.6, 4), (8, 12), (7.4, 4)], lambda x, y: pal.a[3])
    s.set(6, 4, pal.hot)
    return s.outline(strength=0.5)


@painter("bar_nugget")
def nugget(pal):
    s = Sprite()
    _disc(s, 6, 9, 3.2, _lit(pal.p, 6, 9, 3.2))
    _disc(s, 10.5, 7, 2.6, _lit(pal.p, 10.5, 7, 2.6))
    s.set(5, 8, lighten(pal.p[4], 0.4))
    return s.outline(strength=0.5)


@painter("core")
def core(pal, seed=2, spikes=8):
    """Boss heart / core: glowing orb in a cage of spikes."""
    s = Sprite()
    rnd = random.Random(seed)
    for i in range(spikes):
        a = math.pi * 2 * i / spikes
        s.fill_poly([(8 + math.cos(a - 0.2) * 3, 8 + math.sin(a - 0.2) * 3), (8 + math.cos(a) * 7, 8 + math.sin(a) * 7),
                     (8 + math.cos(a + 0.2) * 3, 8 + math.sin(a + 0.2) * 3)], pal.s[2])
    _disc(s, 8, 8, 4.4, _lit(pal.a, 8, 8, 4.4))
    _disc(s, 8, 8, 2.2, lambda x, y: pal.hot)
    s.set(6, 6, (255, 255, 255, 255))
    return s.outline(strength=0.5)


@painter("essence")
def essence(pal):
    """A wisp / vial-less essence: teardrop of light."""
    s = Sprite()
    s.fill_poly([(8, 1.5), (11.5, 8), (10.5, 12), (8, 14), (5.5, 12), (4.5, 8)], _lit(pal.a, 8, 8, 7))
    s.fill_poly([(8, 5), (9.5, 9), (8, 12), (6.5, 9)], lambda x, y: pal.hot)
    s.set(7, 6, (255, 255, 255, 255))
    return s.outline(strength=0.45)


@painter("fragment")
def fragment(pal, seed=5):
    """Jagged crystalline fragment of a boss."""
    s = Sprite()
    s.fill_poly([(3, 12.5), (4.5, 5), (7, 8), (8.5, 2.5), (11, 9), (13, 6), (12.5, 13)], _lit(pal.p, 8, 8, 7))
    s.fill_poly([(8.5, 2.5), (9.6, 8), (8.2, 11), (7.4, 7)], lambda x, y: pal.a[3])
    s.set(8, 4, pal.hot)
    return s.outline(strength=0.5)


# ---------------------------------------------------------------------------------------------
# accessories
# ---------------------------------------------------------------------------------------------
@painter("ring")
def ring(pal, gem_color=None):
    s = Sprite()
    _disc(s, 8, 9, 5.2, lambda x, y: pal.p[3] if (x + y) < 15 else pal.p[1])
    _disc(s, 8, 9, 3.0, lambda x, y: (0, 0, 0, 0))
    for y in range(s.h):
        for x in range(s.w):
            if s.g[y][x] == (0, 0, 0, 0):
                s.g[y][x] = None
    s.fill_poly([(6, 3.8), (10, 3.8), (11, 5.6), (8, 7.6), (5, 5.6)], pal.a[3])
    s.set(7, 4, lighten(pal.a[4], 0.4))
    s.set(8, 5, pal.hot)
    return s.outline(strength=0.5)


@painter("amulet")
def amulet(pal):
    s = Sprite()
    for i in range(7):                                       # chain
        s.set(2 + i, 1 + i // 2 + (i % 2), pal.p[2])
        s.set(13 - i, 1 + i // 2 + (i % 2), pal.p[2])
    s.fill_poly([(8, 7), (11.5, 10), (8, 15), (4.5, 10)], _lit(pal.p, 8, 11, 4.5))
    s.fill_poly([(8, 8.4), (10.2, 10.4), (8, 13.4), (5.8, 10.4)], pal.a[3])
    s.set(7, 10, pal.hot)
    s.set(8, 9, lighten(pal.a[4], 0.4))
    return s.outline(strength=0.5)


@painter("clover")
def clover(pal):
    s = Sprite()
    for cx, cy in ((5.5, 5.5), (10.5, 5.5), (5.5, 10), (10.5, 10)):
        _disc(s, cx, cy, 3.0, _lit(pal.p, cx, cy, 3.0))
    s.fill_rect(7, 7, 8, 8, pal.p[4])
    for i in range(4):
        s.set(8 + i // 2, 12 + i, pal.s[2]) if 12 + i < 16 else None
    s.set(5, 4, lighten(pal.p[4], 0.4))
    s.set(11, 4, lighten(pal.p[4], 0.4))
    return s.outline(strength=0.5)


@painter("feather")
def feather(pal):
    s = Sprite()
    s.fill_poly([(13.5, 1.5), (14, 6), (10, 11), (5, 13), (4, 12), (6, 8), (10, 4)], _lit(pal.p, 9, 7, 7))
    s.fill_poly([(13, 2.5), (12, 6.5), (8, 10.5), (5.5, 12.4)], pal.a[3])
    for i in range(6):
        s.set(3 + i // 2, 13 + (i % 2) - i // 3, pal.s[2])
    return s.outline(strength=0.5)


@painter("heart")
def heart(pal, glow=True):
    s = Sprite()
    s.fill_poly([(8, 14.5), (2, 8), (2.4, 4.4), (5, 2.6), (8, 5), (11, 2.6), (13.6, 4.4), (14, 8)], _lit(pal.p, 8, 8, 7))
    s.set(5, 4, lighten(pal.p[4], 0.5))
    s.set(4, 5, lighten(pal.p[4], 0.3))
    if glow:
        s.set(8, 8, pal.hot)
    return s.outline(strength=0.5)


@painter("horseshoe")
def horseshoe(pal):
    """Magnet: red/blue U with metal tips."""
    s = Sprite()
    _disc(s, 8, 8, 6.0, lambda x, y: pal.p[3] if x < 8 else pal.p[1])
    _disc(s, 8, 8, 3.2, lambda x, y: (0, 0, 0, 1))
    for y in range(s.h):
        for x in range(s.w):
            if s.g[y][x] == (0, 0, 0, 1):
                s.g[y][x] = None
    for y in range(9, 16):
        for x in range(s.w):
            if y > 8 and 5 <= x <= 10:
                s.g[y][x] = None
    s.fill_rect(2, 11, 4, 14, pal.a[3])
    s.fill_rect(11, 11, 13, 14, pal.a[3])
    s.fill_rect(2, 11, 4, 11, (230, 230, 235, 255))
    s.fill_rect(11, 11, 13, 11, (230, 230, 235, 255))
    return s.outline(strength=0.5)


@painter("charm_shield")
def charm_shield(pal, mark="gem"):
    s = Sprite()
    s.fill_poly([(3, 2.5), (13, 2.5), (13, 8.5), (8, 14.5), (3, 8.5)], _lit(pal.p, 8, 8, 7))
    s.fill_poly([(4.5, 4), (11.5, 4), (11.5, 8), (8, 12.5), (4.5, 8)], pal.s[2])
    s.fill_poly([(6, 5), (10, 5), (10, 7.5), (8, 10), (6, 7.5)], pal.a[3])
    s.set(7, 6, pal.hot)
    return s.outline(strength=0.5)


@painter("belt")
def belt(pal):
    s = Sprite()
    s.fill_poly([(1, 5.5), (15, 5.5), (15, 10.5), (1, 10.5)], lambda x, y: pal.s[3] if y < 7 else pal.s[2] if y < 9 else pal.s[1])
    s.fill_poly([(5.5, 4), (10.5, 4), (10.5, 12), (5.5, 12)], pal.p[3])
    s.fill_poly([(6.8, 6), (9.2, 6), (9.2, 10), (6.8, 10)], pal.a[3])
    s.set(7, 7, pal.hot)
    return s.outline(strength=0.5)


# ---------------------------------------------------------------------------------------------
# consumables / gadgets
# ---------------------------------------------------------------------------------------------
@painter("bottle")
def bottle(pal, liquid=None, shape="round"):
    liq = liquid or pal.p
    s = Sprite()
    if shape == "round":
        _disc(s, 8, 10, 4.9, lambda x, y: liq[3] if y < 9 else liq[2] if y < 12 else liq[1])
        for y in range(4, 16):
            for x in range(s.w):
                if (x + 0.5 - 8) ** 2 + (y + 0.5 - 10) ** 2 <= 4.9 ** 2 and y < 8:
                    s.set(x, y, GLASS[3] if y < 7 else liq[3])
    else:
        s.fill_rect(5, 7, 10, 14, liq[2])
        s.fill_rect(5, 7, 10, 8, GLASS[3])
    s.fill_rect(7, 3, 8, 6, GLASS[3])                                        # neck
    s.fill_rect(7, 1, 8, 2, pal.s[2])                                        # cork
    s.set(6, 10, lighten(liq[4], 0.4))
    s.set(6, 11, lighten(liq[4], 0.2))
    return s.outline(strength=0.45)


@painter("jar")
def jar(pal, contents=None):
    c = contents or pal.p
    s = Sprite()
    s.fill_rect(3, 7, 12, 14, GLASS[2])
    s.fill_rect(4, 8, 11, 14, c[2])
    s.fill_rect(4, 8, 11, 9, c[3])
    s.fill_rect(2, 4, 13, 6, pal.s[3])
    s.fill_rect(2, 6, 13, 6, pal.s[1])
    s.set(5, 10, lighten(c[4], 0.4))
    s.set(5, 11, lighten(c[4], 0.2))
    return s.outline(strength=0.45)


@painter("biscuit")
def biscuit(pal):
    s = Sprite()
    s.fill_poly([(2.5, 5), (12, 3.5), (13.5, 11), (4, 12.5)], lambda x, y: pal.s[3] if y < 8 else pal.s[2])
    s.fill_poly([(3, 10.5), (13.5, 11), (4, 12.5), (3.2, 12)], pal.s[1])
    for (x, y) in ((5, 6), (8, 5), (7, 8), (10, 8), (6, 10)):
        s.set(x, y, pal.s[0])
    s.set(9, 6, pal.p[3])
    return s.outline(strength=0.5)


@painter("mirror")
def mirror(pal):
    s = Sprite()
    s.fill_poly([(8.2, 9.5), (13.8, 15), (12.8, 15.8), (7.2, 10.4)], pal.s[2])
    _disc(s, 6.5, 6.5, 5.0, lambda x, y: pal.p[3] if (x + y) < 11 else pal.p[1])
    _disc(s, 6.5, 6.5, 3.5, lambda x, y: GLASS[4] if (x + y) < 10 else pal.a[3])
    s.set(5, 5, (255, 255, 255, 255))
    s.set(8, 8, pal.hot)
    return s.outline(strength=0.5)


@painter("crystal_heart")
def crystal_heart(pal):
    s = Sprite()
    s.fill_poly([(8, 15), (1.5, 8), (2, 4), (5, 2), (8, 4.5), (11, 2), (14, 4), (14.5, 8)], _lit(pal.p, 8, 8, 7))
    s.fill_poly([(8, 12.5), (4, 7.5), (4.4, 5), (6, 4), (8, 6.5), (10, 4), (11.6, 5), (12, 7.5)], lambda x, y: pal.a[3] if x < 8 else pal.a[2])
    s.set(5, 4, (255, 255, 255, 255))
    s.set(9, 7, pal.hot)
    return s.outline(strength=0.5)


@painter("orb")
def orb(pal, r=4.6, halo=True):
    """Projectile sprite: hot core, coloured body, soft translucent halo."""
    s = Sprite()
    if halo:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                if r < d < r + 2.6:
                    a = int(150 * (1 - (d - r) / 2.6))
                    c = pal.glow
                    s.set(x, y, (c[0], c[1], c[2], max(a, 20)))
    _disc(s, 8, 8, r, _lit(pal.a, 8, 8, r))
    _disc(s, 8, 8, r * 0.52, lambda x, y: pal.hot)
    s.set(7, 7, (255, 255, 255, 255))
    return s


# ---------------------------------------------------------------------------------------------
# blocks (16x16)
# ---------------------------------------------------------------------------------------------
def _noise_base(base, seed, w=16, h=16):
    rnd = random.Random(seed)
    s = Sprite(w, h)
    for y in range(h):
        for x in range(w):
            n = rnd.random()
            i = 2 + (1 if n > 0.8 else -1 if n < 0.18 else 0)
            if n > 0.97:
                i = 4
            if n < 0.03:
                i = 0
            s.set(x, y, base[i])
    return s


@painter("ore_block")
def ore_block(pal, deepslate=False, seed=11, density=5, nether=False, end=False):
    base = END_STONE if end else NETHERRACK if nether else DEEPSLATE if deepslate else STONE
    rnd = random.Random(seed)
    s = _noise_base(base, seed + (50 if deepslate else 0))
    placed = 0
    tries = 0
    while placed < density and tries < 200:
        tries += 1
        cx, cy = rnd.randint(1, 13), rnd.randint(1, 13)
        cells = [(0, 0), (1, 0), (0, 1)] + ([(1, 1)] if rnd.random() < 0.5 else [])
        if any(abs(cx - ox) < 4 and abs(cy - oy) < 4 for ox, oy in getattr(rnd, "_spots", [])):
            continue
        rnd._spots = getattr(rnd, "_spots", []) + [(cx, cy)]
        for dx, dy in cells:
            c = pal.p[3] if (dx + dy) == 0 else pal.p[2] if dx + dy == 1 else pal.p[1]
            s.set(cx + dx, cy + dy, c)
        s.set(cx, cy, pal.p[4])
        if rnd.random() < 0.7:
            s.set(cx - 1, cy, darken(pal.p[1], 0.3))
        placed += 1
    return s


@painter("metal_block")
def metal_block(pal, seed=4):
    rnd = random.Random(seed)
    s = Sprite()
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            i = 2 + (1 if n > 0.85 else -1 if n < 0.1 else 0)
            if x == 0 or y == 0:
                i = 4
            if x == 15 or y == 15:
                i = 0
            elif x == 1 or y == 1:
                i = min(i + 1, 4)
            elif x == 14 or y == 14:
                i = max(i - 1, 0)
            s.set(x, y, pal.p[i])
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        s.set(x, y, pal.a[3])
    for i in range(4, 12):                                                   # engraved cross
        s.set(i, 8, pal.p[1])
        s.set(8, i, pal.p[1])
    s.set(8, 8, pal.glow)
    return s


# ---------------------------------------------------------------------------------------------
# GUI
# ---------------------------------------------------------------------------------------------
def accessory_panel():
    """176x166 accessory menu background drawn into a 256x256 sheet (vanilla container layout)."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = im.load()
    W, H = 176, 166
    for y in range(H):
        for x in range(W):
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if edge == 0:
                c = (14, 12, 22, 255)
            elif edge == 1:
                c = (110, 92, 170, 255)
            elif edge == 2:
                c = (62, 52, 104, 255)
            else:
                t = y / H
                c = (int(34 + 14 * t), int(30 + 10 * t), int(58 + 18 * t), 255)
            px[x, y] = c
    def slot(x, y):
        for dy in range(18):
            for dx in range(18):
                edge = (dx == 0 or dy == 0, dx == 17 or dy == 17)
                if edge[0]:
                    c = (12, 10, 20, 255)
                elif edge[1]:
                    c = (140, 126, 196, 255)
                else:
                    c = (24, 20, 40, 255)
                px[x - 1 + dx, y - 1 + dy] = c
    for r in range(2):
        for c in range(3):
            slot(62 + 18 * c, 26 + 18 * r)
    for r in range(3):
        for c in range(9):
            slot(8 + 18 * c, 84 + 18 * r)
    for c in range(9):
        slot(8 + 18 * c, 142)
    # decorative runes either side of the charm grid
    gold = (232, 196, 96, 255)
    for k in range(5):
        for side in (24, 146):
            px[side, 28 + k * 5] = gold
            px[side + 1, 28 + k * 5 + 2] = (178, 142, 60, 255)
    for x in range(56, 120):
        if x % 4 == 0:
            px[x, 20] = gold
    return im


@painter("tome")
def tome(pal, ribbon=True):
    """Closed spell book: leather cover in the palette, gold bands, a gem, pages showing on two edges."""
    s = Sprite()
    s.fill_poly([(3, 2), (13, 2), (13, 14), (3, 14)], lambda x, y: pal.p[3] if (x + y) < 15 else pal.p[1] if (x + y) > 19 else pal.p[2])
    s.fill_rect(3, 2, 4, 13, pal.s[2])                                       # spine
    s.fill_rect(3, 2, 4, 3, lighten(pal.s[3], 0.2))
    s.fill_rect(13, 3, 13, 13, (226, 210, 170, 255))                         # page edge
    s.fill_rect(4, 14, 12, 14, (200, 184, 146, 255))
    for (x0, y0, x1, y1) in ((5, 3, 12, 3), (5, 12, 12, 12), (5, 3, 5, 12), (12, 3, 12, 12)):
        s.fill_rect(x0, y0, x1, y1, pal.a[2])                                # gold frame
    s.fill_poly([(8.5, 5.2), (10.4, 7.4), (8.5, 9.6), (6.6, 7.4)], pal.a[3])
    s.set(8, 6, pal.hot)
    return s.outline(strength=0.5)
