"""Tier 4 - three stages, one per Sovereign. Stormite is the shared metal (mined on high peaks); each Sovereign's core makes its own gear.

  voltaris (storm)  -> tools, Tempest armor, shock / lightning weapons
  hemorath (blood)  -> Sanguine armor, life-stealing weapons
  chronos  (time)   -> Chronal armor, slowing / freezing / rewinding weapons"""
from .items import *
from . import mobs as MB
from .mobs import atk, phase, drop
from .rigs_t4 import voltaris, hemorath, chronos, storm_wisp, storm_totem, blood_bat

STICK = "minecraft:stick"
INGOT = "stormite_ingot"

# =====================================================================================================================
# shared metal
# =====================================================================================================================
V = "voltaris"
block("stormite_ore", "ore", dict(paint="ore_block", pal="storm", seed=41), 6.0, 6.0, tool="diamond", drop="raw_stormite", xp=(5, 11),
      lore="Blue-white veins on the windiest ridges.", gate=V)
block("stormite_block", "block", dict(paint="metal_block", pal="storm"), 8.0, 10.0, tool="diamond", metal=True, lore="Cold, humming.",
      gate=V, recipe=shaped(["XXX", "XXX", "XXX"], {"X": INGOT}, category="building"))
ore_gen("ore_stormite", "stormite_ore", "stormite_ore", size=7, count=16, min_y=70, max_y=210, discard=0.15)     # peaks and high hills only: most of the band is air
material("raw_stormite", V, dict(paint="raw_ore", pal="storm", seed=12), name="Raw Stormite", lore="It tingles when you hold it.")
material(INGOT, V, dict(paint="ingot", pal="storm"), name="Stormite Ingot", lore="Metal that remembers lightning.",
         recipe=[smelt(["raw_stormite", "stormite_ore"], xp=3.0, time=400), shapeless(["stormite_block"], count=9)])

# =====================================================================================================================
# stage 1 - Voltaris, the Storm Regent
# =====================================================================================================================
SC = "storm_core"
material(SC, V, dict(paint="core", pal="storm", seed=6), name="Storm Core", rarity="epic", glint=True,
         lore="A knot of trapped thunder. Voltaris leaves a handful behind.")
add("tempest_sigil", "sigil", "ignarok", dict(paint="sigil", pal="storm"), name="Tempest Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="voltaris"), obtain="Crafted from lightning rods, Cinderite and an Ember Core.",
    lore="Use it on a Ritual Altar in the Overworld, under open sky, to call Voltaris.",
    recipe=shaped(["LCL", "CEC", "LCL"], {"L": "minecraft:lightning_rod", "C": "cinderite_ingot", "E": "ember_core"}, category="misc"))
add("voltaris_bag", "bag", V, dict(paint="bag", pal="storm"), name="Voltaris's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="voltaris"), obtain="Dropped by Voltaris.", lore="Right-click to open. It crackles.")

tool("stormite_pickaxe", V, "pickaxe", dict(paint="pickaxe", pal="storm", vein=True, spikes=True, bend=3.4, anim="flicker"),
     name="Stormite Pickaxe", traits=dict(area=2, depth=3, vein=64, smelt=1, magnet=1), repair=INGOT,
     lore="Mines a 5x5 face three deep, chases veins and smelts as it goes. Sneak for a single block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": SC, "S": STICK}))
tool("stormite_axe", V, "axe", dict(paint="axe", pal="storm", size=1.4, vein=True, double=True, anim="flicker"), name="Stormite Axe",
     traits=dict(fell=1, fell_leaves=1, smelt=1, magnet=1), repair=INGOT, lore="Fells whole forests; the lumber arrives as charcoal.",
     recipe=shaped(["XX", "XD", " S"], {"X": INGOT, "D": SC, "S": STICK}))
tool("stormite_shovel", V, "shovel", dict(paint="shovel", pal="storm", point=True, vein=True, anim="flicker"), name="Stormite Shovel",
     traits=dict(area=2, depth=2, smelt=1, magnet=1), repair=INGOT, lore="5x5 digging, two deep.",
     recipe=shaped(["X", "D", "S"], {"X": INGOT, "D": SC, "S": STICK}))
tool("stormite_hoe", V, "hoe", dict(paint="hoe", pal="storm", reach=5.4, vein=True, anim="flicker"), name="Stormite Hoe",
     traits=dict(hoe_area=4, replant=1, magnet=1), repair=INGOT, lore="Tills, harvests and replants a 9x9 field.",
     recipe=shaped(["XX", " D", " S"], {"X": INGOT, "D": SC, "S": STICK}))

weapon("tempest_edge", V, "sword",
       dict(paint="sword", pal="storm", long=13.5, wide=0.95, guard="narrow", pommel="ring", edge="lightning", anim="flicker"),
       name="Tempest Edge", lore="It hums before the thunder does.", repair=INGOT,
       abilities=[ab("chain_lightning", dmg=26, jumps=5, range=22, jump_range=7, cd=100), ab("shock", chance=0.35, stun=24, dmg=14)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": SC, "S": STICK}))
weapon("thunder_hammer", V, "hammer",
       dict(paint="axe", pal="storm", size=1.6, double=True, vein=True, anim="flicker"),
       name="Thunder Hammer", lore="Swing it like you mean to be heard.", repair=INGOT,
       abilities=[ab("ground_slam", radius=8, dmg=44, cd=160, kb=1.5, style="storm", stun=40), ab("shock", chance=0.5, stun=30, dmg=16)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": INGOT, "D": SC, "S": STICK}))
bow("thunderbow", V, dict(paint="bow", pal="storm", glow=True), name="Thunderbow",
    shot=dict(dmg=32, speed=3.5, pierce=3, style="storm", trail="electric_spark", count=1, life=60, aoe=1.6), draw=14, nograv=True, repair=INGOT,
    lore="The string sings. The target does not hear it.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": SC, "S": "minecraft:string"}))
weapon("stormcaller_staff", V, "staff",
       dict(paint="staff", pal="storm", head="orb", runes=True, crown=True, anim="flicker"),
       name="Stormcaller Staff", lore="Point at the sky's grudge. Sneak for a fan of sparks.", repair=INGOT,
       abilities=[ab("call_lightning", strikes=3, dmg=22, radius=3.6, range=34, cd=180, style="storm"),
                  ab("projectile_wave", count=5, spread=40, dmg=15, speed=1.7, style="storm", cd=60, trail="electric_spark")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": SC, "S": STICK}))

armor_set("tempest", "Tempest", V, "storm", INGOT,
          [dict(need=4, **ab("double_jump", jumps=2, power=0.72)), dict(need=4, **ab("thorns", pct=0.35, shock=3)),
           dict(need=4, **ab("storm_aura", radius=8, dmg=12, every=60, targets=3))],
          lore="Plates of storm-glass over a lightning-cloth lining.", special=SC, ambient="storm", color=0xFFE94D,
          attrs=[dict(attr="movement_speed", amount=0.04, op="mul_base")],
          style=dict(spikes=True, crest=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Two extra jumps, shock attackers, and lightning that picks off whoever stands near you.")

accessory("storm_charm", V, dict(paint="feather", pal="storm"), name="Storm Charm",
          attrs=[dict(attr="movement_speed", amount=0.12, op="mul_base")], abilities=[ab("feather_fall")],
          lore="A feather that has never been rained on.",
          recipe=shaped([" F ", "FXF", " D "], {"F": "minecraft:phantom_membrane", "X": INGOT, "D": SC}))
accessory("conduit_ring", V, dict(paint="ring", pal="storm"), name="Conduit Ring",
          attrs=[dict(attr="attack_speed", amount=0.15, op="mul_base")], abilities=[ab("haste_aura")],
          lore="Static crawls up your arm and then you are faster.",
          recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": SC}))
accessory("heart_of_voltaris", V, dict(paint="heart", pal="storm"), name="Heart of Voltaris",
          attrs=[dict(attr="max_health", amount=8.0)], abilities=[ab("storm_aura", radius=6, dmg=8, every=80, targets=2)],
          rarity="epic", glint=True, obtain="Dropped by Voltaris.", lore="It beats once, and the sky answers.")
consumable("storm_tonic", V, dict(paint="bottle", pal="storm", shape="flask"), name="Storm Tonic",
           abilities=[ab("give_effect", effect="speed", ticks=4800, amp=2, effect2="jump_boost", ticks2=4800, amp2=2,
                         effect3="haste", ticks3=4800, amp3=1)],
           lore="Fizzes, and not entirely from the carbonation.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:lightning_rod", "minecraft:sugar", SC]))
consumable("thunder_feast", V, dict(paint="biscuit", pal="storm"), name="Thunder Feast", anim="eat", seconds=2.0, nutrition=12,
           saturation=1.4, abilities=[ab("heal", amount=30), ab("give_effect", effect="absorption", ticks=2400, amp=3,
                                                              effect2="speed", ticks2=1200, amp2=1)],
           lore="Eat it standing up.", recipe=shapeless(["minecraft:cooked_porkchop", "minecraft:golden_carrot", "minecraft:honey_bottle", SC]))

# ---- minions ------------------------------------------------------------------------------------------------------------------------------
MB.mob("storm_wisp", "Storm Wisp", storm_wisp(), V, hp=34, dmg=12, size=(0.8, 0.9), speed=0.3, fly=True, hover=1.5, keep=[6, 14], style="storm",
       egg=(0xb9cbea, 0xffe94d), xp=10, minion_of="voltaris", follow=48, melee=False,
       attacks=[atk("fan", 1, count=1, spread=0, dmg=0.9, speed=1.3, windup=14, recover=30, style="storm", range=[4, 28], size=0.9, aoe=1.2,
                    trail="electric_spark")],
       drops=[drop("minecraft:redstone", 0, 2, 0.5)],
       sounds=dict(hurt="entity.breeze.hurt", death="entity.breeze.death", ambient="entity.breeze.idle_air", pitch=1.6),
       lore="A knot of weather that learned to aim.")
MB.mob("storm_totem", "Storm Pylon", storm_totem(), V, hp=95, dmg=14, armor=6, size=(1.1, 3.4), speed=0.0, kb=1.0, style="storm",
       egg=(0x394a66, 0x9fd8ff), xp=20, minion_of="voltaris", follow=48, melee=False, fire=True,
       attacks=[atk("lightning", 1, count=2, delay=30, radius=2.0, dmg=0.9, area=3, aimed=2, windup=20, recover=40, interval=14, style="storm",
                    range=[0, 40])],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.amethyst_cluster.break", pitch=0.7),
       lore="Anchors the Regent's shield. Break it.")

_p1 = lambda w, **k: atk("lightning", w, windup=30, recover=20, range=[0, 50], style="storm", **k)
MB.boss(
    "voltaris", "Voltaris, the Storm Regent", voltaris(), size=(1.7, 3.7), speed=0.3, title="Sovereign of Thunder", fly=True, hover=3.0,
    style="storm", keep=[9, 18], bar=dict(color="yellow", overlay="notched_10"), minion="storm_wisp", sigil="tempest_sigil", xp=1100,
    egg=(0x27324f, 0xffe94d), kb=1.0, death_ticks=100,
    ritual=dict(dim="minecraft:overworld", time="any", sky=True, xp=0, hearts=0, items=[]),
    sounds=dict(hurt="entity.breeze.hurt", death="entity.lightning_bolt.thunder", ambient="entity.breeze.idle_air", pitch=0.6),
    bag=[drop(SC, 8, 12), drop(INGOT, 8, 12), drop("heart_of_voltaris", 1, 1), drop("minecraft:netherite_ingot", 1, 2),
         drop("minecraft:experience_bottle", 20, 30), drop("thunder_hammer", 1, 1, 0.3), drop("minecraft:enchanted_golden_apple", 1, 1, 0.35)],
    phases=[
        phase("Gathering Clouds", 1.0, cd=(44, 72), attacks=[
            _p1(3, count=5, radius=2.4, dmg=1.3, delay=27, interval=9, area=9, aimed=2),
            atk("orbs", 2, count=3, dmg=0.8, speed=0.42, interval=14, style="storm", range=[4, 50], windup=24, recover=20),
            atk("gust", 2, radius=12, dmg=0.35, kb=2.0, windup=24, recover=20, style="storm", range=[0, 12]),
            atk("fan", 2, count=5, spread=36, dmg=0.55, speed=1.2, windup=22, recover=14, style="storm", trail="electric_spark", range=[6, 50]),
            atk("perch", 1, windup=36, stun=80, vuln=1.6, radius=5, style="storm", range=[0, 60])]),
        phase("Tempest", 0.66, cd=(36, 60), speed=1.1, dmg=1.1, color="blue",
              enter=[atk("summon", mob="storm_wisp", count=3, cap=5, windup=30, recover=16, style="storm")],
              attacks=[
                  _p1(3, count=7, radius=2.4, dmg=1.4, delay=27, interval=7, area=10, aimed=3),
                  atk("orbs", 2, count=4, dmg=0.85, speed=0.45, interval=12, style="storm", range=[4, 50], windup=24, recover=20),
                  atk("gust", 2, radius=13, dmg=0.4, kb=2.2, windup=22, recover=18, style="storm", range=[0, 13]),
                  atk("fan", 2, count=6, spread=44, dmg=0.55, speed=1.25, windup=20, recover=14, volleys=2, interval=10, style="storm",
                      trail="electric_spark", range=[6, 50]),
                  atk("spiral", 2, arms=3, dmg=0.45, duration=60, interval=4, speed=0.55, windup=20, recover=20, style="storm", range=[0, 50]),
                  atk("summon", 2, mob="storm_wisp", count=2, cap=5, windup=30, recover=20, style="storm", range=[0, 60]),
                  atk("perch", 1, windup=34, stun=80, vuln=1.6, radius=5.5, style="storm", range=[0, 60])]),
        phase("Eye of the Storm", 0.33, cd=(28, 50), speed=1.2, dmg=1.25, color="white",
              nodes=dict(mob="storm_totem", count=3, radius=10),
              attacks=[
                  _p1(3, count=9, radius=2.4, dmg=1.5, delay=26, interval=6, area=11, aimed=3),
                  atk("orbs", 2, count=5, dmg=0.9, speed=0.48, interval=10, style="storm", range=[4, 50], windup=22, recover=18),
                  atk("gust", 1, radius=14, dmg=0.4, kb=2.4, windup=22, recover=18, style="storm", range=[0, 14]),
                  atk("gust", 1, radius=16, dmg=0.3, kb=2.0, pull=True, windup=26, recover=18, style="storm", range=[6, 50]),
                  atk("spiral", 2, arms=4, dmg=0.5, duration=70, interval=4, speed=0.55, turn=11, windup=20, recover=20, style="storm", range=[0, 50]),
                  atk("fan", 2, count=7, spread=50, dmg=0.6, speed=1.3, volleys=2, interval=9, windup=18, recover=14, style="storm",
                      trail="electric_spark", range=[6, 50]),
                  atk("perch", 2, windup=30, stun=90, vuln=1.8, radius=6, style="storm", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 2 - Hemorath, the Crimson Matriarch
# =====================================================================================================================
H = "hemorath"
BC = "blood_core"
material(BC, H, dict(paint="core", pal="blood", seed=8), name="Blood Core", rarity="epic", glint=True,
         lore="Still warm. Still, somehow, hungry.")
add("blood_sigil", "sigil", "voltaris", dict(paint="sigil", pal="blood"), name="Blood Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="hemorath"), obtain="Crafted from redstone blocks, bone blocks and a Storm Core.",
    lore="Use it on a Ritual Altar in the Overworld at night. It costs four hearts of health.",
    recipe=shaped(["RBR", "BCB", "RBR"], {"R": "minecraft:redstone_block", "B": "minecraft:bone_block", "C": SC}, category="misc"))
add("hemorath_bag", "bag", H, dict(paint="bag", pal="blood"), name="Hemorath's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="hemorath"), obtain="Dropped by Hemorath.", lore="Right-click to open. It is wet.")

weapon("bloodletter", H, "sword",
       dict(paint="sword", pal="blood", long=12, wide=1.3, guard="round", pommel="spike", edge="serrated", vein=True, anim="pulse"),
       name="Bloodletter", lore="It drinks. It is never full.", repair=INGOT,
       abilities=[ab("lifesteal", pct=0.16), ab("bleed", secs=6, ticks=120, amp=1), ab("execute", mult=1.8, threshold=0.3)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": BC, "S": STICK}))
weapon("crimson_scythe", H, "scythe",
       dict(paint="sword", pal="blood", long=14, wide=1.1, guard="narrow", pommel="spike", curve=-2.2, edge="serrated", vein=True, anim="pulse"),
       name="Crimson Scythe", lore="Reaps in arcs. Gives most of it back to you.", repair=INGOT,
       abilities=[ab("blood_reap", radius=6, dmg=38, heal_pct=0.45, cd=180), ab("lifesteal", pct=0.1)],
       recipe=shaped(["XXD", " SX", "S  "], {"X": INGOT, "D": BC, "S": STICK}))
bow("fang_bow", H, dict(paint="bow", pal="blood", glow=True), name="Fang Bow",
    shot=dict(dmg=34, speed=3.4, pierce=2, style="blood", trail="crimson_spore", count=1, life=70, aoe=1.2, homing=True), draw=15, nograv=True,
    repair=INGOT, lore="The arrows find the warm ones on their own.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": BC, "S": "minecraft:string"}))
weapon("vampire_dagger", H, "dagger",
       dict(paint="sword", pal="blood", long=8.5, wide=0.9, guard="narrow", pommel="ring", edge="serrated", anim="pulse"),
       name="Vampire Dagger", lore="Short, silent, and thirsty.", repair=INGOT,
       abilities=[ab("backstab", mult=2.4), ab("lifesteal", pct=0.2), ab("dash_strike", dist=8, dmg=30, cd=90, style="blood")],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": BC, "S": STICK}))

armor_set("sanguine", "Sanguine", H, "blood", INGOT,
          [dict(need=4, **ab("bloodlust", pct=0.1, cap=8)), dict(need=4, **ab("crimson_veil", thr=0.5, pct=0.3)),
           dict(need=4, **ab("regen_ooc", amount=2, delay=60))],
          lore="Lacquered with something you should not ask about.", special=BC, ambient="blood", color=0xD0102A,
          attrs=[dict(attr="max_health", amount=2.0)],
          style=dict(horns=True, spikes=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Heal from the damage you deal, take less once you are hurt, and mend quickly when left alone.")

accessory("blood_pendant", H, dict(paint="amulet", pal="blood"), name="Blood Pendant",
          attrs=[dict(attr="max_health", amount=10.0)], abilities=[ab("bloodlust", pct=0.05, cap=4)],
          lore="A droplet that never dries.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": BC}))
accessory("vampiric_ring", H, dict(paint="ring", pal="blood"), name="Vampiric Ring",
          attrs=[dict(attr="attack_damage", amount=0.15, op="mul_base")], abilities=[ab("crimson_veil", thr=0.4, pct=0.2)],
          lore="Tightens when you are close to the end.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": BC}))
accessory("matriarchs_kiss", H, dict(paint="heart", pal="blood"), name="Matriarch's Kiss",
          attrs=[dict(attr="max_health", amount=6.0), dict(attr="armor_toughness", amount=2.0)], abilities=[ab("bloodlust", pct=0.12, cap=10)],
          rarity="epic", glint=True, obtain="Dropped by Hemorath.", lore="Cold lips. Warm blood.")
consumable("blood_vial", H, dict(paint="jar", pal="blood"), name="Blood Vial",
           abilities=[ab("heal", amount=40), ab("give_effect", effect="regeneration", ticks=300, amp=2)],
           lore="Do not read the label.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:red_dye", "minecraft:spider_eye", BC]))
consumable("crimson_elixir", H, dict(paint="bottle", pal="blood", shape="flask"), name="Crimson Elixir",
           abilities=[ab("give_effect", effect="strength", ticks=4800, amp=2, effect2="resistance", ticks2=4800, amp2=1,
                         effect3="health_boost", ticks3=4800, amp3=3)],
           lore="A toast to someone who is no longer here.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:redstone", "minecraft:nether_wart", BC]))

MB.mob("blood_bat", "Blood Bat", blood_bat(), H, hp=26, dmg=9, size=(0.7, 0.6), speed=0.5, fly=True, hover=0.8, style="blood",
       egg=(0x5a2a3a, 0xff3355), xp=8, minion_of="hemorath", follow=48,
       sounds=dict(hurt="entity.bat.hurt", death="entity.bat.death", ambient="entity.bat.ambient", pitch=0.7),
       lore="Feeds on what its mother bleeds. Feeds her in return.")

MB.boss(
    "hemorath", "Hemorath, the Crimson Matriarch", hemorath(), size=(1.6, 3.9), speed=0.34, title="Sovereign of the Hunt", style="blood",
    bar=dict(color="pink", overlay="notched_12"), minion="blood_bat", sigil="blood_sigil", xp=1700, egg=(0x7a5568, 0xff3355), kb=0.9,
    ritual=dict(dim="minecraft:overworld", time="night", sky=False, xp=0, hearts=4, items=[]),
    sounds=dict(hurt="entity.witch.hurt", death="entity.evoker.death", ambient="entity.bat.ambient", pitch=0.55),
    bag=[drop(BC, 8, 12), drop(INGOT, 8, 12), drop("matriarchs_kiss", 1, 1), drop("minecraft:netherite_ingot", 2, 3),
         drop("minecraft:experience_bottle", 24, 36), drop("crimson_scythe", 1, 1, 0.3), drop("minecraft:enchanted_golden_apple", 1, 1, 0.35)],
    phases=[
        phase("Hunger", 1.0, cd=(40, 66), attacks=[
            atk("leech", 3, windup=24, duration=60, range=[4, 24], dmg=0.16, heal=0.7, style="blood", recover=20),
            atk("pools", 2, windup=26, recover=24, count=3, radius=3.0, dmg=0.4, tick=0.25, life=200, style="blood", effect="slowness",
                effect_amp=1, range=[3, 36]),
            atk("leap", 2, windup=26, air=18, radius=6.0, dmg=1.4, style="blood", range=[6, 30]),
            atk("slam", 2, windup=28, recover=18, radius=6.5, dmg=1.5, style="blood", range=[0, 9]),
            atk("fan", 1, count=5, spread=50, dmg=0.55, speed=1.2, windup=22, recover=14, style="blood", range=[5, 40])]),
        phase("Brood", 0.66, cd=(34, 58), speed=1.12, dmg=1.1, color="purple",
              enter=[atk("summon", mob="blood_bat", count=4, cap=8, windup=30, recover=16, style="blood")],
              attacks=[
                  atk("leech", 3, windup=22, duration=70, range=[4, 24], dmg=0.18, heal=0.75, style="blood", recover=20),
                  atk("pools", 2, windup=24, recover=22, count=4, radius=3.0, dmg=0.45, tick=0.28, life=220, style="blood", effect="slowness",
                      effect_amp=1, range=[3, 36]),
                  atk("leap", 2, windup=24, air=18, radius=6.5, dmg=1.5, style="blood", range=[6, 30]),
                  atk("summon", 2, mob="blood_bat", count=3, cap=8, windup=30, recover=20, style="blood", range=[0, 50]),
                  atk("reflect", 2, windup=14, duration=80, pct=0.5, style="blood", range=[0, 12]),
                  atk("ring", 2, windup=28, recover=22, speed=0.55, max_radius=22, dmg=1.3, style="blood", range=[0, 24])]),
        phase("Blood Moon", 0.33, cd=(26, 46), speed=1.25, dmg=1.3, color="red",
              attacks=[
                  atk("leech", 3, windup=20, duration=80, range=[4, 26], dmg=0.22, heal=0.9, style="blood", recover=18),
                  atk("pools", 2, windup=22, recover=20, count=5, radius=3.0, dmg=0.5, tick=0.3, life=240, style="blood", effect="slowness",
                      effect_amp=2, range=[3, 36]),
                  atk("leap", 2, windup=22, air=16, radius=7.0, dmg=1.6, style="blood", range=[6, 30]),
                  atk("reflect", 2, windup=12, duration=90, pct=0.6, style="blood", range=[0, 14]),
                  atk("ring", 2, windup=26, recover=20, speed=0.6, max_radius=24, dmg=1.4, style="blood", range=[0, 26]),
                  atk("spiral", 2, arms=5, dmg=0.45, duration=70, interval=4, speed=0.55, turn=12, windup=20, recover=20, style="blood", range=[0, 40]),
                  atk("summon", 1, mob="blood_bat", count=3, cap=8, windup=26, recover=18, style="blood", range=[0, 50]),
                  atk("exposed", 2, windup=16, duration=70, vuln=1.8, style="blood", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 3 - Chronos, the Hourless
# =====================================================================================================================
C = "chronos"
CC = "chrono_core"
material(CC, C, dict(paint="core", pal="chrono", seed=10), name="Chrono Core", rarity="epic", glint=True,
         lore="Ticks. Do not hold it to your ear.")
add("chrono_sigil", "sigil", "hemorath", dict(paint="sigil", pal="chrono"), name="Chrono Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="chronos"), obtain="Crafted from gold blocks, clocks and a Blood Core.",
    lore="Use it on a Ritual Altar. The ritual drains five levels of experience.",
    recipe=shaped(["GKG", "KBK", "GKG"], {"G": "minecraft:gold_block", "K": "minecraft:clock", "B": BC}, category="misc"))
add("chronos_bag", "bag", C, dict(paint="bag", pal="chrono"), name="Chronos's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="chronos"), obtain="Dropped by Chronos.", lore="Right-click to open. It was already open.")

weapon("chronoblade", C, "sword",
       dict(paint="sword", pal="chrono", long=12.5, wide=1.0, guard="basket", pommel="ring", curve=-1.6, runes=True, anim="shimmer"),
       name="Chronoblade", lore="Strikes where you will have been.", repair=INGOT,
       abilities=[ab("dash_strike", dist=10, dmg=32, cd=80, style="chrono"), ab("slow_hit", ticks=60, amp=2),
                  ab("on_kill_buff", effect="haste", ticks=120, amp=2)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": CC, "S": STICK}))
weapon("epoch_greatsword", C, "greatsword",
       dict(paint="sword", pal="chrono", long=15.5, wide=2.0, guard="wing", pommel="gem", offset=-1.2, runes=True, anim="shimmer"),
       name="Epoch Greatsword", lore="One swing, and the room remembers it.", repair=INGOT,
       abilities=[ab("ground_slam", radius=9, dmg=52, cd=150, kb=1.2, style="chrono", stun=36), ab("slow_hit", ticks=80, amp=2)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": CC, "S": STICK}))
bow("chrono_bow", C, dict(paint="bow", pal="chrono", glow=True), name="Chrono Bow",
    shot=dict(dmg=36, speed=3.6, pierce=1, style="chrono", trail="soul_fire_flame", count=1, life=70, aoe=1.4, homing=True), draw=14, nograv=True,
    repair=INGOT, lore="Arrows arrive before they are loosed.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": CC, "S": "minecraft:string"}))
weapon("pocketwatch_staff", C, "staff",
       dict(paint="staff", pal="chrono", head="orb", runes=True, crown=True, anim="shimmer"),
       name="Pocketwatch Staff", lore="Right-click stops the world. Sneak and it runs backwards.", repair=INGOT,
       abilities=[ab("time_freeze", radius=12, ticks=80, cd=500, style="chrono"), ab("rewind", back=60, heal=8, cd=360)],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": CC, "S": STICK}))

armor_set("chronal", "Chronal", C, "chrono", INGOT,
          [dict(need=4, **ab("quicken", cd_mult=0.75)), dict(need=4, **ab("dodge", chance=0.18)),
           dict(need=4, **ab("time_dilation", chance=0.25, radius=7, ticks=80))],
          lore="Every plate is a little behind, or a little ahead.", special=CC, ambient="time", color=0x3EC6B0,
          style=dict(crest=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Abilities recharge faster, some hits are simply not there, and being struck slows everything near you.")

accessory("hourglass_charm", C, dict(paint="charm_shield", pal="chrono"), name="Hourglass Charm",
          abilities=[ab("quicken", cd_mult=0.85)], lore="The sand falls up if you let it.",
          recipe=shaped(["XSX", "XDX", " X "], {"X": INGOT, "D": CC, "S": "minecraft:string"}))
accessory("pocket_watch", C, dict(paint="orb", pal="chrono"), name="Pocket Watch",
          attrs=[dict(attr="attack_speed", amount=0.2, op="mul_base"), dict(attr="movement_speed", amount=0.08, op="mul_base")],
          lore="It is always exactly now.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": CC}))
accessory("chronos_heart", C, dict(paint="heart", pal="chrono"), name="Heart of Chronos",
          attrs=[dict(attr="max_health", amount=6.0), dict(attr="attack_speed", amount=0.1, op="mul_base")], abilities=[ab("quicken", cd_mult=0.8)],
          rarity="epic", glint=True, obtain="Dropped by Chronos.", lore="Skips a beat. Then catches up.")
consumable("chrono_tonic", C, dict(paint="bottle", pal="chrono", shape="flask"), name="Chrono Tonic",
           abilities=[ab("give_effect", effect="haste", ticks=4800, amp=3, effect2="speed", ticks2=4800, amp2=3, effect3="resistance",
                         ticks3=4800, amp3=1)],
           lore="Drink it twice and you may not remember the first time.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:clock", "minecraft:glowstone_dust", CC]))
consumable("time_loop_elixir", C, dict(paint="jar", pal="chrono"), name="Time Loop Elixir",
           abilities=[ab("cleanse"), ab("heal", amount=50), ab("give_effect", effect="regeneration", ticks=400, amp=3)],
           lore="You will feel better. You have felt better before.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:clock", "minecraft:golden_apple", CC]))

MB.boss(
    "chronos", "Chronos, the Hourless", chronos(), size=(1.5, 3.9), speed=0.3, title="Sovereign of Hours", fly=True, hover=1.4, style="chrono",
    keep=[7, 15], bar=dict(color="green", overlay="notched_12"), sigil="chrono_sigil", xp=2400, egg=(0xd9b24a, 0x3ec6b0), kb=1.0, death_ticks=100,
    ritual=dict(dim="", time="any", sky=False, xp=5, hearts=0, items=[]),
    sounds=dict(hurt="block.bell.use", death="block.bell.resonate", ambient="block.amethyst_block.chime", pitch=0.5),
    bag=[drop(CC, 8, 12), drop(INGOT, 8, 12), drop("chronos_heart", 1, 1), drop("minecraft:netherite_ingot", 3, 4),
         drop("minecraft:experience_bottle", 30, 40), drop("pocketwatch_staff", 1, 1, 0.3), drop("minecraft:enchanted_golden_apple", 1, 2, 0.4)],
    phases=[
        phase("Borrowed Hours", 1.0, cd=(42, 70), taken=0.55, attacks=[
            atk("rewind", 2, windup=40, back=60, dmg=0.3, style="chrono", recover=20, range=[0, 50]),
            atk("orbs", 2, count=3, dmg=0.9, speed=0.4, interval=14, style="chrono", range=[4, 50], windup=24, recover=20, trail="soul_fire_flame"),
            atk("blink", 2, windup=18, dmg=1.3, radius=3.5, style="chrono", range=[4, 40]),
            atk("pools", 2, windup=26, recover=22, count=3, radius=3.5, dmg=0.15, tick=0.15, life=220, style="chrono", effect="slowness", effect_amp=2,
                range=[3, 40]),
            atk("fan", 1, count=7, spread=60, dmg=0.5, speed=1.1, windup=22, recover=14, style="chrono", range=[5, 40]),
            atk("exposed", 2, windup=20, duration=90, vuln=2.2, style="chrono", range=[0, 60])]),
        phase("Hourless", 0.66, cd=(36, 60), speed=1.1, dmg=1.1, taken=0.55, color="blue",
              attacks=[
                  atk("stop", 3, windup=30, hold=50, count=14, radius=6.5, dmg=0.8, speed=0.9, style="chrono", recover=30, range=[0, 50]),
                  atk("rewind", 2, windup=36, back=60, dmg=0.3, style="chrono", recover=18, range=[0, 50]),
                  atk("blink", 2, windup=14, dmg=1.4, radius=3.6, style="chrono", range=[4, 40]),
                  atk("ring", 2, windup=28, recover=22, speed=0.6, max_radius=22, dmg=1.3, style="chrono", range=[0, 24]),
                  atk("spiral", 2, arms=4, dmg=0.45, duration=70, interval=4, speed=0.55, turn=13, windup=20, recover=20, style="chrono", range=[0, 40]),
                  atk("orbs", 2, count=4, dmg=0.9, speed=0.42, interval=12, style="chrono", range=[4, 50], windup=22, recover=18, trail="soul_fire_flame"),
                  atk("exposed", 2, windup=18, duration=85, vuln=2.3, style="chrono", range=[0, 60])]),
        phase("Last Second", 0.33, cd=(28, 48), speed=1.2, dmg=1.3, taken=0.5, color="white",
              attacks=[
                  atk("stop", 3, windup=26, hold=40, count=18, radius=7.0, dmg=0.85, speed=1.0, style="chrono", recover=26, range=[0, 50]),
                  atk("rewind", 2, windup=30, back=60, dmg=0.35, style="chrono", recover=16, range=[0, 50]),
                  atk("blink", 3, windup=12, dmg=1.5, radius=3.8, style="chrono", range=[4, 40]),
                  atk("spiral", 2, arms=5, dmg=0.5, duration=80, interval=4, speed=0.6, turn=14, windup=18, recover=18, style="chrono", range=[0, 40]),
                  atk("pools", 2, windup=22, recover=20, count=5, radius=3.5, dmg=0.2, tick=0.2, life=240, style="chrono", effect="slowness", effect_amp=3,
                      range=[3, 40]),
                  atk("orbs", 2, count=5, dmg=0.95, speed=0.46, interval=10, style="chrono", range=[4, 50], windup=20, recover=16, trail="soul_fire_flame"),
                  atk("exposed", 2, windup=16, duration=80, vuln=2.5, style="chrono", range=[0, 60])]),
    ])
