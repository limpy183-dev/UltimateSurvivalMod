#!/usr/bin/env python3
"""Writes README.md from the same content tables the game is generated from, so the tier/boss table, item lists, balance table
and config reference can never drift from the mod.

    python tools/gen_readme.py [--checks N]      # N = number of in-game checks of the last full game-test run (optional)"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from usm_gen import balance, items as I, mobs as MB, abilities as AB       # noqa: E402
from usm_gen.gates import GATES                                            # noqa: E402
import usm_gen.content_t1, usm_gen.content_t2, usm_gen.content_t3, usm_gen.content_t4, usm_gen.content_t5   # noqa: E402,F401,E401
import usm_gen.content_t6, usm_gen.content_extra                           # noqa: E402,F401,E401
import usm_gen.content_mobs as CMOBS                                       # noqa: E402
from usm_gen import extras as EX, power as POWER, particles as PART       # noqa: E402

POWER.apply()                                                              # the same steps gen_all.py takes after the content modules
EX.register_trophies()                                                     # the same steps gen_all.py takes after the content modules
EX.finish()

TIER_TITLE = {1: "Tier 1 - Early", 2: "Tier 2 - Mid", 3: "Tier 3 - High", 4: "Tier 4 - Late", 5: "Tier 5 - Endgame", 6: "Tier 6 - Transcendent"}
GATE_NAME = {g["id"]: g["name"] for g in GATES}
ATTR_NAMES = {"max_health": "max health", "attack_damage": "attack damage", "attack_speed": "attack speed", "movement_speed": "movement speed",
              "armor_toughness": "armor toughness", "armor": "armor", "luck": "luck"}


def ability_cell(item):
    out = []
    for a in item.abilities:
        if a["id"] == "bow_shot":
            continue
        kind, title, text, cd = AB.describe(a)
        out.append("**%s** - %s%s" % (title, text, (" _(cd %gs)_" % cd) if cd else ""))
    return "<br>".join(out)


def attr_cell(attrs):
    parts = []
    for a in attrs or []:
        v = a["amount"]
        op = a.get("op", "add")
        parts.append("%+g%s %s" % (v * 100 if op == "mul_base" else v, "%" if op == "mul_base" else "", ATTR_NAMES.get(a["attr"], a["attr"])))
    return ", ".join(parts)


def stat_cell(it):
    s = it.stats
    if it.kind in ("sword", "staff"):
        return "%g dmg, %.1f/s, reach %+g, %d dur" % (s["damage"], s["speed"], s["reach"], s["durability"])
    if it.kind == "bow":
        shot = next((a for a in it.abilities if a["id"] == "bow_shot"), None)
        return ("%g dmg bolt, pierce %d, no ammo" % (shot["dmg"], shot.get("pierce", 0)) if shot else "arrow bow") + ", %d dur" % s["durability"]
    if it.kind in ("pickaxe", "axe", "shovel", "hoe"):
        traits = []
        if s.get("area"):
            traits.append("%dx%d x%d deep" % (2 * s["area"] + 1, 2 * s["area"] + 1, s.get("depth", 1)))
        if s.get("hoe_area"):
            traits.append("%dx%d field" % (2 * s["hoe_area"] + 1, 2 * s["hoe_area"] + 1))
        for k, label in (("vein", "vein %d"), ("fell", "fells trees"), ("smelt", "auto-smelt"), ("magnet", "magnet"), ("replant", "replants")):
            if s.get(k):
                traits.append(label % s[k] if "%d" in label else label)
        return "mining %g, %d dur; %s" % (s["mine"], s["durability"], ", ".join(traits))
    if it.kind == "armor":
        return "%d armor, %g toughness" % (s["armor"], s["toughness"])
    return ""


def attack_types():
    """Number of boss/mob attack types registered in the Java attack library."""
    names = set()
    for f in (ROOT / "src/main/java/com/ultimatesurvival/mob/attack").glob("*.java"):
        names |= set(re.findall(r"(?<![\w.])(?:TYPES|t)\.put\(\"([a-z_]+)\"", f.read_text(encoding="utf-8")))
    return len(names)


def armor_cut(dmg, armor, tough):
    """Vanilla armor formula: the fraction of a hit that gets through."""
    return 1 - min(20.0, max(armor / 5.0, armor - dmg / (2.0 + tough / 4.0))) / 25.0


def balance_rows():
    rows = []
    for gid, b in balance.BOSS_STATS.items():
        g = balance.GATE_STATS[gid]
        out = []
        for arch in ("sword", "greatsword"):
            d = balance.weapon_dmg(gid, arch)
            dps = d * armor_cut(d, b["armor"], 0) * balance.ARCHETYPE[arch]["spd"]
            out.append("%.0f s" % (b["hp"] / dps))
        taken = b["dmg"] * armor_cut(b["dmg"], g["armor"], g["tough"] * 4)
        rows.append((GATE_NAME[gid], b["hp"], b["armor"], out[0], out[1], b["dmg"], "%.0f" % taken, "%d" % -(-40 // taken)))
    return rows


def table(head, rows):
    out = ["| " + " | ".join(head) + " |", "|" + "|".join("---" for _ in head) + "|"]
    out += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return "\n".join(out)


def config_reference():
    src = (ROOT / "src/main/java/com/ultimatesurvival/config/UsmConfig.java").read_text(encoding="utf-8")
    rows, doc = [], ""
    for line in src.splitlines():
        t = line.strip()
        m = re.match(r"/\*\*\s*(.*?)\s*\*/", t)
        if m:
            doc = m.group(1)
            continue
        f = re.match(r"public (double|int|boolean) (\w+) = ([^;]+);", t)
        if f:
            rows.append(("`%s`" % f.group(2), "`%s`" % f.group(3).replace("_", ""), doc))
            doc = ""
    return table(["Option", "Default", "Meaning"], rows)


def drop_name(item):
    """'book:sharpness:8' -> 'Sharpness VIII tome'; 'minecraft:nether_star' -> 'nether star'."""
    if item.startswith("book:"):
        _, ench, level = item.split(":")
        roman = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"][int(level)]
        return "%s %s tome" % (ench.replace("_", " ").title(), roman)
    return item.split(":")[-1].replace("_", " ")


def boss_section(m):
    g = next(x for x in GATES if x["id"] == m.id)
    e = m.extra
    r = e["ritual"] or {}
    cost = []
    if r.get("dim"):
        cost.append("dimension `%s`" % r["dim"].split(":")[-1])
    if r.get("time", "any") != "any":
        cost.append({"night": "at night", "noon": "around midday", "day": "by day"}[r["time"]])
    if r.get("sky"):
        cost.append("under open sky")
    if r.get("xp"):
        cost.append("%d levels of experience" % r["xp"])
    if r.get("hearts"):
        cost.append("%d hearts of health" % r["hearts"])
    for it in r.get("items", []):
        cost.append("%d x %s" % (it["count"], it["item"].split(":")[-1].replace("_", " ")))
    st = m.stats
    lines = ["#### %s - %s\n" % (m.name, e.get("title", "")),
             "Tier %d | **%d HP**, %d armor, base damage %d | theme `%s` | summon: `%s`%s | XP %d\n" % (
                 m.tier, st["hp"], st["armor"], st["dmg"], e.get("style", ""), e.get("sigil") or "vanilla ritual",
                 (" - " + ", ".join(cost)) if cost else "", e.get("xp", 0))]
    for n, ph in enumerate(e["phases"], 1):
        kinds = []
        for a in ph["attacks"]:
            k = a["type"]
            kinds.append(k if k not in kinds else None)
        names = [k for k in kinds if k]
        enter = [a["type"] for a in ph.get("enter", [])]
        extra = []
        if ph.get("nodes"):
            extra.append("shield: %d x `%s`" % (ph["nodes"]["count"], ph["nodes"]["mob"]))
        if enter:
            extra.append("opens with `%s`" % "`, `".join(enter))
        lines.append("- **Phase %d - %s** (from %d%% HP): `%s`%s" % (n, ph["name"], round(ph["at"] * 100), "`, `".join(names),
                                                                   ("; " + "; ".join(extra)) if extra else ""))
    lines.append("- Drops: " + ", ".join("%s x%s" % (drop_name(d["item"]), d["min"] if d["min"] == d["max"] else "%d-%d" % (d["min"], d["max"]))
                                         + (" (%d%%)" % round(d["chance"] * 100) if d["chance"] < 1 else "") for d in e["bag"]) + "\n")
    return "\n".join(lines)


def main():
    checks = None
    if "--checks" in sys.argv:
        checks = int(sys.argv[sys.argv.index("--checks") + 1])
    gate_order = [g["id"] for g in GATES]
    out = []
    w = out.append

    w("# Ultimate Survival\n")
    w("A singleplayer Fabric mod for **Minecraft 26.2** (Java 25, Mojang mappings). It turns the whole survival game into a six-tier progression: eleven custom "
      "bosses (plus the Ender Dragon) each unlock the next band of absurdly strong gear, tools, armor sets and accessories, while everything you have not earned "
      "yet stays inert. Every texture, model, rig, animation and recipe is generated by the Python scripts in `tools/` - nothing is borrowed.\n")
    w("**%d items - %d blocks - %d armor sets - %d mobs and bosses - %d attack types - %d abilities.**\n" % (
        len(I.ITEMS), len(I.BLOCKS), len(I.SETS), len(MB.MOBS), attack_types(), len(AB.CATALOG)))

    w("## Highlights\n")
    w("- **Six tiers, thirteen gates.** Tier 1 from the start; Tier 2 after the Ender Dragon; Tier 3 after Ignarok; Tier 4 in three stages (Voltaris, Hemorath, Chronos); "
      "Tier 5 in three stages (Nihilus, Solareon, the Omnarch); **Tier 6 (Transcendence) in four stages: Boreus the Rime Tyrant, Morbane the Blightlord, Astraeus the Star-Eater and "
      "Eternus, the Final Thread.** Gear of a gate you have not cleared can be carried, but has no damage, armor, mining power, "
      "abilities or set bonuses; its tooltip names the boss that unlocks it. Progress is saved with the world.")
    w("- **Faster, harder-hitting, bigger.** The v2 power pass raised weapon damage to roughly 1.4-1.9x, every ability's damage 1.6x and its blast 1.2x, made weapons swing 35% faster, "
      "abilities recharge 35% sooner, bows draw 30% faster, furnaces cook twice as fast and Sovereigns emerge and fall 35% sooner, and scaled every particle effect up 1.6x (with a cast "
      "flourish on every ability and sparks on every hit). Tier 6 sits far above the old endgame: Eternium tools mine a 13x13 face eight deep, armor grants flight, a death that does not "
      "take, stars that circle you; weapons freeze, infect, rain meteors and call beams from orbit.")
    w("- **Eleven bosses with real fights.** Custom 3D rigs with animations, boss bars, 3-5 HP-threshold phases, telegraphed attack patterns, minions, shield "
      "pylons, vulnerability windows, reflect stances, black holes, an eclipse that rots anyone standing in the dark, spinning beams, supernovas, collapsing arenas - and, in Tier 6, a whiteout that punishes standing still, "
      "an infection you must wash off, worlds that orbit the arena, constellations of light, and threads that haul you in. "
      "They cannot be cheesed from a pillar or hole, never despawn, and reset when abandoned. Each is summoned with a craftable sigil on a Ritual Altar.")
    w("- **%d regular mobs** with natural spawn rules in the Overworld, the Nether and the End, plus **tier-scaled invasions** and vanilla hostiles that grow with each tier." % len(
        [m for m in MB.MOBS if m.kind == "mob"]))
    w("- **Tools that change mining:** 3x3 -> 13x13 faces (Eternium: eight deep), felling axes, vein mining, auto-smelting, magnets, area hoes that replant.")
    w("- **Weapons with abilities** (right-click, sneak+right-click for the second, on-hit and on-kill effects, HUD cooldowns), energy bows, armor sets with set "
      "bonuses, **6 accessory slots** of their own, consumables and permanent Heart Crystals.")
    w("- **Raised limits (all configurable):** enchantments up to level 10 including in anvils (no *Too Expensive!*), stack sizes up to 99, max health above 1024.")
    w("- **The Codex of the Sundering** (press `J`): a custom book GUI with Sovereigns, Arsenal, Bestiary, Progress and Tips tabs; Tier 1 gear is listed from "
      "the start (faded until you own a piece, recipe and all) and every other entry reads `???` until you find, meet or beat what it describes; live 3D previews of every "
      "mob playing its attacks; every scrollbar can be grabbed and dragged.")
    w("- **Extras:** a Boss Rush mode, boss trophies, rare Sovereign shrines in the world, %d advancements, wandering-trader tomes, a theme per boss (music, bar, "
      "sounds, particles) and screen shake - see *Extras* below." % len(
        json.loads((ROOT / "src/main/resources/data/ultimatesurvival/usm/content.json").read_text(encoding="utf-8"))["advancements"]))
    w("- Everything is tunable from one JSON file, with reduced-effects, screen-shake and screen-flash switches.\n")

    w("## Extras\n")
    w("- **Boss Rush** - `/usm bossrush start` (`stop` calls it off): all eleven Sovereigns one after another around you, no altars or rituals, a ten-second breather "
      "and a partial heal between fights. Dying, leaving the arena or changing dimension ends the run. Kills count as normal progression; finishing pays 2 Omni Cores, "
      "2 Weave Cores, a Titan Heart, a Nether Star, 3 Enchanted Golden Apples and 6000 XP, and earns an advancement.")
    w("- **Trophies** - every Sovereign (and the Ender Dragon) leaves a placeable trophy block; the glowing ones give light.")
    w("- **Sovereign shrines** - rare ruined platforms with a Ritual Altar, four lantern pillars and a loot chest (including enchanted books above the vanilla maximum), "
      "in the Overworld and on the outer End islands. Only built on flat, dry ground.")
    w("- **Advancements** - %d: a root, one per gate, and goals for the Apex arsenal, the Threadcutter, Heart Crystals, Titan Hearts, a level-ten tome, a complete Codex and the Boss Rush." % len(
        json.loads((ROOT / "src/main/resources/data/ultimatesurvival/usm/content.json").read_text(encoding="utf-8"))["advancements"]))
    w("- **Wandering trader** - sells %d enchanted tomes above the vanilla maximum (Sharpness VII, Protection VI, Fortune V ...)." % len(
        list((ROOT / "src/main/resources/data/ultimatesurvival/villager_trade").glob("*.json"))))
    w("- **A theme per boss** - its own boss-bar style, battle music (a vanilla track re-pitched under a mod sound event), roar, warning and impact sounds, "
      "signature particles (%d custom particle types, art by `tools/usm_gen/particles.py`)" % len(PART.NAMES) + ", and camera shake on heavy hits.")
    w("- **Set ambience** - a full armor set puts on a faint show of its own (leaves, cloud, embers, sparks, blood, ticking motes, void, light, all of them at once), and the eyes, runes and sigils of every set from Azurite up are a full-bright layer: they shine in the dark.")
    w("- **Screen flash** - the biggest moments (a Sovereign awakening, changing phase or dying, a ritual's lightning, a supernova, a blinding flash, a Boss Rush win) wash the "
      "screen in colour for a moment. Gentle by design: never fully opaque, at most one per second, and off with `reducedEffects` or `screenFlashStrength: 0` (set it if you are "
      "sensitive to flashing light).")
    w("- **Treasure-bag beacon** - a fallen Sovereign's bag glows and sends a column of sparks into the sky until you pick it up.")
    w("- **Swing animations** - every weapon family swings its own way: daggers, rapiers, spears and staves thrust; axes, scythes, greatswords and hammers sweep in long, slow arcs.")
    w("- **Effects budget** - every spectacle is capped by `maxParticlesPerEffect`; `reducedEffects` cuts particles to a trickle and skips flashes and shake, `screenShakeStrength` scales (or silences) the shake.")
    w("- **Recipe viewers** - every recipe is a vanilla recipe JSON, so REI/EMI/JEI list them with no extra integration; no viewer-specific plugin is shipped.\n")

    w("## Build and run\n")
    w("```\n./gradlew build               # compile + jar (build/libs)\n./gradlew runClient            # play it\n"
      "./gradlew runClientGameTest    # boot the real client and run the whole automated in-game suite (~25 min); USM_ONLY=soak = natural boss fights (~25 min), USM_ONLY=worldgen = ore/shrine generation\n"
      "python tools/gen_all.py        # regenerate every texture / model / rig / recipe / lang file and the content manifest (needs Pillow)\n"
      "python tools/gen_readme.py     # regenerate this file\n```\n")
    w("Requirements: JDK 25, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.2. The Gradle build never needs Python - the generated files are committed.\n")
    w("**Installing the jar:** run `./gradlew build`, then copy `build/libs/ultimate-survival-%s.jar` and Fabric API (0.161.0+26.2) into the `mods` folder of a Fabric 26.2 "
      "profile (Fabric Loader 0.19.5+, Java 25). Singleplayer only: the mod is client-side and runs on the integrated server.\n"
      % re.search(r"^version=(\S+)", (ROOT / "gradle.properties").read_text(), re.M).group(1))

    w("## Progression, bosses and tiers\n")
    rows = []
    for g in GATES:
        if g["boss"] is None:
            rows.append((1, g["name"], "-", "-", "Verdite tools, weapons, armor, accessories"))
            continue
        bid = g["id"]
        m = next((x for x in MB.MOBS if x.id == bid), None)
        if m:
            e = m.extra
            rows.append((g["tier"], g["name"], "%d HP / %d armor / %d dmg" % (m.stats["hp"], m.stats["armor"], m.stats["dmg"]),
                         "`%s` (%s)" % (e.get("sigil"), e.get("style")), ", ".join(sorted({x.name for x in I.ITEMS if x.gate == bid and x.kind != "material"})[:0]) or
                         "%d new items" % len([x for x in I.ITEMS if x.gate == bid])))
        else:
            rows.append((g["tier"], g["name"], "vanilla (200 HP, scaled by your tier)", "End crystals", "%d new items" % len([x for x in I.ITEMS if x.gate == bid])))
    w(table(["Tier", "Gate / boss", "Stats", "Summon", "Unlocks"], rows))
    w("\n`/usm status` shows where you are. Items unlock per boss, so inside Tiers 4 and 5 each boss opens its own branch of gear; the \"current tier\" is "
      "the highest cleared gate's tier and drives invasions and vanilla-mob scaling.\n")

    w("### Boss compendium\n")
    for m in MB.MOBS:
        if m.kind == "boss":
            w(boss_section(m))
    w("The Ender Dragon is the Tier 2 gate: killing it (vanilla fight, vanilla ritual) opens Tier 2 and drops drakescale.\n")

    w("## Items by gate\n")
    for gid in gate_order:
        group = [x for x in I.ITEMS if x.gate == gid and not x.hidden]
        blocks = [b for b in I.BLOCKS if b.gate == gid]
        if not group and not blocks:
            continue
        t = balance.GATE_STATS[gid]["tier"]
        w("### %s - %s\n" % (TIER_TITLE[t], GATE_NAME[gid]))
        for kind_title, kinds in (("Tools", ("pickaxe", "axe", "shovel", "hoe")), ("Weapons", ("sword", "staff", "bow", "gadget")), ("Armor", ("armor",)),
                                  ("Accessories", ("accessory",)), ("Consumables", ("consumable",)), ("Materials, sigils and bags", ("material", "sigil", "bag"))):
            sub = [x for x in group if x.kind in kinds]
            if not sub:
                continue
            w("**%s**\n" % kind_title)
            if kind_title in ("Accessories",):
                w(table(["Item", "Bonuses"], [(x.name, "; ".join(filter(None, [attr_cell(x.extra.get("attrs")), ability_cell(x)]))) for x in sub]))
            elif kind_title == "Consumables":
                w(table(["Item", "Effect"], [(x.name, ability_cell(x)) for x in sub]))
            elif kind_title.startswith("Materials"):
                w(table(["Item", "Notes"], [(x.name, (x.obtain or x.lore)) for x in sub]))
            elif kind_title == "Armor":
                pieces = sorted({x.extra["set"] for x in sub})
                rows = []
                for st in (s for s in I.SETS if s["id"] in pieces):
                    first = next(x for x in sub if x.extra["set"] == st["id"])
                    bonus = "<br>".join("**%s** - %s" % (AB.describe(b)[1], AB.describe(b)[2]) for b in st["bonus"])
                    rows.append((st["name"], "%d armor in total, %g toughness per piece" % (balance.GATE_STATS[gid]["armor"], first.stats["toughness"]) +
                                 ("; " + attr_cell(first.extra.get("attrs")) + " per piece" if first.extra.get("attrs") else ""), bonus))
                w(table(["Set", "Defense", "Four-piece bonuses"], rows))
            else:
                w(table(["Item", "Stats", "Abilities"], [(x.name, stat_cell(x), ability_cell(x)) for x in sub]))
            w("")
        if blocks:
            w("**Blocks:** " + ", ".join("%s (%s)" % (b.name, b.kind) for b in blocks) + "\n")

    w("## Mobs\n")
    rows = []
    for m in MB.MOBS:
        if m.kind != "mob":
            continue
        sp = m.extra.get("spawn")
        where = "boss minion / invasion" if not sp else "%s%s%s" % ({"overworld": "Overworld", "nether": "Nether", "end": "End"}[sp["dim"]],
                                                                    ", night" if sp["night"] else "", (", after " + GATE_NAME[sp["gate"]]) if sp["gate"] else "")
        attacks = ", ".join(sorted({a["type"] for a in m.extra["attacks"]}))
        rows.append((m.name, m.tier, "%g" % m.stats["hp"], "%g" % m.stats["dmg"], where, attacks or "melee"))
    w(table(["Mob", "Tier", "HP", "Damage", "Appears", "Attacks"], rows))
    w("\nInvasions (every ~%d minutes per player, scaled by tier): %s.\n" % (10, "; ".join("tier %d - \"%s\"" % (i["tier"], i["title"]) for i in CMOBS.INVASIONS)))

    w("## Controls and commands\n")
    w(table(["Key / action", "What it does"], [
        ("`J`", "open the Codex of the Sundering (you receive it on first join; lost it? craft one from a book and redstone, or run `/usm codex`)"),
        ("`K`", "open your 6 accessory slots (their bonuses work without being held)"),
        ("Right-click", "use a weapon's first ability (cooldown shown beside the hotbar)"),
        ("Sneak + right-click", "use a weapon's second ability"),
        ("Sneak while mining/chopping", "break a single block instead of an area, vein or whole tree"),
        ("Jump in mid-air", "double jump (Azurite / Tempest / Voidwalker armor sets)"),
        ("Sigil on a Ritual Altar", "summon a Sovereign - each has its own dimension / time / sky / cost rule (see the Codex)"),
    ]))
    w("\n| Command (op level 2) | Effect |\n|---|---|")
    for c, e in (("`/usm status`", "tier, gates cleared, next gate"), ("`/usm unlock <1-6|boss|all>`", "clear that gate and every earlier one"),
                 ("`/usm lock <1-6|boss>`", "re-lock that gate and every later one"), ("`/usm reset`", "reset progression and discoveries"),
                 ("`/usm codex`", "(re)give the Codex"), ("`/usm discover <all|key>`", "reveal Codex entries")):
        w("| %s | %s |" % (c, e))
    w("")

    w("## Configuration\n")
    w("`config/ultimatesurvival.json` is written on first start and documents itself; unknown keys are dropped, missing keys are restored, out-of-range numbers are clamped "
      "to what the game can represent, and a file that is not valid JSON is ignored (defaults apply, the file is left for you to fix) instead of crashing the game.\n")
    w(config_reference())
    w("\nValues that shape registered items (`weaponDamageMultiplier`, `armorMultiplier`) apply on the next game start.\n")

    w("## Balance table\n")
    w("Every stat in the content tables is derived from this one table (`tools/usm_gen/balance.py`). Weapon damage = tier damage x archetype multiplier.\n")
    w(table(["Gate", "Tier", "Sword dmg", "Armor (set)", "Toughness/piece", "Tool durability", "Mining speed", "Enchantability"],
            [(GATE_NAME[g], v["tier"], v["dmg"], v["armor"], v["tough"], v["dur"], v["speed"], v["ench"]) for g, v in balance.GATE_STATS.items()]))
    w("\n" + table(["Archetype", "Damage x", "Speed", "Reach"], [(k, v["m"], v["spd"], "%+g" % v["reach"]) for k, v in balance.ARCHETYPE.items()]))
    w("\nBosses: " + "; ".join("%s %d HP / %d armor / %d dmg" % (k, v["hp"], v["armor"], v["dmg"]) for k, v in balance.BOSS_STATS.items()) +
      ". Attack numbers in the tables are multipliers of the boss's base damage, so `bossDamageMultiplier` rescales every fight at once.\n")
    w("### Balance note\n")
    w("Gear of the boss's own gate is meant to win the fight with room to spare, gear of the previous gate to win it narrowly. The estimates below use the vanilla "
      "armor formula with full-charge hits and nothing else: no enchantments, abilities, potions, accessories or Heart Crystals. Real fights run longer than the "
      "\"kill time\" (phase shields, vulnerability windows, reflect stances, dodging) and shorter than a bare sword would suggest (abilities and set bonuses). "
      "\"Hits to die\" is how many base-damage hits a 40 HP player in the matching armor set survives; most attacks are multiples of the base hit, which is why "
      "they are telegraphed. The Omnarch's heavy attacks can end an unprepared player in one or two hits - bring Heart Crystals and healing.\n")
    w(table(["Boss", "HP", "Armor", "Sword kill time", "Greatsword kill time", "Base hit", "Taken in matching armor", "Hits to die"], balance_rows()))
    w("\nTune everything at once with `bossHealthMultiplier`, `bossDamageMultiplier` and `weaponDamageMultiplier` in the config.\n")

    w("## Verification\n")
    w("What \"works\" means here: it was **run**, not just compiled. `./gradlew runClientGameTest` boots the real Minecraft 26.2 client, creates worlds, and drives the mod "
      "from the inside (server commands, the real server tick, the real renderer, screenshots reviewed by hand)." +
      (" The last full run executed **%d checks** with no failures." % checks if checks else "") + "\n")
    w("**Verified by running**\n")
    w("- **Progression**: gate locks (stats, abilities, armor, recipes, tooltips), `/usm` commands, saved and restored progress, a wounded boss surviving save-and-quit "
      "(health, phase, boss bar), raised enchant/stack/health limits, the anvil without *Too Expensive!*, the Ender Dragon gate.")
    w("- **Gear** (representative items of every family; the rest share the same code paths and differ only in numbers): area/vein/felling/smelt/magnet tools, weapon abilities and cooldowns, bows and staves, armor-set bonuses, the accessory slots, "
      "consumables, the Tier 3 fire kit and the Tier 5 kit (15 new abilities), swing animations.")
    w("- **The Codex**: opening, tabs, discovery (Tier 1 listed faded from the start, `???` for the rest until found), recipes, boss pages with live 3D previews, handout once, recovery, scrollbars dragged with the real mouse.")
    w("- **All eleven bosses**: rituals and their rules, every phase, every attack type forced and checked for what it should do (and not do: walls block beams, "
      "cages close, shields hold), minions, loot bags, trophies, gates, no-cheese rules, and **soak fights** - each boss run from first tick to last on its own AI against an "
      "unkillable player, checking that phases advance, every phase attacks, at least half of its attack library shows up unprompted, nothing is left behind, and the server keeps "
      "its tick budget.")
    w("- **The v2 power pass and Tier 6**: every older suite re-run against the new numbers (read from the content tables, never hard-coded); `T6Tests` - the global power numbers, crafting gates, the 13x13x8 "
      "Eternium pickaxe, every new ability (absolute zero + shatter, frostbite, blizzard, contagion spreading from corpse to corpse, plague burst, toxic cloud, orbital strike, meteor shower, starfall), "
      "the four armor sets, survival flight (and its removal), the star orbit, the frost aura, Second Wind, Omega Slash, Titan Hearts, the bows; `Boss4Tests` - for Boreus, Morbane, Astraeus and Eternus every new "
      "attack forced and checked for what it should and should not do (a gap between ice lines is safe, standing still in the whiteout is not, water washes off the infection, the pocket near the boss is safe from "
      "the planets, a wall breaks the threads), every phase, every shield, the hoards, the rituals and their costs; config files from before the power pass are migrated exactly once.")
    w("- **Mobs**: all %d regular mobs' attacks, natural spawn rules, tier invasions, vanilla-mob scaling." % len([m for m in MB.MOBS if m.kind == "mob"]))
    w("- **World**: in a world made with the ordinary generator, every ore (Verdite, Azurite, Stormite on peaks, Cinderite in the Nether, Aetherite in the End) really "
      "generates, in its dimension and height band; a Sovereign shrine builds on natural terrain.")
    w("- **Polish**: mod sound events resolve on both sides, boss themes start and stop, custom particles render, screen shake and flash (and their off switches), set "
      "ambience, advancements, trophies, wandering-trader tomes (a hundred traders rolled), Boss Rush, loot beacons, config robustness (broken, partial and absurd files).\n")
    w("**Not verified by running - read this before you trust it**\n")
    w("- **How it sounds and feels.** Sounds are vanilla samples re-pitched under the mod's own events; the tests prove they exist and are wired, nobody has listened to a "
      "full fight. Balance is derived from one table and checked for completability by the soak fights, not for fun by a human (see `PLAYTEST.md`).")
    w("- **Long-run natural spawning** of the regular mobs and how often shrines turn up in a real world (the rules and the placement are tested; the frequency over hours is not).")
    w("- **Other hardware, other settings**: shaders, other resolutions and GUI scales, low-end machines. Particle caps and `reducedEffects` are there for that.")
    w("- **Multiplayer and dedicated servers**: out of scope by design (the mod is `client`-only).")
    w("- **Item-by-item hands-on**: the %d items were generated from tables and are tested by family, not each one by hand; a few hundred recipes, tooltips and texture details rest on that generation being uniform." % len(I.ITEMS))
    w("- **Recipe-viewer plugins** (EMI/REI/JEI): none shipped; every recipe is a vanilla recipe JSON, which those mods read on their own.")
    w("- The wandering trader's *appearance* in the world is vanilla's roll; only the content of its stock is verified.\n")
    w("Suites live in `src/gametest`; `DEV_NOTES.md` lists them and how to run a subset (`USM_ONLY=boss3,t5`, `USM_ONLY=soak`, `USM_ONLY=worldgen`).\n")

    w("## Project layout\n")
    w("```\nsrc/main        common code: content registration, items, abilities, mobs/bosses/attacks, progression, worldgen hooks\n"
      "src/client      rig renderer, Codex GUI, HUD, keybinds\nsrc/gametest    the in-game verification suites\n"
      "src/main/resources  generated assets and data (do not edit by hand)\ntools/          Python generators: usm_gen/* tables + rig DSL, gen_all.py, gen_readme.py\n```\n")
    w("## Credits and licence\n")
    w("All art, models and (re-pitched) audio references are original or generated by the scripts in `tools/`; no Mojang assets are redistributed. "
      "Built with Fabric, Fabric API and Loom.\n")
    (ROOT / "README.md").write_text("\n".join(out), encoding="utf-8")
    print("README.md written (%d lines)" % len("\n".join(out).splitlines()))


if __name__ == "__main__":
    main()
