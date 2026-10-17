"""GUI art: the Codex book panel, tab/status icons and the boss emblems (also reused as sigil/trophy icons)."""
import math
import random
from PIL import Image
from .sprite import Sprite
from .color import ramp, lighten, darken, rainbow, hexc
from .painters import painter, PAINTERS
from .painters_misc import _disc, _lit

PANEL_W, PANEL_H = 360, 220


def codex_panel():
    """The open book: violet leather cover, gold trim, two parchment pages with a shaded spine."""
    W, H = PANEL_W, PANEL_H
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(5)
    for y in range(H):
        for x in range(W):
            n = rnd.randint(-5, 5)
            px[x, y] = (37 + n, 22 + n // 2, 58 + n, 255)

    def box(x0, y0, x1, y1, col):
        for x in range(x0, x1 + 1):
            px[x, y0] = col
            px[x, y1] = col
        for y in range(y0, y1 + 1):
            px[x0, y] = col
            px[x1, y] = col

    box(0, 0, W - 1, H - 1, (13, 7, 22, 255))
    box(1, 1, W - 2, H - 2, (80, 52, 120, 255))
    box(3, 3, W - 4, H - 4, (142, 110, 36, 255))
    box(4, 4, W - 5, H - 5, (214, 178, 78, 255))
    # studs in the cover corners
    for cx, cy in ((7, 7), (W - 8, 7), (7, H - 8), (W - 8, H - 8)):
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                px[cx + dx, cy + dy] = (232, 198, 96, 255) if dx + dy < 1 else (150, 112, 40, 255)
    # parchment
    x0, y0, x1, y1 = 10, 10, W - 11, H - 11
    mid = W / 2
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = abs(x + 0.5 - mid) / (mid - x0)                      # 0 at the spine .. 1 at the outer edge
            shade = 1.0 - 0.30 * max(0.0, 1.0 - t * 4.2)             # spine shadow
            if t > 0.94:
                shade *= 0.90
            if y < y0 + 2 or y > y1 - 2:
                shade *= 0.93
            n = rnd.uniform(-5, 5)
            r, g, b = 226, 210, 170
            px[x, y] = (int(r * shade + n), int(g * shade + n), int(b * shade + n * 0.8), 255)
    # spine
    for y in range(y0, y1 + 1):
        for x in range(int(mid) - 2, int(mid) + 2):
            px[x, y] = (46, 30, 24, 255) if x in (int(mid) - 1, int(mid)) else (92, 66, 46, 255)
    for y in range(y0 + 8, y1, 14):
        px[int(mid) - 1, y] = (214, 178, 78, 255)
        px[int(mid), y] = (214, 178, 78, 255)
    # inner page frame + corner filigree
    for (a0, b0, a1, b1) in ((x0 + 5, y0 + 5, int(mid) - 6, y1 - 5), (int(mid) + 5, y0 + 5, x1 - 5, y1 - 5)):
        box(a0, b0, a1, b1, (170, 138, 96, 255))
        for (cx, cy, sx, sy) in ((a0, b0, 1, 1), (a1, b0, -1, 1), (a0, b1, 1, -1), (a1, b1, -1, -1)):
            for k in range(6):
                px[cx + sx * k, cy] = (176, 136, 52, 255)
                px[cx, cy + sy * k] = (176, 136, 52, 255)
            px[cx + sx * 2, cy + sy * 2] = (214, 178, 78, 255)
            px[cx + sx * 3, cy + sy * 3] = (176, 136, 52, 255)
    return im


# ---------------------------------------------------------------------------------------------
# tab / status icons (16x16 each, one strip)
# ---------------------------------------------------------------------------------------------
GOLD = ramp("#d9b24a")
BONE = ramp("#e8dcc8")


def _icon_boss():
    s = Sprite()
    s.fill_poly([(4, 4), (6, 7), (8, 3), (10, 7), (12, 4), (12, 8), (4, 8)], GOLD[3])          # crown
    s.fill_poly([(4, 8), (12, 8), (12, 12), (10.5, 12), (10.5, 14), (5.5, 14), (5.5, 12), (4, 12)], _lit(BONE, 8, 11, 5))
    s.set(6, 10, (40, 28, 50, 255))
    s.set(9, 10, (40, 28, 50, 255))
    return s.outline(strength=0.5)


def _icon_items():
    return PAINTERS["sword"](__import__("usm_gen.materials", fromlist=["PALS"]).PALS["azurite"], long=9, wide=1.0, guard="cross",
                             pommel="ball", offset=2.6)


def _icon_mob():
    s = Sprite()
    s.fill_poly([(3, 5), (13, 5), (13, 13), (3, 13)], _lit(ramp("#6fa85a"), 8, 9, 6))
    s.fill_poly([(3, 5), (5, 2), (6, 5)], GOLD[3])
    s.fill_poly([(13, 5), (11, 2), (10, 5)], GOLD[3])
    s.fill_rect(5, 7, 6, 8, (255, 230, 90, 255))
    s.fill_rect(10, 7, 11, 8, (255, 230, 90, 255))
    s.fill_rect(5, 11, 11, 11, (30, 20, 30, 255))
    s.set(6, 12, BONE[4])
    s.set(10, 12, BONE[4])
    return s.outline(strength=0.5)


def _icon_progress():
    s = Sprite()
    pts = []
    for i in range(10):
        a = -math.pi / 2 + math.pi * i / 5
        r = 6.6 if i % 2 == 0 else 2.8
        pts.append((8 + math.cos(a) * r, 8.6 + math.sin(a) * r))
    s.fill_poly(pts, _lit(GOLD, 8, 8, 6))
    s.set(7, 5, (255, 250, 200, 255))
    return s.outline(strength=0.5)


def _icon_tip():
    s = Sprite()
    _disc(s, 8, 6.5, 4.6, _lit(ramp("#ffe066"), 8, 6, 4.6))
    s.fill_rect(6, 11, 10, 13, ramp("#8a8a96")[2])
    s.fill_rect(7, 14, 9, 14, ramp("#8a8a96")[1])
    s.set(6, 5, (255, 255, 255, 255))
    return s.outline(strength=0.5)


def _icon_lock():
    s = Sprite()
    s.fill_rect(4, 7, 11, 13, ramp("#d9b24a")[2])
    s.fill_rect(4, 7, 11, 8, ramp("#d9b24a")[3])
    for y in range(3, 8):
        s.set(5, y, ramp("#b8c4d8")[3])
        s.set(10, y, ramp("#b8c4d8")[3])
    s.fill_rect(6, 3, 9, 4, ramp("#b8c4d8")[3])
    s.set(7, 10, (30, 20, 20, 255))
    s.set(8, 10, (30, 20, 20, 255))
    s.set(7, 11, (30, 20, 20, 255))
    return s.outline(strength=0.5)


def _icon_question():
    s = Sprite()
    rows = [
        ".####.", "##..##", "...##.", "..##..", "..##..", "......", "..##..", "..##..",
    ]
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == "#":
                s.set(5 + x, 3 + y, (210, 190, 230, 255))
    return s.outline(strength=0.4)


def _icon_check():
    s = Sprite()
    for (x, y) in ((3, 8), (4, 9), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 4)):
        s.set(x, y, (90, 220, 120, 255))
        s.set(x, y + 1, (50, 160, 80, 255))
    return s.outline(strength=0.4)


def codex_icons():
    """Strip: boss, items, mobs, progression, tips, lock, question, check (16x16 each)."""
    names = (_icon_boss, _icon_items, _icon_mob, _icon_progress, _icon_tip, _icon_lock, _icon_question, _icon_check)
    strip = Image.new("RGBA", (16 * len(names), 16), (0, 0, 0, 0))
    for i, fn in enumerate(names):
        strip.paste(fn().image(), (16 * i, 0))
    return strip


# ---------------------------------------------------------------------------------------------
# boss emblems (32x32)
# ---------------------------------------------------------------------------------------------
def _medallion(s, ring):
    _disc(s, 16, 16, 15.2, lambda x, y: ring[3] if (x + y) < 30 else ring[1])
    _disc(s, 16, 16, 13.2, lambda x, y: (16, 12, 28, 255))
    _disc(s, 16, 16, 12.2, lambda x, y: (26, 20, 44, 255))


def _emblem_flame(s, c):
    s.fill_poly([(16, 5), (22, 13), (21.5, 20), (16, 26), (10.5, 20), (10, 13), (13.5, 12)], lambda x, y: c[3] if y < 17 else c[2])
    s.fill_poly([(16, 11), (19.5, 17), (16, 24), (12.5, 17)], lambda x, y: lighten(c[4], 0.2) if y < 19 else c[4])
    s.fill_poly([(16, 16), (18, 20), (16, 23), (14, 20)], (255, 250, 200, 255))


def _emblem_bolt(s, c):
    s.fill_poly([(19, 5), (10, 17), (15, 17), (12, 27), (23, 13), (17.5, 13)], lambda x, y: c[4] if x < 17 else c[3])
    s.fill_poly([(18, 8), (13, 16), (16.5, 16), (14.5, 22), (20, 14), (16, 14)], (255, 255, 235, 255))


def _emblem_drop(s, c):
    s.fill_poly([(16, 5), (22.5, 15), (22, 21), (16, 26), (10, 21), (9.5, 15)], _lit(c, 14, 14, 9))
    s.fill_poly([(16, 9), (19.5, 15), (16, 22), (12.5, 15)], lambda x, y: c[1] if y > 16 else c[2])
    s.set(13, 13, (255, 200, 210, 255)); s.set(13, 14, (255, 160, 180, 255))
    for k in range(3):                                      # fangs
        s.set(13 + k * 3, 24 + (k % 2), BONE[4])


def _emblem_hourglass(s, c):
    s.fill_rect(8, 6, 23, 7, c[4])
    s.fill_rect(8, 24, 23, 25, c[4])
    s.fill_poly([(9, 8), (23, 8), (16.5, 16)], (54, 90, 98, 255))
    s.fill_poly([(9, 24), (23, 24), (16.5, 16)], (54, 90, 98, 255))
    s.fill_poly([(11, 9), (21, 9), (16, 15)], c[3])
    s.fill_poly([(13, 23), (19, 23), (16, 20)], c[2])
    s.fill_rect(16, 15, 16, 19, c[3])


def _emblem_void(s, c):
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            r = math.hypot(dx, dy)
            if r > 11.6:
                continue
            a = math.atan2(dy, dx)
            swirl = (a + r * 0.55) % (math.pi * 2 / 3)
            if r < 3.2:
                s.set(x, y, (4, 2, 10, 255))
            elif swirl < 0.8:
                s.set(x, y, c[4] if r > 7 else c[3])
            elif swirl < 1.3:
                s.set(x, y, c[2])
            else:
                s.set(x, y, c[0])


def _emblem_sun(s, c):
    for k in range(8):
        a = math.pi * 2 * k / 8
        s.fill_poly([(16 + math.cos(a - 0.22) * 6, 16 + math.sin(a - 0.22) * 6), (16 + math.cos(a) * 12, 16 + math.sin(a) * 12),
                     (16 + math.cos(a + 0.22) * 6, 16 + math.sin(a + 0.22) * 6)], c[4] if k % 2 == 0 else c[3])
    _disc(s, 16, 16, 6.2, _lit(c, 15, 15, 6.2))
    _disc(s, 16, 16, 3.4, lambda x, y: (255, 252, 225, 255))


def _emblem_dragon(s, c):
    s.fill_poly([(4, 16), (16, 8), (28, 16), (16, 24)], lambda x, y: c[2] if y > 16 else c[3])
    s.fill_poly([(7.5, 16), (16, 10.5), (24.5, 16), (16, 21.5)], (14, 10, 24, 255))
    _disc(s, 16, 16, 4.6, _lit(c, 15, 15, 4.6))
    s.fill_poly([(15.2, 12.2), (16.8, 12.2), (16.8, 19.8), (15.2, 19.8)], (6, 4, 10, 255))
    s.set(13, 14, (255, 255, 255, 255))
    for (x, y) in ((3, 15), (2, 16), (3, 17), (28, 15), (29, 16), (28, 17)):
        s.set(x, y, c[4])


def _emblem_omni(s, c):
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            r = math.hypot(dx, dy)
            if 8.4 < r < 11.8:
                s.set(x, y, rainbow(((math.atan2(dy, dx) / (math.pi * 2)) % 1.0), sat=0.85, light=0.62))
    pts = []
    for i in range(10):
        a = -math.pi / 2 + math.pi * i / 5
        r = 7.4 if i % 2 == 0 else 3.2
        pts.append((16 + math.cos(a) * r, 16.4 + math.sin(a) * r))
    s.fill_poly(pts, lambda x, y: (240, 232, 255, 255) if x < 16 else (200, 188, 238, 255))
    for (x, y) in ((6, 9), (25, 22), (8, 24), (24, 8)):
        s.set(x, y, (255, 255, 255, 255))


def _emblem_snow(s, c):
    for k in range(6):
        a = math.pi * 2 * k / 6 - math.pi / 2
        ux, uy = math.cos(a), math.sin(a)
        for t in range(2, 12):                                           # one arm of the snowflake
            x, y = 16 + ux * t, 16 + uy * t
            s.set(int(x), int(y), (255, 255, 255, 255) if t % 4 == 0 else c[4])
            s.set(int(x - uy * 0.9), int(y + ux * 0.9), c[3])
        for t0 in (5, 8):                                                # side branches
            bx, by = 16 + ux * t0, 16 + uy * t0
            for sgn in (-1, 1):
                ba = a + sgn * math.pi / 3
                for t in range(1, 4):
                    s.set(int(bx + math.cos(ba) * t), int(by + math.sin(ba) * t), c[3])
    _disc(s, 16, 16, 2.6, lambda x, y: (255, 255, 255, 255))


def _emblem_spore(s, c):
    s.fill_poly([(6, 17), (8, 11), (12, 7.5), (16, 6.5), (20, 7.5), (24, 11), (26, 17)], lambda x, y: c[3] if y < 12 else c[2])
    s.fill_rect(5, 17, 26, 18, c[1])
    s.fill_poly([(12.5, 18), (19.5, 18), (19, 26), (13, 26)], lambda x, y: (214, 208, 180, 255) if x < 16 else (190, 184, 156, 255))
    for (x, y, r) in ((11, 11, 1.8), (17, 9.5, 1.5), (21, 13, 1.7), (15, 14, 1.2)):
        _disc(s, x, y, r, lambda xx, yy: (240, 236, 214, 255))
    for (x, y) in ((6, 24), (25, 23), (9, 27), (23, 27), (16, 29), (4, 20), (27, 19), (13, 3), (20, 4)):
        s.set(x, y, c[4])


def _emblem_star(s, c):
    pts = []
    for i in range(16):
        a = -math.pi / 2 + math.pi * i / 8
        r = 10.5 if i % 2 == 0 else 3.6
        pts.append((16 + math.cos(a) * r, 16 + math.sin(a) * r))
    s.fill_poly(pts, lambda x, y: (240, 244, 255, 255) if x < 16 else c[4])
    _disc(s, 16, 16, 2.6, lambda x, y: (255, 255, 255, 255))
    for k in range(48):                                                  # a tilted orbit with one world on it
        a = k * math.pi * 2 / 48
        ex, ey = math.cos(a) * 13.5, math.sin(a) * 4.6
        x, y = 16 + ex * math.cos(-0.55) - ey * math.sin(-0.55), 16 + ex * math.sin(-0.55) + ey * math.cos(-0.55)
        if s.get(int(x), int(y)) is None:
            s.set(int(x), int(y), c[2])
    _disc(s, 25.5, 11, 2.0, lambda x, y: c[3])


def _emblem_thread(s, c):
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            r = math.hypot(dx, dy)
            if 7.4 < r < 11.8:
                s.set(x, y, rainbow(((math.atan2(dy, dx) / (math.pi * 2)) % 1.0), sat=0.9, light=0.62))
    s.fill_poly([(16, 3), (21.5, 8.5), (16, 14), (10.5, 8.5)], (255, 240, 200, 255))
    s.set(14, 8, (255, 60, 100, 255))
    s.set(17, 8, (255, 60, 100, 255))
    _disc(s, 16, 17.5, 3.4, lambda x, y: (255, 255, 255, 255))
    for (x, y) in ((6, 9), (25, 22), (8, 24), (24, 8)):
        s.set(x, y, (255, 255, 255, 255))


EMBLEMS = {
    "ender_dragon": (_emblem_dragon, "#b890ff"),
    "ignarok": (_emblem_flame, "#ff7a2a"),
    "voltaris": (_emblem_bolt, "#ffe94d"),
    "hemorath": (_emblem_drop, "#e02848"),
    "chronos": (_emblem_hourglass, "#3ec6b0"),
    "nihilus": (_emblem_void, "#b04bff"),
    "solareon": (_emblem_sun, "#ffe9a0"),
    "omnarch": (_emblem_omni, "#ff6ad5"),
    "boreus": (_emblem_snow, "#7fe8ff"),
    "morbane": (_emblem_spore, "#a6e22e"),
    "astraeus": (_emblem_star, "#7b7bff"),
    "eternus": (_emblem_thread, "#ffffff"),
}


def emblem(gate):
    fn, col = EMBLEMS[gate]
    c = ramp(col)
    s = Sprite(32, 32)
    _medallion(s, c)
    fn(s, c)
    return s.outline(strength=0.45)
