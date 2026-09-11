package dev.annawathe.api.movement;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** 只负责最终速度修正；不额外引入自改 Wathe 的体力系统。 */
/**
 * 速度修正规则链。Anna 只迁移速度叠加，不接管自改 Wathe 的体力和跳跃惩罚。
 */
public final class PlayerMovementApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry> MODIFIERS = new ArrayList<>();
    private static long order;
    private PlayerMovementApi() {}
    public static void registerSpeedModifier(Identifier id, int priority, MovementSpeedModifier modifier) {
        Objects.requireNonNull(id); Objects.requireNonNull(modifier);
        synchronized (MODIFIERS) { MODIFIERS.removeIf(e -> e.id.equals(id)); MODIFIERS.add(new Entry(id, priority, order++, modifier)); MODIFIERS.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Comparator.comparingLong(Entry::order).reversed())); }
    }
    /** 按 priority 依次把 ADD/MULTIPLY/OVERRIDE 应用到当前速度。 */
    public static float resolveMovementSpeed(PlayerEntity player, float vanillaSpeed, float baseSpeed) {
        float current = Math.max(0F, baseSpeed);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        List<Entry> snapshot; synchronized (MODIFIERS) { snapshot = List.copyOf(MODIFIERS); }
        for (Entry entry : snapshot) {
            MovementSpeedResult result = entry.modifier.modify(new MovementSpeedContext(player, game, role, player.isSprinting(), vanillaSpeed, baseSpeed, current));
            if (result == null || result.operation == MovementSpeedResult.Operation.PASS || !Float.isFinite(result.value)) continue;
            current = switch (result.operation) {
                case ADD -> current + result.value;
                case MULTIPLY -> current * result.value;
                case OVERRIDE -> result.value;
                case PASS -> current;
            };
        }
        return Math.max(0F, current);
    }
    /** Anna 第一阶段不接管体力，默认允许自主移动与跳跃。 */
    public static boolean canSelfMove(PlayerEntity player) { return true; }
    public static boolean canJump(PlayerEntity player) { return true; }
    @FunctionalInterface public interface MovementSpeedModifier { MovementSpeedResult modify(MovementSpeedContext context); }
    public record MovementSpeedContext(PlayerEntity player, GameWorldComponent gameWorld, @Nullable Role role, boolean sprinting, float vanillaSpeed, float baseSpeed, float currentSpeed) {}
    public record MovementSpeedResult(Operation operation, float value) {
        public static MovementSpeedResult pass() { return new MovementSpeedResult(Operation.PASS, 0F); }
        public static MovementSpeedResult add(float value) { return new MovementSpeedResult(Operation.ADD, value); }
        public static MovementSpeedResult multiply(float value) { return new MovementSpeedResult(Operation.MULTIPLY, value); }
        public static MovementSpeedResult override(float value) { return new MovementSpeedResult(Operation.OVERRIDE, value); }
        public enum Operation { PASS, ADD, MULTIPLY, OVERRIDE }
    }
    private record Entry(Identifier id, int priority, long order, MovementSpeedModifier modifier) {}
}
