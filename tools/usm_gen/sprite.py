"""Tiny pixel canvas on top of Pillow, built for hand-shaded-looking procedural icons."""
from PIL import Image
from .color import darken, mix, as_rgba


class Sprite:
    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.g = [[None] * w for _ in range(h)]

    # ---- basic access -------------------------------------------------
    def inb(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def get(self, x, y):
        return self.g[y][x] if self.inb(x, y) else None

    def set(self, x, y, c):
        if self.inb(x, y):
            self.g[y][x] = None if c is None else as_rgba(c)

    def copy(self):
        s = Sprite(self.w, self.h)
        s.g = [row[:] for row in self.g]
        return s

    def paste(self, other, ox=0, oy=0):
        for y in range(other.h):
            for x in range(other.w):
                c = other.g[y][x]
                if c is not None:
                    self.set(x + ox, y + oy, c)

    def opaque(self):
        return [(x, y) for y in range(self.h) for x in range(self.w) if self.g[y][x] is not None]

    def fill_poly(self, pts, color):
        """Fill a polygon given in pixel-corner coordinates. `color` is rgba or f(x, y) -> rgba."""
        n = len(pts)
        for y in range(self.h):
            for x in range(self.w):
                cx, cy = x + 0.5, y + 0.5
                inside = False
                j = n - 1
                for i in range(n):
                    xi, yi = pts[i]
                    xj, yj = pts[j]
                    if (yi > cy) != (yj > cy) and cx < (xj - xi) * (cy - yi) / (yj - yi) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.set(x, y, color(x, y) if callable(color) else color)
        return self

    def fill_rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color(x, y) if callable(color) else color)
        return self

    # ---- effects ------------------------------------------------------
    def outline(self, color=None, strength=0.62, diagonal=False):
        """Add a 1px outline around the silhouette. With color=None the outline takes the
        darkened colour of the neighbouring pixel (selective outline: softer, more 'hand-made')."""
        add = []
        nbrs = [(1, 0), (-1, 0), (0, 1), (0, -1)]
        if diagonal:
            nbrs += [(1, 1), (-1, 1), (1, -1), (-1, -1)]
        for y in range(self.h):
            for x in range(self.w):
                if self.g[y][x] is not None:
                    continue
                best = None
                for dx, dy in nbrs:
                    c = self.get(x + dx, y + dy)
                    if c is not None:
                        if best is None or sum(c[:3]) < sum(best[:3]):
                            best = c
                if best is not None:
                    add.append((x, y, as_rgba(color) if color else darken(best, strength)))
        for x, y, c in add:
            self.g[y][x] = c
        return self

    def tint_glow(self, color, positions, strength=1.0):
        for x, y in positions:
            if self.get(x, y) is not None:
                self.set(x, y, mix(self.get(x, y), as_rgba(color), strength))

    def flip_h(self):
        s = Sprite(self.w, self.h)
        s.g = [row[::-1] for row in self.g]
        return s

    # ---- IO -----------------------------------------------------------
    def image(self):
        im = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        px = im.load()
        for y in range(self.h):
            for x in range(self.w):
                c = self.g[y][x]
                if c is not None:
                    px[x, y] = c
        return im

    @staticmethod
    def from_rows(rows, legend):
        """ASCII template -> sprite. `legend` maps a char to an RGBA colour ('.' / ' ' = clear)."""
        h = len(rows)
        w = max(len(r) for r in rows)
        s = Sprite(w, h)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in ". ":
                    continue
                c = legend.get(ch)
                if c is not None:
                    s.set(x, y, c)
        return s


def strip(frames):
    """Stack equally sized sprites vertically (Minecraft animation strip)."""
    w, h = frames[0].w, frames[0].h
    im = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        im.paste(f.image(), (0, i * h))
    return im
