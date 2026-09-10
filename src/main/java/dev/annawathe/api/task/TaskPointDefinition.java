package dev.annawathe.api.task;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** 一个可在世界透视中显示的任务点类型。 */
public record TaskPointDefinition(@NotNull Identifier id, @NotNull String translationKey, int color) {
    public TaskPointDefinition { Objects.requireNonNull(id); Objects.requireNonNull(translationKey); }
}
