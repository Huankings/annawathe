package dev.annawathe.cca;

import dev.annawathe.AnnaWathe;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 世界级心情玩法设置；必须同步到客户端，HUD 才能和服务端死亡开关保持一致。 */
public final class AnnaMoodSettings implements AutoSyncedComponent {
    public static final ComponentKey<AnnaMoodSettings> KEY = ComponentRegistry.getOrCreate(AnnaWathe.id("mood_settings"), AnnaMoodSettings.class);
    private final World world;
    private boolean moodDeathEnabled = true;
    public AnnaMoodSettings(World world) { this.world = world; }
    public boolean isMoodDeathEnabled() { return moodDeathEnabled; }
    public void setMoodDeathEnabled(boolean enabled) { moodDeathEnabled = enabled; if (!world.isClient()) KEY.sync(world); }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        moodDeathEnabled = !tag.contains("MoodDeathEnabled") || tag.getBoolean("MoodDeathEnabled");
    }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) { tag.putBoolean("MoodDeathEnabled", moodDeathEnabled); }
}
