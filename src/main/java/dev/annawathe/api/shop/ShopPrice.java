package dev.annawathe.api.shop;

import dev.annawathe.api.economy.CurrencyAmount;
import dev.annawathe.api.economy.EconomyApi;
import dev.annawathe.api.economy.PlayerEconomyApi;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/** 商店价格：option 之间为 OR，同一 option 内多项货币为 AND。 */
public final class ShopPrice {
    private final List<Option> options;
    private ShopPrice(List<Option> options) { this.options = List.copyOf(options); }
    public static @NotNull ShopPrice money(int amount) { return allOf(CurrencyAmount.money(amount)); }
    public static @NotNull ShopPrice allOf(@NotNull CurrencyAmount... costs) { return new ShopPrice(List.of(option(costs))); }
    public static @NotNull ShopPrice anyOf(@NotNull Option... options) { return new ShopPrice(Arrays.asList(options)); }
    public static @NotNull Option option(@NotNull CurrencyAmount... costs) { return new Option(List.of(costs)); }
    public @NotNull List<Option> options() { return options; }
    public int legacyPrice() {
        if (options.isEmpty()) return 0;
        for (CurrencyAmount cost : options.getFirst().costs()) if (cost.currency().equals(EconomyApi.MONEY)) return cost.amount();
        return options.getFirst().totalAmount();
    }
    public boolean canAfford(@NotNull PlayerEntity player) { return selectPayment(player) != null; }

    /** 可支付方案按货币数量总和升序选择；总和相同时保持定义顺序。 */
    public ShopPayment selectPayment(@NotNull PlayerEntity player) {
        ShopPayment selected = null;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            boolean affordable = true;
            for (CurrencyAmount cost : option.costs()) {
                if (PlayerEconomyApi.get(player, cost.currency()) < cost.amount()) {
                    affordable = false;
                    break;
                }
            }
            if (affordable && (selected == null || option.totalAmount() < selected.totalAmount())) {
                selected = new ShopPayment(i, option.costs());
            }
        }
        return selected;
    }

    /** 开发环境测试使用：不看余额，返回定义中总额最小的支付方案。 */
    public ShopPayment cheapestPaymentForDevelopment() {
        ShopPayment selected = null;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            if (selected == null || option.totalAmount() < selected.totalAmount()) {
                selected = new ShopPayment(i, option.costs());
            }
        }
        return selected;
    }
    public @NotNull List<CurrencyAmount> firstCosts() { return options.isEmpty() ? List.of() : options.getFirst().costs(); }
    /** 客户端价格提示使用的多行文本，OR 方案之间插入 shop.price.or。 */
    public @NotNull List<net.minecraft.text.Text> displayLines() {
        List<net.minecraft.text.Text> lines = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) lines.add(net.minecraft.text.Text.translatable("shop.price.or"));
            Option option = options.get(i);
            if (option.costs().isEmpty()) lines.add(net.minecraft.text.Text.translatable("shop.price.free"));
            else for (CurrencyAmount cost : option.costs()) lines.add(EconomyApi.formatCurrencyAmount(cost, true));
        }
        return lines;
    }
    public record Option(@NotNull List<CurrencyAmount> costs) {
        public Option { costs = costs.stream().filter(Objects::nonNull).filter(c -> c.amount() > 0).toList(); }
        public int totalAmount() { return costs.stream().mapToInt(CurrencyAmount::amount).sum(); }
    }
}
