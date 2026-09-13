# AnnaWathe 中文开发说明

AnnaWathe 是面向原版 Wathe 的扩展框架 Mod，运行于 Minecraft 1.21.1、Fabric、Java 21。它提供本能透视、独立胜利、物品 Tooltip/准确冷却读秒和结算界面重写的统一接入点。

本工程与 `D:\哈比快车最新源码\wathe\Wathe - 副本1` 的自改 Wathe 完全独立。AnnaWathe 只搭配原版 Wathe `1.3.2-1.21.1`，不可与自改 Wathe jar 同时加载。Psycho profile、护盾、临时物品和视觉能力由 AnnaWathe 自己提供，不依赖自改 Wathe。

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
- `MoodTaskApi`：Identifier 任务注册、多任务发放/删除/完成和优先级拦截；
- `MoodTaskPointApi`：任务点类型、地图扫描扩展和客户端穿墙透视；
- `MoodHudApi`：普通/职业色/疯魔 Mood HUD 样式；
- `PlayerLifeStateApi`：creative/spectator 仍按局内存活处理；
- `PlayerMovementApi`：速度 ADD/MULTIPLY/OVERRIDE/PASS 修正规则；
- `PlayerCollisionApi`：SOLID、VANILLA_PUSH、NO_COLLISION 玩家碰撞规则；
- `PlayerAppearanceApi` / `BodyAppearanceApi`：玩家与尸体皮肤覆盖；
- `RoleNameHudApi`：完整准心名字、射线来源、目标过滤、杀手同伙和额外 HUD 扩展；
- `HudOverlayApi`：三阶段通用屏幕 HUD、职业存活过滤、常用布局和热键栏复画；
- `CrosshairHudApi`：可短路的准心 provider、后置 overlay 和标准/进度准心绘制工具；
- `TargetVisibilityApi`：玩家/尸体渲染、准心选中、交互和攻击规则；
- `HeldItemInvisibilityApi` / `PsychosisItemApi`：手持物隐藏与低心情幻觉物品/手臂姿势；
- `PsychoModeApi` / `PsychoModeProfile`：可注册的疯魔持续时间、护盾、武器、临时物品、锁栏和服务端护盾规则；
- `PsychoModeClientApi`：疯魔皮肤、模型特征隐藏、背景音乐和视觉 provider；
- `BodyInfoApi` / `BodyInfoHudApi`：尸体死亡时间+死因摘要、死亡时身份快照及客户端字段可见性规则；
- `PlayerTransformApi`：跨回合、重生和重启保存的调试外观变形；
- `ShopApi` / Anna `ShopEntry`：默认杀手商店重写、职业/动态商店与优先级修改器；
- `EconomyApi` / `PlayerEconomyApi`：多货币余额、AND/OR 支付、被动收入和任务收入扩展；
- `TimeHudApi`：可覆盖默认回合时间的优先级计时显示；
- `InventoryButtonApi`：限制、普通和创造背包的按钮完整生命周期；
- 精神崩溃死亡、调试指令，以及 shift/run/sit/stay/away 五个额外任务。

心情数值采用自改 Wathe 的节奏，而不是原版 1.3.2 的数值：只要存在任意任务，每 tick
下降 `1 / 4000`（约 3 分 20 秒从满值降到零），完成一个真实心情任务回复 `0.4`。
同时存在多个任务时不会按任务数量倍增下降速度。

具体职业胜利和任务规则不放在 AnnaWathe 内，而由扩展 Mod 自己注册。体力、停电、雾效和回放事件兼容仍不属于当前范围。

本轮新增的相关调试指令均要求权限等级 2：

- `/annawathe:gamemode <mode> [player]`：以“玩法存活”语义切换 creative/spectator；
- `/annawathe:playerCollision [true|false]`：查询或切换局内存活玩家碰撞；
- `/annawathe:startnoCollision [seconds]`：查询或设置开局免碰撞时间；
- `/annawathe:transform <player> <appearance> <seconds|permanent>`：让一名玩家变成另一名在线玩家的外观；
- `/annawathe:transform all <appearance> <seconds|permanent>`：让当前服务器所有其它在线玩家变成指定外观；
- `/annawathe:transform clear <player>`、`/annawathe:transform clearAll`：清除变形。

调试变形只覆盖普通外观和准心名称，不改变职业、阵营、声音、物品、碰撞或服务端身份，并且低于扩展职业和特殊视角外观规则。`PlayerMovementApi` 只迁移速度修正，不包含自改 Wathe 的完整体力系统。准心图标扩展统一使用 `CrosshairHudApi`，准心名字与目标文字使用 `RoleNameHudApi`，目标隐藏/不可选中/不可交互/不可攻击统一使用 `TargetVisibilityApi`。

心情与任务接口详见 `README_MOOD_TASK_API.md`。
商店和多货币接口详见 `README_SHOP_CURRENCY_API.md`；时间 HUD 与背包按钮详见 `README_TIME_INVENTORY_API.md`。
通用屏幕 HUD 与准心图标接口详见 `README_HUD_CROSSHAIR_API.md`。

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
| `PlayerLifeStateComponent` | `annawathe:life_state` | creative/spectator 特殊玩法存活授权。 |
| `PlayerAppearanceOverrideComponent` | `annawathe:appearance_override` | 持久化调试外观变形目标与到期时间。 |
| `AnnaCollisionSettings` | `annawathe:collision_settings` | 玩家碰撞总开关、开局免碰撞时间和本局起点。 |
| `AnnaBodyInfoComponent` | `annawathe:body_info` | 尸体死亡原因、死亡时身份和死亡世界时间。 |

新组件必须同时完成 factory、NBT、同步/清理，并在 `fabric.mod.json` 的 `custom.cardinal-components` 声明 ID。否则会出现 `was not registered through mod metadata or plugin`。

## Psycho 与尸体信息 API

`PsychoModeApi.registerProfile` 注册扩展 profile；`start(player, profileId)`、`stop(player)`、`isActive(player)`、`getActiveProfile(player)`、`getArmour(player)` 和 `getRemainingTicks(player)` 用于服务端状态控制。`registerShieldRule` 用于穿盾/特殊护盾规则，规则优先级与其它 Anna API 一致。临时授予物品必须由 profile 的 `grantedItems` 提供，Anna 会自动写入并在结束时精确回收标记物品。

`PsychoModeClientApi.registerVisualProvider` 和 `registerBackgroundAmbience` 只影响客户端皮肤、模型特征和背景音；不能替代服务端合法性判断。

尸体生成时 Anna 会通过 `BodyInfoApi` 保存死亡时间、死因和死亡瞬间职业。`BodyInfoHudApi.registerRule` 的 `deathSummary` 表示“死亡时间+死因”整行，`roleIdentity` 表示死者身份；扩展可以只显示其中一项。默认只有非存活观察者显示两项，具体职业/词条可以注册更高优先级规则。

## 构建

```powershell
cd "D:\哈比快车最新源码\原版哈比列车\annawathe"
.\gradlew.bat build
```

产物：`build/libs/annawathe-1.0.0-1.21.1.jar`。测试前删除旧 jar，只保留一个版本。
