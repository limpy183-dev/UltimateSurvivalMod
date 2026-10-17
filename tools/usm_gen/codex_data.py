"""Lore book text: the prologue, the boss lineup (theme, summon method, attack tips) and general tips.

Boss entries reveal in stages: the name/lore/summon method once the previous Sovereign has fallen
(or you meet it), the full attack guide once you have defeated it yourself."""

PROLOGUE = (
    "Before the first dawn the Weaver spun the world from seven threads: Fire, Storm, Blood, Time, Void, Light - "
    "and a seventh that held the others together. Then the Weaver broke, and the threads snapped. Each became a "
    "Sovereign, enthroned in the element it once governed, and the Dragon of the End was set to keep them apart.\n"
    "The Dragon's seals are failing. Defeat the Sovereigns one by one, gather what they shed, and either mend the "
    "Sundering - or master it.\n"
    "But the Weaver had cut away three older threads before the first dawn - Winter, Rot and the Stars - and a fourth that "
    "held them, the Final Thread. When the Sundered God falls they wake."
)

# per gate: name, title, theme, difficulty note, lore, summon, arena, tips (revealed after the first victory)
BOSSES = {
    "ender_dragon": dict(
        theme="End", rank="Warden of the End",
        lore="The oldest guardian. It hoards the cold light of the End and sheds drakescale when it falls - the key metal "
             "of the second age. With the Dragon dead, the deeper seals begin to crack.",
        summon="Vanilla ritual: place four End Crystals on the exit portal in the End.",
        tips=["It circles the central pillars: break every healing crystal first.",
              "Perch phases are your window - strike while it sits on the portal.",
              "Bring a bow for the crystals and a sword for the perch; carry blocks to hide from the breath."]),
    "ignarok": dict(
        theme="Fire", rank="Sovereign of Cinders - Tier 3 gate",
        lore="A furnace-golem forged to hold the lava seams shut. Greed woke it, and it has burned ever since. Its heart still "
             "glows through cracks in the slag: that glow is the only warmth left in it.",
        summon="Craft a Cinder Sigil and use it on a Ritual Altar in the Nether. Bring fire resistance and room to run.",
        tips=["Red rings on the floor are slams - step out before they close.",
              "Lava pools linger for ten seconds; keep moving, never fight with your back to one.",
              "When it overheats the vents open: it takes extra damage, hit it then.",
              "Phase three rains meteors in telegraphed circles; the inferno ring can be jumped."]),
    "voltaris": dict(
        theme="Storm", rank="Sovereign of Thunder - Tier 4 stage 1",
        lore="Voltaris never touches the ground. It rides the storm front across the world, and where its shadow passes the "
             "sky tears itself open to let the lightning down.",
        summon="Craft a Tempest Sigil and use it on a Ritual Altar under open sky. Thunderstorms make the ritual easier.",
        tips=["Yellow circles mark lightning - leave before the third pulse.",
              "Gusts shove you; fight with a wall at your back, not an edge.",
              "In the eye of the storm it is immune: shatter the three storm totems to ground it.",
              "Spark orbs arc between each other - never let two cross you."]),
    "hemorath": dict(
        theme="Blood", rank="Sovereign of the Hunt - Tier 4 stage 2",
        lore="The mother of every night-thing, Hemorath feeds on life and gives it back to her brood. She was beautiful once. "
             "The hunger took that first.",
        summon="Craft a Blood Sigil and use it on a Ritual Altar at night. The ritual costs you four hearts of health.",
        tips=["When her crimson shield shimmers, STOP attacking - it reflects your damage back.",
              "Leech beams break if you step behind cover; bats feed her, kill them first.",
              "Blood pools hurt the same as lava. Do not fight in them.",
              "In the Blood Moon phase she heals from your hits - burst her down in the windows between volleys."]),
    "chronos": dict(
        theme="Time", rank="Sovereign of Hours - Tier 4 stage 3",
        lore="Chronos spent his hours long ago and now lives on borrowed ones. He remembers every second you have ever "
             "wasted and he will give them all back at once.",
        summon="Craft a Chrono Sigil and use it on a Ritual Altar. The ritual drains five levels of experience.",
        tips=["A rewind snaps you back to where you stood three seconds ago - do not be standing next to anything.",
              "During Time Stop bullets hang in the air: pick your route, it is released all at once.",
              "Only the afterimage that casts a shadow is real.",
              "He is only vulnerable while the hourglass is open - waste the window and he turns back the clock on his wounds."]),
    "nihilus": dict(
        theme="Void", rank="Sovereign of the Absence - Tier 5 stage 1",
        lore="Nihilus is what remains when a thing is unmade: an appetite with a shape. Light bends toward it and does not "
             "come back.",
        summon="Craft a Void Sigil and use it on a Ritual Altar in the End dimension. The ritual consumes four Eyes of Ender.",
        tips=["Black holes pull hardest at the core and barely at the rim: stay near the edge and strafe, then brace for the collapse when one ends.",
              "Rifts in the floor each release a void spawn and burn until it dies. Kill what comes out to close them.",
              "Eclipse: the dome of darkness rots anyone who is not standing in light. Keep a torch, lantern or glowstone beside you, or crack a Lumen Flare.",
              "The cage closes in but turns as it shrinks: run for a gap, never for the middle.",
              "In the last phase three Null Anchors shield it - break every one, then strike while it is staggered."]),
    "solareon": dict(
        theme="Light", rank="Sovereign of Dawn - Tier 5 stage 2",
        lore="Solareon is the sun that was never allowed to set. It judges, and its judgements are always correct, always "
             "immediate and always bright enough to be seen from the next dimension.",
        summon="Craft a Radiant Sigil and use it on a Ritual Altar at midday, under open sky. The ritual costs twelve levels of experience.",
        tips=["Beams stop at terrain: break line of sight, or jump the low ones. Sweeps move at a fixed speed - outrun them.",
              "The flash only catches those who look at it: turn away the moment you see the white swell.",
              "A supernova cannot burn what it cannot see. Get behind solid cover or beyond the marked ring - then punish the exhausted Seraph.",
              "Halo Blades charge in packs. Clear them before the spinning rays begin.",
              "Its mirror stance throws damage back at you: wait it out."]),
    "omnarch": dict(
        theme="Everything", rank="The Sundered God - Tier 5 stage 3",
        lore="Everything the Weaver was, and everything the Weaver broke. The Omnarch wears each Sovereign's power like a "
             "borrowed coat and does not always remember whose it is.",
        summon="Craft an Omni Sigil from one core of every Sovereign and use it on a Ritual Altar. The ritual costs thirty levels of experience "
               "and eight hearts of health. There is no second attempt until it is finished.",
        tips=["Phase one borrows fire, storm and blood - fight it like the first three.",
              "Phase two borrows time, void and light: time stops and rewinds, black holes open, beams sweep low at ankle height - jump them.",
              "Phase three spins rays, closes cages (there is always a gap) and drops two black holes at once.",
              "In the last phase four Apex Pylons shield it and the ground burns in patches: shatter the pylons, and never stand still.",
              "Its enrage timer is real. Bring your best gear and your best potions."]),
}

BOSSES.update({
    "boreus": dict(
        theme="Frost", rank="Sovereign of Winter - Tier 6 stage 1",
        lore="Boreus was the first cold: the quiet before anything had a name. It does not hate the warm. It only wants it to stop.",
        summon="Craft a Rime Sigil and use it on a Ritual Altar. The ritual costs thirty levels of experience and sixteen blocks of Blue Ice.",
        tips=["Cracks on the floor mark ice spikes: stand between two of them, and expect a second wave to fill the gaps.",
              "In the whiteout your screen frosts over while you stand still - keep moving, sprint-jump in circles, never stop to heal.",
              "The deep freeze roots everyone it catches and spears fall on them: jump the wave as it snaps shut, or be outside its ring.",
              "In the third phase three Rime Anchors shield it - shatter them all, then strike while it is staggered.",
              "A frozen enemy is a shattered enemy: Rimebrand's Shatter triples a blow against anything frozen."]),
    "morbane": dict(
        theme="Plague", rank="Sovereign of Rot - Tier 6 stage 2",
        lore="Morbane is what grows where something stopped. It does not kill - it is patient - it only makes sure that whatever you were becomes something else.",
        summon="Craft a Blight Sigil and use it on a Ritual Altar at night. The ritual costs six hearts of health and eight Fermented Spider Eyes.",
        tips=["Pods land in red-ringed circles and become poison clouds for twelve seconds. Never fight in one.",
              "An infestation marks everyone: step into water or drink milk before the fuse runs out, or it bursts and calls pods.",
              "Its leech beam breaks when you step behind cover; kill the pods it calls before they reach you.",
              "In the third phase three Blight Hearts shield it. Shatter them, then burst it down while it is staggered.",
              "Contagion spreads: kill one infected enemy and the plague finds everything near it."]),
    "astraeus": dict(
        theme="Cosmos", rank="Sovereign of Stars - Tier 6 stage 3",
        lore="Astraeus has counted every star and found the number unacceptable. It means to correct it.",
        summon="Craft an Astral Sigil and use it on a Ritual Altar in the End. The ritual costs forty levels of experience and two End Crystals.",
        tips=["Worlds circle it and swing outward: stay inside the ring (close to it) or beyond its edge.",
              "A constellation lays walls of light between its stars: find the open cell and stand in it.",
              "Comets fall in marked circles faster than meteors - do not stand still.",
              "In the third phase four Astral Nodes shield it; break them all and it hangs exhausted.",
              "Flight helps: a Starborn set or a Starwing Pendant lets you stay clear of the floor-based attacks."]),
    "eternus": dict(
        theme="Every Colour", rank="The Final Thread - Tier 6 stage 4",
        lore="The last thread, tied in a ring so it could never be cut: a serpent that ate its own tail so that nothing would ever end. It has been patient. It is not now.",
        summon="Craft a Thread Sigil from the cores of the three older Sovereigns and use it on a Ritual Altar. The ritual costs fifty levels of experience, ten hearts of health and two Nether Stars.",
        tips=["Its threads reach for you and haul you in: break line of sight behind cover and a thread cannot take hold.",
              "Each phase wears another Sovereign's power: fight it like the fight it borrows from.",
              "In the fourth phase it fights with winter, rot and the stars together - keep moving, keep a way out.",
              "In the last phase four Loom Spindles shield it. Shatter them, and never stand still.",
              "Its enrage timer is real. Bring your best gear, your best potions - and a Second Wind."]),
})

TIPS = [  # (tier that reveals it, text)
    (1, "Press J to open this Codex at any time. Tier 1 gear and the gear of every Sovereign you beat is listed, faded until you own a piece; everything else stays hidden until you find, meet or beat it."),
    (1, "Long pages scroll with the mouse wheel, or grab the thin bar at the page edge and drag."),
    (1, "Sneak while mining or chopping to dig a single block instead of an area or a whole tree."),
    (1, "Right-click abilities show a cooldown beside the hotbar. Sneak + right-click uses a weapon's second ability."),
    (1, "Press K to open your accessory slots. Charms and rings in them work without being held, and their bonuses add up."),
    (1, "Armor sets grant bonuses when all four pieces are worn. Hover any piece to see its set bonus."),
    (1, "Items from a later tier can be picked up and carried, but stay inert until you defeat the matching boss. The tooltip says which."),
    (1, "Verdite ore generates in ordinary stone. Smelt it, then craft the pickaxe: three-by-three mining changes everything."),
    (1, "Sensitive to flashing light or shaking? Set screenFlashStrength and screenShakeStrength to 0 (or reducedEffects to true) in the mod's config file."),
    (2, "Anvils no longer say Too Expensive. Combine books to push enchantments all the way to level ten."),
    (2, "Heart Crystals permanently add hearts, up to a cap that rises with each tier."),
    (2, "A Recall Mirror takes you to your bed or the world spawn if you stand still for three seconds."),
    (2, "Rare ruined shrines stand in the Overworld and on the outer End islands: a Ritual Altar, lantern pillars and a chest of tomes beyond the enchanting table's reach."),
    (3, "Bosses never despawn and cannot be cheesed from a pillar or a hole: prepare an open arena and healing."),
    (3, "Ritual Altars summon a Sovereign when you hold its sigil. Check the Bosses tab for each summoning rule."),
    (3, "Wandering traders now stock tomes of Sharpness, Protection, Fortune and more, at levels vanilla never allows."),
    (4, "Nihilus devours light: carry torches or Lumen Flares to the End, and place them where you plan to fight."),
    (4, "Invaders appear with every tier you clear. Vanilla monsters grow stronger too, so keep your armor current."),
    (4, "Every Sovereign leaves a trophy in its treasure bag. Set it on any block as proof of the kill; the Solareon and Ignarok trophies even glow."),
    (5, "The Omnarch's arena collapses in its last phase. Never stand still and never fight at the edge."),
    (6, "Tier 6 gear is made from Eternium: the creatures of Tier 6 drop shards, and four shards press into an ingot. Each Sovereign's hoard holds ingots, cores and its heart."),
    (6, "Flying changes the fights: a Starborn set or a Starwing Pendant gives you survival flight, and falls do no harm while it is worn."),
    (6, "Frozen enemies shatter for triple damage, an infected enemy's death spreads the plague, and a falling star lands wherever you hit - build around one theme."),
    (6, "A Titan Heart adds one more heart, up to twenty in all. Every Sovereign's hoard may hold one."),
    (5, "Beaten all seven? /usm bossrush start calls every Sovereign in a row, no altars needed, for Omni Cores, a Nether Star and an advancement."),
]

# short "where do I get this" lines for items that have no crafting recipe
OBTAIN = {
    "raw_verdite": "Mined from verdite ore, found in ordinary stone between y -24 and 72.",
    "raw_azurite": "Mined from azurite ore deep underground (y -64 to -4). Needs a diamond-tier pickaxe or better.",
    "drakescale": "Dropped by the Ender Dragon as it dies. It can be farmed by respawning the dragon.",
    "eternium_shard": "Dropped by the creatures of Tier 6 (frost, plague and stars) and by their invasions. Four press into an Eternium Ingot.",
}
