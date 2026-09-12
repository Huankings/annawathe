package dev.annawathe.api.shop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * AnnaWathe 正式商店商品模型。它不继承原版 Wathe 的 util.ShopEntry，避免扩展 API 被基础 Mod 内部类锁死。
 * 原版默认商品在 Adapter 中转换为此模型；客户端和服务端始终使用同一份 Anna 商品列表。
 */
public class ShopEntry {
    private final ItemStack stack;
    private final ShopPrice price;
    private final Type type;
    private final PurchaseAction action;
    private final boolean showFailureMessage;
    @FunctionalInterface public interface PurchaseAction { boolean buy(@NotNull PlayerEntity player); }
    public enum Type {
        WEAPON("gui/shop_slot_weapon"), POISON("gui/shop_slot_poison"), TOOL("gui/shop_slot_tool");
        private final Identifier texture;
        Type(String texture) { this.texture = Identifier.of("wathe", texture); }
        public Identifier texture() { return texture; }
    }
    public ShopEntry(@NotNull ItemStack stack, int price, @NotNull Type type) { this(stack, ShopPrice.money(price), type, null, true); }
    public ShopEntry(@NotNull ItemStack stack, @NotNull ShopPrice price, @NotNull Type type) { this(stack, price, type, null, true); }
    protected ShopEntry(@NotNull ItemStack stack, @NotNull ShopPrice price, @NotNull Type type, @NotNull PurchaseAction action, boolean showFailureMessage) {
        this.stack = stack; this.price = price; this.type = type; this.action = action; this.showFailureMessage = showFailureMessage;
    }
    public boolean onBuy(@NotNull PlayerEntity player) { return action != null ? action.buy(player) : GameWorldComponent.KEY.get(player.getWorld()).canUseKillerFeatures(player) && insertStackInFreeSlot(player, stack.copy()); }
    public boolean shouldShowPurchaseFailedMessage(@NotNull PlayerEntity player) { return showFailureMessage; }
    public ItemStack stack() { return stack; }
    public ShopPrice shopPrice() { return price; }
    public int price() { return price.legacyPrice(); }
    public Type type() { return type; }
    public static Type typeFromWathe(dev.doctor4t.wathe.util.ShopEntry.Type type) { return Type.valueOf(type.name()); }
    public static @NotNull ShopEntry directToHotbar(ItemStack stack, ShopPrice price, Type type) { return new ShopEntry(stack, price, type, p -> insertStackInFreeSlot(p, stack.copy()), true); }
    public static @NotNull ShopEntry directToHotbar(ItemStack stack, int price, Type type) { return directToHotbar(stack, ShopPrice.money(price), type); }
    public static @NotNull ShopEntry giveToInventory(ItemStack stack, ShopPrice price, Type type) { return new ShopEntry(stack, price, type, p -> p.giveItemStack(stack.copy()), true); }
    public static @NotNull ShopEntry giveToInventory(ItemStack stack, int price, Type type) { return giveToInventory(stack, ShopPrice.money(price), type); }
    public static @NotNull ShopEntry action(ItemStack stack, ShopPrice price, Type type, PurchaseAction action) { return new ShopEntry(stack, price, type, action, true); }
    public static @NotNull ShopEntry action(ItemStack stack, ShopPrice price, Type type, PurchaseAction action, boolean showFailure) { return new ShopEntry(stack, price, type, action, showFailure); }
    public static @NotNull ShopEntry action(ItemStack stack, int price, Type type, PurchaseAction action) { return action(stack, ShopPrice.money(price), type, action, true); }
    public static @NotNull ShopEntry action(ItemStack stack, int price, Type type, PurchaseAction action, boolean showFailure) { return action(stack, ShopPrice.money(price), type, action, showFailure); }
    public static boolean insertStackInFreeSlot(PlayerEntity player, ItemStack stack) {
        for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isEmpty()) { player.getInventory().setStack(i, stack); return true; }
        return false;
    }
}
