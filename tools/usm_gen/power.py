"""The global power / tempo pass.

Content modules keep their readable numbers; this module rescales every item ability, armor bonus, accessory attribute and bow once,
right after the content is imported and before anything is written - so tooltips, the Codex, recipes and the README all quote the final values.
Bigger hits, bigger blasts, faster recharge, faster draw. Tier 6 content is authored with these numbers already in mind and is skipped.

Run `python tools/usm_gen/power.py` for the self-check."""
from . import items as I
from .abilities import CATALOG

DMG = 1.6          # damage of every ability / aura / bolt
COOLDOWN = 0.65    # recharge time of right-click abilities
REACH = 1.2        # radius of blasts and auras
DASH = 1.25        # dash / blink distance
RANGE = 1.15       # beam / bolt reach
SPEED = 1.2        # projectile speed
DRAW = 0.7         # bow draw time
HEAL = 1.5

_SKIP_GATES = {"boreus", "morbane", "astraeus", "eternus"}


_INT_KEYS = {"cd", "strikes", "jumps", "count", "charge", "delay", "every", "ticks", "ticks2", "ticks3", "cap"}


def _scale(a, key, mul, lo=None, hi=None, integer=False):
    if key not in a:
        return
    v = a[key] * mul
    if lo is not None:
        v = max(lo, v)
    if hi is not None:
        v = min(hi, v)
    if integer or key in _INT_KEYS:
        a[key] = int(round(v))
    else:
        v = round(v) if key in ("dmg", "heal", "amount") and v >= 10 else round(v, 4)
        a[key] = int(v) if v == int(v) and key not in ("pct", "chance") else v


def _ability(a):
    """Rescale one ability ref (a dict from ab()) in place by its kind."""
    kind, _title, _tpl, default_cd = CATALOG[a["id"]]
    if kind == "active":
        _scale(a, "dmg", DMG)
        a["cd"] = max(10, int(round(a.get("cd", default_cd) * COOLDOWN))) if default_cd or "cd" in a else a.get("cd", 0)
        _scale(a, "radius", REACH)
        _scale(a, "dist", DASH)
        _scale(a, "range", RANGE)
        _scale(a, "width", 1.15)
        _scale(a, "strikes", 1.3, integer=True)
        _scale(a, "jumps", 1.5, integer=True)
        _scale(a, "count", 1.4, integer=True)
        _scale(a, "charge", 0.6, lo=8, integer=True)
        _scale(a, "pull", 1.2)
        _scale(a, "speed", SPEED)
        _scale(a, "amount", HEAL)
        _scale(a, "heal", HEAL)
        if a["id"] == "recall":
            _scale(a, "delay", 0.7, integer=True)
    elif kind == "hit":
        _scale(a, "dmg", DMG)
        _scale(a, "pct", 1.3 if a["id"] == "lifesteal" else 1.25, hi=0.6)
        _scale(a, "cap", DMG)
        _scale(a, "chance", 1.3, hi=0.6)
        _scale(a, "power", 1.3)
    elif kind == "mod":
        _scale(a, "mult", 1.25)
    elif kind == "kill":
        _scale(a, "amount", 1.5)
    elif kind == "wear":
        _scale(a, "dmg", DMG)
        _scale(a, "every", 0.8, lo=10)
        _scale(a, "radius", REACH)
        _scale(a, "heal", HEAL)
        _scale(a, "amount", HEAL)
        if a["id"] == "damage_reduction":
            _scale(a, "pct", 1.15, hi=0.6)
        elif a["id"] in ("bloodlust", "thorns"):
            _scale(a, "pct", 1.3, hi=0.8)
        elif a["id"] == "dodge":
            _scale(a, "chance", 1.25, hi=0.5)
        elif a["id"] == "quicken" and "cd_mult" in a:
            a["cd_mult"] = round(1 - (1 - a["cd_mult"]) * 1.25, 3)
        elif a["id"] == "phoenix":
            _scale(a, "cd", 0.6, integer=True)
    elif kind == "use":
        _scale(a, "amount", HEAL)
        for suf in ("", "2", "3"):
            if "effect" + suf in a:
                _scale(a, "ticks" + suf, 1.5, integer=True)
                if a.get("amp" + suf, 0) >= 1:
                    a["amp" + suf] += 1


def _attrs(attrs, seen):
    if attrs is None or id(attrs) in seen:
        return
    seen.add(id(attrs))
    for at in attrs:
        k = at["attr"]
        if k == "movement_speed":
            at["amount"] = round(at["amount"] * 1.5, 4)
        elif k == "attack_speed":
            at["amount"] = round(at["amount"] * 1.3, 4)
        elif k == "attack_damage" and at.get("op") == "mul_base":
            at["amount"] = round(at["amount"] * 1.5, 4)
        elif k == "max_health":
            at["amount"] = float(max(2, round(at["amount"] * 1.4 / 2) * 2))         # whole hearts only


def apply():
    seen = set()
    for it in I.ITEMS:
        if it.kind == "consumable":                                   # every drink and meal is quicker, Tier 6 included
            it.stats["seconds"] = max(0.5, round(it.stats.get("seconds", 1.2) * 0.65, 2))
        if it.gate in _SKIP_GATES:
            continue
        for a in it.abilities:
            if it.kind == "bow" and a["id"] == "bow_shot":
                _scale(a, "dmg", DMG)
                _scale(a, "speed", SPEED)
                _scale(a, "aoe", REACH)
            else:
                _ability(a)
        if it.kind == "bow":
            it.stats["draw"] = max(7, int(round(it.stats.get("draw", 18) * DRAW)))
            it.stats["arrow_mult"] = round(it.stats.get("arrow_mult", 1.5) * 1.5, 2)
        _attrs(it.extra.get("attrs"), seen)
    for st in I.SETS:
        if st["gate"] in _SKIP_GATES:
            continue
        for b in st["bonus"]:
            _ability(b)


def demo():
    a = {"id": "dash_strike", "dist": 10, "dmg": 50, "cd": 100}
    _ability(a)
    assert a["dmg"] == 80 and a["cd"] == 65 and a["dist"] == 12.5, a
    r = {"id": "rend", "pct": 0.03, "cap": 80}
    _ability(r)
    assert abs(r["pct"] - 0.0375) < 1e-9 and r["cap"] == 128, r
    q = {"id": "quicken", "cd_mult": 0.8}
    _ability(q)
    assert abs(q["cd_mult"] - 0.75) < 1e-9, q
    g = {"id": "give_effect", "effect": "strength", "ticks": 600, "amp": 2}
    _ability(g)
    assert g["ticks"] == 900 and g["amp"] == 3, g
    print("power.py ok")


if __name__ == "__main__":
    demo()
