"""Tier 6 (Transcendence) - four stages, one per Sovereign of the older threads. Eternium is the shared metal: the creatures of Tier 6 shed shards, four shards press
into an ingot, and each Sovereign's core turns ingots into its own gear. This tier is authored at its final strength - the global power pass skips it.

  boreus   (frost)   -> tools, Rimeguard armor, weapons that freeze and then shatter
  morbane  (plague)  -> Blightbound armor, weapons that infect: the plague jumps from corpse to corpse
  astraeus (cosmic)  -> Starborn armor (flight), falling stars, beams from orbit
  eternus  (eternal) -> Eternal armor (a death that does not take), the Threadcutter and the weapons above it"""
from .items import *
from . import mobs as MB
from .mobs import atk, phase, drop
from .content_mobs import spawn, invasion
from .rigs_t6 import (boreus, frost_wraith, glacial_brute, rime_anchor, morbane, spore_pod, blight_crawler, mycelid, blight_heart,
                      astraeus, star_sprite, comet_hound, astral_node, eternus, loom_spindle)

STICK = "minecraft:stick"
INGOT = "eternium_ingot"
SHARD = "eternium_shard"
B, M, A, E = "boreus", "morbane", "astraeus", "eternus"
FC, BC, SC, WC = "frost_core", "blight_core", "star_core", "weave_core"

# =====================================================================================================================
# the shared metal and the things every Tier 6 hero wants
# =====================================================================================================================
material(SHARD, B, dict(paint="fragment", pal="eternal", seed=21), name="Eternium Shard", rarity="epic", glint=True,
         lore="A sliver of a thread nobody was meant to cut.")
material(INGOT, B, dict(paint="ingot", pal="eternal", rune=True), name="Eternium Ingot", rarity="epic", glint=True,
         lore="Four shards, pressed until they stopped arguing.",
         recipe=shaped(["XX", "XX"], {"X": SHARD}, category="misc"))
consumable("titan_heart", B, dict(paint="crystal_heart", pal="eternal"), name="Titan Heart", abilities=[ab("perm_health", max=20)], rarity="epic", seconds=1.6, glint=True,
           lore="Permanently adds one heart, up to twenty in all. It does not stop beating.",
           recipe=shaped(["EXE", "XHX", "EXE"], {"E": INGOT, "X": "minecraft:netherite_ingot", "H": "heart_crystal"}))

tool("eternium_pickaxe", B, "pickaxe", dict(paint="pickaxe", pal="eternal", vein=True, spikes=True, bend=3.8, anim="prism"), name="Eternium Pickaxe",
     traits=dict(area=6, depth=8, vein=512, smelt=1, magnet=1), repair=INGOT,
     lore="Mines a 13x13 face eight deep, follows every vein it touches and smelts as it goes. Sneak for a single block.",
     recipe=shaped(["XXX", " D ", " S "], {"X": INGOT, "D": FC, "S": STICK}))
tool("eternium_axe", B, "axe", dict(paint="axe", pal="eternal", size=1.6, vein=True, double=True, anim="prism"), name="Eternium Axe",
     traits=dict(fell=1, fell_leaves=1, smelt=1, magnet=1), repair=INGOT, lore="A forest is half a swing. The lumber arrives as charcoal.",
     recipe=shaped(["XX", "XD", " S"], {"X": INGOT, "D": FC, "S": STICK}))
tool("eternium_shovel", B, "shovel", dict(paint="shovel", pal="eternal", point=True, vein=True, anim="prism"), name="Eternium Shovel",
     traits=dict(area=5, depth=5, smelt=1, magnet=1), repair=INGOT, lore="11x11 digging, five deep.",
     recipe=shaped(["X", "D", "S"], {"X": INGOT, "D": FC, "S": STICK}))
tool("eternium_hoe", B, "hoe", dict(paint="hoe", pal="eternal", reach=6.2, vein=True, anim="prism"), name="Eternium Hoe",
     traits=dict(hoe_area=9, replant=1, magnet=1), repair=INGOT, lore="Tills, harvests and replants a 19x19 field in one stroke.",
     recipe=shaped(["XX", " D", " S"], {"X": INGOT, "D": FC, "S": STICK}))

# =====================================================================================================================
# stage 1 - Boreus, the Rime Tyrant (frost)
# =====================================================================================================================
material(FC, B, dict(paint="core", pal="frost", seed=31), name="Frost Core", rarity="epic", glint=True, lore="Cold enough to be heard.")
add("rime_sigil", "sigil", "omnarch", dict(paint="sigil", pal="frost"), name="Rime Sigil", rarity="epic", stack=16, glint=True,
    extra=dict(boss="boreus"), obtain="Crafted from Blue Ice, Aetherite and an Omni Core.",
    lore="Use it on a Ritual Altar. The ritual costs thirty levels of experience and sixteen blocks of Blue Ice.",
    recipe=shaped(["BAB", "ACA", "BAB"], {"B": "minecraft:blue_ice", "A": "aetherite_ingot", "C": "omni_core"}, category="misc"))
add("boreus_bag", "bag", B, dict(paint="bag", pal="frost"), name="Boreus's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="boreus"), obtain="Dropped by Boreus.", lore="Right-click to open. It is very cold.")

weapon("rimebrand", B, "sword",
       dict(paint="sword", pal="frost", long=14, wide=1.4, guard="wing", pommel="gem", edge="crystal", vein=True, anim="shimmer"),
       name="Rimebrand", lore="Struck once, it never stops being cold.", repair=INGOT,
       abilities=[ab("absolute_zero", radius=18, dmg=480, ticks=100, cd=220, style="frost"), ab("dash_strike", dist=26, dmg=620, cd=50, style="frost"),
                  ab("frostbite", chance=0.45, ticks=60), ab("shatter", mult=3.5), ab("lifesteal", pct=0.1)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": FC, "S": STICK}))
weapon("glacier_maul", B, "hammer",
       dict(paint="axe", pal="frost", size=1.8, double=True, vein=True, anim="shimmer"),
       name="Glacier Maul", lore="Hit the ground and the ground remembers winter.", repair=INGOT,
       abilities=[ab("ground_slam", radius=15, dmg=1100, cd=110, kb=2.4, style="frost", stun=100),
                  ab("projectile_wave", count=13, spread=110, dmg=320, speed=2.6, style="frost", cd=50, trail="snowflake"),
                  ab("frostbite", chance=0.6, ticks=80), ab("knockback_burst", power=2.0)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": INGOT, "D": FC, "S": STICK}))
weapon("hoarfrost_fang", B, "dagger",
       dict(paint="sword", pal="frost", long=9, wide=0.9, guard="narrow", pommel="ring", edge="crystal", anim="shimmer"),
       name="Hoarfrost Fang", lore="It does not cut. It stops what it touches.", repair=INGOT,
       abilities=[ab("blink", dist=28, cd=36), ab("backstab", mult=5.0), ab("frostbite", chance=0.5, ticks=60), ab("shatter", mult=4.0)],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": FC, "S": STICK}))
weapon("blizzard_staff", B, "staff",
       dict(paint="staff", pal="frost", head="orb", runes=True, crown=True, anim="shimmer"),
       name="Blizzard Staff", lore="Weather, on a stick.", repair=INGOT,
       abilities=[ab("blizzard", range=44, radius=13, ticks=140, dmg=110, cd=220, style="frost"),
                  ab("projectile_wave", count=17, spread=100, dmg=260, speed=2.8, style="frost", cd=36, trail="snowflake")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": FC, "S": STICK}))
bow("winters_bow", B, dict(paint="bow", pal="frost", glow=True), name="Winter's Bow",
    shot=dict(dmg=760, speed=5.4, pierce=40, style="frost", trail="snowflake", count=4, spread=12, life=100, aoe=4.0, homing=True), draw=9, nograv=True, repair=INGOT,
    lore="Every arrow is a small winter.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": FC, "S": "minecraft:string"}))

armor_set("rimeguard", "Rimeguard", B, "frost", INGOT,
          [dict(need=4, **ab("frost_aura", radius=13, dmg=110, every=16, freeze=0.25)), dict(need=4, **ab("rime_shell", pct=0.15, chance=0.6, ticks=60)),
           dict(need=4, **ab("regen_ooc", amount=14, delay=40))],
          lore="Plates of winter, stitched with the memory of a snowfall.", special=FC, ambient="frost", color=0x7FD8FF,
          attrs=[dict(attr="max_health", amount=8.0), dict(attr="movement_speed", amount=0.04, op="mul_base")],
          style=dict(horns=True, crest=True, glow=True, spikes=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="You carry a cold that slows and wears down everything near you - and freezes some of it solid. Attackers are frozen where they stand, and wounds close quickly when you are left alone.")

accessory("heart_of_boreus", B, dict(paint="heart", pal="frost"), name="Heart of Boreus",
          attrs=[dict(attr="max_health", amount=40.0), dict(attr="armor_toughness", amount=4.0)], abilities=[ab("frost_aura", radius=9, dmg=70, every=16, freeze=0.15)],
          rarity="epic", glint=True, obtain="Dropped by Boreus.", lore="It has not beaten in an age. It does not need to.")
accessory("glacier_charm", B, dict(paint="charm_shield", pal="frost"), name="Glacier Charm",
          attrs=[dict(attr="armor", amount=12.0), dict(attr="movement_speed", amount=0.2, op="mul_base")], abilities=[ab("thorns", pct=0.6, shock=3)],
          lore="Cold to hold, warm to be near - nobody can say why.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": FC}))
consumable("frostfire_elixir", B, dict(paint="bottle", pal="frost", shape="flask"), name="Frostfire Elixir",
           abilities=[ab("heal", amount=300), ab("give_effect", effect="resistance", ticks=12000, amp=4, effect2="regeneration", ticks2=1200, amp2=4,
                                                 effect3="absorption", ticks3=6000, amp3=9)],
           lore="Burns cold. Heals hot.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:blue_ice", "minecraft:golden_apple", FC]))
consumable("rime_cake", B, dict(paint="biscuit", pal="frost"), name="Rime Cake", anim="eat", seconds=1.6, nutrition=20, saturation=2.0,
           abilities=[ab("heal", amount=400), ab("give_effect", effect="strength", ticks=12000, amp=5, effect2="speed", ticks2=6000, amp2=3)],
           lore="Baked in a freezer, which should not work.", recipe=shapeless(["minecraft:cake", "minecraft:snow_block", "minecraft:honey_bottle", FC]))

# ---- Boreus's creatures -----------------------------------------------------------------------------------------------------------------------------------
MB.mob("rime_wraith", "Rime Wraith", frost_wraith(), B, hp=900, dmg=90, size=(0.9, 1.7), speed=0.38, fly=True, hover=3.5, keep=[8, 22], style="frost", melee=False,
       egg=(0xcfeeff, 0x7fe8ff), xp=60, minion_of="boreus", follow=48,
       attacks=[atk("fan", 3, count=5, spread=36, dmg=0.6, speed=1.7, style="frost", trail="snowflake", windup=18, recover=22, range=[6, 40]),
                atk("blink", 1, windup=12, dmg=1.3, radius=3.2, style="frost", range=[4, 28])],
       drops=[drop(SHARD, 0, 2, 0.6), drop("minecraft:blue_ice", 1, 3)],
       sounds=dict(hurt="entity.stray.hurt", death="entity.stray.death", ambient="entity.stray.ambient", pitch=0.7),
       lore="A winter that learned to hunt.")
MB.mob("rime_anchor", "Rime Anchor", rime_anchor(), B, hp=2600, dmg=95, armor=14, size=(1.1, 3.6), speed=0.0, kb=1.0, style="frost", egg=(0x3d86c4, 0x7fe8ff), xp=120,
       minion_of="boreus", follow=48, melee=False, fire=True,
       attacks=[atk("fan", 2, count=3, spread=30, dmg=0.6, speed=1.5, style="frost", trail="snowflake", range=[0, 50], windup=22, recover=26),
                atk("orbs", 2, count=2, dmg=0.8, speed=0.45, interval=14, style="frost", range=[0, 50], windup=24, recover=24, trail="snowflake")],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.glass.break", pitch=0.5),
       lore="Pins the winter to the world. Break it and the cold has nowhere to stand.")
MB.mob("glacial_brute", "Glacial Brute", glacial_brute(), B, hp=3400, dmg=130, armor=22, size=(1.7, 3.3), speed=0.22, kb=0.9, style="frost", egg=(0xcfeeff, 0x3d86c4), xp=160,
       attacks=[atk("slam", 2, windup=26, recover=18, radius=6.0, dmg=1.4, style="frost", range=[0, 8]),
                atk("icicles", 2, windup=30, lines=6, length=20, width=1.4, dmg=0.9, style="frost", range=[0, 24], recover=24),
                atk("charge", 1, windup=26, dist=16, speed=1.0, stun=34, vuln=1.5, dmg=1.4, width=2.0, style="frost", range=[6, 22], wall_stun=30)],
       drops=[drop(SHARD, 1, 3), drop("minecraft:blue_ice", 2, 6), drop(FC, 0, 1, 0.02)],
       spawn=spawn(weight=8, group=(1, 1), min_tier=5, gate="omnarch"),
       sounds=dict(hurt="entity.polar_bear.hurt", death="entity.polar_bear.death", ambient="entity.polar_bear.ambient", pitch=0.5),
       lore="A glacier that took one step and kept going.")

MB.boss(
    "boreus", "Boreus, the Rime Tyrant", boreus(), size=(2.2, 4.4), speed=0.32, scale=1.5, title="Sovereign of Winter", style="frost",
    keep=None, bar=dict(color="blue", overlay="notched_12"), minion="rime_wraith", sigil="rime_sigil", xp=11000,
    egg=(0xcfeeff, 0x7fe8ff), kb=1.0, death_ticks=140, spawn_ticks=90, enrage=720,
    ritual=dict(dim="minecraft:overworld", time="any", sky=False, xp=30, hearts=0, items=[dict(item="minecraft:blue_ice", count=16)]),
    sounds=dict(hurt="entity.polar_bear.hurt", death="entity.wither.death", ambient="entity.polar_bear.warning", pitch=0.4),
    bag=[drop(FC, 10, 14), drop(INGOT, 16, 22), drop("heart_of_boreus", 1, 1), drop("minecraft:netherite_ingot", 6, 8), drop("titan_heart", 1, 1, 0.5),
         drop("minecraft:experience_bottle", 56, 72), drop("rimebrand", 1, 1, 0.35), drop("glacier_maul", 1, 1, 0.25), drop("minecraft:enchanted_golden_apple", 3, 4, 0.5)],
    phases=[
        phase("Rime", 1.0, cd=(40, 64), attacks=[
            atk("icicles", 3, windup=34, lines=6, length=30, speed=1.5, width=1.5, dmg=0.9, style="frost", range=[0, 60]),
            atk("fan", 2, count=7, spread=50, dmg=0.5, speed=1.5, windup=22, recover=14, style="frost", trail="snowflake", range=[5, 50]),
            atk("slam", 2, windup=28, recover=18, radius=8.0, dmg=1.4, kb=1.4, style="frost", range=[0, 11]),
            atk("charge", 1, windup=34, dist=24, speed=1.2, stun=40, vuln=1.7, dmg=1.5, width=2.4, style="frost", range=[8, 36], wall_stun=40),
            atk("exposed", 1, windup=16, duration=80, vuln=1.8, style="frost", range=[0, 60])]),
        phase("Blizzard", 0.72, cd=(34, 56), speed=1.1, dmg=1.1, color="white",
              enter=[atk("whiteout", windup=30, duration=200, radius=40, dmg=0.35, grace=30, style="frost")],
              attacks=[
                  atk("whiteout", 1, windup=30, duration=180, radius=40, dmg=0.35, grace=30, style="frost", range=[0, 60]),
                  atk("icicles", 3, windup=32, lines=8, length=32, speed=1.6, width=1.5, waves=2, gap=16, dmg=1.0, style="frost", range=[0, 60]),
                  atk("glaciate", 2, windup=34, radius=11, delay=30, shards=3, dmg=1.0, shard_dmg=1.2, style="frost", range=[0, 14]),
                  atk("summon", 2, mob="rime_wraith", count=3, cap=6, windup=30, recover=18, style="frost", range=[0, 60]),
                  atk("spiral", 1, arms=5, dmg=0.5, duration=70, interval=4, speed=0.55, windup=20, recover=20, style="frost", trail="snowflake", range=[0, 50]),
                  atk("ring", 1, windup=28, recover=20, speed=0.6, max_radius=24, dmg=1.2, style="frost", range=[0, 20])]),
        phase("Permafrost", 0.45, cd=(28, 48), speed=1.15, dmg=1.2, color="blue",
              nodes=dict(mob="rime_anchor", count=3, radius=12),
              attacks=[
                  atk("glaciate", 3, windup=32, radius=13, delay=28, shards=4, dmg=1.1, shard_dmg=1.3, style="frost", range=[0, 16]),
                  atk("icicles", 2, windup=30, lines=10, length=34, speed=1.7, width=1.5, waves=2, gap=14, dmg=1.1, style="frost", range=[0, 60]),
                  atk("meteors", 2, windup=28, count=10, delay=24, radius=3.2, dmg=1.3, area=11, aimed=3, style="frost", range=[0, 60]),
                  atk("cage", 2, windup=32, close=52, radius=9, gaps=2, gap_arc=44, dmg=1.2, style="frost", range=[0, 40]),
                  atk("whiteout", 1, windup=28, duration=160, radius=40, dmg=0.4, grace=26, style="frost", range=[0, 60]),
                  atk("summon", 1, mob="rime_wraith", count=2, cap=6, windup=28, recover=18, style="frost", range=[0, 60]),
                  atk("charge", 1, windup=30, dist=24, speed=1.3, stun=36, vuln=1.7, dmg=1.6, width=2.4, style="frost", range=[8, 36], wall_stun=40)]),
        phase("Absolute Zero", 0.18, cd=(24, 42), speed=1.25, dmg=1.4, color="white",
              enter=[atk("pools", windup=40, recover=20, count=10, radius=4.0, near=18, dmg=0.5, tick=0.3, life=400, style="frost", effect="slowness", effect_amp=2)],
              attacks=[
                  atk("glaciate", 3, windup=30, radius=15, delay=26, shards=5, dmg=1.2, shard_dmg=1.4, style="frost", range=[0, 18]),
                  atk("icicles", 3, windup=28, lines=12, length=36, speed=1.8, width=1.6, waves=3, gap=12, dmg=1.2, style="frost", range=[0, 60]),
                  atk("whiteout", 2, windup=26, duration=170, radius=40, dmg=0.45, grace=20, style="frost", range=[0, 60]),
                  atk("spiral", 2, arms=6, dmg=0.55, duration=80, interval=4, speed=0.6, turn=12, windup=20, recover=20, style="frost", trail="snowflake", range=[0, 50]),
                  atk("ring", 1, windup=26, recover=18, speed=0.7, max_radius=26, dmg=1.4, style="frost", range=[0, 20]),
                  atk("meteors", 1, windup=26, count=14, delay=22, radius=3.2, dmg=1.4, area=12, aimed=4, style="frost", range=[0, 60]),
                  atk("exposed", 1, windup=14, duration=70, vuln=2.0, style="frost", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 2 - Morbane, the Blightlord (plague)
# =====================================================================================================================
material(BC, M, dict(paint="core", pal="plague", seed=32), name="Blight Core", rarity="epic", glint=True, lore="It is warm, and it is still growing.")
add("blight_sigil", "sigil", B, dict(paint="sigil", pal="plague"), name="Blight Sigil", rarity="epic", stack=16, glint=True,
    extra=dict(boss="morbane"), obtain="Crafted from Fermented Spider Eyes, Eternium and a Frost Core.",
    lore="Use it on a Ritual Altar at night. The ritual costs six hearts of health and eight Fermented Spider Eyes.",
    recipe=shaped(["FIF", "ICI", "FIF"], {"F": "minecraft:fermented_spider_eye", "I": INGOT, "C": FC}, category="misc"))
add("morbane_bag", "bag", M, dict(paint="bag", pal="plague"), name="Morbane's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="morbane"), obtain="Dropped by Morbane.", lore="Right-click to open. It moves a little.")

weapon("plaguebringer", M, "scythe",
       dict(paint="sword", pal="plague", long=14, wide=1.15, guard="narrow", pommel="spike", curve=-2.4, edge="serrated", vein=True, anim="pulse"),
       name="Plaguebringer", lore="It leaves nothing behind but the idea of an ending.", repair=INGOT,
       abilities=[ab("plague_burst", radius=15, dmg=520, cd=250, clouds=6, style="plague"), ab("toxic_cloud", range=40, radius=9, ticks=200, dmg=110, cd=150, style="plague"),
                  ab("contagion", dmg=180, radius=11, ticks=200), ab("lifesteal", pct=0.15)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": BC, "S": STICK}))
weapon("rotbone_greatsword", M, "greatsword",
       dict(paint="sword", pal="plague", long=16, wide=2.1, guard="wing", pommel="gem", offset=-1.2, edge="serrated", vein=True, anim="pulse"),
       name="Rotbone Greatsword", lore="Carved from something that was still alive when it was carved.", repair=INGOT,
       abilities=[ab("ground_slam", radius=14, dmg=1000, cd=130, kb=2.0, style="plague", stun=60), ab("dash_strike", dist=22, dmg=700, cd=60, style="plague"),
                  ab("contagion", dmg=260, radius=12, ticks=200), ab("rend", pct=0.07, cap=500)],
       recipe=shaped(["XDX", "XDX", " S "], {"X": INGOT, "D": BC, "S": STICK}))
weapon("venom_fang", M, "dagger",
       dict(paint="sword", pal="plague", long=9, wide=0.9, guard="narrow", pommel="ring", edge="serrated", anim="pulse"),
       name="Venom Fang", lore="Quick. Then quicker, once the venom takes.", repair=INGOT,
       abilities=[ab("blink", dist=28, cd=36), ab("backstab", mult=5.0), ab("contagion", dmg=200, radius=10, ticks=240), ab("bleed", ticks=200, amp=2)],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": BC, "S": STICK}))
weapon("spore_staff", M, "staff",
       dict(paint="staff", pal="plague", head="orb", runes=True, crown=True, anim="pulse"),
       name="Spore Staff", lore="A garden you would not want to walk through.", repair=INGOT,
       abilities=[ab("toxic_cloud", range=46, radius=10, ticks=240, dmg=130, cd=140, style="plague"),
                  ab("projectile_wave", count=15, spread=110, dmg=240, speed=2.2, style="plague", cd=40, trail="spore_blossom_air")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": BC, "S": STICK}))
bow("miasma_bow", M, dict(paint="bow", pal="plague", glow=True), name="Miasma Bow",
    shot=dict(dmg=820, speed=5.0, pierce=40, style="plague", trail="spore_blossom_air", count=5, spread=16, life=100, aoe=4.5, homing=True), draw=9, nograv=True, repair=INGOT,
    lore="The arrow arrives. So does everything it was carrying.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": BC, "S": "minecraft:string"}))

armor_set("blightbound", "Blightbound", M, "plague", INGOT,
          [dict(need=4, **ab("blight_aura", radius=13, dmg=130, heal=6, every=16)), dict(need=4, **ab("damage_reduction", pct=0.25)),
           dict(need=4, **ab("regen_ooc", amount=16, delay=40))],
          lore="Plates that grew into one another and agreed to stay.", special=BC, ambient="plague", color=0xA6E22E,
          attrs=[dict(attr="max_health", amount=8.0), dict(attr="attack_speed", amount=0.06, op="mul_base")],
          style=dict(horns=True, crest=True, glow=True, spikes=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="Everything near you rots, and every rotting thing feeds you. Hits land a quarter softer, and wounds close quickly when you are left alone.")

accessory("heart_of_morbane", M, dict(paint="heart", pal="plague"), name="Heart of Morbane",
          attrs=[dict(attr="max_health", amount=44.0), dict(attr="armor_toughness", amount=4.0)], abilities=[ab("blight_aura", radius=9, dmg=80, heal=3, every=16)],
          rarity="epic", glint=True, obtain="Dropped by Morbane.", lore="It beats in a rhythm that is not its own.")
accessory("spore_sac", M, dict(paint="amulet", pal="plague"), name="Spore Sac",
          attrs=[dict(attr="attack_speed", amount=0.3, op="mul_base"), dict(attr="max_health", amount=12.0)], abilities=[ab("night_vision")],
          lore="Do not squeeze it. Everyone squeezes it.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": BC}))
consumable("blight_tonic", M, dict(paint="bottle", pal="plague", shape="flask"), name="Blight Tonic",
           abilities=[ab("cleanse"), ab("heal", amount=300), ab("give_effect", effect="strength", ticks=12000, amp=5, effect2="resistance", ticks2=12000, amp2=4,
                                                               effect3="regeneration", ticks3=1200, amp3=4)],
           lore="Tastes like the last thing a patient ever tasted.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:fermented_spider_eye", "minecraft:golden_apple", BC]))
consumable("fungal_stew", M, dict(paint="biscuit", pal="plague"), name="Fungal Stew", anim="eat", seconds=1.6, nutrition=20, saturation=2.0,
           abilities=[ab("heal", amount=500), ab("give_effect", effect="absorption", ticks=6000, amp=9, effect2="speed", ticks2=6000, amp2=3)],
           lore="It is perfectly safe. It has been tested. Not by you.", recipe=shapeless(["minecraft:mushroom_stew", "minecraft:brown_mushroom", "minecraft:honey_bottle", BC]))

MB.mob("spore_pod", "Spore Pod", spore_pod(), M, hp=500, dmg=80, size=(0.7, 0.7), speed=0.4, style="plague", egg=(0x6a8a2a, 0xc6ff3a), xp=40, minion_of="morbane", follow=48,
       attacks=[atk("leap", 1, windup=14, air=12, radius=3.2, dmg=1.2, style="plague", range=[3, 14], recover=12)],
       drops=[drop(SHARD, 0, 1, 0.5), drop("minecraft:spore_blossom", 0, 1, 0.1)],
       sounds=dict(hurt="entity.slime.hurt", death="entity.slime.death", ambient="entity.slime.squish", pitch=0.6),
       lore="Small, round, and absolutely certain it has a purpose.")
MB.mob("blight_heart", "Blight Heart", blight_heart(), M, hp=2800, dmg=100, armor=14, size=(1.1, 3.6), speed=0.0, kb=1.0, style="plague", egg=(0x5b6e2a, 0xc6ff3a), xp=120,
       minion_of="morbane", follow=48, melee=False, fire=True,
       attacks=[atk("spores", 2, count=3, radius=3.0, windup=26, recover=34, flight=24, dmg=0.5, life=160, style="plague", range=[0, 50]),
                atk("orbs", 2, count=2, dmg=0.8, speed=0.4, interval=14, style="plague", range=[0, 50], windup=24, recover=24, trail="spore_blossom_air")],
       sounds=dict(hurt="block.slime_block.hit", death="block.slime_block.break", pitch=0.5),
       lore="It keeps the plague beating. Stop it and the plague has no rhythm.")
MB.mob("blight_crawler", "Blight Crawler", blight_crawler(), M, hp=1800, dmg=110, armor=12, size=(1.3, 1.1), speed=0.34, kb=0.4, style="plague", egg=(0x4a5e22, 0xc6ff3a), xp=90,
       attacks=[atk("leap", 2, windup=16, air=14, radius=3.6, dmg=1.3, style="plague", range=[4, 16], recover=14),
                atk("spores", 1, count=3, radius=3.0, windup=24, recover=30, flight=22, dmg=0.5, life=160, style="plague", range=[6, 24])],
       drops=[drop(SHARD, 1, 2), drop("minecraft:fermented_spider_eye", 0, 2, 0.5), drop(BC, 0, 1, 0.02)],
       spawn=spawn(weight=10, group=(2, 3), min_tier=5, gate="omnarch", night=True),
       sounds=dict(hurt="entity.spider.hurt", death="entity.spider.death", ambient="entity.spider.ambient", pitch=0.55),
       lore="Eight legs would have been enough. Something added more.")
MB.mob("mycelid", "Mycelid", mycelid(), M, hp=1300, dmg=95, size=(1.0, 1.0), speed=0.3, fly=True, hover=3.0, keep=[8, 20], style="plague", melee=False,
       egg=(0x8a2f4f, 0xc6ff3a), xp=80,
       attacks=[atk("fan", 2, count=5, spread=40, dmg=0.6, speed=1.3, style="plague", trail="spore_blossom_air", windup=20, recover=26, range=[6, 36]),
                atk("spores", 1, count=3, radius=3.0, windup=26, recover=34, flight=26, dmg=0.5, life=160, style="plague", range=[8, 30])],
       drops=[drop(SHARD, 1, 2), drop("minecraft:red_mushroom", 1, 3)],
       spawn=spawn(weight=8, group=(1, 2), min_tier=5, gate="omnarch", y=(60, 320)),
       sounds=dict(hurt="entity.allay.hurt", death="entity.allay.death", ambient="entity.allay.ambient_without_item", pitch=0.5),
       lore="A mushroom that was told it could go anywhere.")

MB.boss(
    "morbane", "Morbane, the Blightlord", morbane(), size=(2.4, 4.1), speed=0.28, scale=1.5, title="Sovereign of Rot", style="plague",
    keep=None, bar=dict(color="green", overlay="notched_12"), minion="spore_pod", sigil="blight_sigil", xp=15500,
    egg=(0x5b6e2a, 0xc6ff3a), kb=1.0, death_ticks=140, spawn_ticks=90, enrage=720,
    ritual=dict(dim="minecraft:overworld", time="night", sky=False, xp=0, hearts=6, items=[dict(item="minecraft:fermented_spider_eye", count=8)]),
    sounds=dict(hurt="entity.slime.hurt", death="entity.wither.death", ambient="entity.zombie_villager.ambient", pitch=0.4),
    bag=[drop(BC, 10, 14), drop(INGOT, 18, 24), drop("heart_of_morbane", 1, 1), drop("minecraft:netherite_ingot", 6, 8), drop("titan_heart", 1, 1, 0.5),
         drop("minecraft:experience_bottle", 56, 72), drop("plaguebringer", 1, 1, 0.35), drop("rotbone_greatsword", 1, 1, 0.25), drop("minecraft:enchanted_golden_apple", 3, 4, 0.5)],
    phases=[
        phase("Spore", 1.0, cd=(42, 66), attacks=[
            atk("spores", 3, windup=30, count=5, radius=3.6, flight=28, life=240, dmg=0.5, style="plague", range=[0, 60]),
            atk("pools", 2, windup=28, recover=22, count=3, radius=3.4, near=7, dmg=0.5, tick=0.3, life=240, style="plague", effect="poison", effect_amp=2, range=[0, 50]),
            atk("fan", 2, count=7, spread=56, dmg=0.55, speed=1.4, windup=22, recover=16, style="plague", trail="spore_blossom_air", range=[5, 50]),
            atk("leap", 2, windup=18, air=16, radius=4.8, dmg=1.5, style="plague", range=[4, 24], recover=16),
            atk("slam", 1, windup=28, recover=18, radius=7.5, dmg=1.4, kb=1.3, style="plague", range=[0, 10]),
            atk("exposed", 1, windup=16, duration=80, vuln=1.8, style="plague", range=[0, 60])]),
        phase("Rot", 0.72, cd=(36, 58), speed=1.08, dmg=1.1, color="yellow",
              enter=[atk("infest", windup=30, fuse=100, pods=2, mob="spore_pod", cap=8, dmg=1.1, radius=6.0, style="plague")],
              attacks=[
                  atk("infest", 2, windup=26, fuse=100, pods=2, mob="spore_pod", cap=8, dmg=1.1, radius=6.0, style="plague", range=[0, 60]),
                  atk("spores", 3, windup=28, count=7, radius=3.8, flight=26, life=240, dmg=0.55, style="plague", range=[0, 60]),
                  atk("leech", 2, windup=22, duration=70, beam=24, dmg=0.18, heal=0.8, style="plague", range=[4, 26], recover=20),
                  atk("summon", 2, mob="blight_crawler", count=2, cap=5, windup=30, recover=18, style="plague", range=[0, 60]),
                  atk("beam", 1, windup=30, duration=80, arms=4, spin=2.0, low=True, beam=30, width=1.1, dmg=0.5, step=4, style="plague", range=[0, 36]),
                  atk("spiral", 1, arms=5, dmg=0.5, duration=70, interval=4, speed=0.55, windup=20, recover=20, style="plague", trail="spore_blossom_air", range=[0, 50])]),
        phase("Pandemic", 0.45, cd=(30, 50), speed=1.15, dmg=1.2, color="green",
              nodes=dict(mob="blight_heart", count=3, radius=12),
              attacks=[
                  atk("infest", 2, windup=24, fuse=80, pods=3, mob="spore_pod", cap=9, dmg=1.2, radius=6.5, style="plague", range=[0, 60]),
                  atk("spores", 3, windup=26, count=9, radius=4.0, flight=24, life=260, dmg=0.6, style="plague", range=[0, 60]),
                  atk("leech", 1, windup=20, duration=80, beam=26, dmg=0.2, heal=0.9, style="plague", range=[4, 28], recover=18),
                  atk("ring", 1, windup=28, recover=20, speed=0.6, max_radius=24, dmg=1.3, style="plague", range=[0, 20]),
                  atk("leap", 2, windup=16, air=16, radius=5.2, dmg=1.6, style="plague", range=[4, 26], recover=14),
                  atk("beam", 2, windup=28, duration=90, arms=6, spin=2.4, low=True, beam=32, width=1.1, dmg=0.55, step=4, style="plague", range=[0, 36]),
                  atk("summon", 1, mob="mycelid", count=2, cap=5, windup=30, recover=18, style="plague", range=[0, 60])]),
        phase("Extinction", 0.18, cd=(24, 42), speed=1.25, dmg=1.4, color="red",
              enter=[atk("pools", windup=40, recover=20, count=12, radius=4.2, near=20, dmg=0.55, tick=0.3, life=400, style="plague", effect="wither", effect_amp=1)],
              attacks=[
                  atk("infest", 3, windup=22, fuse=70, pods=3, mob="spore_pod", cap=10, dmg=1.3, radius=7.0, style="plague", range=[0, 60]),
                  atk("spores", 3, windup=24, count=11, radius=4.2, flight=22, life=280, dmg=0.65, style="plague", range=[0, 60]),
                  atk("beam", 3, windup=26, duration=100, arms=7, spin=2.8, low=True, beam=34, width=1.1, dmg=0.6, step=4, style="plague", range=[0, 36]),
                  atk("leech", 2, windup=18, duration=80, beam=28, dmg=0.22, heal=1.0, style="plague", range=[4, 30], recover=16),
                  atk("charge", 1, windup=28, dist=24, speed=1.3, stun=34, vuln=1.7, dmg=1.7, width=2.6, style="plague", range=[8, 36], wall_stun=36),
                  atk("exposed", 1, windup=14, duration=70, vuln=2.0, style="plague", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 3 - Astraeus, the Star-Eater (cosmic)
# =====================================================================================================================
material(SC, A, dict(paint="core", pal="cosmic", seed=33), name="Star Core", rarity="epic", glint=True, lore="A star, folded small, still burning.")
add("astral_sigil", "sigil", M, dict(paint="sigil", pal="cosmic"), name="Astral Sigil", rarity="epic", stack=16, glint=True,
    extra=dict(boss="astraeus"), obtain="Crafted from Amethyst Blocks, Eternium and a Blight Core.",
    lore="Use it on a Ritual Altar in the End. The ritual costs forty levels of experience and two End Crystals.",
    recipe=shaped(["AIA", "ICI", "AIA"], {"A": "minecraft:amethyst_block", "I": INGOT, "C": BC}, category="misc"))
add("astraeus_bag", "bag", A, dict(paint="bag", pal="cosmic"), name="Astraeus's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="astraeus"), obtain="Dropped by Astraeus.", lore="Right-click to open. It is mostly empty, and very large.")

weapon("comet_blade", A, "sword",
       dict(paint="sword", pal="cosmic", long=14.5, wide=1.3, guard="crown", pommel="gem", edge="flame", vein=True, runes=True, anim="pulse"),
       name="Comet Blade", lore="Falls the way a comet does: late, and all at once.", repair=INGOT,
       abilities=[ab("meteor_shower", range=48, radius=15, count=22, ticks=90, blast=5, dmg=520, cd=250, style="cosmic"), ab("dash_strike", dist=30, dmg=760, cd=40, style="cosmic"),
                  ab("starfall", chance=0.35, dmg=360, radius=5), ab("execute", mult=3.0, threshold=0.4)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": SC, "S": STICK}))
weapon("orrery_maul", A, "hammer",
       dict(paint="axe", pal="cosmic", size=1.8, double=True, vein=True, anim="pulse"),
       name="Orrery Maul", lore="A small solar system on a handle. The planets are the weight.", repair=INGOT,
       abilities=[ab("orbital_strike", range=54, radius=10, delay=20, dmg=2800, cd=300, style="cosmic"),
                  ab("ground_slam", radius=17, dmg=1200, cd=100, kb=2.6, style="cosmic", stun=80), ab("starfall", chance=0.3, dmg=420, radius=5)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": INGOT, "D": SC, "S": STICK}))
weapon("starlance", A, "spear",
       dict(paint="sword", pal="cosmic", long=17, wide=0.9, guard="narrow", pommel="spike", edge="crystal", anim="pulse"),
       name="Starlance", lore="Thrown at a point the star left some time ago.", repair=INGOT,
       abilities=[ab("dash_strike", dist=34, dmg=950, cd=34, style="cosmic"), ab("sunbeam", range=64, dmg=760, width=1.7, cd=60, style="cosmic"),
                  ab("starfall", chance=0.35, dmg=380, radius=5), ab("on_kill_heal", amount=40)],
       recipe=shaped(["  X", " SD", "S  "], {"X": INGOT, "D": SC, "S": STICK}))
weapon("stellar_staff", A, "staff",
       dict(paint="staff", pal="cosmic", head="orb", runes=True, crown=True, anim="pulse"),
       name="Stellar Staff", lore="Points at something very far away and means it.", repair=INGOT,
       abilities=[ab("orbital_strike", range=56, radius=9, delay=18, dmg=2200, cd=220, style="cosmic"),
                  ab("meteor_shower", range=48, radius=14, count=14, ticks=70, blast=4.5, dmg=420, cd=170, style="cosmic")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": SC, "S": STICK}))
bow("galaxy_bow", A, dict(paint="bow", pal="cosmic", glow=True), name="Galaxy Bow",
    shot=dict(dmg=1000, speed=6.0, pierce=50, style="cosmic", trail="end_rod", count=5, spread=20, life=110, aoe=5.0, homing=True), draw=8, nograv=True, repair=INGOT,
    lore="Every shot is a constellation, for as long as it lasts.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": SC, "S": "minecraft:string"}))

armor_set("starborn", "Starborn", A, "cosmic", INGOT,
          [dict(need=4, **ab("flight")), dict(need=4, **ab("star_aura", count=6, orbit=3.0, dmg=140)), dict(need=4, **ab("quicken", cd_mult=0.45))],
          lore="Cut from the dark between two stars, then lit from within.", special=SC, ambient="cosmic", color=0x7B7BFF,
          attrs=[dict(attr="max_health", amount=8.0), dict(attr="movement_speed", amount=0.06, op="mul_base")],
          style=dict(crest=True, glow=True, wings=True), layer=dict(visor="slit", glow=True, emblem="gem"),
          set_desc="You fly. Six stars circle you and cut whatever they touch, and every ability recharges more than twice as fast.")

accessory("heart_of_astraeus", A, dict(paint="heart", pal="cosmic"), name="Heart of Astraeus",
          attrs=[dict(attr="max_health", amount=48.0), dict(attr="armor_toughness", amount=5.0)], abilities=[ab("star_aura", count=4, orbit=2.6, dmg=90)],
          rarity="epic", glint=True, obtain="Dropped by Astraeus.", lore="Light left it a very long time ago. It is still arriving.")
accessory("starwing_pendant", A, dict(paint="amulet", pal="cosmic"), name="Starwing Pendant",
          attrs=[dict(attr="movement_speed", amount=0.15, op="mul_base"), dict(attr="max_health", amount=12.0)], abilities=[ab("flight")],
          lore="Do not look at the sky and wish. It heard.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": SC}))
consumable("stardust_elixir", A, dict(paint="bottle", pal="cosmic", shape="flask"), name="Stardust Elixir",
           abilities=[ab("heal", amount=300), ab("give_effect", effect="haste", ticks=12000, amp=4, effect2="speed", ticks2=12000, amp2=4,
                                                 effect3="jump_boost", ticks3=12000, amp3=4)],
           lore="Sparkles on the way down. And on the way back.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:glowstone_dust", "minecraft:ender_eye", SC]))
consumable("nebula_bread", A, dict(paint="biscuit", pal="cosmic"), name="Nebula Bread", anim="eat", seconds=1.6, nutrition=20, saturation=2.0,
           abilities=[ab("heal", amount=500), ab("give_effect", effect="absorption", ticks=6000, amp=9, effect2="strength", ticks2=6000, amp2=5)],
           lore="Rises on its own, mostly sideways.", recipe=shapeless(["minecraft:bread", "minecraft:glow_berries", "minecraft:honey_bottle", SC]))

MB.mob("star_sprite", "Star Sprite", star_sprite(), A, hp=700, dmg=80, size=(0.7, 0.8), speed=0.42, fly=True, hover=3.0, keep=[8, 22], style="cosmic", melee=False,
       egg=(0x161a52, 0xa8b4ff), xp=60, minion_of="astraeus", follow=48,
       attacks=[atk("orbs", 2, count=3, dmg=0.8, speed=0.5, interval=12, style="cosmic", windup=20, recover=24, range=[5, 40], trail="end_rod"),
                atk("fan", 2, count=5, spread=40, dmg=0.55, speed=1.8, style="cosmic", trail="end_rod", windup=18, recover=22, range=[6, 40])],
       drops=[drop(SHARD, 0, 2, 0.6), drop("minecraft:amethyst_shard", 1, 3)],
       sounds=dict(hurt="entity.allay.hurt", death="entity.allay.death", ambient="entity.allay.ambient_without_item", pitch=1.6),
       lore="A spark that got ideas about being a star.")
MB.mob("astral_node", "Astral Node", astral_node(), A, hp=3000, dmg=110, armor=14, size=(1.2, 3.9), speed=0.0, kb=1.0, style="cosmic", egg=(0x161a52, 0x7b7bff), xp=140,
       minion_of="astraeus", follow=48, melee=False, fire=True,
       attacks=[atk("beam", 2, windup=30, duration=50, beam=34, sweep=90, width=0.9, dmg=0.45, step=4, style="cosmic", range=[0, 48], recover=26),
                atk("orbs", 2, count=3, dmg=0.8, speed=0.45, interval=14, style="cosmic", range=[0, 50], windup=24, recover=24, trail="end_rod")],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.amethyst_cluster.break", pitch=0.4),
       lore="Holds the sky to its place. Drop it and the sky starts to fall.")
MB.mob("comet_hound", "Comet Hound", comet_hound(), A, hp=2200, dmg=120, armor=12, size=(1.1, 1.2), speed=0.42, kb=0.4, style="cosmic", egg=(0x161a52, 0xa8b4ff), xp=110,
       attacks=[atk("charge", 2, windup=16, dist=20, speed=1.3, stun=28, vuln=1.4, dmg=1.4, width=1.8, style="cosmic", range=[5, 26], wall_stun=24),
                atk("leap", 1, windup=14, air=12, radius=3.4, dmg=1.2, style="cosmic", range=[4, 16], recover=12)],
       drops=[drop(SHARD, 1, 3), drop("minecraft:end_rod", 0, 2, 0.4), drop(SC, 0, 1, 0.02)],
       spawn=spawn(dim="end", weight=14, group=(2, 3), min_tier=5, gate="omnarch"),
       sounds=dict(hurt="entity.wolf.hurt", death="entity.wolf.death", ambient="entity.wolf.growl", pitch=0.55),
       lore="It left a tail across half the sky and brought the other half with it.")

MB.boss(
    "astraeus", "Astraeus, the Star-Eater", astraeus(), size=(1.9, 4.1), speed=0.34, scale=1.5, title="Sovereign of Stars", style="cosmic", fly=True, hover=3.0,
    keep=[12, 26], bar=dict(color="purple", overlay="notched_20"), minion="star_sprite", sigil="astral_sigil", xp=21000,
    egg=(0x161a52, 0xa8b4ff), kb=1.0, death_ticks=140, spawn_ticks=90, enrage=720,
    ritual=dict(dim="minecraft:the_end", time="any", sky=False, xp=40, hearts=0, items=[dict(item="minecraft:end_crystal", count=2)]),
    sounds=dict(hurt="entity.allay.hurt", death="entity.wither.death", ambient="block.beacon.ambient", pitch=0.45),
    bag=[drop(SC, 10, 14), drop(INGOT, 20, 26), drop("heart_of_astraeus", 1, 1), drop("minecraft:netherite_ingot", 6, 8), drop("titan_heart", 1, 1, 0.5),
         drop("minecraft:experience_bottle", 56, 72), drop("comet_blade", 1, 1, 0.35), drop("orrery_maul", 1, 1, 0.25), drop("minecraft:enchanted_golden_apple", 3, 4, 0.5)],
    phases=[
        phase("Dusk", 1.0, cd=(40, 62), attacks=[
            atk("meteors", 3, windup=26, count=10, delay=18, interval=5, radius=3.4, dmg=1.2, area=11, aimed=4, style="cosmic", range=[0, 60]),
            atk("orbs", 2, count=4, dmg=0.8, speed=0.5, interval=12, windup=22, recover=18, style="cosmic", trail="end_rod", range=[4, 50]),
            atk("fan", 2, count=9, spread=70, dmg=0.55, speed=1.6, windup=20, recover=14, style="cosmic", trail="end_rod", range=[5, 50]),
            atk("planets", 2, windup=36, duration=120, count=3, r0=5, r1=18, omega=0.08, size=1.7, dmg=1.0, style="cosmic", range=[0, 40]),
            atk("blink", 1, windup=12, dmg=1.4, radius=3.8, style="cosmic", range=[4, 40]),
            atk("exposed", 1, windup=16, duration=80, vuln=1.8, style="cosmic", range=[0, 60])]),
        phase("Night", 0.72, cd=(34, 56), speed=1.1, dmg=1.1, color="blue",
              enter=[atk("constellation", windup=44, hold=50, points=6, skip=2, radius=18, width=1.3, dmg=1.0, style="cosmic")],
              attacks=[
                  atk("constellation", 3, windup=42, hold=50, points=7, skip=3, radius=19, width=1.3, dmg=1.0, style="cosmic", range=[0, 60]),
                  atk("planets", 2, windup=34, duration=130, count=4, r0=5, r1=20, omega=0.085, size=1.7, dmg=1.0, style="cosmic", range=[0, 40]),
                  atk("blackhole", 2, windup=34, radius=12, core=3.2, life=110, pull=0.12, blast=1.0, tick=0.32, style="cosmic", range=[0, 60]),
                  atk("summon", 2, mob="star_sprite", count=3, cap=6, windup=28, recover=18, style="cosmic", range=[0, 60]),
                  atk("supernova", 1, windup=80, radius=36, dmg=2.4, exhaust=90, vuln=2.0, style="cosmic", range=[0, 60]),
                  atk("meteors", 2, windup=24, count=12, delay=16, interval=5, radius=3.4, dmg=1.3, area=12, aimed=4, style="cosmic", range=[0, 60])]),
        phase("Nova", 0.45, cd=(28, 48), speed=1.15, dmg=1.2, color="purple",
              nodes=dict(mob="astral_node", count=4, radius=14),
              attacks=[
                  atk("constellation", 3, windup=38, hold=56, points=8, skip=3, radius=20, width=1.3, pulse=4, dmg=1.1, style="cosmic", range=[0, 60]),
                  atk("planets", 3, windup=32, duration=160, count=5, r0=5, r1=22, omega=0.09, size=1.8, dmg=1.1, style="cosmic", range=[0, 40]),
                  atk("blackhole", 2, windup=32, count=2, radius=13, core=3.4, life=120, pull=0.13, blast=1.1, tick=0.34, style="cosmic", range=[0, 60]),
                  atk("supernova", 2, windup=76, radius=38, dmg=2.6, exhaust=85, vuln=2.1, style="cosmic", range=[0, 60]),
                  atk("beam", 2, windup=28, duration=100, arms=4, spin=2.4, low=True, beam=40, width=1.0, dmg=0.55, step=4, style="cosmic", range=[0, 40]),
                  atk("sunder", 1, windup=34, length=50, width=3.2, dmg=1.9, style="cosmic", range=[4, 60])]),
        phase("Heat Death", 0.18, cd=(24, 42), speed=1.25, dmg=1.4, color="white",
              enter=[atk("pools", windup=40, recover=20, count=12, radius=4.0, near=20, dmg=0.6, tick=0.3, life=400, style="cosmic", range=[0, 60])],
              attacks=[
                  atk("planets", 3, windup=30, duration=170, count=6, r0=5, r1=24, omega=0.095, size=1.9, dmg=1.2, style="cosmic", range=[0, 40]),
                  atk("constellation", 3, windup=36, hold=60, points=9, skip=4, radius=22, width=1.3, pulse=4, dmg=1.2, style="cosmic", range=[0, 60]),
                  atk("supernova", 3, windup=70, radius=40, dmg=2.8, exhaust=80, vuln=2.2, style="cosmic", range=[0, 60]),
                  atk("beam", 3, windup=26, duration=110, arms=6, spin=2.8, low=True, beam=42, width=1.0, dmg=0.6, step=4, style="cosmic", range=[0, 40]),
                  atk("blackhole", 2, windup=30, count=2, radius=13, core=3.4, life=120, pull=0.13, blast=1.1, tick=0.34, style="cosmic", range=[0, 60]),
                  atk("meteors", 2, windup=22, count=14, delay=14, interval=4, radius=3.4, dmg=1.4, area=13, aimed=4, style="cosmic", range=[0, 60]),
                  atk("exposed", 1, windup=14, duration=70, vuln=2.0, style="cosmic", range=[0, 60])]),
    ])

# =====================================================================================================================
# stage 4 - Eternus, the Final Thread (every colour)
# =====================================================================================================================
material(WC, E, dict(paint="core", pal="eternal", seed=34), name="Weave Core", rarity="epic", glint=True, lore="The place where every thread meets. It is knotted.")
add("thread_sigil", "sigil", A, dict(paint="sigil", pal="eternal"), name="Thread Sigil", rarity="epic", stack=16, glint=True,
    extra=dict(boss="eternus"), obtain="Crafted from the cores of the three older Sovereigns, Eternium, a Nether Star and Omni Cores.",
    lore="Use it on a Ritual Altar. The ritual costs fifty levels of experience, ten hearts of health and two Nether Stars. There is no second attempt until it is finished.",
    recipe=shaped(["FBS", "INI", "OOO"], {"F": FC, "B": BC, "S": SC, "I": INGOT, "N": "minecraft:nether_star", "O": "omni_core"}, category="misc"))
add("eternus_bag", "bag", E, dict(paint="bag", pal="eternal"), name="Eternus's Hoard", rarity="epic", stack=16, glint=True,
    extra=dict(boss="eternus"), obtain="Dropped by Eternus.", lore="Right-click to open. It has no inside.")

weapon("threadcutter", E, "sword",
       dict(paint="sword", pal="eternal", long=15, wide=1.6, guard="wing", pommel="gem", edge="flame", vein=True, runes=True, anim="prism"),
       name="Threadcutter", lore="It was made to cut one thread. The thread knows which.", repair=INGOT,
       abilities=[ab("omega_slash", radius=19, count=7, dmg=1000, cd=200, style="eternal"), ab("dash_strike", dist=34, dmg=1500, cd=30, style="eternal"),
                  ab("rend", pct=0.09, cap=3000), ab("lifesteal", pct=0.2), ab("execute", mult=3.0, threshold=0.5)],
       recipe=shaped(["XD", "XD", " S"], {"X": INGOT, "D": WC, "S": STICK}))
weapon("loomhammer", E, "hammer",
       dict(paint="axe", pal="eternal", size=1.9, double=True, vein=True, anim="prism"),
       name="Loomhammer", lore="It does not break things. It unpicks them.", repair=INGOT,
       abilities=[ab("ground_slam", radius=20, dmg=2600, cd=90, kb=3.0, style="eternal", stun=120), ab("absolute_zero", radius=22, dmg=1200, ticks=120, cd=200, style="eternal"),
                  ab("frostbite", chance=0.5, ticks=80), ab("shatter", mult=4.0)],
       recipe=shaped(["XDX", "XSX", " S "], {"X": INGOT, "D": WC, "S": STICK}))
weapon("eternal_daggers", E, "dagger",
       dict(paint="sword", pal="eternal", long=9.5, wide=1.0, guard="narrow", pommel="ring", edge="crystal", runes=True, anim="prism"),
       name="Eternal Daggers", lore="There are two. Neither is ever where the other was.", repair=INGOT,
       abilities=[ab("blink", dist=34, cd=24), ab("backstab", mult=7.0), ab("contagion", dmg=400, radius=12, ticks=240), ab("starfall", chance=0.4, dmg=500, radius=5)],
       recipe=shaped(["D", "X", "S"], {"X": INGOT, "D": WC, "S": STICK}))
weapon("chronicle_staff", E, "staff",
       dict(paint="staff", pal="eternal", head="orb", runes=True, crown=True, anim="prism"),
       name="Chronicle Staff", lore="Every line it writes has already happened.", repair=INGOT,
       abilities=[ab("meteor_shower", range=54, radius=17, count=26, ticks=100, blast=5.5, dmg=700, cd=160, style="eternal"),
                  ab("omega_slash", radius=18, count=6, dmg=800, cd=160, style="eternal")],
       recipe=shaped(["  D", " X ", "S  "], {"X": INGOT, "D": WC, "S": STICK}))
bow("infinity_bow", E, dict(paint="bow", pal="eternal", glow=True), name="Infinity Bow",
    shot=dict(dmg=1600, speed=7.0, pierce=99, style="eternal", trail="end_rod", count=7, spread=30, life=120, aoe=6.0, homing=True), draw=7, nograv=True, repair=INGOT,
    lore="It never runs out, and neither do the things it fires.",
    recipe=shaped([" XS", "D S", " XS"], {"X": INGOT, "D": WC, "S": "minecraft:string"}))

armor_set("eternal", "Eternal", E, "eternal", INGOT,
          [dict(need=4, **ab("second_wind", pct=1.0, cd=3600, radius=16, ticks=100)), dict(need=4, **ab("omni_aura", radius=15, dmg=200, heal=14, every=16)),
           dict(need=4, **ab("quicken", cd_mult=0.4)), dict(need=4, **ab("damage_reduction", pct=0.3))],
          lore="Woven from every thread there has ever been, and one more.", special=WC, ambient="eternal", color=0xFFFFFF,
          attrs=[dict(attr="max_health", amount=10.0), dict(attr="movement_speed", amount=0.06, op="mul_base"), dict(attr="attack_damage", amount=0.1, op="mul_base")],
          style=dict(horns=True, crest=True, glow=True, spikes=True, wings=True), layer=dict(visor="full", glow=True, emblem="gem"),
          set_desc="A killing blow is refused: you stand back up whole and everything near you stops. Everything near you is worn down while you recover, abilities recharge two and a half times as fast, and hits land a third softer.")

accessory("heart_of_eternus", E, dict(paint="heart", pal="eternal"), name="Heart of Eternus",
          attrs=[dict(attr="max_health", amount=60.0), dict(attr="armor_toughness", amount=6.0), dict(attr="attack_damage", amount=0.4, op="mul_base")],
          abilities=[ab("omni_aura", radius=11, dmg=120, heal=6, every=16)], rarity="epic", glint=True, obtain="Dropped by Eternus.", lore="It beats once. It has been beating once for a very long time.")
accessory("ouroboros_ring", E, dict(paint="ring", pal="eternal"), name="Ouroboros Ring",
          attrs=[dict(attr="attack_speed", amount=0.35, op="mul_base"), dict(attr="movement_speed", amount=0.12, op="mul_base")], abilities=[ab("quicken", cd_mult=0.7)],
          lore="It is eating itself, and has not got any smaller.", recipe=shaped([" X ", "XDX", " X "], {"X": INGOT, "D": WC}))
accessory("thread_of_fate", E, dict(paint="feather", pal="eternal"), name="Thread of Fate",
          attrs=[dict(attr="attack_damage", amount=0.5, op="mul_base"), dict(attr="max_health", amount=20.0)], abilities=[ab("star_aura", count=3, orbit=2.4, dmg=100)],
          lore="One end is in your hand. The other has already been cut.", recipe=shaped(["S S", " X ", " D "], {"S": "minecraft:string", "X": INGOT, "D": WC}))
consumable("eternal_elixir", E, dict(paint="bottle", pal="eternal", shape="flask"), name="Eternal Elixir",
           abilities=[ab("cleanse"), ab("heal", amount=800), ab("give_effect", effect="strength", ticks=18000, amp=6, effect2="resistance", ticks2=18000, amp2=5,
                                                               effect3="regeneration", ticks3=3000, amp3=5)],
           lore="Every flavour, in turn.", recipe=shapeless(["minecraft:glass_bottle", "minecraft:nether_star", "minecraft:enchanted_golden_apple", WC]))
consumable("ambrosia", E, dict(paint="biscuit", pal="eternal"), name="Ambrosia", anim="eat", seconds=2.0, nutrition=20, saturation=2.0,
           abilities=[ab("heal", amount=1000), ab("give_effect", effect="absorption", ticks=9000, amp=14, effect2="saturation", ticks2=20, amp2=0)],
           lore="Food of the ones who do not get hungry. It is not enough.", recipe=shapeless(["minecraft:cooked_beef", "minecraft:golden_carrot", "minecraft:honey_bottle", "minecraft:enchanted_golden_apple", WC]))

MB.mob("loom_spindle", "Loom Spindle", loom_spindle(), E, hp=3600, dmg=130, armor=16, size=(1.2, 3.9), speed=0.0, kb=1.0, style="eternal", egg=(0x14101f, 0xffffff), xp=160,
       minion_of="eternus", follow=48, melee=False, fire=True,
       attacks=[atk("beam", 2, windup=30, duration=50, beam=34, sweep=90, width=0.9, dmg=0.45, step=4, style="eternal", range=[0, 48], recover=26),
                atk("orbs", 2, count=3, dmg=0.8, speed=0.45, interval=14, style="eternal", range=[0, 50], windup=24, recover=24, trail="end_rod")],
       sounds=dict(hurt="block.amethyst_block.hit", death="block.beacon.deactivate", pitch=0.4),
       lore="Holds the Final Thread taut. Cut it and the thread has to hold itself.")

MB.boss(
    "eternus", "Eternus, the Final Thread", eternus(), size=(2.6, 5.2), speed=0.32, scale=1.8, title="The Final Thread", style="eternal", fly=True, hover=4.0,
    keep=[10, 24], bar=dict(color="pink", overlay="notched_20"), minion="halo_blade", sigil="thread_sigil", xp=34000,
    egg=(0xffffff, 0xff4d6d), kb=1.0, death_ticks=170, spawn_ticks=110, enrage=900,
    ritual=dict(dim="", time="any", sky=False, xp=50, hearts=10, items=[dict(item="minecraft:nether_star", count=2)]),
    sounds=dict(hurt="entity.wither.hurt", death="entity.wither.death", ambient="entity.wither.ambient", pitch=0.4),
    bag=[drop(WC, 12, 16), drop(INGOT, 24, 32), drop("heart_of_eternus", 1, 1), drop("minecraft:netherite_ingot", 8, 12), drop("titan_heart", 1, 2, 0.8),
         drop("minecraft:experience_bottle", 64, 96), drop("threadcutter", 1, 1, 0.4), drop("loomhammer", 1, 1, 0.25), drop("infinity_bow", 1, 1, 0.2),
         drop("minecraft:enchanted_golden_apple", 4, 6, 0.6), drop("minecraft:nether_star", 1, 2, 0.5)],
    phases=[
        phase("Ember and Storm", 1.0, cd=(40, 62), attacks=[
            atk("meteors", 2, windup=28, count=9, delay=24, radius=3.2, dmg=1.2, area=10, aimed=3, style="ember", range=[0, 60]),
            atk("lightning", 2, windup=28, count=7, radius=2.4, dmg=1.3, delay=26, interval=8, area=11, aimed=3, style="storm", range=[0, 60]),
            atk("tether", 2, windup=40, reach=48, pull=2.0, radius=5.5, dmg=1.2, style="eternal", range=[0, 50]),
            atk("fan", 2, count=9, spread=70, dmg=0.55, speed=1.5, windup=20, recover=14, style="eternal", trail="end_rod", range=[5, 50]),
            atk("slam", 1, windup=26, recover=18, radius=8.0, dmg=1.5, kb=1.4, style="eternal", range=[0, 11]),
            atk("charge", 1, windup=32, dist=24, speed=1.15, stun=44, vuln=1.7, dmg=1.6, width=2.6, style="eternal", range=[8, 34], wall_stun=40)]),
        phase("Blood and Time", 0.78, cd=(36, 58), speed=1.08, dmg=1.1, color="red",
              enter=[atk("summon", mob="halo_blade", count=3, cap=6, windup=30, recover=16, style="eternal")],
              attacks=[
                  atk("leech", 2, windup=22, duration=80, beam=26, dmg=0.2, heal=0.9, style="blood", range=[4, 28], recover=18),
                  atk("stop", 2, windup=28, hold=46, count=18, radius=7.2, dmg=0.85, speed=0.95, style="chrono", recover=28, range=[0, 50]),
                  atk("rewind", 1, windup=34, back=60, dmg=0.3, style="chrono", recover=18, range=[0, 50]),
                  atk("tether", 2, windup=38, reach=48, pull=2.1, radius=6.0, dmg=1.3, style="eternal", range=[0, 50]),
                  atk("ring", 1, windup=28, recover=20, speed=0.65, max_radius=24, dmg=1.3, style="eternal", range=[0, 20]),
                  atk("blackhole", 1, windup=32, radius=12, core=3.0, life=100, pull=0.11, blast=0.9, tick=0.3, style="eternal", range=[0, 60]),
                  atk("exposed", 1, windup=16, duration=80, vuln=1.9, style="eternal", range=[0, 60])]),
        phase("Void and Light", 0.55, cd=(32, 54), speed=1.15, dmg=1.2, color="white",
              attacks=[
                  atk("beam", 3, windup=28, duration=100, arms=5, spin=2.6, low=True, beam=38, width=1.0, dmg=0.55, step=4, style="radiant", range=[0, 40]),
                  atk("supernova", 2, windup=74, radius=38, dmg=2.6, exhaust=85, vuln=2.0, style="radiant", range=[0, 60]),
                  atk("blackhole", 2, windup=30, count=2, radius=13, core=3.4, life=110, pull=0.12, blast=1.0, tick=0.32, style="void", range=[0, 60]),
                  atk("cage", 2, windup=30, close=50, radius=9, gaps=3, gap_arc=40, dmg=1.3, style="void", range=[0, 40]),
                  atk("sunder", 2, windup=34, length=50, width=3.2, dmg=1.9, style="eternal", range=[4, 60]),
                  atk("tether", 2, windup=36, reach=48, pull=2.2, radius=6.0, dmg=1.4, style="eternal", range=[0, 50]),
                  atk("flash", 1, windup=36, blind=90, dmg=0.35, cone=80, beam=60, style="radiant", range=[0, 60])]),
        phase("Winter, Rot and the Stars", 0.32, cd=(28, 48), speed=1.2, dmg=1.3, color="purple",
              attacks=[
                  atk("icicles", 2, windup=30, lines=10, length=34, speed=1.7, width=1.5, waves=2, gap=14, dmg=1.1, style="frost", range=[0, 60]),
                  atk("glaciate", 2, windup=32, radius=13, delay=28, shards=4, dmg=1.1, shard_dmg=1.3, style="frost", range=[0, 16]),
                  atk("spores", 2, windup=26, count=9, radius=4.0, flight=24, life=250, dmg=0.6, style="plague", range=[0, 60]),
                  atk("infest", 1, windup=24, fuse=80, pods=2, mob="halo_blade", cap=8, dmg=1.2, radius=6.5, style="plague", range=[0, 60]),
                  atk("planets", 2, windup=32, duration=150, count=5, r0=5, r1=22, omega=0.09, size=1.8, dmg=1.1, style="cosmic", range=[0, 40]),
                  atk("constellation", 2, windup=38, hold=52, points=8, skip=3, radius=20, width=1.3, pulse=4, dmg=1.1, style="cosmic", range=[0, 60]),
                  atk("whiteout", 1, windup=28, duration=150, radius=40, dmg=0.4, grace=24, style="frost", range=[0, 60])]),
        phase("The Unwoven", 0.12, cd=(22, 40), speed=1.3, dmg=1.5, color="red",
              nodes=dict(mob="loom_spindle", count=4, radius=15),
              enter=[atk("pools", windup=40, recover=20, count=14, radius=4.2, near=22, dmg=0.6, tick=0.3, life=500, style="eternal", range=[0, 60])],
              attacks=[
                  atk("tether", 3, windup=34, reach=50, pull=2.3, radius=6.5, dmg=1.5, style="eternal", range=[0, 50]),
                  atk("supernova", 2, windup=70, radius=40, dmg=2.9, exhaust=80, vuln=2.2, style="eternal", range=[0, 60]),
                  atk("constellation", 2, windup=34, hold=60, points=9, skip=4, radius=22, width=1.3, pulse=4, dmg=1.3, style="eternal", range=[0, 60]),
                  atk("planets", 2, windup=30, duration=170, count=6, r0=5, r1=24, omega=0.095, size=1.9, dmg=1.2, style="eternal", range=[0, 40]),
                  atk("beam", 2, windup=26, duration=110, arms=6, spin=3.0, low=True, beam=40, width=1.0, dmg=0.6, step=4, style="eternal", range=[0, 40]),
                  atk("blackhole", 2, windup=30, count=2, radius=13, core=3.4, life=120, pull=0.13, blast=1.1, tick=0.34, style="eternal", range=[0, 60]),
                  atk("meteors", 2, windup=24, count=14, delay=22, radius=3.2, dmg=1.4, area=12, aimed=4, style="ember", range=[0, 60]),
                  atk("glaciate", 1, windup=30, radius=15, delay=26, shards=5, dmg=1.2, shard_dmg=1.4, style="frost", range=[0, 18]),
                  atk("exposed", 1, windup=14, duration=70, vuln=2.1, style="eternal", range=[0, 60])]),
    ])

# =====================================================================================================================
# the invasion of tier 6
# =====================================================================================================================
invasion(6, [("glacial_brute", 2, 3), ("blight_crawler", 3, 4), ("mycelid", 2, 3), ("comet_hound", 2, 3), ("rime_wraith", 2, 3), ("star_sprite", 3, 4)], xp=1600, title="The Threads Unravel",
         reward=[drop("minecraft:experience_bottle", 20, 36), drop(SHARD, 8, 14), drop(FC, 0, 1, 0.2), drop(BC, 0, 1, 0.2), drop(SC, 0, 1, 0.2), drop(WC, 0, 1, 0.08)])
