package dev.annawathe.cca;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 只保存一枚临时授权标记；死亡/重生不会复制。 */
/**
 * 玩家“非生存模式仍按局内存活”授权。
 * 这是临时玩法状态，死亡/重生不复制；服务端写入后同步给客户端显示逻辑。
 */
public final class PlayerLifeStateComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerLifeStateComponent> KEY =
            ComponentRegistry.getOrCreate(dev.annawathe.AnnaWathe.id("life_state"), PlayerLifeStateComponent.class);
    private static final String KEY_ALIVE = "AliveInNonSurvivalMode";
    private final PlayerEntity player;
    private boolean aliveInNonSurvivalMode;

    public PlayerLifeStateComponent(PlayerEntity player) { this.player = player; }
    public boolean isAliveInNonSurvivalMode() { return aliveInNonSurvivalMode; }
    public void setAliveInNonSurvivalMode(boolean value) {
        if (aliveInNonSurvivalMode == value) return;
        aliveInNonSurvivalMode = value;
        if (!player.getWorld().isClient) KEY.sync(player);
    }
    public void clearAliveInNonSurvivalMode() { setAliveInNonSurvivalMode(false); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { tag.putBoolean(KEY_ALIVE, aliveInNonSurvivalMode); }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { aliveInNonSurvivalMode = tag.getBoolean(KEY_ALIVE); }
}
