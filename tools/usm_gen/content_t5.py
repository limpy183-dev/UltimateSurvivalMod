"""Tier 5 - three stages, one per final Sovereign. Aetherite is the shared metal (mined in the End); each Sovereign's core makes its own gear.

  nihilus  (void)   -> tools, Voidwalker armor, gravity weapons
  solareon (light)  -> Seraph armor, beams and smiting weapons
  omnarch  (apex)   -> Apex armor, the Apex Blade and everything above it

A Lumen Flare (gate: Chronos) is the one preparation item: Nihilus eats the light, and a flare keeps you alive in the dark."""
from .items import *
from . import mobs as MB
from .mobs import atk, phase, drop
from .content_mobs import spawn, invasion
from .rigs_t5 import (nihilus, solareon, omnarch, void_spawn, null_anchor, apex_pylon, halo_blade, void_stalker, rift_weaver,
                      radiant_sentinel, prism_drone)

STICK = "minecraft:stick"
INGOT = "aetherite_ingot"
N, S, O = "nihilus", "solareon", "omnarch"
VC, RC, OC = "void_core", "radiant_core", "omni_core"

# =====================================================================================================================
# preparation: a light for the dark
# =====================================================================================================================
consumable("lumen_flare", "chronos", dict(paint="orb", pal="radiant", anim="pulse"), name="Lumen Flare", seconds=0.6,
           abilities=[ab("light_flare", radius=14, ticks=600)], stack=16,
           lore="Crack it and the dark keeps its distance for half a minute.",
           recipe=shapeless(["minecraft:glowstone_dust", "minecraft:blaze_powder", "minecraft:amethyst_shard", "minecraft:gunpowder"], count=3))

# =====================================================================================================================
# the shared metal: Aetherite, mined in the End
# =====================================================================================================================
block("aetherite_ore", "ore", dict(paint="ore_block", pal="aether", end=True, seed=51), 7.0, 9.0, tool="diamond", drop="raw_aetherite", xp=(8, 16), light=5,
      lore="Pale threads of something that was never quite metal, running through End stone.", gate=N)
block("aetherite_block", "block", dict(paint="metal_block", pal="aether"), 9.0, 12.0, tool="diamond", metal=True, lore="Quiet. Slightly too bright.",
      gate=N, recipe=shaped(["XXX", "XXX", "XXX"], {"X": INGOT}, category="building"))
ore_gen("ore_aetherite", "aetherite_ore", "aetherite_ore", size=6, count=10, min_y=10, max_y=90, dimension="end", discard=0.1)
material("raw_aetherite", N, dict(paint="raw_ore", pal="aether", seed=15), name="Raw Aetherite", lore="It is warm on one side and cold on the other.")
material(INGOT, N, dict(paint="ingot", pal="aether"), name="Aetherite Ingot", lore="Metal that has not decided what it is yet.",
         recipe=[smelt(["raw_aetherite", "aetherite_ore"], xp=5.0, time=500), shapeless(["aetherite_block"], count=9)])

# =====================================================================================================================
# stage 1 - Nihilus, Devourer of Light (void)
# =====================================================================================================================
material(VC, N, dict(paint="core", pal="void", seed=12), name="Void Core", rarity="epic", glint=True, lore="It weighs less than nothing.")
add("void_sigil", "sigil", "chronos", dict(paint="sigil", pal="void"), name="Void Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="nihilus"), obtain="Crafted from crying obsidian, Eyes of Ender and a Chrono Core.",
    lore="Use it on a Ritual Altar in the End. The ritual consumes four Eyes of Ender.",
    recipe=shaped(["OEO", "ECE", "OEO"], {"O": "minecraft:crying_obsidian", "E": "minecraft:ender_eye", "C": "chrono_core"}, category="misc"))
add("nihilus_bag", "bag", N, dict(paint="bag", pal="void"), name="Nihilus's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="nihilus"), obtain="Dropped by Nihilus.", lore="Right-click to open. It is lighter than it looks.")

tool("aetherite_pickaxe", N, "pickaxe", dict(paint="pickaxe", pal="aether", vein=True, spikes=True, bend=3.4, anim="shimmer"), name="Aetherite Pickaxe",
     traits=dict(area=3, depth=5, vein=128, smelt=1, magnet=1), repair=INGOT,
     lore="Mines a 7x7 face five deep, follows veins across the map and smelts as it goes. Sneak for a single block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": VC, "S": STICK}))
tool("aetherite_axe", N, "axe", dict(paint="axe", pal="aether", size=1.5, vein=True, double=True, anim="shimmer"), name="Aetherite Axe",
     traits=dict(fell=1, fell_leaves=1, smelt=1, magnet=1), repair=INGOT, lore="A forest is one swing. The lumber arrives as charcoal.",
     recipe=shaped(["XX", "XD", " S"], {"X": INGOT, "D": VC, "S": STICK}))
tool("aetherite_shovel", N, "shovel", dict(paint="shovel", pal="aether", point=True, vein=True, anim="shimmer"), name="Aetherite Shovel",
     traits=dict(area=3, depth=3, smelt=1, magnet=1), repair=INGOT, lore="7x7 digging, three deep.",
     recipe=shaped(["X", "D", "S"], {"X": INGOT, "D": VC, "S": STICK}))
tool("aetherite_hoe", N, "hoe", dict(paint="hoe", pal="aether", reach=5.8, vein=True, anim="shimmer"), name="Aetherite Hoe",
     traits=dict(hoe_area=6, replant=1, magnet=1), repair=INGOT, lore="Tills, harvests and replants a 13x13 field in one stroke.",
     recipe=shaped(["XX", " D", " S"], {"X": INGOT, "D": VC, "S": STICK}))

weapon("voidrender", N, "sword",
       dict(paint="sword", pal="void", long=13, wide=1.45, guard="wing", pommel="gem", edge="crystal", vein=True, anim="pulse"),
       name="Voidrender", lore="Cuts the space between two heartbeats.", repair=INGOT,
       abilities=[ab("dash_strike", dist=14, dmg=90, cd=90, style="void"), ab("gravity_pull", radius=12, dmg=40, cd=220),
                  ab("rend", pct=0.03, cap=80)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": VC, "S": STICK}))
weapon("event_horizon", N, "greatsword",
       dict(paint="sword", pal="void", long=15.5, wide=2.0, guard="wing", pommel="gem", offset=-1.2, vein=True, anim="pulse"),
       name="Event Horizon", lore="Anything that comes near it does not leave the same.", repair=INGOT,
       abilities=[ab("singularity", range=30, radius=11, core=3.4, ticks=90, dmg=50, pull=0.16, cd=400, style="void"), ab("rend", pct=0.045, cap=120),
                  ab("slow_hit", ticks=40, amp=3)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": VC, "S": STICK}))
weapon("null_dagger", N, "dagger",
       dict(paint="sword", pal="void", long=8.5, wide=0.9, guard="narrow", pommel="ring", edge="crystal", anim="pulse"),
       name="Null Dagger", lore="Not there when you look. Then there.", repair=INGOT,
       abilities=[ab("blink", dist=18, cd=60), ab("backstab", mult=3.2), ab("devour", amount=6, absorb=2)],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": VC, "S": STICK}))
weapon("singularity_staff", N, "staff",
       dict(paint="staff", pal="void", head="orb", runes=True, crown=True, anim="pulse"),
       name="Singularity Staff", lore="Point it at a problem. The problem is no longer where it was.", repair=INGOT,
       abilities=[ab("singularity", range=34, radius=12, core=3.6, ticks=100, dmg=60, pull=0.18, cd=260, style="void"),
                  ab("projectile_wave", count=7, spread=50, dmg=40, speed=1.6, style="void", cd=60, trail="reverse_portal", pierce=1)],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": VC, "S": STICK}))
bow("rift_bow", N, dict(paint="bow", pal="void", glow=True), name="Rift Bow",
    shot=dict(dmg=70, speed=3.8, pierce=6, style="void", trail="reverse_portal", count=2, life=70, aoe=2.0, homing=True), draw=15, nograv=True, repair=INGOT,
    lore="Every arrow leaves through a door that was not there.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": VC, "S": "minecraft:string"}))

armor_set("voidwalker", "Voidwalker", N, "void", INGOT,
          [dict(need=4, **ab("double_jump", jumps=3, power=0.8)), dict(need=4, **ab("dodge", chance=0.2)),
           dict(need=4, **ab("entropy_aura", radius=9, dmg=16, every=20))],
          lore="Plates of cold dark, stitched with threads of nowhere.", special=VC, ambient="void", color=0xB04BFF,
          attrs=[dict(attr="movement_speed", amount=0.05, op="mul_base"), dict(attr="max_health", amount=4.0)],
          style=dict(horns=True, crest=True, glow=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Three extra jumps, attacks that sometimes miss you entirely, and a cold that slows and wears down everything near you.")

accessory("void_shard", N, dict(paint="fragment", pal="void"), name="Void Shard",
          attrs=[dict(attr="attack_damage", amount=0.2, op="mul_base")], abilities=[ab("entropy_aura", radius=5, dmg=8, every=40)],
          lore="A sliver of the dark, kept in a pocket for company.",
          recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": VC}))
accessory("null_ring", N, dict(paint="ring", pal="void"), name="Null Ring",
          attrs=[dict(attr="movement_speed", amount=0.1, op="mul_base")], abilities=[ab("dodge", chance=0.12)],
          lore="Wearing it feels like being slightly elsewhere.",
          recipe=shaped(["X X", " D ", "X X"], {"X": INGOT, "D": VC}))
accessory("heart_of_nihilus", N, dict(paint="heart", pal="void"), name="Heart of Nihilus",
          attrs=[dict(attr="max_health", amount=12.0), dict(attr="armor_toughness", amount=2.0)], abilities=[ab("entropy_aura", radius=6, dmg=10, every=30)],
          rarity="epic", glint=True, obtain="Dropped by Nihilus.", lore="It does not beat. It waits to.")
consumable("void_tonic", N, dict(paint="bottle", pal="void", shape="flask"), name="Void Tonic",
           abilities=[ab("give_effect", effect="strength", ticks=6000, amp=3, effect2="resistance", ticks2=6000, amp2=2, effect3="jump_boost", ticks3=6000, amp3=3)],
           lore="Tastes like leaving.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:chorus_fruit", "minecraft:ender_pearl", VC]))
consumable("null_elixir", N, dict(paint="jar", pal="void"), name="Null Elixir",
           abilities=[ab("cleanse"), ab("heal", amount=70), ab("give_effect", effect="regeneration", ticks=400, amp=4, effect2="absorption", ticks2=1200, amp2=4)],
           lore="Whatever was wrong is no longer on record.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:golden_apple", "minecraft:ender_eye", VC]))

# ---- Nihilus' creatures -----------------------------------------------------------------------------------------------------------------------------------
MB.mob("void_spawn", "Void Spawn", void_spawn(), N, hp=70, dmg=24, size=(0.9, 0.8), speed=0.4, style="void", egg=(0x1c0f2e, 0xff6ad5), xp=14,
       minion_of="nihilus", follow=48,
       attacks=[atk("leap", 1, windup=16, air=14, radius=3.0, dmg=1.2, style="void", range=[4, 14], recover=14)],
       drops=[drop("minecraft:ender_pearl", 0, 1, 0.3)],
       sounds=dict(hurt="entity.silverfish.hurt", death="entity.silverfish.death", ambient="entity.silverfish.ambient", pitch=0.6),
       lore="What falls out when a rift opens. It is mostly teeth.")
MB.mob("null_anchor", "Null Anchor", null_anchor(), N, hp=320, dmg=30, armor=8, size=(1.1, 3.6), speed=0.0, kb=1.0, style="void", egg=(0x1c0f2e, 0xb04bff), xp=36,
       minion_of="nihilus", follow=48, melee=False, fire=True,
       attacks=[atk("orbs", 2, count=2, dmg=0.8, speed=0.4, interval=14, style="void", range=[0, 50], windup=24, recover=24, trail="reverse_portal"),
                atk("fan", 1, count=3, spread=30, dmg=0.6, speed=1.3, style="void", range=[0, 50], windup=22, recover=26, trail="reverse_portal")],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.amethyst_cluster.break", pitch=0.5),
       lore="Pins the Devourer to the world. Break it and it falls.")

# ---- the regular creatures of the End and the bright side ---------------------------------------------------------------------------------------------------
MB.mob("void_stalker", "Void Stalker", void_stalker(), N, hp=240, dmg=34, armor=8, size=(0.7, 2.9), speed=0.36, style="void", egg=(0x120a1c, 0xff6ad5), xp=44,
       attacks=[atk("blink", 2, windup=12, dmg=1.3, radius=3.0, style="void", range=[4, 30]),
                atk("leech", 1, windup=18, duration=40, beam=10, range=[2, 12], dmg=0.12, heal=0.8, style="void", recover=18)],
       drops=[drop("minecraft:ender_pearl", 1, 3), drop("raw_aetherite", 0, 2, 0.35), drop("void_core", 0, 1, 0.02)],
       spawn=spawn(dim="end", weight=24, group=(1, 3), min_tier=4, gate="chronos"),
       sounds=dict(hurt="entity.enderman.hurt", death="entity.enderman.death", ambient="entity.enderman.ambient", pitch=0.6),
       lore="Tall, patient, and always standing somewhere you have just looked away from.")
MB.mob("rift_weaver", "Rift Weaver", rift_weaver(), N, hp=170, dmg=30, size=(0.9, 0.9), speed=0.32, fly=True, hover=2.0, keep=[8, 18], style="void", melee=False,
       egg=(0x1c0f2e, 0xffffff), xp=40,
       attacks=[atk("rifts", 2, count=1, cap=3, radius=2.4, windup=24, recover=40, dmg=0.6, range=[4, 30], mob="void_spawn", style="void"),
                atk("orbs", 2, count=2, dmg=0.85, speed=0.42, interval=14, style="void", windup=22, recover=26, range=[5, 40], trail="reverse_portal")],
       drops=[drop("minecraft:chorus_fruit", 1, 3), drop("raw_aetherite", 0, 2, 0.3)],
       spawn=spawn(dim="end", weight=12, group=(1, 2), min_tier=4, gate="chronos"),
       sounds=dict(hurt="entity.endermite.hurt", death="entity.endermite.death", ambient="entity.endermite.ambient", pitch=0.5),
       lore="Sews doors into the air and then wonders who opens them.")
MB.mob("radiant_sentinel", "Radiant Sentinel", radiant_sentinel(), N, hp=420, dmg=40, armor=16, size=(1.8, 3.4), speed=0.2, kb=0.8, style="radiant", egg=(0xd8c890, 0xffffff), xp=60,
       attacks=[atk("beam", 2, windup=26, duration=50, beam=22, sweep=80, width=0.9, dmg=0.5, step=4, low=True, style="radiant", range=[4, 26], recover=24),
                atk("slam", 2, windup=26, recover=18, radius=5.5, dmg=1.3, style="radiant", range=[0, 8], kb=1.1),
                atk("reflect", 1, windup=12, duration=60, pct=0.4, style="radiant", range=[0, 30], recover=14, move=False)],
       drops=[drop("minecraft:gold_ingot", 2, 5), drop("minecraft:glowstone_dust", 1, 4), drop("radiant_core", 0, 1, 0.02)],
       spawn=spawn(weight=8, group=(1, 1), min_tier=5, gate="nihilus"),
       sounds=dict(hurt="entity.iron_golem.hurt", death="entity.iron_golem.death", ambient="block.beacon.ambient", pitch=0.5),
       lore="Built to guard a morning. Nobody told it the morning ended.")
MB.mob("prism_drone", "Prism Drone", prism_drone(), N, hp=130, dmg=28, size=(0.8, 0.8), speed=0.36, fly=True, hover=4.0, keep=[8, 20], style="radiant", melee=False,
       egg=(0xe8d890, 0x8fd9ff), xp=36,
       attacks=[atk("fan", 3, count=3, spread=22, dmg=0.6, speed=1.5, windup=20, recover=26, style="radiant", trail="end_rod", range=[6, 36]),
                atk("flash", 1, windup=36, blind=60, dmg=0.3, cone=70, beam=30, style="radiant", range=[8, 28], recover=30)],
       drops=[drop("minecraft:glowstone_dust", 1, 3), drop("minecraft:amethyst_shard", 0, 2, 0.5), drop("minecraft:end_rod", 0, 2, 0.3)],
       spawn=spawn(weight=10, group=(1, 2), min_tier=5, gate="nihilus", y=(70, 320)),
       sounds=dict(hurt="entity.allay.hurt", death="entity.allay.death", ambient="entity.allay.ambient_without_item", pitch=1.5),
       lore="A lamp that learned geometry. Do not look straight at it.")

MB.boss(
    "nihilus", "Nihilus, Devourer of Light", nihilus(), size=(2.0, 3.4), speed=0.3, scale=1.5, title="Sovereign of the Absence", fly=True, hover=2.5,
    style="void", keep=[10, 22], bar=dict(color="purple", overlay="notched_12"), minion="void_spawn", sigil="void_sigil", xp=3400,
    egg=(0x1c0f2e, 0xb04bff), kb=1.0, death_ticks=130, spawn_ticks=90, enrage=720,
    ritual=dict(dim="minecraft:the_end", time="any", sky=False, xp=0, hearts=0, items=[dict(item="minecraft:ender_eye", count=4)]),
    sounds=dict(hurt="entity.enderman.hurt", death="entity.ender_dragon.death", ambient="entity.enderman.stare", pitch=0.4),
    bag=[drop(VC, 8, 12), drop(INGOT, 10, 14), drop("heart_of_nihilus", 1, 1), drop("minecraft:netherite_ingot", 3, 5),
         drop("minecraft:experience_bottle", 30, 40), drop("event_horizon", 1, 1, 0.3), drop("minecraft:enchanted_golden_apple", 1, 2, 0.4)],
    phases=[
        phase("Appetite", 1.0, cd=(46, 74), attacks=[
            atk("blackhole", 2, windup=36, radius=12, core=3.0, life=100, pull=0.11, blast=0.9, tick=0.3, style="void", range=[0, 60]),
            atk("rifts", 2, windup=28, count=3, cap=5, radius=2.8, dmg=0.6, mob="void_spawn", style="void", range=[0, 50]),
            atk("orbs", 2, count=3, dmg=0.8, speed=0.42, interval=14, style="void", range=[4, 50], windup=24, recover=20, trail="reverse_portal"),
            atk("fan", 2, count=5, spread=36, dmg=0.55, speed=1.2, windup=22, recover=14, style="void", trail="reverse_portal", range=[6, 50]),
            atk("perch", 1, windup=36, stun=80, vuln=1.7, radius=5.5, style="void", range=[0, 60])]),
        phase("Eclipse", 0.66, cd=(38, 62), speed=1.1, dmg=1.1, color="red",
              enter=[atk("gloom", windup=30, duration=300, radius=28, dmg=0.35, interval=20, style="void")],
              attacks=[
                  atk("blackhole", 2, windup=34, count=2, radius=12, core=3.2, life=110, pull=0.12, blast=1.0, tick=0.32, style="void", range=[0, 60]),
                  atk("rifts", 2, windup=26, count=4, cap=6, radius=2.8, dmg=0.65, mob="void_spawn", style="void", range=[0, 50]),
                  atk("gloom", 1, windup=30, duration=240, radius=28, dmg=0.35, interval=20, style="void", range=[0, 60]),
                  atk("spiral", 2, arms=4, dmg=0.45, duration=60, interval=4, speed=0.55, windup=20, recover=20, style="void", trail="reverse_portal", range=[0, 50]),
                  atk("charge", 2, windup=30, dist=24, speed=1.1, stun=40, vuln=1.6, dmg=1.5, width=2.2, style="void", range=[8, 40], wall_stun=0),
                  atk("orbs", 2, count=4, dmg=0.85, speed=0.45, interval=12, style="void", range=[4, 50], windup=22, recover=18, trail="reverse_portal"),
                  atk("cage", 2, windup=34, close=56, radius=8.5, gaps=2, dmg=1.2, style="void", range=[0, 40]),
                  atk("summon", 1, mob="void_spawn", count=3, cap=6, windup=30, recover=20, style="void", range=[0, 60]),
                  atk("exposed", 1, windup=16, duration=80, vuln=1.8, style="void", range=[0, 60])]),
        phase("Oblivion", 0.33, cd=(30, 52), speed=1.2, dmg=1.3, color="white",
              nodes=dict(mob="null_anchor", count=3, radius=11),
              attacks=[
                  atk("blackhole", 3, windup=32, count=2, radius=13, core=3.4, life=120, pull=0.13, blast=1.1, tick=0.34, style="void", range=[0, 60]),
                  atk("cage", 2, windup=30, close=48, radius=9, gaps=2, gap_arc=46, dmg=1.3, style="void", range=[0, 40]),
                  atk("gust", 1, windup=26, radius=18, dmg=0.3, kb=2.0, pull=True, style="void", range=[6, 50], recover=18),
                  atk("spiral", 2, arms=5, dmg=0.5, duration=70, interval=4, speed=0.58, turn=12, windup=20, recover=20, style="void", trail="reverse_portal", range=[0, 50]),
                  atk("orbs", 2, count=5, dmg=0.9, speed=0.48, interval=10, style="void", range=[4, 50], windup=20, recover=16, trail="reverse_portal"),
                  atk("rifts", 1, windup=24, count=5, cap=7, radius=2.8, dmg=0.7, mob="void_spawn", style="void", range=[0, 50]),
                  atk("perch", 2, windup=30, stun=90, vuln=1.9, radius=6, style="void", range=[0, 60]),
                  atk("fan", 1, count=7, spread=50, dmg=0.6, speed=1.3, volleys=2, interval=9, windup=18, recover=14, style="void", trail="reverse_portal",
                      range=[6, 50])]),
    ])

# =====================================================================================================================
# stage 2 - Solareon, the Radiant Seraph (light)
# =====================================================================================================================
material(RC, S, dict(paint="core", pal="radiant", seed=14), name="Radiant Core", rarity="epic", glint=True, lore="Too bright to look at. You look anyway.")
add("radiant_sigil", "sigil", N, dict(paint="sigil", pal="radiant"), name="Radiant Sigil", rarity="rare", stack=16, glint=True,
    extra=dict(boss="solareon"), obtain="Crafted from sea lanterns, gold blocks and a Void Core.",
    lore="Use it on a Ritual Altar in the Overworld at midday, under open sky. The ritual costs twelve levels of experience.",
    recipe=shaped(["LGL", "GCG", "LGL"], {"L": "minecraft:sea_lantern", "G": "minecraft:gold_block", "C": VC}, category="misc"))
add("solareon_bag", "bag", S, dict(paint="bag", pal="radiant"), name="Solareon's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="solareon"), obtain="Dropped by Solareon.", lore="Right-click to open. Squint.")

weapon("solaris_edge", S, "sword",
       dict(paint="sword", pal="radiant", long=13.5, wide=1.2, guard="crown", pommel="gem", edge="crystal", runes=True, anim="shimmer"),
       name="Solaris Edge", lore="A morning that has never ended.", repair=INGOT,
       abilities=[ab("sunbeam", range=40, dmg=110, width=1.3, cd=120, style="radiant"), ab("solar_flare", radius=12, dmg=60, blind=80, cd=240),
                  ab("smite", mult=1.6), ab("sear", dmg=30, glow=100)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": RC, "S": STICK}))
weapon("dawnbreaker", S, "hammer",
       dict(paint="axe", pal="radiant", size=1.7, double=True, vein=True, anim="shimmer"),
       name="Dawnbreaker", lore="Swing it at night. Watch the night reconsider.", repair=INGOT,
       abilities=[ab("supernova", charge=24, radius=16, dmg=260, cd=600, style="radiant"),
                  ab("ground_slam", radius=9, dmg=120, cd=160, kb=1.6, style="radiant", stun=50), ab("smite", mult=1.5)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": INGOT, "D": RC, "S": STICK}))
weapon("seraph_spear", S, "spear",
       dict(paint="sword", pal="radiant", long=16, wide=0.9, guard="narrow", pommel="spike", edge="crystal", anim="shimmer"),
       name="Seraph Spear", lore="Thrown in a line that is already behind you.", repair=INGOT,
       abilities=[ab("dash_strike", dist=16, dmg=100, cd=70, style="radiant"), ab("sunbeam", range=50, dmg=90, width=0.9, cd=100, style="radiant"),
                  ab("on_kill_heal", amount=8)],
       recipe=shaped(["  X", " SD", "S  "], {"X": INGOT, "D": RC, "S": STICK}))
weapon("prism_staff", S, "staff",
       dict(paint="staff", pal="radiant", head="orb", runes=True, crown=True, anim="shimmer"),
       name="Prism Staff", lore="Splits one light into several, and all of them aimed.", repair=INGOT,
       abilities=[ab("sunbeam", range=48, dmg=130, width=1.5, cd=100, style="radiant"),
                  ab("projectile_wave", count=9, spread=70, dmg=50, speed=1.8, style="radiant", cd=70, trail="end_rod")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": RC, "S": STICK}))
bow("daybreak_bow", S, dict(paint="bow", pal="radiant", glow=True), name="Daybreak Bow",
    shot=dict(dmg=95, speed=4.2, pierce=10, style="radiant", trail="end_rod", count=1, life=80, aoe=2.6), draw=14, nograv=True, repair=INGOT,
    lore="Arrows arrive as sunrise.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": RC, "S": "minecraft:string"}))

armor_set("seraph", "Seraph", S, "radiant", INGOT,
          [dict(need=4, **ab("halo_aura", radius=8, dmg=12, every=20)), dict(need=4, **ab("damage_reduction", pct=0.15)),
           dict(need=4, **ab("regen_ooc", amount=3, delay=60))],
          lore="Gold that remembers being daylight.", special=RC, ambient="light", color=0xFFF3B0,
          attrs=[dict(attr="max_health", amount=4.0)],
          style=dict(crest=True, glow=True, wings=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="You shed light that exposes and burns nearby enemies, hits land softer, and wounds close quickly when you are left alone.")

accessory("sun_pendant", S, dict(paint="amulet", pal="radiant"), name="Sun Pendant",
          attrs=[dict(attr="max_health", amount=10.0)], abilities=[ab("halo_aura", radius=5, dmg=6, every=40)],
          lore="Warm, even in a cave.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": RC}))
accessory("prism_ring", S, dict(paint="ring", pal="radiant"), name="Prism Ring",
          attrs=[dict(attr="attack_speed", amount=0.2, op="mul_base")], abilities=[ab("quicken", cd_mult=0.88)],
          lore="Everything it touches arrives in colours.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": RC}))
accessory("heart_of_solareon", S, dict(paint="heart", pal="radiant"), name="Heart of Solareon",
          attrs=[dict(attr="max_health", amount=14.0), dict(attr="armor_toughness", amount=2.0)], abilities=[ab("halo_aura", radius=6, dmg=10, every=30)],
          rarity="epic", glint=True, obtain="Dropped by Solareon.", lore="Still warm from the last time it was the sun.")
consumable("radiant_elixir", S, dict(paint="bottle", pal="radiant", shape="flask"), name="Radiant Elixir",
           abilities=[ab("give_effect", effect="strength", ticks=6000, amp=3, effect2="regeneration", ticks2=6000, amp2=2, effect3="absorption", ticks3=6000, amp3=4)],
           lore="Glows. Do not hold it up to the light - it is the light.",
           recipe=shapeless(["minecraft:glass_bottle", "minecraft:glowstone_dust", "minecraft:golden_carrot", RC]))
consumable("dawn_bread", S, dict(paint="biscuit", pal="radiant"), name="Dawn Bread", anim="eat", seconds=2.0, nutrition=14, saturation=1.6,
           abilities=[ab("heal", amount=80), ab("give_effect", effect="absorption", ticks=2400, amp=4, effect2="speed", ticks2=1200, amp2=2)],
           lore="Baked at first light. Eaten at last.", recipe=shapeless(["minecraft:bread", "minecraft:honey_bottle", "minecraft:golden_apple", RC]))

MB.mob("halo_blade", "Halo Blade", halo_blade(), S, hp=120, dmg=36, size=(0.7, 0.5), speed=0.45, fly=True, hover=1.0, keep=[0, 0], style="radiant", melee=False,
       egg=(0xfffbe0, 0xe8c25a), xp=26, minion_of="solareon", follow=48,
       attacks=[atk("charge", 1, windup=14, dist=18, speed=1.25, stun=18, vuln=1.3, dmg=1.2, width=1.2, style="radiant", range=[4, 28], wall_stun=0)],
       sounds=dict(hurt="entity.vex.hurt", death="entity.vex.death", ambient="entity.vex.ambient", pitch=1.4),
       lore="A sword that stopped waiting for a hand.")

MB.boss(
    "solareon", "Solareon, the Radiant Seraph", solareon(), size=(1.4, 3.6), speed=0.32, scale=1.35, title="Sovereign of Dawn", fly=True, hover=3.5,
    style="radiant", keep=[10, 24], bar=dict(color="white", overlay="notched_20"), minion="halo_blade", sigil="radiant_sigil", xp=4800,
    egg=(0xe8c25a, 0xffffff), kb=1.0, death_ticks=130, spawn_ticks=90, enrage=720,
    ritual=dict(dim="minecraft:overworld", time="noon", sky=True, xp=12, hearts=0, items=[]),
    sounds=dict(hurt="entity.vex.hurt", death="block.beacon.deactivate", ambient="block.beacon.ambient", pitch=0.5),
    bag=[drop(RC, 8, 12), drop(INGOT, 10, 14), drop("heart_of_solareon", 1, 1), drop("minecraft:netherite_ingot", 4, 6),
         drop("minecraft:experience_bottle", 36, 48), drop("solaris_edge", 1, 1, 0.3), drop("minecraft:enchanted_golden_apple", 2, 3, 0.4)],
    phases=[
        phase("Dawn", 1.0, cd=(44, 70), attacks=[
            atk("beam", 3, windup=34, duration=70, beam=44, sweep=130, width=1.1, dmg=0.55, step=3, style="radiant", range=[6, 60]),
            atk("fan", 2, count=5, spread=40, dmg=0.55, speed=1.4, windup=22, recover=14, style="radiant", trail="end_rod", range=[6, 50]),
            atk("flash", 1, windup=40, blind=80, dmg=0.3, cone=80, beam=60, style="radiant", range=[0, 60]),
            atk("pillars", 2, windup=34, count=6, radius=2.4, dmg=1.3, delay=26, interval=8, style="radiant", range=[0, 60]),
            atk("exposed", 1, windup=18, duration=80, vuln=1.8, style="radiant", range=[0, 60])]),
        phase("Zenith", 0.66, cd=(36, 60), speed=1.1, dmg=1.1, color="yellow",
              enter=[atk("summon", mob="halo_blade", count=3, cap=6, windup=30, recover=16, style="radiant")],
              attacks=[
                  atk("beam", 3, windup=32, duration=90, arms=4, spin=2.2, beam=40, width=1.0, dmg=0.5, step=4, style="radiant", range=[0, 60]),
                  atk("supernova", 2, windup=80, radius=36, dmg=2.4, exhaust=90, vuln=2.0, style="radiant", range=[0, 60]),
                  atk("summon", 2, mob="halo_blade", count=2, cap=6, windup=30, recover=18, style="radiant", range=[0, 60]),
                  atk("reflect", 1, windup=14, duration=80, pct=0.5, style="radiant", range=[0, 14]),
                  atk("flash", 2, windup=36, blind=90, dmg=0.35, cone=80, beam=60, style="radiant", range=[0, 60]),
                  atk("sunder", 1, windup=40, length=46, width=3.0, dmg=1.8, style="radiant", range=[4, 60]),
                  atk("orbs", 1, count=4, dmg=0.85, speed=0.46, interval=12, style="radiant", range=[4, 50], windup=22, recover=18, trail="end_rod")]),
        phase("Judgement", 0.33, cd=(28, 48), speed=1.2, dmg=1.3, color="red",
              attacks=[
                  atk("beam", 3, windup=28, duration=100, arms=6, spin=2.6, beam=42, width=1.0, dmg=0.55, step=4, style="radiant", range=[0, 60]),
                  atk("supernova", 3, windup=70, radius=40, dmg=2.7, exhaust=80, vuln=2.1, style="radiant", range=[0, 60]),
                  atk("sunder", 2, windup=34, length=52, width=3.4, dmg=2.0, style="radiant", range=[4, 60]),
                  atk("pillars", 2, windup=30, count=9, radius=2.4, dmg=1.4, delay=24, interval=6, style="radiant", range=[0, 60]),
                  atk("blink", 1, windup=12, dmg=1.4, radius=3.8, style="radiant", range=[4, 40]),
                  atk("reflect", 1, windup=12, duration=90, pct=0.6, style="radiant", range=[0, 14]),
                  atk("summon", 1, mob="halo_blade", count=3, cap=6, windup=26, recover=16, style="radiant", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 3 - the Omnarch, Sundered God (everything)
# =====================================================================================================================
material(OC, O, dict(paint="core", pal="apex", seed=16), name="Omni Core", rarity="epic", glint=True, lore="Both halves of it, pressed together.")
add("omni_sigil", "sigil", S, dict(paint="sigil", pal="apex"), name="Omni Sigil", rarity="epic", stack=16, glint=True,
    extra=dict(boss="omnarch"), obtain="Crafted from one core of every Sovereign, netherite and a Nether Star.",
    lore="Use it on a Ritual Altar. The ritual costs thirty levels of experience and eight hearts of health. There is no second attempt until it is finished.",
    recipe=shaped(["ESB", "CVR", "NGN"], {"E": "ember_core", "S": "storm_core", "B": "blood_core", "C": "chrono_core", "V": VC, "R": RC,
                                         "N": "minecraft:netherite_ingot", "G": "minecraft:nether_star"}, category="misc"))
add("omnarch_bag", "bag", O, dict(paint="bag", pal="apex"), name="The Omnarch's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="omnarch"), obtain="Dropped by the Omnarch.", lore="Right-click to open. It is both full and empty.")

tool("apex_pickaxe", O, "pickaxe", dict(paint="pickaxe", pal="apex", vein=True, spikes=True, bend=3.6, anim="prism"), name="Apex Pickaxe",
     traits=dict(area=4, depth=6, vein=256, smelt=1, magnet=1), repair=INGOT,
     lore="A 9x9 face, six deep, and every vein it touches. Sneak for a single block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": OC, "S": STICK}))

weapon("apex_blade", O, "sword",
       dict(paint="sword", pal="apex", long=14, wide=1.5, guard="wing", pommel="gem", edge="flame", vein=True, runes=True, anim="prism"),
       name="Apex Blade", lore="Everything, sharpened to a single line.", repair=INGOT,
       abilities=[ab("cleave", radius=14, arc=150, dmg=320, cd=140, style="apex"), ab("dash_strike", dist=18, dmg=180, cd=60, style="apex"),
                  ab("rend", pct=0.06, cap=200), ab("lifesteal", pct=0.1)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": OC, "S": STICK}))
weapon("worldsplitter", O, "greatsword",
       dict(paint="sword", pal="apex", long=16, wide=2.1, guard="wing", pommel="gem", offset=-1.2, vein=True, runes=True, anim="prism"),
       name="Worldsplitter", lore="It was made to end an argument between two halves of the same thing.", repair=INGOT,
       abilities=[ab("ground_slam", radius=12, dmg=260, cd=120, kb=2.0, style="apex", stun=60), ab("cleave", radius=16, arc=200, dmg=360, cd=180, style="apex"),
                  ab("execute", mult=2.2, threshold=0.35)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": OC, "S": STICK}))
bow("apex_bow", O, dict(paint="bow", pal="apex", glow=True), name="Apex Bow",
    shot=dict(dmg=150, speed=4.6, pierce=20, style="apex", trail="enchant", count=3, life=90, aoe=3.2, homing=True), draw=14, nograv=True, repair=INGOT,
    lore="The target is chosen before the string is drawn.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": OC, "S": "minecraft:string"}))
weapon("weavers_staff", O, "staff",
       dict(paint="staff", pal="apex", head="orb", runes=True, crown=True, anim="prism"),
       name="Weaver's Staff", lore="Pulls on the seventh thread. Everything else moves.", repair=INGOT,
       abilities=[ab("apex_nova", radius=18, dmg=360, heal=40, cd=360, style="apex"),
                  ab("call_lightning", strikes=5, dmg=140, radius=5, range=40, cd=160, style="apex")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": OC, "S": STICK}))

armor_set("apex", "Apex", O, "apex", INGOT,
          [dict(need=4, **ab("damage_reduction", pct=0.2)), dict(need=4, **ab("quicken", cd_mult=0.6)),
           dict(need=4, **ab("omni_aura", radius=11, dmg=34, heal=3, every=20))],
          lore="Half light, half dark, and nothing at all in between.", special=OC, ambient="apex", color=0xFF6AD5,
          attrs=[dict(attr="max_health", amount=6.0), dict(attr="movement_speed", amount=0.05, op="mul_base")],
          style=dict(horns=True, crest=True, glow=True, spikes=True), layer=dict(visor="full", glow=True, emblem="gem"),
          set_desc="Hits land a fifth softer, every ability recharges forty percent faster, and everything near you is worn down while you recover.")

accessory("heart_of_the_omnarch", O, dict(paint="heart", pal="apex"), name="Heart of the Omnarch",
          attrs=[dict(attr="max_health", amount=20.0), dict(attr="attack_damage", amount=0.2, op="mul_base"), dict(attr="armor_toughness", amount=3.0)],
          abilities=[ab("omni_aura", radius=7, dmg=14, heal=1, every=30)], rarity="epic", glint=True, obtain="Dropped by the Omnarch.",
          lore="It beats in two rhythms. They almost agree.")
accessory("weavers_thread", O, dict(paint="feather", pal="apex"), name="Weaver's Thread",
          attrs=[dict(attr="movement_speed", amount=0.12, op="mul_base"), dict(attr="attack_speed", amount=0.15, op="mul_base")], abilities=[ab("quicken", cd_mult=0.85)],
          lore="One end is in your hand. You have not found the other.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": OC}))
accessory("apex_signet", O, dict(paint="ring", pal="apex"), name="Apex Signet",
          attrs=[dict(attr="armor_toughness", amount=4.0), dict(attr="max_health", amount=8.0)], abilities=[ab("damage_reduction", pct=0.08)],
          lore="Stamped with a name that has been scratched out and rewritten.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": OC}))
consumable("apex_elixir", O, dict(paint="bottle", pal="apex", shape="flask"), name="Apex Elixir",
           abilities=[ab("give_effect", effect="strength", ticks=6000, amp=4, effect2="resistance", ticks2=6000, amp2=3, effect3="regeneration", ticks3=6000, amp3=3)],
           lore="Both halves, drunk at once.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:nether_star", "minecraft:enchanted_golden_apple", OC]))
consumable("omni_feast", O, dict(paint="biscuit", pal="apex"), name="Omni Feast", anim="eat", seconds=2.4, nutrition=20, saturation=2.0,
           abilities=[ab("heal", amount=100), ab("give_effect", effect="absorption", ticks=3600, amp=6, effect2="saturation", ticks2=20, amp2=0)],
           lore="A meal for someone who has stopped being hungry for anything but more.",
           recipe=shapeless(["minecraft:cooked_beef", "minecraft:golden_carrot", "minecraft:honey_bottle", "minecraft:enchanted_golden_apple", OC]))

MB.mob("apex_pylon", "Apex Pylon", apex_pylon(), O, hp=520, dmg=50, armor=10, size=(1.2, 3.9), speed=0.0, kb=1.0, style="apex", egg=(0x2b1b3d, 0xf3e6b0), xp=60,
       minion_of="omnarch", follow=48, melee=False, fire=True,
       attacks=[atk("beam", 2, windup=30, duration=50, beam=34, sweep=90, width=0.9, dmg=0.45, step=4, style="apex", range=[0, 48], recover=26),
                atk("orbs", 2, count=3, dmg=0.8, speed=0.42, interval=14, style="apex", range=[0, 50], windup=24, recover=24, trail="enchant")],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.beacon.deactivate", pitch=0.4),
       lore="Holds the two halves of the Omnarch a little too close together.")

MB.boss(
    "omnarch", "The Omnarch, Sundered God", omnarch(), size=(1.8, 3.8), speed=0.3, scale=1.6, title="The Sundered God", style="apex",
    keep=None, bar=dict(color="pink", overlay="notched_20"), minion="halo_blade", sigil="omni_sigil", xp=7500,
    egg=(0x7a3fc0, 0xfffbe0), kb=1.0, death_ticks=160, spawn_ticks=110, enrage=900,
    ritual=dict(dim="", time="any", sky=False, xp=30, hearts=8, items=[]),
    sounds=dict(hurt="entity.wither.hurt", death="entity.wither.death", ambient="entity.wither.ambient", pitch=0.4),
    bag=[drop(OC, 10, 14), drop(INGOT, 14, 20), drop("heart_of_the_omnarch", 1, 1), drop("minecraft:netherite_ingot", 6, 8),
         drop("minecraft:experience_bottle", 48, 64), drop("apex_blade", 1, 1, 0.35), drop("worldsplitter", 1, 1, 0.2),
         drop("minecraft:enchanted_golden_apple", 3, 4, 0.5), drop("minecraft:nether_star", 1, 1, 0.5)],
    phases=[
        phase("Echo of Embers", 1.0, cd=(46, 70), attacks=[
            atk("meteors", 2, windup=30, count=8, delay=26, radius=3.2, dmg=1.2, area=9, aimed=3, style="ember", range=[0, 60]),
            atk("lightning", 2, windup=30, count=7, radius=2.4, dmg=1.3, delay=27, interval=8, area=11, aimed=3, style="storm", range=[0, 60]),
            atk("leech", 1, windup=22, duration=70, beam=22, dmg=0.16, heal=0.7, style="blood", range=[4, 24], recover=20),
            atk("slam", 2, windup=28, recover=18, radius=7.5, dmg=1.5, kb=1.4, style="apex", range=[0, 10]),
            atk("charge", 1, windup=34, dist=22, speed=1.05, stun=50, vuln=1.6, dmg=1.5, width=2.4, style="apex", range=[8, 32], wall_stun=40),
            atk("fan", 1, count=7, spread=60, dmg=0.55, speed=1.2, windup=22, recover=14, style="apex", trail="enchant", range=[5, 50])]),
        phase("Echo of Hours", 0.70, cd=(40, 62), speed=1.08, dmg=1.1, color="purple",
              attacks=[
                  atk("stop", 2, windup=30, hold=50, count=16, radius=7.0, dmg=0.8, speed=0.9, style="chrono", recover=30, range=[0, 50]),
                  atk("rewind", 1, windup=36, back=60, dmg=0.3, style="chrono", recover=18, range=[0, 50]),
                  atk("blackhole", 2, windup=34, radius=12, core=3.0, life=100, pull=0.11, blast=0.9, tick=0.3, style="void", range=[0, 60]),
                  atk("beam", 2, windup=32, duration=70, sweep=120, low=True, beam=36, width=1.0, dmg=0.55, step=3, style="radiant", range=[0, 40]),
                  atk("supernova", 1, windup=80, radius=34, dmg=2.4, exhaust=90, vuln=1.9, style="radiant", range=[0, 60]),
                  atk("sunder", 2, windup=40, length=44, width=3.2, dmg=1.8, style="apex", range=[4, 60]),
                  atk("exposed", 1, windup=18, duration=80, vuln=1.9, style="apex", range=[0, 60])]),
        phase("Sundered", 0.40, cd=(34, 54), speed=1.15, dmg=1.2, color="red",
              enter=[atk("summon", mob="halo_blade", count=3, cap=6, windup=30, recover=16, style="apex")],
              attacks=[
                  atk("beam", 3, windup=30, duration=90, arms=3, spin=2.0, low=True, beam=34, width=1.0, dmg=0.55, step=4, style="apex", range=[0, 40]),
                  atk("cage", 2, windup=32, close=50, radius=9, gaps=3, gap_arc=40, dmg=1.3, style="void", range=[0, 40]),
                  atk("blackhole", 2, windup=32, count=2, radius=13, core=3.4, life=110, pull=0.12, blast=1.0, tick=0.32, style="void", range=[0, 60]),
                  atk("supernova", 2, windup=76, radius=38, dmg=2.5, exhaust=85, vuln=2.0, style="radiant", range=[0, 60]),
                  atk("sunder", 2, windup=36, length=48, width=3.2, dmg=1.9, style="apex", range=[4, 60]),
                  atk("flash", 1, windup=36, blind=90, dmg=0.35, cone=80, beam=60, style="radiant", range=[0, 60]),
                  atk("rifts", 1, windup=26, count=3, cap=6, radius=2.8, dmg=0.65, mob="void_spawn", style="void", range=[0, 50])]),
        phase("Apex", 0.15, cd=(26, 44), speed=1.25, dmg=1.4, color="white",
              nodes=dict(mob="apex_pylon", count=4, radius=13),
              enter=[atk("pools", windup=40, recover=20, count=12, radius=4.0, near=20, dmg=0.6, tick=0.3, life=400, style="apex", range=[0, 60])],
              attacks=[
                  atk("blackhole", 2, windup=30, count=2, radius=13, core=3.4, life=110, pull=0.12, blast=1.1, tick=0.34, style="void", range=[0, 60]),
                  atk("beam", 2, windup=28, duration=100, arms=5, spin=2.8, low=True, beam=34, width=1.0, dmg=0.6, step=4, style="apex", range=[0, 40]),
                  atk("supernova", 3, windup=70, radius=40, dmg=2.8, exhaust=80, vuln=2.1, style="radiant", range=[0, 60]),
                  atk("sunder", 2, windup=32, length=52, width=3.4, dmg=2.0, style="apex", range=[4, 60]),
                  atk("stop", 2, windup=26, hold=40, count=20, radius=7.5, dmg=0.85, speed=1.0, style="chrono", recover=26, range=[0, 50]),
                  atk("cage", 1, windup=30, close=48, radius=9, gaps=3, gap_arc=36, dmg=1.4, style="void", range=[0, 40]),
                  atk("lightning", 1, windup=26, count=9, radius=2.4, dmg=1.4, delay=26, interval=6, area=12, aimed=3, style="storm", range=[0, 60]),
                  atk("meteors", 1, windup=26, count=10, delay=24, radius=3.2, dmg=1.3, area=10, aimed=3, style="ember", range=[0, 60]),
                  atk("exposed", 1, windup=16, duration=70, vuln=2.0, style="apex", range=[0, 60])]),
    ])

# =====================================================================================================================
# invasion wave for tier 5
# =====================================================================================================================
invasion(5, [("void_stalker", 3, 4), ("rift_weaver", 2, 3), ("radiant_sentinel", 1, 2), ("prism_drone", 2, 3), ("void_spawn", 3, 4)], xp=700, title="The Unmaking",
         reward=[drop("minecraft:experience_bottle", 14, 24), drop(INGOT, 3, 8), drop(VC, 0, 1, 0.25), drop(RC, 0, 1, 0.25), drop(OC, 0, 1, 0.1)])
