package dev.annawathe.api.body;

import dev.annawathe.cca.AnnaBodyInfoComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 尸体快照的服务端门面；扩展可注册死亡瞬间身份解析器。 */
public final class BodyInfoApi {
    private static final List<Entry> ROLE_RESOLVERS = new ArrayList<>();
    private static long order;
    private BodyInfoApi() {}
    public static synchronized void registerRoleResolver(Identifier id, int priority, RoleResolver resolver) {
        ROLE_RESOLVERS.removeIf(e -> e.id.equals(id));
        ROLE_RESOLVERS.add(new Entry(id, priority, order++, resolver));
        ROLE_RESOLVERS.sort(Comparator.comparingInt((Entry e) -> e.priority).reversed().thenComparing(Comparator.comparingLong((Entry e) -> e.order).reversed()));
    }
    public static void initializeBody(@NotNull PlayerBodyEntity body, @NotNull PlayerEntity victim, @NotNull Identifier deathReason) {
        AnnaBodyInfoComponent component = AnnaBodyInfoComponent.KEY.get(body);
        component.setDeathReason(deathReason);
        component.setDeathWorldTime(victim.getWorld().getTime());
        component.setRoleId(resolveRole(victim));
        component.sync();
    }
    public static BodyInfoSnapshot get(@NotNull PlayerBodyEntity body) { return BodyInfoSnapshot.from(body); }
    public static void setDeathReason(@NotNull PlayerBodyEntity body, @NotNull Identifier reason) { AnnaBodyInfoComponent.KEY.get(body).setDeathReason(reason); }
    public static void setRoleId(@NotNull PlayerBodyEntity body, @Nullable Identifier roleId) { AnnaBodyInfoComponent.KEY.get(body).setRoleId(roleId); }
    public static void setDeathWorldTime(@NotNull PlayerBodyEntity body, long worldTime) { AnnaBodyInfoComponent.KEY.get(body).setDeathWorldTime(worldTime); }
    public static void sync(@NotNull PlayerBodyEntity body) { AnnaBodyInfoComponent.KEY.get(body).sync(); }
    private static Identifier resolveRole(PlayerEntity victim) {
        List<Entry> entries;
        synchronized (ROLE_RESOLVERS) { entries = List.copyOf(ROLE_RESOLVERS); }
        for (Entry entry : entries) { Identifier result = entry.resolver.resolve(victim); if (result != null) return result; }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role role = game.getRole(victim);
        return role == null ? null : role.identifier();
    }
    @FunctionalInterface public interface RoleResolver { @Nullable Identifier resolve(PlayerEntity victim); }
    private record Entry(Identifier id, int priority, long order, RoleResolver resolver) {}
}
