package dev.annawathe.api.task;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.annawathe.api.economy.EconomyApi;
import dev.annawathe.api.economy.PlayerEconomyApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 只在任务真正完成并从任务栏移除后触发，不会把普通 setMood 误判成任务完成。 */
public final class TaskCompletionApi {
    public static final int DEFAULT_PRIORITY = 0;
    public static final Event<AfterTaskComplete> AFTER_TASK_COMPLETE = EventFactory.createArrayBacked(
            AfterTaskComplete.class, listeners -> context -> { for (AfterTaskComplete listener : listeners) listener.afterTaskComplete(context); });
    private static final List<IncomeProviderEntry> INCOME_PROVIDERS = new ArrayList<>();
    private static final List<IncomeRuleEntry> INCOME_RULES = new ArrayList<>();
    private static long order;
    private TaskCompletionApi() {}

    public static synchronized void registerTaskIncomeProvider(Identifier id, int priority, TaskIncomeProvider provider) {
        Objects.requireNonNull(id); Objects.requireNonNull(provider);
        INCOME_PROVIDERS.removeIf(entry -> entry.id().equals(id));
        INCOME_PROVIDERS.add(new IncomeProviderEntry(id, priority, order++, provider));
        INCOME_PROVIDERS.sort(Comparator.<IncomeProviderEntry>comparingInt(IncomeProviderEntry::priority).reversed()
                .thenComparing(Comparator.comparingLong(IncomeProviderEntry::order).reversed()));
    }

    public static synchronized void registerTaskIncomeRule(Identifier id, int priority, TaskIncomeRule rule) {
        Objects.requireNonNull(id); Objects.requireNonNull(rule);
        INCOME_RULES.removeIf(entry -> entry.id().equals(id));
        INCOME_RULES.add(new IncomeRuleEntry(id, priority, order++, rule));
        INCOME_RULES.sort(Comparator.<IncomeRuleEntry>comparingInt(IncomeRuleEntry::priority).reversed()
                .thenComparing(Comparator.comparingLong(IncomeRuleEntry::order).reversed()));
    }

    public static void dispatch(TaskCompletionContext context) {
        if (!suppressDefaultIncome(context)) {
            if (context.gameWorld().canUseKillerFeatures(context.player())) {
                if (EconomyApi.TASK_MONEY_PER_KILLER_TASK > 0) {
                    PlayerEconomyApi.add(context.player(), EconomyApi.TASK_MONEY, EconomyApi.TASK_MONEY_PER_KILLER_TASK);
                }
            } else {
                int income = 0;
                synchronized (TaskCompletionApi.class) {
                    for (IncomeProviderEntry entry : INCOME_PROVIDERS) {
                        income += Math.max(0, entry.provider().getTaskIncome(context));
                    }
                }
                if (income > 0) PlayerEconomyApi.add(context.player(), EconomyApi.MONEY, income);
            }
        }
        AFTER_TASK_COMPLETE.invoker().afterTaskComplete(context);
    }

    private static boolean suppressDefaultIncome(TaskCompletionContext context) {
        synchronized (TaskCompletionApi.class) {
            for (IncomeRuleEntry entry : INCOME_RULES) {
                if (entry.rule().getDecision(context) == TaskIncomeDecision.SUPPRESS_DEFAULT_INCOME) return true;
            }
        }
        return false;
    }
    @FunctionalInterface public interface AfterTaskComplete { void afterTaskComplete(@NotNull TaskCompletionContext context); }
    @FunctionalInterface public interface TaskIncomeProvider { int getTaskIncome(@NotNull TaskCompletionContext context); }
    @FunctionalInterface public interface TaskIncomeRule { @NotNull TaskIncomeDecision getDecision(@NotNull TaskCompletionContext context); }
    public enum TaskIncomeDecision { PASS, SUPPRESS_DEFAULT_INCOME }
    public record TaskCompletionContext(@NotNull ServerPlayerEntity player,
                                        @NotNull GameWorldComponent gameWorld,
                                        @Nullable Role role,
                                        @NotNull Identifier taskId,
                                        @Nullable MoodTaskDefinition definition,
                                        boolean rewardedMood) {}
    private record IncomeProviderEntry(Identifier id, int priority, long order, TaskIncomeProvider provider) {}
    private record IncomeRuleEntry(Identifier id, int priority, long order, TaskIncomeRule rule) {}
}
