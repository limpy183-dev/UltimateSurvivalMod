# Ultimate Survival Mod — Master Prompt

Build a **Fabric mod for Minecraft 26.2** (Java, Mojang mappings, latest Fabric Loader + Fabric API for 26.2; verify the exact versions/Java requirement from the Fabric site before writing `build.gradle`). Working name: **Ultimate Survival**. It is a **progression mod** in the spirit of Terraria's Calamity and Fargo's Souls: the player and the mobs both get dramatically stronger, gated behind a chain of bosses.

## Go all out
**Go crazy and do your absolute best.** Be ambitious, creative and thorough: this should feel like a massive, polished, professional-grade mod, not a minimal prototype. Where this prompt gives a number or example, treat it as a floor, not a ceiling. Add more items, bosses, attacks, abilities, effects and ideas than listed, and sweat the details. Don't stop at "good enough".

## Ground rules
- The mod must **compile, launch (`runClient`), and load a world without crashing**. Run `./gradlew build` after every phase and fix errors before moving on.
- Work in **phases** (listed at the bottom). Finish and verify each phase before starting the next. Don't stub things and leave them half-done: if something is listed, it works in-game.
- Keep the code data-driven: items, bosses, tiers and unlock rules live in registries/data tables, not copy-pasted classes. Adding a new item or boss should be a few lines.
- **Singleplayer only.** Don't build or test for multiplayer/dedicated servers (no netcode hardening, no per-player sync concerns beyond what the integrated server needs). Set the mod as client/integrated-server only where sensible. Progression is still saved with the world/player data so it persists between sessions.
- Everything tunable (damage multipliers, enchant cap, boss HP) goes in one JSON config file.

## 1. Progression system (the core)
Five tiers. Each tier has a **gate**; items and recipes of that tier are locked until the gate is cleared.

| Tier | Gate | Content |
|---|---|---|
| 1 Early | Unlocked from the start | Entry-level tools, weapons, armor, utility items |
| 2 Mid | Defeat the **Ender Dragon** | Stronger gear, first abilities |
| 3 High | Defeat **Boss 1** (custom, medium difficulty) | Strong gear with active abilities |
| 4 Late | Defeat Bosses **2–4** (one more tier per boss, or cumulative — your design, document it) | Powerful gear, multi-ability |
| 5 Endgame | Defeat the **final bosses (5–7)** | Extremely overpowered gear (weapons, armor, accessories, etc.) |

Locking behaviour (pick the least annoying, and state it): locked recipes don't show/craft, and any locked item that somehow reaches the player (creative, loot, trade) is **unusable** (no attack/ability/armor stats) with a tooltip saying which boss unlocks it. Progress is saved with the world, with a `/usm unlock <tier|boss>` and `/usm reset` command for ops/testing.

## 2. Bosses (7+, each harder than the last)
Every boss needs: custom model + animations, a name bar, **3 phases** triggered by HP thresholds, **at least 3 distinct attacks per phase** (telegraphed so they're dodgeable), arena-friendly behavior (no despawn, no cheese via pillar/suffocation), unique drops, and a **summon method** (craftable summoning item + altar/structure or ritual; the Ender Dragon is the vanilla exception).

Escalation guideline: Boss 1 ≈ tough fight for a geared diamond/netherite player; Boss 7 ≈ requires the best gear from the previous tier plus skill. Use mechanics beyond "more HP": projectile patterns/bullet-hell, summoned minions, arena hazards, teleport/dash attacks, damage-reflect or shield phases, time-limited vulnerability windows, screen/sound effects. Give each boss a distinct theme and attack identity (e.g. fire, void, storm, time, blood, etc. — design the lineup and write it into the lore book).

Add **regular custom mobs** too (~15–20): overworld/nether/end variants plus tiered "invaders" that spawn more dangerously as the player's tier rises, so the world scales with progression. Each has custom model/texture, sounds, and drops used in crafting.

## 3. Items (aim for 80+, scaling by tier)
Each tier adds items of every category: **swords/weapons, bows/ranged, armor sets (with set bonuses), tools, accessories/charms, consumables, materials/ores, boss summoners.** Higher tier ⇒ more abilities and more interesting mechanics, not just bigger numbers.

Required examples (add many more beyond these):
- **Pickaxe / Shovel:** break a **3×3** area (sneak to disable). Higher tiers: 5×5, vein-miner, auto-smelt, magnet drops.
- **Axe:** **felling** — breaking one log fells the whole tree (including leaves option, durability cost capped).
- **Hoe:** till/harvest/replant in an area.
- **Weapons:** per-item abilities such as lifesteal, chain lightning, ground slam, dash strike, projectile waves, summon allies, time slow, execute-below-HP, on-kill buffs. Active abilities use right-click with cooldowns + a small HUD indicator.
- **Armor sets:** set bonuses (fire immunity, flight/glide, regen, damage reflect, double jump, phase through damage, etc.), scaling up to god-tier endgame sets.
- **Accessories/charms** (a dedicated slot or Curios-style Fabric equivalent if it exists for 26.2, otherwise a slot in the lore book/GUI): passive buffs that stack.
- **Endgame items** should be absurdly strong on purpose (this is the Calamity/Fargo fantasy), but still have unique mechanics and a crafting cost that requires fighting the final bosses.

Balance via a single table: tier → damage/armor/durability range, so numbers stay coherent. Add a short balance note in the README.

## 4. Raised vanilla limits
- Enchantments: raise max levels well above vanilla (e.g. Sharpness/Protection/Efficiency/Unbreaking/Fortune etc. up to ~10, config-controlled), make them work in anvils, enchanting tables (via custom books/loot at higher tiers) and commands. Fix the "Too Expensive!" anvil cap.
- Max stack/ other limits where it's safe and makes sense (config toggles). Anything risky gets a toggle, default on.

## 5. The Guide Book ("Codex")
- Given **once** to every player on first join (tracked so it doesn't re-give; also craftable/recoverable if lost).
- Custom opening GUI (not the vanilla book screen): themed background, animated flourish/sound on open, tabs for **Bosses, Items (by tier), Mobs, Progression, Tips**.
- Entries are **hidden/"???" until unlocked** by actually progressing: first encountering a mob, killing a boss, obtaining an item. Unlocked entries show lore, stats, abilities, crafting recipe, summon method and boss attack tips.
- Shows current tier, next gate, and a checklist of defeated bosses.

## 6. Art & audio — everything custom
Every item, block, mob, boss and the book GUI needs its **own texture/model/icon** — no vanilla reuse and no placeholder squares.
- Items: unique 16×16 (or larger, high-detail) PNGs per item, consistent per-tier visual language (colors/glow). Generate them programmatically (a Python/Pillow script checked into `tools/`) with per-item palettes and shapes, or hand-pixel them in code; handheld vs generated models set correctly; animated/glowing textures for top-tier items (`.mcmeta` animation or emissive).
- Mobs/bosses: custom entity models (Blockbench-style geometry via Fabric's entity model layers, or GeckoLib-equivalent if a 26.2 version exists), unique textures, idle/walk/attack animations, large bosses visibly bigger.
- **Beautiful, amazing animations and visual effects everywhere the player interacts with gear.** Using an item or wearing armor should *feel* spectacular, scaling in flair with tier:
  - Weapons/tools: custom swing and use animations (first and third person), slash arcs/trails, impact bursts, charge-up glows, and dramatic ability casts (shockwaves, lightning, beams, summoning circles, screen shake/flash).
  - Armor: visible auras, glowing eyes/runes, particle trails, wings/capes/halos or floating orbitals on higher sets, equip animation and sound, set-bonus activation effects, and ambient effects while worn (embers, void sparks, etc.).
  - Items in the world/inventory: glowing/animated textures, floating or rotating item drops with beams/particles, enchant-style shimmer, and cooldown-ready pulses on the HUD.
  - Endgame gear gets the most elaborate effects (full-screen ability cinematics, custom shaders/overlays where feasible), while early gear stays subtle but polished. Effects must stay performant (particle caps, config toggle to reduce them).
- Custom sounds (can reuse/pitch-shift vanilla sounds registered under the mod's own sound events), particles for abilities, boss music/bossbar styling.
- Lang file with proper names/tooltips for everything.

## 7. Other things that fit the mod
Add your own ideas if they serve the progression fantasy, for example: custom ores/dimension or boss arena structures worldgen, an altar block for rituals, a boss-trophy/relic system, achievements/advancements per tier, a post-win "boss rush" or higher-difficulty re-fight, trader NPC that sells tier items, loot beyond drops (boss treasure bags), config + game rules, JEI/REI/EMI recipe compat if available.

## Deliverables
1. Full Gradle project in this folder (`src/main`, `src/client`, assets, data, `fabric.mod.json`) that builds a jar.
2. `README.md` with: tier/boss table, item list, controls, commands, config options, how to run/build.
3. `tools/` for any asset-generation scripts.
4. At the end, report what was verified by actually running it vs. what was only compiled.

## Phases (verify each before the next)
1. Project setup, mod loads, config, saved progression data, `/usm` commands, lock/unlock logic.
2. Tier 1–2 items, tool abilities (3×3, tree felling), raised enchant limits, textures for all of them.
3. Guide Book + custom GUI + discovery system.
4. Boss framework (phases, attacks, bossbar, summon rituals) + Boss 1 + tier 3 items.
5. Bosses 2–4 + tier 4 content + custom regular mobs and tier-scaled invaders.
6. Final bosses 5–7 + endgame OP items/armor sets.
7. Polish: sounds/particles, balance pass, README, extra ideas, full playtest checklist.
