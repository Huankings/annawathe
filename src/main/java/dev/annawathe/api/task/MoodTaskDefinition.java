package dev.annawathe.api.task;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 可注册、保存、同步和渲染的心情任务定义。 */
public final class MoodTaskDefinition {
    private final Identifier id;
    private final String translationKey;
    private final Factory factory;
    private final NbtReader nbtReader;
    private final boolean randomlyAssignable;
    private final float randomWeight;
    private final Set<Identifier> taskPointIds;
    private final @Nullable PlayerMoodComponent.Task legacyTask;

    private MoodTaskDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.translationKey = Objects.requireNonNull(builder.translationKey, "translationKey");
        this.factory = Objects.requireNonNull(builder.factory, "factory");
        this.nbtReader = Objects.requireNonNull(builder.nbtReader, "nbtReader");
        this.randomlyAssignable = builder.randomlyAssignable;
        this.randomWeight = Math.max(0.001F, builder.randomWeight);
        this.taskPointIds = Set.copyOf(builder.taskPointIds);
        this.legacyTask = builder.legacyTask;
    }

    public Identifier id() { return id; }
    public String translationKey() { return translationKey; }
    public boolean randomlyAssignable() { return randomlyAssignable; }
    public float randomWeight() { return randomWeight; }
    public Set<Identifier> taskPointIds() { return taskPointIds; }
    public @Nullable PlayerMoodComponent.Task legacyTask() { return legacyTask; }
    public MoodTaskInstance create(PlayerEntity player) { return factory.create(player); }
    public MoodTaskInstance read(PlayerEntity player, NbtCompound nbt) { return nbtReader.read(player, nbt); }

    public static Builder builder(Identifier id, String translationKey, Factory factory, NbtReader nbtReader) {
        return new Builder(id, translationKey, factory, nbtReader);
    }

    @FunctionalInterface public interface Factory { @NotNull MoodTaskInstance create(@NotNull PlayerEntity player); }
    @FunctionalInterface public interface NbtReader { @NotNull MoodTaskInstance read(@NotNull PlayerEntity player, @NotNull NbtCompound nbt); }

    public static final class Builder {
        private final Identifier id;
        private final String translationKey;
        private final Factory factory;
        private final NbtReader nbtReader;
        private boolean randomlyAssignable;
        private float randomWeight = 1F;
        private final LinkedHashSet<Identifier> taskPointIds = new LinkedHashSet<>();
        private @Nullable PlayerMoodComponent.Task legacyTask;

        private Builder(Identifier id, String translationKey, Factory factory, NbtReader nbtReader) {
            this.id = id; this.translationKey = translationKey; this.factory = factory; this.nbtReader = nbtReader;
        }

        public Builder randomlyAssignable() { this.randomlyAssignable = true; return this; }
        public Builder randomlyAssignable(boolean value) { this.randomlyAssignable = value; return this; }
        public Builder randomWeight(float value) { this.randomWeight = value; return this; }
        public Builder taskPoints(Identifier... ids) { this.taskPointIds.addAll(List.of(ids)); return this; }
        public Builder taskPoints(Collection<Identifier> ids) { this.taskPointIds.addAll(ids); return this; }

        /** 只供 AnnaWathe 适配原版四个枚举任务，新扩展不应依赖此入口。 */
        public Builder legacyTask(PlayerMoodComponent.Task task) { this.legacyTask = task; return this; }
        public MoodTaskDefinition build() { return new MoodTaskDefinition(this); }
    }
}
