package dev.annawathe.api.economy;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** 货币的显示定义；余额不放在定义中，而放在玩家的商店组件中。 */
public record CurrencyDefinition(@NotNull Identifier id, @NotNull String icon, @NotNull String translationKey,
                                 long order, @NotNull EconomyApi.CurrencyHudPredicate hudPredicate) {
    public CurrencyDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(icon); Objects.requireNonNull(translationKey); Objects.requireNonNull(hudPredicate);
    }
}
