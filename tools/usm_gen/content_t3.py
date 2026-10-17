"""Tier 3 - gate: ignarok. Cinderite: black iron veined with ember, forged in the Nether with Ignarok's core.
Contains the boss itself (rig, phases, ritual), its minion, the sigil/altar/bag, and the tier's items."""
from .items import *
from . import mobs as MB
from .mobs import atk, phase, drop
from .rigs_t3 import ignarok, ember_imp

C = "cinder"
G = "ignarok"
INGOT = "cinderite_ingot"
CORE = "ember_core"
STICK = "minecraft:stick"

# ---- blocks & world generation --------------------------------------------------------------------------------
block("cinderite_ore", "ore", dict(paint="ore_block", pal=C, nether=True, seed=31), 5.0, 6.0, tool="diamond", drop="raw_cinderite",
      xp=(4, 9), light=4, lore="Black iron with an ember trapped in every seam.", gate=G)
block("cinderite_block", "block", dict(paint="metal_block", pal=C), 7.0, 9.0, tool="diamond", metal=True, lore="Warm to the touch, always.",
      gate=G, recipe=shaped(["XXX", "XXX", "XXX"], {"X": INGOT}, category="building"))
ore_gen("ore_cinderite", "cinderite_ore", "cinderite_ore", size=5, count=7, min_y=10, max_y=118, dimension="nether", discard=0.2)

# the altar every sigil is used on (available from the start; the sigils are what is gated)
block("ritual_altar", "altar", dict(pal=C), 5.0, 1200.0, tool="diamond", light=7,
      lore="Obsidian and gold, cut to hold a name. Use a sigil on it.", gate="start",
      recipe=shaped(["OGO", "ODO", "OOO"], {"O": "minecraft:obsidian", "G": "minecraft:gold_ingot", "D": "minecraft:diamond"}))

# ---- materials ---------------------------------------------------------------------------------------------------------
material("raw_cinderite", G, dict(paint="raw_ore", pal=C, seed=9), name="Raw Cinderite", lore="Smelt it hot. It does not mind.")
material(INGOT, G, dict(paint="ingot", pal=C), name="Cinderite Ingot", lore="Black iron that never quite cools.",
         recipe=[smelt(["raw_cinderite", "cinderite_ore"], xp=2.4, time=360), shapeless(["cinderite_block"], count=9)])
material(CORE, G, dict(paint="core", pal=C, seed=4), name="Ember Core", rarity="epic", glint=True,
         lore="The last coal of a furnace-heart. Ignarok sheds a handful when it falls.")

# ---- summoning -----------------------------------------------------------------------------------------------------------------
add("cinder_sigil", "sigil", "ender_dragon", dict(paint="sigil", pal=C), name="Cinder Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="ignarok"), obtain="Crafted from blaze rods, drakescale and a ghast tear.",
    lore="Use it on a Ritual Altar in the Nether to wake Ignarok.",
    recipe=shaped(["BDB", "DGD", "BDB"], {"B": "minecraft:blaze_rod", "D": "drakescale", "G": "minecraft:ghast_tear"}, category="misc"))
add("ignarok_bag", "bag", G, dict(paint="bag", pal=C), name="Ignarok's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="ignarok"), obtain="Dropped by Ignarok.", lore="Right-click to open. It is still warm.")

# ---- Ignarok's unique relic ---------------------------------------------------------------------------------------------------------
accessory("heart_of_ignarok", G, dict(paint="heart", pal=C), name="Heart of Ignarok",
          attrs=[dict(attr="max_health", amount=6.0), dict(attr="attack_damage", amount=0.10, op="mul_base")],
          abilities=[ab("fire_res")], rarity="epic", glint=True, obtain="Dropped by Ignarok.",
          lore="It still beats, slowly, like a forge settling.")

# ---- the minion ------------------------------------------------------------------------------------------------------------------------
MB.mob("ember_imp", "Ember Imp", ember_imp(), G, hp=22, dmg=7, size=(0.7, 1.3), speed=0.28, fly=True, fire=True, hover=1.4,
       egg=(0xc2401f, 0xffb03a), xp=8, minion_of="ignarok", follow=40,
       attacks=[atk("fan", 1, count=1, spread=0, dmg=0.9, speed=1.15, windup=16, recover=30, style="ember", range=[4, 22], size=0.9, aoe=1.2)],
       drops=[drop("minecraft:blaze_powder", 0, 2, 0.5)],
       sounds=dict(hurt="entity.blaze.hurt", death="entity.blaze.death", ambient="entity.blaze.ambient", pitch=1.5),
       lore="A scrap of Ignarok's heat given a grudge and wings.")

# ---- the boss ------------------------------------------------------------------------------------------------------------------------------
_slam = lambda w, **k: atk("slam", w, windup=30, recover=18, range=[0, 9.5], **k)
_fan = lambda w, **k: atk("fan", w, spread=44, speed=1.25, windup=22, recover=14, style="ember", range=[5, 40], **k)
_pools = lambda w, **k: atk("pools", w, windup=26, recover=24, range=[3, 38], tick=0.3, life=200, **k)
_pillars = lambda w, **k: atk("pillars", w, windup=36, recover=20, range=[2, 32], **k)
_charge = lambda w, **k: atk("charge", w, windup=36, range=[8, 40], dist=22, stun=50, **k)

MB.boss(
    "ignarok", "Ignarok, the Cinder Warden", ignarok(), size=(1.9, 4.1), speed=0.27, title="Sovereign of Cinders", fire=True, style="ember",
    bar=dict(color="red", overlay="notched_10"), minion="ember_imp", sigil="cinder_sigil", egg=(0x3b2f33, 0xff7a1f), xp=700,
    ritual=dict(dim="minecraft:the_nether", time="any", sky=False, xp=0, hearts=0, items=[]),
    sounds=dict(hurt="entity.iron_golem.hurt", death="entity.iron_golem.death", ambient="entity.blaze.ambient", pitch=0.55),
    bag=[drop(CORE, 8, 12), drop(INGOT, 6, 10), drop("heart_of_ignarok", 1, 1), drop("minecraft:netherite_scrap", 2, 4),
         drop("minecraft:experience_bottle", 12, 20), drop("minecraft:enchanted_golden_apple", 1, 1, 0.3)],
    phases=[
        phase("Smoldering", 1.0, cd=(46, 78), attacks=[
            _slam(3, radius=7.5, dmg=1.5),
            _fan(3, count=5, dmg=0.6),
            _pools(2, count=3, radius=3.2, dmg=0.5)]),
        phase("Blazing", 0.66, cd=(36, 62), speed=1.12, dmg=1.1, color="yellow",
              enter=[atk("summon", mob="ember_imp", count=2, cap=4, windup=30, recover=16)],
              attacks=[
                  _slam(3, radius=8, dmg=1.6),
                  _fan(3, count=7, dmg=0.6, volleys=2, interval=10),
                  _pools(2, count=4, radius=3.2, dmg=0.5),
                  _pillars(3, count=6, radius=2.1, dmg=1.2),
                  atk("summon", 2, mob="ember_imp", count=2, cap=4, windup=36, recover=24, range=[0, 40]),
                  _charge(2, dmg=1.7)]),
        phase("Inferno", 0.33, cd=(26, 48), speed=1.25, dmg=1.25, color="purple",
              attacks=[
                  _slam(2, radius=8.5, dmg=1.7),
                  atk("ring", 3, windup=30, recover=22, speed=0.6, max_radius=24, dmg=1.3, range=[0, 26]),
                  atk("meteors", 3, windup=30, recover=20, count=9, delay=26, interval=7, radius=3.0, dmg=1.2, area=8, range=[0, 40]),
                  atk("spiral", 2, windup=20, recover=20, arms=4, dmg=0.5, duration=60, interval=4, range=[0, 40]),
                  atk("overheat", 2, windup=20, recover=20, duration=120, vuln=1.4, speed=1.35, pulse=0.5, range=[0, 40]),
                  _charge(2, dmg=1.9),
                  _pillars(2, count=8, radius=2.1, dmg=1.3)]),
    ])


# =====================================================================================================================
# Tier 3 gear. Every recipe needs Cinderite (Nether ore) and Ember Cores (Ignarok's drop), so the gate is a real wall.
# =====================================================================================================================
# ---- tools: 3x3x2 mining, vein mining and auto-smelting ------------------------------------------------------------
tool("cinderite_pickaxe", G, "pickaxe", dict(paint="pickaxe", pal=C, vein=True, spikes=True, bend=3.2, anim="flicker"),
     name="Cinderite Pickaxe", traits=dict(area=1, depth=2, vein=32, smelt=1, magnet=1), repair=INGOT,
     lore="Mines a 3x3x2 column, follows ore veins, and smelts what it breaks. Sneak to dig one block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": CORE, "S": STICK}))
tool("cinderite_axe", G, "axe", dict(paint="axe", pal=C, size=1.3, vein=True, anim="flicker"), name="Cinderite Axe",
     traits=dict(fell=1, fell_leaves=1, smelt=1, magnet=1), repair=INGOT,
     lore="Fells the whole tree and hands you charcoal instead of logs.",
     recipe=shaped(["XX", "XD", " S"], {"X": INGOT, "D": CORE, "S": STICK}))
tool("cinderite_shovel", G, "shovel", dict(paint="shovel", pal=C, point=True, vein=True, anim="flicker"), name="Cinderite Shovel",
     traits=dict(area=1, depth=2, smelt=1, magnet=1), repair=INGOT, lore="Digs 3x3x2 and bakes sand into glass as it goes.",
     recipe=shaped(["X", "D", "S"], {"X": INGOT, "D": CORE, "S": STICK}))
tool("cinderite_hoe", G, "hoe", dict(paint="hoe", pal=C, reach=5.4, vein=True, anim="flicker"), name="Cinderite Hoe",
     traits=dict(hoe_area=3, replant=1, magnet=1), repair=INGOT, lore="Tills, harvests and replants a 7x7 field in one swing.",
     recipe=shaped(["XX", " D", " S"], {"X": INGOT, "D": CORE, "S": STICK}))

# ---- weapons ---------------------------------------------------------------------------------------------------------------
weapon("cinder_brand", G, "sword",
       dict(paint="sword", pal=C, long=12.5, wide=1.35, guard="crown", pommel="gem", edge="flame", vein=True, anim="flicker"),
       name="Cinder Brand", lore="Still warm from the warden's furnace heart.", repair=INGOT,
       abilities=[ab("fire_nova", radius=6, dmg=18, ignite=6, cd=140), ab("ignite", ignite=5, dmg=4), ab("burning_bonus", mult=1.35)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": CORE, "S": STICK}))
weapon("magma_cleaver", G, "greatsword",
       dict(paint="sword", pal=C, long=15, wide=2.0, guard="wing", pommel="gem", offset=-1.2, edge="flame", vein=True, anim="flicker"),
       name="Magma Cleaver", lore="Too hot to hold for long. Too angry to put down.", repair=INGOT,
       abilities=[ab("ground_slam", radius=7, dmg=26, cd=150, kb=1.4, style="ember"), ab("ignite", ignite=6, dmg=5), ab("knockback_burst", power=1.5)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": CORE, "S": STICK}))
bow("inferno_bow", G, dict(paint="bow", pal=C, glow=True), name="Inferno Bow",
    shot=dict(dmg=24, speed=3.2, pierce=1, style="ember", trail="flame", count=1, life=60, aoe=2.8), draw=15, nograv=True, repair=INGOT,
    lore="Arrows are for people who plan to stay cool. Its bolts burst and burn.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": CORE, "S": "minecraft:string"}))
weapon("pyre_staff", G, "staff",
       dict(paint="staff", pal=C, head="orb", runes=True, crown=True, anim="flicker"),
       name="Pyre Staff", lore="Hurls a sun-coloured ball of flame. Sneak for a nova.", repair=INGOT,
       abilities=[ab("projectile_wave", count=1, spread=0, dmg=28, speed=1.5, style="ember", cd=30, trail="flame", aoe=3.6, size=1.6),
                  ab("fire_nova", radius=7, dmg=20, ignite=7, cd=160)],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": CORE, "S": STICK}))
weapon("ember_dagger", G, "dagger",
       dict(paint="sword", pal=C, long=8.5, wide=0.85, guard="narrow", pommel="ring", edge="flame", anim="flicker"),
       name="Ember Dagger", lore="Quick, quiet, and the last thing that was warm.", repair=INGOT,
       abilities=[ab("backstab", mult=2.0), ab("ignite", ignite=4, dmg=3), ab("dash_strike", dist=7, dmg=18, cd=90, style="ember")],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": CORE, "S": STICK}))

# ---- armor: Cinderite ---------------------------------------------------------------------------------------------------------
armor_set("cinderite", "Cinderite", G, C, INGOT,
          [dict(need=4, **ab("fire_res")), dict(need=4, **ab("thorns", pct=0.3, ignite=5)),
           dict(need=4, **ab("flame_aura", radius=4.5, dmg=4))],
          lore="Black plate that glows brighter every time it is struck.", special=CORE, ambient="ember", color=0xFF7A1F,
          style=dict(spikes=True, crest=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Immune to fire, reflect melee hits and set attackers ablaze, and scorch everything that stands close.")

# ---- accessories --------------------------------------------------------------------------------------------------------------------
accessory("ember_ring", G, dict(paint="ring", pal=C), name="Ember Ring",
          attrs=[dict(attr="attack_damage", amount=0.12, op="mul_base"), dict(attr="attack_speed", amount=0.08, op="mul_base")],
          abilities=[ab("fire_res")], lore="A band of black iron with a coal set in it.",
          recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": CORE}))
accessory("magma_charm", G, dict(paint="charm_shield", pal=C), name="Magma Charm",
          attrs=[dict(attr="armor", amount=4.0), dict(attr="armor_toughness", amount=2.0)],
          abilities=[ab("flame_aura", radius=3.5, dmg=3)], lore="It sweats lava in its sleep.",
          recipe=shaped(["XSX", "XDX", " X "], {"X": INGOT, "D": CORE, "S": "minecraft:string"}))
accessory("phoenix_ash", G, dict(paint="feather", pal=C), name="Phoenix Ash",
          abilities=[ab("phoenix", pct=0.5, cd=6000)], rarity="epic", glint=True,
          lore="Powder from something that has done this before.",
          recipe=shaped(["DXD", "XFX", "DXD"], {"X": INGOT, "D": CORE, "F": "minecraft:totem_of_undying"}))

# ---- consumables ------------------------------------------------------------------------------------------------------------------------
consumable("fire_tonic", G, dict(paint="bottle", pal=C, shape="flask"), name="Fire Tonic",
           abilities=[ab("give_effect", effect="strength", ticks=3600, amp=1, effect2="fire_resistance", ticks2=3600, amp2=0,
                         effect3="speed", ticks3=3600, amp3=0)],
           lore="Tastes like a campfire. Feels like winning.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:blaze_powder", "minecraft:magma_cream", CORE]))
consumable("phoenix_feast", G, dict(paint="biscuit", pal=C), name="Phoenix Feast", anim="eat", seconds=2.0, stack=16, nutrition=10,
           saturation=1.2, abilities=[ab("heal", amount=24), ab("give_effect", effect="regeneration", ticks=240, amp=2,
                                                            effect2="absorption", ticks2=1200, amp2=2)],
           lore="Cooked in a forge that has never gone out.",
           recipe=shapeless(["minecraft:cooked_beef", "minecraft:blaze_powder", "minecraft:honey_bottle", CORE]))
