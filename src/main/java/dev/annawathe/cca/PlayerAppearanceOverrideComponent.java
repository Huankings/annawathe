package dev.annawathe.cca;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import java.util.UUID;

/** 调试变形状态。它不是职业状态，所以跨回合、重生和重启保留，直到显式清除。 */
public final class PlayerAppearanceOverrideComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerAppearanceOverrideComponent> KEY = ComponentRegistry.getOrCreate(dev.annawathe.AnnaWathe.id("appearance_override"), PlayerAppearanceOverrideComponent.class);
    private final PlayerEntity player;
    private @Nullable UUID targetUuid;
    private long expiresAt = -1L;
    public PlayerAppearanceOverrideComponent(PlayerEntity player) { this.player = player; }
    /** 变形成谁；UUID 而不是 Player 实例，保证组件可安全保存到 NBT。 */
    public @Nullable UUID getTargetUuid() { return targetUuid; }
    /** 到期世界 tick；-1 表示永久。 */
    public long getExpiresAt() { return expiresAt; }
    /** 只有目标存在且未到期才算有效，过期状态不会继续覆盖皮肤。 */
    public boolean isActive() { return targetUuid != null && (expiresAt < 0 || player.getWorld().getTime() < expiresAt); }
    /** 写入/覆盖变形目标；客户端只接收同步结果，不参与合法性判断。 */
    public void set(@Nullable UUID target, long expiry) { targetUuid = target; expiresAt = target == null ? -1L : expiry; sync(); }
    /** 显式解除变形。 */
    public void clear() { set(null, -1L); }
    /** 服务端每 tick 回收有限时长变形，永久变形不会被自动清除。 */
    public void tickServer() { if (targetUuid != null && expiresAt >= 0 && player.getWorld().getTime() >= expiresAt) clear(); }
    /** 变形状态属于玩家视觉数据，必须同步给观察者客户端。 */
    private void sync() { if (!player.getWorld().isClient) KEY.sync(player); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { if (targetUuid != null) tag.putUuid("Target", targetUuid); tag.putLong("ExpiresAt", expiresAt); }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { targetUuid = tag.containsUuid("Target") ? tag.getUuid("Target") : null; expiresAt = tag.contains("ExpiresAt") ? tag.getLong("ExpiresAt") : -1L; }
}
