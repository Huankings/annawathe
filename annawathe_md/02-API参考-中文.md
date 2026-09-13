# AnnaWathe API 参考（中文）

> 面向扩展开发者的公开接口手册
> 适用于 AnnaWathe `1.0.0-1.21.1` + 原版 Wathe `1.3.2-1.21.1`

**本文档覆盖 `dev.annawathe.api` 包下全部 24 组公开 API**，共 70 个文件（common 45 / client 25）。所有签名均取自源码，未做简化或推测。

---

## 目录

- [0. 使用前提](#0-使用前提)
- [1. 通用规则](#1-通用规则)
- [2. 视觉类 API](#2-视觉类-api)
  - [2.1 InstinctApi](#21-instinctapi-本能透视)
  - [2.2 CrosshairHudApi](#22-crosshairhudapi-准心)
  - [2.3 RoleNameHudApi](#23-rolenamehudapi-准心名字)
  - [2.4 HudOverlayApi](#24-hudoverlayapi-通用屏幕-hud)
  - [2.5 MoodHudApi](#25-moodhudapi-心情-hud)
  - [2.6 TimeHudApi](#26-timehudapi-顶部计时)
  - [2.7 BodyInfoApi / BodyInfoHudApi](#27-bodyinfoapi--bodyinfohudapi-尸体信息)
  - [2.8 PlayerAppearanceApi / BodyAppearanceApi](#28-playerappearanceapi--bodyappearanceapi-外观)
  - [2.9 HeldItemInvisibilityApi / PsychosisItemApi](#29-helditeminvisibilityapi--psychosisitemapi-手持物隐藏与幻觉)
  - [2.10 TargetVisibilityApi](#210-targetvisibilityapi-目标可见性)
- [3. 玩法类 API](#3-玩法类-api)
  - [3.1 VictoryApi / CustomVictory](#31-victoryapi--customvictory-胜利仲裁)
  - [3.2 MoodTaskApi](#32-moodtaskapi-任务注册与发放)
  - [3.3 MoodTaskPointApi](#33-moodtaskpointapi-任务点)
  - [3.4 TaskCompletionApi](#34-taskcompletionapi-任务完成事件与收入)
  - [3.5 MoodApi](#35-moodapi-心情数值)
  - [3.6 ShopApi / ShopEntry / ShopPrice](#36-shopapi--shopentry--shopprice-商店)
  - [3.7 EconomyApi / PlayerEconomyApi](#37-economyapi--playereconomyapi-经济)
  - [3.8 PsychoModeApi / PsychoModeProfile](#38-psychomodeapi--psychomodeprofile-疯魔模式)
  - [3.9 PlayerLifeStateApi](#39-playerlifestateapi-玩法存活)
  - [3.10 PlayerMovementApi](#310-playermovementapi-移动速度)
  - [3.11 PlayerCollisionApi](#311-playercollisionapi-玩家碰撞)
  - [3.12 PlayerTransformApi](#312-playertransformapi-调试变形)
- [4. 客户端交互类 API](#4-客户端交互类-api)
  - [4.1 ItemTooltipApi](#41-itemtooltipapi-物品-tooltip)
  - [4.2 InventoryButtonApi](#42-inventorybuttonapi-背包按钮)
  - [4.3 PsychoModeClientApi](#43-psychomodeclientapi-疯魔客户端视觉)
  - [4.4 补充与辅助类型](#44-补充与辅助类型)
- [5. 内部设施速查](#5-内部设施速查)
- [6. 迁移清单](#6-迁移清单)

---

## 0. 使用前提

### 0.1 依赖配置

```gradle
repositories {
    maven { url = 'https://maven.ladysnake.org/releases' }   // Cardinal Components
}

dependencies {
    modApi files("libs/annawathe-1.0.0-1.21.1.jar")
    modApi files("libs/wathe-1.3.2-1.21.1.jar")            // AnnaWathe 的 API 签名引用 Wathe 类型
}
```

在 `fabric.mod.json` 中声明依赖：

```json
{
  "depends": {
    "annawathe": ">=1.0.0-1.21.1",
    "wathe": ">=1.3.2-1.21.1"
  }
}
```

### 0.2 注册时机（非常重要）

| 注册内容 | 必须的时机 |
| --- | --- |
| **职业商店、默认商店修改器** | **common 初始化阶段**（`ModInitializer#onInitialize`）——客户端显示与服务端购买解析必须用同一份列表 |
| 任务定义、任务点类型、扫描 handler | common 初始化阶段 |
| 货币、被动收入规则 | common 初始化阶段 |
| 疯魔 profile、护盾规则 | common 初始化阶段（`PsychoModeApi.init()` 之后） |
| 所有客户端 API（HUD、准心、Tooltip、皮肤） | client 初始化阶段（`ClientModInitializer#onInitializeClient`） |

### 0.3 客户端 / 服务端分包

AnnaWathe 使用 Fabric 的 `splitEnvironmentSourceSets`：

- 服务端与通用代码：`dev.annawathe.api.*`（除 `api.client.*`）
- **仅客户端**：`dev.annawathe.api.client.*` —— 这些类带 `@Environment(EnvType.CLIENT)`，在服务端直接引用会崩溃

---

## 1. 通用规则

### 1.1 优先级语义

**AnnaWathe 的所有规则链统一遵循同一套语义**，可以放心组合：

| 规则 | 说明 |
| --- | --- |
| `priority` 越大越先执行 | 与 Minecraft 事件优先级习惯相反，请注意 |
| `priority` 相同时，**后注册者先执行** | 便于扩展覆盖内置规则 |
| 使用**相同的 Identifier** 重复注册 | **覆盖**旧规则（不是追加），便于热重载配置 |
| `DEFAULT_PRIORITY` | 各 API 都提供该常量，值均为 `0` |

> **两条链是不同的：** 短路链（如 `InstinctApi`、`VictoryApi`、`CrosshairHudApi` 的 provider）取第一个"非 PASS"结果就结束；非短路链（如 `HudOverlayApi`、`RoleNameHudApi.renderExtraHud`）会执行全部注册项。

### 1.2 客户端 / 服务端边界

AnnaWathe 把边界写在了 API 文档注释里，扩展必须遵守：

> **客户端视觉永远不能替代服务端校验。** 准心、名字、HUD、皮肤、手持物隐藏、幻觉物品全部只是显示层。真实攻击、交互、购买和技能合法性必须在服务端重新判定。

对应的服务端复核入口：

| 客户端显示 | 服务端必须复核 |
| --- | --- |
| `CrosshairHudApi` 显示"可以开枪" | 服务端检查弹药、冷却、距离、目标合法性 |
| `TargetVisibilityApi` 客户端选中 | 服务端 `canInteractWith*` / `canAttack*` |
| `PlayerAppearanceApi` 伪装皮肤 | 服务端职业、阵营、声音、身份不变 |
| `BodyAppearanceApi` 尸体外观 | 真实 owner UUID 必须保留给验尸、尸袋与回放 |
| `PsychosisItemApi` 幻觉手持物 | 服务端真实物品不变 |

### 1.3 不要触碰的内部实现

| 包 / 类 | 原因 |
| --- | --- |
| `dev.annawathe.bridge.*` | 内部桥接接口，源码注释明确"扩展不应直接使用" |
| `dev.annawathe.mixin.*` | Mixin 注入点，版本间会变化 |
| `dev.annawathe.network.*` | 内部网络包 |
| `dev.annawathe.task.*`、`dev.annawathe.mood.*` | 内部状态机与扫描器 |

### 1.4 不要重复 Mixin 的底层入口

以下入口已被 AnnaWathe **完整接管**，扩展再次 Mixin 会导致冲突或规则失效：

| 原版目标 | 请改用 |
| --- | --- |
| `WatheClient`（本能） | `InstinctApi` |
| `CrosshairRenderer` | `CrosshairHudApi` |
| `RoleNameRenderer` | `RoleNameHudApi` |
| `InGameHud`（普通 HUD / 黑屏 / 狙击镜） | `HudOverlayApi` |
| `MoodRenderer` | `MoodHudApi` + `AnnaMoodRenderer` |
| `TimeRenderer` | `TimeHudApi` |
| `StoreRenderer` | 已由 `AnnaStoreRenderer` 接管 |
| `RoundTextRenderer` | `AnnaRoundTextRenderer` 的公开布局方法 |
| `MurderGameMode`（胜利） | `VictoryApi` |
| `WatheItemTooltips`（Tooltip callback） | `ItemTooltipApi` |
| `WatheClient`（心跳/生命判断） | `PlayerLifeStateApi` |
| `Entity#collidesWith` / `pushAwayFrom` / `LivingEntity#pushAway` | `PlayerCollisionApi` |

---

## 2. 视觉类 API

### 2.1 `InstinctApi` 本能透视

**包名：** `dev.annawathe.api.instinct.InstinctApi`
**环境：** 仅客户端使用（服务端调用无意义但不崩溃）

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerAvailability(Identifier id, int priority, AvailabilityHandler h)` | 注册"本能是否可用"规则 |
| `registerHighlight(Identifier id, int priority, HighlightHandler h)` | 注册"目标显示什么颜色"规则 |
| `resolveAvailability(PlayerEntity viewer)` | 解析资格（通常无需自行调用） |
| `resolveHighlight(PlayerEntity viewer, Entity target)` | 解析高亮 |

#### 结果类型

```java
public enum AvailabilityResult { PASS, ENABLE, DISABLE }
// PASS   = 继续询问低优先级规则
// ENABLE = 启用本能，并结束资格链
// DISABLE= 禁用本能，并结束资格链

public record HighlightResult(Action action, int color) {
    public static HighlightResult pass();          // 继续
    public static HighlightResult color(int c);    // 使用该颜色，结束链
    public static HighlightResult hide();          // 隐藏该目标，结束链
    public enum Action { PASS, COLOR, HIDE }
}
```

#### 示例

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

#### 注意事项

- **必须使用 `AnnaWatheClient.inputActive()`** 判断本能键是否激活，不要直接读取 `WatheClient.instinctKeybind` —— 因为 AnnaWathe 支持"按一下切换"模式，直接读 keybind 会在该模式下失效。
- 本能只是客户端视觉，**不能**用它替代服务端的攻击/交互/购买校验。

---

### 2.2 `CrosshairHudApi` 准心

**包名：** `dev.annawathe.api.client.gui.CrosshairHudApi`
**环境：** 仅客户端。只在第一人称绘制准心时调度，第三人称不会调用。

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerProvider(Identifier id, int priority, Provider provider)` | 注册准心 provider（**短路链**） |
| `registerOverlay(Identifier id, int priority, Overlay overlay)` | 注册准心后置 overlay（**不短路**） |
| `renderStandardCrosshair(...)` | 绘制 Wathe 标准 3x3 普通/目标准心 |
| `renderKnifeProgressCrosshair(...)` | 匕首 ready/progress 图标 |
| `renderBatProgressCrosshair(...)` | 棍棒 ready/progress 图标 |
| `renderIconProgressCrosshair(...)` | 自定义 10x7 ready/background/fill 纹理 |
| `renderCentered(...)` | 在屏幕中心局部坐标系执行自定义绘制 |
| `drawCrosshairIcon` / `drawKnifeProgressIcon` / `drawBatProgressIcon` / `drawIconProgress` | 已在居中坐标系内时更细粒度的组合 |

#### 结果语义

```java
public enum Result { PASS, HANDLED }
// PASS    = 继续询问低优先级 provider；全部 PASS 后绘制原版默认准心
// HANDLED = 停止 provider 链并跳过默认准心
//           返回 HANDLED 但不绘制任何内容 = 故意隐藏默认准心
```

- **Provider 是短路链**：`priority` 大者先执行；同 priority 后注册者先执行。
- **Overlay 永不短路**：所有 overlay 都会执行，`priority` 越大越晚绘制（越在上层）。

#### 示例

```java
// 替换默认准心
CrosshairHudApi.registerProvider(MyMod.id("crosshair/my_weapon"), 100, context -> {
    if (!context.mainHandStack().isOf(MY_WEAPON)) {
        return CrosshairHudApi.Result.PASS;
    }
    boolean target = findValidClientTarget(context.player()) != null;
    float progress = getClientProgress(context.player(), context.tickDelta());
    CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
    return CrosshairHudApi.Result.HANDLED;
});

// 保留默认准心，只在其下方追加进度条
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

#### 注意事项

- 进度值会被限制在 `0..1`，标准填充宽度限制在 `0..10`。
- 内置 helper 会**恢复矩阵与混合状态**；如果你直接操作矩阵或 `RenderSystem`，必须自行成对 `push`/`pop` 并恢复状态。
- 查询玩家/尸体目标时请同时使用 `TargetVisibilityApi.canTargetPlayer(...)` / `canTargetBody(...)`，否则隐藏目标仍会泄漏"可锁定"提示。

---

### 2.3 `RoleNameHudApi` 准心名字

**包名：** `dev.annawathe.api.client.gui.RoleNameHudApi`
**环境：** 仅客户端。这是准心下方文字与"看向谁"的统一入口。

#### 方法（九类规则）

| 方法 | 规则类型 | 说明 |
| --- | --- | --- |
| `registerHudVisibility(Identifier, int, HudVisibilityHandler)` | 是否显示整块名字 HUD | 返回 `PASS` / `SHOW` / `HIDE` |
| `registerRaycastSource(Identifier, int, RaycastSourceHandler)` | 射线来源实体 | 返回非 null 即采用（例如从摄像头/无人机发射） |
| `registerPlayerTargetFilter(Identifier, int, PlayerTargetFilter)` | 玩家目标过滤 | 返回 `PASS` / `ALLOW` / `DENY` |
| `registerName(Identifier, int, NameHandler)` | 玩家名文本 | 返回非 null 即替换 |
| `registerEntityName(Identifier, int, EntityNameHandler)` | 非玩家实体名文本 | 返回非 null 即替换 |
| `registerCohortState(Identifier, int, CohortStateHandler)` | 该玩家是否算"同伙" | 返回 `Boolean`，null 继续 |
| `registerCohortTargetState(Identifier, int, CohortTargetStateHandler)` | 同伙是否作为目标显示 | 返回 `Boolean`，null 继续 |
| `registerCohortHint(Identifier, int, CohortHintHandler)` | 是否显示同伙提示 | 返回 `PASS` / `SHOW` / `HIDE` |
| `registerExtraHud(Identifier, int, ExtraHudRenderer)` | 额外 HUD 绘制 | **不短路**，全部执行 |

#### 辅助方法

| 方法 | 说明 |
| --- | --- |
| `findLookedAtBody(ClientPlayerEntity p, float range)` | 查找视线命中的尸体（已按 `TargetVisibilityApi` 过滤） |
| `defaultLookRange(PlayerEntity p)` | 默认观察距离：旁观/创造 8 格，否则 2 格 |

#### 上下文

```java
public record Context(
    TextRenderer renderer, ClientPlayerEntity player, DrawContext drawContext,
    RenderTickCounter tickCounter, float range,
    @Nullable PlayerEntity targetPlayer, @Nullable Entity targetEntity,
    @Nullable Text displayedTargetName, float nametagAlpha, float noteAlpha) {}
```

#### 示例

```java
RoleNameHudApi.registerName(MyMod.id("disguise_name"), 100, (viewer, target, original) -> {
    String alias = myDisguiseOf(target);
    return alias == null ? null : Text.literal(alias);
});
```

#### 注意事项

- 所有回调都只属于**客户端显示层**，不改变任何服务端身份。
- AnnaWathe 自己用这条链实现了：调试变形显示被模仿者原名（priority 50）、疯魔目标名乱码、尸体信息 HUD 串联。

---

### 2.4 `HudOverlayApi` 通用屏幕 HUD

**包名：** `dev.annawathe.api.client.hud.HudOverlayApi`
**配套：** `HudOverlayContext`、`HudOverlayLayer`、`HudOverlayLayout`

#### 绘制阶段

| 阶段 | 时机 | 适用 |
| --- | --- | --- |
| `HudOverlayLayer.BEFORE_HUD` | Minecraft 主 HUD **之前** | 需要尽早覆盖画面的控制、绑架提示 |
| `HudOverlayLayer.MAIN_HUD` | Wathe 主 HUD **之后** | 普通职业状态文字 |
| `HudOverlayLayer.AFTER_HUD` | 整套 HUD **最后** | 狙击镜等最上层遮罩 |

同一阶段内**所有 renderer 都会执行**。`priority` 越大越晚绘制（越在上层）；同 priority 后注册者更晚绘制。

#### 方法

| 方法 | 说明 |
| --- | --- |
| `register(Identifier id, HudOverlayLayer layer, int priority, HudOverlayRenderer renderer)` | 注册 renderer |
| `registerAliveRole(Identifier id, HudOverlayLayer layer, int priority, Role role, HudOverlayRenderer renderer)` | 注册"仅当本地玩家是该职业且按玩法存活时"显示的 HUD |

`registerAliveRole` 内部使用 `GameFunctions.isPlayerAliveAndSurvival(...)` 生成上下文，因此**自动遵守 `PlayerLifeStateApi` 的 creative/spectator 特殊存活授权**，扩展不需要自己区分普通生存与受授权状态。

#### 上下文能力（`HudOverlayContext`）

字段：`client`、`player`、`textRenderer`、`drawContext`、`tickCounter`、`gameWorld`、`aliveAndSurvival`、`spectatingOrCreative`、`debugHudVisible`、`hudHidden`、`currentScreen`
方法：`width()`、`height()`、`tickDelta()`、`isRunning()`、`isRole(Role)`、`isAliveRole(Role)`、`renderHotbar()`

#### 布局辅助（`HudOverlayLayout`）

提供右下角单行/多行文字与准心附近居中文字的通用坐标换算，例如 `drawBottomRightLine(context, Text, int color)`。**不要在扩展里硬编码坐标。**

#### 示例

```java
// 职业状态（右下角）
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

// 非职业专属（被控制）
HudOverlayApi.register(
        MyMod.id("hud/controlled"),
        HudOverlayLayer.BEFORE_HUD,
        HudOverlayApi.DEFAULT_PRIORITY,
        context -> {
            if (!context.aliveAndSurvival() || !isControlled(context.player())) return;
            context.drawContext().fill(0, 0, context.width(), context.height(), 0xCC000000);
        }
);

// AFTER_HUD 遮罩 + 保留热键栏
HudOverlayApi.register(MyMod.id("hud/scope"), HudOverlayLayer.AFTER_HUD, 1000, context -> {
    if (!context.aliveAndSurvival() || !isScoped()) return;
    drawScope(context.drawContext(), context.width(), context.height());
    context.renderHotbar();
});
```

#### 注意事项

- `AFTER_HUD` 遮罩会盖住先前画出的热键栏。需要保留热键栏时调用 `context.renderHotbar()`，它仍经过原版 Wathe 的热键栏包装并保留 Wathe 纹理。**不要**为了复画热键栏新增 Mixin `InGameHud#renderHotbar`。
- 被控制、被绑架这类状态不一定属于本地玩家职业，应使用普通 `register` 并显式检查 `context.aliveAndSurvival()`。

---

### 2.5 `MoodHudApi` 心情 HUD

**包名：** `dev.annawathe.api.client.mood.MoodHudApi`
**配套：** `MoodHudStyle`、`PsychoMoodHudStyle`、`MoodHudContext`、`MoodHudColors`

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerRoleStyle(Role role, MoodHudStyle style)` | 为职业注册普通心情 HUD 样式 |
| `registerMoodProvider(Identifier id, int priority, MoodStyleProvider provider)` | 注册优先级样式 provider |
| `registerPsychoStyle(Identifier id, int priority, PsychoStyleProvider provider)` | 注册疯魔 HUD 样式 |
| `registerVisibleGameMode(GameMode mode)` | 声明某个游戏模式下也显示 Mood HUD |
| `registerVisibleGameModePredicate(Identifier id, int priority, GameModePredicate predicate)` | 条件式可见性 |
| `resolve(...)` / `resolvePsycho(...)` | 解析最终样式（通常无需自行调用） |

解析顺序：**provider 链（priority 降序、同级后注册优先）→ 角色样式表 → 内置默认样式**。

#### `MoodHudStyle` 可配置项

```java
MoodHudApi.registerRoleStyle(MY_ROLE, MoodHudStyle.builder(MyMod.id("hud/mood_my_role"))
        .arrows(MyMod.id("hud/arrow_up"), MyMod.id("hud/arrow_down"))  // 上/下箭头
        .overlays(ctx -> List.of(MyMod.id("hud/overlay_1")))            // 额外覆盖层
        .icon(ctx -> { /* 自定义图标绘制 */ })
        .barColor(MY_ROLE.color())                                      // 固定色条
        // .hsvMoodBar()                                                // 或：随心情变色的 HSV 条
        // .bar((ctx, width, alpha) -> { ... })                          // 或：完全自定义条
        .barVisibleWhen(ctx -> ctx.moodAlpha() > 0F)
        .hideWarning()                                                  // 隐藏崩溃警告
        .build());
```

默认值：`arrows = false`、`warning = true`、`overlays = 空列表`、`bar = null`（因此 `shouldRenderBar` 必须先有 `bar`）。

#### 颜色工具

```java
// barColor 同时接受 0xRRGGBB 和 Color#getRGB() 的 0xAARRGGBB
// 最终透明度始终由 HUD 淡入淡出状态决定
context.drawContext().fill(0, 0, w, 1, MoodHudColors.withAlpha(myColor, alpha));
```

> **必须使用 `MoodHudColors.withAlpha`** 重写 alpha，**不能**按位 OR —— 因为 `Color#getRGB()` 返回的是带 `0xFF` alpha 的值，按位 OR 会让自定义条永远不透明。

#### 疯魔样式

`PsychoMoodHudStyle` 可替换：完整身体、破损身体、眼睛、跑马文本、文本颜色、倒计时条颜色。

#### 注意事项

- `registerRoleStyle` 只影响显示；`moodType` 是 `NONE` 的职业不会显示并会被清空任务。
- `MoodHudApi.shouldRender(GameMode)` 虽然公开，但 AnnaWathe 内部**没有调用它**；它不会影响默认 Mood HUD 的门禁。

---

### 2.6 `TimeHudApi` 顶部计时

**包名：** `dev.annawathe.api.time.TimeHudApi`
**环境：** 通用（服务端调用不报错，但只影响客户端显示）

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerProvider(Identifier id, int priority, TimeDisplayProvider provider)` | 注册时间显示 provider（**短路链**） |
| `registerDefaultProvider(Identifier id, int priority, TimeDisplayProvider provider)` | 注册"默认兜底"provider（排序时视为最早注册） |
| `resolveDisplay(PlayerEntity viewer)` | 解析最终显示（通常无需自行调用） |

#### 结果工厂

```java
TimeDisplay.pass();                                  // 继续询问低优先级
TimeDisplay.hide();                                  // 隐藏计时，结束链
TimeDisplay.show(int ticks);                          // 动态色显示
TimeDisplay.showCountdown(int ticks, int warning);     // 动态色 + 低时间警告
TimeDisplay.showDynamic(int ticks, int warning, int changeFlashThreshold);
TimeDisplay.showFixedColor(int ticks, int color);      // 固定颜色
```

常量：`NO_LOW_TIME_WARNING = -1`、`DEFAULT_CHANGE_FLASH_THRESHOLD = 10`。

#### 示例

```java
TimeHudApi.registerProvider(MyMod.id("special_countdown"), 100, viewer -> {
    if (!shouldShow(viewer)) return TimeHudApi.TimeDisplay.pass();
    return TimeHudApi.TimeDisplay.showFixedColor(getTicks(viewer), 0xE04B4B);
});
```

#### 注意事项

- 返回 `SHOW` 或 `HIDE` 会**结束解析**，`PASS` 继续。
- 普通扩展使用 priority `0` 也会**排在 Anna 默认回合时间之前**（默认 provider 用 `registerDefaultProvider` 注册，排序时被当作最早）。
- **时间来源 ID 改变时 renderer 会重置滚动数字**，避免不同倒计时之间残留动画。
- 本 API 只负责客户端显示。真实倒计时必须由你自己的服务端组件维护并同步。

---

### 2.7 `BodyInfoApi` / `BodyInfoHudApi` 尸体信息

#### `BodyInfoApi`（common）

**包名：** `dev.annawathe.api.body.BodyInfoApi`

| 方法 | 说明 |
| --- | --- |
| `registerRoleResolver(Identifier id, int priority, RoleResolver resolver)` | 注册"死者职业 ID"解析器 |
| `initializeBody(PlayerBodyEntity body, PlayerEntity victim, Identifier deathReason)` | 初始化尸体信息（尸体生成时由 AnnaWathe 自动调用） |
| `get(PlayerBodyEntity body)` → `BodyInfoSnapshot` | 读取快照 |
| `setDeathReason` / `setRoleId` / `setDeathWorldTime` | 修改字段 |
| `sync(PlayerBodyEntity body)` | 手动同步 |

`BodyInfoSnapshot` 字段：`deathReason`（默认 `wathe:generic`）、`roleId`（可空）、`deathWorldTime`。

#### `BodyInfoHudApi`（client）

**包名：** `dev.annawathe.api.client.gui.BodyInfoHudApi`

| 方法 | 说明 |
| --- | --- |
| `registerRule(Identifier id, int priority, VisibilityRule rule)` | 注册可见性规则 |
| `registerRoleNameProvider(Identifier id, int priority, RoleNameProvider provider)` | 注册身份名文本 |

```java
public record Visibility(Decision decision, boolean deathSummary, boolean roleIdentity) {
    public static Visibility pass();
    public static Visibility hide();
    public static Visibility show(boolean deathSummary, boolean roleIdentity);  // 两个字段独立开关
}
public enum Decision { PASS, SHOW, HIDE }
```

**默认可见性：** 没有规则命中时，`旁观/创造视角 → 显示两行`，否则隐藏。

#### 翻译键与回退

| 内容 | 键 |
| --- | --- |
| 死亡摘要整行 | `hud.annawathe.body.death_info` |
| 身份标签 | `hud.annawathe.body.role_info` |
| 死因 | `death_reason.<命名空间>.<路径>` |

职业名三级回退：注册的 `RoleNameProvider` → Harpy 职业名（装了 HarpyModLoader 时）→ 原版四基础职业用 `announcement.role.<路径>`，扩展职业用 `announcement.role.<命名空间>.<路径>` 且颜色为白色。

#### 注意事项

- `deathSummary` 表示"死亡时间 + 死因"**整行**，`roleIdentity` 表示死者身份；扩展可以只显示其中一项。
- 渲染受 `TargetVisibilityApi.canRenderBody` 与 `RoleNameHudApi.findLookedAtBody` 约束。

---

### 2.8 `PlayerAppearanceApi` / `BodyAppearanceApi` 外观

#### `PlayerAppearanceApi`（client）

**包名：** `dev.annawathe.api.client.appearance.PlayerAppearanceApi`

| 方法 | 说明 |
| --- | --- |
| `registerPlayerSkin(Identifier id, int priority, PlayerSkinHandler handler)` | 玩家皮肤覆盖（短路链） |
| `registerBodySkin(Identifier id, int priority, BodySkinHandler handler)` | 尸体皮肤覆盖（短路链） |
| `resolvePlayerSkin(AbstractClientPlayerEntity player)` | 解析最终玩家皮肤 |
| `resolveBodySkin(PlayerBodyEntity body)` | 解析最终尸体皮肤 |
| `resolveOriginalSkinTextures(UUID uuid, boolean fallback)` | 按 UUID 取原始皮肤 |
| `resolveOriginalPlayerName(UUID uuid)` | 按 UUID 取原始玩家名 |

#### `BodyAppearanceApi`（common）

**包名：** `dev.annawathe.api.appearance.BodyAppearanceApi`

| 方法 | 说明 |
| --- | --- |
| `register(Identifier id, int priority, Handler handler)` | 注册"尸体该用谁的外观"解析器 |
| `resolveAppearanceUuid(PlayerEntity victim, PlayerEntity killer, Identifier reason)` | 解析尸体视觉 UUID |

#### 注意事项

- AnnaWathe 自己用 `PlayerAppearanceApi` 实现了疯魔皮肤（priority `10000`）与调试变形外观。**你的规则 priority 需要高于 10000 才能覆盖疯魔皮肤。**
- `BodyAppearanceApi` 返回的是**尸体视觉 UUID**。真实的 `PlayerBodyEntity` owner UUID **必须保留**给验尸、尸袋、回放和死亡判定，绝对不能改成 appearance UUID。
- 返回的皮肤只影响客户端模型、披风与尸体 renderer，不影响服务端任何判断。

---

### 2.9 `HeldItemInvisibilityApi` / `PsychosisItemApi` 手持物隐藏与幻觉

#### `HeldItemInvisibilityApi`（client）

**包名：** `dev.annawathe.api.client.invisibility.HeldItemInvisibilityApi`

| 方法 | 说明 |
| --- | --- |
| `registerHiddenItem(Role role, Item item)` | 简单形式：某职业的某物品对他人隐藏 |
| `registerHiddenItems(Role role, Collection<Item> items)` | 批量形式 |
| `registerRule(Identifier id, int priority, VisibilityRule rule)` | 完整规则（短路链，返回 `true` 即隐藏） |
| `shouldHideFromOtherLivingPlayers(viewer, holder, hand, stack)` | 查询是否隐藏 |
| `applyInvisibility(viewer, holder, hand[, stack])` | 渲染层入口：隐藏时返回 `ItemStack.EMPTY` |
| `isHiddenByAnyRule(holder, hand[, stack])` | 是否命中任一规则 |
| `hasHiddenHeldItem(PlayerEntity holder)` | 主手或副手是否有隐藏物品 |

上下文：`VisibilityContext(gameWorld, holder, hand, stack, role)`

#### `PsychosisItemApi`（client）

**包名：** `dev.annawathe.api.client.mood.PsychosisItemApi`
低心情幻觉手持物与手臂姿势 provider。规则顺序为 **priority > 0 → 默认值 → priority ≤ 0**。结果提供 `pass()`、`item(...)`、`itemWithPose(...)`、`emptyWithPose(...)`。

#### 注意事项

- 手持物隐藏只影响**其它局内存活玩家**看到的模型；本人 F5 视角、死亡/普通旁观视角和真实服务端物品都不受影响。
- 幻觉物品与 ArmPose **只存在观察者客户端缓存**；死亡、停局、reset、断线必须清空（AnnaWathe 已处理，但扩展不要把它当作持久状态）。

---

### 2.10 `TargetVisibilityApi` 目标可见性

**包名：** `dev.annawathe.api.visibility.TargetVisibilityApi`
**环境：** 通用（客户端用于渲染与选中，服务端用于交互与攻击）

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerPlayerRule(Identifier id, int priority, PlayerRule rule)` | 玩家规则 |
| `registerBodyRule(Identifier id, int priority, BodyRule rule)` | 尸体规则 |
| `canRenderPlayer(viewer, target)` / `canRenderBody(viewer, body)` | 是否渲染 |
| `canTargetPlayer(...)` / `canTargetBody(...)` | 是否可被准心选中 |
| `canInteractWithPlayer(...)` / `canInteractWithBody(...)` | 是否可交互 |
| `canAttackPlayer(...)` / `canAttackBody(...)` | 是否可攻击 |
| `canRenderEntity` / `canTargetEntity` / `canInteractWithEntity` / `canAttackEntity` | 泛型入口（非玩家/尸体实体恒 `true`） |

#### 结果语义

```java
public enum Decision { PASS, ALLOW, DENY }
// 按优先级询问：首个 ALLOW → true，首个 DENY → false，全部 PASS → true（放行）
```

#### 四类 Action 的影响范围

| Action | 影响 |
| --- | --- |
| `RENDER` | 渲染与实体存在性（`isInvisibleTo`、玩家/尸体 renderer 取消） |
| `TARGET` | 客户端准心选中（`canHit`）。**只影响客户端提示** |
| `INTERACT` | 交互（`Entity#interact` / `interactAt`） |
| `ATTACK` | 近战（`PlayerEntity#attack` 包装） |

#### 注意事项

- **AnnaWathe 内部没有注册任何规则**，默认全部放行。这是供扩展使用的骨架。
- `TARGET` 过滤**只影响客户端选中和准心**；`INTERACT` / `ATTACK` 必须在服务端能力入口重新校验。
- `viewer == null` 时直接返回 `true`。

---

## 3. 玩法类 API

### 3.1 `VictoryApi` / `CustomVictory` 胜利仲裁

**包名：** `dev.annawathe.api.win`

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerRule(Identifier id, int priority, VictoryRule rule)` | 注册胜利规则（**短路链**） |
| `evaluate(ServerWorld, GameWorldComponent, GameFunctions.WinStatus)` | 解析（通常无需自行调用） |
| `endGameWithCustomVictory(ServerWorld, CustomVictory)` | 立即以独立胜利结束对局 |
| `endGameWithVanillaWin(ServerWorld, GameFunctions.WinStatus, Collection<UUID> extra)` | 以原版阵营结算并追加共胜者 |

#### 结果语义

```java
public enum Action { PASS, KEEP_RUNNING, VANILLA_WIN, CUSTOM_WIN }

VictoryResult.pass();
//   保持原版结算流程

VictoryResult.keepRunning();
//   阻止本次结算，对局继续。只能在明确的保活条件下使用

VictoryResult.vanillaWin(GameFunctions.WinStatus status, Collection<UUID> extraWinnerUuids);
//   按原版阵营结算，并追加额外赢家

VictoryResult.customWin(CustomVictory victory);
//   独立胜利
```

#### 规则上下文

```java
public record Context(
    ServerWorld world,
    GameWorldComponent gameWorld,
    List<ServerPlayerEntity> alivePlayers,      // 已按 GameFunctions.isPlayerAliveAndSurvival 过滤
    GameFunctions.WinStatus vanillaWinStatus) {}
```

#### `CustomVictory` 数据

```java
public record CustomVictory(
    Identifier id, String announcementTranslationKey, String detailTranslationKey,
    String fallbackTitle, int color, List<UUID> winnerUuids, CustomVictoryGroup winnerGroup)

CustomVictory.of(Identifier id, int color, Collection<? extends PlayerEntity> players);
CustomVictory.builder(Identifier id, int color)
        .announcementTranslationKey(String)   // 默认 announcement.win.<ns>.<path>
        .detailTranslationKey(String)         // 默认 game.win.<ns>.<path>
        .titleTranslationKey(String)          // 默认 announcement.role.<ns>.<path>
        .fallbackTitle(String)                // 默认由 path 生成（下划线分词首字母大写）
        .winners(Collection<UUID>)
        .winnersFromPlayers(Collection<? extends PlayerEntity>)
        .build();
```

#### 示例

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

#### 注意事项

- 赢家保存的是 **UUID**，不保存运行时 `Player` 实例。
- 扩展必须自己补齐翻译：`announcement.win.<ns>.<path>`、`game.win.<ns>.<path>`、`announcement.role.<ns>.<path>`。
- **务必显式调用 `.fallbackTitle(...)`** —— 当前版本的 Builder 没有给 `fallbackTitle` 赋值，缺失翻译键时会渲染出 `null Wins`（详见模组介绍文档"已知问题"章节）。
- AnnaWathe 不内置任何具体职业的胜利规则。

---

### 3.2 `MoodTaskApi` 任务注册与发放

**包名：** `dev.annawathe.api.task.MoodTaskApi`

#### 内置任务 ID 常量

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

#### 注册

| 方法 | 说明 |
| --- | --- |
| `registerTask(MoodTaskDefinition definition)` | 注册任务（同 ID 覆盖） |
| `getDefinition(Identifier id)` / `getDefinitions()` | 查询 |
| `getRandomAssignableDefinitions()` | 随机池内容 |
| `getRegisteredTaskIds()` | 全部已注册 ID |
| `getTranslationKey(Identifier id)` | 翻译键（未知任务回退 `annawathe.task.unknown`） |
| `getTaskPointIds(Identifier id)` | 该任务关联的任务点 |

#### 发放 / 删除 / 完成

| 方法 | 说明 |
| --- | --- |
| `assignTask(ServerPlayerEntity player, Identifier id)` | 发放指定任务 |
| `assignRandomTask(ServerPlayerEntity player)` | 随机发放 1 个 |
| `assignRandomTasks(ServerPlayerEntity player, int count)` | 随机发放 N 个 |
| `fillRandomTaskSlots(ServerPlayerEntity player)` | 填满所有空槽 |
| `removeTask(ServerPlayerEntity player, Identifier id)` | **静默删除** |
| `completeTask(ServerPlayerEntity player, Identifier id, boolean rewardMood)` | **正常完成** |
| `hasTask(PlayerEntity player, Identifier id)` | 是否持有 |
| `getActiveTaskIds(PlayerEntity player)` | 当前任务列表 |

#### 规则

| 方法 | 说明 |
| --- | --- |
| `registerAssignmentRule(Identifier id, int priority, AssignmentRule rule)` | 发放前拦截（返回 `DENY` 即拒绝） |
| `registerCompletionRule(Identifier id, int priority, CompletionRule rule)` | 完成前拦截（返回 `DENY` 即拒绝） |
| `canAssign(AssignmentContext)` / `canComplete(CompletionContext)` | 查询 |

> **注意：这里的 `Decision` 只有 `PASS` 和 `DENY`**，没有"强制放行"。任一规则返回 `DENY` 即整体拒绝。

#### 状态枚举

```java
AssignmentStatus: SUCCESS, PARTIAL_SUCCESS, INVALID_COUNT, GAME_NOT_RUNNING, PLAYER_NOT_ALIVE,
                  MOOD_NOT_SUPPORTED, TASK_LIMIT_REACHED, TASK_NOT_REGISTERED,
                  TASK_ALREADY_ACTIVE, ASSIGNMENT_DENIED, NO_AVAILABLE_TASK
OperationStatus:  SUCCESS, TASK_NOT_ACTIVE, COMPLETION_DENIED
AssignmentSource: INTERNAL_PRIMARY_COOLDOWN, INTERNAL_SLOT_REFILL, EXTERNAL_RANDOM, EXTERNAL_SPECIFIC
```

#### 校验顺序

`count ≤ 0` → `INVALID_COUNT`；对局未运行 → `GAME_NOT_RUNNING`；非玩法存活 → `PLAYER_NOT_ALIVE`；职业不支持心情 → `MOOD_NOT_SUPPORTED`；槽满 → `TASK_LIMIT_REACHED`；已持有 → `TASK_ALREADY_ACTIVE`；未注册 → `TASK_NOT_REGISTERED`；被规则拒绝 → `ASSIGNMENT_DENIED`；池空 → `NO_AVAILABLE_TASK`；实发少于请求 → `PARTIAL_SUCCESS`。

#### `MoodTaskDefinition` 构建

```java
MoodTaskApi.registerTask(MoodTaskDefinition.builder(
        MyMod.id("my_task"),
        "task.mymod.my_task",
        player -> new MyTask(),                  // Factory：新建实例
        (player, nbt) -> new MyTask(nbt)         // NbtReader：从存档恢复
)
        .randomlyAssignable()                    // 进入所有玩家的随机池（默认不进）
        .randomWeight(2.0F)                      // 随机权重（默认 1.0）
        .taskPoints(MoodTaskPointApi.SEAT)       // 关联任务点
        .build());
```

- 扩展任务**默认只能指定发放**，需要进入随机池必须显式 `.randomlyAssignable()`。
- 任务实例实现 `MoodTaskInstance`，NBT 中**只保存实例自己的进度**；稳定任务 ID 由 AnnaWathe 统一写入。
- `.legacyTask(...)` 只供 AnnaWathe 适配原版四个枚举任务，**新扩展不要使用**。
- **不要修改原版 `PlayerMoodComponent.Task` 枚举**。

#### `removeTask` 与 `completeTask` 的语义差别

| | `removeTask` | `completeTask` |
| --- | --- | --- |
| 回复心情 | ✗ | ✓（`rewardMood = true` 时回复 `0.4`） |
| 播放完成动画 | ✗ | ✓ |
| 触发 `AFTER_TASK_COMPLETE` | ✗ | ✓ |
| 结算任务收入 | ✗ | ✓ |
| 计入卡住统计 | ✗ | ✓（其它任务 +1） |

---

### 3.3 `MoodTaskPointApi` 任务点

**包名：** `dev.annawathe.api.task.MoodTaskPointApi`

#### 内置任务点 ID 与颜色

```java
MoodTaskPointApi.BED            // annawathe:bed           0x57D6FF
MoodTaskPointApi.FOOD_TRAY      // annawathe:food_tray     0x61D95C
MoodTaskPointApi.COCKTAIL_TRAY  // annawathe:cocktail_tray 0xFF85A8
MoodTaskPointApi.SEAT           // annawathe:seat          0x7AF4E1
MoodTaskPointApi.KEYED_DOOR     // annawathe:keyed_door    0xFFF79B
```

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerTaskPoint(TaskPointDefinition definition)` | 注册任务点类型 |
| `registerTaskPoint(Identifier id, String translationKey, int color)` | 简化重载 |
| `registerScanHandler(Identifier id, int priority, ScanHandler handler)` | 注册地图扫描扩展 |
| `getDefinition` / `getDefinitions` / `getRegisteredIds` / `isRegistered` | 查询 |
| `getTranslationKey(Identifier id)` / `getColor(Identifier id)` | 翻译键与颜色（未知返回白色） |
| `scanExtraTaskPoints(TaskPointScanContext context)` | 由扫描器调用 |

#### 示例

```java
MoodTaskPointApi.registerTaskPoint(MyMod.id("my_point"), "hud.mymod.my_point", 0x66CCFF);

MoodTaskPointApi.registerScanHandler(MyMod.id("my_point_scan"), 0, context -> {
    if (context.state().isOf(MyBlocks.MY_BLOCK)) context.addTaskPoint(MyMod.id("my_point"));
});
```

#### 注意事项

- 扫描 handler **每次只判断 `context.pos()` 这一格**。AnnaWathe 已把扫描范围限制在"列车重置模板区域平移后 ∩ `playArea`"，**扩展不得再次扫描整个世界**。
- `addTaskPoint(...)` 会丢弃未注册的 ID。
- 客户端透视只对**当前任务关联的任务点**高亮；旁观/创造视角显示全部已注册类型。

---

### 3.4 `TaskCompletionApi` 任务完成事件与收入

**包名：** `dev.annawathe.api.task.TaskCompletionApi`

| 成员 | 说明 |
| --- | --- |
| `AFTER_TASK_COMPLETE` | Fabric 事件：任务完成后触发（**不短路**） |
| `registerTaskIncomeProvider(Identifier id, int priority, TaskIncomeProvider provider)` | 为非杀手任务提供金币收入 |
| `registerTaskIncomeRule(Identifier id, int priority, TaskIncomeRule rule)` | 抑制默认任务收入（返回 `SUPPRESS_DEFAULT_INCOME`） |
| `dispatch(TaskCompletionContext context)` | 由 AnnaWathe 调用 |

```java
public enum TaskIncomeDecision { PASS, SUPPRESS_DEFAULT_INCOME }
```

#### 注意事项

- `registerTaskIncomeRule` 只能**抑制收入**，**不会阻止完成事件**——任务仍然算完成。
- 杀手任务币收益路径保留，但默认常量 `TASK_MONEY_PER_KILLER_TASK = 0`、`TASK_MONEY_PER_KILL = 0`，即默认不发放。

---

### 3.5 `MoodApi` 心情数值

**包名：** `dev.annawathe.api.mood.MoodApi`

| 方法 | 说明 |
| --- | --- |
| `getMood(PlayerEntity)` / `setMood(PlayerEntity, float)` | 读取/设置心情（走原版公共入口） |
| `setDrainMultiplier(PlayerEntity, float)` | 在基础下降值上**乘算**（≥ 0） |
| `getDrainMultiplier(PlayerEntity)` | 查询当前倍率 |
| `protectFromDrain(PlayerEntity, int ticks)` | 暂停下降 N tick（取 `max` 叠加，不会缩短已有保护） |
| `getDrainProtectionTicks(PlayerEntity)` | 查询剩余保护 |
| `clearExternalDrainState(PlayerEntity)` | 重置倍率为 `1.0`、保护为 `0` |
| `isMoodDeathEnabled(PlayerEntity)` / `setMoodDeathEnabled(PlayerEntity, boolean)` | 世界级精神崩溃死亡开关 |
| `MENTAL_BREAKDOWN` | 死因常量 `wathe:mental_breakdown` |

#### 基础数值（固定，不可通过本 API 修改）

| 常量 | 值 |
| --- | --- |
| 基础下降 | `1/4000` 每 tick（约 3 分 20 秒清空） |
| 完成回复 | `0.4` |
| 任务槽阈值 | `0.51` / `0.17` |
| 崩溃警告阈值 | `0.15` |
| 任务上限 | `3` |

---

### 3.6 `ShopApi` / `ShopEntry` / `ShopPrice` 商店

#### `ShopApi`

**包名：** `dev.annawathe.api.shop.ShopApi`

| 方法 | 说明 |
| --- | --- |
| `registerRoleShop(Role role, RoleShopProvider provider)` | 注册职业商店（动态 provider） |
| `registerStaticRoleShop(Role role, Supplier<List<ShopEntry>> supplier)` | 静态列表 |
| `registerStaticRoleShop(Supplier<List<ShopEntry>> supplier, Role... roles)` | 多职业共用 |
| `registerStaticRoleShops(Collection<Role> roles, Supplier<List<ShopEntry>> supplier)` | 批量 |
| `registerShopModifier(Identifier id, int priority, ShopModifier modifier)` | 修改默认商店列表（可以增删改） |
| `getEntriesForPlayer(PlayerEntity)` | 解析某玩家当前可见商品 |
| `hasShop(PlayerEntity)` / `hasRoleShop(Role)` | 是否有商店 |
| `resolveShop(PlayerEntity)` → `ResolvedShop` | 完整解析结果 |
| `defaultPurchase(ShopPurchaseContext)` | 默认购买实现 |
| `getDefaultShopPrice(Item)` / `getDefaultPrice(Item, int fallback)` | 查询 Anna 默认价格 |
| `getDefaultCurrencyPrice(Item, int option, Identifier currency, int fallback)` | 查询指定方案中某货币价格 |
| `sendPurchaseFailedMessage(PlayerEntity)` / `playBuySound(PlayerEntity)` / `playFailSound(PlayerEntity)` | UI 反馈 |

#### `AnnaDefaultShop`

**包名：** `dev.annawathe.api.shop.AnnaDefaultShop`

| 方法 | 说明 |
| --- | --- |
| `entries()` → `List<ShopEntry>` | 返回 AnnaWathe 维护的默认杀手商店列表（每次调用返回新建的不可变列表） |

**这 12 项就是默认商店的全部内容，价格与 `ShopApi.getDefaultShopPrice` 系列查询共用同一份定义。** 源码注释明确写了它**故意不读取**原版 `GameConstants.SHOP_ENTRIES` 的价格，使 Anna 的价格表可以独立调整；商品行为仍调用原版组件已有的服务端能力。

| 槽位 | 商品 | 价格 | 备注 |
| --- | --- | --- | --- |
| WEAPON | 匕首 | 100 | |
| WEAPON | 左轮 | 250 | |
| WEAPON | 手雷 | 300 | |
| WEAPON | 疯魔模式 | 350 | `action` → `PlayerShopComponent::usePsychoMode` |
| POISON | 毒瓶 | 70 | |
| POISON | 蝎子 | 40 | |
| TOOL | 爆竹 | 10 | |
| TOOL | 开锁器 | 50 | |
| TOOL | 撬棍 | 25 | |
| TOOL | 尸袋 | 70 | |
| TOOL | 停电 | 250 | `action` → `PlayerShopComponent::useBlackout` |
| TOOL | 便签 ×4 | 10 | |

> **扩展不应该修改这份列表。** 想调整默认商店请用 `ShopApi.registerShopModifier(...)`，它在解析阶段生效且不影响其它扩展看到的原始定义。

#### `ShopEntry`

**包名：** `dev.annawathe.api.shop.ShopEntry`

```java
public enum Type { WEAPON, POISON, TOOL }     // 对应贴图 wathe:gui/shop_slot_*

// 构造（默认 = 杀手快捷栏限制）
new ShopEntry(ItemStack stack, int price, Type type);
new ShopEntry(ItemStack stack, ShopPrice price, Type type);

// 四种投递方式
ShopEntry.directToHotbar(stack, price, type);     // 跳过杀手判定，直接插快捷栏空格
ShopEntry.giveToInventory(stack, price, type);    // 走完整背包
ShopEntry.action(stack, price, type, player -> ...);
ShopEntry.action(stack, price, type, player -> ..., boolean showFailure);
```

| 投递方式 | 行为 |
| --- | --- |
| **默认**（`action == null`） | 要求 `canUseKillerFeatures(player)` **且** 快捷栏（槽 0–8）有空位，否则失败 |
| `directToHotbar` | 跳过杀手判定，直接插快捷栏空格 |
| `giveToInventory` | `player.giveItemStack(stack.copy())`，走完整背包 |
| `action` | 调用自定义谓词；5 参重载可把 `showFailure` 置 `false` 让失败静默 |

> 默认构造保留原版杀手快捷栏限制。**非杀手商店应显式使用 `directToHotbar`、`giveToInventory` 或 `action`。**

#### `ShopPrice`

**包名：** `dev.annawathe.api.shop.ShopPrice`

```java
ShopPrice.money(100);                                  // 纯金币
ShopPrice.allOf(CurrencyAmount.money(100), CurrencyAmount.of(BLOOD, 2));   // AND
ShopPrice.anyOf(                                       // OR
        ShopPrice.option(CurrencyAmount.money(200)),
        ShopPrice.option(CurrencyAmount.of(BLOOD, 4)));
```

| 方法 | 说明 |
| --- | --- |
| `canAfford(PlayerEntity)` | 是否买得起 |
| `selectPayment(PlayerEntity)` → `ShopPayment` | 选择支付方案（**可支付方案中货币数量总和最小**；总和相同取定义顺序靠前者） |
| `legacyPrice()` | 首个方案中的金币数额，否则首方案总额 |
| `displayLines()` | 客户端价格多行文本，OR 之间插入 `shop.price.or`，空成本显示 `shop.price.free` |

**option 之间是 OR，同一 option 内是 AND。** option 构造时会过滤 `null` 与 `amount ≤ 0` 项。

#### 示例

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

#### 购买流程与职责边界

| 阶段 | 责任方 |
| --- | --- |
| 客户端点击 | 只发送**商品索引**（`annawathe:store_buy`） |
| 服务端校验 | 重解析商店列表 → 对局运行 + 玩法存活 → 索引范围 → 支付方案 + 余额 → 物品冷却 |
| 交付 | **扩展的 `purchase` 回调只负责交付商品** |
| 扣款 | AnnaWathe 在交付成功后原子扣除全部货币 |

> **扩展购买回调禁止自行扣款**，也不能把客户端余额当作合法性判断。

---

### 3.7 `EconomyApi` / `PlayerEconomyApi` 经济

#### `EconomyApi`

**包名：** `dev.annawathe.api.economy.EconomyApi`

| 方法 | 说明 |
| --- | --- |
| `registerCurrency(Identifier id, String icon, String translationKey, CurrencyHudPredicate predicate)` | 注册货币 |
| `getCurrency(Identifier)` / `getCurrencyOrFallback(Identifier)` | 查询 |
| `currencySnapshot()` | 全部已注册货币 |
| `formatCurrencyAmount(CurrencyAmount, boolean icon)` | 格式化为文本 |
| `registerBalanceHudRole(Role)` / `registerBalanceHudRoles(Collection<Role>)` | 哪些职业显示余额 HUD |
| `registerBalanceHudPredicate(Identifier id, int priority, BalanceHudPredicate)` | 条件式余额 HUD |
| `shouldRenderBalanceHud(PlayerEntity)` | 是否显示余额 |
| `getVisibleCurrencyBalances(PlayerEntity, boolean shopFallback)` | 可见余额列表 |
| `registerPassiveIncomeRole(Role)` / `registerPassiveIncomeRoles(Collection<Role>)` | 哪些职业有被动收入 |
| `registerPassiveIncomeRule(Identifier id, int priority, PassiveIncomeRule)` | 收入资格规则（`ALLOW` / `DENY` / `PASS`，首个非 PASS 即决） |
| `registerPassiveIncomeModifier(Identifier id, int priority, PassiveIncomeModifier)` | 收入数值修改器（链式，每步 `max(0, ·)`） |
| `canReceivePassiveIncome(...)` / `calculatePassiveIncome(..., int base)` | 查询 |

**常量：** `MONEY = wathe:money`、`TASK_MONEY = wathe:task_money`、`MONEY_ICON = "\uE781"`、`TASK_MONEY_ICON = "\uE782"`、`TASK_MONEY_PER_KILLER_TASK = 0`、`TASK_MONEY_PER_KILL = 0`。

**被动收入判定顺序：** 收入规则 → `PASSIVE_INCOME_ROLES` → `canUseKillerFeatures`。

#### `PlayerEconomyApi`

**包名：** `dev.annawathe.api.economy.PlayerEconomyApi`

| 方法 | 说明 |
| --- | --- |
| `get(PlayerEntity, Identifier currency)` | 读取余额 |
| `set(PlayerEntity, Identifier currency, int amount)` | 设置余额 |
| `add(PlayerEntity, Identifier currency, int amount)` | 增减余额 |
| `spend(PlayerEntity, List<CurrencyAmount> costs)` | 原子消费（先全量校验、再一次扣、只同步一次） |
| `snapshot(PlayerEntity)` | 全部余额快照 |

#### 示例

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

#### 注意事项

- 余额保存在原版 `wathe:shop` CCA 中：金币用原版 `balance` 字段，其它货币写入 `CurrencyBalances` NBT，跟随原版同步与 `reset()`。
- **扩展不需要自行同步余额**，也不需要发包。

---

### 3.8 `PsychoModeApi` / `PsychoModeProfile` 疯魔模式

#### 常量

```java
PsychoModeApi.DEFAULT_PROFILE_ID = wathe:psycho_mode
PsychoModeApi.DEFAULT_MODE_NAME_TRANSLATION_KEY  = "psycho_mode.wathe.default"
PsychoModeApi.DEFAULT_SHIELD_NAME_TRANSLATION_KEY = "psycho_shield.wathe.default"
```

#### 状态控制

| 方法 | 说明 |
| --- | --- |
| `start(PlayerEntity)` / `start(PlayerEntity, Identifier profileId)` / `start(PlayerEntity, PsychoModeProfile)` | 启动（返回 `false` 表示快捷栏空间不足等前置条件不满足） |
| `stop(PlayerEntity)` / `stop(PlayerEntity, boolean recordReplay)` | 停止 |
| `isActive(PlayerEntity)` / `isActive(PlayerEntity, Identifier profileId)` | 是否激活 |
| `getActiveProfile` / `getRemainingTicks` / `getArmour` / `getMaxTicks` / `getInitialArmour` | 状态查询 |

#### 注册

| 方法 | 说明 |
| --- | --- |
| `registerProfile(PsychoModeProfile profile)` | 注册 profile |
| `getProfile(Identifier)` / `getProfileOrDefault(Identifier)` / `createDefaultProfile()` | 查询 |
| `registerShieldRule(Identifier id, int priority, PsychoShieldRule handler)` | 护盾 / 穿盾规则 |
| `registerStartProfileProvider(Identifier id, int priority, StartProfileProvider provider)` | 动态决定启动哪个 profile |

#### 查询与工具

| 方法 | 说明 |
| --- | --- |
| `isLockedItem` / `shouldPreventDrop` / `findLockedHotbarSlot` | 锁栏查询（`findLockedHotbarSlot` 扫描 0–8 返回首个锁定格） |
| `isMeleeKillWeapon` / `getMeleeHitSound` | 近战击杀查询 |
| `shouldPlayBackgroundSound(World, SoundEvent)` | 背景音播放条件 |
| `markGrantedItem(profile, stack)` / `isGrantedForProfile(stack, profileId)` | 授予物品标记（数据组件 `annawathe:psycho_granted_profile`） |
| `resolveShield(PsychoShieldContext)` | 护盾仲裁（**扩展可直接调用以复用同一套规则**） |
| `putModeReplayData` / `createModeReplayData` / `resolveModeNameTranslationKey` / `resolveShieldNameTranslationKey` | 回放数据 |

#### 护盾规则接口

```java
@FunctionalInterface
public interface PsychoShieldRule { PsychoShieldResult resolve(PsychoShieldContext context); }

public enum PsychoShieldResult { PASS, BLOCK, BYPASS }

// 服务端一次"可能击中疯魔玩家"的只读上下文
public record PsychoShieldContext(
        @NotNull PlayerEntity victim,
        @Nullable PlayerEntity killer,
        @NotNull Identifier deathReason,
        @NotNull PlayerPsychoComponent component,
        @NotNull PsychoModeProfile profile,
        @NotNull NbtCompound replayData) {}
```

#### 护盾优先级

规则按 `priority` 降序、同优先级注册序降序排列，取**第一个返回非 PASS** 的结果：

- `PsychoShieldResult.BLOCK` → 消耗一点护盾并取消死亡
- `PsychoShieldResult.BYPASS` → 穿透护盾
- 全部 `PASS` 或返回 `null` → 回退为 `armour > 0 ? BLOCK : PASS`

#### `PsychoModeProfile` 全部可配置字段

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
        .backgroundSound(soundEvent, true)           // 声音 + 是否播放
        .visualSettings(PsychoVisualSettings.skin(wideTex, slimTex, true))
        .lockedItemPredicate(stack -> ...)           // 额外锁定谓词
        .meleeWeaponPredicate(stack -> ...)          // 额外近战武器谓词
        .build();

PsychoModeProfile.copyOf(existingProfile, MyMod.id("copy"));   // 整份复制
```

`PsychoVisualSettings.none()` 或 `PsychoVisualSettings.skin(wide, slim, hideFeatures)`。

#### 注意事项

- 临时授予物品必须由 profile 的 `grantedItems` 提供；AnnaWathe 会自动写入并在结束时**精确回收**标记物品，扩展不要自己手动给物品。
- **启动前置条件：授予物品数量 ≤ 快捷栏空格数**，否则 `start` 返回 `false`。
- 锁栏有三重防护：滚轮回滚、数字键拦截、服务端丢弃非法换栏包。

---

### 3.9 `PlayerLifeStateApi` 玩法存活

**包名：** `dev.annawathe.api.PlayerLifeStateApi`

| 方法 | 说明 |
| --- | --- |
| `hasAliveOverride(PlayerEntity)` | 是否持有特殊存活授权 |
| `setAliveInCurrentGameMode(ServerPlayerEntity, boolean)` | 直接设置 |
| `clearAliveOverride(PlayerEntity)` | 清除 |
| `changeGameModeAsGameplayAlive(ServerPlayerEntity, GameMode)` | **推荐入口**：切到 creative/spectator 时授予授权，切回生存时清除 |
| `isGameplayAliveGameModeChangeAllowed(ServerPlayerEntity)` | 判断某次切换是否为合法授权（供撤销钩子识别） |
| `isNonSurvivalMode(GameMode)` | 是否为 creative/spectator |

#### 语义

授权后，以下三个判断被覆盖：

| 原版判断 | 覆盖结果 |
| --- | --- |
| `isPlayerAliveAndSurvival` | `true` |
| `isPlayerSpectatingOrCreative` | `false` |
| `isPlayerEliminated` | `false` |

#### 注意事项

- **只改变 Anna/Wathe 的玩法存活判断**，不改变原版权限或物品消耗规则。
- 授权仅对 `CREATIVE` / `SPECTATOR` 有意义；普通 `/gamemode` 会**撤销**授权。
- 组件的重生策略是 `NEVER_COPY`：死亡、重生都不复制。
- 无本局职业的玩家不能获得特殊存活授权。

---

### 3.10 `PlayerMovementApi` 移动速度

**包名：** `dev.annawathe.api.movement.PlayerMovementApi`

| 方法 | 说明 |
| --- | --- |
| `registerSpeedModifier(Identifier id, int priority, MovementSpeedModifier modifier)` | 注册速度修正规则 |
| `resolveMovementSpeed(PlayerEntity, float vanillaSpeed, float baseSpeed)` | 解析 |
| `canSelfMove(PlayerEntity)` | **恒返回 `true`**（预留接口，当前无调用方） |
| `canJump(PlayerEntity)` | **恒返回 `true`**（预留接口，当前无调用方） |

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

**链的计算方式：** 起点 `current = max(0, baseSpeed)`，逐条应用 `ADD`(+)、`MULTIPLY`(*)、`OVERRIDE`(=)；`PASS` 或结果非有限值时跳过；结束再 `max(0, current)`。

**基础速度：** 疾跑 `0.10F`，平常 `0.07F`（仅在玩家按玩法存活时生效）。

> **本 API 不包含体力系统。** 自改 Wathe 的体力消耗、心情体力惩罚与跳跃限制**没有**迁移到 AnnaWathe。如果将来增加体力，必须单独增加组件、同步与生命周期清理，不要把体力字段塞进速度 API。

---

### 3.11 `PlayerCollisionApi` 玩家碰撞

**包名：** `dev.annawathe.api.collision.PlayerCollisionApi`

| 方法 | 说明 |
| --- | --- |
| `registerRule(Identifier id, int priority, Rule rule)` | 注册碰撞规则（短路链） |
| `resolve(PlayerEntity self, PlayerEntity other)` | 解析最终模式 |
| `blocksMovement(self, other)` / `suppressesPush(self, other)` | 查询 |

```java
public enum PlayerCollisionMode {
    PASS(false, true),          // 交给后续规则/原版
    SOLID(true, true),          // 挡路 + 保留原版推挤
    VANILLA_PUSH(false, true),  // 不挡路 + 保留原版推挤
    NO_COLLISION(false, false); // 不挡路 + 不推挤
}

public record PlayerCollisionContext(...) {}
```

#### 默认解析顺序

1. 跨世界 → `PASS`；
2. 规则链取首个非 `PASS`；
3. 全部 `PASS` 后使用默认规则：若 `!game.isRunning()`、或总开关关闭、或任一方非玩法存活 → `PASS`；否则 `isStartDelayActive() ? VANILLA_PUSH : SOLID`。

`suppressesPush` 必须**双向**都不允许推挤才返回 `true`。

#### 开局免碰撞

世界组件 `AnnaCollisionSettings`：`enabled`（默认 `true`）、`startDelaySeconds`（默认 **30**）、`roundStartTick`。

> **起点记录在真正进入 `ACTIVE` 之后**，不是按执行 `/start` 的时间计算。

#### 注意事项

- 涉及底层 Mixin：`Entity#collidesWith`、`Entity#pushAwayFrom`、`EntityView#getEntityCollisions`、`LivingEntity#pushAway` 都已统一询问本 API。**扩展职业不要重复注入这些底层入口。**
- 尸体（`PlayerBodyEntity`）被排除在移动碰撞之外。

---

### 3.12 `PlayerTransformApi` 调试变形

**包名：** `dev.annawathe.api.appearance.PlayerTransformApi`

| 方法 | 说明 |
| --- | --- |
| `setAppearance(PlayerEntity target, UUID appearanceUuid, int seconds)` | 限时变形（`seconds < 0` 表示永久） |
| `setPermanentAppearance(PlayerEntity target, UUID appearanceUuid)` | 永久变形 |
| `clearAppearance(PlayerEntity target)` | 清除 |
| `clearAll()` | 清除全部在线玩家 |
| `isActive(PlayerEntity target)` | 是否处于变形状态 |
| `getTargetUuid(PlayerEntity target)` | 当前外观 UUID |
| `getRemainingSeconds(PlayerEntity target)` | 剩余秒数（永久返回 `-1`） |

#### 注意事项

- **只改客户端显示的目标 UUID**，不改变职业、阵营、声音、手持物、碰撞或服务端身份。
- 优先级**低于**扩展职业伪装、幻觉视角等更高优先级的视觉规则。
- 永久状态跨死亡、回合和重启保存，直到显式清除。本 API 是**管理员调试门面**。

---

## 4. 客户端交互类 API

### 4.1 `ItemTooltipApi` 物品 Tooltip

**包名：** `dev.annawathe.api.client.tooltip.ItemTooltipApi`
**环境：** 仅客户端

| 方法 | 说明 |
| --- | --- |
| `initialize()` | 由 AnnaWathe 客户端入口调用，扩展无需调用 |
| `registerItem(Item item)` | 注册标准描述 |
| `registerItems(Item... items)` | 批量注册 |
| `registerAppender(Identifier id, int priority, Item item, TooltipAppender app)` | 追加动态文本 |
| `getRemainingCooldownTicks(PlayerEntity player, Item item)` | 读取真实剩余冷却 tick |
| `formatCooldownTicks(int ticks)` | 格式化冷却文本 |
| `COOLDOWN_COLOR` | 冷却文本颜色常量 |

#### 示例

```java
ItemTooltipApi.registerItem(MY_ITEM);
ItemTooltipApi.registerAppender(MyMod.id("item_state"), 10, MY_ITEM, context -> {
    context.tooltip().add(Text.literal("额外状态"));
});
```

#### 关键实现细节

- 标准 Tooltip 读取 **`<物品翻译键>.tooltip`**，例如 `item.wathe.knife.tooltip`。
- 冷却**直接读取当前 `ItemCooldownManager` 条目的 `endTick - tick`**，**不使用固定总冷却或进度反推** —— 所以加速、减冷却等改动都能正确显示。
- 冷却读取刻意**不用反射**（用两个 Mixin `@Accessor`），避免生产环境重映射后字段名变化导致倒计时静默为 0。

#### 注意事项

- **Tooltip 只是显示层**，不能作为服务端合法性判断。
- AnnaWathe 已**禁用原版 Wathe 的 Tooltip callback**，**不要**为同类物品重复注册全局 `ItemTooltipCallback`。

---

### 4.2 `InventoryButtonApi` 背包按钮

**包名：** `dev.annawathe.api.client.inventory.InventoryButtonApi`
**环境：** 仅客户端。支持 `LIMITED`（限制背包）、`VANILLA`（普通背包）、`CREATIVE`（创造背包）三类屏幕。

#### 方法

| 方法 | 说明 |
| --- | --- |
| `registerProvider(Identifier id, int priority, InventoryButtonProvider provider)` | 注册 provider |
| `initializeScreen(...)` / `tickScreen(...)` / `renderScreen(...)` / `closeScreen(...)` | 由 AnnaWathe 调度，扩展无需调用 |
| `allowInventoryKeyClose(Screen, int keyCode, int scanCode)` | 由调度器询问 |
| `reset()` | 由 AnnaWathe 在断线时调用 |

#### `InventoryButtonExtension` 生命周期

每次打开屏幕都会创建一个**新的 extension 实例**，可以安全保存当前屏幕的临时状态。API 统一调度：

```java
public interface InventoryButtonExtension {
    default void init(InventoryButtonContext context) {}
    default void tick(InventoryButtonContext context) {}
    default void render(InventoryButtonContext context, DrawContext draw, int mouseX, int mouseY, float delta) {}
    default boolean allowInventoryKeyClose(InventoryButtonContext context, int keyCode, int scanCode) { return true; }
    default void close(InventoryButtonContext context) {}
}
```

#### 示例

```java
InventoryButtonApi.registerProvider(MyMod.id("guide"), 0, context -> {
    if (context.type() == InventoryScreenType.LIMITED) return null;   // 该屏幕不挂载
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

#### 动态控件与分页

| 方法 | 说明 |
| --- | --- |
| `addWidget(Identifier group, ClickableWidget widget)` | 加入分组 |
| `replaceGroup(Identifier group, List<ClickableWidget>)` | 替换整组 |
| `clearGroup(Identifier group)` | 清空整组（隐藏、禁用、解除焦点） |
| `setGroupVisible(Identifier group, boolean visible)` | 整组显隐 |

`InventoryPageState` 按 `Identifier` 隔离页码，并在**断线和换局时清理**。

#### 注意事项

- 动态控件必须使用**独立 group ID** 管理；关闭时旧控件会被隐藏、禁用并解除焦点。
- `allowInventoryKeyClose` 只控制当前屏幕的按键行为，**不能替代服务端交互校验**。

---

### 4.3 `PsychoModeClientApi` 疯魔客户端视觉

**包名：** `dev.annawathe.api.client.psycho.PsychoModeClientApi`
**环境：** 仅客户端

| 方法 | 说明 |
| --- | --- |
| `registerDefaultClientHandlers()` | 由 AnnaWathe 客户端入口调用，扩展无需调用 |
| `registerVisualProvider(Identifier id, int priority, VisualProvider provider)` | 注册视觉 provider（短路链，首个非 null 生效） |
| `registerBackgroundAmbience(SoundEvent sound, int intervalTicks)` | 注册背景环境音（同 sound id 去重） |
| `resolveVisualSettings(AbstractClientPlayerEntity)` | 解析最终视觉设置 |
| `resolveSkinTextures(AbstractClientPlayerEntity)` | 解析疯魔皮肤 |
| `shouldHideFeatures(AbstractClientPlayerEntity)` | 是否隐藏普通模型特征 |

**常量：** `DEFAULT_VISUAL_PRIORITY = 0`、`PLAYER_APPEARANCE_PRIORITY = 10_000`（默认疯魔皮肤注册所用的优先级）。

#### 注意事项

- 视觉 provider 只影响**客户端皮肤、模型特征和背景音**，**不能替代服务端合法性判断**。
- 默认疯魔皮肤以 priority `10000` 注册到 `PlayerAppearanceApi`；要覆盖它需要更高的 priority。
- 背景音播放条件是"世界中至少存在一个激活 profile 且声明了该声音"。

---

### 4.4 补充与辅助类型

这些类型在 API 包中公开，但属于"配合使用"或"只读查询"性质，一并列出以保证参考完整。

#### `PsychoDataComponentTypes` 数据组件

**包名：** `dev.annawathe.api.psycho.PsychoDataComponentTypes`
**环境：** 通用

| 成员 | 说明 |
| --- | --- |
| `PSYCHO_GRANTED_PROFILE` | `ComponentType<String>`，注册 id 为 **`annawathe:psycho_granted_profile`** |
| `init()` | 在 common 初始化阶段主动触发本类加载（AnnaWathe 已调用，扩展无需调用） |

**作用：** 给"临时疯魔物品"打上 profile 标记，使结束时**只回收 Anna 自己授予的物品**，不会误删玩家原有的同类物品。

> **注册时机极其重要：** 数据组件必须在 Minecraft registry **冻结之前**注册。AnnaWathe 在 `onInitialize` 里第一件事就是调用 `PsychoDataComponentTypes.init()`，源码注释明确写了**不能等玩家第一次购买疯魔时再懒加载**。扩展如果需要自己的数据组件，也必须遵循同样的时机要求。

#### `ShopPayment`

**包名：** `dev.annawathe.api.shop.ShopPayment`

```java
public record ShopPayment(int optionIndex, @NotNull List<CurrencyAmount> costs) {
    public static ShopPayment of(int optionIndex, List<CurrencyAmount> costs);
    public static ShopPayment money(int amount);
    public int totalAmount();
    public NbtList toNbtList();
}
```

`ShopPrice.selectPayment(player)` / `cheapestPaymentForDevelopment()` 的返回值。`optionIndex` 是命中方案在价格定义中的下标；`costs` 是不可变副本。

#### 其余上下文与枚举（只读查询用）

| 类型 | 包名 | 用途 |
| --- | --- | --- |
| `BodyInfoSnapshot` | `api.body` | 尸体快照：`deathReason`、`roleId`（可空）、`deathWorldTime` |
| `PlayerCollisionContext` | `api.collision` | 碰撞规则的只读上下文（`self`、`other`、`world` 等） |
| `CurrencyDefinition` | `api.economy` | 货币定义：`id`、`icon`、`translationKey`、`order`、`hudPredicate` |
| `PsychoItemPredicate` | `api.psycho` | 疯魔物品谓词接口（`lockedItemPredicate` / `meleeWeaponPredicate` 使用） |
| `PsychoShieldContext` / `PsychoShieldResult` | `api.psycho` | 护盾规则上下文与结果（见 §3.8） |
| `ShopContext` | `api.shop` | 商店解析上下文（`player`、`gameWorld`、`role`、`roleSpecificShop`） |
| `ShopModifier` / `RoleShopProvider` | `api.shop` | 商店修改器与职业商店 provider 的函数式接口 |
| `ShopPurchaseContext` / `ShopPurchaseResult` | `api.shop` | 购买上下文与购买结果枚举 |
| `MoodTaskDefinition` | `api.task` | 任务定义（用 `MoodTaskDefinition.builder(...)` 构建，见 §3.2） |
| `MoodTaskInstance` | `api.task` | 任务运行时实例接口（扩展实现它） |
| `TaskPointDefinition` / `TaskPointScanContext` | `api.task` | 任务点定义与扫描上下文（见 §3.3） |
| `CustomVictoryGroup` | `api.win` | 独立胜利分组（`titleTranslationKey`、`fallbackTitle`、`color`、`playerUuids`） |
| `HudOverlayContext` / `HudOverlayLayer` / `HudOverlayLayout` | `api.client.hud` | 通用 HUD 的上下文、阶段枚举与布局辅助（见 §2.4） |
| `MoodHudContext` / `MoodHudStyle` / `PsychoMoodHudStyle` / `MoodHudColors` | `api.client.mood` | 心情 HUD 的上下文、样式与颜色工具（见 §2.5） |
| `InventoryButtonContext` / `InventoryButtonExtension` / `InventoryButtonProvider` / `InventoryButtonLayout` / `InventoryPageState` / `InventoryPageSwitchWidget` / `InventoryScreenType` | `api.client.inventory` | 背包按钮的上下文、生命周期接口、布局、分页状态与屏幕类型（见 §4.2） |

#### 内部访问接口（扩展不应使用）

| 类型 | 包名 | 说明 |
| --- | --- | --- |
| `PlayerEconomyAccess` | `api.economy` | 由 `PlayerShopComponentEconomyMixin` 在 `wathe:shop` 组件上实现的**内部桥接接口**，用于读写非金币余额。请始终通过 `PlayerEconomyApi` 访问余额，不要直接引用它 |

---

## 5. 内部设施速查

**以下内容仅供理解实现，扩展不应直接依赖。**

### 5.1 CCA 组件

| 组件 | CCA id | 对象 |
| --- | --- | --- |
| `PlayerInstinctComponent` | `annawathe:instinct` | Player（CHARACTER 复制） |
| `AnnaRoundEndState` | `annawathe:round_state` | World + Scoreboard |
| `AnnaMoodSettings` | `annawathe:mood_settings` | World |
| `AnnaTaskPointWorldState` | `annawathe:task_points` | World |
| `PlayerLifeStateComponent` | `annawathe:life_state` | Player（NEVER_COPY） |
| `PlayerAppearanceOverrideComponent` | `annawathe:appearance_override` | Player（CHARACTER 复制） |
| `AnnaCollisionSettings` | `annawathe:collision_settings` | World |
| `AnnaBodyInfoComponent` | `annawathe:body_info` | PlayerBodyEntity（NEVER_COPY） |

### 5.2 网络包

| 包 | 方向 | 内容 |
| --- | --- | --- |
| `annawathe:task_point_sync` | S2C | 整张任务点表快照 |
| `annawathe:store_buy` | C2S | 仅商品索引 |

### 5.3 数据组件

| 数据组件 | id | 类型 | 用途 |
| --- | --- | --- | --- |
| `PSYCHO_GRANTED_PROFILE` | `annawathe:psycho_granted_profile` | `ComponentType<String>` | 标记 Anna 授予的临时疯魔物品，结束时精确回收（见 §4.4） |

### 5.4 内部包

| 包 | 内容 |
| --- | --- |
| `dev.annawathe.bridge` | 内部桥接：`PsychoComponentBridge`、`MoodTaskBridge`、`PlayerBodyAppearanceBridge` |
| `dev.annawathe.compat.wathe` | Wathe 版本差异兼容（反射查找 `TaskCompletePayload`） |
| `dev.annawathe.mixin.compat` | HarpyModLoader 软兼容（仅在检测到该 Mod 时加载） |
| `dev.annawathe.mood` | 心情状态机与内置任务实现 |
| `dev.annawathe.task` | 任务点扫描与同步调度 |
| `dev.annawathe.network` | 两个自定义 payload |

---

## 6. 迁移清单

把旧的"深层 Mixin 式扩展"迁移到 AnnaWathe API 的步骤：

1. **确认基础版本**：你的扩展使用的是**原版 Wathe**，不是自改 Wathe。
2. **本能**：把 `WatheClient` 的本能 Mixin 改成 `InstinctApi.registerAvailability` / `registerHighlight`。
3. **胜利**：把 `MurderGameMode` 的胜利 Mixin 改成 `VictoryApi.registerRule`。
4. **Tooltip**：把自定义 Tooltip callback 改成 `ItemTooltipApi`。
5. **任务**：不要给原版 `PlayerMoodComponent.Task` 枚举加值，改用 `MoodTaskApi.registerTask`。
6. **Mood HUD**：不要 Mixin `MoodRenderer`，改用 `MoodHudApi`。
7. **准心 / 准心名字 / 通用 HUD**：不要 Mixin `CrosshairRenderer` / `RoleNameRenderer` / `InGameHud`，改用对应 API。
8. **时间 HUD / 背包按钮**：不要 Mixin `TimeRenderer`，改用 `TimeHudApi`；背包按钮改用 `InventoryButtonApi`。
9. **碰撞 / 存活**：不要注入 `Entity#collidesWith` 等底层入口，改用 `PlayerCollisionApi` / `PlayerLifeStateApi`。
10. **删除**已被 AnnaWathe 覆盖的旧 Mixin json 条目。
11. **重新构建**扩展，并用"**原版 Wathe + AnnaWathe + 你的扩展**"三方组合测试。

---

## 附：常见问题

**Q：为什么我的规则没有生效？**
A：按顺序检查——① 是否在正确的初始化时机注册（商店必须在 common）；② `priority` 是否被别人覆盖（同 ID 会互相替换）；③ 是否被更高 priority 的规则短路了；④ 客户端 API 是否注册在客户端入口。

**Q：两个扩展注册了相同 ID 怎么办？**
A：后注册者**覆盖**前者。请始终使用自己 Mod 的命名空间作为 ID 前缀，例如 `MyMod.id("hud/xxx")`。

**Q：我能在服务端调用客户端 API 吗？**
A：不能。`dev.annawathe.api.client.*` 的类在服务端不存在，会直接崩溃。请把调用点放在客户端入口或客户端专用代码中。

**Q：为什么 `removeTask` 没给我回心情？**
A：这是设计行为。`removeTask` 是静默删除，只有 `completeTask` 才走正常完成流程（回心情、播动画、触发事件与收入）。

**Q：疯魔模式给的物品被我自己回收了会怎样？**
A：不要手动管理。`grantedItems` 由 AnnaWathe 自动写入标记并在结束时精确回收；手动处理会导致物品残留或误删。

---

*本文档基于 `annawathe` 工程源码整理。英文版见 [`02-API-Reference-English.md`](02-API-Reference-English.md)。*
