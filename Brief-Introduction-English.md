# AnnaWathe — Introduction

AnnaWathe is an add-on for **Wathe**.

It reworks and enhances parts of Wathe's mechanics, and it provides enough API interfaces that add-on mods can hook in through the API instead of needing Mixins.

---

## Reworked & Enhanced Mechanics

### Instinct Sight

- Instinct can now be used as a **toggle key** instead of hold-only (adjustable via a debug command)
- Non-alive players can see corpses through walls (corpse colour shows the role colour)
- Non-alive players can see role colours through walls (compatible with add-on roles provided by HarpyModLoader)

### Mood System

- The mood system has been rewritten, adding **5 extra tasks** (crouch, run, sit, stay still, stay away from people)
- Mood drains faster, and the mood restored by completing a task is lower than in vanilla
- When mood reaches zero, the player **dies of a mental breakdown** (can be toggled with a debug command)
- When mood enters the depressive state, a **near-death warning** appears (while the mental breakdown toggle is on)
- When mood drops below a certain point, **new task slots unlock** (up to 3 stacked tasks)
- With multiple tasks stacked, completing one task **does not remove** the other active tasks
- When a task is impossible to complete, making progress on other tasks will eventually **remove the impossible task**

### Shop System

- The shop system has been reworked, and shop prices have been changed as well

### Item Tooltips

- Instead of reading a fixed cooldown time, it now correctly reads each item's **cooldown ticks** for display, avoiding abnormal cooldown display

### Task Point Sight

- Fixed task points can be seen through walls — toggled with the **`Y` key** by default (and holding the matching key lets you see your own door)

### Collision System

- For the first **30 seconds** of a round, players no longer collide with each other, preventing malicious gameplay behaviour

### Round-End Information

- The round-end system has been reworked, showing player names + role information (compatible with HarpyModLoader role information)

### Corpse HUD Information

- Shows a corpse's **cause of death, time of death and identity**

### A Fun Mechanic

- Admins can use the transformation commands to change other players' appearance! It is a lot of fun!

---

## Commands

### AnnaWathe Commands

| Command | Effect |
| --- | --- |
| `/annawathe:gamemode <mode> [player]` | Switch creative / spectator using "gameplay-alive" semantics. Players without a role in the current round cannot be granted special alive status |
| `/annawathe:playerCollision [true\|false]` | Query or toggle collision between gameplay-alive players |
| `/annawathe:startnoCollision [seconds]` | Query or set the collision-immunity duration after a round starts (`0` = restore collision immediately) |
| `/annawathe:transform <player> <appearance> permanent\|<seconds>` | Make a player look like another online player |
| `/annawathe:transform all <appearance> permanent\|<seconds>` | Make every other online player look like the specified player |
| `/annawathe:transform clear <player>` | Clear one player's transformation |
| `/annawathe:transform clearAll` | Clear every player's transformation |
| `/annawathe:transform query <player>` | Check whether a player is currently transformed |

### Mood Commands

| Command | Effect |
| --- | --- |
| `/wathe:setMood <0..1> [players]` | Set target players' mood |
| `/wathe:moodEffectDeath [true\|false]` | Query or set the "mental breakdown death" world toggle |
| `/wathe:moodTask list` | List all registered mood tasks |
| `/wathe:moodTask assign <task> [player]` | Assign the specified task |
| `/wathe:moodTask remove <task> [player]` | Remove the specified task (**no mood reward**) |
| `/wathe:moodTask complete <task> [player]` | Complete the specified task (**grants mood**, fires the completion event) |
| `/wathe:taskPoints` | Query task point count and auto-reload state |
| `/wathe:taskPoints reload` | Rescan task points and sync to all clients |
| `/wathe:taskPoints refresh` | Rebroadcast the cache only, without rescanning |
| `/wathe:taskPoints autoRefresh [true\|false]` | Query or set "automatically rescan task points at round start" |

### Instinct Commands

| Command | Effect |
| --- | --- |
| `/instinct` | Query the current instinct key mode |
| `/instinct key <true\|false>` | Set "press to toggle" or "hold to activate" |

---

## Open API

API systems currently available:

| Group | API | Purpose |
| --- | --- | --- |
| Visual · Instinct | `InstinctApi` | Priority rules for instinct availability and target highlighting |
| Visual · Crosshair | `CrosshairHudApi` | Crosshair provider / overlay plus built-in drawing helpers |
| Visual · Name | `RoleNameHudApi` | Crosshair names, raycast source, target filters, cohort state, extra HUD |
| Visual · Generic HUD | `HudOverlayApi` | Three-phase screen HUD registration and dispatch |
| Visual · Mood | `MoodHudApi` | Normal / role-coloured / psycho Mood HUD styles |
| Visual · Time | `TimeHudApi` | Priority display rules for the top countdown |
| Visual · Corpse | `BodyInfoApi` / `BodyInfoHudApi` | Corpse death time, cause of death, identity snapshot and visibility |
| Visual · Appearance | `PlayerAppearanceApi` / `BodyAppearanceApi` | Player and corpse skin resolution |
| Visual · Hiding | `HeldItemInvisibilityApi` / `PsychosisItemApi` | Held item hiding and low-mood hallucination items |
| Visual · Visibility | `TargetVisibilityApi` | Render / target / interact / attack target rules |
| Gameplay · Victory | `VictoryApi` / `CustomVictory` | Vanilla win interception, extra winners, custom victories |
| Gameplay · Tasks | `MoodTaskApi` / `MoodTaskPointApi` / `TaskCompletionApi` | Task registration and assignment, task point extensions, completion events and income |
| Gameplay · Mood | `MoodApi` | Mood values, drain multiplier, protection time, death toggle |
| Gameplay · Shop | `ShopApi` / `ShopEntry` / `ShopPrice` | Role shops, dynamic shops, priority modifiers, composite prices |
| Gameplay · Economy | `EconomyApi` / `PlayerEconomyApi` | Currency registration, balance access, passive income |
| Gameplay · Psycho | `PsychoModeApi` / `PsychoModeProfile` / `PsychoModeClientApi` | Psycho profiles, shield rules, client visuals |
| Gameplay · Life State | `PlayerLifeStateApi` | Treat creative / spectator as gameplay-alive |
| Gameplay · Movement | `PlayerMovementApi` | Speed modifier rule chain (no stamina system) |
| Gameplay · Collision | `PlayerCollisionApi` | SOLID / VANILLA_PUSH / NO_COLLISION |
| Gameplay · Transform | `PlayerTransformApi` | Persistent debug appearance transformation |
| Client · Items | `ItemTooltipApi` | Multi-line descriptions, real cooldown countdown, dynamic appenders |
| Client · Interaction | `InventoryButtonApi` / `InventoryPageState` | Inventory button lifecycle and paging |

This mod will keep opening up new API interfaces in future updates — stay tuned!

---

## Compatibility

- This mod supports Wathe's latest **1.4.1** version, and is also compatible with the older **1.3.2** version's mechanics.
- This mod is **soft-compatible with HarpyModLoader** — used via Mixin to fix forced-role bookkeeping and neutral-role priority assignment. Not installing HarpyModLoader does not affect gameplay.

### Required Mods

- Wathe
