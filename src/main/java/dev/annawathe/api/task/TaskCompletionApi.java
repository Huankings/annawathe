package dev.annawathe.api.task;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 只在任务真正完成并从任务栏移除后触发，不会把普通 setMood 误判成任务完成。 */
public final class TaskCompletionApi {
    public static final Event<AfterTaskComplete> AFTER_TASK_COMPLETE = EventFactory.createArrayBacked(
            AfterTaskComplete.class, listeners -> context -> { for (AfterTaskComplete listener : listeners) listener.afterTaskComplete(context); });
    private TaskCompletionApi() {}
    public static void dispatch(TaskCompletionContext context) { AFTER_TASK_COMPLETE.invoker().afterTaskComplete(context); }
    @FunctionalInterface public interface AfterTaskComplete { void afterTaskComplete(@NotNull TaskCompletionContext context); }
    public record TaskCompletionContext(@NotNull ServerPlayerEntity player,
                                        @NotNull GameWorldComponent gameWorld,
                                        @Nullable Role role,
                                        @NotNull Identifier taskId,
                                        @Nullable MoodTaskDefinition definition,
                                        boolean rewardedMood) {}
}
