package dev.annawathe.cca;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 原版 GameWorldComponent 没有碰撞配置，因此单独保存并同步。 */
public final class AnnaCollisionSettings implements AutoSyncedComponent {
    public static final ComponentKey<AnnaCollisionSettings> KEY = ComponentRegistry.getOrCreate(dev.annawathe.AnnaWathe.id("collision_settings"), AnnaCollisionSettings.class);
    private final World world;
    private boolean enabled = true;
    private int startDelaySeconds = 30;
    private long roundStartTick = -1L;
    public AnnaCollisionSettings(World world) { this.world = world; }
    /** 返回局内存活玩家碰撞总开关。该值保存于世界，重启后仍保留。 */
    public boolean isEnabled() { return enabled; }
    /** 修改碰撞总开关，并立即向客户端同步，保证客户端预测和服务端判断一致。 */
    public void setEnabled(boolean value) { enabled = value; sync(); }
    /** 返回 ACTIVE 开始后的免碰撞保护秒数。 */
    public int getStartDelaySeconds() { return startDelaySeconds; }
    /** 设置保护秒数；负值归零，0 表示进入 ACTIVE 后立即启用碰撞。 */
    public void setStartDelaySeconds(int value) { startDelaySeconds = Math.max(0, value); sync(); }
    /** 记录本局 ACTIVE 起点；不能在开局指令执行时提前计时。 */
    public void markRoundStart(long tick) { roundStartTick = tick; sync(); }
    /** 停局时清除上一局起点，避免下一局误用旧时间。 */
    public void clearRoundStart() { roundStartTick = -1L; sync(); }
    public boolean isStartDelayActive() {
        if (roundStartTick < 0 || startDelaySeconds <= 0) return false;
        long elapsed = Math.max(0L, world.getTime() - roundStartTick);
        return elapsed < startDelaySeconds * 20L;
    }
    private void sync() { if (!world.isClient) KEY.sync(world); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { tag.putBoolean("Enabled", enabled); tag.putInt("StartDelaySeconds", startDelaySeconds); tag.putLong("RoundStartTick", roundStartTick); }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled"); startDelaySeconds = Math.max(0, tag.contains("StartDelaySeconds") ? tag.getInt("StartDelaySeconds") : 30); roundStartTick = tag.contains("RoundStartTick") ? tag.getLong("RoundStartTick") : -1L; }
}
