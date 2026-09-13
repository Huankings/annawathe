# AnnaWathe — Mod Overview (English)

> An extension framework mod for vanilla **Wathe: Murder Mystery (1.3.2 – 1.4.1)**, bundled with a full set of vanilla-enhancing gameplay rules
> Version `1.0.0-1.21.1` · Minecraft 1.21.1 · Fabric · Java 21

---

## Table of Contents

1. [What This Is](#1-what-this-is)
2. [Requirements & Installation](#2-requirements--installation)
3. [How It Relates to Wathe and Other Add-ons](#3-how-it-relates-to-wathe-and-other-add-ons)
4. [Feature Overview](#4-feature-overview)
5. [Mood & Task Mechanics](#5-mood--task-mechanics)
6. [Shop & Economy](#6-shop--economy)
7. [Psycho Mode](#7-psycho-mode)
8. [Instinct Sight](#8-instinct-sight)
9. [Screens & HUD](#9-screens--hud)
10. [Admin & Debug Commands](#10-admin--debug-commands)
11. [The Extension Framework](#11-the-extension-framework)
12. [Compatibility & Known Boundaries](#12-compatibility--known-boundaries)
13. [Known Issues & TODO](#13-known-issues--todo)
14. [Data Reference](#14-data-reference)
15. [Appendix: Build & Version Info](#15-appendix-build--version-info)

---

## 1. What This Is

AnnaWathe is an **extension framework mod** built on top of vanilla Wathe. It does two things.

**First, it replaces the substrate.** In vanilla Wathe, an add-on that wants to change instinct highlight colours, inject its own win condition, add mood tasks, or draw its own HUD must write Mixins into Wathe's internals — which collide with each other and break on every version bump. AnnaWathe takes over those points entirely and turns them into a uniform **registration-based API**: add-ons register *rules* and never touch Mixins again.

**Second, it ships its own enhanced ruleset.** While providing those interfaces, AnnaWathe is also their first consumer: it reprices the killer shop, reworks the mood drain curve, adds 5 new tasks, adds a task-point overlay, turns Psycho Mode into configurable profiles, redraws the welcome and round-end screens, and adds corpse death info. So it is both a *framework* and a *gameplay mod*.

It adds **no** new roles, items or blocks. Vanilla Wathe's four factions (Civilian / Killer / Vigilante / Loose End) and all of its existing items remain untouched.

**Scale:** 158 Java files, 6,574 lines of code; 8 CCA components; 48 Mixins; 70 files of public API with roughly 105 registration-based extension points.

---

## 2. Requirements & Installation

| Item | Requirement |
| --- | --- |
| Minecraft | 1.21.1 (locked for both build and runtime) |
| Java | 21 |
| Loader | Fabric Loader 0.17.2 or compatible |
| Base mod | **Vanilla Wathe `1.3.2-1.21.1` – `1.4.1-1.21.1`** (both the older 1.3.2 and the latest 1.4.1 are supported) |
| Required deps | Fabric API, Cardinal Components API 6.1.1 |
| Environment | Both sides — client and server must both install it |
| Soft compat (optional) | HarpyModLoader (two compat Mixins activate automatically when present) |

### Installation

1. Install Fabric Loader 0.17.2+ and Fabric API.
2. Put **vanilla Wathe** into `mods/` — either 1.3.2 or 1.4.1 works; AnnaWathe is compatible with both.
3. Put `annawathe-1.0.0-1.21.1.jar` into `mods/` (on both client and server).
4. On startup, make sure the log does not contain `was not registered through mod metadata or plugin` — that is the classic symptom of missing CCA metadata.

### Supported Wathe Versions

AnnaWathe supports **both Wathe 1.3.2 and 1.4.1** — that is, the whole `1.3.2 – 1.4.1` range.

- **At runtime**: installing either 1.3.2 or 1.4.1 plays correctly; both versions' mechanics have been adapted.
- **Dependency declaration**: the recommended form is `"wathe": ">=1.3.2-1.21.1 <=1.4.1-1.21.1"`, pinning the upper bound to the verified 1.4.1 so a future Wathe release is not silently accepted.

> **Note (project state at the time of writing):** this project's `gradle.properties` and `libs/` still use `wathe-1.3.2-1.21.1.jar` as the **compile** dependency, so the `fabric.mod.json` visible in the SDK declares `"wathe": ">=1.3.2-1.21.1"` — that constrains only the **lower** bound and already permits 1.4.1 and newer at runtime. Making the upper bound effective requires changing that declaration to the recommended form above. Other than that, 1.3.2 is merely the compile baseline and **does not mean AnnaWathe can only run on 1.3.2**.

### Important Warning

> **Never load a modified Wathe jar together with AnnaWathe.**
> AnnaWathe only pairs with vanilla Wathe. Modified Wathe builds (for example `Wathe - 副本1`) and AnnaWathe are two independently maintained lines; loading both causes startup crashes or broken mechanics.
> Likewise, **never keep two AnnaWathe versions installed**. Delete the old jar before testing.

---

## 3. How It Relates to Wathe and Other Add-ons

| Project | Type | Relationship to AnnaWathe |
| --- | --- | --- |
| **Wathe: Murder Mystery** (doctor4t) | Base mod | AnnaWathe's **only base dependency**; the murder-mystery social game core on 1.21.1 |
| **AnnaWathe** (this project) | Extension framework + ruleset | Takes over Wathe's extension points, exposes a registration API, and ships enhanced gameplay |
| **Kin's Wathe** | Content add-on | Mainly **adds roles** (7 civilians + 5 killers + 3 neutrals), modifiers and a config screen; requires HarpyModLoader |
| **Wathe: Extended** | Content/config add-on | Mainly **adds a guidebook GUI, modifiers, map tooling and a config screen**, plus a batch of Wathe/add-on bug fixes |
| **Noelle's Roles / Stupid Express / Starry Express** | Content add-ons | Add roles, custom victories, decoration and guidebooks |

**The key difference:** Kin's Wathe and Wathe: Extended *add things to the game*; AnnaWathe *replaces the rules underneath and opens the interfaces*. If you want more roles, install the former. If you want your own add-on to stop writing Mixins — or you want a repriced shop, a reworked mood curve, or a task-point overlay — use AnnaWathe.

AnnaWathe does **not** bundle the concrete role rules of NoellesRoles or StupidExpress. Those roles' win conditions and tasks are registered by their own add-ons through AnnaWathe's API.

---

## 4. Feature Overview

### 4.1 Core Gameplay Changes

| Feature | Description |
| --- | --- |
| **Independently priced killer shop** | All 12 prices are maintained by AnnaWathe and **deliberately do not read** vanilla `GameConstants.SHOP_ENTRIES`, so balance can be tuned separately |
| **Multi-currency economy** | Register arbitrary currencies, AND/OR composite prices, passive income and task income; only money is enabled by default |
| **Reworked mood curve** | Uses the modified-Wathe values: `1/4000` drain per tick (empty in ~3m20s), `0.4` restored per task completion; **multiple tasks no longer multiply the drain** |
| **Mental breakdown death** | Mood reaching zero actually kills you, with death reason `wathe:mental_breakdown`; a world-level toggle can disable it |
| **5 new tasks** | Crouch, run, sit, stay still, stay away from people (see §5) |
| **Task-point overlay** | 5 task-point types rendered through walls; toggled with the `Y` key |
| **Psycho Mode as profiles** | Psycho Mode becomes a registrable profile: custom duration, shields, weapon, temporary items, hotbar locking, skin and ambience |
| **Redrawn round-end screens** | Welcome announcement and win/loss summary rewritten: dynamic columns, avatars, death markers, dual-section custom victories, adaptive scaling |
| **Corpse death info** | Corpses show "time of death + cause + victim's role", visible to spectators/creative by default |
| **Spawn collision immunity** | For 30 seconds after a round starts, players do not collide with each other |
| **Debug transformations** | Admins can make a player look like any online player — timed or permanent, persisted across rounds |

### 4.2 UI & HUD Changes

| UI | What changed |
| --- | --- |
| Crosshair | Vanilla 3x3 crosshair fully taken over; add-ons can register custom crosshairs and progress icons below the crosshair |
| Crosshair name | Player names below the crosshair taken over; registrable raycast source, target filter, cohort state and extra HUD |
| Mood HUD | Three styles (normal / role-coloured / psycho) with registrable bars, icons, arrows, overlays and warnings |
| Top countdown | Registrable priority countdown displays with independent scrolling digits and low-time warnings |
| Shop balance | Multi-currency balances in the top-right; a zeroing animation plays before fading out |
| Inventory buttons | Custom buttons and paging across the limited, vanilla and creative inventory screens |
| Item tooltips | Unified multi-line descriptions and **real cooldown countdowns** |
| Generic screen HUD | Three-phase free drawing (before HUD / after Wathe's main HUD / after the whole HUD), with a controlled hotbar re-render entry point |
| Corpse info | A death-summary line and an identity line, independently toggleable |

### 4.3 Administrative Features

- Switch creative/spectator using *gameplay-alive* semantics (`/annawathe:gamemode`);
- Player collision master switch and spawn collision-immunity duration;
- Debug commands for mood, tasks, task-point cache and the mental-breakdown toggle;
- Player appearance transformation (timed / permanent / all players / query / clear).

---

## 5. Mood & Task Mechanics

### 5.1 Mood Values (matching the modified Wathe)

| Constant | Value | Meaning |
| --- | --- | --- |
| Base drain rate | `1 / 4000` per tick | Full to zero in about **3 minutes 20 seconds** (200 s) |
| Task completion reward | `0.4` | Per completed real mood task |
| Do multiple tasks stack? | **No** | One drain per tick whether you hold 1 or 3 tasks |
| Extra task-slot thresholds | `0.51` / `0.17` | Mood > 0.51 → 1 slot; 0.17–0.51 → 2 slots; ≤ 0.17 → 3 slots |
| Breakdown warning threshold | `0.15` | Below this, a shaking warning appears on screen |
| Task cap | 3 | `MAX_TASKS` |
| First task delay | 600 ticks (30 s) | `TIME_TO_FIRST_TASK` |
| Refill cooldown | Random 600–1200 ticks (30–60 s) | Draws the next task when you hold none |

> **Why not vanilla values:** vanilla Wathe (both 1.3.2 and 1.4.1) drains slower and rewards `0.5` per task. AnnaWathe pins the modified-Wathe pacing; a source comment states this explicitly so the game does not silently fall back to the vanilla feel at runtime.

**Tuning interfaces:** `MoodApi.setDrainMultiplier(float)` multiplies the base drain; `MoodApi.protectFromDrain(ticks)` pauses it temporarily; `MoodApi.setMoodDeathEnabled(bool)` is the world-level toggle.

### 5.2 Built-in Tasks (9)

All 9 tasks enter the random pool. Weights default to `1.0` and **decay with how often the task has already been drawn** — the more often you get it, the less likely it is next time.

| Task ID | Translation key | Completion condition | Task point |
| --- | --- | --- | --- |
| `wathe:sleep` | `task.sleep` | Sleeping for **160 ticks cumulative (8 s)** | Bed |
| `wathe:outside` | `task.outside` | Under open sky for 160 cumulative ticks | — |
| `wathe:eat` | `task.eat` | Eating food (instant) | Food tray |
| `wathe:drink` | `task.drink` | Drinking a cocktail (instant) | Cocktail tray |
| `annawathe:shift` | `annawathe.task.shift` | Sneaking (`isSneaking()`) for 160 cumulative ticks | — |
| `annawathe:run` | `annawathe.task.run` | Sprinting (`isSprinting()`) for 160 cumulative ticks | — |
| `annawathe:sit` | `annawathe.task.sit` | Sitting on a seat for 160 cumulative ticks | Seat |
| `annawathe:stay` | `annawathe.task.stay` | Movement² ≤ `1.0E-4` (~0.01 blocks) per tick while on ground or in a vehicle, for 160 cumulative ticks | — |
| `annawathe:away` | `annawathe.task.away` | No other gameplay-alive player within **12 blocks**, for 160 cumulative ticks | — |

The first 4 wrap vanilla tasks (`BuiltInMoodTasks.Legacy`); the last 5 are new AnnaWathe tasks (`Timed`, whose timer only decrements while the condition holds).

In-game prompt strings (as shipped in the language files):

- Real task: `annawathe.task.feel` "You feel like " + task text;
- Fake task: `annawathe.task.fake` "You could fake " + task text;
- Task text: crouching for a bit / going for a run / sitting down for a bit / staying still / staying away from other people / completing an unknown task.

### 5.3 Task Points

| Task point | ID | Colour | Detected on |
| --- | --- | --- | --- |
| Bed | `annawathe:bed` | `#57D6FF` | Bed blocks (normalised to the head half) |
| Food tray | `annawathe:food_tray` | `#61D95C` | Food on a beverage plate |
| Cocktail tray | `annawathe:cocktail_tray` | `#FF85A8` | Cocktail on a beverage plate |
| Seat | `annawathe:seat` | `#7AF4E1` | Mountable blocks |
| Matching door | `annawathe:keyed_door` | `#FFF79B` | Small doors that carry a key name |

**Scanning is strictly bounded:** only the intersection of the train's translated reset-template area and the map's `playArea` is scanned. Add-on scan handlers may only inspect the current block — they are not allowed to sweep the world themselves.

**Overlay behaviour:**

- Default key **`Y`** (rebindable; the category shows as Wathe). Toggling prints a green/red action-bar message;
- Gameplay-alive players only see task points belonging to **their current task**; spectators/creative see every registered type;
- Doors are hidden by default and only appear when you hold a `wathe:key` in your main hand whose first LORE line matches that door's key name;
- Within 3 blocks a type label is drawn; multiple types are joined with ` / `.

Task-point data is scanned server-side as a whole table and synced to clients through a dedicated S2C packet, sent on player join and when round initialisation finishes.

### 5.4 Mental Breakdown Death

When mood hits zero and the toggle is on, the player dies of `wathe:mental_breakdown`.

- Language files: `death_reason.wathe.mental_breakdown` "mental breakdown", `replay.death.wathe.mental_breakdown.died` "%s died of mental breakdown";
- Toggle: `/wathe:moodEffectDeath [true|false]`;
- With it disabled, mood still drains and tasks are still assigned — you just cannot die from it.

The check is **deliberately performed after task completion is settled**, because completing a task can push mood back above zero within the same tick; settling first prevents false deaths.

### 5.5 Stuck-task Handling

The system tracks a "stuck count" per task. When you hold 2+ tasks that are all stuck 6 times, or exactly 1 task stuck 4 times, the stuck tasks are removed automatically and the cooldown is reset, so a player can never be permanently locked out by impossible tasks.

---

## 6. Shop & Economy

### 6.1 Default Killer Shop Prices

All prices are maintained by AnnaWathe and **do not read vanilla `GameConstants.SHOP_ENTRIES`**.

| Category | Item | Price (money) |
| --- | --- | --- |
| Weapon | Knife | 100 |
| Weapon | Revolver | 250 |
| Weapon | Grenade | 300 |
| Weapon | Psycho Mode | 350 |
| Poison | Poison Vial | 70 |
| Poison | Scorpion | 40 |
| Tool | Firecracker | 10 |
| Tool | Lockpick | 50 |
| Tool | Crowbar | 25 |
| Tool | Body Bag | 70 |
| Tool | Blackout | 250 |
| Tool | Note (4 at once) | 10 |

The shop UI has exactly three slot categories: weapon, poison and tool.

### 6.2 Multi-currency & Economy

Two currencies are registered by default:

| Currency | ID | Status |
| --- | --- | --- |
| Money | `wathe:money` | Enabled; used by the shop and HUD |
| Task money | `wathe:task_money` | **Definition and income paths are kept, but everything is off by default** (no HUD entry, zero income) |

**Where balances live:** money uses the vanilla `wathe:shop` CCA's `balance` field; other currencies are stored in the `CurrencyBalances` NBT that AnnaWathe injects onto the same component, following vanilla's sync, reset and respawn strategy. Add-ons do not need to send their own sync packets.

**Price model:** `ShopPrice` supports composite prices — multiple *options* are **OR**'d, while multiple currencies inside one option are **AND**'d. When several options are affordable, the one with the **smallest total currency amount** wins; ties go to the earlier-defined option. Unaffordable entries display localised "or" / "Free" text.

**Passive income:** you can register which roles receive passive income, income rules, and income value modifiers. All three follow the same rule chain: higher `priority` runs first, later registrations win at equal priority.

### 6.3 Purchase Flow & Server-side Validation

The client sends **only the shop entry index** — never a price or item payload. The server re-validates in this order:

1. Re-resolve the player's current shop list (including role shops and every modifier);
2. Whether the game is running and the player is gameplay-alive;
3. Whether the index is in range;
4. Recompute the best payment option and check the balance;
5. Whether the item is on cooldown.

**Deliver first, charge after:** currency is deducted atomically only after the item has actually been delivered (validate every currency, deduct once, sync once) — so multi-currency payments can never half-charge. Purchase callbacks registered by add-ons are **not allowed** to charge the player themselves.

> In development environments there is a convenience path: an insufficient balance is topped up to 10× the price for testing. Production servers never reach it.

---

## 7. Psycho Mode

Vanilla Psycho Mode is reworked into a **registrable profile state machine**. The default profile reproduces the vanilla feel.

### 7.1 Default Profile Parameters

| Parameter | Value | Notes |
| --- | --- | --- |
| Duration | **600 ticks (30 s)** | `PSYCHO_TIMER` |
| Shield layers | **1** | Blocks one lethal hit |
| Granted item | Bat | Placed into the hotbar when it starts |
| Melee kill | Enabled | A full-swing bat hit kills; death reason `wathe:bat_hit` |
| Lock hotbar | Enabled | Cannot switch to another slot during the ability |
| Lock granted items | Enabled | Cannot drop or swap them away |
| Remove granted items on end | Enabled | Precisely reclaims items granted by this profile |
| Auto-select granted item | Enabled | |
| Skin | `psycho.png` / `psycho_thin.png` | slim/wide chosen from the player's model |
| Hide model features | Enabled | Hides normal features, but **always keeps the held item layer** |
| Ambience | `AMBIENT_PSYCHO_DRONE` every 20 ticks | |
| Hit / shield sounds | `ITEM_BAT_HIT` / `ITEM_PSYCHO_ARMOUR` | |

**Precondition to start:** the number of granted items must not exceed the free hotbar slots, otherwise the start call fails rather than force-stuffing a full hotbar.

### 7.2 Shields & Extension Points

- **Shield arbitration:** add-ons can register shield-bypass / special shield rules; rules are sorted by `priority` and the first non-PASS result wins. With no rule registered, it falls back to "shield > 0 blocks one hit, otherwise it does not".
- **Hotbar locking defended in three places:** scroll-wheel switching rolls back, number-key switching is rejected, and the server discards client packets that try to select a non-locked slot — a malicious client cannot bypass it.
- **Registrable extension points:** register profiles, register start-profile providers, register shield rules, register visual providers, register background ambience, replace the psycho skin, and control feature hiding.
- **Client-side hallucination items:** at low mood, the observer's client sees fake held items and arm poses. These live **only in the viewer's local cache** and are cleared on death, round stop, reset and disconnect; nothing is written to any save.

---

## 8. Instinct Sight

Instinct is split into two independent registration chains: *availability* (can it be used) and *highlight* (what colour targets get). Default rules:

**Default availability (`annawathe:default_instinct`):** stays neutral while the key is inactive; when active it enables for players who can use killer features and are gameplay-alive, or who are spectating/creative.

**Actual colours of the default highlight (`annawathe:default_highlight`):**

| Target | Condition | Colour |
| --- | --- | --- |
| Fellow killer | Both can use killer features | `#990000` dark red |
| Civilian (mood ≥ 0.55) | Gameplay-alive | `#4EDD35` bright green |
| Civilian (mood 0.2–0.55) | Gameplay-alive | `#1FAFAF` teal |
| Civilian (mood < 0.2) | Gameplay-alive | `#171DC6` deep blue |
| Dropped items / notes / firecrackers | | `#DB9D00` gold |
| Corpses | Spectator/creative viewers only | The victim's role colour |
| Living players | Spectator/creative viewers only | Target's role colour (white if none) |

> Gameplay-alive players do **not** get outlines on corpses — deliberate design; corpse information goes through its own HUD channel.

**Key modes:** both "press to toggle" and "hold to activate" are supported. Toggle is the default and can be changed with `/instinct key <true|false>`.

**Boundary:** instinct is purely a client-side visual. Real attacks, interactions, purchases and ability legality are always re-validated on the server.

---

## 9. Screens & HUD

### 9.1 Welcome & Round-End Screens

The welcome announcement and win/loss summary are fully rewritten:

- **Dynamic column counts:** 5 columns for civilians (auto-splitting with many players), 3 for the dual civilian/killer section, 6 for Loose Ends;
- **Real role titles**, role colours, and the neutral title colour `#CC6600` (matching vanilla `RoleAnnouncementTexts.NEUTRAL`);
- **Player avatars** with skin caching and a failure cache, fetched asynchronously per player;
- **Death markers:** dead players' avatars are darkened and crossed out;
- **Dual-section custom victory layout** with per-group titles;
- **Adaptive text:** names and role labels scale down or truncate (minimum scale 0.3), narrow windows reduce the column count, and partial rows keep the current alignment;
- Vanilla sound sequences play on welcome and end.

### 9.2 Crosshair & Crosshair Name

- **Crosshair:** the vanilla 3x3 crosshair is fully taken over. Add-ons can register a *provider* to replace the default crosshair (a short-circuiting chain, highest priority first) or an *overlay* to append small hints after the default crosshair (never short-circuits). Built-in drawing helpers cover the standard normal/target crosshair, knife progress icon, bat progress icon, and custom 10×7 icon progress bars.
- **Crosshair name:** player names below the crosshair are taken over, with nine registrable rule types — HUD visibility, raycast source entity, player target filter, player name text, non-player entity name text, cohort classification, cohort target display, cohort hints, and extra HUD. Default look range is 8 blocks for spectators/creative and 2 blocks when alive.
- **Psycho identity masking:** while in Psycho Mode, a player's name renders as dark-red obfuscated `urscrewed` plus random characters.

### 9.3 Mood HUD

Three styles, all replaceable by add-on registrations:

| Style | Icons | Notes |
| --- | --- | --- |
| Normal (real tasks) | `mood_depressive` / `mood_mid` / `mood_happy` | Icon switches with mood, arrows enabled, bar colour follows mood via HSV |
| Role-coloured (fake tasks) | fixed `mood_killer` | No arrows, dark red bar |
| Psycho | `mood_psycho` / `_hit` / `_eyes` | Gaussian shake, horizontally scrolling "Kill them all!", countdown bar, 12 ghost layers |

Below mood `0.15` the string `annawathe.hud.mood.breakdown_warning` ("MENTAL BREAKDOWN IMMINENT") appears with a shake that intensifies with progress.

### 9.4 Top Countdown

The round countdown at the top of the screen is taken over. The default provider only shows it when the role can see time, or when spectating/creative while a round is running. Add-ons can register higher-priority countdowns with dynamic colouring, low-time warnings, or a fixed colour; switching between different time sources resets the scrolling digit animation.

### 9.5 Shop Balance & Tooltips

- **Balance display:** one line per currency in the top-right (visibility decided by a registered predicate). A balance reaching zero plays a zeroing animation before fading out instead of vanishing instantly.
- **Tooltips:** unified multi-line item descriptions plus **real cooldown countdowns**. Cooldowns read `endTick - tick` from the live `ItemCooldownManager` entry rather than reverse-engineering a fixed total, so cooldown acceleration or reduction displays correctly. AnnaWathe disables vanilla Wathe's tooltip callback — do not register another global callback for the same items.

### 9.6 Inventory Buttons & Generic Screen HUD

- **Inventory buttons:** custom buttons are supported on the limited, vanilla and creative inventory screens. Each screen opening creates a fresh extension instance with a full `init` / `tick` / `render` / `allowInventoryKeyClose` / `close` lifecycle. Dynamic widgets are managed as groups and are hidden, disabled and unfocused on close; page state is isolated per ID and cleared on disconnect and round changes.
- **Generic screen HUD:** three drawing phases — before the main HUD, after Wathe's main HUD, and after the entire HUD. Suited to role status text, full-screen overlays and sniper scopes. To keep the hotbar visible, re-render it through the controlled entry point; do **not** Mixin the vanilla HUD again.
- **Corpse info HUD:** a "time of death + cause" line and an "identity" line, visible only to non-alive observers by default; each field toggles independently, and role names fall back through registered name → Harpy role name → vanilla translation key.

---

## 10. Admin & Debug Commands

Every command below **requires permission level 2**.

### AnnaWathe Commands

| Command | Effect |
| --- | --- |
| `/annawathe:gamemode <mode> [player]` | Switch creative/spectator using gameplay-alive semantics. Players without a role in the current round cannot be granted special alive status |
| `/annawathe:playerCollision [true\|false]` | Query or toggle collision between gameplay-alive players |
| `/annawathe:startnoCollision [seconds]` | Query or set the collision-immunity duration after a round starts (`0` = immediate collision) |
| `/annawathe:transform <player> <appearance> permanent\|<seconds>` | Make one player look like another online player |
| `/annawathe:transform all <appearance> permanent\|<seconds>` | Make every other online player look like the given player |
| `/annawathe:transform clear <player>` | Clear one player's transformation |
| `/annawathe:transform clearAll` | Clear every player's transformation |
| `/annawathe:transform query <player>` | Check whether a player is currently transformed |

### Mood & Task Commands (`/wathe:` namespace)

| Command | Effect |
| --- | --- |
| `/wathe:setMood <0..1> [players]` | Set target players' mood |
| `/wathe:moodEffectDeath [true\|false]` | Query or set the world-level mental-breakdown toggle |
| `/wathe:moodTask list` | List all registered mood tasks |
| `/wathe:moodTask assign <task> [player]` | Assign a task |
| `/wathe:moodTask remove <task> [player]` | Remove a task (**no mood reward**) |
| `/wathe:moodTask complete <task> [player]` | Complete a task (**grants mood**, fires the completion event) |
| `/wathe:taskPoints` | Query task-point count and auto-reload state |
| `/wathe:taskPoints reload` | Rescan task points and sync to all clients |
| `/wathe:taskPoints refresh` | Rebroadcast the cache without rescanning |
| `/wathe:taskPoints autoRefresh [true\|false]` | Query or set automatic rescan at round start |

### Instinct Commands

| Command | Effect |
| --- | --- |
| `/instinct` | Query the current instinct key mode |
| `/instinct key <true\|false>` | Set "press to toggle" or "hold to activate" |

**`assign` vs `complete`:** `removeTask` deletes silently — no mood, no completion animation, no completion event. Only `completeTask` runs the real flow: restores `0.4` mood, plays the animation, and fires the completion event and task-income settlement.

---

## 11. The Extension Framework

AnnaWathe exposes **24 API groups**, all under the `dev.annawathe.api` package, 70 files in total. Full method signatures, result semantics and code examples are in **[`02-API-Reference-English.md`](02-API-Reference-English.md)**.

| Group | API | Purpose |
| --- | --- | --- |
| Visual · Instinct | `InstinctApi` | Priority rules for instinct availability and target highlighting |
| Visual · Crosshair | `CrosshairHudApi` | Crosshair providers/overlays plus built-in drawing helpers |
| Visual · Name | `RoleNameHudApi` | Crosshair names, raycast source, target filters, cohort state, extra HUD |
| Visual · Generic HUD | `HudOverlayApi` | Three-phase screen HUD registration and dispatch |
| Visual · Mood | `MoodHudApi` | Normal / role-coloured / psycho mood HUD styles |
| Visual · Time | `TimeHudApi` | Priority display rules for the top countdown |
| Visual · Corpse | `BodyInfoApi` / `BodyInfoHudApi` | Corpse death time, cause and identity snapshots plus visibility |
| Visual · Appearance | `PlayerAppearanceApi` / `BodyAppearanceApi` | Player and corpse skin resolution |
| Visual · Hiding | `HeldItemInvisibilityApi` / `PsychosisItemApi` | Held-item hiding and low-mood hallucination items |
| Visual · Visibility | `TargetVisibilityApi` | Render / target / interact / attack rules for entities |
| Gameplay · Victory | `VictoryApi` / `CustomVictory` | Vanilla-win interception, extra winners, custom victories |
| Gameplay · Tasks | `MoodTaskApi` / `MoodTaskPointApi` / `TaskCompletionApi` | Task registration, task-point extensions, completion events and income |
| Gameplay · Mood | `MoodApi` | Mood values, drain multiplier, protection time, death toggle |
| Gameplay · Shop | `ShopApi` / `ShopEntry` / `ShopPrice` | Role shops, dynamic shops, priority modifiers, composite prices |
| Gameplay · Economy | `EconomyApi` / `PlayerEconomyApi` | Currency registration, balance access, passive income |
| Gameplay · Psycho | `PsychoModeApi` / `PsychoModeProfile` / `PsychoModeClientApi` | Psycho profiles, shield rules, client visuals |
| Gameplay · Life state | `PlayerLifeStateApi` | Treat creative/spectator as gameplay-alive |
| Gameplay · Movement | `PlayerMovementApi` | Speed modifier chain (no stamina system) |
| Gameplay · Collision | `PlayerCollisionApi` | SOLID / VANILLA_PUSH / NO_COLLISION |
| Gameplay · Transform | `PlayerTransformApi` | Persistent debug appearance transformations |
| Client · Items | `ItemTooltipApi` | Multi-line descriptions, real cooldown countdowns, dynamic appenders |
| Client · Interaction | `InventoryButtonApi` / `InventoryPageState` | Inventory button lifecycle and paging |

### Unified Rule Semantics

Nearly every API uses the same priority semantics, so add-ons can combine freely:

- Higher `priority` runs **first**;
- At equal priority, the **later registration wins**;
- Registering the same ID again **replaces** the old rule;
- Result enums are uniformly `PASS` (continue) or an explicit terminal value (`ENABLE`, `HIDE`, `ALLOW`, `DENY`, `SHOW`, …).

### Three Hard Boundaries

1. **Client visuals never replace server validation.** Crosshairs, names, HUDs, skins and hallucinations are display-only; real attacks, interactions, purchases and ability legality must be re-checked on the server.
2. **Do not re-Mixin low-level entry points that are already taken over.** Crosshair, crosshair name, generic HUD, mood renderer, time HUD, shop renderer, round-end renderer, collision and alive checks are all fully intercepted — use the matching API instead.
3. **Do not depend on the `bridge` package or Mixin injection points.** Those are AnnaWathe internals and may change between versions.

---

## 12. Compatibility & Known Boundaries

### 12.1 Compatibility

| Target | Status |
| --- | --- |
| Vanilla Wathe 1.3.2 – 1.4.1 | **Supported range**; 1.4.1 is also adapted |
| HarpyModLoader | **Soft compatible**: two compat Mixins load only when it is detected, handling forced-role bookkeeping and neutral-role priority assignment. Not a dependency; inert without it |
| NoellesRoles / StupidExpress / StarryExpress | Their role rules are not bundled; those add-ons must register victories and tasks through the API |
| Modified Wathe | **Incompatible** — never load both |
| Iris / shaders | No special handling in the source |

One implementation detail of the Harpy compat Mixins is worth noting: it decides whether to apply by checking mod metadata only, and **does not pre-load the target classes** — pre-loading would make NoellesRoles' own later injections fail. All reflection exceptions are swallowed and vanilla behaviour is preserved.

### 12.2 Explicit Boundaries

- **No stamina system.** `PlayerMovementApi` only provides a speed modifier chain (`ADD` / `MULTIPLY` / `OVERRIDE` / `PASS`). The modified Wathe's stamina drain, mood-based stamina penalty and jump restrictions were **not** migrated.
- **No blackout, fog or replay-event compatibility** mechanisms.
- **Some APIs are currently skeletons.** `TargetVisibilityApi` has **no built-in rules** inside AnnaWathe (everything passes by default); `VictoryApi` bundles no concrete role rules; `CustomVictory` currently has no internal caller. They are interfaces for add-ons and change nothing on their own.
- **`PlayerMovementApi.canSelfMove` / `canJump` always return `true`** and currently have no callers — reserved interfaces.
- **Debug transformations are cosmetic only.** They change no role, faction, sound, real item, collision or server-side identity, and rank below higher-priority visual rules such as role disguises and hallucination views.
- **`PlayerLifeStateApi` only changes gameplay-alive checks**, not vanilla creative/spectator permissions or item consumption. A normal `/gamemode` revokes the grant.
- **Corpse appearance must never change the real owner UUID** — the real UUID is required for coroner checks, body bags, replays and death resolution.

---

## 13. Known Issues & TODO

The following were found during source analysis and are **genuine gaps in the current version**, recorded here so they are not forgotten.

### 13.1 `CustomVictory` does not assign `fallbackTitle`

`CustomVictory.Builder.build()` never assigns the record's `fallbackTitle` field. If an add-on calls `customWin` without explicitly calling `.fallbackTitle(...)` and the language file lacks the matching key, the round-end screen renders text such as **"null Wins"**. `CustomVictoryGroup` has the same problem.

**Workaround:** always call `.fallbackTitle(...)` explicitly when registering a custom victory.

### 13.2 `CustomVictory` does not serialise the group title

`CustomVictory.writeToNbt` serialises `announcement`, `detail`, `fallback`, `color`, `winners` and `group`, but **not** the group `title` translation key produced by the Builder. After one NBT round-trip (save reload, cross-dimension sync, etc.) the group title is lost and falls back to the fallback text.

### 13.3 Other Observations

- `MoodHudApi.shouldRender(GameMode)` is public but never called inside AnnaWathe — the mood HUD gate is actually decided by `AnnaMoodRenderer` via `moodType != NONE`. Calling it is harmless but will not affect the default mood HUD.
- `VictoryBridge.capture()` is currently an empty implementation kept purely as an extension point.
- A commented-out alternative mood-drain formula (the hand-computed "3m20s" version) remains in the source; the active constant is `1 / getInTicks(3, 20)`.

> If you would rather not publish this chapter, simply delete it — no other chapter depends on it.

---

## 14. Data Reference

### 14.1 CCA Components

| Component | CCA id | Attached to | Purpose |
| --- | --- | --- | --- |
| `PlayerInstinctComponent` | `annawathe:instinct` | Player | Instinct key mode (toggle by default), persisted across respawns |
| `AnnaRoundEndState` | `annawathe:round_state` | World + Scoreboard | Custom victory data, extra winner UUIDs, role snapshots |
| `AnnaMoodSettings` | `annawathe:mood_settings` | World | Mental-breakdown death toggle (on by default) |
| `AnnaTaskPointWorldState` | `annawathe:task_points` | World | Task-point cache and auto-rescan at round start (on by default) |
| `PlayerLifeStateComponent` | `annawathe:life_state` | Player | Gameplay-alive grant for creative/spectator, **never copied on respawn** |
| `PlayerAppearanceOverrideComponent` | `annawathe:appearance_override` | Player | Debug transformation target and expiry, persisted across respawns |
| `AnnaCollisionSettings` | `annawathe:collision_settings` | World | Collision master switch (on), spawn immunity seconds (30), round start tick |
| `AnnaBodyInfoComponent` | `annawathe:body_info` | Corpse | Death cause, role at death, death world time |

### 14.2 Network Packets

| Packet | Direction | Payload |
| --- | --- | --- |
| `annawathe:task_point_sync` | Server → Client | The whole task-point table (position → set of types) |
| `annawathe:store_buy` | Client → Server | Shop entry index only |

### 14.3 Translation Key Conventions

| Kind | Format | Example |
| --- | --- | --- |
| Custom victory announcement | `announcement.win.<namespace>.<path>` | `announcement.win.mymod.lone_winner` |
| Custom victory detail | `game.win.<namespace>.<path>` | `game.win.mymod.lone_winner` |
| Custom victory group title | `announcement.role.<namespace>.<path>` | `announcement.role.mymod.neutral` |
| Item description | `<item translation key>.tooltip` | `item.wathe.knife.tooltip` |
| Death reason | `death_reason.<namespace>.<path>` | `death_reason.wathe.mental_breakdown` |
| Corpse info | `hud.annawathe.body.death_info` / `hud.annawathe.body.role_info` | fixed |
| Task points | `annawathe.hud.task_point.*` | `annawathe.hud.task_point.bed` |

### 14.4 Key Bindings & Sounds

| Item | Value |
| --- | --- |
| Task-point overlay key | `Y` (category: Wathe) |
| Instinct key | Reuses Wathe's instinct key binding |
| Psycho ambience | `AMBIENT_PSYCHO_DRONE`, every 20 ticks |
| Psycho hit / shield sounds | `ITEM_BAT_HIT` / `ITEM_PSYCHO_ARMOUR` |
| Welcome/end sound sequence | `UI_RISER`, `UI_PIANO` (vanilla Wathe sounds) |

---

## 15. Appendix: Build & Version Info

### 15.1 Building from Source

```powershell
cd "D:\哈比快车最新源码\原版哈比列车\annawathe"
.\gradlew.bat build
```

Artifacts: `build/libs/annawathe-1.0.0-1.21.1.jar` (~530 KB) and `annawathe-1.0.0-1.21.1-sources.jar`.

**Delete the old jar before testing — keep exactly one version installed.**

> **Build verification status (measured while writing this document):** running `.\gradlew.bat build --offline` with Gradle 9.0.0 + Fabric Loom 1.13.6 returned `BUILD SUCCESSFUL` with every task `UP-TO-DATE`, i.e. the current source matches the artifact in `build/libs`. The `fabric.mod.json` extracted from inside the jar confirms `id = annawathe`, `version = 1.0.0-1.21.1`, `environment = *` and three registered Mixin configs.
>
> The build also reports "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10". That is a **future-compatibility warning only**; the build completes normally on Gradle 9.0.0 and the artifact is unaffected.

### 15.2 Build Dependencies

| Item | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Yarn mappings | 1.21.1+build.3 |
| Fabric Loader | 0.17.2 |
| Fabric API | 0.116.7+1.21.1 |
| Cardinal Components API | 6.1.1 |
| Ratatouille | 1.4.3-1.21.1 |
| MidnightLib | 1.5.7-fabric (dev runtime only) |
| Wathe | **1.3.2-1.21.1 – 1.4.1-1.21.1 (supported runtime range)** |
| Wathe (compile dep) | `libs/wathe-1.3.2-1.21.1.jar` — the current compile baseline is 1.3.2; that is a build choice, and 1.4.1 is supported at runtime |
| Java | 21 |

### 15.3 Source Layout

```text
annawathe/
├─ libs/wathe-1.3.2-1.21.1.jar          Base mod compile dependency (compile baseline 1.3.2; runtime compatible with 1.3.2–1.4.1)
├─ src/main/java/dev/annawathe/         Server / common: API, CCA, Mixins, commands, network, tasks
│  ├─ api/                              Public API (45 files)
│  ├─ bridge/                           Internal bridges (add-ons must not use these)
│  ├─ cca/                              Cardinal Components components
│  ├─ command/                          Admin and debug commands
│  ├─ compat/                           Wathe version-difference compatibility
│  ├─ mixin/                            Server Mixins (18) + Harpy compat (2)
│  ├─ mood/                             Mood task state machine and built-in tasks
│  ├─ network/                          Two custom network packets
│  └─ task/                             Task-point scanning and sync
├─ src/client/java/dev/annawathe/       Client: entrypoint, renderers, client Mixins
│  ├─ api/client/                       Client extension API (25 files)
│  ├─ client/gui/                       Per-screen renderers
│  ├─ client/mixin/                     Client Mixins (28)
│  ├─ client/psychosis/                 Hallucination visual cache
│  └─ client/task/                      Task-point overlay client state
├─ src/main/resources/                  fabric.mod.json, 3 Mixin configs, en_us/zh_cn language files
├─ README.md                            Chinese development notes
├─ README_MOOD_TASK_API.md              Mood & task API
├─ README_SHOP_CURRENCY_API.md          Shop & economy API
├─ README_TIME_INVENTORY_API.md         Time HUD & inventory button API
├─ README_HUD_CROSSHAIR_API.md          Generic screen HUD & crosshair API
└─ AGENTS.md                            Long-term collaboration and development rules
```

### 15.4 Source Statistics

| Item | Count |
| --- | --- |
| Java files | 158 (main 92 / client 66) |
| Lines of code | 6,574 (main 3,815 / client 2,759) |
| Public API files | 70 (common 45 / client 25) |
| Registration-based extension points | ~105 |
| CCA components | 8 |
| Mixins | 48 (common 18 / client 28 / Harpy compat 2) |
| Built-in mood tasks | 9 |
| Default shop entries | 12 |

---

*Compiled from the `annawathe` source tree; every value comes from source constants, `fabric.mod.json` and the shipped en_us/zh_cn language files.*
*Chinese edition: [`01-模组介绍-中文.md`](01-模组介绍-中文.md); API signatures and code examples: [`02-API-Reference-English.md`](02-API-Reference-English.md).*
