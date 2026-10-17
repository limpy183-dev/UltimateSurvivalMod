"""Item icon painters. Each painter takes a Pal (+ keyword params) and returns a Sprite.
Animation is applied afterwards by anim.py, so painters stay static and deterministic."""
import math
import random
from .sprite import Sprite
from .diag import Diag, to_ap
from .color import mix, lighten, darken, as_rgba

PAINTERS = {}


def painter(name):
    def deco(fn):
        PAINTERS[name] = fn
        return fn
    return deco


# ---------------------------------------------------------------------------------------------
# helpers that decorate a Diag via post-processing in (a, p) space
# ---------------------------------------------------------------------------------------------
def _each(s, fn):
    for y in range(s.h):
        for x in range(s.w):
            if s.get(x, y) is not None:
                a, p = to_ap(x, y)
                fn(s, x, y, a, p)


def glow_vein(d, a0, a1, col, c=0.0, w=0.32):
    def post(s):
        def f(s, x, y, a, p):
            if a0 <= a <= a1 and abs(p - c) <= w:
                t = (a - a0) / max(a1 - a0, 1e-6)
                s.set(x, y, mix(col, (255, 255, 255, 255), 0.25 + 0.35 * math.sin(t * 6.0) ** 2))
        _each(s, f)
    d.post.append(post)


def rune_dots(d, a0, a1, col, c=0.0, step=1.9, off=0.0):
    def post(s):
        def f(s, x, y, a, p):
            k = (a - a0) / step
            if a0 <= a <= a1 and abs(p - c) < 0.55 and abs(k - round(k)) < 0.28:
                s.set(x, y, col)
        _each(s, f)
    d.post.append(post)


def edge_sparks(d, a0, a1, col, side=1, density=0.55, seed=1):
    rnd = random.Random(seed)

    def post(s):
        # decorate pixels just outside the lit edge (flames / lightning / crystals)
        add = []
        for y in range(s.h):
            for x in range(s.w):
                if s.get(x, y) is None:
                    a, p = to_ap(x, y)
                    if a0 <= a <= a1 and 0.9 < p * side < 2.2 and rnd.random() < density:
                        # only attach when a neighbour belongs to the blade
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                            if s.get(x + dx, y + dy) is not None:
                                add.append((x, y))
                                break
        for x, y in add:
            s.set(x, y, col)
    d.post.append(post)


def serrate(d, a0, a1, rmp, side=1, every=2.0, depth=1.0, hw=1.2):
    """Add saw teeth on one edge as small boxes."""
    n = int((a1 - a0) / every)
    for i in range(n):
        a = a0 + (i + 0.5) * every
        d.box(a - 0.45, a + 0.45, side * (hw - 0.1), side * (hw + depth), rmp)


# ---------------------------------------------------------------------------------------------
# SWORD family
# ---------------------------------------------------------------------------------------------
GUARDS = {
    "cross": lambda d, pal, g: d.box(4.2, 5.6, -3.1, 3.1, g),
    "narrow": lambda d, pal, g: d.box(4.3, 5.3, -2.2, 2.2, g),
    "wing": lambda d, pal, g: (d.poly([(3.6, -0.6), (4.0, -3.6), (5.8, -3.4), (5.8, -0.6)], g),
                               d.poly([(3.6, 0.6), (4.0, 3.6), (5.8, 3.4), (5.8, 0.6)], g),
                               d.box(4.1, 5.9, -0.7, 0.7, g)),
    "round": lambda d, pal, g: d.ellipse(4.9, 0.0, 0.95, 3.0, g),
    "crown": lambda d, pal, g: (d.box(4.2, 5.4, -3.1, 3.1, g), d.box(5.3, 6.6, -3.1, -2.3, g),
                                d.box(5.3, 6.6, 2.3, 3.1, g), d.box(5.3, 6.4, -0.5, 0.5, g)),
    "basket": lambda d, pal, g: (d.ellipse(5.2, 0.0, 1.6, 2.9, g), d.ellipse(5.2, 0.0, 0.9, 1.7, pal.s)),
}
POMMELS = {
    "gem": lambda d, pal: d.disc(1.0, 0.0, 0.95, pal.a),
    "ball": lambda d, pal: d.disc(1.0, 0.0, 0.95, pal.s),
    "spike": lambda d, pal: d.taper(-0.2, 1.8, 0.2, 0.9, pal.s),
    "ring": lambda d, pal: d.disc(0.9, 0.0, 1.15, pal.p),
    "none": lambda d, pal: None,
}


@painter("sword")
def sword(pal, long=12.5, wide=1.25, curve=0.0, guard="cross", pommel="gem", edge=None,
          groove=0.28, vein=None, gcol=None, tip=0.25, grip=None, seed=1, runes=False, blade=None, offset=0.0):
    d = Diag()
    g = gcol if gcol is not None else pal.a
    o = offset                                                             # slide the whole weapon along its axis
    d.shift = o                                                            # (parts shift via Diag, post effects below add o)
    d.bar(0.9, 4.4, 0.55, grip or pal.s)                                   # grip
    POMMELS[pommel](d, pal)
    GUARDS[guard](d, pal, g)
    bl = blade or pal.p
    d.taper(5.4, 5.4 + long, wide, tip, bl, groove=groove, curve=curve)
    a0, a1 = 6.2 + o, 5.4 + o + long - 1.4
    if edge == "serrated":
        serrate(d, 7.0, a1 - o, bl, side=1, hw=wide * 0.9)
    elif edge == "flame":
        edge_sparks(d, 6.5 + o, a1, pal.glow, side=1, seed=seed)
        edge_sparks(d, 8.0 + o, a1, pal.hot, side=1, density=0.25, seed=seed + 7)
    elif edge == "lightning":
        edge_sparks(d, 6.5 + o, a1, pal.glow, side=-1, density=0.4, seed=seed)
        edge_sparks(d, 6.5 + o, a1, lighten(pal.glow, 0.5), side=1, density=0.3, seed=seed + 3)
    elif edge == "crystal":
        edge_sparks(d, 7.0 + o, a1, pal.p[3], side=1, density=0.5, seed=seed)
        edge_sparks(d, 7.0 + o, a1, pal.p[3], side=-1, density=0.4, seed=seed + 5)
    if vein:
        glow_vein(d, a0 + 0.6, a1 - 0.8, vein if not isinstance(vein, bool) else pal.glow)
    if runes:
        rune_dots(d, a0 + 0.8, a1 - 1.0, pal.glow)
    return d.render(outline_strength=0.5)


# ---------------------------------------------------------------------------------------------
# INGOT / bar
# ---------------------------------------------------------------------------------------------
@painter("ingot")
def ingot(pal, gloss=True, rmp=None, rune=False):
    r = rmp or pal.p
    s = Sprite()
    top = [(2.0, 8.0), (6.0, 4.0), (14.0, 4.0), (10.0, 8.0)]
    front = [(2.0, 8.0), (10.0, 8.0), (10.0, 12.0), (2.0, 12.0)]
    side = [(10.0, 8.0), (14.0, 4.0), (14.0, 8.0), (10.0, 12.0)]
    s.fill_poly(front, lambda x, y: r[2] if y < 11 else r[1])
    s.fill_poly(side, r[1])
    s.fill_poly(top, lambda x, y: r[4] if y == 4 else r[3])
    # bright bevel along the near-top edge
    for x in range(3, 10):
        s.set(x, 8, r[3])
    if gloss:
        for (x, y) in ((6, 5), (7, 5), (3, 8)):
            if s.get(x, y):
                s.set(x, y, lighten(r[4], 0.35))
    if rune:
        for (x, y) in ((5, 10), (7, 10)):
            s.set(x, y, pal.glow)
    return s.outline(strength=0.5)
