# AnnaWathe 屏幕 HUD 与准心 API

本文说明 AnnaWathe 提供的通用屏幕 HUD 和准心图标客户端接口。扩展只注册 renderer，
不要再 Mixin 原版 Wathe 的 `InGameHud` 或 `CrosshairRenderer`。

这两套 API 都只决定客户端显示。服务端物品、攻击和 C2S 接收器仍需重新校验职业、玩法存活、
冷却、距离、目标可见性和技能状态。

## API 选择

| 需求 | API |
| --- | --- |
| 右下角职业状态、全屏黑幕、狙击镜 | `HudOverlayApi` |
| 3x3 准心、锁定高亮、准心下方小进度图标 | `CrosshairHudApi` |
| 准心下玩家名、实体文字、同伙提示 | `RoleNameHudApi` |
| Mood、疯魔 HUD 样式 | `MoodHudApi` |
| 顶部回合时间 | `TimeHudApi` |

## HudOverlayApi

包名：`dev.annawathe.api.client.hud`。

### 绘制阶段

- `BEFORE_HUD`：在 Minecraft 主 HUD 前绘制，适合需要尽早覆盖画面的控制或绑架提示。
- `MAIN_HUD`：在 Wathe 主 HUD 后绘制，适合普通职业状态文字。
- `AFTER_HUD`：在整套 HUD 最后绘制，适合狙击镜等最上层遮罩。

同一阶段内所有 renderer 都会执行。priority 从小到大绘制，同 priority 按注册先后绘制，
因此更大的 priority 和更晚注册的 renderer 会盖在上面。同一阶段重复注册相同 ID 会替换旧注册。

### 存活职业 HUD

```java
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
```

`registerAliveRole` 同时检查本地玩家的职业和
`GameFunctions.isPlayerAliveAndSurvival(...)`。它会自然遵守 AnnaWathe 的特殊玩法存活授权，
扩展不需要自行区分普通生存与受授权的 creative/spectator。

### 非职业专属 HUD

被控制、被绑架等状态不一定属于本地玩家职业，应使用普通 `register` 并明确检查存活：

```java
HudOverlayApi.register(
        MyMod.id("hud/controlled"),
        HudOverlayLayer.BEFORE_HUD,
        HudOverlayApi.DEFAULT_PRIORITY,
        context -> {
            if (!context.aliveAndSurvival() || !isControlled(context.player())) {
                return;
            }
            context.drawContext().fill(0, 0, context.width(), context.height(), 0xCC000000);
        }
);
```

### AFTER_HUD 与热键栏复画

AFTER_HUD 遮罩会覆盖之前画出的热键栏。需要保留热键栏时使用受控入口：

```java
HudOverlayApi.register(MyMod.id("hud/scope"), HudOverlayLayer.AFTER_HUD, 1000, context -> {
    if (!context.aliveAndSurvival() || !isScoped()) {
        return;
    }
    drawScope(context.drawContext(), context.width(), context.height());
    context.renderHotbar();
});
```

`renderHotbar()` 仍经过原版 Wathe 的热键栏包装，会保留 Wathe 纹理。不要为了复画热键栏新增 Mixin。

### HudOverlayContext

上下文提供 `client`、`player`、`textRenderer`、`drawContext`、`tickCounter`、`gameWorld`、
`aliveAndSurvival`、`spectatingOrCreative`、`debugHudVisible`、`hudHidden`、`currentScreen`，
以及 `width()`、`height()`、`tickDelta()`、`isRunning()`、`isRole(...)`、`isAliveRole(...)`
和 `renderHotbar()`。

`HudOverlayLayout` 提供右下角单行/多行文字和准心附近居中文字的通用坐标换算。

## CrosshairHudApi

包名：`dev.annawathe.api.client.gui.CrosshairHudApi`。

准心 API 只在原版 Wathe 准备绘制第一人称准心时调度。第三人称不会调用 provider 或 overlay。

### Provider

Provider 用于替换默认准心，是高 priority 优先的短路链。同 priority 下后注册者先执行：

```java
CrosshairHudApi.registerProvider(MyMod.id("crosshair/my_weapon"), 100, context -> {
    if (!context.mainHandStack().isOf(MY_WEAPON)) {
        return CrosshairHudApi.Result.PASS;
    }

    boolean target = findValidClientTarget(context.player()) != null;
    float progress = getClientProgress(context.player(), context.tickDelta());
    CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
    return CrosshairHudApi.Result.HANDLED;
});
```

- `PASS`：继续询问低优先级 provider；全部 PASS 后绘制原版 Wathe 默认准心。
- `HANDLED`：停止 provider 链并跳过默认准心。
- 返回 `HANDLED` 但不绘制任何内容，可用于故意隐藏默认准心。

### Overlay

Overlay 用于保留默认准心，只在其后追加小型提示。它不会短路，所有 overlay 都会执行；
priority 越大越晚绘制。

```java
CrosshairHudApi.registerOverlay(MyMod.id("crosshair/watch_progress"), 0, context -> {
    Float progress = getWatchProgress(context.player());
    if (progress == null) {
        return;
    }

    int width = 20;
    int x = context.centerX() - width / 2;
    int y = context.centerY() + 12;
    context.drawContext().fill(x, y, x + width, y + 2, 0x88000000);
    context.drawContext().fill(x, y, x + Math.round(width * progress), y + 2, 0xFFFFFFFF);
});
```

### 绘制辅助方法

- `renderStandardCrosshair`：Wathe 标准 3x3 普通/目标准心。
- `renderKnifeProgressCrosshair`：匕首样式 ready/progress 图标。
- `renderBatProgressCrosshair`：棍棒样式 ready/progress 图标。
- `renderIconProgressCrosshair`：扩展自定义 10x7 ready/background/fill 纹理。
- `renderCentered`：在屏幕中心局部坐标系中执行自定义绘制。
- `drawCrosshairIcon`、`drawKnifeProgressIcon`、`drawBatProgressIcon`、`drawIconProgress`：
  在已经居中的坐标系内进行更细粒度组合。

进度值会限制在 `0..1`，标准填充宽度限制在 `0..10`。内置 helper 会恢复矩阵和准心使用的
混合状态；扩展直接操作矩阵或 RenderSystem 时仍必须自行成对 push/pop 并恢复状态。

## 与目标可见性的关系

准心颜色和图标属于客户端提示。查询玩家或尸体目标时应同时使用
`TargetVisibilityApi.canTargetPlayer(...)`、`canTargetBody(...)` 或 `canTargetEntity(...)`，避免隐藏目标
仍泄漏锁定提示。真实交互和攻击必须在服务端分别调用对应的 INTERACT/ATTACK 能力入口进行复核。
