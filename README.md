# AnnaWathe 中文开发说明

AnnaWathe 是面向原版 Wathe 的扩展框架 Mod，运行于 Minecraft 1.21.1、Fabric、Java 21。它提供本能透视、独立胜利、物品 Tooltip/准确冷却读秒和结算界面重写的统一接入点。

本工程与 `D:\哈比快车最新源码\wathe\Wathe - 副本1` 的自改 Wathe 完全独立。AnnaWathe 只搭配原版 Wathe `1.3.2-1.21.1`，不可与自改 Wathe jar 同时加载。

## 运行环境

| 项目 | 当前要求 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| Loader | Fabric Loader 0.17.2 或兼容版本 |
| 基础 Mod | 原版 Wathe 1.3.2-1.21.1 |
| 依赖 | Fabric API、Cardinal Components API 6.1.1 |

构建依赖固定为 `libs/wathe-1.3.2-1.21.1.jar`。运行时也必须使用同一原版 Wathe 分支。

## 目录导览

```text
annawathe/
├─ libs/wathe-1.3.2-1.21.1.jar
├─ src/main/java/dev/annawathe/       common/服务端入口、API、CCA、Mixin
├─ src/client/java/dev/annawathe/     客户端入口、Tooltip、结算 renderer、Mixin
├─ src/main/resources/                Fabric/CCA/Mixin 配置与语言文件
├─ AGENTS.md                          后续开发固定规则
└─ build/libs/                        构建产物
```

## 已实现能力

- `InstinctApi`：本能资格与目标高亮优先级规则；
- `/instinct key true|false`：玩家本能按键模式；
- `VictoryApi`：普通胜利拦截、额外共胜、独立胜利；
- `CustomVictory` / `CustomVictoryGroup`：独立胜利数据、颜色、翻译和赢家 UUID；
- `ItemTooltipApi`：多行物品描述、真实冷却条目读秒、动态追加文本；
- `AnnaRoundTextRenderer`：欢迎公告、普通/独立/Loose Ends 结算重绘。

具体职业胜利规则不放在 AnnaWathe 内，而由扩展 Mod 自己注册。商店、体力、移动、任务、停电、雾效、疯魔等自改 Wathe API 不属于当前 AnnaWathe 范围。

## 本能 API

包名：`dev.annawathe.api.instinct.InstinctApi`。

```java
InstinctApi.registerAvailability(MyMod.id("role_instinct"), 20, viewer -> {
    return isMyRole(viewer) && AnnaWatheClient.inputActive()
            ? InstinctApi.AvailabilityResult.ENABLE
            : InstinctApi.AvailabilityResult.PASS;
});

InstinctApi.registerHighlight(MyMod.id("marked_target"), 30, (viewer, target) -> {
    return isMarked(viewer, target)
            ? InstinctApi.HighlightResult.color(0xFFAA00)
            : InstinctApi.HighlightResult.pass();
});
```

priority 越大越先执行，同 priority 后注册者优先。资格返回 `PASS/ENABLE/DISABLE`；高亮返回 `pass()/color()/hide()`。扩展必须使用 `AnnaWatheClient.inputActive()`，不能直接读取 Wathe keybinding。

## 胜利 API

包名：`dev.annawathe.api.win`。

```java
VictoryApi.registerRule(MyMod.id("lone_winner"), 100, context -> {
    if (context.alivePlayers().size() == 1 && isMyRole(context.alivePlayers().getFirst())) {
        return VictoryApi.VictoryResult.customWin(
                CustomVictory.of(MyMod.id("lone_winner"), 0xE3A42D, context.alivePlayers()));
    }
    return VictoryApi.VictoryResult.pass();
});
```

结果含义：

- `pass()`：继续其他规则或原版结算；
- `keepRunning()`：阻止本次普通结算；
- `vanillaWin(status, extraWinnerUuids)`：原版阵营结算并追加赢家；
- `customWin(victory)`：独立胜利。

赢家保存 UUID，不保存运行时 Player 实例。默认翻译 key 为 `announcement.win.<namespace>.<path>`、`game.win.<namespace>.<path>` 和 `announcement.role.<namespace>.<path>`，扩展需补齐语言文件。

## Tooltip API

包名：`dev.annawathe.api.client.tooltip.ItemTooltipApi`，仅客户端可用。

```java
ItemTooltipApi.registerItem(MY_ITEM);
ItemTooltipApi.registerAppender(MyMod.id("item_state"), 10, MY_ITEM, context -> {
    context.tooltip().add(Text.literal("额外状态"));
});
```

标准 Tooltip 读取 `<item.translationKey>.tooltip`；冷却直接读取当前 `ItemCooldownManager` 条目 `endTick - tick`，不使用固定总冷却或进度反推。Tooltip 只是显示层，不能作为服务端合法性判断。

AnnaWathe 已禁用原版 Wathe Tooltip callback，其他扩展不要为同类物品重复注册全局 callback。

## 结算界面

`AnnaRoundTextRenderer` 接管原版欢迎/结算渲染，包含平民、义警、杀手、中立、Loose Ends、独立胜利双分区、动态列数、残行对齐、真实职业标题、职业颜色、头像缓存、死亡暗化、红叉、文本缩放和屏幕高度自适应。

扩展如需额外左侧分组，使用 `AnnaRoundTextRenderer` 的公开布局方法，不要硬编码坐标或再次 Mixin 原版 `RoundTextRenderer`。

标题文字必须遵循“先缩放，再使用局部 y 坐标”的矩阵顺序；否则欢迎/胜利标题会下移并与描述重叠。

## CCA 组件

| 组件 | CCA id | 用途 |
| --- | --- | --- |
| `PlayerInstinctComponent` | `annawathe:instinct` | 玩家本能按键模式。 |
| `AnnaRoundEndState` | `annawathe:round_state` | 独立胜利和额外赢家 UUID。 |

新组件必须同时完成 factory、NBT、同步/清理，并在 `fabric.mod.json` 的 `custom.cardinal-components` 声明 ID。否则会出现 `was not registered through mod metadata or plugin`。

## 构建

```powershell
cd "D:\哈比快车最新源码\原版哈比列车\annawathe"
.\gradlew.bat build
```

产物：`build/libs/annawathe-1.0.0-1.21.1.jar`。测试前删除旧 jar，只保留一个版本。
