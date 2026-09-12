package dev.annawathe.api.shop;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import java.util.List;

/**
 * Anna 自己维护的默认杀手商店。这里故意不读取 Wathe GameConstants.SHOP_ENTRIES 的价格，
 * 使 Anna 的价格表可以独立调整；商品行为仍调用原版组件已有的服务端能力。
 */
public final class AnnaDefaultShop {
    private AnnaDefaultShop() {}
    public static List<ShopEntry> entries() {
        return List.of(
                new ShopEntry(WatheItems.KNIFE.getDefaultStack(), 100, ShopEntry.Type.WEAPON),
                new ShopEntry(WatheItems.REVOLVER.getDefaultStack(), 250, ShopEntry.Type.WEAPON),
                new ShopEntry(WatheItems.GRENADE.getDefaultStack(), 300, ShopEntry.Type.WEAPON),
                ShopEntry.action(WatheItems.PSYCHO_MODE.getDefaultStack(), ShopPrice.money(350), ShopEntry.Type.WEAPON, PlayerShopComponent::usePsychoMode),
                new ShopEntry(WatheItems.POISON_VIAL.getDefaultStack(), 70, ShopEntry.Type.POISON),
                new ShopEntry(WatheItems.SCORPION.getDefaultStack(), 40, ShopEntry.Type.POISON),
                new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 10, ShopEntry.Type.TOOL),
                new ShopEntry(WatheItems.LOCKPICK.getDefaultStack(), 50, ShopEntry.Type.TOOL),
                new ShopEntry(WatheItems.CROWBAR.getDefaultStack(), 25, ShopEntry.Type.TOOL),
                new ShopEntry(WatheItems.BODY_BAG.getDefaultStack(), 70, ShopEntry.Type.TOOL),
                ShopEntry.action(WatheItems.BLACKOUT.getDefaultStack(), ShopPrice.money(250), ShopEntry.Type.TOOL, PlayerShopComponent::useBlackout),
                new ShopEntry(new ItemStack(WatheItems.NOTE, 4), 10, ShopEntry.Type.TOOL)
        );
    }
}
