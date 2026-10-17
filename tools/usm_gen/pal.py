"""Palettes: a few base colours -> hue-shifted ramps. One Pal per material/tier."""
from .color import ramp, as_rgba, lighten


class Pal:
    def __init__(self, primary, secondary, accent, glow=None, dark=None):
        self.p = ramp(primary)        # main material (blade, ingot body ...)
        self.s = ramp(secondary)      # handle / leather / trim
        self.a = ramp(accent)         # gems, runes, inlays
        self.glow = as_rgba(glow or accent)
        self.hot = lighten(self.glow, 0.55)
        self.dark = as_rgba(dark) if dark else self.p[0]
