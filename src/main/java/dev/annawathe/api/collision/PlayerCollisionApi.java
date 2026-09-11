package dev.annawathe.api.collision;

import dev.annawathe.cca.AnnaCollisionSettings;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * 玩家物理碰撞的统一注册与解析入口。
 * 规则优先级越大越先执行，同优先级后注册者优先；推挤判定会同时解析两个方向。
 */
public final class PlayerCollisionApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry> RULES = new ArrayList<>();
    private static long order;
    private PlayerCollisionApi() {}
    public static void registerRule(Identifier id, int priority, Rule rule) {
        Objects.requireNonNull(id); Objects.requireNonNull(rule);
        synchronized (RULES) { RULES.removeIf(e -> e.id.equals(id)); RULES.add(new Entry(id, priority, order++, rule)); RULES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Comparator.comparingLong(Entry::order).reversed())); }
    }
    /** 解析移动方向上的最终碰撞模式。 */
    public static PlayerCollisionMode resolve(PlayerEntity self, PlayerEntity other) {
        if (self.getWorld() != other.getWorld()) return PlayerCollisionMode.PASS;
        GameWorldComponent game = GameWorldComponent.KEY.get(self.getWorld());
        AnnaCollisionSettings settings = AnnaCollisionSettings.KEY.get(self.getWorld());
        PlayerCollisionContext context = new PlayerCollisionContext(self, other, self.getWorld(), game, settings);
        List<Entry> snapshot; synchronized (RULES) { snapshot = List.copyOf(RULES); }
        for (Entry entry : snapshot) { PlayerCollisionMode result = entry.rule.resolve(context); if (result != null && result != PlayerCollisionMode.PASS) return result; }
        if (!game.isRunning() || !settings.isEnabled() || !GameFunctions.isPlayerAliveAndSurvival(self) || !GameFunctions.isPlayerAliveAndSurvival(other)) return PlayerCollisionMode.PASS;
        return settings.isStartDelayActive() ? PlayerCollisionMode.VANILLA_PUSH : PlayerCollisionMode.SOLID;
    }
    public static boolean blocksMovement(PlayerEntity self, PlayerEntity other) { return resolve(self, other).blocksMovement(); }
    public static boolean suppressesPush(PlayerEntity self, PlayerEntity other) { return !resolve(self, other).allowsVanillaPush() || !resolve(other, self).allowsVanillaPush(); }
    @FunctionalInterface public interface Rule { PlayerCollisionMode resolve(PlayerCollisionContext context); }
    private record Entry(Identifier id, int priority, long order, Rule rule) {}
}
