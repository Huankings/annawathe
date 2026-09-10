package dev.annawathe.cca;

import dev.annawathe.AnnaWathe;
import dev.annawathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import java.util.*;

/** annawathe 自己的结算旁路状态，避免给原版 GameRoundEndComponent 添加字段。 */
public final class AnnaRoundEndState implements AutoSyncedComponent {
    public static final ComponentKey<AnnaRoundEndState> KEY = ComponentRegistry.getOrCreate(AnnaWathe.id("round_state"), AnnaRoundEndState.class);
    private final @Nullable World world;
    private final @Nullable Scoreboard scoreboard;
    private @Nullable CustomVictory customVictory;
    private final Set<UUID> extraWinners = new HashSet<>();
    /**
     * 结算时的职业快照。原版 Wathe 的 GameRoundEndComponent 只保存平民/义警/杀手占位，
     * HarpyModLoader 的扩展职业会在这里丢失；因此必须在角色表清理前复制一份到 Anna 状态。
     * 该数据只服务于结算界面显示，不参与服务端胜利判定，也不替代当前对局角色表。
     */
    private final Map<UUID, EndRoleSnapshot> roleSnapshots = new HashMap<>();
    public AnnaRoundEndState(World world) { this.world = world; this.scoreboard = null; }
    public AnnaRoundEndState(Scoreboard scoreboard) { this.world = null; this.scoreboard = scoreboard; }
    public AnnaRoundEndState(Scoreboard scoreboard, MinecraftServer server) { this.world = null; this.scoreboard = scoreboard; }
    public void setCustomVictory(@Nullable CustomVictory value) { customVictory = value; sync(); }
    public @Nullable CustomVictory getCustomVictory() { return customVictory; }
    public Set<UUID> getExtraWinners() { return Set.copyOf(extraWinners); }
    public void setExtraWinners(Collection<UUID> values) { extraWinners.clear(); extraWinners.addAll(values); sync(); }
    public Map<UUID, EndRoleSnapshot> getRoleSnapshots() { return Map.copyOf(roleSnapshots); }

    /** 在原版结算组件写入完成后调用，保存本局每名玩家的真实职业和布局阵营。 */
    public void captureRoleSnapshots(Collection<? extends net.minecraft.server.network.ServerPlayerEntity> players,
                                     GameWorldComponent game,
                                     GameRoundEndComponentFallback fallback) {
        roleSnapshots.clear();
        for (var player : players) {
            Role role = game.getRole(player.getUuid());
            EndRoleGroup group = classify(role, fallback.groupFor(player.getUuid()));
            if (role != null) {
                roleSnapshots.put(player.getUuid(), new EndRoleSnapshot(role.identifier(), role.color(), group));
            } else {
                roleSnapshots.put(player.getUuid(), new EndRoleSnapshot(null, fallback.colorFor(player.getUuid()), group));
            }
        }
        sync();
    }

    /** 清空上一局快照，避免新局或离开世界后误显示旧职业。 */
    public void reset() { customVictory = null; extraWinners.clear(); roleSnapshots.clear(); sync(); }

    private static EndRoleGroup classify(@Nullable Role role, EndRoleGroup fallback) {
        if (role == null) return fallback;
        if (role == WatheRoles.LOOSE_END) return EndRoleGroup.LOOSE_END;
        if (role.canUseKiller()) return EndRoleGroup.KILLER;
        if (role == WatheRoles.VIGILANTE) return EndRoleGroup.VIGILANTE;
        if (!role.isInnocent()) return EndRoleGroup.NEUTRAL;
        return EndRoleGroup.CIVILIAN;
    }

    public enum EndRoleGroup { CIVILIAN, VIGILANTE, KILLER, NEUTRAL, LOOSE_END, UNKNOWN }

    public record EndRoleSnapshot(@Nullable Identifier roleId, int color, EndRoleGroup group) {}

    /** 原版占位信息，仅在极端情况下角色表缺失时提供降级分组和颜色。 */
    public interface GameRoundEndComponentFallback {
        EndRoleGroup groupFor(UUID uuid);
        int colorFor(UUID uuid);
    }
    private void sync() { if (world != null) KEY.sync(world); else if (scoreboard != null) KEY.sync(scoreboard); }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        if (customVictory != null) tag.put("customVictory", customVictory.writeToNbt());
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (UUID id : extraWinners) list.add(net.minecraft.nbt.NbtHelper.fromUuid(id));
        tag.put("extraWinners", list);
        net.minecraft.nbt.NbtList snapshots = new net.minecraft.nbt.NbtList();
        for (var entry : roleSnapshots.entrySet()) {
            NbtCompound snapshot = new NbtCompound();
            snapshot.putUuid("uuid", entry.getKey());
            EndRoleSnapshot value = entry.getValue();
            if (value.roleId() != null) snapshot.putString("role", value.roleId().toString());
            snapshot.putInt("color", value.color());
            snapshot.putInt("group", value.group().ordinal());
            snapshots.add(snapshot);
        }
        tag.put("roleSnapshots", snapshots);
    }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        customVictory = tag.contains("customVictory") ? CustomVictory.fromNbt(tag.getCompound("customVictory")) : null;
        extraWinners.clear();
        for (var e : tag.getList("extraWinners", net.minecraft.nbt.NbtElement.INT_ARRAY_TYPE)) extraWinners.add(net.minecraft.nbt.NbtHelper.toUuid(e));
        roleSnapshots.clear();
        for (var element : tag.getList("roleSnapshots", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) {
            NbtCompound snapshot = (NbtCompound) element;
            Identifier id = snapshot.contains("role") ? Identifier.tryParse(snapshot.getString("role")) : null;
            int ordinal = snapshot.getInt("group");
            EndRoleGroup[] groups = EndRoleGroup.values();
            EndRoleGroup group = ordinal >= 0 && ordinal < groups.length ? groups[ordinal] : EndRoleGroup.UNKNOWN;
            roleSnapshots.put(snapshot.getUuid("uuid"), new EndRoleSnapshot(id, snapshot.getInt("color"), group));
        }
    }
}
