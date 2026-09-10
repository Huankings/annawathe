package dev.annawathe.mood;

import dev.annawathe.api.task.MoodTaskInstance;
import dev.doctor4t.wathe.block.entity.SeatEntity;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

/** AnnaWathe 内置任务实例；服务端负责推进，客户端只通过 NBT 获得显示状态。 */
public final class BuiltInMoodTasks {
    private BuiltInMoodTasks() {}

    public static final class Legacy implements MoodTaskInstance {
        private final PlayerMoodComponent.TrainTask delegate;
        public Legacy(PlayerMoodComponent.TrainTask delegate) { this.delegate = delegate; }
        public PlayerMoodComponent.TrainTask delegate() { return delegate; }
        @Override public void tick(@NotNull PlayerEntity player) { delegate.tick(player); }
        @Override public boolean isFulfilled(@NotNull PlayerEntity player) { return delegate.isFulfilled(player); }
        @Override public @NotNull NbtCompound writeNbt() { return delegate.toNbt(); }
    }

    public abstract static class Timed implements MoodTaskInstance {
        protected int timer;
        protected Timed(int timer) { this.timer = timer; }
        protected abstract boolean shouldCount(PlayerEntity player);
        @Override public void tick(@NotNull PlayerEntity player) { if (timer > 0 && shouldCount(player)) timer--; }
        @Override public boolean isFulfilled(@NotNull PlayerEntity player) { return timer <= 0; }
        @Override public @NotNull NbtCompound writeNbt() { NbtCompound nbt = new NbtCompound(); nbt.putInt("timer", timer); return nbt; }
    }

    public static final class Shift extends Timed {
        public Shift(int timer) { super(timer); }
        @Override protected boolean shouldCount(PlayerEntity player) { return player.isSneaking(); }
    }
    public static final class Run extends Timed {
        public Run(int timer) { super(timer); }
        @Override protected boolean shouldCount(PlayerEntity player) { return player.isSprinting(); }
    }
    public static final class Sit extends Timed {
        public Sit(int timer) { super(timer); }
        @Override protected boolean shouldCount(PlayerEntity player) { return player.hasVehicle() && player.getVehicle() instanceof SeatEntity; }
    }
    public static final class Away extends Timed {
        public Away(int timer) { super(timer); }
        @Override protected boolean shouldCount(PlayerEntity player) {
            double rangeSq = MoodTaskState.AWAY_RANGE * MoodTaskState.AWAY_RANGE;
            for (PlayerEntity target : player.getWorld().getPlayers()) {
                if (target != player && GameFunctions.isPlayerAliveAndSurvival(target)
                        && target.squaredDistanceTo(player) <= rangeSq) return false;
            }
            return true;
        }
    }
    public static final class Stay implements MoodTaskInstance {
        private static final double EPSILON_SQUARED = 1.0E-4;
        private int timer;
        private double x, y, z;
        private boolean hasPosition;
        public Stay(int timer) { this.timer = timer; }
        @Override public void tick(@NotNull PlayerEntity player) {
            if (hasPosition) {
                double dx = player.getX() - x, dy = player.getY() - y, dz = player.getZ() - z;
                if (dx * dx + dy * dy + dz * dz <= EPSILON_SQUARED && (player.hasVehicle() || player.isOnGround()) && timer > 0) timer--;
            }
            x = player.getX(); y = player.getY(); z = player.getZ(); hasPosition = true;
        }
        @Override public boolean isFulfilled(@NotNull PlayerEntity player) { return timer <= 0; }
        @Override public @NotNull NbtCompound writeNbt() { NbtCompound nbt = new NbtCompound(); nbt.putInt("timer", timer); return nbt; }
    }
}
