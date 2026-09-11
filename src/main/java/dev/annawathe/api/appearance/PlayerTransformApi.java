package dev.annawathe.api.appearance;

import dev.annawathe.cca.PlayerAppearanceOverrideComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * 服务端调试变形门面。它只改变客户端显示的目标 UUID，不改变职业、阵营、声音、
 * 物品、碰撞或攻击判定，因此可以在局内外安全使用。
 */
public final class PlayerTransformApi {
    private PlayerTransformApi() {}
    /** durationTicks 小于 0 表示永久，否则从当前世界 tick 起计算到期时间。 */
    public static void transform(ServerPlayerEntity player, UUID appearanceUuid, long durationTicks) {
        long expiry = durationTicks < 0 ? -1L : player.getWorld().getTime() + durationTicks;
        PlayerAppearanceOverrideComponent.KEY.get(player).set(appearanceUuid, expiry);
    }
    public static void transformPermanent(ServerPlayerEntity player, UUID appearanceUuid) { transform(player, appearanceUuid, -1L); }
    public static void clear(ServerPlayerEntity player) { PlayerAppearanceOverrideComponent.KEY.get(player).clear(); }
    public static void clearAll(MinecraftServer server) { for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) clear(player); }
    public static boolean isTransformed(ServerPlayerEntity player) { return PlayerAppearanceOverrideComponent.KEY.get(player).isActive(); }
    public static @Nullable UUID getAppearanceUuid(ServerPlayerEntity player) { var c = PlayerAppearanceOverrideComponent.KEY.get(player); return c.isActive() ? c.getTargetUuid() : null; }
}
