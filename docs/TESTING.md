# ItemCooldowns 2.0 — verification report

## Scope

Testing used a separate disposable world/server and a real vanilla Minecraft client. Production worlds, publishing tokens and user secrets were not used. Java and Maven were installed under `/tmp` as requested.

| Environment | Result |
|---|---|
| Paper 1.21.11 build 132, Java 21 | Plugin enable / final build startup checked |
| Vanilla client 1.21.11, software rendering | In-game visual checks and authentic PNG captures |
| Tracker self-test | 11 assertions passed |
| Concurrent tracker use | 64 attempts, exactly one cooldown start |
| Bundled languages | All 10 files parse; all have the same 38 leaf keys |
| PlaceholderAPI absent | Plugin starts; optional integration safely stays off |
| PlaceholderAPI installed | Not integration-tested |
| Folia | Entity-region scheduling implemented; not runtime-tested |
| Spigot / Bukkit / Purpur / Pufferfish / Leaves | Targeted through compatible API paths; not runtime-tested |
| Other versions in 1.21–26.3 | Not runtime-tested |

**Do not advertise this table as complete certification for every version or fork.** Plugin bytecode targets Java 21; the server may require a newer runtime.

## In-game observations

- `/icd gui` opens a menu with black/purple glass borders and gold corners.
- The crystal menu entry shows `Ready`, or remaining seconds plus progress while its timer runs.
- Crystal block use starts the configured 45-second timer.
- ActionBar, BossBar and sidebar show a decreasing timer.
- With the crystal's native overlay disabled, another block-use attempt is cancelled and produces a readable blocked message and smoke particles.
- Blocked feedback is rate-limited to one burst per player per second; cancellation applies on every attempt.
- Charging a trident does **not** start its timer.
- Releasing a charged trident launches the projectile and starts the five-second timer. The launch capture includes both the projectile and the timer.
- The plugin loads without PlaceholderAPI; no hard runtime dependency is required.

Sounds are configured and code paths are present, but no auditory validation was recorded. The Creative → Survival switch, damage gating against a second human player, Riptide, optional item rules and in-game language switching have not been separately exercised in this run.

## Automated tracker checks

Run `bash tools/test-tracker.sh` after installing Maven/Java. This is an explicitly invoked standalone test, not a JUnit test automatically run by Surefire.

1. First use starts a timer.
2. A repeated use is blocked.
3. Another player is independent.
4. Another material is independent.
5. Active timer count is correct.
6. Per-material reset clears that timer.
7. Reset does not affect another player.
8. Player-wide reset clears that player's timers.
9. 64 concurrent attempts yield exactly one start.
10. Expired entries are ready.
11. Global reset clears all tracked players.

## Gallery provenance

All files in `gallery/originals/` are direct 1280×720 screen captures of the Minecraft client on the isolated Paper server.

- `02-bossbar-actionbar.png`: live HUD.
- `03-blocked-overlay.png`: blocked message, smoke and live timers. The filename is historical; `end-crystal.overlay` was **false** for this capture.
- `04-gui-menu.png`: new framed menu, ready state.
- `07-gui-active-cooldown.png`: new framed menu, active timer.
- `08-trident-throw.png`: real trident launch plus timer.
- `09-trident-charge-no-cooldown.png`: trident being charged with no trident timer.

The four top-level presentation PNGs add a dark backdrop, titles and an external purple/gold pixel frame. The full screenshot is pasted at native size without editing its in-game text or HUD. There are no AI-generated screenshots. `index.html` embeds all presentation images and works offline.

Menu/HUD/trident screenshots were captured from the v2 build with the new visual design and feedback limiting. A subsequent small source revision added drag cancellation, enabled the sidebar in fresh-install defaults, corrected crossbow shot handling and aggregated multi-material rule status. Final compilation, tracker checks and server startup were rerun; the full gameplay matrix was not rerun after those small changes.

## Known behavior / further checks

- Native inventory cooldown overlays can prevent the client from sending repeated-use packets. No server plugin can issue per-attempt feedback for packets it never receives. Disable the rule's overlay if this feedback is important.
- Cooldowns are per UUID and material, not one shared global timer per rule; changing spear variants can therefore use a different timer.
- `nearest` in commands/HUD/placeholders currently means the timer with the longest remaining time.
- Crystal/anchor timers gate interaction attempts, not confirmed placement success.
- Sidebar mode temporarily replaces the player's scoreboard; disable it when another plugin controls the sidebar. Restoration is best-effort.
- Vanilla inventory background colors cannot be fully changed without a resource pack. No resource pack is included.
- Reload clears plugin timers. Prefer a full restart after installation or updates, especially when native overlays are in use.

Before a broad public release, run the missing matrix above, including Folia with multiple regions, multiple simultaneous players, installed PlaceholderAPI, oldest supported Spigot and the intended 26.x Java/runtime combination.

## Pre-publication merge

GitHub main had advanced to `e615d3f` (v1.01). Its PvPManager hook, `use-mode`/legacy `block-use`, Creative option alias, anchor-use and crystal-entity interactions were merged into v2 before publication. The old 50 ms same-attack cooldown bypass was deliberately not restored; atomic check/start stays in place. Fresh v2 weapon defaults are `block`; explicit existing policies are retained. The merged build was recompiled and all 11 standalone tracker assertions rerun. Optional policy/PvPManager gameplay was not integration-tested, so this is a beta, not a broad compatibility certification.
