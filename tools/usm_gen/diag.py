"""Procedural painter for items that lie on the classic bottom-left -> top-right diagonal.

Every pixel is mapped to (a, p): `a` = distance along the item axis from the grip end,
`p` = signed distance across it (positive = upper-left = lit side). Parts are shapes in that
space shaded with a 5-step ramp, which makes weapon silhouettes parametric and easy to vary."""
import math
from .sprite import Sprite

SQ2 = math.sqrt(2.0)
OX, OY = 1.5, 14.5      # pixel-centre of the grip end


def to_ap(x, y):
    px, py = x + 0.5, y + 0.5
    return ((px - OX) - (py - OY)) / SQ2, (-(px - OX) - (py - OY)) / SQ2


def shade(q):
    """q = normalised cross offset (-1 dark side .. +1 lit side) -> ramp index 0..4."""
    if q > 0.55:
        return 4
    if q > 0.12:
        return 3
    if q > -0.38:
        return 2
    return 1


class Diag:
    def __init__(self):
        self.parts = []          # (test(a,p)->bool, color(a,p)->rgba)
        self.post = []           # callables(sprite) run after rendering (glints, runes ...)
        self.shift = 0.0         # slides every part along the axis (daggers sit lower, greatswords higher)

    # ---- primitives -----------------------------------------------------------
    def bar(self, a0, a1, hw, rmp, c=0.0, flat=False):
        def test(a, p):
            return a0 <= a <= a1 and abs(p - c) <= hw

        def col(a, p):
            if flat:
                return rmp[2]
            return rmp[shade((p - c) / max(hw, 0.6))]
        self.parts.append((test, col))
        return self

    def taper(self, a0, a1, hw0, hw1, rmp, c=0.0, groove=0.0, tip_cut=0.0, edge=None, curve=0.0):
        """Blade-like shape: half-width interpolates hw0->hw1 over [a0, a1]. `groove` darkens a
        centre fuller; `edge` lights the leading (p>0) edge with a colour; `curve` bends the
        blade sideways (scimitar / katana) by that many units at the tip."""
        def hw(a):
            return hw0 + (hw1 - hw0) * ((a - a0) / (a1 - a0))

        def cen(a):
            return c + curve * ((a - a0) / (a1 - a0)) ** 2

        def test(a, p):
            if not (a0 <= a <= a1 - tip_cut):
                return False
            return abs(p - cen(a)) <= hw(a)

        def col(a, p):
            h = max(hw(a), 0.6)
            q = (p - cen(a)) / h
            i = shade(q)
            if groove and abs(p - cen(a)) < groove:
                i = max(0, i - 1)
            if edge is not None and q > 0.72:
                return edge
            return rmp[i]
        self.parts.append((test, col))
        return self

    def box(self, a0, a1, p0, p1, rmp, flat=False):
        def test(a, p):
            return a0 <= a <= a1 and p0 <= p <= p1

        def col(a, p):
            if flat:
                return rmp[2]
            mid = (p0 + p1) / 2
            return rmp[shade((p - mid) / max((p1 - p0) / 2, 0.6))]
        self.parts.append((test, col))
        return self

    def disc(self, a, p0, r, rmp, hi=True):
        def test(aa, pp):
            return (aa - a) ** 2 + (pp - p0) ** 2 <= r * r

        def col(aa, pp):
            # light from upper-left of the screen == +p and -a-ish
            d = ((pp - p0) - (aa - a) * 0.35) / max(r, 0.6)
            return rmp[shade(d)]
        self.parts.append((test, col))
        return self

    def ellipse(self, a, p0, ra, rp, rmp):
        def test(aa, pp):
            return ((aa - a) / ra) ** 2 + ((pp - p0) / rp) ** 2 <= 1.0

        def col(aa, pp):
            return rmp[shade(((pp - p0) / rp) - ((aa - a) / ra) * 0.3)]
        self.parts.append((test, col))
        return self

    def arc(self, a_c, thick, p_max, bend, rmp, side=0, a_off=0.0):
        """Crescent/bent bar crossing the axis: a = a_c - bend*(p/p_max)^2 +/- thick/2 for |p|<=p_max.
        side: 0 both sides, +1 only p>0, -1 only p<0."""
        def centre(p):
            return a_c - bend * (p / p_max) ** 2

        def test(a, p):
            if abs(p) > p_max:
                return False
            if side > 0 and p < -0.4:
                return False
            if side < 0 and p > 0.4:
                return False
            return abs(a - centre(p)) <= thick / 2

        def col(a, p):
            return rmp[shade(((a_c - a) / max(thick / 2, 0.6)) * 0.9 + (p / p_max) * 0.1)]
        self.parts.append((test, col))
        return self

    def poly(self, pts, rmp, flat=False):
        """Convex polygon in (a, p) space."""
        def test(a, p):
            sign = 0
            n = len(pts)
            for i in range(n):
                a1, p1 = pts[i]
                a2, p2 = pts[(i + 1) % n]
                cross = (a2 - a1) * (p - p1) - (p2 - p1) * (a - a1)
                if cross != 0:
                    s = 1 if cross > 0 else -1
                    if sign and s != sign:
                        return False
                    sign = s
            return True

        cx = sum(q[0] for q in pts) / len(pts)
        cp = sum(q[1] for q in pts) / len(pts)
        ext = max(abs(q[1] - cp) for q in pts) or 1.0

        def col(a, p):
            return rmp[2] if flat else rmp[shade((p - cp) / ext)]
        self.parts.append((test, col))
        return self

    def dot(self, a, p, color, r=0.55):
        """Single bright pixel-ish accent (gem glint, rune)."""
        def test(aa, pp):
            return (aa - a) ** 2 + (pp - p) ** 2 <= r * r

        self.parts.append((test, lambda aa, pp: color))
        return self

    # ---- render ---------------------------------------------------------------
    def render(self, outline=True, outline_strength=0.6):
        s = Sprite()
        for y in range(16):
            for x in range(16):
                a, p = to_ap(x, y)
                a -= self.shift
                col = None
                for test, cf in self.parts:      # later parts paint over earlier ones
                    if test(a, p):
                        col = cf(a, p)
                if col is not None:
                    s.set(x, y, col)
        for fn in self.post:
            fn(s)
        if outline:
            s.outline(strength=outline_strength)
        return s
