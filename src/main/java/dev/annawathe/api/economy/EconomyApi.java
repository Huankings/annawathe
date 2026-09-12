package dev.annawathe.api.economy;

import dev.annawathe.AnnaWathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * AnnaWathe 经济接口。服务端余额读写与客户端 HUD 定义集中在这里，扩展不需要自建同步字段。
 * 任务币定义仍然存在，但默认不注册 HUD、不发放收入，便于未来重新启用而不改变 API。
 */
public final class EconomyApi {
    public static final int DEFAULT_PRIORITY = 0;
    public static final Identifier MONEY = Identifier.of("wathe", "money");
    public static final Identifier TASK_MONEY = Identifier.of("wathe", "task_money");
    public static final String MONEY_ICON = "\uE781";
    public static final String TASK_MONEY_ICON = "\uE782";
    /** 任务币实验当前关闭；收益路径保留，未来启用时只需调整对应数值。 */
    public static final int TASK_MONEY_PER_KILLER_TASK = 0;
    public static final int TASK_MONEY_PER_KILL = 0;
    private static final Map<Identifier, CurrencyDefinition> CURRENCIES = new LinkedHashMap<>();
    private static final Set<Role> HUD_ROLES = new HashSet<>();
    private static final Set<Role> PASSIVE_INCOME_ROLES = new HashSet<>();
    private static final List<PrioritizedHudPredicate> HUD_PREDICATES = new ArrayList<>();
    private static final List<PrioritizedIncomeRule> INCOME_RULES = new ArrayList<>();
    private static final List<PrioritizedIncomeModifier> INCOME_MODIFIERS = new ArrayList<>();
    private static long order;

    static {
        registerCurrency(MONEY, MONEY_ICON, "currency.wathe.money",
                context -> context.gameWorld().canUseKillerFeatures(context.player()) || (context.role() != null && HUD_ROLES.contains(context.role())));
        // 任务币暂时关闭：保留定义和支付逻辑，但不默认出现在 HUD，也不自动产生收益。
        registerCurrency(TASK_MONEY, TASK_MONEY_ICON, "currency.wathe.task_money", context -> false);
    }

    private EconomyApi() {}

    public static synchronized @NotNull CurrencyDefinition registerCurrency(@NotNull Identifier id, @NotNull String icon,
                                                                              @NotNull String translationKey, @NotNull CurrencyHudPredicate predicate) {
        CurrencyDefinition definition = new CurrencyDefinition(Objects.requireNonNull(id), Objects.requireNonNull(icon),
                Objects.requireNonNull(translationKey), order++, Objects.requireNonNull(predicate));
        CURRENCIES.put(id, definition); return definition;
    }
    public static synchronized @Nullable CurrencyDefinition getCurrency(@NotNull Identifier id) { return CURRENCIES.get(id); }
    public static synchronized @NotNull List<CurrencyDefinition> currencySnapshot() { return List.copyOf(CURRENCIES.values()); }
    public static @NotNull CurrencyDefinition getCurrencyOrFallback(@NotNull Identifier id) {
        CurrencyDefinition value = getCurrency(id);
        return value == null ? new CurrencyDefinition(id, "", "currency." + id.getNamespace() + "." + id.getPath(), Long.MAX_VALUE, context -> false) : value;
    }
    public static @NotNull MutableText formatCurrencyAmount(@NotNull CurrencyAmount amount, boolean icon) {
        CurrencyDefinition definition = getCurrencyOrFallback(amount.currency());
        MutableText text = Text.literal(Integer.toString(amount.amount()));
        return icon && !definition.icon().isEmpty() ? text.append(definition.icon()) : text.append(" ").append(Text.translatable(definition.translationKey()));
    }
    public static synchronized void registerBalanceHudRole(@NotNull Role role) { HUD_ROLES.add(role); }
    public static synchronized void registerBalanceHudPredicate(@NotNull Identifier id, int priority, @NotNull BalanceHudPredicate predicate) {
        HUD_PREDICATES.removeIf(e -> e.id().equals(id)); HUD_PREDICATES.add(new PrioritizedHudPredicate(id, priority, order++, predicate));
        HUD_PREDICATES.sort(Comparator.<PrioritizedHudPredicate>comparingInt(PrioritizedHudPredicate::priority).reversed()
                .thenComparing(Comparator.comparingLong(PrioritizedHudPredicate::order).reversed()));
    }
    public static boolean shouldRenderBalanceHud(@NotNull PlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld()); Role role = game.getRole(player);
        if (game.canUseKillerFeatures(player) || role != null && HUD_ROLES.contains(role)) return true;
        synchronized (EconomyApi.class) { for (PrioritizedHudPredicate e : HUD_PREDICATES) if (e.predicate.shouldRender(game, player, role)) return true; }
        return false;
    }
    public static @NotNull List<CurrencyBalance> getVisibleCurrencyBalances(@NotNull PlayerEntity player, boolean shopFallback) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld()); Role role = game.getRole(player);
        List<CurrencyBalance> result = new ArrayList<>();
        for (CurrencyDefinition currency : currencySnapshot()) {
            int amount = PlayerEconomyApi.get(player, currency.id()); if (amount <= 0) continue;
            CurrencyHudContext context = new CurrencyHudContext(game, player, role, currency, amount);
            boolean visible = currency.hudPredicate().shouldRender(context);
            if (!visible && shopFallback && currency.id().equals(MONEY)) visible = true;
            if (visible) result.add(new CurrencyBalance(currency, amount));
        }
        result.sort(Comparator.<CurrencyBalance>comparingInt(CurrencyBalance::amount).reversed().thenComparingLong(v -> v.currency().order())); return result;
    }
    public static synchronized void registerBalanceHudRoles(@NotNull Collection<Role> roles) { roles.forEach(EconomyApi::registerBalanceHudRole); }
    public static synchronized boolean hasRegisteredBalanceHudRole(@NotNull Role role) { return HUD_ROLES.contains(role); }
    public static synchronized void registerPassiveIncomeRole(@NotNull Role role) { PASSIVE_INCOME_ROLES.add(Objects.requireNonNull(role)); }
    public static synchronized void registerPassiveIncomeRoles(@NotNull Collection<Role> roles) { roles.forEach(EconomyApi::registerPassiveIncomeRole); }
    public static synchronized boolean hasRegisteredPassiveIncomeRole(@NotNull Role role) { return PASSIVE_INCOME_ROLES.contains(role); }
    public static synchronized void registerPassiveIncomeRule(@NotNull Identifier id, int priority, @NotNull PassiveIncomeRule rule) {
        INCOME_RULES.removeIf(e -> e.id().equals(id)); INCOME_RULES.add(new PrioritizedIncomeRule(id, priority, order++, rule));
        INCOME_RULES.sort(Comparator.<PrioritizedIncomeRule>comparingInt(PrioritizedIncomeRule::priority).reversed()
                .thenComparing(Comparator.comparingLong(PrioritizedIncomeRule::order).reversed()));
    }
    public static synchronized void registerPassiveIncomeModifier(@NotNull Identifier id, int priority, @NotNull PassiveIncomeModifier modifier) {
        INCOME_MODIFIERS.removeIf(e -> e.id().equals(id)); INCOME_MODIFIERS.add(new PrioritizedIncomeModifier(id, priority, order++, modifier));
        INCOME_MODIFIERS.sort(Comparator.<PrioritizedIncomeModifier>comparingInt(PrioritizedIncomeModifier::priority).reversed()
                .thenComparing(Comparator.comparingLong(PrioritizedIncomeModifier::order).reversed()));
    }
    public static boolean canReceivePassiveIncome(net.minecraft.server.world.ServerWorld world, GameWorldComponent game, net.minecraft.server.network.ServerPlayerEntity player) {
        Role role = game.getRole(player); PassiveIncomeEligibilityContext context = new PassiveIncomeEligibilityContext(world, game, player, role);
        synchronized (EconomyApi.class) { for (PrioritizedIncomeRule e : INCOME_RULES) { PassiveIncomeDecision d = e.rule.getDecision(context); if (d == PassiveIncomeDecision.ALLOW) return true; if (d == PassiveIncomeDecision.DENY) return false; } }
        if (role != null && hasRegisteredPassiveIncomeRole(role)) return true;
        return game.canUseKillerFeatures(player);
    }
    public static int calculatePassiveIncome(net.minecraft.server.world.ServerWorld world, GameWorldComponent game, net.minecraft.server.network.ServerPlayerEntity player, int base) {
        if (base <= 0 || !canReceivePassiveIncome(world, game, player)) return 0; Role role = game.getRole(player); int value = base;
        PassiveIncomeContext context = new PassiveIncomeContext(world, game, player, role, base);
        synchronized (EconomyApi.class) { for (PrioritizedIncomeModifier e : INCOME_MODIFIERS) value = Math.max(0, e.modifier.modifyIncome(context, value)); }
        return value; // Anna 不复制原版上限算法；调用方仍可在最终写入前执行原版阵营上限。
    }
    @FunctionalInterface public interface CurrencyHudPredicate { boolean shouldRender(CurrencyHudContext context); }
    @FunctionalInterface public interface BalanceHudPredicate { boolean shouldRender(GameWorldComponent game, PlayerEntity player, @Nullable Role role); }
    @FunctionalInterface public interface PassiveIncomeRule { PassiveIncomeDecision getDecision(PassiveIncomeEligibilityContext context); }
    @FunctionalInterface public interface PassiveIncomeModifier { int modifyIncome(PassiveIncomeContext context, int currentIncome); }
    public enum PassiveIncomeDecision { PASS, ALLOW, DENY }
    public record CurrencyHudContext(GameWorldComponent gameWorld, PlayerEntity player, @Nullable Role role, CurrencyDefinition currency, int amount) {}
    public record CurrencyBalance(CurrencyDefinition currency, int amount) {}
    public record PassiveIncomeEligibilityContext(net.minecraft.server.world.ServerWorld world, GameWorldComponent gameWorld, net.minecraft.server.network.ServerPlayerEntity player, @Nullable Role role) {}
    public record PassiveIncomeContext(net.minecraft.server.world.ServerWorld world, GameWorldComponent gameWorld, net.minecraft.server.network.ServerPlayerEntity player, @Nullable Role role, int baseIncome) {}
    private record PrioritizedHudPredicate(Identifier id, int priority, long order, BalanceHudPredicate predicate) {}
    private record PrioritizedIncomeRule(Identifier id, int priority, long order, PassiveIncomeRule rule) {}
    private record PrioritizedIncomeModifier(Identifier id, int priority, long order, PassiveIncomeModifier modifier) {}
}
