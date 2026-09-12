# AnnaWathe 时间 HUD 与背包按钮 API

## TimeHudApi

```java
TimeHudApi.registerProvider(MyMod.id("special_countdown"), 100, viewer -> {
    if (!shouldShow(viewer)) return TimeHudApi.TimeDisplay.pass();
    return TimeHudApi.TimeDisplay.showFixedColor(getTicks(viewer), 0xE04B4B);
});
```

provider 返回 `PASS` 会继续后续规则，返回 `SHOW` 或 `HIDE` 会结束解析。priority 越大越先执行；
同 priority 后注册者优先。普通扩展 priority 0 也排在 Anna 默认回合时间之前。

可使用动态颜色、低时间警告或固定颜色。时间来源 ID 改变时 renderer 会重置滚动数字，避免不同
倒计时之间残留动画。该 API 仅负责客户端显示，服务端时间和玩法状态仍由扩展自己的组件同步。

## InventoryButtonApi

支持 `LIMITED`、`VANILLA` 和 `CREATIVE` 三种背包。

```java
InventoryButtonApi.registerProvider(MyMod.id("guide"), 0, context -> {
    if (context.type() == InventoryScreenType.LIMITED) return null;
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

每次打开屏幕都会创建一个 extension 实例，可以安全保存当前屏幕的临时状态。API 统一调度
`init`、`tick`、`render`、`allowInventoryKeyClose` 和 `close`。

使用 `addWidget(group, widget)`、`replaceGroup`、`clearGroup` 和 `setGroupVisible` 管理动态控件。
关闭时旧控件会被隐藏、禁用并解除焦点。`InventoryPageState` 按 Identifier 隔离页码，并在断线和
换局时清理。
