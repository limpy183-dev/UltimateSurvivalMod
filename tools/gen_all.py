#!/usr/bin/env python3
"""Generate every asset + data file + the runtime manifest from the Python content tables.

    python tools/gen_all.py            # (re)generate
    python tools/gen_all.py --sheet    # also write build/contact_sheet.png for visual review

Outputs go under src/main/resources and are tracked in tools/.generated so stale files are
removed on the next run. Commit the outputs: the Gradle build never needs Python."""
import json
import sys
import textwrap
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from PIL import Image                                  # noqa: E402
from usm_gen import balance, items as I                # noqa: E402
from usm_gen import abilities as AB                    # noqa: E402
from usm_gen.anim import animate                       # noqa: E402
from usm_gen.gates import GATES                        # noqa: E402
from usm_gen.lang_static import LANG as STATIC_LANG   # noqa: E402
from usm_gen.materials import PALS                     # noqa: E402
from usm_gen.painters import PAINTERS                  # noqa: E402
from usm_gen.sprite import Sprite, strip               # noqa: E402
import usm_gen.painters_tools                          # noqa: E402,F401
import usm_gen.painters_armor as ARMOR                 # noqa: E402
import usm_gen.painters_misc as MISC                   # noqa: E402,F401
import usm_gen.painters_gui as GUI                     # noqa: E402
import usm_gen.painters_boss as BOSSART                # noqa: E402
from usm_gen import mobs as MB                         # noqa: E402
from usm_gen import codex_data as CODEX                # noqa: E402
from usm_gen import sounds as SND, particles as PART, extras as EX   # noqa: E402

import usm_gen.content_t1                              # noqa: E402,F401
import usm_gen.content_t2                              # noqa: E402,F401
import usm_gen.content_t3                              # noqa: E402,F401
import usm_gen.content_t4                              # noqa: E402,F401
import usm_gen.content_mobs as CMOBS                   # noqa: E402
import usm_gen.content_t5                              # noqa: E402,F401
import usm_gen.content_t6                              # noqa: E402,F401
import usm_gen.content_extra                           # noqa: E402,F401
from usm_gen import power as POWER                     # noqa: E402

POWER.apply()                                          # global power / tempo pass (Tier 6 content is authored at final strength and skipped)
EX.register_trophies()                                 # trophy blocks
EX.finish()                                            # tome / trophy rows of every boss bag

NS = I.NS
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS
TRACK = ROOT / "tools" / ".generated"

written = []


def out(path, text=None, image=None):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    if image is not None:
        image.save(path)
    else:
        path.write_text(text, encoding="utf-8")
    written.append(str(path.relative_to(ROOT)).replace("\\", "/"))


def wjson(path, obj):
    out(path, json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


def clean():
    if TRACK.exists():
        for line in TRACK.read_text().splitlines():
            p = ROOT / line
            if p.is_file():
                p.unlink()


def ing(s):
    if isinstance(s, (list, tuple)):
        return [ing(x) for x in s]
    return s if (s.startswith("#") or ":" in s) else f"{NS}:{s}"


# ---------------------------------------------------------------------------------------------
# recipes
# ---------------------------------------------------------------------------------------------
def recipe_json(rid, count_result, r):
    res = {"id": f"{NS}:{rid}"}
    t = r["type"]
    count = r.get("count", 1)
    if count > 1:
        res["count"] = count
    if t == "shaped":
        return {"type": "minecraft:crafting_shaped", "category": r["category"], "key": {k: ing(v) for k, v in r["key"].items()},
                "pattern": r["pattern"], "result": res}
    if t == "shapeless":
        return {"type": "minecraft:crafting_shapeless", "category": r["category"], "ingredients": [ing(i) for i in r["ingredients"]],
                "result": res}
    if t == "smelting":
        return {"type": "minecraft:smelting", "category": "misc", "ingredient": ing(r["ingredient"]), "result": {"id": f"{NS}:{rid}"},
                "experience": r["xp"], "cookingtime": r["time"]}
    if t == "smithing":
        return {"type": "minecraft:smithing_transform", "template": ing(r["template"]), "base": ing(r["base"]),
                "addition": ing(r["addition"]), "result": res}
    raise ValueError(t)


def write_recipes(result_id, recipes):
    """Writes recipe files for one result; returns the list of recipe ids (smelting also gets a blasting twin)."""
    ids = []
    for n, r in enumerate(recipes):
        name = result_id if n == 0 else f"{result_id}_{n}"
        wjson(DATA / "recipe" / f"{name}.json", recipe_json(result_id, 1, r))
        ids.append(name)
        if r["type"] == "smelting" and r.get("blast"):
            rj = recipe_json(result_id, 1, r)
            rj["type"] = "minecraft:blasting"
            rj["cookingtime"] = max(20, r["time"] // 2)
            wjson(DATA / "recipe" / f"{name}_from_blasting.json", rj)
            ids.append(f"{name}_from_blasting")
    return ids


# ---------------------------------------------------------------------------------------------
# items
# ---------------------------------------------------------------------------------------------
def item_model(item_id, texture, parent):
    wjson(ASSETS / "models" / "item" / f"{item_id}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{texture}"}})


def item_def(item_id, model):
    wjson(ASSETS / "items" / f"{item_id}.json", {"model": {"type": "minecraft:model", "model": model}})


def wrap(text, width=42):
    return "\n".join(textwrap.wrap(text, width))


def ability_lines(item, lang):
    for n, a in enumerate(item.abilities):
        if a["id"] not in AB.CATALOG:
            raise KeyError("ability text missing for " + a["id"])
        kind, title, text, cd = AB.describe(a)
        lang[f"item.{NS}.{item.id}.ab.{n}"] = "|".join([kind, title, wrap(text), ("%g" % cd) if cd else ""])


def gen_item(item, lang, sheet):
    art = dict(item.art)
    painter = PAINTERS[art.pop("paint")]
    pal = PALS[art.pop("pal")]
    anim = art.pop("anim", None)
    res = painter(pal, **art)
    if isinstance(res, dict):                                   # bow: idle + three draw frames
        sheet.append(res["main"])
        out(ASSETS / "textures" / "item" / f"{item.id}.png", image=res["main"].image())
        for k in range(3):
            out(ASSETS / "textures" / "item" / f"{item.id}_pulling_{k}.png", image=res[f"pull_{k}"].image())
        wjson(ASSETS / "models" / "item" / f"{item.id}.json", {"parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{item.id}"}})
        for k in range(3):
            wjson(ASSETS / "models" / "item" / f"{item.id}_pulling_{k}.json",
                  {"parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{item.id}_pulling_{k}"}})
        draw = item.stats.get("draw", 20)
        wjson(ASSETS / "items" / f"{item.id}.json", {"model": {
            "type": "minecraft:condition", "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": f"{NS}:item/{item.id}"},
            "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": round(1.0 / draw, 5),
                        "fallback": {"type": "minecraft:model", "model": f"{NS}:item/{item.id}_pulling_0"},
                        "entries": [{"threshold": 0.65, "model": {"type": "minecraft:model", "model": f"{NS}:item/{item.id}_pulling_1"}},
                                    {"threshold": 0.9, "model": {"type": "minecraft:model", "model": f"{NS}:item/{item.id}_pulling_2"}}]}}})
    else:
        frames = animate(res, pal, anim)
        sheet.append(frames[0])
        out(ASSETS / "textures" / "item" / f"{item.id}.png", image=strip(frames))
        if len(frames) > 1:
            wjson(ASSETS / "textures" / "item" / f"{item.id}.png.mcmeta", {"animation": {"frametime": 3, "interpolate": False}})
        item_model(item.id, item.id, "minecraft:item/handheld" if item.model == "handheld" else "minecraft:item/generated")
        item_def(item.id, f"{NS}:item/{item.id}")
    lang[f"item.{NS}.{item.id}"] = item.name
    if item.lore:
        lang[f"item.{NS}.{item.id}.lore"] = item.lore
    obtain = item.obtain or CODEX.OBTAIN.get(item.id, "")
    if obtain:
        lang[f"item.{NS}.{item.id}.obtain"] = obtain
    ability_lines(item, lang)
    if item.recipe:
        item.recipe_ids = write_recipes(item.id, item.recipes())


# ---------------------------------------------------------------------------------------------
# blocks, tags, world generation
# ---------------------------------------------------------------------------------------------
def gen_altar(b, lang, tags):
    """The ritual altar: a hand-built multi-box model sharing one 16x16 texture."""
    out(ASSETS / "textures" / "block" / f"{b.id}.png", image=BOSSART.altar_texture(PALS[b.art["pal"]]))
    model = json.loads(json.dumps(BOSSART.ALTAR_MODEL).replace("ritual_altar", b.id))
    wjson(ASSETS / "models" / "block" / f"{b.id}.json", model)
    wjson(ASSETS / "blockstates" / f"{b.id}.json", {"variants": {"": {"model": f"{NS}:block/{b.id}"}}})
    item_def(b.id, f"{NS}:block/{b.id}")
    lang[f"block.{NS}.{b.id}"] = b.name
    if b.lore:
        lang[f"block.{NS}.{b.id}.lore"] = b.lore
    wjson(DATA / "loot_table" / "blocks" / f"{b.id}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{b.id}", "pools": [{
        "rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"{NS}:{b.id}"}]}]})
    tags["mineable/pickaxe"].append(f"{NS}:{b.id}")
    tags["needs_diamond_tool"].append(f"{NS}:{b.id}")
    b.recipe_ids = write_recipes(b.id, b.recipes())


def gen_trophy(b, lang, tags):
    """A boss trophy: plinth, stem and a plaque that carries the boss's emblem."""
    pal = PALS[b.art["pal"]]
    base = PAINTERS["metal_block"](pal)
    out(ASSETS / "textures" / "block" / f"{b.id}.png", image=base.image())
    emblem = GUI.emblem(b.art["emblem"]).image()
    out(ASSETS / "textures" / "block" / f"{b.id}_emblem.png", image=emblem)
    faces = lambda side: {d: {"texture": "#emblem" if d in side else "#all"} for d in ("down", "up", "north", "south", "west", "east")}
    elements = [
        {"from": [3, 0, 3], "to": [13, 2, 13], "faces": faces(())},
        {"from": [6, 2, 6], "to": [10, 8, 10], "faces": faces(())},
        {"from": [2, 8, 6.5], "to": [14, 16, 9.5], "faces": faces(("north", "south"))},
        {"from": [1, 7, 6], "to": [15, 8, 10], "faces": faces(())},
    ]
    wjson(ASSETS / "models" / "block" / f"{b.id}.json", {"parent": "minecraft:block/block", "textures": {
        "all": f"{NS}:block/{b.id}", "emblem": f"{NS}:block/{b.id}_emblem", "particle": f"{NS}:block/{b.id}"}, "elements": elements})
    wjson(ASSETS / "blockstates" / f"{b.id}.json", {"variants": {"": {"model": f"{NS}:block/{b.id}"}}})
    item_def(b.id, f"{NS}:block/{b.id}")
    lang[f"block.{NS}.{b.id}"] = b.name
    if b.lore:
        lang[f"block.{NS}.{b.id}.lore"] = b.lore
    wjson(DATA / "loot_table" / "blocks" / f"{b.id}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{b.id}", "pools": [{
        "rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"{NS}:{b.id}"}]}]})
    tags["mineable/pickaxe"].append(f"{NS}:{b.id}")
    b.recipe_ids = []


def gen_block(b, lang, sheet, tags):
    if b.kind == "altar":
        return gen_altar(b, lang, tags)
    if b.kind == "trophy":
        return gen_trophy(b, lang, tags)
    art = dict(b.art)
    painter = PAINTERS[art.pop("paint")]
    pal = PALS[art.pop("pal")]
    sp = painter(pal, **art)
    sheet.append(sp)
    out(ASSETS / "textures" / "block" / f"{b.id}.png", image=sp.image())
    wjson(ASSETS / "models" / "block" / f"{b.id}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{b.id}"}})
    wjson(ASSETS / "blockstates" / f"{b.id}.json", {"variants": {"": {"model": f"{NS}:block/{b.id}"}}})
    item_def(b.id, f"{NS}:block/{b.id}")
    lang[f"block.{NS}.{b.id}"] = b.name
    if b.lore:
        lang[f"block.{NS}.{b.id}.lore"] = b.lore
    drop = f"{NS}:{b.drop}" if b.drop else f"{NS}:{b.id}"
    if b.kind == "ore":
        loot = {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{b.id}", "pools": [{"rolls": 1.0, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{b.id}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": drop, "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]}]}]}]}
    else:
        loot = {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{b.id}", "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{b.id}"}]}]}
    wjson(DATA / "loot_table" / "blocks" / f"{b.id}.json", loot)
    tags["mineable/pickaxe"].append(f"{NS}:{b.id}")
    if b.tool in ("stone", "iron", "diamond"):
        tags[f"needs_{b.tool}_tool"].append(f"{NS}:{b.id}")
    if b.kind == "ore":
        tags["c:ores"].append(f"{NS}:{b.id}")
    b.recipe_ids = write_recipes(b.id, b.recipes())


def write_tags(tags):
    for name, values in tags.items():
        if not values:
            continue
        if name.startswith("c:"):
            for kind in ("block", "item"):
                wjson(RES / "data" / "c" / "tags" / kind / (name[2:] + ".json"), {"replace": False, "values": values})
        else:
            wjson(RES / "data" / "minecraft" / "tags" / "block" / (name + ".json"), {"replace": False, "values": values})


def gen_worldgen():
    manifest = []
    for w in I.WORLDGEN:
        if w["dimension"] == "nether":
            targets = [{"state": {"Name": f"{NS}:{w['stone']}"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_nether"}}]
        elif w["dimension"] == "end":
            targets = [{"state": {"Name": f"{NS}:{w['stone']}"}, "target": {"predicate_type": "minecraft:block_match", "block": "minecraft:end_stone"}}]
        else:
            targets = [{"state": {"Name": f"{NS}:{w['stone']}"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
                       {"state": {"Name": f"{NS}:{w['deep']}"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]
        wjson(DATA / "worldgen" / "configured_feature" / f"{w['name']}.json",
              {"type": "minecraft:ore", "config": {"discard_chance_on_air_exposure": w["discard"], "size": w["size"], "targets": targets}})
        height = {"type": "minecraft:trapezoid" if w["peak"] else "minecraft:uniform",
                  "min_inclusive": {"absolute": w["min_y"]}, "max_inclusive": {"absolute": w["max_y"]}}
        wjson(DATA / "worldgen" / "placed_feature" / f"{w['name']}.json",
              {"feature": f"{NS}:{w['name']}", "placement": [{"type": "minecraft:count", "count": w["count"]}, {"type": "minecraft:in_square"},
                                                              {"type": "minecraft:height_range", "height": height}, {"type": "minecraft:biome"}]})
        manifest.append({"feature": w["name"], "dimension": w["dimension"]})
    return manifest


# ---------------------------------------------------------------------------------------------
# sounds, particles, trades, shrines, advancements
# ---------------------------------------------------------------------------------------------
def gen_sounds(mob_rows):
    data, ids, mob_sounds = SND.build()
    wjson(ASSETS / "sounds.json", data)
    for row in mob_rows:                                           # creatures speak through their own mod events (the pitch now lives in sounds.json)
        mine = mob_sounds.get(row["id"])
        if mine:
            row["sounds"] = dict(mine, pitch=1.0)
    return ids


def gen_particles():
    for name in PART.NAMES:
        frames = PART.frames(name)
        for n, im in enumerate(frames):
            out(ASSETS / "textures" / "particle" / f"{name}_{n}.png", image=im)
        wjson(ASSETS / "particles" / f"{name}.json", {"textures": [f"{NS}:{name}_{n}" for n in range(len(frames))]})
    return list(PART.NAMES)


def _book(enchant, level):
    return {"id": "minecraft:enchanted_book", "components": {"minecraft:stored_enchantments": {f"minecraft:{enchant}": level}}}


def gen_trades(lang):
    """Wandering-trader trades are data in this version: one villager_trade file per tome + a tag that adds them to the trader's uncommon set."""
    names = []
    for ench, level, emeralds, diamonds in EX.BOOK_TRADES:
        name = f"tome_{ench}_{level}"
        trade = {"wants": {"id": "minecraft:emerald", "count": float(emeralds)}, "gives": _book(ench, level), "max_uses": 1.0, "xp": 10.0, "reputation_discount": 0.0}
        if diamonds:
            trade["additional_wants"] = {"id": "minecraft:diamond", "count": float(diamonds)}
        wjson(DATA / "villager_trade" / f"{name}.json", trade)
        names.append(f"{NS}:{name}")
    wjson(RES / "data" / "minecraft" / "tags" / "villager_trade" / "wandering_trader" / "uncommon.json", {"replace": False, "values": names})
    return len(names)


def gen_shrine_loot():
    entries = []
    for item, lo, hi, weight in EX.SHRINE_LOOT:
        if item.startswith("book:"):
            _, ench, level = item.split(":")
            entries.append({"type": "minecraft:item", "name": "minecraft:enchanted_book", "weight": weight, "functions": [{
                "function": "minecraft:set_components", "components": {"minecraft:stored_enchantments": {f"minecraft:{ench}": int(level)}}}]})
        else:
            entries.append({"type": "minecraft:item", "name": item, "weight": weight, "functions": [{
                "function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}]})
    wjson(DATA / "loot_table" / "chests" / "shrine.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 4.0, "max": 7.0}, "entries": entries}]})
    wjson(DATA / "worldgen" / "configured_feature" / "sovereign_shrine.json", {"type": f"{NS}:sovereign_shrine", "config": {}})
    wjson(DATA / "worldgen" / "placed_feature" / "sovereign_shrine.json", {"feature": f"{NS}:sovereign_shrine", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 90}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})


def gen_advancements(lang):
    adv = DATA / "advancement"
    for aid, parent, icon, frame, title, desc, how in EX.ADVANCEMENTS:
        if how == "tick":
            crit = {"trigger": "minecraft:tick"}
        elif how == "code":
            crit = {"trigger": "minecraft:impossible"}
        else:
            crit = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": how}]}}
        body = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{NS}.{aid}.title"},
                            "description": {"translate": f"advancements.{NS}.{aid}.description"}, "frame": frame,
                            "show_toast": True, "announce_to_chat": aid != "root", "hidden": False},
                "criteria": {"done": crit}, "requirements": [["done"]]}
        if parent:
            body["parent"] = f"{NS}:{parent}"
        else:
            body["display"]["background"] = "minecraft:textures/gui/advancements/backgrounds/adventure.png"
        if frame == "challenge":
            body["rewards"] = {"experience": 500}
        elif frame == "goal":
            body["rewards"] = {"experience": 100}
        wjson(adv / f"{aid}.json", body)
        lang[f"advancements.{NS}.{aid}.title"] = title
        lang[f"advancements.{NS}.{aid}.description"] = desc
    return [a[0] for a in EX.ADVANCEMENTS]


# ---------------------------------------------------------------------------------------------
# armor sets
# ---------------------------------------------------------------------------------------------
def gen_sets(lang, sheet):
    manifest = []
    for st in I.SETS:
        pal = PALS[st["pal"]]
        humanoid, leggings = ARMOR.armor_layers(pal, **st["layer"])
        out(ASSETS / "textures" / "entity" / "equipment" / "humanoid" / f"{st['id']}.png", image=humanoid)
        out(ASSETS / "textures" / "entity" / "equipment" / "humanoid_leggings" / f"{st['id']}.png", image=leggings)
        layers = {"humanoid": [{"texture": f"{NS}:{st['id']}"}], "humanoid_leggings": [{"texture": f"{NS}:{st['id']}"}]}
        for folder, sheet_img in (("humanoid", humanoid), ("humanoid_leggings", leggings)):                 # eyes and runes: a second, full-bright layer (ArmorGlow)
            glow_img = ARMOR.glow_layer(sheet_img, pal)
            if glow_img.getbbox() is not None:
                out(ASSETS / "textures" / "entity" / "equipment" / folder / f"{st['id']}_glow.png", image=glow_img)
                layers[folder].append({"texture": f"{NS}:{st['id']}_glow"})
        wjson(ASSETS / "equipment" / f"{st['id']}.json", {"layers": layers})
        lang[f"set.{NS}.{st['id']}"] = st["name"]
        for n, b in enumerate(st["bonus"]):
            kind, title, text, _ = AB.describe(b)
            lang[f"set.{NS}.{st['id']}.bonus.{n}"] = "%s|%s" % (title, wrap(text, 40))
        manifest.append(dict(id=st["id"], name=st["name"], gate=st["gate"], pieces=st["pieces"], bonus=st["bonus"],
                             ambient=st["ambient"], color=st["color"], desc=st["desc"]))
    return manifest


# ---------------------------------------------------------------------------------------------
def gen_codex(lang):
    """Book panel, icons, boss emblems and all lore text. Returns the manifest section."""
    out(ASSETS / "textures" / "gui" / "codex.png", image=GUI.codex_panel())
    out(ASSETS / "textures" / "gui" / "codex_icons.png", image=GUI.codex_icons())
    lang["codex.prologue"] = CODEX.PROLOGUE
    bosses = []
    for gid, b in CODEX.BOSSES.items():
        out(ASSETS / "textures" / "gui" / "emblem" / f"{gid}.png", image=GUI.emblem(gid).image())
        lang[f"codex.boss.{gid}.rank"] = b["rank"]
        lang[f"codex.boss.{gid}.theme"] = b["theme"]
        lang[f"codex.boss.{gid}.lore"] = b["lore"]
        lang[f"codex.boss.{gid}.summon"] = b["summon"]
        for n, t in enumerate(b["tips"]):
            lang[f"codex.boss.{gid}.tip.{n}"] = t
        bosses.append(dict(id=gid, tips=len(b["tips"])))
    tips = []
    for n, (tier, text) in enumerate(CODEX.TIPS):
        lang[f"codex.tip.{n}"] = text
        tips.append(dict(tier=tier))
    return dict(bosses=bosses, tips=tips)


# ---------------------------------------------------------------------------------------------
# mobs & bosses
# ---------------------------------------------------------------------------------------------
def gen_mobs(lang, sheet):
    rows = []
    for m in MB.MOBS:
        rig_json, base, glow = m.rig.build()
        wjson(ASSETS / "usm_rigs" / f"{m.id}.json", rig_json)
        out(ASSETS / "textures" / "entity" / f"{m.id}.png", image=base)
        out(ASSETS / "textures" / "entity" / f"{m.id}_glow.png", image=glow)
        wjson(DATA / "loot_table" / "entities" / f"{m.id}.json", {"type": "minecraft:entity", "pools": []})
        lang[f"entity.{NS}.{m.id}"] = m.name
        if m.extra.get("lore"):
            lang[f"entity.{NS}.{m.id}.lore"] = m.extra["lore"]
        if m.extra.get("title"):
            lang[f"entity.{NS}.{m.id}.title"] = m.extra["title"]
        if m.kind == "boss" and "," in m.name:                      # "Ignarok, the Cinder Warden" -> a title card that fits the screen
            lang[f"entity.{NS}.{m.id}.short"], lang[f"entity.{NS}.{m.id}.epithet"] = [part.strip() for part in m.name.split(",", 1)]
        egg = m.extra.get("egg")
        if egg:
            sp = PAINTERS["egg"](None, c1=egg[0], c2=egg[1], seed=len(m.id))
            sheet.append(sp)
            out(ASSETS / "textures" / "item" / f"{m.id}_spawn_egg.png", image=sp.image())
            item_model(f"{m.id}_spawn_egg", f"{m.id}_spawn_egg", "minecraft:item/generated")
            item_def(f"{m.id}_spawn_egg", f"{NS}:item/{m.id}_spawn_egg")
            lang[f"item.{NS}.{m.id}_spawn_egg"] = f"{m.name} Spawn Egg"
        rows.append(m.manifest())
    return rows


def contact_sheet(sprites, path, scale=6, cols=14):
    rows = (len(sprites) + cols - 1) // cols
    cell = 16 * scale + 6
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (38, 38, 46, 255))
    for i, sp in enumerate(sprites):
        im = sp.image().resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.paste(im, ((i % cols) * cell + 3, (i // cols) * cell + 3), im)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def recipe_shape(pattern, key):
    """A shaped recipe as the crafting grid sees it: trimmed to its bounding box, with each cell replaced by its ingredient."""
    rows = [list(r) for r in pattern]
    cells = [(y, x) for y, r in enumerate(rows) for x, c in enumerate(r) if c != " "]
    y0, y1 = min(c[0] for c in cells), max(c[0] for c in cells)
    x0, x1 = min(c[1] for c in cells), max(c[1] for c in cells)
    return tuple(tuple(key.get(rows[y][x], "") if x < len(rows[y]) and rows[y][x] != " " else "" for x in range(x0, x1 + 1)) for y in range(y0, y1 + 1))


def check_recipes():
    """Two recipes that fill the crafting grid identically (or as a mirror image) can never both be craftable: fail the generator instead of shipping one."""
    seen = {}
    for res in list(I.ITEMS) + list(I.BLOCKS):
        for r in res.recipes():
            if r["type"] == "shaped":
                shape = recipe_shape(r["pattern"], r["key"])
                keys = [("shaped", shape), ("shaped", tuple(tuple(reversed(row)) for row in shape))]
            elif r["type"] == "shapeless":
                keys = [("shapeless", tuple(sorted(r["ingredients"])))]
            else:
                continue
            for k in keys:
                if k in seen and seen[k] != res.id:
                    raise ValueError("recipe clash: %s and %s craft from the same ingredients in the same shape" % (seen[k], res.id))
            seen.setdefault(keys[0], res.id)


def main():
    check_recipes()
    clean()
    lang = dict(STATIC_LANG)
    sheet = []
    tags = {"mineable/pickaxe": [], "needs_stone_tool": [], "needs_iron_tool": [], "needs_diamond_tool": [], "c:ores": []}

    for b in I.BLOCKS:
        gen_block(b, lang, sheet, tags)
    for it in I.ITEMS:
        gen_item(it, lang, sheet)
    sets = gen_sets(lang, sheet)
    mob_rows = gen_mobs(lang, sheet)
    sound_ids = gen_sounds(mob_rows)
    particle_ids = gen_particles()
    trades = gen_trades(lang)
    gen_shrine_loot()
    advancements = gen_advancements(lang)
    worldgen = gen_worldgen()
    write_tags(tags)

    out(ASSETS / "textures" / "gui" / "accessories.png", image=MISC.accessory_panel())
    codex_manifest = gen_codex(lang)

    for g in GATES:
        lang[f"ultimatesurvival.gate.{g['id']}"] = g["name"]
    lang[f"entity.{NS}.bolt"] = "Energy Bolt"

    manifest = {
        "version": 1,
        "gates": [dict(g, order=i) for i, g in enumerate(GATES)],
        "balance": {"gates": balance.GATE_STATS, "boss": balance.BOSS_STATS, "tiers": balance.TIER_NAMES},
        "blocks": [dict(b.manifest(), recipe_ids=b.recipe_ids) for b in I.BLOCKS],
        "items": [it.manifest() for it in I.ITEMS],
        "sets": sets,
        "mobs": mob_rows,
        "invasions": CMOBS.INVASIONS,
        "worldgen": worldgen,
        "sounds": sound_ids,
        "particles": particle_ids,
        "advancements": advancements,
        "trophies": list(EX.TROPHIES),
        "codex": codex_manifest,
    }
    wjson(DATA / "usm" / "content.json", manifest)
    wjson(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))
    TRACK.write_text("\n".join(written) + "\n")
    print(f"generated {len(written)} files: {len(I.ITEMS)} items, {len(I.BLOCKS)} blocks, {len(I.SETS)} armor sets, {len(MB.MOBS)} mobs")
    if "--sheet" in sys.argv:
        contact_sheet(sheet, ROOT / "build" / "contact_sheet.png")


if __name__ == "__main__":
    main()
