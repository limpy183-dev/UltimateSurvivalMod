"""Custom particle sprites: one signature particle per theme, four animation frames of 8x8 pixels each.

Their behaviour comes from vanilla particle classes (see client/ParticleSetup.java); only the art and the identity are new."""
import math
from PIL import Image

NAMES = ["ember_spark", "storm_arc", "blood_drop", "chrono_tick", "void_mote", "radiant_spark", "apex_glimmer",
         "frost_flake", "plague_spore", "cosmic_star", "eternal_prism"]
FRAMES = 4


def _img():
    return Image.new("RGBA", (8, 8), (0, 0, 0, 0)), None


def _put(im, x, y, c):
    if 0 <= x < 8 and 0 <= y < 8:
        im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def ember_spark(f):
    im, _ = _img()
    r = [2, 2, 1, 1][f]
    for y in range(8):
        for x in range(8):
            d = abs(x - 3.5) + abs(y - 3.5)
            if d <= r + 0.6:
                _put(im, x, y, (255, 138, 42))
            if d <= r - 0.4:
                _put(im, x, y, (255, 220, 90))
    _put(im, 3, 3, (255, 250, 200))
    for (x, y) in ((3, 6 - f), (4, 1 + f)):                                   # a short ash tail
        _put(im, x, y, (200, 60, 20))
    return im


def storm_arc(f):
    im, _ = _img()
    paths = [[(1, 0), (2, 1), (3, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7)],
             [(6, 0), (5, 1), (4, 2), (5, 3), (4, 4), (3, 5), (2, 6), (1, 7)],
             [(3, 0), (3, 1), (4, 2), (3, 3), (4, 4), (3, 5), (3, 6), (4, 7)],
             [(0, 3), (2, 3), (3, 4), (4, 3), (5, 4), (7, 4)]]
    for (x, y) in paths[f]:
        _put(im, x, y, (255, 233, 77))
        if (x + y) % 2 == 0:
            _put(im, x, y, (255, 255, 230))
    return im


def blood_drop(f):
    im, _ = _img()
    top = [1, 1, 2, 2][f]
    shape = [(3, top), (4, top), (3, top + 1), (4, top + 1), (2, top + 2), (3, top + 2), (4, top + 2), (5, top + 2),
             (2, top + 3), (3, top + 3), (4, top + 3), (5, top + 3), (3, top + 4), (4, top + 4)]
    for (x, y) in shape:
        _put(im, x, y, (208, 16, 42))
    for (x, y) in ((2, top + 3), (3, top + 4), (5, top + 3)):
        _put(im, x, y, (110, 6, 20))
    _put(im, 3, top + 2, (255, 130, 150))
    return im


def chrono_tick(f):
    im, _ = _img()
    for k in range(16):
        a = k * math.pi / 8
        _put(im, int(round(3.5 + 3.0 * math.sin(a))), int(round(3.5 - 3.0 * math.cos(a))), (62, 198, 176))
    a = f * math.pi / 2
    for t in (1, 2):
        _put(im, int(round(3.5 + t * math.sin(a))), int(round(3.5 - t * math.cos(a))), (217, 178, 74))
    _put(im, 3, 3, (250, 240, 200))
    _put(im, 4, 4, (250, 240, 200))
    return im


def void_mote(f):
    im, _ = _img()
    for y in (3, 4):
        for x in (3, 4):
            _put(im, x, y, (40, 10, 70))
    ring = [(3, 1), (5, 2), (6, 4), (4, 6), (2, 5), (1, 3), (2, 1), (6, 2), (5, 6), (1, 5)]
    for k in range(4):
        x, y = ring[(f * 2 + k * 2) % len(ring)]
        _put(im, x, y, (176, 75, 255) if k % 2 == 0 else (255, 106, 213))
    for (x, y) in ((2, 3), (5, 4), (3, 2), (4, 5)):
        _put(im, x, y, (110, 40, 170))
    return im


def radiant_spark(f):
    im, _ = _img()
    ln = [1, 2, 3, 2][f]
    for t in range(1, ln + 1):
        c = (255, 226, 120) if t < 3 else (255, 244, 200)
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            _put(im, 3 + dx * t + (1 if dx > 0 else 0), 3 + dy * t + (1 if dy > 0 else 0), c)
    for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4)):
        _put(im, x, y, (255, 255, 245))
    return im


def apex_glimmer(f):
    im, _ = _img()
    r = [2, 2, 3, 2][f]                                                       # frame 0 is the first thing seen up close: no bare white square
    col = (255, 106, 213) if f % 2 == 0 else (127, 232, 255)
    for y in range(8):
        for x in range(8):
            if abs(x - 3.5) + abs(y - 3.5) <= r + 0.5:
                _put(im, x, y, col)
    for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4)):
        _put(im, x, y, (255, 255, 255) if f else (255, 214, 244))
    return im


def frost_flake(f):
    im, _ = _img()
    for k in range(6):                                                         # six arms that turn a little each frame
        a = k * math.pi / 3 + f * 0.12
        for t in (1, 2, 3):
            _put(im, int(round(3.5 + t * math.cos(a))), int(round(3.5 + t * math.sin(a))), (200, 240, 255) if t < 3 else (127, 232, 255))
    _put(im, 3, 3, (255, 255, 255))
    _put(im, 4, 4, (255, 255, 255))
    return im


def plague_spore(f):
    im, _ = _img()
    r = [1.6, 2.0, 2.4, 2.0][f]
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d <= r:
                _put(im, x, y, (166, 226, 46) if d > r - 1.0 else (210, 255, 120))
    for (x, y) in ((1, 1 + f % 2), (6, 5 - f % 2), (5, 1), (2, 6)):
        _put(im, x, y, (90, 120, 20))
    _put(im, 3, 3, (255, 255, 220))
    return im


def cosmic_star(f):
    im, _ = _img()
    ln = [1, 2, 3, 2][f]
    for t in range(1, ln + 1):
        c = (168, 180, 255) if t < 3 else (230, 235, 255)
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            _put(im, 3 + dx * t + (1 if dx > 0 else 0), 3 + dy * t + (1 if dy > 0 else 0), c)
    for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4)):
        _put(im, x, y, (255, 255, 255))
    for (x, y) in ((1, 1), (6, 6), (1, 6), (6, 1)):
        _put(im, x, y, (123, 123, 255))
    return im


def eternal_prism(f):
    import colorsys
    im, _ = _img()
    r, g, b = (int(v * 255) for v in colorsys.hsv_to_rgb(f / 4.0, 0.55, 1.0))
    for y in range(8):
        for x in range(8):
            if abs(x - 3.5) + abs(y - 3.5) <= 3.1:
                _put(im, x, y, (r, g, b))
    for (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4)):
        _put(im, x, y, (255, 255, 255))
    return im


PAINTERS = {n: globals()[n] for n in NAMES}


def frames(name):
    return [PAINTERS[name](f) for f in range(FRAMES)]
