"""Regular mobs: twelve creatures across the tiers, their natural spawn rules, and the invasion waves that scale with progress.

spawn = dict(dim, weight, group=[min,max], min_tier, gate, night, y=[min,max]) - read by mob/Mobs.java (placements + biome spawns)."""
from . import mobs as MB
from .mobs import atk, drop
from .rigs_mobs import beast, brute, floater

INVASIONS = []


def invasion(tier, mobs, xp, reward=None, title=None):
    """One invasion wave for a tier: mobs = [(mob id, min, max)]; reward = list of drop rows handed out when the wave is cleared."""
    INVASIONS.append(dict(tier=tier, mobs=[dict(mob=m, min=a, max=b) for m, a, b in mobs], xp=xp, reward=reward or [], title=title or ""))


def spawn(dim="overworld", weight=20, group=(1, 1), min_tier=1, gate="", night=False, y=None):
    return dict(dim=dim, weight=weight, group=list(group), min_tier=min_tier, gate=gate, night=night, y=list(y) if y else None)


# =====================================================================================================================
# overworld - tier 1
# =====================================================================================================================
MB.mob("thornback", "Thornback", beast("thornback", dict(body="#6b5a3a", spike="#3a2e1c", head="#7a6644"), "#9fe36a", body=(12, 9, 18), leg_h=7,
                                       head=(8, 8, 9), spikes=5, horns=2, tail=3, eyes="#ff9a3a", spike_pal="spike"),
       "start", hp=34, dmg=6, size=(1.1, 1.1), speed=0.3, kb=0.1, egg=(0x6b5a3a, 0x9fe36a), xp=7, style="verdant",
       attacks=[atk("charge", 1, windup=22, dist=11, speed=0.9, stun=24, vuln=1.3, dmg=1.3, width=1.4, style="verdant", range=[5, 18], wall_stun=30)],
       drops=[drop("minecraft:porkchop", 1, 3), drop("minecraft:leather", 0, 2, 0.6)],
       spawn=spawn(weight=40, group=(2, 4), min_tier=1, night=True),
       sounds=dict(hurt="entity.hoglin.hurt", death="entity.hoglin.death", ambient="entity.hoglin.ambient", pitch=1.1),
       lore="A boar that decided armour was a lifestyle. It charges anything that moves after dark.")
MB.mob("moss_wisp", "Moss Wisp", floater("moss_wisp", dict(body="#4f8a3a", accent="#a8e06a"), "#a8e06a", core=(5, 5, 5), y=9, puffs=4, eyes="#ffffff"),
       "start", hp=18, dmg=4, size=(0.7, 0.8), speed=0.3, fly=True, hover=1.0, keep=[5, 12], egg=(0x4f8a3a, 0xa8e06a), xp=6, style="verdant", melee=False,
       attacks=[atk("fan", 1, count=1, spread=0, dmg=1.0, speed=1.1, windup=16, recover=40, style="verdant", range=[4, 24], size=0.8, aoe=0.0)],
       drops=[drop("minecraft:glowstone_dust", 0, 2, 0.6), drop("minecraft:moss_block", 0, 1, 0.3)],
       spawn=spawn(weight=30, group=(1, 3), min_tier=1, night=True),
       sounds=dict(hurt="entity.allay.hurt", death="entity.allay.death", ambient="entity.allay.ambient_without_item", pitch=1.3),
       lore="A scrap of lamplight that learned to spit.")

# =====================================================================================================================
# overworld - tier 2 (after the Ender Dragon)
# =====================================================================================================================
MB.mob("cave_stalker", "Cave Stalker", beast("cave_stalker", dict(body="#2f3a4a", spike="#8fd0ff"), "#7fe8ff", body=(9, 6, 12), leg_h=6, head=(6, 5, 6),
                                             legs=6, fangs=True, eyes="#7fe8ff"),
       "ender_dragon", hp=48, dmg=9, size=(1.2, 0.9), speed=0.34, egg=(0x2f3a4a, 0x7fe8ff), xp=12, style="azure",
       attacks=[atk("leap", 1, windup=18, air=14, radius=3.5, dmg=1.2, style="azure", range=[4, 14], recover=14)],
       drops=[drop("minecraft:string", 1, 3), drop("minecraft:spider_eye", 0, 1, 0.5), drop("raw_azurite", 0, 1, 0.12)],
       spawn=spawn(weight=25, group=(2, 3), min_tier=2, gate="ender_dragon", y=(-64, 40)),
       sounds=dict(hurt="entity.spider.hurt", death="entity.spider.death", ambient="entity.spider.ambient", pitch=0.8),
       lore="Hunts in the dark between the ore veins. It always jumps first.")
MB.mob("crystal_golem", "Crystal Golem", brute("crystal_golem", dict(body="#5a7a9a", accent="#a8d8ff", core="#7fe8ff"), "#7fe8ff", h=44, bulk=1.15, spikes=4,
                                              eyes="#e8ffff", cracks=0.35),
       "ender_dragon", hp=130, dmg=14, armor=8, size=(1.5, 2.8), speed=0.2, kb=0.6, egg=(0x5a7a9a, 0xa8d8ff), xp=24, style="azure",
       attacks=[atk("slam", 1, windup=26, recover=18, radius=4.5, dmg=1.4, style="azure", range=[0, 7], kb=1.0)],
       drops=[drop("minecraft:amethyst_shard", 2, 5), drop("raw_azurite", 0, 2, 0.4), drop("drakescale", 0, 1, 0.04)],
       spawn=spawn(weight=10, group=(1, 1), min_tier=2, gate="ender_dragon", y=(-64, 30)),
       sounds=dict(hurt="block.amethyst_block.hit", death="block.amethyst_cluster.break", ambient="block.amethyst_block.chime", pitch=0.6),
       lore="A geode that stood up. Do not hit it where it glitters.")
MB.mob("sky_wyvern", "Sky Wyvern", floater("sky_wyvern", dict(body="#3a5a8a", accent="#7fe8ff"), "#7fe8ff", core=(6, 5, 12), y=14, wings=2, tail=8,
                                           eyes="#e8ffff", head=(5, 5, 6)),
       "ender_dragon", hp=75, dmg=14, size=(1.4, 1.1), speed=0.34, fly=True, hover=5.0, keep=[8, 16], egg=(0x3a5a8a, 0x7fe8ff), xp=18, style="azure",
       attacks=[atk("fan", 3, count=3, spread=28, dmg=0.7, speed=1.2, windup=22, recover=26, style="azure", range=[6, 36]),
                atk("charge", 2, windup=26, dist=14, speed=1.0, stun=20, vuln=1.4, dmg=1.4, width=1.6, style="azure", range=[8, 26], wall_stun=0)],
       drops=[drop("minecraft:phantom_membrane", 1, 2), drop("minecraft:feather", 1, 3)],
       spawn=spawn(weight=15, group=(1, 2), min_tier=2, gate="ender_dragon", night=True),
       sounds=dict(hurt="entity.phantom.hurt", death="entity.phantom.death", ambient="entity.phantom.ambient", pitch=0.7),
       lore="Hunts by moonlight. The dragon's smaller, angrier cousin.")

# =====================================================================================================================
# the Nether - tier 3 (after Ignarok)
# =====================================================================================================================
MB.mob("cinder_hound", "Cinder Hound", beast("cinder_hound", dict(body="#3a2a2a", spike="#ff7a1f", head="#4a3030"), "#ff7a1f", body=(9, 8, 15), leg_h=8,
                                            head=(7, 7, 8), spikes=4, horns=2, tail=4, eyes="#ffd25a", fangs=True, bright=0.3),
       "ignarok", hp=55, dmg=12, size=(1.0, 1.2), speed=0.4, fire=True, egg=(0x3a2a2a, 0xff7a1f), xp=16, style="ember",
       attacks=[atk("charge", 1, windup=16, dist=12, speed=1.1, stun=20, vuln=1.3, dmg=1.2, width=1.3, style="ember", range=[5, 20], wall_stun=24)],
       drops=[drop("minecraft:blaze_powder", 0, 2), drop("minecraft:magma_cream", 0, 1, 0.4), drop("raw_cinderite", 0, 1, 0.06)],
       spawn=spawn(dim="nether", weight=35, group=(3, 5), min_tier=3, gate="ignarok"),
       sounds=dict(hurt="entity.zoglin.hurt", death="entity.zoglin.death", ambient="entity.zoglin.ambient", pitch=0.8),
       lore="Hunts in packs along the basalt shores. The fire is not decoration.")
MB.mob("magma_brute", "Magma Brute", brute("magma_brute", dict(body="#4a2a22", accent="#ff7a1f", core="#ffb03a"), "#ff7a1f", h=46, bulk=1.3, spikes=5,
                                          eyes="#ffd25a", cracks=0.5),
       "ignarok", hp=170, dmg=20, armor=10, size=(1.7, 3.0), speed=0.22, kb=0.7, fire=True, egg=(0x4a2a22, 0xff7a1f), xp=30, style="ember",
       attacks=[atk("slam", 2, windup=26, recover=18, radius=5.5, dmg=1.5, style="ember", range=[0, 8], kb=1.1),
                atk("pools", 1, windup=24, recover=22, count=2, radius=2.6, dmg=0.3, tick=0.25, life=120, style="ember", range=[3, 24])],
       drops=[drop("minecraft:magma_cream", 1, 3), drop("minecraft:netherite_scrap", 0, 1, 0.05), drop("ember_core", 0, 1, 0.03)],
       spawn=spawn(dim="nether", weight=14, group=(1, 1), min_tier=3, gate="ignarok"),
       sounds=dict(hurt="entity.iron_golem.hurt", death="entity.iron_golem.death", ambient="entity.magma_cube.squish", pitch=0.55),
       lore="What a lava seam looks like when it gets tired of being a seam.")
MB.mob("ash_wraith", "Ash Wraith", floater("ash_wraith", dict(body="#3a3434", accent="#ff7a1f"), "#ff7a1f", core=(6, 8, 6), y=16, halo=5, eyes="#ffd25a"),
       "ignarok", hp=62, dmg=13, size=(0.9, 1.6), speed=0.3, fly=True, fire=True, hover=1.5, keep=[6, 14], egg=(0x3a3434, 0xff7a1f), xp=20, style="ember",
       melee=False,
       attacks=[atk("orbs", 2, count=2, dmg=0.9, speed=0.4, interval=14, style="ember", windup=22, recover=26, range=[5, 30], aoe=1.5),
                atk("fan", 2, count=3, spread=30, dmg=0.6, speed=1.2, windup=20, recover=24, style="ember", range=[4, 28])],
       drops=[drop("minecraft:bone", 0, 2), drop("minecraft:blaze_powder", 0, 1, 0.4)],
       spawn=spawn(dim="nether", weight=22, group=(1, 2), min_tier=3, gate="ignarok"),
       sounds=dict(hurt="entity.ghast.hurt", death="entity.ghast.death", ambient="entity.ghast.ambient", pitch=1.3),
       lore="What is left of a visitor who stayed too long. It still throws things.")

# =====================================================================================================================
# tier 4: the three Sovereigns' creatures
# =====================================================================================================================
MB.mob("thunder_hawk", "Thunder Hawk", floater("thunder_hawk", dict(body="#4a5a7a", accent="#ffe94d"), "#9fe3ff", core=(5, 5, 10), y=14, wings=2, tail=5,
                                              eyes="#ffe94d", head=(4, 4, 6)),
       "voltaris", hp=95, dmg=22, size=(1.3, 1.0), speed=0.42, fly=True, hover=4.0, keep=[8, 18], egg=(0x4a5a7a, 0xffe94d), xp=30, style="storm",
       attacks=[atk("charge", 2, windup=22, dist=16, speed=1.1, stun=18, vuln=1.4, dmg=1.3, width=1.5, style="storm", range=[8, 28], wall_stun=0),
                atk("fan", 2, count=3, spread=16, dmg=0.6, speed=1.4, windup=18, recover=22, style="storm", trail="electric_spark", range=[6, 36])],
       drops=[drop("minecraft:feather", 2, 4), drop("minecraft:lightning_rod", 0, 1, 0.3), drop("storm_core", 0, 1, 0.03)],
       spawn=spawn(weight=14, group=(1, 2), min_tier=4, gate="voltaris", y=(70, 320)),
       sounds=dict(hurt="entity.parrot.hurt", death="entity.parrot.death", ambient="entity.breeze.idle_air", pitch=0.5),
       lore="Rides the front of every storm Voltaris has ever called.")
MB.mob("leech_thrall", "Leech Thrall", brute("leech_thrall", dict(body="#6a4a58", accent="#d8cdb8", core="#ff3355"), "#ff3355", h=38, bulk=1.0, spikes=0,
                                             eyes="#ff3355", paint="rock", cracks=0.4),
       "hemorath", hp=120, dmg=24, armor=6, size=(1.0, 2.4), speed=0.26, egg=(0x6a4a58, 0xff3355), xp=30, style="blood",
       attacks=[atk("leech", 2, windup=18, duration=40, beam=9, range=[2, 10], dmg=0.12, heal=0.8, style="blood", recover=18),
                atk("leap", 1, windup=22, air=14, radius=3.2, dmg=1.2, style="blood", range=[5, 14])],
       drops=[drop("minecraft:rotten_flesh", 1, 3), drop("minecraft:bone", 0, 2), drop("blood_core", 0, 1, 0.03)],
       spawn=spawn(weight=16, group=(1, 3), min_tier=4, gate="hemorath", night=True),
       sounds=dict(hurt="entity.zombie_villager.hurt", death="entity.zombie_villager.death", ambient="entity.zombie_villager.ambient", pitch=0.6),
       lore="Hemorath's leftovers. They keep what they take.")
MB.mob("clockwork_sentinel", "Clockwork Sentinel", brute("clockwork_sentinel", dict(body="#7a6a3a", accent="#d9b24a", core="#3ec6b0"), "#3ec6b0", h=48,
                                                        bulk=1.2, spikes=3, eyes="#3ec6b0", cracks=0.25),
       "chronos", hp=170, dmg=28, armor=14, size=(1.6, 3.1), speed=0.2, kb=0.8, egg=(0x7a6a3a, 0x3ec6b0), xp=36, style="chrono",
       attacks=[atk("ring", 2, windup=28, recover=22, speed=0.5, max_radius=12, dmg=1.2, style="chrono", range=[0, 14]),
                atk("slam", 2, windup=26, recover=18, radius=5.0, dmg=1.4, style="chrono", range=[0, 7])],
       drops=[drop("minecraft:iron_ingot", 2, 5), drop("minecraft:clock", 0, 1, 0.25), drop("chrono_core", 0, 1, 0.03)],
       spawn=spawn(weight=10, group=(1, 1), min_tier=4, gate="chronos"),
       sounds=dict(hurt="block.chain.hit", death="block.anvil.destroy", ambient="block.bell.use", pitch=0.5),
       lore="Still counting. It has been counting for a very long time.")
MB.mob("hourglass_wraith", "Hourglass Wraith", floater("hourglass_wraith", dict(body="#2a5a66", accent="#e8d9a8"), "#3ec6b0", core=(6, 9, 6), y=17, halo=6,
                                                      eyes="#e8d9a8"),
       "chronos", hp=100, dmg=26, size=(0.9, 1.7), speed=0.3, fly=True, hover=1.5, keep=[6, 14], egg=(0x2a5a66, 0xe8d9a8), xp=34, style="chrono", melee=False,
       attacks=[atk("blink", 2, windup=14, dmg=1.3, radius=3.2, style="chrono", range=[4, 30]),
                atk("orbs", 2, count=3, dmg=0.85, speed=0.42, interval=12, style="chrono", windup=20, recover=24, range=[5, 36], trail="soul_fire_flame")],
       drops=[drop("minecraft:glowstone_dust", 1, 3), drop("minecraft:amethyst_shard", 0, 2, 0.5)],
       spawn=spawn(weight=12, group=(1, 2), min_tier=4, gate="chronos"),
       sounds=dict(hurt="entity.vex.hurt", death="entity.vex.death", ambient="entity.vex.ambient", pitch=0.6),
       lore="A minute that wandered off from its hour.")

# =====================================================================================================================
# invasions: periodic waves of tier-appropriate creatures; they scale with the config's invaderScalingPerTier
# =====================================================================================================================
invasion(1, [("thornback", 3, 5), ("moss_wisp", 2, 3)], xp=60, title="The Hollow Hunt",
         reward=[MB.drop("minecraft:experience_bottle", 3, 6), MB.drop("verdite_ingot", 2, 5)])
invasion(2, [("thornback", 2, 3), ("cave_stalker", 3, 4), ("crystal_golem", 1, 1), ("sky_wyvern", 1, 2)], xp=140, title="The Deep Awakens",
         reward=[MB.drop("minecraft:experience_bottle", 6, 10), MB.drop("azurite_ingot", 2, 5), MB.drop("drakescale", 0, 1, 0.3)])
invasion(3, [("cinder_hound", 4, 6), ("magma_brute", 1, 2), ("ash_wraith", 2, 3), ("ember_imp", 3, 4)], xp=260, title="Ash and Ember",
         reward=[MB.drop("minecraft:experience_bottle", 8, 14), MB.drop("cinderite_ingot", 2, 5), MB.drop("ember_core", 0, 1, 0.3)])
invasion(4, [("thunder_hawk", 2, 3), ("leech_thrall", 3, 4), ("clockwork_sentinel", 1, 2), ("hourglass_wraith", 2, 3), ("storm_wisp", 2, 3)], xp=420,
         title="The Sovereigns' Court",
         reward=[MB.drop("minecraft:experience_bottle", 10, 18), MB.drop("stormite_ingot", 2, 6), MB.drop("storm_core", 0, 1, 0.25),
                 MB.drop("blood_core", 0, 1, 0.25), MB.drop("chrono_core", 0, 1, 0.25)])
