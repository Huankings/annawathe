package dev.annawathe.api.task;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

/**
 * 单个玩家身上的心情任务运行时实例。
 *
 * <p>该接口由 AnnaWathe 自己定义，不能继续复用原版 Wathe 的 TrainTask：
 * 原版接口要求返回不可扩展的 Task 枚举，扩展 Mod 因而无法声明新任务。</p>
 */
public interface MoodTaskInstance {
    default void tick(@NotNull PlayerEntity player) {
    }

    boolean isFulfilled(@NotNull PlayerEntity player);

    @NotNull NbtCompound writeNbt();
}
