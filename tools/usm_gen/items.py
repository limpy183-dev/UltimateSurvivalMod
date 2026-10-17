"""Content DSL. Content modules call these helpers at import time; gen_all.py turns the registries into
textures, models, recipes, lang and the runtime manifest (data/ultimatesurvival/usm/content.json)."""
from . import balance
from .gates import GATES

NS = "ultimatesurvival"
ITEMS = []
BLOCKS = []
SETS = []
WORLDGEN = []
_BY_ID = {}

RARITY_BY_TIER = {1: "common", 2: "uncommon", 3: "rare", 4: "epic", 5: "epic", 6: "epic"}
MELEE = ("sword", "greatsword", "dagger", "rapier", "scythe", "hammer", "spear")


class Item:
    def __init__(self, id, kind, gate, art, name=None, stats=None, abilities=None, recipe=None,
                 rarity=None, lore=None, stack=None, model="generated", extra=None, glint=False, hidden=False, obtain=None):
        self.id, self.kind, self.gate = id, kind, gate
        self.art = art
        self.name = name or id.replace("_", " ").title()
        self.stats = stats or {}
        self.abilities = abilities or []
        self.recipe = recipe                       # dict or list of dicts (see shaped()/shapeless()/smelt())
        self.recipe_ids = []                       # filled by gen_all
        self.tier = balance.GATE_STATS[gate]["tier"]
        self.rarity = rarity or RARITY_BY_TIER[self.tier]
        self.lore = lore or ""
        self.stack = stack
        self.model = model                         # "generated" | "handheld"
        self.extra = extra or {}
        self.glint = glint
        self.hidden = hidden
        self.obtain = obtain or ""

    def recipes(self):
        if not self.recipe:
            return []
        return self.recipe if isinstance(self.recipe, list) else [self.recipe]

    def manifest(self):
        m = dict(id=self.id, kind=self.kind, gate=self.gate, tier=self.tier, rarity=self.rarity,
                 stats=self.stats, abilities=self.abilities, glint=self.glint)
        if self.stack:
            m["stack"] = self.stack
        if self.recipe:
            m["recipe"] = self.recipes()
            m["recipe_ids"] = self.recipe_ids
        if self.hidden:
            m["hidden"] = True
        m.update(self.extra)
        return m


def add(id, kind, gate, art, **kw):
    if id in _BY_ID:
        raise ValueError("duplicate item " + id)
    it = Item(id, kind, gate, art, **kw)
    ITEMS.append(it)
    _BY_ID[id] = it
    return it


def get(id):
    return _BY_ID[id]


def ab(id, **params):
    """Ability reference with parameters, e.g. ab('lifesteal', pct=0.1)."""
    d = {"id": id}
    d.update(params)
    return d


# ---- recipes ---------------------------------------------------------------------------------
def shaped(pattern, key, count=1, category="equipment"):
    return dict(type="shaped", pattern=pattern, key=key, count=count, category=category)


def shapeless(ingredients, count=1, category="misc"):
    return dict(type="shapeless", ingredients=ingredients, count=count, category=category)


def smelt(ingredient, xp=0.7, time=200, blast=True):
    return dict(type="smelting", ingredient=ingredient, xp=xp, time=max(20, int(time * balance.SMELT)), blast=blast, count=1)


def smithing(template, base, addition):
    return dict(type="smithing", template=template, base=base, addition=addition, count=1)


# ---- weapons ---------------------------------------------------------------------------------
def weapon(id, gate, arch, art, name=None, tweak=1.0, abilities=None, recipe=None, lore=None,
           dur_mult=1.0, repair=None, **kw):
    a = balance.ARCHETYPE[arch]
    g = balance.GATE_STATS[gate]
    stats = dict(damage=balance.weapon_dmg(gate, arch, tweak), speed=a["spd"], reach=a["reach"],
                 durability=int(g["dur"] * dur_mult), enchant=g["ench"])
    extra = dict(arch=arch)
    if repair:
        extra["repair"] = repair
    return add(id, kind="staff" if arch == "staff" else "sword",
               gate=gate, art=art, name=name, stats=stats, abilities=abilities, recipe=recipe, lore=lore,
               model="handheld", extra=extra, **kw)


def bow(id, gate, art, name=None, shot=None, ammo=False, draw=18, arrow_mult=1.5, dur_mult=1.0, recipe=None,
        lore=None, repair=None, nograv=False, **kw):
    g = balance.GATE_STATS[gate]
    stats = dict(durability=int(g["dur"] * dur_mult), enchant=g["ench"], ammo=1 if ammo else 0, draw=draw, arrow_mult=arrow_mult,
                 nograv=1 if nograv else 0)
    abilities = [ab("bow_shot", **shot)] if shot else []
    extra = {"repair": repair} if repair else {}
    return add(id, kind="bow", gate=gate, art=art, name=name, stats=stats, abilities=abilities, recipe=recipe, lore=lore,
               model="handheld", extra=extra, **kw)


def tool(id, gate, kind, art, name=None, traits=None, recipe=None, lore=None, repair=None, tweak=1.0, abilities=None, **kw):
    g = balance.GATE_STATS[gate]
    stats = dict(damage=round(g["dmg"] * balance.TOOL_M[kind] * tweak, 1), speed=balance.TOOL_SPD[kind],
                 durability=g["dur"], mine=g["speed"], enchant=g["ench"])
    stats.update(traits or {})
    extra = {"repair": repair} if repair else {}
    return add(id, kind=kind, gate=gate, art=art, name=name, stats=stats, abilities=abilities, recipe=recipe, lore=lore,
               model="handheld", extra=extra, **kw)


def accessory(id, gate, art, name=None, abilities=None, attrs=None, recipe=None, lore=None, **kw):
    extra = {"attrs": attrs} if attrs else {}
    return add(id, kind="accessory", gate=gate, art=art, name=name, abilities=abilities, recipe=recipe, lore=lore,
               extra=extra, stack=1, **kw)


def consumable(id, gate, art, name=None, abilities=None, recipe=None, lore=None, anim="drink", seconds=1.2,
               stack=16, nutrition=0, saturation=0.0, **kw):
    stats = dict(seconds=seconds, stack=stack)
    if anim == "eat":
        stats.update(nutrition=nutrition, saturation=saturation)
    return add(id, kind="consumable", gate=gate, art=art, name=name, abilities=abilities, recipe=recipe, lore=lore,
               stats=stats, extra=dict(anim=anim), **kw)


def gadget(id, gate, art, name=None, abilities=None, recipe=None, lore=None, **kw):
    return add(id, kind="gadget", gate=gate, art=art, name=name, abilities=abilities, recipe=recipe, lore=lore, stack=1, **kw)


def material(id, gate, art, name=None, recipe=None, lore=None, stack=None, **kw):
    return add(id, "material", gate, art, name=name, recipe=recipe, lore=lore, stack=stack, **kw)


def bolt(style, pal):
    """Hidden item whose sprite the projectile entity renders (see entity/Bolt.java)."""
    return add("bolt_" + style, "material", "start", dict(paint="orb", pal=pal, anim="pulse"), name="Bolt (%s)" % style,
               hidden=True, stack=1)


# ---- armor -----------------------------------------------------------------------------------
PIECES = (("head", "helmet", ["XXX", "X X"], ["XDX", "X X"]),
          ("chest", "chestplate", ["X X", "XXX", "XXX"], ["X X", "XDX", "XXX"]),
          ("legs", "leggings", ["XXX", "X X", "X X"], ["XXX", "XDX", "X X"]),
          ("feet", "boots", ["X X", "X X"], ["X X", "D D"]))


def armor_set(set_id, name, gate, pal, ingredient, bonuses, lore="", style=None, special=None, ambient="", color=0xFFFFFF,
              attrs=None, kb=0.0, layer=None, set_desc=""):
    """Four armor pieces + the set definition. `special` (an item id) replaces some ingredients in the recipes.
    `attrs` are extra attribute modifiers added to *every* piece (list of dicts)."""
    g = balance.GATE_STATS[gate]
    style = style or {}
    defs = balance.split_armor(g["armor"])
    ids = []
    for (slot, piece, pat, pat_special), d in zip(PIECES, defs):
        iid = "%s_%s" % (set_id, piece)
        key = {"X": ingredient}
        pattern = pat
        if special:
            key["D"] = special
            pattern = pat_special
        add(iid, "armor", gate, dict(paint="armor_icon", pal=pal, piece=piece, **style),
            name="%s %s" % (name, piece.title()), lore=lore,
            stats=dict(armor=d, toughness=g["tough"], kb=kb, durability_mult=g["adur"], enchant=g["ench"]),
            recipe=shaped(pattern, key), extra=dict(slot=slot, set=set_id, repair=ingredient, **({"attrs": attrs} if attrs else {})))
        ids.append(iid)
    SETS.append(dict(id=set_id, name=name + " Armor", gate=gate, pal=pal, pieces=ids, bonus=bonuses, ambient=ambient,
                     color=color, layer=layer or {}, desc=set_desc))
    return ids


# ---- blocks & world generation ---------------------------------------------------------------------
class Block:
    def __init__(self, id, kind, art, hardness, resistance, tool, name, lore, drop, xp, deepslate, light, metal, gate="start", recipe=None):
        self.gate = gate
        self.recipe = recipe
        self.recipe_ids = []
        self.id, self.kind, self.art = id, kind, art
        self.hardness, self.resistance, self.tool = hardness, resistance, tool
        self.name = name or id.replace("_", " ").title()
        self.lore, self.drop, self.xp = lore, drop, xp
        self.deepslate, self.light, self.metal = deepslate, light, metal

    def recipes(self):
        if not self.recipe:
            return []
        return self.recipe if isinstance(self.recipe, list) else [self.recipe]

    def manifest(self):
        m = dict(id=self.id, kind=self.kind, hardness=self.hardness, resistance=self.resistance, tool=self.tool, gate=self.gate)
        if self.recipe:
            m["recipe"] = self.recipes()
        if self.kind == "ore":
            m.update(xp_min=self.xp[0], xp_max=self.xp[1])
        if self.deepslate:
            m["deepslate"] = True
        if self.light:
            m["light"] = self.light
        if self.metal:
            m["metal"] = True
        return m


def block(id, kind, art, hardness, resistance, tool="stone", name=None, lore="", drop=None, xp=(1, 3),
          deepslate=False, light=0, metal=False, gate="start", recipe=None):
    b = Block(id, kind, art, hardness, resistance, tool, name, lore, drop, xp, deepslate, light, metal, gate, recipe)
    BLOCKS.append(b)
    return b


def ore_gen(name, stone_block, deep_block, size, count, min_y, max_y, dimension="overworld", peak=True, discard=0.0):
    """Vanilla-style ore vein feature (configured + placed) added to every biome of a dimension."""
    WORLDGEN.append(dict(name=name, stone=stone_block, deep=deep_block, size=size, count=count, min_y=min_y, max_y=max_y,
                         dimension=dimension, peak=peak, discard=discard))
