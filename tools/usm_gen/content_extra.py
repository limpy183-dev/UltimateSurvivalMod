"""The arsenal expansion: more weapon families and a few charms for tiers 2-5, authored at the old numbers (the global power pass scales them like everything else).

Every Sovereign's branch used to offer a sword and two or three more; now each one has a spear, a rapier, a scythe, a hammer or a dagger of its own too, the Omnarch gets
the full tool set and a handful of accessories fill the gaps. Ingredients follow each tier's convention: the tier's ingot, the Sovereign's core, a stick."""
from .items import *

STICK = "minecraft:stick"
STRING = "minecraft:string"

# =====================================================================================================================
# tier 2 - the Ender Dragon (azurite + drakescale)
# =====================================================================================================================
D = "ender_dragon"
weapon("wyrm_rapier", D, "rapier",
       dict(paint="sword", pal="azurite", long=14, wide=0.8, guard="basket", pommel="ring", edge="crystal", anim="shimmer"),
       name="Wyrm Rapier", lore="Thin as a rumour, and it gets around.", repair="azurite_ingot",
       abilities=[ab("dash_strike", dist=9, dmg=14, cd=70, style="azure"), ab("bleed", ticks=100, amp=1)],
       recipe=shaped(["  D", " X ", "XS "], {"X": "azurite_ingot", "D": "drakescale", "S": STICK}))
weapon("drake_glaive", D, "spear",
       dict(paint="sword", pal="azurite", long=16, wide=0.9, guard="narrow", pommel="spike", edge="crystal", anim="shimmer"),
       name="Drake Glaive", lore="Reaches a little farther than seems fair.", repair="azurite_ingot",
       abilities=[ab("projectile_wave", count=3, spread=24, dmg=12, speed=1.7, style="azure", cd=60, trail="end_rod"), ab("on_kill_heal", amount=3)],
       recipe=shaped(["  X", " SD", "S  "], {"X": "azurite_ingot", "D": "drakescale", "S": STICK}))
accessory("wyrm_scale_charm", D, dict(paint="charm_shield", pal="azurite"), name="Wyrm Scale Charm",
          attrs=[dict(attr="armor", amount=3.0), dict(attr="max_health", amount=2.0)], abilities=[ab("thorns", pct=0.15)],
          lore="Scales, still bristling at whatever bit them.", recipe=shaped([" D ", "XSX", " D "], {"X": "azurite_ingot", "D": "drakescale", "S": STRING}))

# =====================================================================================================================
# tier 3 - Ignarok (cinderite + ember core)
# =====================================================================================================================
G = "ignarok"
weapon("ash_scythe", G, "scythe",
       dict(paint="sword", pal="cinder", long=13, wide=1.1, guard="narrow", pommel="spike", curve=-2.2, edge="flame", vein=True, anim="flicker"),
       name="Ash Scythe", lore="Harvests whatever was still green.", repair="cinderite_ingot",
       abilities=[ab("fire_nova", radius=7, dmg=22, ignite=6, cd=120), ab("ignite", ignite=5, dmg=5), ab("burning_bonus", mult=1.5)],
       recipe=shaped(["XXD", " SX", "S  "], {"X": "cinderite_ingot", "D": "ember_core", "S": STICK}))
weapon("volcano_hammer", G, "hammer",
       dict(paint="axe", pal="cinder", size=1.7, double=True, vein=True, anim="flicker"),
       name="Volcano Hammer", lore="One swing makes a mountain.", repair="cinderite_ingot",
       abilities=[ab("ground_slam", radius=7, dmg=40, cd=150, kb=1.5, style="ember", stun=40), ab("ignite", ignite=6, dmg=6)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": "cinderite_ingot", "D": "ember_core", "S": STICK}))
weapon("ember_spear", G, "spear",
       dict(paint="sword", pal="cinder", long=16, wide=0.9, guard="narrow", pommel="spike", edge="flame", anim="flicker"),
       name="Ember Spear", lore="Thrown, it lands. Held, it still lands.", repair="cinderite_ingot",
       abilities=[ab("dash_strike", dist=12, dmg=30, cd=80, style="ember"), ab("burning_bonus", mult=1.4)],
       recipe=shaped(["  X", " SD", "S  "], {"X": "cinderite_ingot", "D": "ember_core", "S": STICK}))
accessory("swiftness_anklet", G, dict(paint="ring", pal="cinder"), name="Swiftness Anklet",
          attrs=[dict(attr="movement_speed", amount=0.2, op="mul_base")], abilities=[ab("feather_fall"), ab("fire_res")],
          lore="Runs hot, and keeps the wearer ahead of it.", recipe=shaped(["X X", " D ", "X X"], {"X": "cinderite_ingot", "D": "ember_core"}))

# =====================================================================================================================
# tier 4 - Voltaris, Hemorath, Chronos (stormite + their cores)
# =====================================================================================================================
V, H, C = "voltaris", "hemorath", "chronos"
weapon("storm_rapier", V, "rapier",
       dict(paint="sword", pal="storm", long=14.5, wide=0.8, guard="basket", pommel="ring", edge="lightning", anim="flicker"),
       name="Storm Rapier", lore="The point arrives a moment before the sound.", repair="stormite_ingot",
       abilities=[ab("chain_lightning", dmg=20, jumps=5, range=22, jump_range=7, cd=90), ab("shock", chance=0.35, stun=20, dmg=12)],
       recipe=shaped(["  D", " X ", "XS "], {"X": "stormite_ingot", "D": "storm_core", "S": STICK}))
weapon("tempest_spear", V, "spear",
       dict(paint="sword", pal="storm", long=16.5, wide=0.9, guard="narrow", pommel="spike", edge="lightning", anim="flicker"),
       name="Tempest Spear", lore="Aimed at the sky, it takes the hint.", repair="stormite_ingot",
       abilities=[ab("call_lightning", strikes=4, dmg=24, radius=4, range=32, cd=150), ab("dash_strike", dist=14, dmg=36, cd=70, style="storm")],
       recipe=shaped(["  X", " SD", "S  "], {"X": "stormite_ingot", "D": "storm_core", "S": STICK}))
weapon("crimson_greatsword", H, "greatsword",
       dict(paint="sword", pal="blood", long=15, wide=1.9, guard="round", pommel="spike", offset=-1.0, edge="serrated", vein=True, anim="pulse"),
       name="Crimson Greatsword", lore="Too heavy to lift, until it is lifting you.", repair="stormite_ingot",
       abilities=[ab("blood_reap", radius=7, dmg=36, heal_pct=0.4, cd=160), ab("lifesteal", pct=0.12), ab("execute", mult=2.0, threshold=0.3)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": "stormite_ingot", "D": "blood_core", "S": STICK}))
weapon("hunger_staff", H, "staff",
       dict(paint="staff", pal="blood", head="orb", runes=True, crown=True, anim="pulse"),
       name="Hunger Staff", lore="What it takes from one it gives to another.", repair="stormite_ingot",
       abilities=[ab("heal_pulse", amount=24, radius=6, cd=200), ab("projectile_wave", count=5, spread=40, dmg=22, speed=1.6, style="blood", cd=60, trail="crimson_spore")],
       recipe=shaped(["  D", " X ", "S  "], {"X": "stormite_ingot", "D": "blood_core", "S": STICK}))
accessory("titan_belt", H, dict(paint="belt", pal="blood"), name="Titan Belt",
          attrs=[dict(attr="max_health", amount=6.0), dict(attr="armor_toughness", amount=2.0)], abilities=[ab("crimson_veil", thr=0.5, pct=0.3)],
          lore="Holds a great deal in, and the rest out.", recipe=shaped(["XDX", "X X", " X "], {"X": "stormite_ingot", "D": "blood_core"}))
weapon("chrono_dagger", C, "dagger",
       dict(paint="sword", pal="chrono", long=8.5, wide=0.9, guard="narrow", pommel="ring", runes=True, anim="shimmer"),
       name="Chrono Dagger", lore="A second, sharpened.", repair="stormite_ingot",
       abilities=[ab("blink", dist=14, cd=50), ab("backstab", mult=2.5), ab("slow_hit", ticks=60, amp=2)],
       recipe=shaped(["D", "X", "S"], {"X": "stormite_ingot", "D": "chrono_core", "S": STICK}))
weapon("hourglass_hammer", C, "hammer",
       dict(paint="axe", pal="chrono", size=1.7, double=True, vein=True, anim="shimmer"),
       name="Hourglass Hammer", lore="Brings the hour down, all at once.", repair="stormite_ingot",
       abilities=[ab("time_freeze", radius=10, ticks=100, cd=400), ab("ground_slam", radius=7, dmg=50, cd=140, kb=1.4, style="chrono", stun=40)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": "stormite_ingot", "D": "chrono_core", "S": STICK}))
accessory("soul_lantern", C, dict(paint="amulet", pal="chrono"), name="Soul Lantern",
          attrs=[dict(attr="luck", amount=1.0)], abilities=[ab("night_vision"), ab("magnet", radius=12)],
          lore="Lights the way, and everything worth picking up on it.", recipe=shaped([" X ", "XDX", "X X"], {"X": "stormite_ingot", "D": "chrono_core"}))

# =====================================================================================================================
# tier 5 - Nihilus, Solareon, the Omnarch (aetherite + their cores)
# =====================================================================================================================
N, S, O = "nihilus", "solareon", "omnarch"
weapon("void_scythe", N, "scythe",
       dict(paint="sword", pal="void", long=14.5, wide=1.15, guard="narrow", pommel="spike", curve=-2.4, edge="crystal", vein=True, anim="pulse"),
       name="Void Scythe", lore="Reaps what was never planted.", repair="aetherite_ingot",
       abilities=[ab("gravity_pull", radius=12, dmg=40, cd=200), ab("rend", pct=0.035, cap=100), ab("devour", amount=6, absorb=2)],
       recipe=shaped(["XXD", " SX", "S  "], {"X": "aetherite_ingot", "D": "void_core", "S": STICK}))
weapon("rift_spear", N, "spear",
       dict(paint="sword", pal="void", long=17, wide=0.9, guard="narrow", pommel="spike", edge="crystal", anim="pulse"),
       name="Rift Spear", lore="Every thrust comes out of a different door.", repair="aetherite_ingot",
       abilities=[ab("dash_strike", dist=16, dmg=80, cd=60, style="void"), ab("projectile_wave", count=5, spread=40, dmg=40, speed=1.7, style="void", cd=60, trail="reverse_portal")],
       recipe=shaped(["  X", " SD", "S  "], {"X": "aetherite_ingot", "D": "void_core", "S": STICK}))
weapon("dawn_dagger", S, "dagger",
       dict(paint="sword", pal="radiant", long=9, wide=0.9, guard="narrow", pommel="gem", edge="crystal", anim="shimmer"),
       name="Dawn Dagger", lore="A glint, then a gap.", repair="aetherite_ingot",
       abilities=[ab("blink", dist=16, cd=40), ab("backstab", mult=3.0), ab("sear", dmg=24, glow=80)],
       recipe=shaped(["D", "X", "S"], {"X": "aetherite_ingot", "D": "radiant_core", "S": STICK}))
weapon("radiant_greatsword", S, "greatsword",
       dict(paint="sword", pal="radiant", long=16, wide=2.0, guard="crown", pommel="gem", offset=-1.2, edge="crystal", runes=True, anim="shimmer"),
       name="Radiant Greatsword", lore="Heavier than light has any right to be.", repair="aetherite_ingot",
       abilities=[ab("solar_flare", radius=12, dmg=70, blind=80, cd=200), ab("sunbeam", range=40, dmg=100, width=1.2, cd=120, style="radiant"), ab("smite", mult=1.6)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": "aetherite_ingot", "D": "radiant_core", "S": STICK}))
weapon("apex_dagger", O, "dagger",
       dict(paint="sword", pal="apex", long=9.5, wide=1.0, guard="narrow", pommel="ring", edge="flame", runes=True, anim="prism"),
       name="Apex Dagger", lore="The shortest distance between two halves.", repair="aetherite_ingot",
       abilities=[ab("blink", dist=20, cd=40), ab("backstab", mult=4.0), ab("rend", pct=0.05, cap=150), ab("lifesteal", pct=0.1)],
       recipe=shaped(["D", "X", "S"], {"X": "aetherite_ingot", "D": "omni_core", "S": STICK}))
weapon("apex_hammer", O, "hammer",
       dict(paint="axe", pal="apex", size=1.8, double=True, vein=True, anim="prism"),
       name="Apex Hammer", lore="Settles arguments with the ground.", repair="aetherite_ingot",
       abilities=[ab("ground_slam", radius=14, dmg=300, cd=110, kb=2.2, style="apex", stun=70), ab("apex_nova", radius=16, dmg=300, heal=30, cd=300, style="apex")],
       recipe=shaped(["XDX", "XSX", " S "], {"X": "aetherite_ingot", "D": "omni_core", "S": STICK}))
tool("apex_axe", O, "axe", dict(paint="axe", pal="apex", size=1.6, vein=True, double=True, anim="prism"), name="Apex Axe",
     traits=dict(fell=1, fell_leaves=1, smelt=1, magnet=1), repair="aetherite_ingot", lore="A forest is a splinter. The lumber arrives as charcoal.",
     recipe=shaped(["XX", "XD", " S"], {"X": "aetherite_ingot", "D": "omni_core", "S": STICK}))
tool("apex_shovel", O, "shovel", dict(paint="shovel", pal="apex", point=True, vein=True, anim="prism"), name="Apex Shovel",
     traits=dict(area=4, depth=4, smelt=1, magnet=1), repair="aetherite_ingot", lore="9x9 digging, four deep.",
     recipe=shaped(["X", "D", "S"], {"X": "aetherite_ingot", "D": "omni_core", "S": STICK}))
tool("apex_hoe", O, "hoe", dict(paint="hoe", pal="apex", reach=6.0, vein=True, anim="prism"), name="Apex Hoe",
     traits=dict(hoe_area=8, replant=1, magnet=1), repair="aetherite_ingot", lore="Tills, harvests and replants a 17x17 field in one stroke.",
     recipe=shaped(["XX", " D", " S"], {"X": "aetherite_ingot", "D": "omni_core", "S": STICK}))
accessory("aegis_charm", O, dict(paint="charm_shield", pal="apex"), name="Aegis Charm",
          attrs=[dict(attr="armor_toughness", amount=3.0), dict(attr="max_health", amount=8.0)], abilities=[ab("thorns", pct=0.35, shock=2), ab("dodge", chance=0.1)],
          lore="Turns the blow around and hands it back.", recipe=shaped(["XSX", "XDX", " X "], {"X": "aetherite_ingot", "D": "omni_core", "S": STRING}))
