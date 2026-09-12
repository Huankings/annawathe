package dev.annawathe.api.economy;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/** 由 PlayerShopComponent Mixin 实现的通用余额存储协议。 */
public interface PlayerEconomyAccess {
    int annawathe$getCurrencyAmount(@NotNull Identifier currency);

    void annawathe$setCurrencyAmount(@NotNull Identifier currency, int amount);

    @NotNull Map<Identifier, Integer> annawathe$getCurrencyBalancesSnapshot();

    /** 先校验全部费用，再一次性扣除并同步，避免多货币购买发生部分扣款。 */
    boolean annawathe$spend(@NotNull List<CurrencyAmount> costs);
}
