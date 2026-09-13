package dev.annawathe.cca;

import dev.annawathe.AnnaWathe;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 尸体的服务端快照：死亡原因、死亡瞬间职业和死亡世界时间。 */
public final class AnnaBodyInfoComponent implements AutoSyncedComponent {
    public static final ComponentKey<AnnaBodyInfoComponent> KEY = ComponentRegistry.getOrCreate(AnnaWathe.id("body_info"), AnnaBodyInfoComponent.class);
    private final PlayerBodyEntity body;
    private Identifier deathReason = Identifier.of("wathe", "generic");
    private @Nullable Identifier roleId;
    private long deathWorldTime = -1L;
    public AnnaBodyInfoComponent(PlayerBodyEntity body) { this.body = body; }
    public Identifier getDeathReason() { return deathReason; }
    public void setDeathReason(Identifier value) { deathReason = value == null ? Identifier.of("wathe", "generic") : value; }
    public @Nullable Identifier getRoleId() { return roleId; }
    public void setRoleId(@Nullable Identifier value) { roleId = value; }
    public long getDeathWorldTime() { return deathWorldTime; }
    public void setDeathWorldTime(long value) { deathWorldTime = value; }
    public void sync() { if (!body.getWorld().isClient) KEY.sync(body); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putString("DeathReason", deathReason.toString());
        if (roleId != null) tag.putString("RoleId", roleId.toString());
        tag.putLong("DeathWorldTime", deathWorldTime);
    }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        Identifier reason = Identifier.tryParse(tag.getString("DeathReason"));
        deathReason = reason == null ? Identifier.of("wathe", "generic") : reason;
        roleId = tag.contains("RoleId") ? Identifier.tryParse(tag.getString("RoleId")) : null;
        deathWorldTime = tag.contains("DeathWorldTime") ? tag.getLong("DeathWorldTime") : -1L;
    }
}
