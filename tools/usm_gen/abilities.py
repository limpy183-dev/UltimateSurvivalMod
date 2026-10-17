"""Ability text catalog. The Java side implements the behaviour; this file owns the words, so tooltips
and the Codex always quote the real numbers from the content tables.

kind: active (right-click), hit, kill, passive (always on while held), wear (armor set / accessory),
      use (consumable effect), mod (damage modifier), trait (tool trait)"""

# id -> (kind, title, template, default cooldown ticks)
CATALOG = {
    # --- weapons -----------------------------------------------------------------------------
    "dash_strike":      ("active", "Dash Strike", "Dash up to {dist:g} blocks, striking everything in your path for {dmg:g} damage.", 120),
    "blink":            ("active", "Blink", "Teleport up to {dist:g} blocks where you look.", 80),
    "ground_slam":      ("active", "Ground Slam", "Smash the ground: foes within {radius:g} blocks take up to {dmg:g} damage and are thrown back.", 160),
    "projectile_wave":  ("active", "Bolt Volley", "Fire {count:g} {style} bolts in a fan, {dmg:g} damage each.", 70),
    "chain_lightning":  ("active", "Chain Lightning", "Strike a target for {dmg:g}; the lightning jumps up to {jumps:g} more times.", 110),
    "heal_pulse":       ("active", "Healing Pulse", "Heal {amount:g} health (half for nearby allies).", 300),
    "lifesteal":        ("hit", "Lifesteal", "Heal {pct:.0%} of the damage you deal.", 0),
    "knockback_burst":  ("hit", "Concussive Strike", "Hits blast the target backwards.", 0),
    "bleed":            ("hit", "Bleed", "Hits cause bleeding (poison) for {secs:g}s.", 0),
    "execute":          ("mod", "Execute", "Deal {mult:g}x damage to enemies below {thr:.0%} health.", 0),
    "backstab":         ("mod", "Backstab", "Deal {mult:g}x damage to enemies facing away from you.", 0),
    "on_kill_buff":     ("kill", "Hunter's Rush", "Kills grant {effect_name} for {secs:g}s.", 0),
    "on_kill_heal":     ("kill", "Soul Harvest", "Kills restore {amount:g} health.", 0),
    "bow_shot":         ("passive", "Energy Bow", "Fires {count:g} {style} bolt(s) for up to {dmg:g} damage; pierces {pierce:g}; needs no arrows.", 0),
    # --- worn: armor sets and accessories ---------------------------------------------------------
    "regen_ooc":        ("wear", "Verdant Resolve", "Regenerate {amount:g} health every second while unhurt for {secs:g}s.", 0),
    "feather_fall":     ("wear", "Feather Fall", "Immune to fall damage.", 0),
    "double_jump":      ("wear", "Skyborne", "Jump again in mid-air ({jumps:g} extra jump(s)).", 0),
    "glide_sneak":      ("wear", "Air Brake", "Hold sneak while falling to glide down gently.", 0),
    "damage_reduction": ("wear", "Ward", "Take {pct:.0%} less damage.", 0),
    "dodge":            ("wear", "Evasion", "{chance:.0%} chance to avoid an attack entirely.", 0),
    "thorns":           ("wear", "Thorns", "Reflect {pct:.0%} of melee damage to attackers{ignite_text}{shock_text}.", 0),
    "magnet":           ("wear", "Item Magnet", "Pull dropped items from {radius:g} blocks away.", 0),
    "fire_res":         ("wear", "Fireproof", "Immune to fire and lava damage.", 0),
    "night_vision":     ("wear", "Night Sight", "See clearly in the dark.", 0),
    "water_breathing":  ("wear", "Gills", "Breathe underwater.", 0),
    "haste_aura":       ("wear", "Haste", "Mine and swing faster.", 0),
    # --- tier 3: fire ---------------------------------------------------------------------------
    "fire_nova":        ("active", "Fire Nova", "Erupt in flame: foes within {radius:g} blocks take {dmg:g} damage and burn for {ignite:g}s.", 140),
    "ignite":           ("hit", "Ignite", "Hits set the target ablaze for {ignite:g}s and burn for {dmg:g} extra damage.", 0),
    "burning_bonus":    ("mod", "Fuel the Fire", "Deal {mult:g}x damage to burning enemies.", 0),
    "flame_aura":       ("wear", "Flame Aura", "Enemies within {radius:g} blocks burn and take {dmg:g} damage every second.", 0),
    "phoenix":          ("wear", "Phoenix Rebirth", "A killing blow instead restores you to {pct:.0%} health, burning everything near you. Recharges in {cd_s:g}s.", 0),
    # --- tier 4: storm / blood / time -----------------------------------------------------------
    "shock":            ("hit", "Shock", "{chance:.0%} chance to stun the target for {stun_s:g}s and arc {dmg:g} damage to the nearest other enemy.", 0),
    "call_lightning":   ("active", "Call Lightning", "Strike where you look {strikes:g} times for {dmg:g} damage each in a {radius:g}-block burst, stunning what survives.", 180),
    "storm_aura":       ("wear", "Storm Aura", "Every {every_s:g}s lightning falls on up to {targets:g} enemies within {radius:g} blocks for {dmg:g} damage.", 0),
    "bloodlust":        ("wear", "Blood Lust", "Heal {pct:.0%} of the damage you deal.", 0),
    "crimson_veil":     ("wear", "Crimson Veil", "Below {thr:.0%} health you take {pct:.0%} less damage.", 0),
    "blood_reap":       ("active", "Blood Reap", "Sweep a crimson arc: foes within {radius:g} blocks take {dmg:g} damage and you heal {heal_pct:.0%} of it.", 200),
    "slow_hit":         ("hit", "Temporal Drag", "Hits slow the target for {secs:g}s.", 0),
    "time_freeze":      ("active", "Time Freeze", "Freeze every enemy within {radius:g} blocks in place for {secs:g}s.", 500),
    "rewind":           ("active", "Rewind", "Return to where you stood {back_s:g}s ago and recover {heal:g} health.", 360),
    "quicken":          ("wear", "Quickened", "Abilities recharge {cd_pct:.0%} faster.", 0),
    "time_dilation":    ("wear", "Time Dilation", "{chance:.0%} chance when hit to slow everything within {radius:g} blocks for {secs:g}s.", 0),
    # --- tier 5: void / light / apex ------------------------------------------------------------
    "singularity":      ("active", "Singularity", "Open a black hole where you look: foes within {radius:g} blocks are dragged in, those at its core take {dmg:g} damage "
                                                  "every half second for {secs:g}s, then it collapses outward.", 300),
    "gravity_pull":     ("active", "Gravity Well", "Drag every enemy within {radius:g} blocks to your feet, dealing {dmg:g} damage and slowing them.", 220),
    "rend":             ("hit", "Rend", "Hits tear away {pct:.1%} of the target's maximum health as extra damage (up to {cap:g}).", 0),
    "devour":           ("kill", "Devour", "Kills restore {amount:g} health and grant absorption.", 0),
    "entropy_aura":     ("wear", "Entropy", "Enemies within {radius:g} blocks are slowed and take {dmg:g} damage every {every_s:g}s.", 0),
    "sunbeam":          ("active", "Sunbeam", "Fire a piercing beam of light up to {range:g} blocks for {dmg:g} damage (double against undead and void creatures). Terrain stops it.", 140),
    "solar_flare":      ("active", "Solar Flare", "Blind and burn every enemy within {radius:g} blocks for {dmg:g} damage.", 240),
    "supernova":        ("active", "Supernova", "Gather light for a moment, then detonate: foes within {radius:g} blocks take up to {dmg:g} damage and are hurled away. You resist the blast.", 600),
    "sear":             ("hit", "Searing Light", "Hits burn for {dmg:g} extra damage and leave the target glowing.", 0),
    "smite":            ("mod", "Smite", "Deal {mult:g}x damage to the undead and to creatures of the void and of blood.", 0),
    "halo_aura":        ("wear", "Halo", "You shed light that Nihilus cannot eat. Enemies within {radius:g} blocks are revealed and take {dmg:g} damage every {every_s:g}s.", 0),
    "light_flare":      ("use", "Lumen Flare", "Floods {radius:g} blocks around you with light for {secs:g}s - the Devourer's darkness cannot touch it - and grants Night Vision.", 0),
    "cleave":           ("active", "World Cleave", "Slash a {arc:g} degree arc {radius:g} blocks deep for {dmg:g} damage and throw foes aside.", 140),
    "apex_nova":        ("active", "Apex Nova", "Release everything: foes within {radius:g} blocks take {dmg:g} damage, and you heal {heal:g} and gain absorption.", 360),
    "omni_aura":        ("wear", "Omni Aura", "Enemies within {radius:g} blocks take {dmg:g} damage every {every_s:g}s and you recover {heal:g} health each time.", 0),
    # --- tier 6: frost / plague / cosmos / every colour ----------------------------------------------------
    "absolute_zero":    ("active", "Absolute Zero", "Freeze everything within {radius:g} blocks solid for {secs:g}s and deal up to {dmg:g} damage. Frozen foes shatter under a Shatter blow.", 300),
    "blizzard":         ("active", "Blizzard", "Call a storm where you look: for {secs:g}s everything within {radius:g} blocks is slowed and takes {dmg:g} damage every quarter second, "
                                               "and whatever is still frozen when it breaks takes five times as much.", 360),
    "frostbite":        ("hit", "Frostbite", "{chance:.0%} chance to freeze the target solid for {secs:g}s.", 0),
    "shatter":          ("mod", "Shatter", "Deal {mult:g}x damage to frozen enemies, breaking the ice.", 0),
    "frost_aura":       ("wear", "Rime Aura", "Enemies within {radius:g} blocks are slowed and take {dmg:g} damage every {every_s:g}s, and {freeze:.0%} of the time are frozen solid.", 0),
    "rime_shell":       ("wear", "Rime Shell", "Blows land {pct:.0%} softer, and {chance:.0%} of attackers are frozen solid for {secs:g}s.", 0),
    "contagion":        ("hit", "Contagion", "Hits poison the target; when it dies the plague bursts out for {dmg:g} damage to everything within {radius:g} blocks and infects it in turn.", 0),
    "toxic_cloud":      ("active", "Toxic Cloud", "Release a cloud of spores where you look: for {secs:g}s everything inside {radius:g} blocks is poisoned and takes {dmg:g} damage twice a second.", 260),
    "plague_burst":     ("active", "Plague Burst", "Everything within {radius:g} blocks takes {dmg:g} damage and is infected, and the ground around you blooms with poison clouds.", 320),
    "blight_aura":      ("wear", "Blight Aura", "Enemies within {radius:g} blocks rot for {dmg:g} damage every {every_s:g}s, and each one you wither heals you for {heal:g}.", 0),
    "meteor_shower":    ("active", "Meteor Shower", "Call {count:g} falling stars over the place you look: each blast deals {dmg:g} damage in {blast:g} blocks.", 420),
    "orbital_strike":   ("active", "Orbital Strike", "After a moment's warning a beam from orbit hits where you look for {dmg:g} damage in a {radius:g}-block blast.", 500),
    "starfall":         ("hit", "Starfall", "{chance:.0%} chance to call a star down on the target for {dmg:g} damage.", 0),
    "star_aura":        ("wear", "Orrery", "{count:g} stars circle you and cut anything they touch for {dmg:g} damage.", 0),
    "flight":           ("wear", "Flight", "You can fly. Falls do no harm while you wear it.", 0),
    "omega_slash":      ("active", "Omega Slash", "Slash all around you {count:g} times in a row: everything within {radius:g} blocks takes {dmg:g} damage each time, and the last blow is twice as strong.", 300),
    "second_wind":      ("wear", "Second Wind", "A killing blow instead restores you to {pct:.0%} health and freezes everything within {radius:g} blocks. Recharges in {cd_s:g}s.", 0),
    # --- consumables -------------------------------------------------------------------------------
    "heal":             ("use", "Healing", "Restores {amount:g} health.", 0),
    "give_effect":      ("use", "Potion Effects", "{effects_text}", 0),
    "cleanse":          ("use", "Cleanse", "Removes every active effect.", 0),
    "perm_health":      ("use", "Heart Crystal", "Permanently increases maximum health by 1 heart (up to {max:g}).", 0),
    "recall":           ("active", "Recall", "Hold still for {secs:g}s to return to your spawn point.", 900),
}

EFFECT_NAMES = {
    "speed": "Speed", "strength": "Strength", "regeneration": "Regeneration", "resistance": "Resistance",
    "fire_resistance": "Fire Resistance", "night_vision": "Night Vision", "haste": "Haste", "jump_boost": "Jump Boost",
    "absorption": "Absorption", "water_breathing": "Water Breathing", "slow_falling": "Slow Falling", "luck": "Luck",
    "health_boost": "Health Boost", "invisibility": "Invisibility", "saturation": "Saturation",
}
ROMAN = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"]


def _effect_text(params):
    parts = []
    for suf in ("", "2", "3"):
        e = params.get("effect" + suf)
        if e:
            amp = int(params.get("amp" + suf, 0))
            secs = params.get("ticks" + suf, 600) / 20
            parts.append("%s %s (%gs)" % (EFFECT_NAMES.get(e, e.replace("_", " ").title()), ROMAN[min(amp, 9)], secs))
    return "Grants " + ", ".join(parts) + "."


def describe(ab):
    """-> (kind, title, text, cooldown_seconds_or_None) for an ability ref dict."""
    kind, title, tpl, cd = CATALOG[ab["id"]]
    p = dict(ab)
    p.setdefault("secs", p.get("ticks", p.get("delay", 100)) / 20 if ab["id"] != "regen_ooc" else p.get("delay", 100) / 20)
    p.setdefault("thr", p.get("threshold", 0.25))
    if ab["id"] == "on_kill_buff":
        p["effect_name"] = EFFECT_NAMES.get(p.get("effect", "speed"), p.get("effect", "speed").title()) + " " + ROMAN[min(int(p.get("amp", 0)), 9)]
    if ab["id"] == "give_effect":
        p["effects_text"] = _effect_text(p)
    if ab["id"] == "recall":
        p["secs"] = p.get("delay", 60) / 20
    p["ignite_text"] = (" and set them ablaze for %gs" % p["ignite"]) if ab["id"] == "thorns" and p.get("ignite") else ""
    p["cd_s"] = p.get("cd", 6000) / 20
    p["cd_pct"] = 1 - p.get("cd_mult", 1.0)
    p["shock_text"] = (" and shock them for %gs" % p["shock"]) if ab["id"] == "thorns" and p.get("shock") else ""
    p["stun_s"] = p.get("stun", 20) / 20
    p["every_s"] = p.get("every", 60) / 20
    p["back_s"] = p.get("back", 60) / 20
    p.setdefault("strikes", 3)
    p.setdefault("targets", 3)
    p.setdefault("heal", 6)
    p.setdefault("heal_pct", 0.4)
    p.setdefault("thr", 0.5)
    p.setdefault("ignite", 5)
    p.setdefault("pierce", 0)
    p.setdefault("count", 1)
    p.setdefault("style", "energy")
    p.setdefault("amount", 1)
    p.setdefault("jumps", 1)
    p.setdefault("max", 10)
    p.setdefault("radius", 5)
    p.setdefault("dist", 6)
    p.setdefault("dmg", 8)
    p.setdefault("pct", 0.1)
    p.setdefault("mult", 2.0)
    p.setdefault("chance", 0.1)
    cds = None
    if kind == "active":
        cds = ab.get("cd", cd) / 20
    return kind, title, tpl.format_map(_Safe(p)), cds


class _Safe(dict):
    def __missing__(self, key):
        return "?"
