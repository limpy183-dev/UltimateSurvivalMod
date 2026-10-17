"""Named palettes per material / boss theme (the per-tier visual language)."""
from .pal import Pal

PALS = {
    # T1 matte green-bronze           T2 azure steel            T3 ember
    "verdite":  Pal("#5fa86a", "#8a5a2b", "#c9803c"),
    "azurite":  Pal("#4aa3e0", "#b8c4d8", "#7fe8ff"),
    "cinder":   Pal("#d94f2b", "#3a2a2a", "#ffb03a"),
    # T4 storm / blood / time
    "storm":    Pal("#7da7d9", "#394a66", "#ffe94d"),
    "blood":    Pal("#a3122a", "#1a0a10", "#e8dcc8", glow="#ff3355"),
    "chrono":   Pal("#d9b24a", "#2a5a66", "#3ec6b0"),
    # T5 void / radiance / apex
    "void":     Pal("#6a2fb0", "#14081f", "#d64bff"),
    "radiant":  Pal("#fff3b0", "#c9a640", "#8fd9ff"),
    "apex":     Pal("#2b1b3d", "#111111", "#ff6ad5"),
    "aether":   Pal("#c8b8ff", "#2a2150", "#7ef0ff"),
    # T6 frost / plague / cosmos / every colour
    "frost":    Pal("#bfeaff", "#2f6fa8", "#7fe8ff"),
    "plague":   Pal("#7ea32a", "#2b3a14", "#c6ff3a"),
    "cosmic":   Pal("#3a3fb0", "#0a0c30", "#a8b4ff"),
    "eternal":  Pal("#f7ecd0", "#2a2236", "#ff9ad5"),
    # utility palettes
    "magnet":   Pal("#c43c3c", "#8a8a96", "#e8e8f0"),
    "drake":    Pal("#7a4fc8", "#2a1a44", "#b890ff"),
    "crimson":  Pal("#d02840", "#5a1020", "#ff7088"),
}
