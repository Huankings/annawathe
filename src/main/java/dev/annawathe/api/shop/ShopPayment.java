package dev.annawathe.api.shop;

import dev.annawathe.api.economy.CurrencyAmount;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

/** 服务端最终选择并扣除的一组支付方案。 */
public record ShopPayment(int optionIndex, @NotNull List<CurrencyAmount> costs) {
    public ShopPayment { costs = List.copyOf(costs); }
    public static ShopPayment of(int optionIndex, List<CurrencyAmount> costs) { return new ShopPayment(optionIndex, costs); }
    public static ShopPayment money(int amount) { return new ShopPayment(0, List.of(CurrencyAmount.money(amount))); }
    public int totalAmount() { return costs.stream().mapToInt(CurrencyAmount::amount).sum(); }
    public NbtList toNbtList() {
        NbtList list = new NbtList();
        for (CurrencyAmount cost : costs) {
            NbtCompound tag = new NbtCompound();
            tag.putString("currency", cost.currency().toString());
            tag.putInt("amount", cost.amount());
            list.add(tag);
        }
        return list;
    }
}
