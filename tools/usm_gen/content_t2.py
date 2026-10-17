"""Tier 2 - gate: ender_dragon. Azurite: sky-blue and silver, cool to the touch, forged with drakescale."""
from .items import *

A = "azurite"
INGOT = "azurite_ingot"
SCALE = "drakescale"
STICK = "minecraft:stick"
G = "ender_dragon"

# ---- blocks & world generation ---------------------------------------------------------------------------
block("azurite_ore", "ore", dict(paint="ore_block", pal=A, seed=21), 4.0, 3.0, tool="diamond", drop="raw_azurite", xp=(3, 7),
      lore="Pale blue crystal, buried where the stone turns to slate.", gate=G)
block("deepslate_azurite_ore", "ore", dict(paint="ore_block", pal=A, deepslate=True, seed=22), 5.5, 3.0, tool="diamond",
      drop="raw_azurite", xp=(3, 7), deepslate=True, gate=G)
block("azurite_block", "block", dict(paint="metal_block", pal=A), 6.0, 7.0, tool="diamond", metal=True, lore="Cold and bright.",
      gate=G, recipe=shaped(["XXX", "XXX", "XXX"], {"X": "azurite_ingot"}, category="building"))
ore_gen("ore_azurite", "azurite_ore", "deepslate_azurite_ore", size=6, count=5, min_y=-64, max_y=-4, discard=0.25)

# ---- materials -------------------------------------------------------------------------------------------
material("raw_azurite", G, dict(paint="raw_ore", pal=A, seed=7), name="Raw Azurite", lore="Needs a hot furnace and a steady hand.")
material(INGOT, G, dict(paint="ingot", pal=A), name="Azurite Ingot", lore="Sky-blue steel; it hums faintly near the End.",
         recipe=[smelt(["raw_azurite", "azurite_ore", "deepslate_azurite_ore"], xp=1.6, time=300),
                 shapeless(["azurite_block"], count=9)])
material(SCALE, G, dict(paint="scale", pal=A), name="Drakescale",
         lore="Shed by the dragon of the End as it fell. Still warm.", rarity="rare")

# ---- tools -------------------------------------------------------------------------------------------------
tool("azurite_pickaxe", G, "pickaxe", dict(paint="pickaxe", pal=A, vein=True, spikes=True, bend=3.0), name="Azurite Pickaxe",
     traits=dict(area=1, vein=24, magnet=1), repair=INGOT,
     lore="3x3 mining, ore veins fall together, and drops fly to your pack. Sneak to dig one block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": SCALE, "S": STICK}))
tool("azurite_axe", G, "axe", dict(paint="axe", pal=A, size=1.2, vein=True), name="Azurite Axe",
     traits=dict(fell=1, fell_leaves=1, magnet=1), repair=INGOT, lore="Fells the whole tree, leaves and all.",
     recipe=shaped(["XX", "XD", " S"], {"X": INGOT, "D": SCALE, "S": STICK}))
tool("azurite_shovel", G, "shovel", dict(paint="shovel", pal=A, point=True, vein=True), name="Azurite Shovel",
     traits=dict(area=1, depth=1, magnet=1), repair=INGOT, lore="3x3 digging with drops pulled straight to you.",
     recipe=shaped(["X", "D", "S"], {"X": INGOT, "D": SCALE, "S": STICK}))
tool("azurite_hoe", G, "hoe", dict(paint="hoe", pal=A, reach=5.4, vein=True), name="Azurite Hoe",
     traits=dict(hoe_area=2, replant=1, magnet=1), repair=INGOT, lore="Tills, harvests and replants a 5x5 field.",
     recipe=shaped(["XX", " D", " S"], {"X": INGOT, "D": SCALE, "S": STICK}))

# ---- weapons -----------------------------------------------------------------------------------------------
weapon("wyrmfang", G, "sword",
       dict(paint="sword", pal=A, long=13, wide=1.05, guard="wing", pommel="gem", vein=True, anim="shimmer"),
       name="Wyrmfang", lore="A fang of the End, ground to an edge.", repair=INGOT,
       abilities=[ab("dash_strike", dist=7, dmg=10, cd=100, style="azure"), ab("on_kill_buff", effect="speed", ticks=100, amp=1)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": SCALE, "S": STICK}))
weapon("drakebreaker", G, "greatsword",
       dict(paint="sword", pal=A, long=15, wide=1.9, guard="wing", pommel="gem", offset=-1.2, vein=True, anim="shimmer"),
       name="Drakebreaker", lore="Heavy enough to ring the ground like a bell.", repair=INGOT,
       abilities=[ab("ground_slam", radius=6, dmg=13, cd=160, kb=1.2, style="azure"), ab("knockback_burst", power=1.3)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": SCALE, "S": STICK}))
bow("skyshot_bow", G, dict(paint="bow", pal=A, glow=True), name="Skyshot",
    shot=dict(dmg=12, speed=3.0, pierce=2, style="azure", trail="end_rod", count=1, life=50), draw=16, nograv=True,
    repair=INGOT, lore="Needs no arrows: it draws light itself. Bolts pierce.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": SCALE, "S": "minecraft:string"}))
weapon("azure_wand", G, "staff",
       dict(paint="staff", pal=A, head="orb", runes=True, anim="pulse"),
       name="Azure Wand", lore="Three bolts a flick. Sneak to blink.", repair=INGOT,
       abilities=[ab("projectile_wave", count=3, spread=30, dmg=7, speed=1.6, style="azure", cd=40, trail="dust"),
                  ab("blink", dist=9, cd=100)],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": SCALE, "S": STICK}))

# ---- armor: Skyborne ----------------------------------------------------------------------------------------------
armor_set("azurite", "Azurite", G, A, INGOT,
          [dict(need=4, **ab("double_jump", jumps=1, power=0.62)), dict(need=4, **ab("feather_fall")),
           dict(need=4, **ab("glide_sneak"))],
          lore="Light as cloud-glass, hard as the dragon's temper.", special=SCALE, ambient="cloud", color=0x7FE8FF,
          style=dict(horns=True, wings=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Jump again in mid-air, never take fall damage, hold sneak to glide.")

# ---- accessories ----------------------------------------------------------------------------------------------------
accessory("azure_amulet", G, dict(paint="amulet", pal=A), name="Azure Amulet",
          attrs=[dict(attr="armor", amount=3.0), dict(attr="knockback_resistance", amount=0.2)],
          lore="Cold against the skin, steady against the blow.",
          recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": SCALE}))
accessory("drake_heart", G, dict(paint="heart", pal=A), name="Drake Heart",
          attrs=[dict(attr="max_health", amount=4.0), dict(attr="attack_damage", amount=0.08, op="mul_base")],
          lore="It beats once a minute. The dragon liked it that way.",
          recipe=shaped(["DXD", "XRX", " X "], {"D": SCALE, "X": INGOT, "R": "minecraft:heart_of_the_sea"}))
accessory("feather_charm", G, dict(paint="feather", pal=A), name="Skyfeather Charm",
          abilities=[ab("feather_fall"), ab("glide_sneak")], lore="Weighs nothing, saves everything.",
          recipe=shaped([" F ", "FXF", " D "], {"F": "minecraft:feather", "X": INGOT, "D": SCALE}))
accessory("magnet_ring", G, dict(paint="horseshoe", pal="magnet"), name="Magnet Ring",
          abilities=[ab("magnet", radius=7)], lore="Drops fall toward you as if they'd been looking for you.",
          recipe=shaped(["R R", "RXR", " D "], {"R": "minecraft:redstone", "X": INGOT, "D": SCALE}))

# ---- consumables -------------------------------------------------------------------------------------------------------
consumable("greater_salve", G, dict(paint="jar", pal=A), name="Greater Salve",
           abilities=[ab("heal", amount=16), ab("give_effect", effect="regeneration", ticks=160, amp=1)],
           lore="The good stuff, bottled for dragons' wounds.", seconds=1.0,
           recipe=shapeless(["healing_salve", "healing_salve", SCALE]))
consumable("drake_brew", G, dict(paint="bottle", pal="drake", shape="flask"), name="Drake Brew",
           abilities=[ab("give_effect", effect="resistance", ticks=2400, amp=0, effect2="fire_resistance", ticks2=2400, amp2=0,
                         effect3="night_vision", ticks3=2400, amp3=0)],
           lore="Smoke curls out when you uncork it.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:dragon_breath", SCALE, "minecraft:blaze_powder"]))
consumable("heart_crystal", G, dict(paint="crystal_heart", pal="crimson"), name="Heart Crystal",
           abilities=[ab("perm_health", max=5)], lore="Permanently adds one heart (up to 5). Once only per crystal.",
           rarity="rare", seconds=1.6, glint=True,
           recipe=shaped(["DXD", "XGX", "DXD"], {"D": SCALE, "X": INGOT, "G": "minecraft:diamond"}))

# ---- gadget ----------------------------------------------------------------------------------------------------------------
gadget("recall_mirror", G, dict(paint="mirror", pal=A), name="Recall Mirror",
       abilities=[ab("recall", delay=60, cd=900)], lore="Hold still, look into it, and you are home.",
       recipe=shaped([" XD", " SX", "S  "], {"X": INGOT, "D": SCALE, "S": STICK}))
