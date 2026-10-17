"""Pickaxe / axe / shovel / hoe, staffs and bows (diagonal-axis painters)."""
import math
from .diag import Diag
from .color import lighten
from .painters import painter, glow_vein, rune_dots


def _handle(d, pal, top=13.0, grip=None):
    d.bar(0.6, top, 0.55, grip or pal.s)
    d.disc(0.9, 0.0, 0.8, pal.s)


@painter("pickaxe")
def pickaxe(pal, bend=2.8, span=6.2, thick=2.3, vein=False, spikes=False, head=None, grip=None):
    d = Diag()
    _handle(d, pal, 14.0, grip)
    h = head or pal.p
    d.arc(13.4, thick, span, bend, h)
    d.box(12.4, 14.6, -0.9, 0.9, pal.s)                                    # collar
    d.dot(13.6, 0.0, pal.a[3], 0.7)
    if spikes:
        d.dot(11.2, span - 0.4, pal.glow, 0.6)
        d.dot(11.2, -span + 0.4, pal.glow, 0.6)
    if vein:
        glow_vein(d, 11.5, 15.0, pal.glow, c=0.0, w=0.35)
    return d.render(outline_strength=0.5)


@painter("axe")
def axe(pal, size=1.0, vein=False, head=None, grip=None, double=False):
    d = Diag()
    _handle(d, pal, 14.5, grip)
    h = head or pal.p
    s = size * 1.18
    d.poly([(10.6, 0.4), (15.9, 0.4), (16.6, 2.6 * s), (15.5, 5.0 * s), (11.4, 4.0 * s)], h)
    if double:
        d.poly([(10.6, -0.4), (15.8, -0.4), (16.4, -2.6 * s), (15.4, -5.0 * s), (11.6, -4.0 * s)], h)
    d.box(11.6, 14.2, -0.8, 0.8, pal.s)
    d.dot(13.4, 0.0, pal.a[3], 0.7)
    if vein:
        glow_vein(d, 11.8, 15.2, pal.glow, c=1.6, w=0.3)
    return d.render(outline_strength=0.5)


@painter("shovel")
def shovel(pal, rad=2.45, head=None, grip=None, vein=False, point=False):
    d = Diag()
    _handle(d, pal, 13.2, grip)
    h = head or pal.p
    d.ellipse(14.4, 0.0, rad * (1.25 if point else 1.0), rad * 0.88, h)
    if point:
        d.taper(14.0, 17.0, 1.1, 0.2, h)
    d.box(12.4, 13.6, -0.9, 0.9, pal.s)
    d.dot(14.6, 0.4, lighten(h[4], 0.3), 0.6)
    if vein:
        glow_vein(d, 13.6, 16.0, pal.glow, c=0.0, w=0.3)
    return d.render(outline_strength=0.5)


@painter("hoe")
def hoe(pal, reach=5.4, head=None, grip=None, vein=False, hook=True):
    d = Diag()
    _handle(d, pal, 14.4, grip)
    h = head or pal.p
    d.box(13.0, 14.8, 0.0, reach, h)
    if hook:
        d.box(11.6, 14.8, reach - 1.2, reach, h)
    d.box(12.6, 14.0, -0.8, 0.8, pal.s)
    d.dot(13.9, 1.2, lighten(h[4], 0.3), 0.55)
    if vein:
        glow_vein(d, 13.2, 14.4, pal.glow, c=2.2, w=0.3)
    return d.render(outline_strength=0.5)


@painter("staff")
def staff(pal, head="orb", runes=False, grip=None, crown=False):
    d = Diag()
    d.bar(0.6, 15.0, 0.55, grip or pal.s)
    d.disc(0.9, 0.0, 0.85, pal.p)
    if head == "orb":
        d.disc(16.0, 0.0, 2.1, pal.a)
        d.disc(16.0, 0.0, 1.0, [pal.hot] * 5)
    elif head == "crystal":
        d.poly([(14.4, 0.0), (16.0, 1.5), (18.2, 0.0), (16.0, -1.5)], pal.a)
        d.dot(16.0, 0.4, pal.hot, 0.7)
    elif head == "ring":
        d.ellipse(16.0, 0.0, 2.3, 2.3, pal.p)
        d.ellipse(16.0, 0.0, 1.2, 1.2, pal.s)
        d.dot(16.0, 0.0, pal.glow, 0.8)
    elif head == "crescent":
        d.arc(16.6, 1.8, 3.4, 2.6, pal.p)
        d.dot(15.6, 0.0, pal.glow, 0.9)
    elif head == "flame":
        d.taper(14.6, 18.0, 1.5, 0.2, pal.a)
        d.taper(14.6, 17.0, 0.8, 0.2, [pal.hot] * 5)
    elif head == "skull":
        d.disc(16.0, 0.0, 2.0, pal.s)
        d.dot(16.4, 0.7, pal.dark, 0.5)
        d.dot(16.4, -0.7, pal.dark, 0.5)
    d.box(13.4, 14.4, -1.0, 1.0, pal.p)
    if crown:
        d.dot(15.2, 2.0, pal.glow, 0.55)
        d.dot(15.2, -2.0, pal.glow, 0.55)
    if runes:
        rune_dots(d, 3.0, 12.0, pal.glow)
    return d.render(outline_strength=0.5)


def _seg_dist(a, p, a0, p0, a1, p1):
    abx, aby = a1 - a0, p1 - p0
    l2 = abx * abx + aby * aby
    t = max(0.0, min(1.0, ((a - a0) * abx + (p - p0) * aby) / l2)) if l2 else 0.0
    return math.hypot(a - (a0 + abx * t), p - (p0 + aby * t))


@painter("bow")
def bow(pal, bulge=3.6, tips=None, string=None, glow=False, limb=None):
    """Returns {'main','pull_0','pull_1','pull_2'}: idle + the three draw frames of the bow."""
    out = {}
    wood = limb or pal.s
    tipc = tips or pal.a
    strc = string or [(232, 232, 232, 255)] * 5
    for name, pull in (("main", 0.0), ("pull_0", 1.3), ("pull_1", 2.6), ("pull_2", 3.9)):
        d = Diag()

        def centre(a):
            t = (a - 9.5) / 7.5
            return bulge * (1 - t * t)

        d.parts.append((lambda a, p: 2.0 <= a <= 17.0 and abs(p - centre(a)) <= 0.85,
                        lambda a, p: wood[3] if p - centre(a) > 0.2 else wood[1] if p - centre(a) < -0.35 else wood[2]))
        d.dot(2.2, 0.0, tipc[3], 0.9)
        d.dot(16.8, 0.0, tipc[3], 0.9)
        d.dot(9.5, bulge, tipc[2], 0.8)                                       # grip wrap
        mid = -pull
        d.parts.append((lambda a, p, mid=mid: _seg_dist(a, p, 2.2, 0.0, 9.5, mid) < 0.42 or _seg_dist(a, p, 9.5, mid, 16.8, 0.0) < 0.42,
                        lambda a, p: strc[3]))
        if glow:
            glow_vein(d, 4.0, 15.0, pal.glow, c=bulge * 0.6, w=0.3)
        out[name] = d.render(outline_strength=0.5)
    return out
