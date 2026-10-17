"""Single balance table. Every numeric stat in the content tables is derived from here so the
whole mod stays coherent (README is generated from this too).

Columns are indexed by *gate* (progression stage), not just tier, because tiers 4, 5 and 6 each
contain several boss stages.

Power pass (v2): weapon damage is roughly 1.4-1.9x what it was, everything swings/draws/mines/recharges faster
(see ARCHETYPE, TOOL_SPD, TEMPO and power.py), and Tier 6 (Transcendence) sits far above the old endgame."""

# gate id -> stats ------------------------------------------------------------------------
#        tier  sword  armor(total of 4)  tough/piece  dur(tool)  dur(armor mult)  mine speed  enchant
GATE_STATS = {
    "start":        dict(tier=1, dmg=10,  armor=20,  tough=1.0,  dur=1000,   adur=25,   speed=10,  ench=14),
    "ender_dragon": dict(tier=2, dmg=16,  armor=30,  tough=2.0,  dur=3200,   adur=55,   speed=14,  ench=18),
    "ignarok":      dict(tier=3, dmg=24,  armor=40,  tough=3.0,  dur=5000,   adur=75,   speed=17,  ench=20),
    "voltaris":     dict(tier=4, dmg=33,  armor=50,  tough=4.0,  dur=7500,   adur=100,  speed=20,  ench=22),
    "hemorath":     dict(tier=4, dmg=44,  armor=60,  tough=5.0,  dur=9500,   adur=125,  speed=23,  ench=24),
    "chronos":      dict(tier=4, dmg=58,  armor=70,  tough=6.0,  dur=12000,  adur=155,  speed=26,  ench=26),
    "nihilus":      dict(tier=5, dmg=80,  armor=85,  tough=8.0,  dur=18000,  adur=200,  speed=32,  ench=28),
    "solareon":     dict(tier=5, dmg=112, armor=100, tough=10.0, dur=27000,  adur=270,  speed=40,  ench=30),
    "omnarch":      dict(tier=5, dmg=170, armor=130, tough=14.0, dur=60000,  adur=480,  speed=56,  ench=30),
    "boreus":       dict(tier=6, dmg=240, armor=150, tough=16.0, dur=80000,  adur=560,  speed=64,  ench=30),
    "morbane":      dict(tier=6, dmg=320, armor=170, tough=18.0, dur=100000, adur=640,  speed=72,  ench=30),
    "astraeus":     dict(tier=6, dmg=430, armor=200, tough=22.0, dur=130000, adur=760,  speed=84,  ench=30),
    "eternus":      dict(tier=6, dmg=600, armor=240, tough=28.0, dur=200000, adur=1000, speed=100, ench=30),
}

# weapon archetypes: damage multiplier, attack speed (hits/s), reach bonus (blocks)
ARCHETYPE = {
    "sword":      dict(m=1.00, spd=1.6, reach=0.0),
    "greatsword": dict(m=1.45, spd=1.0, reach=0.5),
    "dagger":     dict(m=0.65, spd=2.8, reach=-0.5),
    "rapier":     dict(m=0.90, spd=1.9, reach=0.7),
    "scythe":     dict(m=1.20, spd=1.2, reach=0.6),
    "hammer":     dict(m=1.60, spd=0.8, reach=0.2),
    "spear":      dict(m=1.10, spd=1.3, reach=1.0),
    "axe":        dict(m=1.25, spd=1.0, reach=0.0),
    "staff":      dict(m=0.40, spd=1.4, reach=0.0),
    "tool":       dict(m=0.45, spd=1.2, reach=0.0),
}
SWING_SPEED = 1.35                    # every weapon swings this much faster than the old table (capped below vanilla's "no cooldown" 4.0)
for _a in ARCHETYPE.values():
    _a["spd"] = round(min(_a["spd"] * SWING_SPEED, 3.8), 2)

# boss base stats (before config multipliers)
BOSS_STATS = {
    "ignarok":  dict(hp=700,   armor=12, dmg=14,  scale=2.2),
    "voltaris": dict(hp=1100,  armor=14, dmg=18,  scale=2.0),
    "hemorath": dict(hp=1700,  armor=16, dmg=24,  scale=2.2),
    "chronos":  dict(hp=2400,  armor=18, dmg=30,  scale=2.1),
    "nihilus":  dict(hp=3400,  armor=20, dmg=40,  scale=2.6),
    "solareon": dict(hp=4800,  armor=22, dmg=52,  scale=2.4),
    "omnarch":  dict(hp=7500,  armor=26, dmg=70,  scale=3.2),
    "boreus":   dict(hp=11000, armor=28, dmg=95,  scale=2.6),
    "morbane":  dict(hp=15500, armor=30, dmg=115, scale=2.8),
    "astraeus": dict(hp=21000, armor=32, dmg=140, scale=3.0),
    "eternus":  dict(hp=34000, armor=36, dmg=185, scale=3.6),
}

# digging tools: damage multiplier of the sword value, attack speed
TOOL_M = {"pickaxe": 0.6, "shovel": 0.65, "axe": 1.35, "hoe": 0.3}
TOOL_SPD = {"pickaxe": 1.6, "shovel": 1.4, "axe": 1.3, "hoe": 3.8}

# fights start and end quicker: a boss's emergence and death sequences run at this fraction of their old length
TEMPO = 0.65
# furnaces and blast furnaces cook this much faster
SMELT = 0.5


def split_armor(total):
    """Distribute a set's total armor points over helmet/chestplate/leggings/boots like vanilla (2/6/5/2)."""
    base = [2, 6, 5, 2]
    raw = [total * b / sum(base) for b in base]
    vals = [int(x) for x in raw]
    order = sorted(range(4), key=lambda i: raw[i] - vals[i], reverse=True)
    for i in order[: total - sum(vals)]:
        vals[i] += 1
    return vals


TIER_NAMES = {1: "Early", 2: "Mid", 3: "High", 4: "Late", 5: "Endgame", 6: "Transcendent"}


def weapon_dmg(gate, archetype, tweak=1.0):
    """Total hit damage shown on the tooltip (includes the base 1.0 of the player)."""
    return round(GATE_STATS[gate]["dmg"] * ARCHETYPE[archetype]["m"] * tweak, 1)
