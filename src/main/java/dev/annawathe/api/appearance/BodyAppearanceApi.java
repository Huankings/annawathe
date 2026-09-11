package dev.annawathe.api.appearance;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * 尸体生成时的外观解析链。返回值只表示尸体“看起来像谁”，
 * 不会修改 PlayerBodyEntity 的真实 owner UUID。
 */
public final class BodyAppearanceApi {
    private static final List<Entry> RULES = new ArrayList<>();
    private static long order;
    private BodyAppearanceApi() {}
    public static synchronized void register(Identifier id, int priority, Handler handler) { Objects.requireNonNull(id); Objects.requireNonNull(handler); RULES.removeIf(e -> e.id.equals(id)); RULES.add(new Entry(id, priority, order++, handler)); RULES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Comparator.comparingLong(Entry::order).reversed())); }
    public static @Nullable UUID resolveAppearanceUuid(PlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason) { List<Entry> snapshot; synchronized (RULES) { snapshot = List.copyOf(RULES); } for (Entry e : snapshot) { UUID value = e.handler.resolve(victim, killer, reason); if (value != null) return value; } return null; }
    @FunctionalInterface public interface Handler { @Nullable UUID resolve(PlayerEntity victim, @Nullable PlayerEntity killer, Identifier deathReason); }
    private record Entry(Identifier id, int priority, long order, Handler handler) {}
}
