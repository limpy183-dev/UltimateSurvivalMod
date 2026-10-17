# Playtest checklist

The automated suites (`./gradlew runClientGameTest`, plus `USM_ONLY=soak` and `USM_ONLY=worldgen`; see `DEV_NOTES.md`) prove the mechanics: numbers, hooks, gates, loot, attack timing, rituals, natural fights from spawn to death, ore and shrine generation. It cannot tell you
whether the mod *feels* right - how a fight reads, whether the sounds are pleasant, whether the screen is legible. This list is for the human pass.

Quick setup: `./gradlew runClient`, new **Survival** world (default difficulty Normal; the bosses refuse to come on Peaceful unless `bossesOnPeaceful` is set), cheats on.
Handy commands (op level 2):

| Command | Use |
|---|---|
| `/usm status` | where you are in the progression |
| `/usm unlock all` (or `1`-`6`, or a boss id such as `chronos`) | clear gates so you can try their gear |
| `/usm lock <n or boss>` / `/usm reset` | go back |
| `/usm discover all` | reveal every Codex entry |
| `/usm bossrush start` / `stop` | the eleven Sovereigns back to back |
| `/give @s ultimatesurvival:<item>` | e.g. `omni_sigil`, `apex_blade`, `heart_crystal` |
| `/give @s ultimatesurvival:<boss>_spawn_egg` | spawn any boss or mob without the ritual |
| `/place feature ultimatesurvival:sovereign_shrine ~ ~ ~` | build a shrine where you stand |
| `/effect give @s minecraft:resistance infinite 4 true` | take your time looking at a fight |

Tick a box only when it looks *and* feels right; write what was off next to the ones that were not.

## 1. Start-up and first join
- [ ] The game reaches the title screen with no error dialog; `config/ultimatesurvival.json` appears after the first launch.
- [ ] Controls lists an **Ultimate Survival** category with `J` (Codex) and `K` (accessories).
- [ ] First join gives you the Codex of the Sundering; `/usm status` says Tier 1.
- [ ] Creative inventory has a tab with every item, block and spawn egg; no pink/black squares in any slot, in the hand, on the ground or worn.
- [ ] Quit to the title and reload the world: nothing is lost (progress, accessories, discoveries).

## 2. Gates and locks
- [ ] Hold a Tier 2 item (`/give @s ultimatesurvival:azurite_sword`) before the Ender Dragon: red "locked" tooltip naming the dragon, no damage bonus, no abilities, no area mining.
- [ ] `/usm unlock 2` makes the same item work at once, without a relog; `/usm lock 2` takes it away again.
- [ ] Armor sets of a locked gate give no armor points, no set bonus and no ambient effects.
- [ ] Killing a boss shows a message and plays the unlock sound; the advancement toast appears; the Codex tab "Progress" updates.

## 3. Tools
- [ ] Verdite -> Apex pickaxes break 3x3 up to 9x9 faces in the direction you look (not just in front of the player); sneaking breaks one block.
- [ ] Vein mining follows a whole ore vein; auto-smelt hands you ingots; the magnet pulls drops straight into the inventory; nothing duplicates.
- [ ] Axes fell the whole tree (including big jungle/dark-oak trees) without leaving floating leaves behind forever; shovels and hoes work their areas; hoes replant.
- [ ] Mining speed feels right per tier - not instant at Tier 2, nearly instant at Tier 5. Durability is plentiful, `Unbreakable`-feeling at the top.
- [ ] Enchanting these tools in an anvil reaches the raised limits (level 10 where the enchantment allows it) and never says *Too Expensive!*.

## 4. Weapons, bows, staves
- [ ] Right-click and sneak + right-click use two different abilities; the cooldown shows beside the hotbar and the item shows a cooldown sweep.
- [ ] Every sword: on-hit and on-kill effects trigger, particles are visible but not blinding, sounds are not painful on repeat.
- [ ] Weapon families swing differently - daggers, rapiers, spears and staves thrust, greatswords and hammers sweep slowly - in first *and* third person.
- [ ] Bows fire energy bolts without ammo, pierce as advertised, and charge feels responsive; staffs fire on right-click.
- [ ] Damage numbers feel like a clear step up from the gate before and below the boss HP table in the README.

## 5. Armor, accessories, consumables
- [ ] Each of the nine sets: 4-piece bonus message ("Set bonus active"), the ambient particles (leaves, cloud, embers, sparks, blood, time motes, void, light, all of them) look good in first *and* third person and are not nauseating.
- [ ] At night, in third person (`F5`), the eyes, runes and sigils of every set from Azurite up glow; the rest of the armor does not.
- [ ] Double jump works in the air (Azurite / Tempest / Voidwalker), once or twice as written, and not while swimming or climbing.
- [ ] `K` opens six accessory slots; each accessory's bonus applies while it sits there and stops when removed; the contents survive a relog and a death-and-respawn.
- [ ] Consumables: eating animation, cooldown, effect text; Heart Crystals add permanent max health (watch the cap with `maxHealthCap`).

## 6. Raised limits
- [ ] Enchanting table/loot: the *Tome* books (wandering trader, shrine chests, boss bags) hold levels above vanilla; combining books in an anvil reaches level 10.
- [ ] Stack sizes above 64 where configured behave in chests, hoppers, shift-click and the creative inventory.
- [ ] Toggle each limit option in `config/ultimatesurvival.json` off and confirm the vanilla behavior returns after a restart.

## 7. Codex (`J`)
- [ ] Opens and closes cleanly (also with `Esc`); tabs Sovereigns, Arsenal, Bestiary, Progress, Tips all render; scroll bars work; long entries wrap and never run off the page.
- [ ] Tier 1 items and blocks are listed from the very first minute: italic grey names and veiled icons until you hold one, but their stats, abilities and recipes are fully readable. Holding one makes the entry full colour and shows the "Codex updated" toast once.
- [ ] Everything beyond Tier 1 reads `???` with no spoilers (also in tooltips) until you find, meet or beat it.
- [ ] Every scrollbar (item list, right-hand page, Progress page, Tips) can be grabbed with the mouse and dragged; clicking the bare track jumps there; the wheel still works; the bar brightens when you hover it.
- [ ] The 3D preview of each mob and boss animates its attacks and does not clip out of its frame; every boss's page lists ritual and phases once the boss is discovered.
- [ ] `/usm discover all` unlocks everything; `Codex complete` advancement fires only when the whole book is revealed.

## 8. The Sovereigns
For each: set it up, fight it, then check the list under the table. Use `/usm unlock <previous boss>` to get the right gear.

| # | Boss | Altar rule | Sigil |
|---|---|---|---|
| 1 | Ignarok, Cinder Warden | Nether | `cinder_sigil` |
| 2 | Voltaris, Storm Regent | Overworld, open sky | `tempest_sigil` |
| 3 | Hemorath, Crimson Matriarch | Overworld, at night, 4 hearts | `blood_sigil` |
| 4 | Chronos, the Hourless | anywhere, 5 levels of XP | `chrono_sigil` |
| 5 | Nihilus, Devourer of Light | the End, 4 Eyes of Ender | `void_sigil` |
| 6 | Solareon, Radiant Seraph | Overworld, midday, open sky, 12 levels | `radiant_sigil` |
| 7 | The Omnarch, Sundered God | anywhere, 30 levels + 8 hearts | `omni_sigil` |
| 8 | Boreus, the Rime Tyrant | Overworld, 30 levels + 16 Blue Ice | `rime_sigil` |
| 9 | Morbane, the Blightlord | Overworld, at night, 6 hearts + 8 Fermented Spider Eyes | `blight_sigil` |
| 10 | Astraeus, the Star-Eater | the End, 40 levels + 2 End Crystals | `astral_sigil` |
| 11 | Eternus, the Final Thread | anywhere, 50 levels + 10 hearts + 2 Nether Stars | `thread_sigil` |

For every boss:
- [ ] Using the sigil anywhere but the Ritual Altar does nothing but a hint; using it on the altar with a rule broken says *which* rule and keeps the sigil.
- [ ] The ritual (runes, thunder, tremors, boss climbing out) reads as an event; breaking the altar mid-ritual cancels it and returns the sigil.
- [ ] The model looks good from every side and at close range; idle, walk, each attack, spawn and death animations play; no limb flies off or sinks into the ground.
- [ ] Boss bar: name, colour, notches; the music theme starts when you come near and stops when the boss dies or you leave.
- [ ] Phase changes are announced (roar, flash, bar flash) and the attacks really change; each attack is **telegraphed** long enough to react to with the gear of its tier.
- [ ] You cannot cheese it: standing on a pillar, in a hole, or far away with a bow does not make you safe, and walking away does not reset it for free (it resets only when everyone is gone for a while).
- [ ] It does not despawn, does not get stuck, does not slide through the floor, and the arena stays usable (hazards clean up afterwards).
- [ ] Screen shake and particles are strong at the big moments, and `reducedEffects` / `screenShakeStrength` visibly tone them down.
- [ ] On death: the bag (or direct drops), a trophy, XP, the unlock message and sound; the items it unlocks now work; the Codex "Sovereigns" page shows the kill.
- [ ] Saving and quitting in the middle of a fight, then loading, either resumes sensibly or resets the boss without leaving a stray bar or hazards.

Fights worth special attention: **Voltaris** (lightning markers vs. gusts), **Hemorath** (the leech, blood pools, its swarm), **Chronos** (rewind and time stop - does the screen make sense?), **Nihilus** (black holes and rifts, the gloom), **Solareon** (the eclipse: standing in the dark must be readable; the beam and the supernova), **Omnarch** (four phases, every previous mechanic reappearing; must be survivable with Apex gear and unforgiving without).

## 9. Mobs and invasions
- [ ] Natural spawns appear in the stated dimensions and conditions, in sensible numbers; none spawn in peaceful mode or underwater by accident.
- [ ] Each mob has a distinct silhouette, sound set and one clearly readable special attack; ranged ones can be avoided; none can hit through walls.
- [ ] An invasion arrives about every ten minutes per player (`invasionIntervalSeconds`, +-25%); it announces itself, scales with your tier, and ends (no endless waves). Lower the interval to see it sooner.
- [ ] Vanilla mobs grow stronger with your tier in a way that is noticeable but not unfair.

## 10. World
- [ ] The five ores generate (Verdite and Azurite and Stormite in the Overworld, Cinderite in the Nether, Aetherite in the End) at believable depths, in veins you can find in a few minutes.
- [ ] `/place feature ultimatesurvival:sovereign_shrine ~ ~ ~` builds a shrine; the chest holds loot with high-level books; flying around finds natural ones eventually in both the Overworld and the outer End islands (never on the central dragon island).
- [ ] Trophy blocks can be placed on any surface, give light where they should, and break back into themselves.

## 11. Boss Rush
- [ ] `/usm bossrush start` shows the title, the first boss arrives after the countdown, a ten-second breather and partial heal follow each kill, the order is Ignarok -> Eternus (eleven fights; bring food).
- [ ] Dying or leaving ends it with a message; `/usm bossrush stop` removes the active boss; finishing pays the reward and the time is shown.

## 12. Sound, particles, performance
- [ ] No sound is missing (silent roar/hit) or unbearably loud/high; boss themes are audible but not louder than the fight.
- [ ] Custom particles (embers, arcs, blood, ticks, void motes, radiant sparks, apex glimmers, frost flakes, spores, stars, prisms) look like their theme and never show as white squares or the purple/black missing texture.
- [ ] F3 shows the server tick staying under 50 ms during the Omnarch fight with everything on; frame rate stays playable with `maxParticlesPerEffect` at its default.
- [ ] `reducedEffects: true` leaves every attack readable (telegraphs still show).
- [ ] The full-screen flashes (boss awakening, phase change, death, a ritual's lightning, supernovas) are clear but not harsh, never strobe, and `screenFlashStrength: 0` removes them.
- [ ] A fallen Sovereign's treasure bag is easy to spot from a distance (glow plus a column of sparks).

## 13. Config and robustness
- [ ] Editing `config/ultimatesurvival.json` (including breaking the JSON on purpose) never crashes startup: unknown keys are dropped, missing keys restored, a broken file falls back to defaults.
- [ ] Multiple worlds keep separate progression; deleting a world leaves no state behind; `/usm reset` really resets.
- [ ] Creative-mode and spectator-mode players are never hurt by boss attacks and do not trigger boss behavior.

## 14. Tier 6 and the power pass
- [ ] **Power**: weapons clearly hit harder and swing faster than before (compare a Verdite sword against the README table); abilities recharge noticeably sooner; bows draw quickly; furnaces cook twice as fast; a ritual takes about three and a half seconds. None of it feels like a typo.
- [ ] **Effects**: every right-click ability throws a ring and column of its tier's colour and a little camera rattle; every melee hit sparks; kills burst. Nothing blocks the view from first person (no big squares in your face), even at point-blank range - and `reducedEffects` / `effectIntensity` tone it all down.
- [ ] **Boreus**: the cracks before the spikes (stand in a gap, the second wave fills it), the whiteout frosting your screen when you stop and ending when you move, the deep freeze you can jump, three Rime Anchors, the pools in the last phase.
- [ ] **Morbane**: pods land where the red rings are and leave clouds; the infestation message appears and water or milk really cures it; Blight Hearts shield it in phase three.
- [ ] **Astraeus**: planets sweep outward (is the pocket near the boss readable?), the constellation's lines are visible *before* they hurt, comets, black holes in star colours, four Astral Nodes.
- [ ] **Eternus**: threads haul you in (and a wall stops them), each phase clearly wears another Sovereign's mechanics, the rainbow colour shimmer is not harsh, four Loom Spindles in the last phase. It is meant to be survivable only with Tier 6 gear *and* a Second Wind.
- [ ] **Gear**: Rimebrand freezes then shatters; Plaguebringer's infection jumps from corpse to corpse (try a pack of zombies); the Comet Blade's meteors and the Stellar Staff's orbital strike are dramatic but fair to *you* (they never hit allies or you); the Starborn set lets you fly in survival and takes it away when removed; Eternal armor refuses one deadly blow and recharges.
- [ ] **Eternium tools**: the 13x13 pickaxe is fun rather than laggy (try it on a big stone face); the Eternium axe fells a huge tree in one go without freezing the game.
- [ ] **Balance feel**: Tier 6 should feel like a different game from Tier 5 - bigger numbers, bigger fights - without a single boss being impossible with the previous tier's best gear plus one or two upgrades.

