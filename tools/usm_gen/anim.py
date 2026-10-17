"""Animated-texture frame builders (vanilla .png.mcmeta vertical strips)."""
import math
from .color import mix, lighten, rainbow
from .diag import to_ap


def _glow_set(pal):
    return {tuple(pal.glow), tuple(pal.hot), tuple(pal.a[3]), tuple(pal.a[4])}


def shimmer(sprite, pal, frames=12, width=1.5, strength=0.45):
    """A bright band sweeps along the item axis (enchanted-metal look)."""
    out = []
    for k in range(frames):
        f = sprite.copy()
        centre = -3 + (22 * k / frames)
        for y in range(f.h):
            for x in range(f.w):
                c = f.get(x, y)
                if c is None:
                    continue
                a, p = to_ap(x, y)
                d = abs(a - centre)
                if d < width:
                    f.set(x, y, lighten(c, strength * (1 - d / width)))
        out.append(f)
    return out


def pulse(sprite, pal, frames=10, amount=0.55):
    """Accent / glow pixels breathe between their colour and white-hot."""
    gs = _glow_set(pal)
    out = []
    for k in range(frames):
        f = sprite.copy()
        t = (math.sin(2 * math.pi * k / frames) + 1) / 2
        for y in range(f.h):
            for x in range(f.w):
                c = f.get(x, y)
                if c is not None and tuple(c) in gs:
                    f.set(x, y, mix(c, pal.hot, amount * t))
        out.append(f)
    return out


def flicker(sprite, pal, frames=8, seed=3):
    import random
    rnd = random.Random(seed)
    gs = _glow_set(pal)
    out = []
    for k in range(frames):
        f = sprite.copy()
        for y in range(f.h):
            for x in range(f.w):
                c = f.get(x, y)
                if c is not None and tuple(c) in gs and rnd.random() < 0.45:
                    f.set(x, y, mix(c, pal.hot, rnd.random() * 0.7))
        out.append(f)
    return out


def prism(sprite, pal, frames=16):
    """Apex / endgame: accent pixels cycle through the rainbow, with a shimmer band on top."""
    gs = _glow_set(pal)
    base = shimmer(sprite, pal, frames, width=1.8, strength=0.35)
    for k, f in enumerate(base):
        for y in range(f.h):
            for x in range(f.w):
                c = f.get(x, y)
                if c is not None and (tuple(c) in gs):
                    f.set(x, y, rainbow((k / frames + (x + y) * 0.03) % 1.0))
    return base


STYLES = {"shimmer": shimmer, "pulse": pulse, "flicker": flicker, "prism": prism}


def animate(sprite, pal, style):
    if not style:
        return [sprite]
    return STYLES[style](sprite, pal)
