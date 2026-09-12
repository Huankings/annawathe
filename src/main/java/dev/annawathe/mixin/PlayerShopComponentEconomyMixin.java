package dev.annawathe.mixin;

import dev.annawathe.api.economy.CurrencyAmount;
import dev.annawathe.api.economy.EconomyApi;
import dev.annawathe.api.economy.PlayerEconomyAccess;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在原版 wathe:shop CCA 上追加多货币余额。
 * 金币继续使用原版 public balance 字段，非金币使用 Anna 的 Map；原版 CCA 同步包会自然携带注入后的 NBT。
 */
@Mixin(PlayerShopComponent.class)
public abstract class PlayerShopComponentEconomyMixin implements PlayerEconomyAccess {
    @Shadow public int balance;
    @Shadow public abstract void sync();

    @Unique
    private final Map<Identifier, Integer> annawathe$currencyBalances = new HashMap<>();

    @Override
    public int annawathe$getCurrencyAmount(@NotNull Identifier currency) {
        return EconomyApi.MONEY.equals(currency) ? this.balance : this.annawathe$currencyBalances.getOrDefault(currency, 0);
    }

    @Override
    public void annawathe$setCurrencyAmount(@NotNull Identifier currency, int amount) {
        this.annawathe$setCurrencyAmountUnsynced(currency, amount);
        this.sync();
    }

    @Override
    public @NotNull Map<Identifier, Integer> annawathe$getCurrencyBalancesSnapshot() {
        Map<Identifier, Integer> result = new HashMap<>(this.annawathe$currencyBalances);
        if (this.balance > 0) result.put(EconomyApi.MONEY, this.balance);
        return Map.copyOf(result);
    }

    @Override
    public boolean annawathe$spend(@NotNull List<CurrencyAmount> costs) {
        for (CurrencyAmount cost : costs) {
            if (this.annawathe$getCurrencyAmount(cost.currency()) < cost.amount()) return false;
        }
        for (CurrencyAmount cost : costs) {
            this.annawathe$setCurrencyAmountUnsynced(
                    cost.currency(),
                    this.annawathe$getCurrencyAmount(cost.currency()) - cost.amount()
            );
        }
        this.sync();
        return true;
    }

    @Unique
    private void annawathe$setCurrencyAmountUnsynced(Identifier currency, int amount) {
        int clamped = Math.max(0, amount);
        if (EconomyApi.MONEY.equals(currency)) {
            this.balance = clamped;
        } else if (clamped == 0) {
            this.annawathe$currencyBalances.remove(currency);
        } else {
            this.annawathe$currencyBalances.put(currency, clamped);
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void annawathe$clearCurrenciesOnRoundReset(CallbackInfo ci) {
        // 原版会在开局/停局/玩家重置时调用 reset；自定义货币必须与金币共享同一局生命周期。
        this.annawathe$currencyBalances.clear();
    }

    @Inject(method = "writeToNbt", at = @At("TAIL"))
    private void annawathe$writeCurrencyBalances(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        NbtCompound currencies = new NbtCompound();
        for (Map.Entry<Identifier, Integer> entry : this.annawathe$currencyBalances.entrySet()) {
            if (entry.getValue() > 0) currencies.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("CurrencyBalances", currencies);
    }

    @Inject(method = "readFromNbt", at = @At("TAIL"))
    private void annawathe$readCurrencyBalances(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        this.annawathe$currencyBalances.clear();
        if (!tag.contains("CurrencyBalances", NbtElement.COMPOUND_TYPE)) return;
        NbtCompound currencies = tag.getCompound("CurrencyBalances");
        for (String key : currencies.getKeys()) {
            Identifier currency = Identifier.tryParse(key);
            int amount = currencies.getInt(key);
            if (currency != null && !EconomyApi.MONEY.equals(currency) && amount > 0) {
                this.annawathe$currencyBalances.put(currency, amount);
            }
        }
    }
}
