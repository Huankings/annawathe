package dev.annawathe.api.economy;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** 商店价格中的一项货币数量。数量只允许非负，实际余额由 Wathe 的玩家组件保存。 */
public record CurrencyAmount(@NotNull Identifier currency, int amount) {
    public CurrencyAmount {
        Objects.requireNonNull(currency, "currency");
        if (amount < 0) throw new IllegalArgumentException("Currency amount cannot be negative");
    }

    public static @NotNull CurrencyAmount of(@NotNull Identifier currency, int amount) { return new CurrencyAmount(currency, amount); }
    public static @NotNull CurrencyAmount money(int amount) { return of(EconomyApi.MONEY, amount); }
    public static @NotNull CurrencyAmount taskMoney(int amount) { return of(EconomyApi.TASK_MONEY, amount); }
}
