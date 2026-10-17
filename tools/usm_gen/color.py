"""Colour helpers: hex parsing and hue-shifted shading ramps (dark -> light)."""
import colorsys


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def as_rgba(c):
    return hexc(c) if isinstance(c, str) else (tuple(c) + (255,))[:4]


def to_hex(c):
    return "#%02x%02x%02x" % tuple(int(max(0, min(255, v))) for v in c[:3])


def _hls(c):
    r, g, b = (v / 255.0 for v in c[:3])
    return colorsys.rgb_to_hls(r, g, b)


def _rgb(h, l, s, a=255):
    r, g, b = colorsys.hls_to_rgb(h % 1.0, max(0.0, min(1.0, l)), max(0.0, min(1.0, s)))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def _toward(h, target, amt):
    """Move hue h toward `target` along the shortest arc by fraction amt."""
    d = ((target - h + 0.5) % 1.0) - 0.5
    return h + d * amt


# lightness offsets for a 5-step ramp, darkest first
_OFFS = (-0.30, -0.16, 0.0, 0.14, 0.27)


def ramp(base, n=5):
    """n shades of `base`, darkest first; entry n//2 is exactly `base`.
    Shadows drift toward blue/violet and lights toward yellow (hue shifting) so palettes
    read as hand-shaded pixel art rather than flat brightness changes."""
    h, l, s = _hls(as_rgba(base))
    out = []
    for i in range(n):
        t = i / (n - 1)
        off = _OFFS[round(t * 4)] if n == 5 else (t - 0.5) * 0.6
        li = min(0.93, max(0.05, l + off))
        if t < 0.5:
            hi = _toward(h, 0.68, (0.5 - t) * 0.22)
        else:
            hi = _toward(h, 0.12, (t - 0.5) * 0.18)
        si = min(1.0, s * (1.0 + (0.5 - t) * 0.25) if t < 0.5 else s * (1.0 - (t - 0.5) * 0.45))
        out.append(_rgb(hi, li, si))
    out[n // 2] = as_rgba(base)
    return out


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def lighten(c, amt):
    return mix(c, (255, 255, 255, 255), amt)


def darken(c, amt):
    return mix(c, (0, 0, 0, 255), amt)


def rainbow(phase, sat=0.8, light=0.62):
    """Colour on the hue wheel for animated 'apex' textures (phase 0..1)."""
    return _rgb(phase, light, sat)
