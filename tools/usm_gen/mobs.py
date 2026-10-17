"""Mob / boss DSL. A mob is a stat block + a Rig (model, texture, animation rules) + an attack list.
Bosses add phases (HP thresholds), a ritual (how to summon), a sigil item and a treasure bag.

Attack parameters are read by the Java attack library (mob/attack/Attacks.java); damage values are
MULTIPLIERS of the mob's base damage so the balance table stays the single source of numbers."""
from . import balance
from . import items as I
from .rig import Rig

MOBS = []
_BY = {}

# bar colours understood by vanilla's BossEvent
BAR_COLORS = ("pink", "blue", "red", "green", "yellow", "purple", "white")


def atk(type, w=1, **kw):
    """One attack in a phase: type + selection weight + parameters (see Attacks.java for each type's keys)."""
    d = {"type": type, "w": w}
    d.update(kw)
    return d


def phase(name, at, attacks, cd=(40, 70), speed=1.0, dmg=1.0, color=None, enter=None, taken=1.0, nodes=None):
    """A boss phase: it starts once health drops to `at` (fraction); cd = ticks between attacks (min, max).
    taken = multiplier on damage the boss takes in this phase; nodes = dict(mob=id, count=n, radius=r): shield pylons that
    make the boss immune until every one of them is destroyed."""
    return dict(name=name, at=at, attacks=attacks, cd=list(cd), speed=speed, dmg=dmg, color=color, enter=enter or [], taken=taken,
                nodes=nodes)


class Mob:
    def __init__(self, id, name, rig, kind, gate, stats, size, extra):
        self.id, self.name, self.rig, self.kind, self.gate = id, name, rig, kind, gate
        self.stats, self.size, self.extra = stats, size, extra
        self.tier = balance.GATE_STATS[gate]["tier"]

    def manifest(self):
        m = dict(id=self.id, kind=self.kind, gate=self.gate, tier=self.tier, size=list(self.size))
        m.update(self.stats)
        m.update(self.extra)
        return m


def _add(m):
    if m.id in _BY:
        raise ValueError("duplicate mob " + m.id)
    MOBS.append(m)
    _BY[m.id] = m
    return m


def get(id):
    return _BY[id]


def mob(id, name, rig, gate, hp, dmg, size, armor=0, speed=0.28, attacks=None, drops=None, egg=(0x555555, 0xaaaaaa),
        xp=5, fly=False, fire=False, kb=0.0, follow=32, spawn=None, eye=None, scale=1.0, reach=None, role="mob",
        lore="", sounds=None, melee=True, minion_of=None, anim_len=None, hover=0.0, keep=None, style="ember"):
    """Regular mob. `spawn` = natural spawn rule dict (dimension / weight / min_tier / group) or None."""
    extra = dict(name=name, attacks=attacks or [], drops=drops or [], egg=list(egg), xp=xp, fly=fly, fire=fire, kb=kb,
                 follow=follow, spawn=spawn, eye=eye if eye is not None else round(size[1] * 0.85, 2), scale=scale,
                 reach=reach, role=role, lore=lore, sounds=sounds or {}, melee=melee, hover=hover, keep=keep, style=style)
    if minion_of:
        extra["minion_of"] = minion_of
    if anim_len:
        extra["anim_len"] = anim_len
    return _add(Mob(id, name, rig, "mob", gate, dict(hp=hp, dmg=dmg, armor=armor, speed=speed), size, extra))


def boss(id, name, rig, phases, size, ritual, bag, speed=0.3, title="", fire=False, fly=False, kb=1.0, eye=None, bar=None,
         sigil=None, minion=None, xp=600, scale=1.0, spawn_ticks=70, death_ticks=90, enrage=480, follow=64, drops=None,
         reach=None, hover=0.0, sounds=None, music=None, style="ember", egg=None, keep=None):
    """Boss: base stats come from balance.BOSS_STATS[id]; `ritual` is the summoning rule, `bag` the treasure-bag rows."""
    b = balance.BOSS_STATS[id]
    extra = dict(name=name, title=title, phases=phases, ritual=ritual, bag=bag, fire=fire, fly=fly, kb=kb, xp=xp, follow=follow,
                 eye=eye if eye is not None else round(size[1] * 0.9, 2), bar=bar or dict(color="red", overlay="notched_10"),
                 sigil=sigil, minion=minion, scale=scale, spawn_ticks=max(30, int(spawn_ticks * balance.TEMPO)),
                 death_ticks=max(40, int(death_ticks * balance.TEMPO)), enrage=enrage,
                 drops=drops or [], reach=reach, hover=hover, sounds=sounds or {}, music=music or {}, egg=list(egg) if egg else None,
                 spawn=None, role="boss", melee=True, style=style, keep=keep)
    return _add(Mob(id, name, rig, "boss", id, dict(hp=b["hp"], dmg=b["dmg"], armor=b["armor"], speed=speed), size, extra))


def drop(item, lo=1, hi=None, chance=1.0):
    return dict(item=item, min=lo, max=hi if hi is not None else lo, chance=chance)
