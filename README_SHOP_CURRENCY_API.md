# AnnaWathe 商店与经济 API

AnnaWathe 使用 `dev.annawathe.api.shop.ShopEntry` 作为正式商品模型，不使用原版 Wathe 的
`dev.doctor4t.wathe.util.ShopEntry`。默认杀手商店也由 `AnnaDefaultShop` 独立定义价格。

## 默认价格

当前默认商店保持纯金币支付，价格为：匕首 100、左轮 250、手雷 300、疯魔 350、毒瓶 70、
蝎子 40、爆竹 10、开锁器 50、撬棍 25、尸袋 70、停电 250、四张便签 10。

任务币和多货币支付代码已经保留，但任务币 HUD、任务奖励和击杀奖励默认均为关闭状态。

## 自定义货币

```java
public static final Identifier BLOOD = MyMod.id("blood");

EconomyApi.registerCurrency(
        BLOOD,
        "\uE783",
        "currency.mymod.blood",
        context -> context.role() == MyRoles.BLOOD_MAGE
);

PlayerEconomyApi.add(player, BLOOD, 3);
int amount = PlayerEconomyApi.get(player, BLOOD);
```

货币余额保存在原版 `wathe:shop` CCA 中。非金币余额写入 `CurrencyBalances` NBT，随原版
组件同步，并在原版商店组件 `reset()` 时和金币一起清除。扩展不需要自行同步余额。

## 价格

```java
ShopPrice.money(100);

ShopPrice.allOf(
        CurrencyAmount.money(100),
        CurrencyAmount.of(BLOOD, 2)
);

ShopPrice.anyOf(
        ShopPrice.option(CurrencyAmount.money(200)),
        ShopPrice.option(CurrencyAmount.of(BLOOD, 4))
);
```

多个 option 是 OR，同一 option 内是 AND。玩家同时买得起多组方案时，选择货币数量总和最小的
方案；总和相同则选择定义顺序更靠前的方案。

## 职业商店

职业商店必须在 common 初始化阶段注册，使客户端显示和服务端购买解析使用相同规则。

```java
ShopApi.registerRoleShop(MyRoles.BLOOD_MAGE, player -> List.of(
        ShopEntry.giveToInventory(
                MyItems.BLOOD_DAGGER.getDefaultStack(),
                ShopPrice.allOf(CurrencyAmount.money(150), CurrencyAmount.of(BLOOD, 2)),
                ShopEntry.Type.WEAPON
        )
));
```

默认 `new ShopEntry(...)` 保留原版杀手快捷栏限制。非杀手商店应显式使用
`directToHotbar`、`giveToInventory` 或 `action`。

## 修改默认商店

```java
ShopApi.registerShopModifier(MyMod.id("blood_shop"), 20, (context, entries) -> {
    if (context.role() != MyRoles.BLOOD_MAGE) return;
    entries.removeIf(entry -> entry.stack().isOf(WatheItems.KNIFE));
    entries.add(ShopEntry.giveToInventory(
            MyItems.BLOOD_DAGGER.getDefaultStack(), 150, ShopEntry.Type.WEAPON));
});
```

priority 越大越先执行；同 priority 后注册者先执行；相同 ID 覆盖旧规则。

客户端购买只发送商品索引。服务端会重新检查对局、玩法存活、动态列表、价格、余额和物品冷却，
交付成功后再原子扣除全部货币。扩展 provider 不能自行扣款。

## 被动和任务收入

`EconomyApi.registerPassiveIncomeRole`、`registerPassiveIncomeRule` 和
`registerPassiveIncomeModifier` 接管原版 Murder 循环中的资格与数值。

`TaskCompletionApi.registerTaskIncomeProvider` 为非杀手任务提供金币收入；
`registerTaskIncomeRule` 可以抑制默认任务收入但不会阻止完成事件。杀手任务币与击杀任务币的
服务端路径已保留，当前常量均为 0。
