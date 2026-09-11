package dev.annawathe.api.client.appearance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
/** 客户端准心名称覆盖链；返回 null 表示当前规则放行给低优先级规则。 */
public final class RoleNameApi {
    private static final List<Entry> RULES = new ArrayList<>();
    private static long order;
    private RoleNameApi() {}
    public static synchronized void registerName(Identifier id, int priority, NameHandler handler) { RULES.removeIf(e -> e.id.equals(id)); RULES.add(new Entry(id, priority, order++, handler)); RULES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Comparator.comparingLong(Entry::order).reversed())); }
    public static @Nullable Text resolve(PlayerEntity viewer, PlayerEntity target, Text original) { List<Entry> snapshot; synchronized (RULES) { snapshot = List.copyOf(RULES); } for (Entry e : snapshot) { Text result = e.handler.resolve(viewer, target, original); if (result != null) return result; } return original; }
    @FunctionalInterface public interface NameHandler { @Nullable Text resolve(PlayerEntity viewer, PlayerEntity target, Text original); }
    private record Entry(Identifier id, int priority, long order, NameHandler handler) {}
}
