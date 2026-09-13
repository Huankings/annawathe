# AnnaWathe API Reference (English)

> Public interface manual for add-on developers
> For AnnaWathe `1.0.0-1.21.1` + vanilla Wathe `1.3.2-1.21.1` – `1.4.1-1.21.1`

**This document covers all 24 public API groups under the `dev.annawathe.api` package** — 70 files in total (45 common / 25 client). Every signature is taken verbatim from the source; nothing is simplified or inferred.

---

## Table of Contents

- [0. Prerequisites](#0-prerequisites)
- [1. Common Rules](#1-common-rules)
- [2. Visual APIs](#2-visual-apis)
  - [2.1 InstinctApi](#21-instinctapi)
  - [2.2 CrosshairHudApi](#22-crosshairhudapi)
  - [2.3 RoleNameHudApi](#23-rolenamehudapi)
  - [2.4 HudOverlayApi](#24-hudoverlayapi)
  - [2.5 MoodHudApi](#25-moodhudapi)
  - [2.6 TimeHudApi](#26-timehudapi)
  - [2.7 BodyInfoApi / BodyInfoHudApi](#27-bodyinfoapi--bodyinfohudapi)
  - [2.8 PlayerAppearanceApi / BodyAppearanceApi](#28-playerappearanceapi--bodyappearanceapi)
  - [2.9 HeldItemInvisibilityApi / PsychosisItemApi](#29-helditeminvisibilityapi--psychosisitemapi)
  - [2.10 TargetVisibilityApi](#210-targetvisibilityapi)
- [3. Gameplay APIs](#3-gameplay-apis)
  - [3.1 VictoryApi / CustomVictory](#31-victoryapi--customvictory)
  - [3.2 MoodTaskApi](#32-moodtaskapi)
  - [3.3 MoodTaskPointApi](#33-moodtaskpointapi)
  - [3.4 TaskCompletionApi](#34-taskcompletionapi)
  - [3.5 MoodApi](#35-moodapi)
  - [3.6 ShopApi / ShopEntry / ShopPrice](#36-shopapi--shopentry--shopprice)
  - [3.7 EconomyApi / PlayerEconomyApi](#37-economyapi--playereconomyapi)
  - [3.8 PsychoModeApi / PsychoModeProfile](#38-psychomodeapi--psychomodeprofile)
  - [3.9 PlayerLifeStateApi](#39-playerlifestateapi)
  - [3.10 PlayerMovementApi](#310-playermovementapi)
  - [3.11 PlayerCollisionApi](#311-playercollisionapi)
  - [3.12 PlayerTransformApi](#312-playertransformapi)
- [4. Client Interaction APIs](#4-client-interaction-apis)
  - [4.1 ItemTooltipApi](#41-itemtooltipapi)
  - [4.2 InventoryButtonApi](#42-inventorybuttonapi)
  - [4.3 PsychoModeClientApi](#43-psychomodeclientapi)
  - [4.4 Supplementary & Helper Types](#44-supplementary--helper-types)
- [5. Internal Facilities Reference](#5-internal-facilities-reference)
- [6. Migration Checklist](#6-migration-checklist)

---

## 0. Prerequisites

### 0.1 Dependency Setup

```gradle
repositories {
    maven { url = 'https://maven.ladysnake.org/releases' }   // Cardinal Components
}

dependencies {
    modApi files("libs/annawathe-1.0.0-1.21.1.jar")
    // AnnaWathe API signatures reference Wathe types. Any version works at compile time
    // (1.3.2 or 1.4.1) as long as it matches the Wathe version you target; AnnaWathe itself
    // is compatible with both 1.3.2 and 1.4.1.
    modApi files("libs/wathe-1.3.2-1.21.1.jar")
}
```

Declare the dependency in `fabric.mod.json`:

```json
{
  "depends": {
    "annawathe": ">=1.0.0-1.21.1",
    "wathe": ">=1.3.2-1.21.1 <=1.4.1-1.21.1"
  }
}
```

> **Writing `wathe` as a range** — `>=1.3.2-1.21.1 <=1.4.1-1.21.1` — is recommended: it gives both a lower and an upper bound, so the mod runs on either 1.3.2 or 1.4.1 while a future Wathe release is not silently accepted. Writing only `>=1.3.2-1.21.1` is semantically equivalent to allowing 1.4.1 and anything newer.

### 0.2 Registration Timing (Important)

| What you register | Required timing |
| --- | --- |
| **Role shops, default-shop modifiers** | **Common init** (`ModInitializer#onInitialize`) — client display and server-side purchase resolution must use the same list |
| Task definitions, task-point types, scan handlers | Common init |
| Currencies, passive income rules | Common init |
| Psycho profiles, shield rules | Common init (after `PsychoModeApi.init()`) |
| All client APIs (HUD, crosshair, tooltips, skins) | Client init (`ClientModInitializer#onInitializeClient`) |

### 0.3 Client / Server Source Split

AnnaWathe uses Fabric's `splitEnvironmentSourceSets`:

- Server and common code: `dev.annawathe.api.*` (everything except `api.client.*`)
- **Client only**: `dev.annawathe.api.client.*` — these carry `@Environment(EnvType.CLIENT)`; referencing them on a dedicated server crashes

---

## 1. Common Rules

### 1.1 Priority Semantics

**Every rule chain in AnnaWathe follows the same semantics**, so you can combine them freely:

| Rule | Description |
| --- | --- |
| Higher `priority` runs first | The opposite of Minecraft's event convention — watch out |
| At equal priority, the **later registration runs first** | Makes it easy to override built-in rules |
| Registering the **same Identifier** again | **Replaces** the old rule (not appended) — handy for config reloads |
| `DEFAULT_PRIORITY` | Provided by every API, always `0` |

> **Two kinds of chains:** short-circuiting chains (`InstinctApi`, `VictoryApi`, `CrosshairHudApi` providers) stop at the first non-`PASS` result. Non-short-circuiting chains (`HudOverlayApi`, `RoleNameHudApi.renderExtraHud`) run every registration.

### 1.2 Client / Server Boundary

The boundary is written into the API Javadoc, and add-ons must respect it:

> **Client visuals never replace server validation.** Crosshairs, names, HUDs, skins, held-item hiding and hallucination items are display-only. Real attacks, interactions, purchases and ability legality must be re-validated on the server.

Corresponding server-side re-check entry points:

| Client display | Server must re-check |
| --- | --- |
| `CrosshairHudApi` showing "can shoot" | Ammo, cooldown, distance, target legality |
| `TargetVisibilityApi` client target selection | Server-side `canInteractWith*` / `canAttack*` |
| `PlayerAppearanceApi` disguise skin | Server role, faction, sounds, identity unchanged |
| `BodyAppearanceApi` corpse appearance | The real owner UUID must be preserved for coroner checks, body bags and replays |
| `PsychosisItemApi` hallucination items | Server-side real items unchanged |

### 1.3 Do Not Touch Internal Implementation

| Package / class | Why |
| --- | --- |
| `dev.annawathe.bridge.*` | Internal bridge interfaces; source comments state add-ons must not use them |
| `dev.annawathe.mixin.*` | Mixin injection points — they change between versions |
| `dev.annawathe.network.*` | Internal network payloads |
| `dev.annawathe.task.*`, `dev.annawathe.mood.*` | Internal state machine and scanner |

### 1.4 Do Not Re-Mixin Intercepted Entry Points

The following are **fully taken over** by AnnaWathe; Mixin-ing them again causes conflicts or silently disabled rules:

| Vanilla target | Use instead |
| --- | --- |
| `WatheClient` (instinct) | `InstinctApi` |
| `CrosshairRenderer` | `CrosshairHudApi` |
| `RoleNameRenderer` | `RoleNameHudApi` |
| `InGameHud` (normal HUD / blackout / scope) | `HudOverlayApi` |
| `MoodRenderer` | `MoodHudApi` + `AnnaMoodRenderer` |
| `TimeRenderer` | `TimeHudApi` |
| `StoreRenderer` | Already handled by `AnnaStoreRenderer` |
| `RoundTextRenderer` | `AnnaRoundTextRenderer`'s public layout methods |
| `MurderGameMode` (victory) | `VictoryApi` |
| `WatheItemTooltips` (tooltip callback) | `ItemTooltipApi` |
| `WatheClient` (heartbeat / alive checks) | `PlayerLifeStateApi` |
| `Entity#collidesWith` / `pushAwayFrom` / `LivingEntity#pushAway` | `PlayerCollisionApi` |

---

## 2. Visual APIs

### 2.1 `InstinctApi`

**Package:** `dev.annawathe.api.instinct.InstinctApi`
**Environment:** client-side use (calling it on a server is pointless but harmless)

#### Methods

| Method | Description |
| --- | --- |
| `registerAvailability(Identifier id, int priority, AvailabilityHandler h)` | Register an "is instinct available" rule |
| `registerHighlight(Identifier id, int priority, HighlightHandler h)` | Register a "what colour is the target" rule |
| `resolveAvailability(PlayerEntity viewer)` | Resolve availability (you normally do not call this) |
| `resolveHighlight(PlayerEntity viewer, Entity target)` | Resolve highlight |

#### Result Types

```java
public enum AvailabilityResult { PASS, ENABLE, DISABLE }
// PASS    = continue to lower-priority rules
// ENABLE  = enable instinct and end the chain
// DISABLE = disable instinct and end the chain

public record HighlightResult(Action action, int color) {
    public static HighlightResult pass();          // continue
    public static HighlightResult color(int c);    // use this colour, end the chain
    public static HighlightResult hide();          // hide this target, end the chain
    public enum Action { PASS, COLOR, HIDE }
}
```

#### Example

```java
InstinctApi.registerAvailability(MyMod.id("role_instinct"), 20, viewer ->
        isMyRole(viewer) && AnnaWatheClient.inputActive()
                ? InstinctApi.AvailabilityResult.ENABLE
                : InstinctApi.AvailabilityResult.PASS);

InstinctApi.registerHighlight(MyMod.id("marked_target"), 30, (viewer, target) ->
        isMarked(viewer, target)
                ? InstinctApi.HighlightResult.color(0xFFAA00)
                : InstinctApi.HighlightResult.pass());
```

#### Notes

- **You must use `AnnaWatheClient.inputActive()`** to test whether the instinct key is active. Do not read `WatheClient.instinctKeybind` directly — AnnaWathe supports a "press to toggle" mode, where reading the keybind raw gives wrong results.
- Instinct is a client visual only; it can never replace server-side attack/interact/purchase validation.

---

### 2.2 `CrosshairHudApi`

**Package:** `dev.annawathe.api.client.gui.CrosshairHudApi`
**Environment:** client only. Only dispatched while drawing the first-person crosshair; third person never calls it.

#### Methods

| Method | Description |
| --- | --- |
| `registerProvider(Identifier id, int priority, Provider provider)` | Register a crosshair provider (**short-circuiting chain**) |
| `registerOverlay(Identifier id, int priority, Overlay overlay)` | Register a post-crosshair overlay (**never short-circuits**) |
| `renderStandardCrosshair(...)` | Draw Wathe's standard 3x3 normal/target crosshair |
| `renderKnifeProgressCrosshair(...)` | Knife ready/progress icon |
| `renderBatProgressCrosshair(...)` | Bat ready/progress icon |
| `renderIconProgressCrosshair(...)` | Custom 10×7 ready/background/fill textures |
| `renderCentered(...)` | Run custom drawing in screen-centre local coordinates |
| `drawCrosshairIcon` / `drawKnifeProgressIcon` / `drawBatProgressIcon` / `drawIconProgress` | Finer-grained composition when already centred |

#### Result Semantics

```java
public enum Result { PASS, HANDLED }
// PASS    = ask lower-priority providers; if all PASS, vanilla's default crosshair is drawn
// HANDLED = stop the provider chain and skip the default crosshair
//           returning HANDLED without drawing anything = intentionally hiding the default crosshair
```

- **Providers short-circuit**: higher `priority` first; later registration wins at equal priority.
- **Overlays never short-circuit**: all overlays run; higher `priority` draws later (on top).

#### Example

```java
// Replace the default crosshair
CrosshairHudApi.registerProvider(MyMod.id("crosshair/my_weapon"), 100, context -> {
    if (!context.mainHandStack().isOf(MY_WEAPON)) {
        return CrosshairHudApi.Result.PASS;
    }
    boolean target = findValidClientTarget(context.player()) != null;
    float progress = getClientProgress(context.player(), context.tickDelta());
    CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
    return CrosshairHudApi.Result.HANDLED;
});

// Keep the default crosshair, append a progress bar below it
CrosshairHudApi.registerOverlay(MyMod.id("crosshair/watch_progress"), 0, context -> {
    Float progress = getWatchProgress(context.player());
    if (progress == null) return;

    int width = 20;
    int x = context.centerX() - width / 2;
    int y = context.centerY() + 12;
    context.drawContext().fill(x, y, x + width, y + 2, 0x88000000);
    context.drawContext().fill(x, y, x + Math.round(width * progress), y + 2, 0xFFFFFFFF);
});
```

#### Notes

- Progress values are clamped to `0..1`; standard fill width is clamped to `0..10`.
- The built-in helpers **restore the matrix and blend state**; if you touch the matrix or `RenderSystem` yourself, you must pair `push`/`pop` and restore state.
- When querying player/corpse targets, also use `TargetVisibilityApi.canTargetPlayer(...)` / `canTargetBody(...)` — otherwise hidden targets still leak a "lockable" hint.

---

### 2.3 `RoleNameHudApi`

**Package:** `dev.annawathe.api.client.gui.RoleNameHudApi`
**Environment:** client only. This is the unified entry point for text below the crosshair and for "who am I looking at".

#### Methods (Nine Rule Types)

| Method | Rule type | Description |
| --- | --- | --- |
| `registerHudVisibility(Identifier, int, HudVisibilityHandler)` | Whether the whole name HUD renders | Returns `PASS` / `SHOW` / `HIDE` |
| `registerRaycastSource(Identifier, int, RaycastSourceHandler)` | Raycast source entity | Non-null wins (e.g. cast from a camera/drone) |
| `registerPlayerTargetFilter(Identifier, int, PlayerTargetFilter)` | Player target filter | Returns `PASS` / `ALLOW` / `DENY` |
| `registerName(Identifier, int, NameHandler)` | Player name text | Non-null replaces it |
| `registerEntityName(Identifier, int, EntityNameHandler)` | Non-player entity name text | Non-null replaces it |
| `registerCohortState(Identifier, int, CohortStateHandler)` | Whether a player counts as a cohort | Returns `Boolean`; null continues |
| `registerCohortTargetState(Identifier, int, CohortTargetStateHandler)` | Whether a cohort shows as a target | Returns `Boolean`; null continues |
| `registerCohortHint(Identifier, int, CohortHintHandler)` | Whether to show a cohort hint | Returns `PASS` / `SHOW` / `HIDE` |
| `registerExtraHud(Identifier, int, ExtraHudRenderer)` | Extra HUD drawing | **Never short-circuits**; all run |

#### Helpers

| Method | Description |
| --- | --- |
| `findLookedAtBody(ClientPlayerEntity p, float range)` | Find the corpse under the crosshair (already filtered by `TargetVisibilityApi`) |
| `defaultLookRange(PlayerEntity p)` | Default look range: 8 blocks for spectators/creative, otherwise 2 |

#### Context

```java
public record Context(
    TextRenderer renderer, ClientPlayerEntity player, DrawContext drawContext,
    RenderTickCounter tickCounter, float range,
    @Nullable PlayerEntity targetPlayer, @Nullable Entity targetEntity,
    @Nullable Text displayedTargetName, float nametagAlpha, float noteAlpha) {}
```

#### Example

```java
RoleNameHudApi.registerName(MyMod.id("disguise_name"), 100, (viewer, target, original) -> {
    String alias = myDisguiseOf(target);
    return alias == null ? null : Text.literal(alias);
});
```

#### Notes

- Every callback is **client display only** and changes no server-side identity.
- AnnaWathe itself uses this chain to implement: showing the impersonated player's real name for debug transformations (priority 50), psycho name obfuscation, and the corpse-info HUD chain.

---

### 2.4 `HudOverlayApi`

**Package:** `dev.annawathe.api.client.hud.HudOverlayApi`
**Companions:** `HudOverlayContext`, `HudOverlayLayer`, `HudOverlayLayout`

#### Drawing Phases

| Phase | Timing | Suited to |
| --- | --- | --- |
| `HudOverlayLayer.BEFORE_HUD` | **Before** Minecraft's main HUD | Control/kidnap prompts that must cover the screen early |
| `HudOverlayLayer.MAIN_HUD` | **After** Wathe's main HUD | Normal role status text |
| `HudOverlayLayer.AFTER_HUD` | **After** the entire HUD | Topmost overlays such as sniper scopes |

Within one phase, **every renderer runs**. Higher `priority` draws later (on top); later registration draws later at equal priority.

#### Methods

| Method | Description |
| --- | --- |
| `register(Identifier id, HudOverlayLayer layer, int priority, HudOverlayRenderer renderer)` | Register a renderer |
| `registerAliveRole(Identifier id, HudOverlayLayer layer, int priority, Role role, HudOverlayRenderer renderer)` | Register a HUD shown only when the local player has that role and is gameplay-alive |

`registerAliveRole` builds its context through `GameFunctions.isPlayerAliveAndSurvival(...)`, so it **automatically respects `PlayerLifeStateApi`'s creative/spectator grants** — add-ons do not need to distinguish plain survival from a granted state.

#### Context Capabilities (`HudOverlayContext`)

Fields: `client`, `player`, `textRenderer`, `drawContext`, `tickCounter`, `gameWorld`, `aliveAndSurvival`, `spectatingOrCreative`, `debugHudVisible`, `hudHidden`, `currentScreen`
Methods: `width()`, `height()`, `tickDelta()`, `isRunning()`, `isRole(Role)`, `isAliveRole(Role)`, `renderHotbar()`

#### Layout Helpers (`HudOverlayLayout`)

Provides coordinate conversion for bottom-right single/multi-line text and centred text near the crosshair — for example `drawBottomRightLine(context, Text, int color)`. **Never hardcode coordinates in an add-on.**

#### Example

```java
// Role status (bottom right)
HudOverlayApi.registerAliveRole(
        MyMod.id("hud/my_role/status"),
        HudOverlayLayer.MAIN_HUD,
        HudOverlayApi.DEFAULT_PRIORITY,
        MY_ROLE,
        context -> HudOverlayLayout.drawBottomRightLine(
                context,
                Text.translatable("hud.mymod.my_role.ready"),
                MY_ROLE.color()
        )
);

// Not role-specific (being controlled)
HudOverlayApi.register(
        MyMod.id("hud/controlled"),
        HudOverlayLayer.BEFORE_HUD,
        HudOverlayApi.DEFAULT_PRIORITY,
        context -> {
            if (!context.aliveAndSurvival() || !isControlled(context.player())) return;
            context.drawContext().fill(0, 0, context.width(), context.height(), 0xCC000000);
        }
);

// AFTER_HUD overlay while keeping the hotbar visible
HudOverlayApi.register(MyMod.id("hud/scope"), HudOverlayLayer.AFTER_HUD, 1000, context -> {
    if (!context.aliveAndSurvival() || !isScoped()) return;
    drawScope(context.drawContext(), context.width(), context.height());
    context.renderHotbar();
});
```

#### Notes

- An `AFTER_HUD` overlay covers the hotbar drawn earlier. To keep it visible, call `context.renderHotbar()` — it still goes through vanilla Wathe's hotbar wrapper and preserves Wathe textures. Do **not** add a Mixin to `InGameHud#renderHotbar` just to redraw the hotbar.
- States such as being controlled or kidnapped do not necessarily belong to the local player's role, so use plain `register` and check `context.aliveAndSurvival()` explicitly.

---

### 2.5 `MoodHudApi`

**Package:** `dev.annawathe.api.client.mood.MoodHudApi`
**Companions:** `MoodHudStyle`, `PsychoMoodHudStyle`, `MoodHudContext`, `MoodHudColors`

#### Methods

| Method | Description |
| --- | --- |
| `registerRoleStyle(Role role, MoodHudStyle style)` | Register a normal mood HUD style for a role |
| `registerMoodProvider(Identifier id, int priority, MoodStyleProvider provider)` | Register a priority style provider |
| `registerPsychoStyle(Identifier id, int priority, PsychoStyleProvider provider)` | Register a psycho HUD style |
| `registerVisibleGameMode(GameMode mode)` | Declare an extra game mode in which the mood HUD shows |
| `registerVisibleGameModePredicate(Identifier id, int priority, GameModePredicate predicate)` | Conditional visibility |
| `resolve(...)` / `resolvePsycho(...)` | Resolve the final style (you normally do not call these) |

Resolution order: **provider chain (priority descending, later registration first) → role style table → built-in default style**.

#### `MoodHudStyle` Options

```java
MoodHudApi.registerRoleStyle(MY_ROLE, MoodHudStyle.builder(MyMod.id("hud/mood_my_role"))
        .arrows(MyMod.id("hud/arrow_up"), MyMod.id("hud/arrow_down"))  // up/down arrows
        .overlays(ctx -> List.of(MyMod.id("hud/overlay_1")))            // extra overlays
        .icon(ctx -> { /* custom icon drawing */ })
        .barColor(MY_ROLE.color())                                      // fixed-colour bar
        // .hsvMoodBar()                                                // or: HSV bar that follows mood
        // .bar((ctx, width, alpha) -> { ... })                          // or: fully custom bar
        .barVisibleWhen(ctx -> ctx.moodAlpha() > 0F)
        .hideWarning()                                                  // hide the breakdown warning
        .build());
```

Defaults: `arrows = false`, `warning = true`, `overlays = empty list`, `bar = null` (so `shouldRenderBar` requires a `bar` first).

#### Colour Helper

```java
// barColor accepts both 0xRRGGBB and Color#getRGB()'s 0xAARRGGBB.
// Final alpha is always decided by the HUD fade state.
context.drawContext().fill(0, 0, w, 1, MoodHudColors.withAlpha(myColor, alpha));
```

> You **must** use `MoodHudColors.withAlpha` to rewrite alpha, **not** bitwise OR — `Color#getRGB()` returns a value with `0xFF` alpha, so OR-ing makes a custom bar permanently opaque.

#### Psycho Style

`PsychoMoodHudStyle` can replace: full body, damaged body, eyes, scrolling text, text colour and countdown bar colour.

#### Notes

- `registerRoleStyle` only affects display; a role whose `moodType` is `NONE` shows nothing and has its tasks cleared.
- `MoodHudApi.shouldRender(GameMode)` is public but is **never called inside AnnaWathe** — it will not affect the default mood HUD gate.

---

### 2.6 `TimeHudApi`

**Package:** `dev.annawathe.api.time.TimeHudApi`
**Environment:** common (calling it server-side is harmless but only affects client display)

#### Methods

| Method | Description |
| --- | --- |
| `registerProvider(Identifier id, int priority, TimeDisplayProvider provider)` | Register a time display provider (**short-circuiting chain**) |
| `registerDefaultProvider(Identifier id, int priority, TimeDisplayProvider provider)` | Register a fallback provider (treated as the earliest registration when sorting) |
| `resolveDisplay(PlayerEntity viewer)` | Resolve the final display (you normally do not call this) |

#### Result Factories

```java
TimeDisplay.pass();                                  // continue to lower priority
TimeDisplay.hide();                                  // hide the timer, end the chain
TimeDisplay.show(int ticks);                          // dynamic colour
TimeDisplay.showCountdown(int ticks, int warning);     // dynamic colour + low-time warning
TimeDisplay.showDynamic(int ticks, int warning, int changeFlashThreshold);
TimeDisplay.showFixedColor(int ticks, int color);      // fixed colour
```

Constants: `NO_LOW_TIME_WARNING = -1`, `DEFAULT_CHANGE_FLASH_THRESHOLD = 10`.

#### Example

```java
TimeHudApi.registerProvider(MyMod.id("special_countdown"), 100, viewer -> {
    if (!shouldShow(viewer)) return TimeHudApi.TimeDisplay.pass();
    return TimeHudApi.TimeDisplay.showFixedColor(getTicks(viewer), 0xE04B4B);
});
```

#### Notes

- Returning `SHOW` or `HIDE` **ends resolution**; `PASS` continues.
- An add-on using priority `0` still **runs before Anna's default round timer** (the default is registered via `registerDefaultProvider`, which sorts as the earliest).
- **The renderer resets its scrolling digits when the time source ID changes**, preventing leftover animation between different countdowns.
- This API is client display only. A real countdown must be maintained and synced by your own server component.

---

### 2.7 `BodyInfoApi` / `BodyInfoHudApi`

#### `BodyInfoApi` (common)

**Package:** `dev.annawathe.api.body.BodyInfoApi`

| Method | Description |
| --- | --- |
| `registerRoleResolver(Identifier id, int priority, RoleResolver resolver)` | Register a "victim's role id" resolver |
| `initializeBody(PlayerBodyEntity body, PlayerEntity victim, Identifier deathReason)` | Initialise corpse info (called automatically by AnnaWathe when a corpse spawns) |
| `get(PlayerBodyEntity body)` → `BodyInfoSnapshot` | Read the snapshot |
| `setDeathReason` / `setRoleId` / `setDeathWorldTime` | Modify fields |
| `sync(PlayerBodyEntity body)` | Force a sync |

`BodyInfoSnapshot` fields: `deathReason` (default `wathe:generic`), `roleId` (nullable), `deathWorldTime`.

#### `BodyInfoHudApi` (client)

**Package:** `dev.annawathe.api.client.gui.BodyInfoHudApi`

| Method | Description |
| --- | --- |
| `registerRule(Identifier id, int priority, VisibilityRule rule)` | Register a visibility rule |
| `registerRoleNameProvider(Identifier id, int priority, RoleNameProvider provider)` | Register the identity text |

```java
public record Visibility(Decision decision, boolean deathSummary, boolean roleIdentity) {
    public static Visibility pass();
    public static Visibility hide();
    public static Visibility show(boolean deathSummary, boolean roleIdentity);  // two independent switches
}
public enum Decision { PASS, SHOW, HIDE }
```

**Default visibility:** with no matching rule, `spectator/creative → show both lines`, otherwise hidden.

#### Translation Keys and Fallbacks

| Content | Key |
| --- | --- |
| Whole death-summary line | `hud.annawathe.body.death_info` |
| Identity label | `hud.annawathe.body.role_info` |
| Death reason | `death_reason.<namespace>.<path>` |

Role name fallback chain: registered `RoleNameProvider` → Harpy role name (when HarpyModLoader is present) → vanilla's four base roles use `announcement.role.<path>`, add-on roles use `announcement.role.<namespace>.<path>` with white colour.

#### Notes

- `deathSummary` is the whole "time of death + cause" line and `roleIdentity` is the victim's identity; an add-on may show only one of them.
- Rendering is constrained by `TargetVisibilityApi.canRenderBody` and `RoleNameHudApi.findLookedAtBody`.

---

### 2.8 `PlayerAppearanceApi` / `BodyAppearanceApi`

#### `PlayerAppearanceApi` (client)

**Package:** `dev.annawathe.api.client.appearance.PlayerAppearanceApi`

| Method | Description |
| --- | --- |
| `registerPlayerSkin(Identifier id, int priority, PlayerSkinHandler handler)` | Player skin override (short-circuiting chain) |
| `registerBodySkin(Identifier id, int priority, BodySkinHandler handler)` | Corpse skin override (short-circuiting chain) |
| `resolvePlayerSkin(AbstractClientPlayerEntity player)` | Resolve the final player skin |
| `resolveBodySkin(PlayerBodyEntity body)` | Resolve the final corpse skin |
| `resolveOriginalSkinTextures(UUID uuid, boolean fallback)` | Original skin by UUID |
| `resolveOriginalPlayerName(UUID uuid)` | Original player name by UUID |

#### `BodyAppearanceApi` (common)

**Package:** `dev.annawathe.api.appearance.BodyAppearanceApi`

| Method | Description |
| --- | --- |
| `register(Identifier id, int priority, Handler handler)` | Register an "whose appearance should this corpse use" resolver |
| `resolveAppearanceUuid(PlayerEntity victim, PlayerEntity killer, Identifier reason)` | Resolve the corpse's visual UUID |

#### Notes

- AnnaWathe itself uses `PlayerAppearanceApi` for the psycho skin (priority `10000`) and debug transformations. **Your rule needs a priority above 10000 to override the psycho skin.**
- `BodyAppearanceApi` returns a **corpse visual UUID**. The real `PlayerBodyEntity` owner UUID **must be preserved** for coroner checks, body bags, replays and death resolution — never replace it with the appearance UUID.
- The returned skin only affects client models, capes and the corpse renderer; it changes no server-side decision.

---

### 2.9 `HeldItemInvisibilityApi` / `PsychosisItemApi`

#### `HeldItemInvisibilityApi` (client)

**Package:** `dev.annawathe.api.client.invisibility.HeldItemInvisibilityApi`

| Method | Description |
| --- | --- |
| `registerHiddenItem(Role role, Item item)` | Simple form: hide one item for one role |
| `registerHiddenItems(Role role, Collection<Item> items)` | Batch form |
| `registerRule(Identifier id, int priority, VisibilityRule rule)` | Full rule (short-circuiting; `true` hides) |
| `shouldHideFromOtherLivingPlayers(viewer, holder, hand, stack)` | Query |
| `applyInvisibility(viewer, holder, hand[, stack])` | Renderer entry point: returns `ItemStack.EMPTY` when hidden |
| `isHiddenByAnyRule(holder, hand[, stack])` | Whether any rule matches |
| `hasHiddenHeldItem(PlayerEntity holder)` | Whether main or off hand has a hidden item |

Context: `VisibilityContext(gameWorld, holder, hand, stack, role)`

#### `PsychosisItemApi` (client)

**Package:** `dev.annawathe.api.client.mood.PsychosisItemApi`
Provider for low-mood hallucinated held items and arm poses. Resolution order: **priority > 0 → default → priority ≤ 0**. Results offer `pass()`, `item(...)`, `itemWithPose(...)` and `emptyWithPose(...)`.

#### Notes

- Held-item hiding only affects the model seen by **other gameplay-alive players**. Your own F5 view, death/normal spectator views and the real server-side item are unaffected.
- Hallucination items and arm poses live **only in the observer's client cache**; death, round stop, reset and disconnect must clear them (AnnaWathe handles this — do not treat them as persistent state).

---

### 2.10 `TargetVisibilityApi`

**Package:** `dev.annawathe.api.visibility.TargetVisibilityApi`
**Environment:** common (client for rendering and target selection, server for interaction and attacks)

#### Methods

| Method | Description |
| --- | --- |
| `registerPlayerRule(Identifier id, int priority, PlayerRule rule)` | Player rule |
| `registerBodyRule(Identifier id, int priority, BodyRule rule)` | Corpse rule |
| `canRenderPlayer(viewer, target)` / `canRenderBody(viewer, body)` | Whether it renders |
| `canTargetPlayer(...)` / `canTargetBody(...)` | Whether the crosshair can select it |
| `canInteractWithPlayer(...)` / `canInteractWithBody(...)` | Whether it can be interacted with |
| `canAttackPlayer(...)` / `canAttackBody(...)` | Whether it can be attacked |
| `canRenderEntity` / `canTargetEntity` / `canInteractWithEntity` / `canAttackEntity` | Generic entry points (non-player/corpse entities always `true`) |

#### Result Semantics

```java
public enum Decision { PASS, ALLOW, DENY }
// Queried in priority order: first ALLOW → true, first DENY → false, all PASS → true (allow)
```

#### What Each Action Affects

| Action | Effect |
| --- | --- |
| `RENDER` | Rendering and entity presence (`isInvisibleTo`, player/corpse renderer cancellation) |
| `TARGET` | Client crosshair selection (`canHit`). **Client hint only** |
| `INTERACT` | Interaction (`Entity#interact` / `interactAt`) |
| `ATTACK` | Melee (`PlayerEntity#attack` wrapper) |

#### Notes

- **AnnaWathe registers no rules internally** — everything passes by default. This is a skeleton for add-ons.
- `TARGET` filtering **only affects client target selection and the crosshair**; `INTERACT` / `ATTACK` must be re-validated at the server-side ability entry points.
- With `viewer == null` it returns `true` immediately.

---

## 3. Gameplay APIs

### 3.1 `VictoryApi` / `CustomVictory`

**Package:** `dev.annawathe.api.win`

#### Methods

| Method | Description |
| --- | --- |
| `registerRule(Identifier id, int priority, VictoryRule rule)` | Register a victory rule (**short-circuiting chain**) |
| `evaluate(ServerWorld, GameWorldComponent, GameFunctions.WinStatus)` | Resolve (you normally do not call this) |
| `endGameWithCustomVictory(ServerWorld, CustomVictory)` | End the round immediately with a custom victory |
| `endGameWithVanillaWin(ServerWorld, GameFunctions.WinStatus, Collection<UUID> extra)` | Vanilla faction result plus extra winners |

#### Result Semantics

```java
public enum Action { PASS, KEEP_RUNNING, VANILLA_WIN, CUSTOM_WIN }

VictoryResult.pass();
//   Keep the vanilla settlement flow

VictoryResult.keepRunning();
//   Cancel this settlement and keep the round running.
//   Only use under clearly justified keep-alive conditions

VictoryResult.vanillaWin(GameFunctions.WinStatus status, Collection<UUID> extraWinnerUuids);
//   Settle by vanilla faction and append extra winners

VictoryResult.customWin(CustomVictory victory);
//   Custom (standalone) victory
```

#### Rule Context

```java
public record Context(
    ServerWorld world,
    GameWorldComponent gameWorld,
    List<ServerPlayerEntity> alivePlayers,      // already filtered by GameFunctions.isPlayerAliveAndSurvival
    GameFunctions.WinStatus vanillaWinStatus) {}
```

#### `CustomVictory` Data

```java
public record CustomVictory(
    Identifier id, String announcementTranslationKey, String detailTranslationKey,
    String fallbackTitle, int color, List<UUID> winnerUuids, CustomVictoryGroup winnerGroup)

CustomVictory.of(Identifier id, int color, Collection<? extends PlayerEntity> players);
CustomVictory.builder(Identifier id, int color)
        .announcementTranslationKey(String)   // defaults to announcement.win.<ns>.<path>
        .detailTranslationKey(String)         // defaults to game.win.<ns>.<path>
        .titleTranslationKey(String)          // defaults to announcement.role.<ns>.<path>
        .fallbackTitle(String)                // defaults to a prettified path (underscores split, capitalised)
        .winners(Collection<UUID>)
        .winnersFromPlayers(Collection<? extends PlayerEntity>)
        .build();
```

#### Example

```java
VictoryApi.registerRule(MyMod.id("lone_winner"), 100, context -> {
    if (context.alivePlayers().size() == 1 && isMyRole(context.alivePlayers().getFirst())) {
        return VictoryApi.VictoryResult.customWin(
                CustomVictory.builder(MyMod.id("lone_winner"), 0xE3A42D)
                        .fallbackTitle("Lone Winner")
                        .winnersFromPlayers(context.alivePlayers())
                        .build());
    }
    return VictoryApi.VictoryResult.pass();
});
```

#### Notes

- Winners are stored as **UUIDs**, never as live `Player` instances.
- Add-ons must supply their own translations: `announcement.win.<ns>.<path>`, `game.win.<ns>.<path>`, `announcement.role.<ns>.<path>`.
- **Always call `.fallbackTitle(...)` explicitly** — in the current version the Builder does not assign `fallbackTitle`, so a missing translation key renders `null Wins` (see the "Known Issues" chapter of the mod overview).
- AnnaWathe bundles no concrete role victory rules.

---

### 3.2 `MoodTaskApi`

**Package:** `dev.annawathe.api.task.MoodTaskApi`

#### Built-in Task ID Constants

```java
MoodTaskApi.SLEEP   // wathe:sleep
MoodTaskApi.OUTSIDE // wathe:outside
MoodTaskApi.EAT     // wathe:eat
MoodTaskApi.DRINK   // wathe:drink
MoodTaskApi.SHIFT   // annawathe:shift
MoodTaskApi.RUN     // annawathe:run
MoodTaskApi.SIT     // annawathe:sit
MoodTaskApi.STAY    // annawathe:stay
MoodTaskApi.AWAY    // annawathe:away
```

#### Registration

| Method | Description |
| --- | --- |
| `registerTask(MoodTaskDefinition definition)` | Register a task (same ID replaces) |
| `getDefinition(Identifier id)` / `getDefinitions()` | Query |
| `getRandomAssignableDefinitions()` | Current random pool |
| `getRegisteredTaskIds()` | All registered IDs |
| `getTranslationKey(Identifier id)` | Translation key (unknown falls back to `annawathe.task.unknown`) |
| `getTaskPointIds(Identifier id)` | Task points bound to a task |

#### Assignment / Removal / Completion

| Method | Description |
| --- | --- |
| `assignTask(ServerPlayerEntity player, Identifier id)` | Assign a specific task |
| `assignRandomTask(ServerPlayerEntity player)` | Assign one random task |
| `assignRandomTasks(ServerPlayerEntity player, int count)` | Assign N random tasks |
| `fillRandomTaskSlots(ServerPlayerEntity player)` | Fill all free slots |
| `removeTask(ServerPlayerEntity player, Identifier id)` | **Silent removal** |
| `completeTask(ServerPlayerEntity player, Identifier id, boolean rewardMood)` | **Normal completion** |
| `hasTask(PlayerEntity player, Identifier id)` | Whether it is held |
| `getActiveTaskIds(PlayerEntity player)` | Current task list |

#### Rules

| Method | Description |
| --- | --- |
| `registerAssignmentRule(Identifier id, int priority, AssignmentRule rule)` | Pre-assignment hook (return `DENY` to refuse) |
| `registerCompletionRule(Identifier id, int priority, CompletionRule rule)` | Pre-completion hook (return `DENY` to refuse) |
| `canAssign(AssignmentContext)` / `canComplete(CompletionContext)` | Query |

> **Note: `Decision` here only has `PASS` and `DENY`** — there is no "force allow". Any rule returning `DENY` refuses the whole operation.

#### Status Enums

```java
AssignmentStatus: SUCCESS, PARTIAL_SUCCESS, INVALID_COUNT, GAME_NOT_RUNNING, PLAYER_NOT_ALIVE,
                  MOOD_NOT_SUPPORTED, TASK_LIMIT_REACHED, TASK_NOT_REGISTERED,
                  TASK_ALREADY_ACTIVE, ASSIGNMENT_DENIED, NO_AVAILABLE_TASK
OperationStatus:  SUCCESS, TASK_NOT_ACTIVE, COMPLETION_DENIED
AssignmentSource: INTERNAL_PRIMARY_COOLDOWN, INTERNAL_SLOT_REFILL, EXTERNAL_RANDOM, EXTERNAL_SPECIFIC
```

#### Validation Order

`count ≤ 0` → `INVALID_COUNT`; round not running → `GAME_NOT_RUNNING`; not gameplay-alive → `PLAYER_NOT_ALIVE`; role does not support mood → `MOOD_NOT_SUPPORTED`; full slots → `TASK_LIMIT_REACHED`; already held → `TASK_ALREADY_ACTIVE`; unregistered → `TASK_NOT_REGISTERED`; refused by a rule → `ASSIGNMENT_DENIED`; empty pool → `NO_AVAILABLE_TASK`; fewer assigned than requested → `PARTIAL_SUCCESS`.

#### Building a `MoodTaskDefinition`

```java
MoodTaskApi.registerTask(MoodTaskDefinition.builder(
        MyMod.id("my_task"),
        "task.mymod.my_task",
        player -> new MyTask(),                  // Factory: create instance
        (player, nbt) -> new MyTask(nbt)         // NbtReader: restore from save
)
        .randomlyAssignable()                    // enter every player's random pool (off by default)
        .randomWeight(2.0F)                      // random weight (default 1.0)
        .taskPoints(MoodTaskPointApi.SEAT)       // bind task points
        .build());
```

- Add-on tasks are **directed-assignment only by default**; you must call `.randomlyAssignable()` to enter the random pool.
- The task instance implements `MoodTaskInstance`; its NBT holds **only the instance's own progress**. The stable task ID is written by AnnaWathe.
- `.legacyTask(...)` exists only so AnnaWathe can adapt vanilla's four enum tasks — **new add-ons must not use it**.
- **Never add values to vanilla's `PlayerMoodComponent.Task` enum.**

#### `removeTask` vs `completeTask`

| | `removeTask` | `completeTask` |
| --- | --- | --- |
| Restores mood | ✗ | ✓ (`0.4` when `rewardMood = true`) |
| Plays the completion animation | ✗ | ✓ |
| Fires `AFTER_TASK_COMPLETE` | ✗ | ✓ |
| Settles task income | ✗ | ✓ |
| Counts toward stuck tracking | ✗ | ✓ (other tasks +1) |

---

### 3.3 `MoodTaskPointApi`

**Package:** `dev.annawathe.api.task.MoodTaskPointApi`

#### Built-in Task Point IDs and Colours

```java
MoodTaskPointApi.BED            // annawathe:bed           0x57D6FF
MoodTaskPointApi.FOOD_TRAY      // annawathe:food_tray     0x61D95C
MoodTaskPointApi.COCKTAIL_TRAY  // annawathe:cocktail_tray 0xFF85A8
MoodTaskPointApi.SEAT           // annawathe:seat          0x7AF4E1
MoodTaskPointApi.KEYED_DOOR     // annawathe:keyed_door    0xFFF79B
```

#### Methods

| Method | Description |
| --- | --- |
| `registerTaskPoint(TaskPointDefinition definition)` | Register a task-point type |
| `registerTaskPoint(Identifier id, String translationKey, int color)` | Simplified overload |
| `registerScanHandler(Identifier id, int priority, ScanHandler handler)` | Register a map-scan extension |
| `getDefinition` / `getDefinitions` / `getRegisteredIds` / `isRegistered` | Query |
| `getTranslationKey(Identifier id)` / `getColor(Identifier id)` | Translation key and colour (unknown returns white) |
| `scanExtraTaskPoints(TaskPointScanContext context)` | Called by the scanner |

#### Example

```java
MoodTaskPointApi.registerTaskPoint(MyMod.id("my_point"), "hud.mymod.my_point", 0x66CCFF);

MoodTaskPointApi.registerScanHandler(MyMod.id("my_point_scan"), 0, context -> {
    if (context.state().isOf(MyBlocks.MY_BLOCK)) context.addTaskPoint(MyMod.id("my_point"));
});
```

#### Notes

- A scan handler **only inspects `context.pos()` — the current block**. AnnaWathe already bounds scanning to "translated train reset-template area ∩ `playArea`"; **add-ons must not sweep the world again**.
- `addTaskPoint(...)` discards unregistered IDs.
- The client overlay only highlights task points belonging to **the player's current task**; spectator/creative shows every registered type.

---

### 3.4 `TaskCompletionApi`

**Package:** `dev.annawathe.api.task.TaskCompletionApi`

| Member | Description |
| --- | --- |
| `AFTER_TASK_COMPLETE` | Fabric event fired after a task completes (**never short-circuits**) |
| `registerTaskIncomeProvider(Identifier id, int priority, TaskIncomeProvider provider)` | Provide money income for non-killer tasks |
| `registerTaskIncomeRule(Identifier id, int priority, TaskIncomeRule rule)` | Suppress the default task income (return `SUPPRESS_DEFAULT_INCOME`) |
| `dispatch(TaskCompletionContext context)` | Called by AnnaWathe |

```java
public enum TaskIncomeDecision { PASS, SUPPRESS_DEFAULT_INCOME }
```

#### Notes

- `registerTaskIncomeRule` can only **suppress income**; it **does not block the completion event** — the task still counts as completed.
- The killer task-money path is retained, but the constants default to `TASK_MONEY_PER_KILLER_TASK = 0` and `TASK_MONEY_PER_KILL = 0`, so nothing is granted by default.

---

### 3.5 `MoodApi`

**Package:** `dev.annawathe.api.mood.MoodApi`

| Method | Description |
| --- | --- |
| `getMood(PlayerEntity)` / `setMood(PlayerEntity, float)` | Read/set mood (goes through vanilla's public entry point) |
| `setDrainMultiplier(PlayerEntity, float)` | **Multiplies** the base drain (≥ 0) |
| `getDrainMultiplier(PlayerEntity)` | Current multiplier |
| `protectFromDrain(PlayerEntity, int ticks)` | Pause the drain for N ticks (takes the `max`, so it never shortens an existing protection) |
| `getDrainProtectionTicks(PlayerEntity)` | Remaining protection |
| `clearExternalDrainState(PlayerEntity)` | Reset multiplier to `1.0` and protection to `0` |
| `isMoodDeathEnabled(PlayerEntity)` / `setMoodDeathEnabled(PlayerEntity, boolean)` | World-level mental-breakdown toggle |
| `MENTAL_BREAKDOWN` | Death reason constant `wathe:mental_breakdown` |

#### Base Values (fixed, not changeable through this API)

| Constant | Value |
| --- | --- |
| Base drain | `1/4000` per tick (empty in ~3m20s) |
| Completion reward | `0.4` |
| Task-slot thresholds | `0.51` / `0.17` |
| Breakdown warning threshold | `0.15` |
| Task cap | `3` |

---

### 3.6 `ShopApi` / `ShopEntry` / `ShopPrice`

#### `ShopApi`

**Package:** `dev.annawathe.api.shop.ShopApi`

| Method | Description |
| --- | --- |
| `registerRoleShop(Role role, RoleShopProvider provider)` | Register a role shop (dynamic provider) |
| `registerStaticRoleShop(Role role, Supplier<List<ShopEntry>> supplier)` | Static list |
| `registerStaticRoleShop(Supplier<List<ShopEntry>> supplier, Role... roles)` | Shared across roles |
| `registerStaticRoleShops(Collection<Role> roles, Supplier<List<ShopEntry>> supplier)` | Batch |
| `registerShopModifier(Identifier id, int priority, ShopModifier modifier)` | Modify the default shop list (add/remove/change) |
| `getEntriesForPlayer(PlayerEntity)` | Resolve a player's visible entries |
| `hasShop(PlayerEntity)` / `hasRoleShop(Role)` | Whether a shop exists |
| `resolveShop(PlayerEntity)` → `ResolvedShop` | Full resolution result |
| `defaultPurchase(ShopPurchaseContext)` | Default purchase implementation |
| `getDefaultShopPrice(Item)` / `getDefaultPrice(Item, int fallback)` | Query Anna's default prices |
| `getDefaultCurrencyPrice(Item, int option, Identifier currency, int fallback)` | Price of one currency in one option |
| `sendPurchaseFailedMessage(PlayerEntity)` / `playBuySound(PlayerEntity)` / `playFailSound(PlayerEntity)` | UI feedback |

#### `AnnaDefaultShop`

**Package:** `dev.annawathe.api.shop.AnnaDefaultShop`

| Method | Description |
| --- | --- |
| `entries()` → `List<ShopEntry>` | Returns AnnaWathe's default killer shop list (a freshly built immutable list per call) |

**These 12 entries are the entire default shop**, and their prices are the same definitions that `ShopApi.getDefaultShopPrice` and friends query. A source comment states explicitly that it **deliberately does not read** vanilla `GameConstants.SHOP_ENTRIES` prices, so Anna's price table can be tuned independently; item behaviour still calls vanilla's existing server-side capabilities.

| Slot | Item | Price | Notes |
| --- | --- | --- | --- |
| WEAPON | Knife | 100 | |
| WEAPON | Revolver | 250 | |
| WEAPON | Grenade | 300 | |
| WEAPON | Psycho Mode | 350 | `action` → `PlayerShopComponent::usePsychoMode` |
| POISON | Poison Vial | 70 | |
| POISON | Scorpion | 40 | |
| TOOL | Firecracker | 10 | |
| TOOL | Lockpick | 50 | |
| TOOL | Crowbar | 25 | |
| TOOL | Body Bag | 70 | |
| TOOL | Blackout | 250 | `action` → `PlayerShopComponent::useBlackout` |
| TOOL | Note ×4 | 10 | |

> **Add-ons should not modify this list.** To adjust the default shop, use `ShopApi.registerShopModifier(...)` — it applies during resolution and leaves the original definition intact for other add-ons.

#### `ShopEntry`

**Package:** `dev.annawathe.api.shop.ShopEntry`

```java
public enum Type { WEAPON, POISON, TOOL }     // textures: wathe:gui/shop_slot_*

// Constructors (default = killer hotbar restriction)
new ShopEntry(ItemStack stack, int price, Type type);
new ShopEntry(ItemStack stack, ShopPrice price, Type type);

// Four delivery modes
ShopEntry.directToHotbar(stack, price, type);     // skips the killer check, inserts into a free hotbar slot
ShopEntry.giveToInventory(stack, price, type);    // full inventory
ShopEntry.action(stack, price, type, player -> ...);
ShopEntry.action(stack, price, type, player -> ..., boolean showFailure);
```

| Delivery mode | Behaviour |
| --- | --- |
| **Default** (`action == null`) | Requires `canUseKillerFeatures(player)` **and** a free hotbar slot (0–8), otherwise it fails |
| `directToHotbar` | Skips the killer check, inserts into a free hotbar slot |
| `giveToInventory` | `player.giveItemStack(stack.copy())` — full inventory |
| `action` | Runs your predicate; the 5-arg overload can set `showFailure` to `false` for silent failure |

> The default constructor keeps vanilla's killer hotbar restriction. **Non-killer shops should explicitly use `directToHotbar`, `giveToInventory` or `action`.**

#### `ShopPrice`

**Package:** `dev.annawathe.api.shop.ShopPrice`

```java
ShopPrice.money(100);                                  // money only
ShopPrice.allOf(CurrencyAmount.money(100), CurrencyAmount.of(BLOOD, 2));   // AND
ShopPrice.anyOf(                                       // OR
        ShopPrice.option(CurrencyAmount.money(200)),
        ShopPrice.option(CurrencyAmount.of(BLOOD, 4)));
```

| Method | Description |
| --- | --- |
| `canAfford(PlayerEntity)` | Whether the player can afford it |
| `selectPayment(PlayerEntity)` → `ShopPayment` | Choose a payment option (**smallest total currency amount** among affordable options; ties keep the earlier-defined option) |
| `legacyPrice()` | Money amount in the first option, else the first option's total |
| `displayLines()` | Client price lines; inserts `shop.price.or` between options and `shop.price.free` for an empty cost |

**Options are OR'd; currencies inside one option are AND'd.** Option construction filters out `null` and `amount ≤ 0` entries.

#### Example

```java
ShopApi.registerRoleShop(MyRoles.BLOOD_MAGE, player -> List.of(
        ShopEntry.giveToInventory(
                MyItems.BLOOD_DAGGER.getDefaultStack(),
                ShopPrice.allOf(CurrencyAmount.money(150), CurrencyAmount.of(BLOOD, 2)),
                ShopEntry.Type.WEAPON)
));

ShopApi.registerShopModifier(MyMod.id("blood_shop"), 20, (context, entries) -> {
    if (context.role() != MyRoles.BLOOD_MAGE) return;
    entries.removeIf(entry -> entry.stack().isOf(WatheItems.KNIFE));
    entries.add(ShopEntry.giveToInventory(
            MyItems.BLOOD_DAGGER.getDefaultStack(), 150, ShopEntry.Type.WEAPON));
});
```

#### Purchase Flow and Responsibilities

| Stage | Owner |
| --- | --- |
| Client click | Sends **the entry index only** (`annawathe:store_buy`) |
| Server validation | Re-resolve shop list → round running + gameplay-alive → index range → payment option + balance → item cooldown |
| Delivery | **Your `purchase` callback delivers the item only** |
| Charging | AnnaWathe atomically deducts all currencies after successful delivery |

> **Add-on purchase callbacks must not charge the player themselves**, and must not treat the client-reported balance as authoritative.

---

### 3.7 `EconomyApi` / `PlayerEconomyApi`

#### `EconomyApi`

**Package:** `dev.annawathe.api.economy.EconomyApi`

| Method | Description |
| --- | --- |
| `registerCurrency(Identifier id, String icon, String translationKey, CurrencyHudPredicate predicate)` | Register a currency |
| `getCurrency(Identifier)` / `getCurrencyOrFallback(Identifier)` | Query |
| `currencySnapshot()` | All registered currencies |
| `formatCurrencyAmount(CurrencyAmount, boolean icon)` | Format as text |
| `registerBalanceHudRole(Role)` / `registerBalanceHudRoles(Collection<Role>)` | Which roles show the balance HUD |
| `registerBalanceHudPredicate(Identifier id, int priority, BalanceHudPredicate)` | Conditional balance HUD |
| `shouldRenderBalanceHud(PlayerEntity)` | Whether to show balances |
| `getVisibleCurrencyBalances(PlayerEntity, boolean shopFallback)` | Visible balance list |
| `registerPassiveIncomeRole(Role)` / `registerPassiveIncomeRoles(Collection<Role>)` | Which roles receive passive income |
| `registerPassiveIncomeRule(Identifier id, int priority, PassiveIncomeRule)` | Eligibility rule (`ALLOW` / `DENY` / `PASS`, first non-PASS decides) |
| `registerPassiveIncomeModifier(Identifier id, int priority, PassiveIncomeModifier)` | Income value modifier (chained, each step `max(0, ·)`) |
| `canReceivePassiveIncome(...)` / `calculatePassiveIncome(..., int base)` | Query |

**Constants:** `MONEY = wathe:money`, `TASK_MONEY = wathe:task_money`, `MONEY_ICON = "\uE781"`, `TASK_MONEY_ICON = "\uE782"`, `TASK_MONEY_PER_KILLER_TASK = 0`, `TASK_MONEY_PER_KILL = 0`.

**Passive income resolution order:** income rules → `PASSIVE_INCOME_ROLES` → `canUseKillerFeatures`.

#### `PlayerEconomyApi`

**Package:** `dev.annawathe.api.economy.PlayerEconomyApi`

| Method | Description |
| --- | --- |
| `get(PlayerEntity, Identifier currency)` | Read a balance |
| `set(PlayerEntity, Identifier currency, int amount)` | Set a balance |
| `add(PlayerEntity, Identifier currency, int amount)` | Add/subtract |
| `spend(PlayerEntity, List<CurrencyAmount> costs)` | Atomic spend (validate everything, deduct once, sync once) |
| `snapshot(PlayerEntity)` | Snapshot of all balances |

#### Example

```java
public static final Identifier BLOOD = MyMod.id("blood");

EconomyApi.registerCurrency(
        BLOOD,
        "\uE783",
        "currency.mymod.blood",
        context -> context.role() == MyRoles.BLOOD_MAGE);

PlayerEconomyApi.add(player, BLOOD, 3);
int amount = PlayerEconomyApi.get(player, BLOOD);
```

#### Notes

- Balances live in vanilla's `wathe:shop` CCA: money uses the vanilla `balance` field, other currencies go into the `CurrencyBalances` NBT, following vanilla's sync and `reset()`.
- **Add-ons do not need to sync balances** or send packets.

---

### 3.8 `PsychoModeApi` / `PsychoModeProfile`

#### Constants

```java
PsychoModeApi.DEFAULT_PROFILE_ID = wathe:psycho_mode
PsychoModeApi.DEFAULT_MODE_NAME_TRANSLATION_KEY  = "psycho_mode.wathe.default"
PsychoModeApi.DEFAULT_SHIELD_NAME_TRANSLATION_KEY = "psycho_shield.wathe.default"
```

#### State Control

| Method | Description |
| --- | --- |
| `start(PlayerEntity)` / `start(PlayerEntity, Identifier profileId)` / `start(PlayerEntity, PsychoModeProfile)` | Start (returns `false` when preconditions such as insufficient hotbar space fail) |
| `stop(PlayerEntity)` / `stop(PlayerEntity, boolean recordReplay)` | Stop |
| `isActive(PlayerEntity)` / `isActive(PlayerEntity, Identifier profileId)` | Whether it is active |
| `getActiveProfile` / `getRemainingTicks` / `getArmour` / `getMaxTicks` / `getInitialArmour` | State queries |

#### Registration

| Method | Description |
| --- | --- |
| `registerProfile(PsychoModeProfile profile)` | Register a profile |
| `getProfile(Identifier)` / `getProfileOrDefault(Identifier)` / `createDefaultProfile()` | Query |
| `registerShieldRule(Identifier id, int priority, PsychoShieldRule handler)` | Shield / shield-bypass rules |
| `registerStartProfileProvider(Identifier id, int priority, StartProfileProvider provider)` | Dynamically choose which profile starts |

#### Queries and Helpers

| Method | Description |
| --- | --- |
| `isLockedItem` / `shouldPreventDrop` / `findLockedHotbarSlot` | Hotbar-lock queries (`findLockedHotbarSlot` scans 0–8 for the first locked slot) |
| `isMeleeKillWeapon` / `getMeleeHitSound` | Melee-kill queries |
| `shouldPlayBackgroundSound(World, SoundEvent)` | Background sound condition |
| `markGrantedItem(profile, stack)` / `isGrantedForProfile(stack, profileId)` | Granted-item marker (data component `annawathe:psycho_granted_profile`) |
| `resolveShield(PsychoShieldContext)` | Shield arbitration (**add-ons may call this to reuse the same rules**) |
| `putModeReplayData` / `createModeReplayData` / `resolveModeNameTranslationKey` / `resolveShieldNameTranslationKey` | Replay data |

#### Shield Rule Interface

```java
@FunctionalInterface
public interface PsychoShieldRule { PsychoShieldResult resolve(PsychoShieldContext context); }

public enum PsychoShieldResult { PASS, BLOCK, BYPASS }

// Read-only context for one server-side "may have hit a psycho player" event
public record PsychoShieldContext(
        @NotNull PlayerEntity victim,
        @Nullable PlayerEntity killer,
        @NotNull Identifier deathReason,
        @NotNull PlayerPsychoComponent component,
        @NotNull PsychoModeProfile profile,
        @NotNull NbtCompound replayData) {}
```

#### Shield Priority

Rules sort by `priority` descending, then registration order descending, and the **first non-PASS** result wins:

- `PsychoShieldResult.BLOCK` → consume one shield layer and cancel the death
- `PsychoShieldResult.BYPASS` → bypass the shield
- All `PASS` or `null` → falls back to `armour > 0 ? BLOCK : PASS`

#### All `PsychoModeProfile` Options

```java
PsychoModeProfile.builder(MyMod.id("my_psycho"))
        .nameTranslationKey("psycho_mode.mymod.my_psycho")
        .shieldNameTranslationKey("psycho_shield.mymod.my_psycho")
        .durationTicks(600)                          // ≥ 1
        .armour(1)                                   // ≥ 0
        .grantItem(stack) / .grantedItems(List<ItemStack>)
        .lockHotbar(true)
        .lockGrantedItems(true)
        .removeGrantedItemsOnEnd(true)
        .selectFirstGrantedItem(true)
        .preventDroppingLockedItems(true)
        .meleeKill(true, MyMod.id("my_death_reason"))
        .shieldSourceId(MyMod.id("my_psycho"))
        .endEventId(MyMod.id("my_psycho_end"))
        .hitSound(soundEvent) / .shieldSound(soundEvent)
        .backgroundSound(soundEvent, true)           // sound + whether to play it
        .visualSettings(PsychoVisualSettings.skin(wideTex, slimTex, true))
        .lockedItemPredicate(stack -> ...)           // extra lock predicate
        .meleeWeaponPredicate(stack -> ...)          // extra melee weapon predicate
        .build();

PsychoModeProfile.copyOf(existingProfile, MyMod.id("copy"));   // full copy
```

`PsychoVisualSettings.none()` or `PsychoVisualSettings.skin(wide, slim, hideFeatures)`.

#### Notes

- Temporary granted items must come from the profile's `grantedItems`; AnnaWathe writes them and **precisely reclaims** the marked items at the end. Do not hand out items yourself.
- **Precondition to start: the number of granted items must not exceed free hotbar slots**, otherwise `start` returns `false`.
- Hotbar locking is defended three ways: scroll rollback, number-key rejection, and the server discarding illegal slot packets.

---

### 3.9 `PlayerLifeStateApi`

**Package:** `dev.annawathe.api.PlayerLifeStateApi`

| Method | Description |
| --- | --- |
| `hasAliveOverride(PlayerEntity)` | Whether a special alive grant is held |
| `setAliveInCurrentGameMode(ServerPlayerEntity, boolean)` | Set it directly |
| `clearAliveOverride(PlayerEntity)` | Clear it |
| `changeGameModeAsGameplayAlive(ServerPlayerEntity, GameMode)` | **Recommended entry**: grants on switching to creative/spectator, clears when switching back to survival |
| `isGameplayAliveGameModeChangeAllowed(ServerPlayerEntity)` | Whether a given change is an authorised one (used by the revocation hook) |
| `isNonSurvivalMode(GameMode)` | Whether the mode is creative/spectator |

#### Semantics

Once granted, these three vanilla checks are overridden:

| Vanilla check | Overridden result |
| --- | --- |
| `isPlayerAliveAndSurvival` | `true` |
| `isPlayerSpectatingOrCreative` | `false` |
| `isPlayerEliminated` | `false` |

#### Notes

- **Only changes Anna/Wathe gameplay-alive checks**, not vanilla permissions or item consumption.
- A grant is only meaningful for `CREATIVE` / `SPECTATOR`; a normal `/gamemode` **revokes** it.
- The component's respawn strategy is `NEVER_COPY`: neither death nor respawn copies it.
- Players without a role in the current round cannot be granted special alive status.

---

### 3.10 `PlayerMovementApi`

**Package:** `dev.annawathe.api.movement.PlayerMovementApi`

| Method | Description |
| --- | --- |
| `registerSpeedModifier(Identifier id, int priority, MovementSpeedModifier modifier)` | Register a speed modifier rule |
| `resolveMovementSpeed(PlayerEntity, float vanillaSpeed, float baseSpeed)` | Resolve |
| `canSelfMove(PlayerEntity)` | **Always returns `true`** (reserved interface, currently no callers) |
| `canJump(PlayerEntity)` | **Always returns `true`** (reserved interface, currently no callers) |

```java
public record MovementSpeedResult(Operation operation, float value) {
    public static MovementSpeedResult pass();
    public static MovementSpeedResult add(float value);
    public static MovementSpeedResult multiply(float value);
    public static MovementSpeedResult override(float value);
    public enum Operation { PASS, ADD, MULTIPLY, OVERRIDE }
}
public record MovementSpeedContext(PlayerEntity player, GameWorldComponent gameWorld,
        @Nullable Role role, boolean sprinting, float vanillaSpeed,
        float baseSpeed, float currentSpeed) {}
```

**How the chain computes:** starts at `current = max(0, baseSpeed)`, then applies `ADD` (+), `MULTIPLY` (*) and `OVERRIDE` (=) in order; `PASS` and non-finite values are skipped; it ends with another `max(0, current)`.

**Base speeds:** `0.10F` while sprinting, `0.07F` otherwise (only applied to gameplay-alive players).

> **This API contains no stamina system.** The modified Wathe's stamina drain, mood-based stamina penalty and jump restrictions were **not** migrated. If stamina is added later it needs its own component, sync and lifecycle cleanup — do not smuggle stamina fields into the speed API.

---

### 3.11 `PlayerCollisionApi`

**Package:** `dev.annawathe.api.collision.PlayerCollisionApi`

| Method | Description |
| --- | --- |
| `registerRule(Identifier id, int priority, Rule rule)` | Register a collision rule (short-circuiting chain) |
| `resolve(PlayerEntity self, PlayerEntity other)` | Resolve the final mode |
| `blocksMovement(self, other)` / `suppressesPush(self, other)` | Query |

```java
public enum PlayerCollisionMode {
    PASS(false, true),          // defer to later rules / vanilla
    SOLID(true, true),          // blocks movement + keeps vanilla pushing
    VANILLA_PUSH(false, true),  // does not block movement + keeps vanilla pushing
    NO_COLLISION(false, false); // blocks nothing, pushes nothing
}

public record PlayerCollisionContext(...) {}
```

#### Default Resolution Order

1. Different worlds → `PASS`;
2. Rule chain returns the first non-`PASS`;
3. If all `PASS`, the default rule applies: `PASS` when `!game.isRunning()`, or the master switch is off, or either party is not gameplay-alive; otherwise `isStartDelayActive() ? VANILLA_PUSH : SOLID`.

`suppressesPush` returns `true` only when **both directions** disallow pushing.

#### Spawn Collision Immunity

World component `AnnaCollisionSettings`: `enabled` (default `true`), `startDelaySeconds` (default **30**), `roundStartTick`.

> **The start tick is recorded after the round truly enters `ACTIVE`**, not at the moment `/start` is executed.

#### Notes

- Underlying Mixins: `Entity#collidesWith`, `Entity#pushAwayFrom`, `EntityView#getEntityCollisions` and `LivingEntity#pushAway` all consult this API. **Add-on roles must not inject these low-level entry points again.**
- Corpses (`PlayerBodyEntity`) are excluded from movement collision.

---

### 3.12 `PlayerTransformApi`

**Package:** `dev.annawathe.api.appearance.PlayerTransformApi`

| Method | Description |
| --- | --- |
| `setAppearance(PlayerEntity target, UUID appearanceUuid, int seconds)` | Timed transformation (`seconds < 0` means permanent) |
| `setPermanentAppearance(PlayerEntity target, UUID appearanceUuid)` | Permanent transformation |
| `clearAppearance(PlayerEntity target)` | Clear one player |
| `clearAll()` | Clear every online player |
| `isActive(PlayerEntity target)` | Whether a transformation is active |
| `getTargetUuid(PlayerEntity target)` | Current appearance UUID |
| `getRemainingSeconds(PlayerEntity target)` | Remaining seconds (returns `-1` when permanent) |

#### Notes

- **Only changes the client-side display UUID**; it changes no role, faction, sound, held item, collision or server-side identity.
- Its priority is **lower than** add-on role disguises and hallucination views.
- Permanent state persists across death, rounds and restarts until explicitly cleared. This API is an **administrator debug facade**.

---

## 4. Client Interaction APIs

### 4.1 `ItemTooltipApi`

**Package:** `dev.annawathe.api.client.tooltip.ItemTooltipApi`
**Environment:** client only

| Method | Description |
| --- | --- |
| `initialize()` | Called by AnnaWathe's client entrypoint; add-ons do not need it |
| `registerItem(Item item)` | Register standard description lines |
| `registerItems(Item... items)` | Batch |
| `registerAppender(Identifier id, int priority, Item item, TooltipAppender app)` | Append dynamic text |
| `getRemainingCooldownTicks(PlayerEntity player, Item item)` | Read the real remaining cooldown in ticks |
| `formatCooldownTicks(int ticks)` | Format cooldown text |
| `COOLDOWN_COLOR` | Cooldown text colour constant |

#### Example

```java
ItemTooltipApi.registerItem(MY_ITEM);
ItemTooltipApi.registerAppender(MyMod.id("item_state"), 10, MY_ITEM, context -> {
    context.tooltip().add(Text.literal("extra state"));
});
```

#### Key Implementation Details

- The standard tooltip reads **`<item translation key>.tooltip`**, e.g. `item.wathe.knife.tooltip`.
- Cooldowns are read **directly from the live `ItemCooldownManager` entry as `endTick - tick`**, **never** reverse-engineered from a fixed total or a progress value — so cooldown acceleration or reduction displays correctly.
- Cooldown access deliberately avoids reflection (two `@Accessor` Mixins) so that production remapping cannot silently turn the countdown into zero.

#### Notes

- **Tooltips are display only** and must not be used as server-side legality checks.
- AnnaWathe **disables vanilla Wathe's tooltip callback** — do **not** register another global `ItemTooltipCallback` for the same items.

---

### 4.2 `InventoryButtonApi`

**Package:** `dev.annawathe.api.client.inventory.InventoryButtonApi`
**Environment:** client only. Supports `LIMITED` (limited inventory), `VANILLA` (normal inventory) and `CREATIVE` (creative inventory) screens.

#### Methods

| Method | Description |
| --- | --- |
| `registerProvider(Identifier id, int priority, InventoryButtonProvider provider)` | Register a provider |
| `initializeScreen(...)` / `tickScreen(...)` / `renderScreen(...)` / `closeScreen(...)` | Dispatched by AnnaWathe; add-ons do not call these |
| `allowInventoryKeyClose(Screen, int keyCode, int scanCode)` | Queried by the dispatcher |
| `reset()` | Called by AnnaWathe on disconnect |

#### `InventoryButtonExtension` Lifecycle

A **fresh extension instance** is created for every screen opening, so it is safe to keep per-screen temporary state. The API dispatches:

```java
public interface InventoryButtonExtension {
    default void init(InventoryButtonContext context) {}
    default void tick(InventoryButtonContext context) {}
    default void render(InventoryButtonContext context, DrawContext draw, int mouseX, int mouseY, float delta) {}
    default boolean allowInventoryKeyClose(InventoryButtonContext context, int keyCode, int scanCode) { return true; }
    default void close(InventoryButtonContext context) {}
}
```

#### Example

```java
InventoryButtonApi.registerProvider(MyMod.id("guide"), 0, context -> {
    if (context.type() == InventoryScreenType.LIMITED) return null;   // do not attach on this screen
    return new InventoryButtonExtension() {
        @Override
        public void init(InventoryButtonContext context) {
            context.addWidget(MY_GROUP, createGuideButton(context));
        }

        @Override
        public boolean allowInventoryKeyClose(InventoryButtonContext context, int keyCode, int scanCode) {
            return !isEditingText();
        }
    };
});
```

#### Dynamic Widgets and Paging

| Method | Description |
| --- | --- |
| `addWidget(Identifier group, ClickableWidget widget)` | Add to a group |
| `replaceGroup(Identifier group, List<ClickableWidget>)` | Replace a whole group |
| `clearGroup(Identifier group)` | Clear a group (hidden, disabled, unfocused) |
| `setGroupVisible(Identifier group, boolean visible)` | Show/hide a group |

`InventoryPageState` isolates page numbers per `Identifier` and is cleared on **disconnect and round changes**.

#### Notes

- Dynamic widgets must be managed with **their own group ID**; on close, old widgets are hidden, disabled and unfocused.
- `allowInventoryKeyClose` only controls key behaviour on the current screen and **never replaces server-side interaction validation**.

---

### 4.3 `PsychoModeClientApi`

**Package:** `dev.annawathe.api.client.psycho.PsychoModeClientApi`
**Environment:** client only

| Method | Description |
| --- | --- |
| `registerDefaultClientHandlers()` | Called by AnnaWathe's client entrypoint; add-ons do not need it |
| `registerVisualProvider(Identifier id, int priority, VisualProvider provider)` | Register a visual provider (short-circuiting; first non-null wins) |
| `registerBackgroundAmbience(SoundEvent sound, int intervalTicks)` | Register background ambience (deduplicated per sound id) |
| `resolveVisualSettings(AbstractClientPlayerEntity)` | Resolve the final visual settings |
| `resolveSkinTextures(AbstractClientPlayerEntity)` | Resolve the psycho skin |
| `shouldHideFeatures(AbstractClientPlayerEntity)` | Whether normal model features are hidden |

**Constants:** `DEFAULT_VISUAL_PRIORITY = 0`, `PLAYER_APPEARANCE_PRIORITY = 10_000` (the priority the default psycho skin registers with).

#### Notes

- Visual providers only affect **client skins, model features and background audio** and **can never replace server-side validation**.
- The default psycho skin registers with `PlayerAppearanceApi` at priority `10000`; you need a higher priority to override it.
- Background audio plays when "at least one active profile in the world declares that sound".

---

### 4.4 Supplementary & Helper Types

These types are public in the API package but are "used together with" or "read-only query" in nature. They are listed here so the reference is complete.

#### `PsychoDataComponentTypes`

**Package:** `dev.annawathe.api.psycho.PsychoDataComponentTypes`
**Environment:** common

| Member | Description |
| --- | --- |
| `PSYCHO_GRANTED_PROFILE` | `ComponentType<String>` registered as **`annawathe:psycho_granted_profile`** |
| `init()` | Forces this class to load during common init (AnnaWathe already calls it; add-ons do not need to) |

**Purpose:** marks "temporary psycho items" with a profile tag so that on end AnnaWathe **reclaims only the items it granted**, never a player's own identical item.

> **Registration timing is critical:** data components must be registered **before the Minecraft registry freezes**. AnnaWathe's very first action in `onInitialize` is `PsychoDataComponentTypes.init()`, and a source comment states explicitly that it **must not be lazily loaded when a player first buys Psycho Mode**. Add-ons with their own data components must respect the same timing.

#### `ShopPayment`

**Package:** `dev.annawathe.api.shop.ShopPayment`

```java
public record ShopPayment(int optionIndex, @NotNull List<CurrencyAmount> costs) {
    public static ShopPayment of(int optionIndex, List<CurrencyAmount> costs);
    public static ShopPayment money(int amount);
    public int totalAmount();
    public NbtList toNbtList();
}
```

The return value of `ShopPrice.selectPayment(player)` / `cheapestPaymentForDevelopment()`. `optionIndex` is the index of the matched option in the price definition; `costs` is an immutable copy.

#### Remaining Contexts and Enums (read-only query use)

| Type | Package | Purpose |
| --- | --- | --- |
| `BodyInfoSnapshot` | `api.body` | Corpse snapshot: `deathReason`, `roleId` (nullable), `deathWorldTime` |
| `PlayerCollisionContext` | `api.collision` | Read-only context for collision rules (`self`, `other`, `world`, …) |
| `CurrencyDefinition` | `api.economy` | Currency definition: `id`, `icon`, `translationKey`, `order`, `hudPredicate` |
| `PsychoItemPredicate` | `api.psycho` | Psycho item predicate interface (used by `lockedItemPredicate` / `meleeWeaponPredicate`) |
| `PsychoShieldContext` / `PsychoShieldResult` | `api.psycho` | Shield rule context and result (see §3.8) |
| `ShopContext` | `api.shop` | Shop resolution context (`player`, `gameWorld`, `role`, `roleSpecificShop`) |
| `ShopModifier` / `RoleShopProvider` | `api.shop` | Functional interfaces for shop modifiers and role shop providers |
| `ShopPurchaseContext` / `ShopPurchaseResult` | `api.shop` | Purchase context and purchase result enum |
| `MoodTaskDefinition` | `api.task` | Task definition (built with `MoodTaskDefinition.builder(...)`, see §3.2) |
| `MoodTaskInstance` | `api.task` | Task runtime instance interface (your add-on implements it) |
| `TaskPointDefinition` / `TaskPointScanContext` | `api.task` | Task point definition and scan context (see §3.3) |
| `CustomVictoryGroup` | `api.win` | Custom victory group (`titleTranslationKey`, `fallbackTitle`, `color`, `playerUuids`) |
| `HudOverlayContext` / `HudOverlayLayer` / `HudOverlayLayout` | `api.client.hud` | Generic HUD context, phase enum and layout helpers (see §2.4) |
| `MoodHudContext` / `MoodHudStyle` / `PsychoMoodHudStyle` / `MoodHudColors` | `api.client.mood` | Mood HUD context, styles and colour helper (see §2.5) |
| `InventoryButtonContext` / `InventoryButtonExtension` / `InventoryButtonProvider` / `InventoryButtonLayout` / `InventoryPageState` / `InventoryPageSwitchWidget` / `InventoryScreenType` | `api.client.inventory` | Inventory button context, lifecycle interface, layout, page state and screen types (see §4.2) |

#### Internal Access Interface (add-ons must not use)

| Type | Package | Description |
| --- | --- | --- |
| `PlayerEconomyAccess` | `api.economy` | An **internal bridge interface** implemented by `PlayerShopComponentEconomyMixin` on the `wathe:shop` component to read/write non-money balances. Always access balances through `PlayerEconomyApi`; never reference this directly |

---

## 5. Internal Facilities Reference

**For understanding the implementation only — add-ons must not depend on any of this.**

### 5.1 CCA Components

| Component | CCA id | Attached to |
| --- | --- | --- |
| `PlayerInstinctComponent` | `annawathe:instinct` | Player (CHARACTER copy) |
| `AnnaRoundEndState` | `annawathe:round_state` | World + Scoreboard |
| `AnnaMoodSettings` | `annawathe:mood_settings` | World |
| `AnnaTaskPointWorldState` | `annawathe:task_points` | World |
| `PlayerLifeStateComponent` | `annawathe:life_state` | Player (NEVER_COPY) |
| `PlayerAppearanceOverrideComponent` | `annawathe:appearance_override` | Player (CHARACTER copy) |
| `AnnaCollisionSettings` | `annawathe:collision_settings` | World |
| `AnnaBodyInfoComponent` | `annawathe:body_info` | PlayerBodyEntity (NEVER_COPY) |

### 5.2 Network Packets

| Packet | Direction | Payload |
| --- | --- | --- |
| `annawathe:task_point_sync` | S2C | Whole task-point table snapshot |
| `annawathe:store_buy` | C2S | Shop entry index only |

### 5.3 Data Components

| Data component | id | Type | Purpose |
| --- | --- | --- | --- |
| `PSYCHO_GRANTED_PROFILE` | `annawathe:psycho_granted_profile` | `ComponentType<String>` | Marks temporary psycho items granted by Anna so they can be precisely reclaimed at the end (see §4.4) |

### 5.4 Internal Packages

| Package | Contents |
| --- | --- |
| `dev.annawathe.bridge` | Internal bridges: `PsychoComponentBridge`, `MoodTaskBridge`, `PlayerBodyAppearanceBridge` |
| `dev.annawathe.compat.wathe` | Wathe version-difference compatibility (reflective `TaskCompletePayload` lookup) |
| `dev.annawathe.mixin.compat` | HarpyModLoader soft compatibility (loaded only when that mod is detected) |
| `dev.annawathe.mood` | Mood state machine and built-in task implementations |
| `dev.annawathe.task` | Task-point scanning and sync dispatch |
| `dev.annawathe.network` | The two custom payloads |

---

## 6. Migration Checklist

Steps to migrate an old "deep Mixin" add-on onto the AnnaWathe API:

1. **Confirm your base version**: your add-on targets **vanilla Wathe**, not a modified Wathe.
2. **Instinct**: replace your `WatheClient` instinct Mixin with `InstinctApi.registerAvailability` / `registerHighlight`.
3. **Victory**: replace your `MurderGameMode` victory Mixin with `VictoryApi.registerRule`.
4. **Tooltips**: replace your custom tooltip callback with `ItemTooltipApi`.
5. **Tasks**: do not add values to vanilla's `PlayerMoodComponent.Task`; use `MoodTaskApi.registerTask`.
6. **Mood HUD**: do not Mixin `MoodRenderer`; use `MoodHudApi`.
7. **Crosshair / crosshair name / generic HUD**: do not Mixin `CrosshairRenderer` / `RoleNameRenderer` / `InGameHud`; use the matching API.
8. **Time HUD / inventory buttons**: do not Mixin `TimeRenderer`; use `TimeHudApi` and `InventoryButtonApi`.
9. **Collision / alive checks**: do not inject `Entity#collidesWith` and friends; use `PlayerCollisionApi` / `PlayerLifeStateApi`.
10. **Remove** the old Mixin json entries that AnnaWathe now covers.
11. **Rebuild** your add-on and test the three-way combination "**vanilla Wathe + AnnaWathe + your add-on**".

---

## Appendix: FAQ

**Q: Why isn't my rule taking effect?**
A: Check in order — ① registered at the right init phase (shops must be common); ② whether another rule replaced yours (same ID replaces); ③ whether a higher-priority rule short-circuited first; ④ for client APIs, whether it was registered in the client entrypoint.

**Q: What if two add-ons register the same ID?**
A: The later registration **replaces** the earlier one. Always prefix IDs with your own mod namespace, e.g. `MyMod.id("hud/xxx")`.

**Q: Can I call client APIs from the server?**
A: No. Classes in `dev.annawathe.api.client.*` do not exist on a dedicated server and will crash. Keep those calls in your client entrypoint or client-only code.

**Q: Why didn't `removeTask` restore my mood?**
A: That is intended. `removeTask` is a silent removal; only `completeTask` runs the real flow (mood, animation, completion event and income).

**Q: What happens if I reclaim the items granted by Psycho Mode myself?**
A: Do not manage them manually. `grantedItems` are written and precisely reclaimed by AnnaWathe; handling them yourself causes leftover or wrongly deleted items.

---

*Compiled from the `annawathe` source tree. Chinese edition: [`02-API参考-中文.md`](02-API参考-中文.md).*
