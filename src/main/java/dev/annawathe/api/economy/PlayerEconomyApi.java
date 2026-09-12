package dev.annawathe.api.economy;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家余额门面。余额实际存放在原版 wathe:shop CCA 及 Anna 注入的扩展字段中，
 * 所有写操作都由原版组件同步，因此扩展无需自行发送余额网络包。
 */
public final class PlayerEconomyApi {
    private PlayerEconomyApi() {}
    public static int get(@NotNull PlayerEntity player, @NotNull Identifier currency) {
        return access(player).annawathe$getCurrencyAmount(currency);
    }
    public static void set(@NotNull PlayerEntity player, @NotNull Identifier currency, int amount) {
        access(player).annawathe$setCurrencyAmount(currency, amount);
    }
    public static void add(@NotNull PlayerEntity player, @NotNull Identifier currency, int amount) {
        set(player, currency, get(player, currency) + amount);
    }

    public static boolean spend(@NotNull PlayerEntity player, @NotNull java.util.List<CurrencyAmount> costs) {
        return access(player).annawathe$spend(costs);
    }

    public static @NotNull java.util.Map<Identifier, Integer> snapshot(@NotNull PlayerEntity player) {
        return access(player).annawathe$getCurrencyBalancesSnapshot();
    }

    private static PlayerEconomyAccess access(PlayerEntity player) {
        return (PlayerEconomyAccess) (Object) PlayerShopComponent.KEY.get(player);
    }
}
