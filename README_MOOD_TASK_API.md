# AnnaWathe 心情与任务 API

本接口运行在“原版 Wathe 1.3.2 + AnnaWathe”组合中。扩展不得再给原版
`PlayerMoodComponent.Task` 枚举加值，也不要 Mixin `MoodRenderer` 或任务循环。

## 注册任务

```java
MoodTaskApi.registerTask(MoodTaskDefinition.builder(
        MyMod.id("my_task"),
        "task.mymod.my_task",
        player -> new MyTask(),
        (player, nbt) -> new MyTask(nbt)
).taskPoints(MyTaskPoints.MY_POINT).build());
```

扩展任务默认只能指定发放。需要进入所有玩家的普通随机池时，显式调用
`.randomlyAssignable()`。任务运行时实现 `MoodTaskInstance`，NBT 中只保存实例自己的
进度；稳定任务 ID 由 AnnaWathe 统一写入。

## 发放、删除与完成

```java
MoodTaskApi.assignTask(player, MY_TASK);
MoodTaskApi.assignRandomTasks(player, 2);
MoodTaskApi.removeTask(player, MY_TASK);
MoodTaskApi.completeTask(player, MY_TASK, true);
```

`removeTask` 只删除；`completeTask` 才会按正常流程回复心情、播放完成动画并触发
`TaskCompletionApi.AFTER_TASK_COMPLETE`。

## 默认心情数值

- 有至少一个任务时，每 tick 下降 `1 / 4000`，即约 3 分 20 秒从 1 降到 0；
- 完成一个真实心情任务回复 `0.4`；
- 多任务只改变任务数量，不会成倍增加下降速度；
- `MoodApi.setDrainMultiplier` 在上述基础下降值上乘算，临时保护时间则暂停下降。

这些值固定采用自改 Wathe 的配置，不读取原版 Wathe 1.3.2 的 `MOOD_DRAIN` 和
`MOOD_GAIN`，以免运行时又退回原版“下降较慢、单次回复较多”的节奏。

发放前使用 `registerAssignmentRule`，完成前使用 `registerCompletionRule`。priority 越大
越先执行；同 priority 后注册者优先；相同规则 ID 覆盖旧规则。

## 任务点

```java
MoodTaskPointApi.registerTaskPoint(MY_POINT, "hud.mymod.my_point", 0x66CCFF);
MoodTaskPointApi.registerScanHandler(MyMod.id("my_point_scan"), 0, context -> {
    if (context.state().isOf(MyBlocks.MY_BLOCK)) context.addTaskPoint(MY_POINT);
});
```

扫描 handler 每次只判断 `context.pos()` 这一格。AnnaWathe 已限制为当前列车复制区域和
`playArea`，扩展不得再次扫描整个世界。

## Mood HUD

客户端入口中注册：

```java
MoodHudApi.registerRoleStyle(MY_ROLE,
        MoodHudStyle.builder(MyMod.id("hud/mood_my_role"))
                .barColor(MY_ROLE.color())
                .build());
```

`barColor` 同时接受 `0xRRGGBB` 和 `Color#getRGB()` 的 `0xAARRGGBB`，最终透明度始终由
HUD 淡入淡出状态决定。自定义 BarRenderer 应使用 `MoodHudColors.withAlpha`。

疯魔 HUD 使用 `registerPsychoStyle`，可以替换完整身体、破损身体、眼睛、跑马文本、
文本颜色和倒计时条颜色。本轮不包含服务端 Psycho profile API。

## 调试指令

```text
/wathe:setMood <0-1> [players]
/wathe:moodEffectDeath [true|false]
/wathe:moodTask list
/wathe:moodTask assign|remove|complete <task> [player]
/wathe:taskPoints [reload|refresh|autoRefresh]
```
