package dev.annawathe.cca;

import dev.annawathe.AnnaWathe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 保存每位玩家的本能键偏好；这是个人设置，重生时复制但不会影响其他玩家。 */
public final class PlayerInstinctComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerInstinctComponent> KEY = ComponentRegistry.getOrCreate(AnnaWathe.id("instinct"), PlayerInstinctComponent.class);
    private final PlayerEntity player;
    private boolean toggleModeEnabled = true;
    public PlayerInstinctComponent(PlayerEntity player) { this.player = player; }
    public boolean isToggleModeEnabled() { return toggleModeEnabled; }
    public void setToggleModeEnabled(boolean value) { toggleModeEnabled = value; if (!player.getWorld().isClient) KEY.sync(player); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { tag.putBoolean("toggleModeEnabled", toggleModeEnabled); }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { toggleModeEnabled = !tag.contains("toggleModeEnabled") || tag.getBoolean("toggleModeEnabled"); }
}
