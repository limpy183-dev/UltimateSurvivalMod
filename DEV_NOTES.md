# Ultimate Survival - developer notes

Living document: how the project is wired, hard-won 26.2 API facts, and build status per phase.

## Toolchain (verified)
- Minecraft **26.2** (unobfuscated, Mojang names), Fabric Loader 0.19.5, **Loom 1.18.2** (`net.fabricmc.fabric-loom`),
  Fabric API **0.161.0+26.2**, Java **25**, Gradle 9.7.1 (wrapper).
- `./gradlew build` - compile + jar. `./gradlew runClientGameTest` - boots the REAL client, creates a world, runs
  `src/gametest/.../UsmClientGameTest` (asserts + screenshots in `build/run/clientGameTest/screenshots/`). Whole suite ~20 min.
  `USM_ONLY=boss3,t5,mobs` (comma list: tools, ability, wear, fire, rules, codex, cheats, boss, boss2, boss3, **boss4**, t5, **t6**, polish, mobs, **soak**) runs just those suites;
  `USM_PART=nihilus,solareon,omnarch,gallery` narrows boss3 (`boreus,morbane,astraeus,eternus,gallery` narrows boss4; with boss ids it also narrows the soak suite). A failed check logs `GAMETEST FAILED` and halts the JVM (exit 1);
  a partial run halts with exit 0 once its suites pass (closing the world after a partial run can deadlock inside the test framework).
  `soak` is opt-in (not part of the default run): every Sovereign fights its natural AI from spawn to death in real time (~20 min for all seven) - see below.
  The harness steps client and server in lockstep, so `/tick sprint` / `/tick rate` do NOT speed a test up (a sprint reports "20 ticks per second").
  `UsmClientGameTest` turns `pauseOnLostFocus` and vsync off and sets the inactivity FPS limit to MINIMIZED: with the defaults a window that is hidden behind other
  windows freezes the whole lockstep (pause menu) or crawls at ~10 ticks per second ("Can't keep up! ... 15000ms behind" every 30 s in the log is that, not a mod problem).
- `python tools/gen_all.py [--sheet]` regenerates every texture/model/lang/recipe + `content.json` (needs Pillow).
  Generated files are tracked in `tools/.generated` (stale ones are deleted on the next run). **Re-run it after editing
  `tools/usm_gen/*` (incl. lang_static.py) or the game shows raw lang keys.** `--sheet` writes `build/contact_sheet.png`.
  Never run it (or edit `src/`) while a gradle run is in its first ~40 s or while the game test is running.
- To read Minecraft sources: decompile `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-{common,clientonly}-deobf/26.2/*.jar`
  with Vineflower (`~/.gradle/caches/modules-2/files-2.1/org.vineflower/...`) into a scratch dir and grep it. Fabric API
  sources: `https://maven.fabricmc.net/net/fabricmc/fabric-api/<module>/<ver>/<module>-<ver>-sources.jar` (use the module
  versions listed in the game's startup log - the Gradle cache can hold several).
- Editing: bash heredocs break on big/quote-heavy text and mangle `\n` in inline Python - use Write/Edit tools for source files.
  A silent no-op `str.replace` usually means a tab/indent mismatch: assert the pattern is present.

## Architecture
- **Content is data.** `tools/usm_gen/*.py` define gates, balance (`balance.py` = the single stat table), items, blocks, armor
  sets, ore generation, ability text (`abilities.py`), mobs/bosses (`mobs.py`, `rigs*.py`, `content_*.py`). `gen_all.py` writes
  `src/main/resources/data/ultimatesurvival/usm/content.json` (+ every asset/recipe/loot/tag/worldgen file). Java loads it
  (`content/Content`) and registers from it.
- **Items**: `UsmItems.create` switches on `kind`: sword/staff -> `UsmWeaponItem` (abilities), gadget (same class), pickaxe/axe/shovel/hoe ->
  `DigItems.*` (+`DigLogic`: area/felling/vein; `ToolFx`+`BlockMixin`: auto-smelt/magnet drops), bow -> `UsmBow` (arrow bow or energy bow),
  armor -> vanilla equippable + `ArmorSets`/`Wearer` set bonuses, accessory -> plain item + `Accessories` (6 slots/player in `Progress`, own
  menu `AccessoryMenu`/`AccessoryScreen`, key K), consumable -> `UsmConsumable`. Hidden `bolt_*` items are the sprites of the `Bolt` projectile.
- **Abilities** (`ability/*`): `Ability` has optional hooks (use / onHit / onKill / modify / tick / onEquip / onRemove / incoming / outgoing / canUse).
  Registered by id in `WeaponAbilities`, `FireAbilities`, `StormAbilities`, `BloodAbilities`, `TimeAbilities`, `VoidAbilities`, `LightAbilities`,
  `ApexAbilities`, `WearerAbilities`, `ConsumableAbilities`; content rows reference them by id + params. Startup validation fails loudly on an
  unknown id. Text for every ability lives in `tools/usm_gen/abilities.py` (CATALOG) and quotes the real numbers.
- **Damage rescaling choke point**: `LivingEntityMixin` -> `DamageMods.apply` (weapon modifiers like execute/backstab; wearer `incoming`/`outgoing`).
  Extra damage that rides on a hit must use `Dmg.extra` (clears the hurt cooldown first) or the vanilla cooldown swallows it.
- **Attack strength**: vanilla resets the player's charge in `Player.onAttack()` BEFORE item hooks run. `PlayerMixin` captures it; abilities read it
  with `util.AttackStrength.of(player)` (never `getAttackStrengthScale` inside a hit hook).
- **Progression**: `Progress` (SavedData `data/ultimatesurvival/progress.dat`): cleared gates, codex discoveries, codex-given UUIDs, counters,
  accessory slots. `UsmState.progress()` is a process-wide handle (singleplayer: client + integrated server share the JVM, no sync protocol).
  `Gates` has lock/unlock rules; `RecipeSync` awards/revokes recipe-book entries per gate; `CraftingMenuMixin` blanks locked results.
- **Gates** (13, in order): start, ender_dragon, ignarok(T3), voltaris/hemorath/chronos(T4), nihilus/solareon/omnarch(T5), boreus/morbane/astraeus/eternus(T6).
  Every gated item belongs to one gate. Tier 4/5/6 are cumulative stages: items unlock per boss, "current tier" = highest cleared gate's tier.
- **Locked item** = still exists/carryable but: no attribute modifiers (`ItemStackMixin`), no mining speed/drops, no ability, no set/accessory
  bonus, red tooltip line naming the boss. Gate changes refresh equipment (`Gates.mutate`: strips what vanilla LAST applied, clears its cache).
- **Ender Dragon** never calls `die()` -> `EnderDragonMixin` opens the tier-2 gate (first tick of its death animation) and drops drakescale.
- **Raised limits**: `EnchantmentMixin` (max level -> config `enchantmentMaxLevel`, multi-level enchants only), `AnvilMenuMixin`+client
  `AnvilScreenMixin` (no "Too Expensive!"), `ItemInstanceMixin` (stack size -> 99). All toggles in `config/ultimatesurvival.json`.

## 26.2 API facts learned the hard way
- `Identifier` (not ResourceLocation). Vanilla entity types live in `EntityTypes` (`EntityTypes.ZOMBIE`), builder in `EntityType.Builder`.
  Entity tags: `entity.is(EntityTypeTags.UNDEAD)` (`EntityType.is(TagKey)` is gone). Husk is `...monster.zombie.Husk`, Pig `...animal.pig.Pig`.
- Items need `Item.Properties#setId(key)`; `sword/pickaxe/...(ToolMaterial, dmgBaseline, speedBaseline)`; `humanoidArmor(ArmorMaterial, ArmorType)`.
  Attack speed modifier = speed - 4.0, damage modifier = total - 1. Block props: `BlockBehaviour.Properties.of().setId(key)`.
- Rendering/GUI: `GuiGraphicsExtractor` (`fill`, `text`, `blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, texW, texH)`, `item`,
  `outline`); screens override `extractBackground/extractRenderState`; current screen = `mc.gui.screen()`/`setScreen`; HUD via
  `HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, id, element)`; entity renderers via `EntityRenderers.register`
  (render-state + `submit` architecture); `MenuScreens.register` is made accessible by Fabric; menus use Fabric `ExtendedMenuType`.
- Interface default methods need an interface mixin (`ItemInstance#getMaxStackSize`). Name-only `@Inject` on OVERLOADED methods hits only
  one overload - use full descriptors. Helper interfaces a mixin implements must NOT live in the mixin package
  (`IllegalClassLoadError ... cannot be referenced directly`). `ServerPlayer.sendSystemMessage(c, true)` = action bar.
- Client game test API: `context.worldBuilder().create()` -> `TestSingleplayerContext` (`getServer().runCommand/runOnServer/computeOnServer`,
  `getConnection().getServerPlayer()/waitForChunksRender()`, `getWorldSave().open()`), `context.runOnClient/computeOnClient/takeScreenshot/
  waitTicks/waitForScreen`. **Console commands have no entity: use `@a`, never `@s`/`~`** (a relative tp dropped the player out of the world).
- Zombies burn in daylight even with NoAI: use Husks as test dummies. **NoAI mobs never move** (no knockback, no pulls): use an AI-on pig to test forces.
- Item components are NOT bound during mod init: never `new ItemStack(...)` in registration/validation code (check `Item` existence only).
- `tp ... facing x y z` aims from the FEET: pass the feet y for a level pitch (a beam test at eye height otherwise looks 5 degrees up and misses).
  Vanilla clamps max health to 1024 (lifted by `RangedAttributeAccessor` + config `maxHealthCap`); a player at Resistance V
  (amp 4) takes no damage but stays a valid boss target (`setInvulnerable(true)` makes bosses drop the target!).
- `Mob.getLootTable()` is final: mobs get an empty generated loot table JSON and drop through `Mobs.rollDrops`.
- `ServerPlayerGameMode.useItem` re-sets the hand to the stack object `use()` was called with: items that consume themselves and give loot must
  return `InteractionResult.SUCCESS.heldItemTransformedTo(remaining)` (otherwise loot put in the hand slot is overwritten).
- Dummy client-side entities (Codex preview) need `entity.setId(...)` before rendering.
- The `SCALE` attribute set from the registry resizes the model but NOT the hitbox until `refreshDimensions()` runs (`UsmMob` constructor does it).
  Anything anchored to a boss uses `getBbHeight()/getBbWidth()`, never `def.height/width`.
- `LightningBolt` visual-only via `setVisualOnly(true)`; `SpawnPlacements.register` is private -> Fabric `FabricEntityType.Builder.createMob`;
  `Level.getOverworldClockTime()` replaces getDayTime; some `SoundEvents.*` are Holders (`.value()`).
- Light: `level.getBrightness(LightLayer.BLOCK, pos)` is block light only (sky light ignored) - `util.LightZones` adds flares/halos on top.

## Mobs, bosses and the rig system
- **Data**: `tools/usm_gen/mobs.py` (`mob()`, `boss()`, `atk()`, `phase()`, `drop()`) + `rig.py` (bone/box DSL -> packed texture + glow texture +
  animation rules JSON) + `rigs_t3/t4/t5.py` (bosses), `rigs_mobs.py` (beast / brute / floater builders). `gen_all.py` writes
  `assets/ultimatesurvival/usm_rigs/<id>.json`, the two entity textures, spawn egg, empty loot table and the `mobs` manifest section.
  Attack numbers are MULTIPLIERS of the boss base damage (`balance.BOSS_STATS`). `boss(scale=1.5)` scales model AND hitbox.
- **Server** (`mob/`): `MobDef` (parsed row), `Mobs` (registry: entity types, attributes, eggs, drops, sounds, natural spawns), `UsmMob` (stats from
  manifest, synced anim state ANIM/ANIM_START/ANIM_LEN/FLAGS/PHASE, goal-based chase + melee, ranged keep-distance band, attack scheduler,
  hazards, minions, reflect shield), `UsmBoss` (boss bar, phases at HP thresholds with invulnerable roar transition and "no phase skipping" clamp,
  spawn animation, long death sequence, immunities, leash/stuck teleport, enrage timer, shield pylons ("nodes"), treasure bag + gate via AFTER_DEATH),
  `Ritual` + `RitualAltarBlock` + `SigilItem` + `BagItem`, `Invasions` (periodic tier waves), `MobScaling` (vanilla hostiles +20%/tier).
- **Attacks** (`mob/attack/`): an `Attack` schedules timed events on a `Run` (`r.at(tick, fn)`, `r.every(...)`, `r.onEnd(...)`) - telegraph first, damage after.
  Library (30 types): slam, fan, spiral, pools, pillars, summon, charge, meteors, ring, overheat, roar (Attacks); perch, exposed, blink, leap, reflect
  (CommonAttacks); lightning, gust, orbs, leech, rewind, stop (ThemedAttacks); **blackhole, rifts, gloom, cage (VoidAttacks); beam, supernova, flash,
  sunder (LightAttacks)**. `Hazard` (now extensible: `Singularity`, `GloomField`, rift hazards with `linked`) lives in the mob's hazard list and is
  cleared on phase change/death. `Styles` maps a theme id (ember/storm/blood/chrono/void/radiant/apex) to colours, particles and sounds.
  `UsmMob.forceAttack(type)` prefers the current phase's version of an attack.
- **Client** (`client/rig/`): `RigDef` loads the JSON, `RigModel` builds a vanilla `LayerDefinition` (feet-up px -> y-down model space) and evaluates
  declarative animation rules (`sin|walk|kf|const|look|spin|noise`, whens: always/idle/walk/attack:<id>/spawn/dead/vuln/enraged/phase:n), `RigRenderer`
  (+ emissive `GlowLayer`), `Rigs.register()`. The Codex previews any mob live (`MobPreview`: a never-spawned dummy entity replaying its attack animations).
- **Test hooks**: `UsmMob.forceAttack(type)`, `hits` counter (counts hurt attempts, works on Resistance-V players), `noMelee`, `passive`, `runTick()`.

## Status
- [x] Phase 1: setup, config, saved progression, `/usm status|unlock|lock|reset`, lock logic, tooltips.
- [x] Phase 2: T1 (Verdite) + T2 (Azurite) content: tools, weapons, armor sets with bonuses, accessories (6 slots), consumables, recall mirror, ores +
  worldgen, ability HUD, raised enchants/anvil/stacks, recipe + tool locking.
- [x] Phase 3: Codex GUI (5 tabs, discovery, handout/recovery, live 3D mob previews, boss spoils/attacks).
- [x] Phase 4: rig/mob/boss framework, Ignarok (3 phases, ritual altar + Cinder Sigil, treasure bag), Ember Imp, Tier 3 gear.
- [x] Phase 5: Tier 4 - Voltaris (storm, shield pylons), Hemorath (blood, leech/reflect), Chronos (time, stop/rewind) + their gear; 16 regular mobs with natural
  spawn rules and biome spawn lists; tier invasions; vanilla mob scaling. Verified by Boss2Tests/MobTests.
- [x] Phase 6: Tier 5 - Nihilus (void: black holes, rifts, eclipse that needs light, closing cage), Solareon (light: sweeping/spinning beams, supernova,
  blinding flash, sunder), the Omnarch (4 phases borrowing every Sovereign, apex pylons, collapsing arena); Aetherite ore in the End, 3 armor sets,
  18 weapons/bows/tools, accessories, consumables, Lumen Flare; 4 new regular mobs + 4 minions; tier-5 invasion. 15 new abilities. Verified by
  Boss3Tests (Nihilus/Solareon/Omnarch), T5Tests (gear), MobTests (37 mob attacks).
- [x] Phase 7 polish: 130 sound events (vanilla sounds re-pitched under mod events; `tools/usm_gen/sounds.py`) + boss themes (`BossMusic`), 7 custom particle
  types (`particles.py`, vanilla behaviours via `client/ParticleSetup`), camera shake (`Shake` payload + `CameraMixin`) and a full-screen colour flash
  (`ScreenFlash` payload + `ClientFlash` HUD element: never above 80% opacity, one per second, off with `reducedEffects`/`screenFlashStrength: 0`), boss-bar
  styling (`BossBarStyle`), armor-set ambient FX (`AmbientFx`), 8 trophy blocks, 15 advancements, high-level tomes in loot + wandering-trader trades (data), rare
  Sovereign shrines (`ShrineFeature`), Boss Rush (`/usm bossrush`), treasure-bag beacon (`LootBeacon`), per-family swing animations (`SWING_ANIMATION`
  component), config clamping (`UsmConfig.sanitize`), README generator (`tools/gen_readme.py`), `PLAYTEST.md`.
- Phase 7 test suites: `PolishTests` (everything above), `SoakTests` (natural fights, see below), `WorldgenTests` (ores + shrine on natural terrain, a
  second world with the ordinary generator), `PersistTests` (a wounded boss across save-and-quit).

## Final verification record (2026-10-06)
- Default `./gradlew runClientGameTest`: **671 checks, exit 0** (~15 min; tools, ability, wear, fire, rules, codex, boss, boss2, boss3, t5, polish, mobs, the
  save/reload checks incl. a wounded boss, then the worldgen world).
- `USM_ONLY=soak` (all seven bosses, real time, ~20 min): every fight finished, phases advanced, every phase attacked, 9-14 attack types seen per boss,
  longest silence 250 ticks, peak average tick time 3.4 ms, <= 106 entities, nothing left behind, gate + bag every time.
- `USM_ONLY=worldgen`: Verdite ~17000 blocks / 225 chunks, Azurite ~3900, Stormite ~430 in 81 peak chunks, Cinderite ~1300 / 169 Nether chunks, Aetherite ~1700 /
  169 End chunks, shrine built on natural terrain.
- `./gradlew build`: jar + sources jar (`build/libs`).
- Quirks found on the way, all in the harness: the player's server-side menu stays open after a suite drives it (an open menu stops armor changes reaching the
  client: `PolishTests` closes it first), advancement state persists across suites (revoke before asserting "not granted"), `USM_ONLY` partial runs halt with exit 0.


## Creative cheats page (Codex tab 6, only visible in creative mode)
- `cheat/Cheats` is the single list (toggle / number / action). Toggles and numbers edit the live `UsmConfig` and `UsmConfig.save()` writes it back to
  `config/ultimatesurvival.json` (so cheats are global, not per world); actions run once. The client sends `Payloads.CheatAction(id, value)` (toggle 0/1, number =
  index into its allowed values, action = index of its choice); the server ignores it unless `player.isCreative()`. Adding a cheat = one line in `Cheats` + two lang
  keys in `lang_static.py` (`codex.cheat.<id>` and `.desc`). `CodexScreen` draws it with `drawCheatList/drawCheatDetail`; buttons are re-registered every frame (`btns`).
- Accessory slots are `config.accessorySlots` (6..70, `Progress.slots()`): the menu gets the count as its opening data, the extra rows are drawn by `AccessoryScreen`
  under the texture's six, `Progress.accessories()` grows its list, `Accessories.equipped()` only counts the first `slots()` (items above stay stored).
- `noAbilityCooldowns`: `Wearer.cdMult` returns 0 and `UsmWeaponItem` clamps the cooldown to one tick.
- Suite `USM_ONLY=cheats` (`CheatTests`): real client clicks the page, packet reaches the server, extra slots, survival refusal, summon/dismiss.

## Codex rules worth knowing
- `CodexModel.Row.known()` decides whether a page shows content or "???": Tier 1 items/blocks (`preview()`: start gate) are always known; everything else needs
  the discovery key. `ghost()` = Tier 1 and not yet collected - drawn italic/grey with a parchment veil over the icon (`itemIcon`), plus a one-line banner on its page.
  `collected()` is the real discovery key, so the "Entries found" counter and the advancements still count what you actually own.
- Scrollbars are registered per frame (`CodexScreen.Bar`: list, detail page, progress page, tips) so `mouseClicked/Dragged/Released` can grab them (hit zone is
  11 px wide for a 3 px bar; clicking the bare track centres the thumb there). Test hooks: `CodexScreen.entryStates()`, `scrollbarInfo(id)`.

## Soak test (`USM_ONLY=soak`)
`SoakTests` spawns each Sovereign the way a ritual does (`beginSpawn`), leaves its AI alone, and plays an immortal (Resistance V) player who stays near it and lands one
hit a second worth 1% of its max health. A `ServerTickEvents.END_SERVER_TICK` hook samples the boss every tick. It asserts: the fight ends, phases only advance, every phase attacks by itself, half of the boss's attack types show up unforced (attack choice is weighted-random and a phase only lasts a handful of attacks, so more is luck; every type is separately forced and checked by the boss suites), no 800-tick
silences, peak average tick time under 40 ms, fewer than 500 entities, nothing of the mod's left in the world 120 ticks after death, the gate opened, the bag dropped.
`USM_PART=ignarok,omnarch` restricts it. Results are logged as `[soak] ...` lines.


## v2 expansion (2026-10-07): the power pass and Tier 6 "Transcendence"
**Power / tempo pass (everything old got stronger and quicker).** One table + one module, nothing hand-edited in the old content:
- `balance.py`: sword damage per gate ~1.4-1.9x (verdite 10 ... omnarch 170), durability/mining speed up, `SWING_SPEED` 1.35 (attack speed of every archetype), `TOOL_SPD`,
  `TEMPO` 0.65 (boss spawn + death sequences), `SMELT` 0.5 (`smelt()` cooking times).
- `power.py` (run by `gen_all.py`/`gen_readme.py` after all content modules, **before** trophies/tomes): rescales every item ability and set bonus *by kind* from `abilities.CATALOG`
  (damage 1.6x, active cooldown 0.65x, radius 1.2x, dash 1.25x, range 1.15x, projectile speed 1.2x, heal/amount 1.5x, wear-aura interval 0.8x ...), bow draw 0.7x, accessory attribute
  bonuses (speed 1.5x, attack speed 1.3x, % damage 1.5x, max health 1.4x in whole hearts), consumable use time 0.65x, potion durations 1.5x and amplifiers +1. Tier 6 gates are skipped
  (authored at final strength) - except consumable use time. `python -m tools.usm_gen.power` is the self-check. Tooltips/Codex/README quote the scaled numbers because they are generated after it.
- **Tests must not hard-code balance numbers any more**: use `T.stat(item, "damage")`, `T.ab(item, ability, key)`, `T.attr(item, "max_health")`, `T.bonus(set, ability, key)`, `T.setArmor(set)`, `T.g(x)`.
- Effects: `UsmConfig.effectIntensity` (1.6) multiplies every `Fx.n()` count, `maxParticlesPerEffect` 192; old config files are migrated once (`configVersion`): caps raised,
  `veinMineMaxBlocks` 256, `fellMaxLogs` 384. `Fx.cast` (every right-click ability), `Fx.hit` (every melee hit), kill soul-burst, bigger bolt impacts, bow muzzle flashes.
  **Particles never land in the camera**: `Fx.put/ring/line/column/burst` skip positions within 1.5 blocks of a player's eyes (`Fx.visible`) - raw `sendParticles` calls are not filtered.
- Rituals take 70 ticks (`Ritual.LENGTH`; the beats are fractions of it). `gen_all.check_recipes()` fails the build on two recipes that fill the grid identically (it found the Null Ring, which
  could never be crafted next to the Void Shard).

**Tier 6** (gates `boreus` frost, `morbane` plague, `astraeus` cosmic, `eternus` eternal; tier 6; `content_t6.py`, `rigs_t6.py`):
- Eternium (`eternium_shard` dropped by Tier 6 mobs/invasions, 2x2 -> `eternium_ingot`) is the shared metal; each Sovereign's core (`frost_core`, `blight_core`, `star_core`, `weave_core`) makes its gear.
  Sigils chain: rime (omnarch gate) -> blight (boreus) -> astral (morbane) -> thread (astraeus). 4 tools, 20 weapons/bows, 4 armor sets, 11 accessories, 8 consumables, `titan_heart` (perm hearts up to 20).
- New attacks (`mob/attack/FrostAttacks|PlagueAttacks|CosmicAttacks`): `icicles` (lines half a step off the target's bearing: a gap is safe), `whiteout` (stand still = frozen screen then damage),
  `glaciate` (root + spears), `spores` (lobbed pods -> poison-cloud hazards), `infest` (poison as the fuse: water or a cure removes it, else burst + pods), `planets` (orbiting worlds; the pocket
  near the boss and the far side are safe), `constellation` (star polygon; first star half a step off the target's bearing), `tether` (hauls everyone in over 14 ticks, then bursts; terrain breaks the thread).
  Themes `frost|plague|cosmic|eternal` live in `Styles`, `BoltSpec.COLORS`, `AmbientFx`, `sounds.py`, `particles.py` (+`ParticleSetup`), `materials.py`, `painters_gui.EMBLEMS`; `eternal` is rainbow (`Styles.color`).
- New abilities (`ability/Frost|Plague|Cosmic|EternalAbilities`): absolute_zero, blizzard, frostbite, shatter (`util/Frozen` marks frozen mobs), frost_aura, rime_shell, contagion (spreads on death via
  AFTER_DEATH; undead rot with wither because poison cannot touch them), toxic_cloud, plague_burst, blight_aura, meteor_shower, orbital_strike, starfall, star_aura, flight (mayfly while worn, falls harmless;
  landing cancels flying), omega_slash, second_wind (ALLOW_DEATH, like phoenix).
- Bosses: Boreus (4 phases, 3 rime anchors), Morbane (4, 3 blight hearts), Astraeus (4, 4 astral nodes, flies), Eternus (5, 4 loom spindles, flies, borrows every older theme). All use the generic
  rig/phase/node machinery; Boss Rush has 11 stages. A tier-6 invasion and 6 natural-spawn mobs (spawn gate: omnarch) exist.
- `content_extra.py`: 25 more weapons/tools/charms for tiers 2-5 (authored at old numbers, the power pass scales them).
- Suites: `Boss4Tests` (every new attack forced and checked for what it should and should not do, phases, shields, hoards, rituals; screenshots `boss4-*`), `T6Tests` (power pass numbers, crafting gates,
  13x13x8 pickaxe, every new ability, armor sets, flight, Second Wind, hearts, bows). Gallery screenshots: `USM_ONLY=boss4 USM_PART=gallery`.

**Workflow that worked**: edit in the project, then run the game tests from a *copy* (`robocopy src tools /MIR` into a scratch folder, `./gradlew runClientGameTest` there) so a 20-30 minute run never blocks
editing. Several game tests cannot run at once (one window). A failed check halts the JVM at once - fix, rerun just that suite (`USM_ONLY=t6`, `USM_ONLY=boss4 USM_PART=eternus`).

### v2 verification record (2026-10-07)
- Default `./gradlew runClientGameTest` (every suite incl. `boss4`, `t6`, the save/reload checks incl. a wounded boss, and the worldgen world): **1005 checks, exit 0**, 21 min.
  Each check logs `[gametest] ok: ...`, so `grep -c "\[gametest\] ok:" log` is the count.
- `USM_ONLY=soak USM_PART=boreus,morbane,astraeus,eternus` (real time, 12 min, 70 checks), natural AI against an immortal player landing 1%-of-max-HP hits:

  | boss | ticks | attacks started | attack types seen | longest silence | peak entities | peak avg tick |
  |---|---|---|---|---|---|---|
  | Boreus | 3291 | 23 | 11 / 13 | 302 | 37 | 3.3 ms |
  | Morbane | 3411 | 25 | 9 / 13 | 205 | 28 | 1.6 ms |
  | Astraeus | 3391 | 24 | 12 / 13 | 268 | 52 | 1.3 ms |
  | Eternus | 3770 | 28 | 17 / 26 | 198 | 43 | 1.2 ms |

  Every fight ended, phases only advanced, every phase attacked, the shields were broken, nothing of the mod was left 120 ticks after death, the gate opened and the bag dropped.
- `./gradlew build`: `build/libs/ultimate-survival-2.0.0.jar` (+ sources jar).
- Real bugs the new tests found, all fixed: the Null Ring recipe filled the grid exactly like the Void Shard's (so it could never be crafted; `gen_all.check_recipes()` now fails the build on any such clash);
  `/usm reset` cleared the Heart Crystal counter but left the bonus for up to five seconds (`Gates.mutate` now re-applies hearts); a Tether that pulled for one tick only and a burst that missed a hovering
  boss's victims; the plague on undead (poison cannot touch them, the rot uses wither); flight that survived taking the armor off after a relog (`Wearer.tick` revokes it).
- Harness lessons: random invasions between suites killed a 20-HP test player (every suite now starts with `quiet()`); a suite that changes health during a boss's 70-tick phase transition must wait it out;
  `MobTests` steps the player onto the first spike line for `icicles` (standing on the bearing is the designed safe gap); never assert an absolute max-health value after another suite used hearts.
- **Not verified by running**: how any of it sounds or feels in a human's hands (sounds are re-pitched vanilla samples; balance is checked for completability by the soak fights, not for fun); the soak player is
  immortal, so no fight has been tested for being *survivable* by a real player beyond the telegraph/safe-spot checks in `Boss4Tests`; the older seven bosses keep their HP, so they die faster now that damage is 1.4-1.9x
  (`bossHealthMultiplier` in the config scales them up).

