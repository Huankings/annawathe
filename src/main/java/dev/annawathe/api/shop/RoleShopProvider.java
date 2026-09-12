package dev.annawathe.api.shop;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import java.util.List;
/** 根据玩家实时状态构建职业商店；purchase 只负责交付，不能自行扣款。 */
@FunctionalInterface public interface RoleShopProvider {
    @NotNull List<ShopEntry> getShopEntries(@NotNull PlayerEntity player);
    default @NotNull ShopPurchaseResult purchase(@NotNull ShopPurchaseContext context) { return ShopApi.defaultPurchase(context); }
}
