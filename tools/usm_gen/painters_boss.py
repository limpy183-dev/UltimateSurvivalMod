"""Boss-adjacent art: treasure bags, summoning sigils, spawn eggs, the ritual altar block."""
import math
import random
from .sprite import Sprite
from .color import ramp, lighten, darken, mix, as_rgba
from .painters import painter
from .painters_misc import _disc, _lit


@painter("bag")
def bag(pal, seal="gem"):
    """Treasure bag: a cinched sack in the boss colour with a gold tie and a glowing seal."""
    s = Sprite()
    body = pal.p
    s.fill_poly([(8, 3.2), (11.5, 4.6), (13.8, 9), (13, 13), (10.5, 14.8), (5.5, 14.8), (3, 13), (2.2, 9), (4.5, 4.6)],
                lambda x, y: body[4] if (x + y) < 12 and x < 8 else body[3] if (x + y) < 15 else body[2] if x < 9 else body[1])
    s.fill_poly([(4.6, 3.4), (11.4, 3.4), (10.6, 5.6), (5.4, 5.6)], lambda x, y: pal.s[3] if x < 8 else pal.s[2])    # gathered neck
    s.fill_poly([(3.8, 5.6), (12.2, 5.6), (12.2, 6.8), (3.8, 6.8)], pal.a[3])                                          # gold tie
    s.fill_rect(4, 6, 4, 6, pal.a[4])
    s.fill_poly([(2.8, 1.4), (4.8, 3.4), (5.6, 3.4), (4.4, 1.0)], pal.s[2])                                            # frayed ends
    s.fill_poly([(13.2, 1.4), (11.2, 3.4), (10.4, 3.4), (11.6, 1.0)], pal.s[2])
    _disc(s, 8, 10.4, 2.4, lambda x, y: pal.a[3] if (x + y) < 18 else pal.a[2])                                        # seal
    _disc(s, 8, 10.4, 1.2, lambda x, y: pal.hot)
    s.set(7, 9, (255, 255, 255, 255))
    return s.outline(strength=0.5)


@painter("sigil")
def sigil(pal):
    """Summoning sigil: a dark disc cut with a glowing runic star."""
    s = Sprite()
    _disc(s, 8, 8, 6.8, lambda x, y: pal.s[3] if (x + y) < 14 else pal.s[1] if (x + y) > 18 else pal.s[2])
    _disc(s, 8, 8, 5.4, lambda x, y: pal.s[0])
    pts = []
    for k in range(5):                                                             # five-point star, drawn as lines
        a = -math.pi / 2 + k * 4 * math.pi / 5
        pts.append((8 + 4.6 * math.cos(a), 8 + 4.6 * math.sin(a)))
    for i in range(5):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % 5]
        for t in range(0, 21):
            x, y = x0 + (x1 - x0) * t / 20, y0 + (y1 - y0) * t / 20
            s.set(int(x), int(y), pal.a[3])
    for (x, y) in ((8, 3), (3, 7), (12, 7), (5, 12), (10, 12)):
        s.set(x, y, pal.a[4])
    _disc(s, 8, 8, 1.2, lambda x, y: pal.hot)
    for k in range(8):                                                             # rim studs
        a = k * math.pi / 4
        s.set(int(8 + 6.2 * math.cos(a)), int(8 + 6.2 * math.sin(a)), pal.a[2])
    return s.outline(strength=0.5)


@painter("egg")
def egg(pal, c1=0x555555, c2=0xaaaaaa, seed=1):
    """Spawn egg: ovoid in the first colour with speckles in the second (vanilla look)."""
    rnd = random.Random(seed)
    base, spot = ramp("#%06x" % c1), ramp("#%06x" % c2)
    s = Sprite()
    for y in range(16):
        for x in range(16):
            nx, ny = (x + 0.5 - 8) / 5.2, (y + 0.5 - 8.4) / 6.8
            if ny > 0:
                nx *= 1.0 + 0.0 * ny
            d = nx * nx + ny * ny
            if d <= 1.0:
                lit = -(nx * 0.6 + ny * 0.7)
                s.set(x, y, base[4] if lit > 0.55 else base[3] if lit > 0.15 else base[2] if lit > -0.35 else base[1])
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        if s.get(x, y) is not None:
            s.set(x, y, spot[3 if rnd.random() < 0.6 else 2])
    return s.outline(strength=0.5)


def altar_texture(pal, seed=7):
    """16x16 texture for the ritual altar model: dark stone with a glowing rune circle in the middle."""
    from PIL import Image
    rnd = random.Random(seed)
    stone, glow = pal.s, pal.a
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            i = 1 + (1 if n > 0.6 else 0) + (1 if n > 0.93 else 0) - (1 if n < 0.12 else 0)
            px[x, y] = stone[max(0, min(4, i))]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 4.6 <= d <= 5.6:
                px[x, y] = glow[3] if (x + y) % 3 else glow[4]
            elif d < 1.6:
                px[x, y] = pal.hot
    for k in range(4):                                                             # spokes
        a = k * math.pi / 2 + math.pi / 4
        for t in range(2, 9):
            px[int(8 + t * 0.62 * math.cos(a) * 1.2), int(8 + t * 0.62 * math.sin(a) * 1.2)] = glow[2]
    return im


ALTAR_MODEL = {
    "parent": "minecraft:block/block",
    "textures": {"all": "ultimatesurvival:block/ritual_altar", "particle": "ultimatesurvival:block/ritual_altar"},
    "elements": [
        {"from": [0, 0, 0], "to": [16, 4, 16]},
        {"from": [2, 4, 2], "to": [14, 6, 14]},
        {"from": [5, 6, 5], "to": [11, 11, 11]},
        {"from": [2, 11, 2], "to": [14, 13, 14]},
        {"from": [0, 4, 0], "to": [3, 12, 3]},
        {"from": [13, 4, 0], "to": [16, 12, 3]},
        {"from": [0, 4, 13], "to": [3, 12, 16]},
        {"from": [13, 4, 13], "to": [16, 12, 16]},
        {"from": [0.5, 12, 0.5], "to": [2.5, 15, 2.5]},
        {"from": [13.5, 12, 0.5], "to": [15.5, 15, 2.5]},
        {"from": [0.5, 12, 13.5], "to": [2.5, 15, 15.5]},
        {"from": [13.5, 12, 13.5], "to": [15.5, 15, 15.5]},
    ],
}
for _el in ALTAR_MODEL["elements"]:
    _el["faces"] = {d: {"texture": "#all"} for d in ("down", "up", "north", "south", "west", "east")}
