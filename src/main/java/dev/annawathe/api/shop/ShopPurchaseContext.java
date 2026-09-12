package dev.annawathe.api.shop;
import dev.annawathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
public record ShopPurchaseContext(PlayerEntity player, PlayerShopComponent shop, ShopEntry entry, int index, GameWorldComponent gameWorld, @Nullable Role role, boolean roleSpecificShop) {
    public int balance() { return shop.balance; }
    public int currencyBalance(Identifier id) { return dev.annawathe.api.economy.PlayerEconomyApi.get(player, id); }
    public boolean canAffordEntry() { return entry.shopPrice().canAfford(player); }
}
