"""Mod sound events (assets/ultimatesurvival/sounds.json).

Every event is a thin redirect onto a vanilla sound event with its own pitch and volume (sounds.json "type": "event"), so the mod has a
sound identity of its own - one voice per theme, per boss, per creature - without shipping a single audio file. Pitches stay inside
the 0.5 - 2.0 range the sound engine allows."""
from . import mobs as MB

# theme -> telegraph ("warn"), hit ("impact") and phase-change ("roar") voices as (vanilla event, volume, pitch); music = (vanilla track, pitch)
STYLE = {
    "verdant": dict(warn=("entity.evoker.prepare_summon", 1.0, 1.3), impact=("entity.generic.explode", 0.8, 1.7), roar=("entity.ravager.roar", 1.0, 1.2), music=None),
    "azure":   dict(warn=("block.amethyst_block.chime", 1.0, 0.7), impact=("block.amethyst_block.resonate", 1.0, 0.9), roar=("entity.ravager.roar", 1.0, 1.0), music=None),
    "ember":   dict(warn=("entity.blaze.shoot", 1.0, 0.7), impact=("entity.generic.explode", 1.0, 0.8), roar=("entity.ravager.roar", 1.0, 0.6),
                    music=("music.nether.basalt_deltas", 1.0)),
    "storm":   dict(warn=("entity.lightning_bolt.thunder", 1.0, 1.5), impact=("entity.lightning_bolt.impact", 1.0, 1.0), roar=("entity.ravager.roar", 1.0, 0.9),
                    music=("music.overworld.jagged_peaks", 1.0)),
    "blood":   dict(warn=("entity.warden.heartbeat", 1.0, 0.9), impact=("entity.warden.sonic_boom", 0.9, 1.3), roar=("entity.ravager.roar", 1.0, 0.5),
                    music=("music.nether.crimson_forest", 0.85)),
    "chrono":  dict(warn=("block.bell.use", 1.0, 0.6), impact=("block.amethyst_block.resonate", 1.0, 0.5), roar=("entity.warden.roar", 0.9, 0.8),
                    music=("music.overworld.dripstone_caves", 0.9)),
    "void":    dict(warn=("entity.enderman.stare", 1.0, 0.5), impact=("entity.ender_dragon.growl", 1.0, 0.6), roar=("entity.ender_dragon.growl", 1.2, 0.5),
                    music=("music.end", 0.8)),
    "radiant": dict(warn=("block.beacon.power_select", 1.0, 1.3), impact=("block.beacon.deactivate", 1.0, 1.4), roar=("block.beacon.activate", 1.2, 1.6),
                    music=("music.overworld.cherry_grove", 1.0)),
    "apex":    dict(warn=("block.respawn_anchor.charge", 1.0, 0.6), impact=("entity.generic.explode", 1.0, 0.5), roar=("entity.wither.spawn", 1.0, 0.7),
                    music=("music.dragon", 0.8)),
    "frost":   dict(warn=("entity.stray.ambient", 1.0, 0.5), impact=("block.glass.break", 1.0, 0.6), roar=("entity.polar_bear.warning", 1.2, 0.5),
                    music=("music.overworld.frozen_peaks", 1.0)),
    "plague":  dict(warn=("entity.zombie_villager.converted", 1.0, 0.6), impact=("block.slime_block.break", 1.0, 0.5), roar=("entity.ravager.roar", 1.0, 0.45),
                    music=("music.nether.soul_sand_valley", 0.9)),
    "cosmic":  dict(warn=("block.beacon.power_select", 1.0, 0.7), impact=("entity.generic.explode", 1.0, 0.6), roar=("entity.ender_dragon.growl", 1.2, 0.6),
                    music=("music.credits", 0.9)),
    "eternal": dict(warn=("block.beacon.activate", 1.0, 1.0), impact=("entity.firework_rocket.large_blast", 1.0, 0.7), roar=("entity.wither.spawn", 1.2, 0.55),
                    music=("music.overworld.deep_dark", 0.6)),
}

# one-off voices used by the game's own events
MISC = {
    "gate.cleared": ("ui.toast.challenge_complete", 1.0, 0.8),
    "ritual.begin": ("block.beacon.activate", 1.0, 0.6),
    "ritual.rune": ("entity.warden.ambient", 1.0, 0.5),
    "invasion.horn": ("event.raid.horn", 1.0, 0.9),
    "rush.start": ("entity.ender_dragon.growl", 1.0, 0.8),
    "rush.won": ("ui.toast.challenge_complete", 1.0, 1.1),
}


def _event(name, volume, pitch):
    return {"sounds": [{"name": "minecraft:" + name, "type": "event", "volume": round(volume, 3), "pitch": round(max(0.5, min(2.0, pitch)), 3)}]}


def build():
    """-> (sounds.json dict, list of mod sound ids, {mob id: its three sound ids}).  Mob rows get rewired to the mod ids by gen_all."""
    out = {}
    for theme, s in STYLE.items():
        for kind in ("warn", "impact", "roar"):
            ev, vol, pit = s[kind]
            out["style.%s.%s" % (theme, kind)] = _event(ev, vol, pit)
        if s["music"]:
            ev, pit = s["music"]
            out["music.boss.%s" % theme] = _event(ev, 1.0, pit)
    for key, (ev, vol, pit) in MISC.items():
        out[key] = _event(ev, vol, pit)
    mob_sounds = {}
    for m in MB.MOBS:
        snd = m.extra.get("sounds") or {}
        pitch = float(snd.get("pitch", 1.0))
        ids = {}
        for kind in ("hurt", "death", "ambient"):
            if snd.get(kind):
                name = snd[kind]
                out["entity.%s.%s" % (m.id, kind)] = _event(name, 1.0, pitch)
                ids[kind] = "ultimatesurvival:entity.%s.%s" % (m.id, kind)
        if ids:
            mob_sounds[m.id] = ids
    return out, sorted(out), mob_sounds
