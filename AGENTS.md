# AnnaWathe 开发说明

本文件是 `D:\哈比快车最新源码\原版哈比列车\annawathe` 的长期协作规则。后续处理新增功能、API、扩展接入、bug 修复和构建任务时，先读取本文件与 `README.md`。

## 项目边界

AnnaWathe 是原版 Wathe 的扩展框架，不是自改 Wathe 的下一版本。

| 项目 | 路径 | 用途              |
| --- | --- |-----------------|
| AnnaWathe | `D:\哈比快车最新源码\原版哈比列车\annawathe` | 当前框架工程。         |
| 原版 Wathe | `D:\哈比快车最新源码\原版哈比列车\Wathe-main` | 唯一基础 Mod 和源码对照。 |
| HarpyModLoader | `D:\哈比快车最新源码\原版哈比列车\HarpyModLoader` | 原版职业分配器参考。      |
| NoellesRoles | `D:\哈比快车最新源码\原版哈比列车\NoellesRoles` | 原版职业扩展参考。       |
| StupidExpress | `D:\哈比快车最新源码\原版哈比列车\StupidExpress` | 原版职业/独立胜利迁移参考。  |
| StarryExpress | `:\哈比快车最新源码\原版哈比列车\StarryExpress` | 装饰和指南参考。        |
| 自改 Wathe | `D:\哈D比快车最新源码\wathe\Wathe - 副本1` | 仅作机制对照，不是运行依赖。  |
| 自改 HarpyModLoader | `D:\哈比快车最新源码\harpymodloader\HarpyModLoader1` | 仅作扩展对照，不是运行依赖。  |

自改 Wathe、自改扩展和原版 Wathe、AnnaWathe、原版扩展是两条独立维护线。禁止同时加载自改 Wathe jar 与 AnnaWathe。

## 固定流程

1. 先读需求、`README.md`、`build.gradle`、`gradle.properties`、`fabric.mod.json` 和相关 Mixin json。
2. 用户要求“先方案”时只做只读分析，不修改文件。
3. 优先使用 `dev.annawathe.api`，不要让新扩展继续深层 Mixin Wathe 内部逻辑。
4. 服务端/common 代码放 `src/main/java`；客户端代码放 `src/client/java`。
5. 修改 Mixin、CCA、API 或结算布局后执行完整 `.\gradlew.bat build`。
6. 涉及客户端显示或生命周期的改动，必须在单人世界和实际对局中验证。
7. 代码修改处和关键机制边界写中文注释，重点说明服务端/客户端、同步、回合清理、胜利仲裁、冷却读取和坐标变换。

## 必读入口

### AnnaWathe

- `src/main/java/dev/annawathe/AnnaWathe.java`：入口和 `/instinct`。
- `src/main/java/dev/annawathe/api/instinct/InstinctApi.java`：本能注册/解析。
- `src/main/java/dev/annawathe/api/win/VictoryApi.java`：胜利仲裁。
- `src/main/java/dev/annawathe/api/win/CustomVictory.java`：独立胜利数据。
- `src/main/java/dev/annawathe/api/task/MoodTaskApi.java`：任务注册、发放、删除和完成。
- `src/main/java/dev/annawathe/api/task/MoodTaskPointApi.java`：任务点注册与扫描扩展。
- `src/main/java/dev/annawathe/mixin/PlayerMoodComponentMixin.java`：原版 mood CCA 桥接和任务循环接管。
- `src/main/java/dev/annawathe/cca/PlayerInstinctComponent.java`：按键模式组件。
- `src/main/java/dev/annawathe/cca/AnnaRoundEndState.java`：结算旁路状态。
- `src/main/java/dev/annawathe/cca/AnnaWatheComponents.java`：CCA factory。
- `src/main/java/dev/annawathe/mixin/MurderGameModeMixin.java`：胜利注入点。
- `src/client/java/dev/annawathe/client/AnnaWatheClient.java`：客户端初始化和默认本能规则。
- `src/client/java/dev/annawathe/client/gui/AnnaRoundTextRenderer.java`：完整结算 renderer。
- `src/client/java/dev/annawathe/api/client/tooltip/ItemTooltipApi.java`：扩展使用的 Tooltip 门面。
- `src/client/java/dev/annawathe/api/client/mood/MoodHudApi.java`：职业 Mood/疯魔 HUD 样式入口。
- `src/client/java/dev/annawathe/client/gui/AnnaMoodRenderer.java`：唯一 Mood renderer。
- `src/main/resources/fabric.mod.json`：入口、依赖、CCA 元数据。

### 原版 Wathe 对照

- `Wathe-main/src/main/java/dev/doctor4t/wathe/game/gamemode/MurderGameMode.java`：原版胜利循环。
- `Wathe-main/src/main/java/dev/doctor4t/wathe/cca/GameRoundEndComponent.java`：原版结算数据和 `didWin`。
- `Wathe-main/src/main/java/dev/doctor4t/wathe/client/WatheClient.java`：原版本能入口。
- `Wathe-main/src/main/java/dev/doctor4t/wathe/client/gui/RoundTextRenderer.java`：原版公告入口。
- `Wathe-main/src/main/java/dev/doctor4t/wathe/client/util/WatheItemTooltips.java`：被 AnnaWathe 替换的旧 Tooltip callback。

## API 规则

### Instinct

- `registerAvailability(id, priority, handler)`：资格规则。
- `registerHighlight(id, priority, handler)`：目标颜色/隐藏规则。
- 大 priority 先执行；同 priority 后注册者先执行；相同 ID 覆盖旧规则。
- 资格：`PASS` 继续，`ENABLE`/`DISABLE` 结束。
- 高亮：`pass()` 继续，`color(...)`/`hide()` 结束。
- 扩展调用 `AnnaWatheClient.inputActive()`，不直接读取 Wathe keybinding。
- 本能只是客户端视觉，不能替代服务端攻击/交互/购买校验。

### Victory

- `VictoryApi.registerRule(...)` 注册具体职业/词条规则。
- `pass()` 保持原版结算。
- `keepRunning()` 阻止本次结算，只能在明确的保活条件下使用。
- `vanillaWin(...)` 支持普通阵营共胜和额外赢家 UUID。
- `customWin(...)` 支持独立胜利。
- 赢家保存 UUID，不保存 Player 实例。
- 扩展自行补 `announcement.win.*`、`game.win.*`、`announcement.role.*` 翻译。
- AnnaWathe 不内置 NoellesRoles/StupidExpress 的具体职业规则。

### Tooltip

- `registerItem/registerItems` 注册标准描述和实际冷却读秒。
- `registerAppender` 追加动态文本。
- 冷却必须读取 `ItemCooldownManager` 当前条目，不可用固定总冷却反推。
- Tooltip 是客户端显示层，不作为服务端合法性判断。
- 不要为同一物品重复注册全局 `ItemTooltipCallback`。

### Mood 与任务

- 新任务实现 `MoodTaskInstance`，通过 `MoodTaskApi.registerTask` 注册稳定 ID；不要修改原版 Task 枚举。
- 扩展任务默认不进入随机池，需要时显式调用 `randomlyAssignable()`。
- `removeTask` 只是删除；`completeTask` 才会回复心情并触发完成事件。
- 心情基础数值固定采用自改 Wathe：每 tick 下降 `1 / 4000`，任务完成回复 `0.4`；不得重新读取原版 `GameConstants.MOOD_DRAIN/MOOD_GAIN`。
- 多任务只共享一份基础心情下降，禁止再按当前任务数量乘算掉落速度。
- 服务端是任务进度唯一权威；客户端任务数据只用于 HUD 和任务点过滤。
- 普通、职业色和疯魔 HUD 使用 `MoodHudApi`，禁止继续 Mixin 原版 `MoodRenderer`。
- 颜色可能来自 `Color#getRGB()` 时必须用 `MoodHudColors.withAlpha` 重写 alpha，不能按位 OR。
- 任务点扫描 handler 只判断当前格，不得自行全图扫描。

## CCA 规则

当前组件：

| 组件 | ID | 范围 |
| --- | --- | --- |
| `PlayerInstinctComponent` | `annawathe:instinct` | 玩家；CHARACTER 重生复制。 |
| `AnnaRoundEndState` | `annawathe:round_state` | World/Scoreboard；保存独立胜利和额外赢家。 |
| `AnnaMoodSettings` | `annawathe:mood_settings` | World；精神崩溃死亡开关。 |
| `AnnaTaskPointWorldState` | `annawathe:task_points` | World；任务点缓存与自动重扫设置。 |

新增组件必须：

1. 使用稳定的 `annawathe:<path>`；
2. 注册正确 factory 和生命周期；
3. 实现 NBT 读写；
4. 需要时同步；
5. 在 `fabric.mod.json` 的 `custom.cardinal-components` 添加 ID；
6. 补充开局、停局、重生、断线清理。

遗漏元数据会导致 `was not registered through mod metadata or plugin` 启动崩溃。

## Mixin 规则

common Mixin 放 `annawathe.mixins.json`，客户端 Mixin 放 `annawathe.client.mixins.json`，不要交叉登记。

`MurderGameMode.tickServerGameLoop` 的注入 handler 必须保持真实参数和局部变量顺序：

```java
private void handler(
        ServerWorld world,
        GameWorldComponent game,
        CallbackInfo ci,
        @Local GameFunctions.WinStatus status
)
```

`ServerWorld`、`GameWorldComponent` 是目标方法真实参数；`@Local` 参数必须放在 `CallbackInfo` 后。否则会出现 `Invalid descriptor` 或 `Found non-trailing sugared parameters`。

改 Mixin 后必须核对目标 descriptor、目标调用次数和局部变量类型，并做完整构建和进世界测试。

## 结算 renderer 规则

`AnnaRoundTextRenderer` 是唯一结算 renderer。它负责动态列数、区域定位、职业标题、头像、死亡标记、文本缩放和独立胜利布局。

- 人数/列数/行数用 `int`；坐标/尺寸/缩放用 `float`。
- 使用现有布局辅助方法，禁止硬编码坐标。
- 残行保持当前对齐模式。
- 文字必须自动缩放/截断，不能重叠。
- 标题绘制必须先 `scale` 再使用局部 y；不要先 translate y 再 scale。
- 验证普通胜利、独立胜利、Loose Ends、多人数和窄窗口。

## 扩展迁移模板

1. 确认扩展使用的是原版 Wathe，不是自改 Wathe。
2. 把旧 `WatheClient` 本能 Mixin 改成 `InstinctApi` 规则。
3. 把 `MurderGameMode` 胜利 Mixin 改成 `VictoryApi` 规则。
4. 把自定义 Tooltip callback 改成 `ItemTooltipApi`。
5. 删除已被 AnnaWathe 覆盖的旧 Mixin json 条目。
6. 重新构建扩展，并用“原版 Wathe + AnnaWathe + 扩展”测试。

## 崩溃排查

1. 读取日志最深处第一个 `Caused by`，不要只看顶部 RuntimeException。
2. CCA 元数据错误检查 `fabric.mod.json`。
3. Mixin descriptor 错误检查真实参数、`CallbackInfo`、`@Local` 顺序和 descriptor。
4. 启动成功、进世界崩溃时优先检查懒加载的 `MurderGameMode`、命令参数类和 world component。
5. 修复后执行完整 build，替换旧 jar，只保留一个 AnnaWathe 版本再测试。

## 构建

```powershell
cd "D:\哈比快车最新源码\原版哈比列车\annawathe"
.\gradlew.bat build
```

产物：`build/libs/annawathe-1.0.0-1.21.1.jar`。如果修改了对外 API，把最新 jar 复制到使用它的原版扩展工程 `libs` 后再编译扩展。

## 后续提示词

```text
请按 annawathe 根目录 AGENTS.md 的规则处理。
```
