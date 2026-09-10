package dev.annawathe.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 兼容原版 HarpyModLoader 的中立 forceRole 分配。
 * 原版在随机中立槽位达到上限后直接跳过职业，导致后续强制中立职业落回 CIVILIAN。
 * 本 Mixin 在中立列表洗牌完成后先处理强制中立玩家，再让原版随机分配继续；禁用职业仍不会被强制启用。
 */
@Mixin(targets = "org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode")
public abstract class HarpyNeutralForceRoleMixin {
    private static final String HARPY = "org.agmas.harpymodloader.Harpymodloader";

    @Inject(method = "assignCivilianReplacingRoles", at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V", ordinal = 1, shift = At.Shift.AFTER), remap = false)
    private void annawathe$forceNeutral(
            int desiredRoleCount,
            ServerWorld world,
            GameWorldComponent game,
            List<ServerPlayerEntity> players,
            CallbackInfo ci,
            /* 原版 Harpy 方法此处存在三个 ArrayList 局部变量：
             * 0=平民职业列表，1=中立职业列表，2=基础职业候选玩家列表。
             * 必须显式指定 ordinal，不能使用隐式 @Local，否则 MixinExtras 会报
             * Found 3 candidate variables but exactly 1 is required。
             */
            @Local(ordinal = 1) ArrayList<Role> shuffledNeutralRoles,
            @Local(ordinal = 2) ArrayList<ServerPlayerEntity> playersForCivillianRoles) {
        Map<Role, List<UUID>> forced = forcedRoles();
        if (forced == null || shuffledNeutralRoles == null || playersForCivillianRoles == null) return;
        for (Role role : new ArrayList<>(shuffledNeutralRoles)) {
            // 与自改版保持一致：disabled 只关闭随机生成，不阻止管理员明确 forceRole。
            if (!isNeutral(role)) continue;
            List<UUID> forcedUuids = forced.get(role);
            if (forcedUuids == null || forcedUuids.isEmpty()) continue;
            ArrayList<ServerPlayerEntity> forcedPlayers = new ArrayList<>();
            for (ServerPlayerEntity player : playersForCivillianRoles) {
                if (forcedUuids.contains(player.getUuid()) && isOverwriteBaseRole(game.getRole(player))) forcedPlayers.add(player);
            }
            if (forcedPlayers.isEmpty()) continue;
            invokeFindAndAssignPlayers(forcedPlayers.size(), role, forcedPlayers, game, world);
            playersForCivillianRoles.removeAll(forcedPlayers);
            shuffledNeutralRoles.remove(role);
        }
    }

    /** 通过反射调用 Harpy 原版私有分配函数，避免把可选目标方法写入 Anna 的重映射表。 */
    private static int invokeFindAndAssignPlayers(int desiredRoleCount, Role role, List<ServerPlayerEntity> players, GameWorldComponent game, World world) {
        try {
            Class<?> type = Class.forName("org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode");
            var method = type.getDeclaredMethod("findAndAssignPlayers", int.class, Role.class, List.class, GameWorldComponent.class, World.class);
            method.setAccessible(true);
            return ((Number) method.invoke(null, desiredRoleCount, role, players, game, world)).intValue();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Role, List<UUID>> forcedRoles() {
        try { return (Map<Role, List<UUID>>) field(Class.forName(HARPY), "FORCED_MODDED_ROLE").get(null); }
        catch (Throwable ignored) { return null; }
    }

    private static boolean isNeutral(Role role) { return role != null && !role.isInnocent() && !role.canUseKiller(); }
    private static boolean isOverwriteBaseRole(Role role) {
        if (role == null) return false;
        try { return ((List<Role>) field(Class.forName(HARPY), "OVERWRITE_ROLES").get(null)).contains(role); }
        catch (Throwable ignored) { return false; }
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getField(name); field.setAccessible(true); return field;
    }
}
