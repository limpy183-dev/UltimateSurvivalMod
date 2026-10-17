"""Tier 1 - gate: start. Verdite: green-bronze, matte, honest. The first rung of the ladder."""
from .items import *

V = "verdite"
INGOT = "verdite_ingot"
STICK = "minecraft:stick"

# ---- projectile sprites (hidden items rendered by the Bolt entity) ----------------------------------
for _style, _pal in (("verdant", "verdite"), ("azure", "azurite"), ("ember", "cinder"), ("storm", "storm"),
                     ("blood", "blood"), ("chrono", "chrono"), ("void", "void"), ("radiant", "radiant"), ("apex", "apex"),
                     ("frost", "frost"), ("plague", "plague"), ("cosmic", "cosmic"), ("eternal", "eternal")):
    bolt(_style, _pal)

# ---- blocks & world generation ---------------------------------------------------------------------------
block("verdite_ore", "ore", dict(paint="ore_block", pal=V, seed=11), 3.0, 3.0, tool="stone", drop="raw_verdite", xp=(1, 3),
      lore="Green-bronze veins in ordinary stone.")
block("deepslate_verdite_ore", "ore", dict(paint="ore_block", pal=V, deepslate=True, seed=12), 4.5, 3.0, tool="stone",
      drop="raw_verdite", xp=(1, 3), deepslate=True)
block("verdite_block", "block", dict(paint="metal_block", pal=V), 5.0, 6.0, tool="stone", metal=True,
      lore="Nine ingots, pressed flat.", recipe=shaped(["XXX", "XXX", "XXX"], {"X": "verdite_ingot"}, category="building"))
ore_gen("ore_verdite", "verdite_ore", "deepslate_verdite_ore", size=9, count=15, min_y=-24, max_y=72)

# ---- materials -------------------------------------------------------------------------------------------
material("raw_verdite", "start", dict(paint="raw_ore", pal=V, seed=3), name="Raw Verdite", lore="Smelt it into something useful.")
material(INGOT, "start", dict(paint="ingot", pal=V), name="Verdite Ingot", lore="Green-bronze metal drawn from the shallow deeps.",
         recipe=[smelt(["raw_verdite", "verdite_ore", "deepslate_verdite_ore"], xp=0.8, time=200),
                 shapeless(["verdite_block"], count=9)])

# ---- tools: the 3x3 / felling / farming trio starts here --------------------------------------------------
tool("verdite_pickaxe", "start", "pickaxe", dict(paint="pickaxe", pal=V), name="Verdite Pickaxe",
     traits=dict(area=1), repair=INGOT, lore="Breaks a 3x3 area. Sneak to dig a single block.",
     recipe=shaped(["XXX", " S ", " S "], {"X": INGOT, "S": STICK}))
tool("verdite_axe", "start", "axe", dict(paint="axe", pal=V), name="Verdite Axe",
     traits=dict(fell=1), repair=INGOT, lore="Fells the whole tree with one log. Sneak to chop normally.",
     recipe=shaped(["XX", "XS", " S"], {"X": INGOT, "S": STICK}))
tool("verdite_shovel", "start", "shovel", dict(paint="shovel", pal=V), name="Verdite Shovel",
     traits=dict(area=1), repair=INGOT, lore="Digs a 3x3 area of soft ground.",
     recipe=shaped(["X", "S", "S"], {"X": INGOT, "S": STICK}))
tool("verdite_hoe", "start", "hoe", dict(paint="hoe", pal=V), name="Verdite Hoe",
     traits=dict(hoe_area=1, replant=1), repair=INGOT, lore="Tills, harvests and replants a 3x3 patch.",
     recipe=shaped(["XX", " S", " S"], {"X": INGOT, "S": STICK}))

# ---- weapons ------------------------------------------------------------------------------------------------
weapon("verdite_sword", "start", "sword",
       dict(paint="sword", pal=V, long=11, wide=1.2, guard="cross", pommel="ball"),
       name="Verdite Blade", lore="Honest metal. Everyone starts somewhere.", repair=INGOT,
       abilities=[ab("on_kill_buff", effect="speed", ticks=80, amp=0)],
       recipe=shaped(["X", "X", "S"], {"X": INGOT, "S": STICK}))
weapon("verdite_dagger", "start", "dagger",
       dict(paint="sword", pal=V, long=7, wide=0.95, guard="narrow", pommel="ring", offset=2.6),
       name="Verdite Dagger", lore="Quick, quiet, and rude to turn your back on.", repair=INGOT,
       abilities=[ab("backstab", mult=1.7)],
       recipe=shaped(["X", "S"], {"X": INGOT, "S": STICK}))
bow("verdite_bow", "start", dict(paint="bow", pal=V), name="Verdite Longbow", ammo=True, arrow_mult=1.6, draw=18,
    repair=INGOT, lore="A sturdy bow that throws arrows harder than it has any right to.",
    recipe=shaped([" XS", "X S", " XS"], {"X": INGOT, "S": "minecraft:string"}))

# ---- armor: Verdant Resolve -----------------------------------------------------------------------------------
armor_set("verdite", "Verdite", "start", V, INGOT, [dict(need=4, **ab("regen_ooc", delay=100, amount=1))],
          lore="Mossy plates stitched with copper wire.", ambient="leaf", color=0x6FD26A,
          style=dict(visor="slit", gem=True), layer=dict(visor="slit", glow=False, emblem="gem"),
          set_desc="Heal 1 health per second when unhurt for 5 seconds.")

# ---- accessories ----------------------------------------------------------------------------------------------------
accessory("clover_charm", "start", dict(paint="clover", pal=V), name="Lucky Clover",
          attrs=[dict(attr="luck", amount=1.5)], lore="Four leaves, one lucky streak.",
          recipe=shaped(["LL", "LL"], {"L": "minecraft:short_grass"}))
accessory("verdite_ring", "start", dict(paint="ring", pal=V), name="Verdite Ring",
          attrs=[dict(attr="armor", amount=2.0), dict(attr="armor_toughness", amount=1.0)],
          lore="A plain band. It keeps surprising people.",
          recipe=shaped([" X ", "X X", " X "], {"X": INGOT}))
accessory("hunters_pendant", "start", dict(paint="amulet", pal=V), name="Hunter's Pendant",
          attrs=[dict(attr="movement_speed", amount=0.10, op="mul_base"), dict(attr="step_height", amount=0.5)],
          lore="Light on the feet; the forest stops being in the way.",
          recipe=shaped(["S S", " X ", " I "], {"S": "minecraft:string", "X": INGOT, "I": "minecraft:iron_ingot"}))

# ---- consumables -------------------------------------------------------------------------------------------------
consumable("healing_salve", "start", dict(paint="jar", pal=V), name="Healing Salve",
           abilities=[ab("heal", amount=8), ab("give_effect", effect="regeneration", ticks=100, amp=0)],
           lore="Smells like a meadow after rain.", seconds=1.0,
           recipe=shaped(["H", "B"], {"H": "minecraft:glistering_melon_slice", "B": "minecraft:glass_bottle"}))
consumable("hunters_tonic", "start", dict(paint="bottle", pal=V), name="Hunter's Tonic",
           abilities=[ab("give_effect", effect="speed", ticks=1200, amp=1, effect2="strength", ticks2=1200, amp2=0)],
           lore="Bitter, green, and surprisingly effective.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:sugar", "minecraft:spider_eye", INGOT]))
consumable("hardtack", "start", dict(paint="biscuit", pal=V), name="Hardtack", anim="eat", stack=64, nutrition=8, saturation=1.2,
           lore="Keeps for years. Tastes like it.", recipe=shaped(["WW", "WW"], {"W": "minecraft:wheat"}, count=4, category="misc"))


# ---- the Codex: handed out once on first join, craftable if lost ---------------------------------------------------
add("codex", "codex", "start", dict(paint="tome", pal="drake", anim="shimmer"), name="Codex of the Sundering",
    lore="Every Sovereign, every relic, every rule - hidden until you earn the page.", stack=1, rarity="rare",
    recipe=shapeless(["minecraft:book", "minecraft:redstone"], category="misc"))
