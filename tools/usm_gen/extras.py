"""The extras around the fights: boss trophies, the wandering trader's tomes, sovereign shrines and the advancement tree."""
from . import items as I

# gate id -> (display name, palette, lore)
TROPHIES = {
    "ender_dragon": ("Trophy of the Ender Dragon", "drake", "Proof that the oldest guardian can be beaten."),
    "ignarok": ("Trophy of Ignarok", "cinder", "Still warm. Do not leave it near the curtains."),
    "voltaris": ("Trophy of Voltaris", "storm", "Hums faintly during thunderstorms."),
    "hemorath": ("Trophy of Hemorath", "blood", "It looks like it is smiling. It is not."),
    "chronos": ("Trophy of Chronos", "chrono", "Always shows the wrong time. Always the same wrong time."),
    "nihilus": ("Trophy of Nihilus", "void", "The torch next to it never quite reaches it."),
    "solareon": ("Trophy of Solareon", "radiant", "Cannot be looked at for long. Cannot be looked away from either."),
    "omnarch": ("Trophy of the Omnarch", "apex", "Two halves, one pedestal, no argument."),
    "boreus": ("Trophy of Boreus", "frost", "Never melts. Keeps the room two degrees colder than it should be."),
    "morbane": ("Trophy of Morbane", "plague", "Something is growing on it. It was not there yesterday."),
    "astraeus": ("Trophy of Astraeus", "cosmic", "Shows a different sky every night. None of them are ours."),
    "eternus": ("Trophy of Eternus", "eternal", "Turn it around and the other side is the first side."),
}


def register_trophies():
    for gid, (name, pal, lore) in TROPHIES.items():
        I.block("trophy_" + gid, "trophy", dict(pal=pal, emblem=gid), 2.0, 6.0, tool="none", name=name, lore=lore, gate=gid, light=6 if gid in ("solareon", "ignarok", "astraeus", "eternus") else 0)


# the wandering trader sells high-level books for emeralds (and a diamond or two): (enchantment, level, emeralds, diamonds)
BOOK_TRADES = [
    ("sharpness", 7, 36, 3), ("protection", 6, 36, 3), ("efficiency", 7, 30, 2), ("unbreaking", 6, 24, 2), ("fortune", 5, 40, 4),
    ("looting", 5, 40, 4), ("power", 7, 30, 2), ("feather_falling", 6, 24, 1), ("sweeping_edge", 6, 28, 2), ("thorns", 5, 30, 2),
    ("respiration", 5, 24, 1), ("depth_strider", 5, 24, 1), ("lure", 5, 20, 1), ("luck_of_the_sea", 5, 24, 2),
]

# what a sovereign shrine's chest may hold: (item or "book:<enchant>:<level>", min, max, weight)
SHRINE_LOOT = [
    ("minecraft:diamond", 1, 4, 14), ("minecraft:emerald", 4, 12, 16), ("minecraft:gold_ingot", 4, 14, 14), ("minecraft:experience_bottle", 3, 9, 12),
    ("minecraft:golden_apple", 1, 2, 8), ("minecraft:obsidian", 4, 10, 8), ("book:sharpness:7", 1, 1, 3), ("book:protection:6", 1, 1, 3),
    ("book:efficiency:7", 1, 1, 3), ("book:unbreaking:6", 1, 1, 3), ("book:mending:1", 1, 1, 3), ("minecraft:ender_pearl", 2, 5, 6),
    ("minecraft:netherite_scrap", 1, 2, 2),
]

# advancements: id -> (parent, icon item, frame, title, description, how it is earned: "tick" | "code" | an item id)
ADVANCEMENTS = [
    ("root", None, "ultimatesurvival:codex", "task", "Ultimate Survival", "The Sovereigns are waking. Read your Codex.", "tick"),
    ("verdite", "root", "ultimatesurvival:verdite_ingot", "task", "A Green Start", "Smelt your first Verdite Ingot.", "ultimatesurvival:verdite_ingot"),
    ("ender_dragon", "verdite", "ultimatesurvival:drakescale", "goal", "The First Seal", "Defeat the Ender Dragon and open the second age.", "code"),
    ("ignarok", "ender_dragon", "ultimatesurvival:ember_core", "goal", "Ashes to Ashes", "Defeat Ignarok, the Cinder Warden.", "code"),
    ("voltaris", "ignarok", "ultimatesurvival:storm_core", "goal", "Ride the Lightning", "Defeat Voltaris, the Storm Regent.", "code"),
    ("hemorath", "voltaris", "ultimatesurvival:blood_core", "goal", "The Hunt Ends", "Defeat Hemorath, the Crimson Matriarch.", "code"),
    ("chronos", "hemorath", "ultimatesurvival:chrono_core", "goal", "Out of Time", "Defeat Chronos, the Hourless.", "code"),
    ("nihilus", "chronos", "ultimatesurvival:void_core", "goal", "Into the Absence", "Defeat Nihilus, Devourer of Light.", "code"),
    ("solareon", "nihilus", "ultimatesurvival:radiant_core", "goal", "The Sun Sets", "Defeat Solareon, the Radiant Seraph.", "code"),
    ("omnarch", "solareon", "ultimatesurvival:omni_core", "challenge", "Mend the Sundering", "Defeat the Omnarch, the Sundered God.", "code"),
    ("apex_arsenal", "omnarch", "ultimatesurvival:apex_blade", "challenge", "Everything, Sharpened", "Hold the Apex Blade.", "ultimatesurvival:apex_blade"),
    ("boreus", "omnarch", "ultimatesurvival:frost_core", "goal", "Winter Has Come", "Defeat Boreus, the Rime Tyrant.", "code"),
    ("morbane", "boreus", "ultimatesurvival:blight_core", "goal", "Quarantine", "Defeat Morbane, the Blightlord.", "code"),
    ("astraeus", "morbane", "ultimatesurvival:star_core", "goal", "Nothing Left to Count", "Defeat Astraeus, the Star-Eater.", "code"),
    ("eternus", "astraeus", "ultimatesurvival:weave_core", "challenge", "The Last Thread", "Defeat Eternus, the Final Thread.", "code"),
    ("thread_arsenal", "eternus", "ultimatesurvival:threadcutter", "challenge", "Beyond Everything", "Hold the Threadcutter.", "ultimatesurvival:threadcutter"),
    ("titan_hearts", "omnarch", "ultimatesurvival:titan_heart", "goal", "Titan", "Eat a Titan Heart.", "ultimatesurvival:titan_heart"),
    ("boss_rush", "omnarch", "ultimatesurvival:trophy_omnarch", "challenge", "Gauntlet", "Clear a whole Boss Rush.", "code"),
    ("codex_complete", "root", "ultimatesurvival:codex", "challenge", "Nothing Left Unknown", "Fill every page of the Codex.", "code"),
    ("heart_crystals", "ender_dragon", "ultimatesurvival:heart_crystal", "goal", "Many Hearts", "Eat a Heart Crystal.", "ultimatesurvival:heart_crystal"),
    ("tome", "verdite", "minecraft:enchanted_book", "goal", "Beyond the Table", "Own an enchanted book of a level vanilla never allowed.", "code"),
]


# enchanted books a boss's hoard may also hold - every one is a level vanilla would never give you (item, chance)
TOMES = {
    "ignarok": [("book:fire_protection:7", 0.6), ("book:fire_aspect:4", 0.5)],
    "voltaris": [("book:efficiency:8", 0.55), ("book:power:8", 0.55)],
    "hemorath": [("book:sharpness:8", 0.55), ("book:looting:6", 0.5)],
    "chronos": [("book:unbreaking:8", 0.55), ("book:mending:1", 0.6)],
    "nihilus": [("book:protection:8", 0.55), ("book:feather_falling:8", 0.5)],
    "solareon": [("book:fortune:6", 0.55), ("book:sweeping_edge:7", 0.5)],
    "omnarch": [("book:sharpness:10", 0.6), ("book:protection:10", 0.6), ("book:efficiency:10", 0.6), ("book:unbreaking:10", 0.6)],
    "boreus": [("book:fortune:10", 0.6), ("book:looting:10", 0.6)],
    "morbane": [("book:power:10", 0.6), ("book:sweeping_edge:10", 0.6)],
    "astraeus": [("book:fire_aspect:10", 0.55), ("book:knockback:10", 0.55)],
    "eternus": [("book:sharpness:10", 0.7), ("book:protection:10", 0.7), ("book:efficiency:10", 0.7), ("book:unbreaking:10", 0.7), ("book:mending:1", 1.0)],
}


def finish():
    """Adds every boss's trophy and tomes to its treasure bag (the Ender Dragon's trophy is dropped by the dragon hook in Java)."""
    from . import mobs as MB
    for gid in TROPHIES:
        if gid == "ender_dragon":
            continue
        bag = MB.get(gid).extra["bag"]
        bag.append(MB.drop("trophy_" + gid, 1, 1))
        for item, chance in TOMES.get(gid, []):
            bag.append(MB.drop(item, 1, 1, chance))
