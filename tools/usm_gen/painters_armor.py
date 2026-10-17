"""Armor: 16x16 item icons per piece and the 64x32 worn-armor layer textures (vanilla UV layout)."""
import random
from PIL import Image
from .sprite import Sprite
from .color import lighten, darken, mix
from .painters import painter

HELMET, CHEST, LEGS, BOOTS = "helmet", "chestplate", "leggings", "boots"


def _grad(r, x0, y0, x1, y1, rim=True):
    """Shade function: lit from the upper-left, dark toward the lower-right."""
    span = max((x1 - x0) + (y1 - y0), 1e-6)

    def f(x, y):
        t = ((x - x0) + (y - y0)) / span
        i = 4 if t < 0.12 else 3 if t < 0.38 else 2 if t < 0.62 else 1 if t < 0.88 else 0
        return r[i]
    return f


def _poly(s, pts, r):
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    s.fill_poly(pts, _grad(r, min(xs), min(ys), max(xs), max(ys)))


def _mirror(pts):
    return [(16 - x, y) for x, y in pts]


@painter("armor_icon")
def armor_icon(pal, piece=CHEST, horns=False, crest=False, spikes=False, visor="slit", gem=True, trim=None, wings=False, glow=False):
    s = Sprite()
    P, S, A = pal.p, pal.s, pal.a
    tr = trim or S
    if piece == HELMET:
        dome = [(3, 12.5), (3, 7), (4, 5), (6.5, 3.2), (9.5, 3.2), (12, 5), (13, 7), (13, 12.5), (10.5, 12.5), (10.5, 10.2), (5.5, 10.2), (5.5, 12.5)]
        if horns:
            _poly(s, [(3.2, 6.5), (1.0, 3.5), (1.4, 1.2), (3.6, 4.4), (4.4, 5.5)], A)
            _poly(s, _mirror([(3.2, 6.5), (1.0, 3.5), (1.4, 1.2), (3.6, 4.4), (4.4, 5.5)]), A)
        if crest:
            _poly(s, [(7.2, 1.2), (8.8, 1.2), (9.6, 3.6), (6.4, 3.6)], A)
        _poly(s, dome, P)
        s.fill_rect(3, 7, 12, 7, tr[2])                                         # brow band
        if visor == "slit":
            s.fill_rect(5, 8, 10, 9, pal.dark)
        elif visor == "cross":
            s.fill_rect(5, 8, 10, 8, pal.dark)
            s.fill_rect(7, 8, 8, 11, pal.dark)
        elif visor == "full":
            s.fill_rect(5, 8, 10, 10, pal.dark)
        if glow:
            for x in (6, 9):
                s.set(x, 9 if visor == "slit" else 8, pal.glow)
        if gem:
            s.set(7, 5, A[3]); s.set(8, 5, A[4]); s.set(8, 6, A[2])
    elif piece == CHEST:
        if wings:
            _poly(s, [(2, 5), (0.2, 3), (0.6, 8), (2.4, 9)], A)
            _poly(s, _mirror([(2, 5), (0.2, 3), (0.6, 8), (2.4, 9)]), A)
        body = [(2.5, 4.5), (5.5, 3), (6.8, 4.2), (9.2, 4.2), (10.5, 3), (13.5, 4.5), (14, 8.5), (12.2, 8.8), (11.8, 13.5), (4.2, 13.5), (3.8, 8.8), (2, 8.5)]
        if spikes:
            _poly(s, [(2.6, 4.6), (1.4, 1.8), (4.2, 3.6)], A)
            _poly(s, _mirror([(2.6, 4.6), (1.4, 1.8), (4.2, 3.6)]), A)
        _poly(s, body, P)
        s.fill_rect(7, 3, 8, 4, pal.dark)                                       # neck hole
        s.fill_rect(4, 11, 11, 11, tr[2])                                       # belt
        s.fill_rect(5, 7, 10, 7, tr[3])                                         # chest line
        if gem:
            s.fill_rect(7, 8, 8, 9, A[3]); s.set(7, 8, A[4])
        if glow:
            s.set(5, 9, pal.glow); s.set(10, 9, pal.glow); s.set(8, 10, pal.glow)
    elif piece == LEGS:
        legs = [(3.8, 3), (12.2, 3), (12.8, 6), (11.6, 13.8), (8.8, 13.8), (8, 7.2), (7.2, 13.8), (4.4, 13.8), (3.2, 6)]
        _poly(s, legs, P)
        s.fill_rect(4, 3, 11, 4, tr[2])                                         # belt
        if gem:
            s.set(7, 3, A[4]); s.set(8, 3, A[4])
        s.fill_rect(4, 9, 6, 10, tr[3]); s.fill_rect(9, 9, 11, 10, tr[3])      # knee guards
        if glow:
            s.set(5, 7, pal.glow); s.set(10, 7, pal.glow)
    else:
        boot = [(2.2, 6.5), (6.2, 6.5), (6.2, 10), (7.6, 11.4), (7.6, 13.6), (2.2, 13.6)]
        _poly(s, boot, P)
        _poly(s, _mirror(boot), P)
        for x0, x1 in ((2, 5), (10, 13)):
            s.fill_rect(x0, 6, x1, 7, tr[3])                                    # cuff
            s.fill_rect(x0, 12, x1 + 1 if x0 < 8 else x1, 13, S[1])           # sole
        if gem:
            s.set(4, 9, A[3]); s.set(11, 9, A[3])
        if glow:
            s.set(3, 11, pal.glow); s.set(12, 11, pal.glow)
    return s.outline(strength=0.5)


# ---------------------------------------------------------------------------------------------
# worn-armor textures
# ---------------------------------------------------------------------------------------------
def _face_rects(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def _plate(img, rect, pal, rnd, kind="base", light=0):
    """One armor face: bevelled plate, a seam through the middle on larger faces, a few rivets, a touch of dithering."""
    x0, y0, w, h = rect
    px = img.load()
    P = pal.p

    def put(x, y, i):
        px[x0 + x, y0 + y] = P[max(0, min(4, i + light))]

    for y in range(h):
        for x in range(w):
            n = rnd.random()
            idx = 2 + (1 if n > 0.94 else -1 if n < 0.05 else 0)
            if y == 0:
                idx = 4 if x < w - 1 else 3
            elif x == 0:
                idx = 3
            elif y == h - 1:
                idx = 0
            elif x == w - 1:
                idx = 1
            put(x, y, idx)
    if w >= 6:                                  # vertical seam + its highlight
        m = w // 2
        for y in range(1, h - 1):
            put(m, y, 1)
            if m + 1 < w - 1:
                put(m + 1, y, 3)
    if h >= 8:                                  # horizontal seam
        m = h // 2
        for x in range(1, w - 1):
            put(x, m, 1)
            if m + 1 < h - 1:
                put(x, m + 1, 3)
    if w >= 4 and h >= 4:                       # rivets
        for cx, cy in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            px[x0 + cx, y0 + cy] = pal.a[3]


def _line(img, x0, y0, x1, y1, color):
    px = img.load()
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        x = round(x0 + (x1 - x0) * i / steps)
        y = round(y0 + (y1 - y0) * i / steps)
        if 0 <= x < img.width and 0 <= y < img.height:
            px[x, y] = color


def _rect(img, x0, y0, x1, y1, color):
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < img.width and 0 <= y < img.height:
                px[x, y] = color


def glow_layer(img, pal):
    """The emissive part of a worn-armor sheet: every pixel painted in the palette's glow colour (eyes, runes, sigils), alone on a transparent sheet."""
    colour = tuple(pal.glow)
    colour = colour + (255,) if len(colour) == 3 else colour
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    src, dst = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y] == colour:
                dst[x, y] = colour
    return out


def armor_layers(pal, visor="slit", glow=True, trim=None, emblem="gem", seed=7):
    """Return (humanoid, leggings) 64x32 RGBA images."""
    rnd = random.Random(seed)
    tr = trim or pal.s
    layers = []
    for which in ("humanoid", "leggings"):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        # head (+ hat region mirrored for no see-through)
        if which == "humanoid":
            for u0 in (0, 32):
                for name, rect in _face_rects(u0, 0, 8, 8, 8).items():
                    _plate(img, rect, pal, rnd, light=1 if name == "top" else 0)
            fx, fy, fw, fh = _face_rects(0, 0, 8, 8, 8)["front"]
            _rect(img, fx + 1, fy + 3, fx + fw - 2, fy + 4, pal.dark)               # visor slit
            if visor == "full":
                _rect(img, fx + 1, fy + 3, fx + fw - 2, fy + 6, pal.dark)
            if glow:
                _rect(img, fx + 2, fy + 4, fx + 2, fy + 4, pal.glow)
                _rect(img, fx + fw - 3, fy + 4, fx + fw - 3, fy + 4, pal.glow)
            _rect(img, fx, fy, fx + fw - 1, fy, tr[3])                               # brow
            if emblem == "gem":
                _rect(img, fx + 3, fy, fx + 4, fy + 1, pal.a[3])
        # body / arms / legs, drawn in both layers (each layer's armor pieces pick what they need)
        parts = [((16, 16), (8, 12, 4)), ((40, 16), (4, 12, 4)), ((0, 16), (4, 12, 4))]
        for (u, v), (w, h, d) in parts:
            for name, rect in _face_rects(u, v, w, h, d).items():
                _plate(img, rect, pal, rnd, light=1 if name == "top" else (-1 if name == "bottom" else 0))
        # chest emblem + belt (body front)
        bx, by, bw, bh = _face_rects(16, 16, 8, 12, 4)["front"]
        _rect(img, bx, by + bh - 4, bx + bw - 1, by + bh - 4, tr[3])
        _rect(img, bx, by + bh - 3, bx + bw - 1, by + bh - 3, tr[1])
        if which == "humanoid":
            _rect(img, bx + 3, by + 3, bx + 4, by + 5, pal.a[3])
            _rect(img, bx + 3, by + 3, bx + 3, by + 3, pal.a[4])
            if glow:
                _line(img, bx + 1, by + 2, bx + 2, by + 5, pal.glow)
                _line(img, bx + bw - 2, by + 2, bx + bw - 3, by + 5, pal.glow)
                _rect(img, bx + 3, by + 6, bx + 4, by + 7, pal.glow)                  # a sigil above the belt
                ax, ay, aw, ah = _face_rects(40, 16, 4, 12, 4)["front"]
                _line(img, ax + 1, ay + 3, ax + 1, ay + 8, pal.glow)                  # a rune down each arm
                _rect(img, ax + 2, ay + 5, ax + 2, ay + 5, pal.glow)
            # shoulder trim on arms
            for u in (40,):
                for name, rect in _face_rects(u, 16, 4, 12, 4).items():
                    if name in ("front", "back", "left", "right"):
                        _rect(img, rect[0], rect[1], rect[0] + rect[2] - 1, rect[1] + 1, tr[3])
                        _rect(img, rect[0], rect[1] + rect[3] - 2, rect[0] + rect[2] - 1, rect[1] + rect[3] - 1, tr[1])
        # leg details: knee guards + boot soles
        for name, rect in _face_rects(0, 16, 4, 12, 4).items():
            if name in ("front", "back", "left", "right"):
                x, y, w, h = rect
                if which == "leggings":
                    _rect(img, x, y + 5, x + w - 1, y + 6, tr[3])
                    if glow and name == "front":
                        _rect(img, x + 1, y + 5, x + 2, y + 5, pal.glow)
                else:
                    _rect(img, x, y + h - 3, x + w - 1, y + h - 3, tr[3])
                    _rect(img, x, y + h - 2, x + w - 1, y + h - 1, tr[1])
        layers.append(img)
    return layers[0], layers[1]
