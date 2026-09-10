package dev.annawathe.mixin.compat;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 保持原版 HarpyModLoader 的 forceRole 双向索引一致，避免同一玩家残留多个强制职业。 */
@Mixin(targets = "org.agmas.harpymodloader.Harpymodloader")
public abstract class HarpyForceRoleBookkeepingMixin {
    @Inject(method = "addToForcedRoles", at = @At("HEAD"), remap = false)
    private static void annawathe$deduplicate(Role role, PlayerEntity player, CallbackInfo ci) {
        try {
            Map<Role, List<UUID>> byRole = (Map<Role, List<UUID>>) field("FORCED_MODDED_ROLE").get(null);
            Map<UUID, Role> byPlayer = (Map<UUID, Role>) field("FORCED_MODDED_ROLE_FLIP").get(null);
            UUID uuid = player.getUuid();
            Role previous = byPlayer.get(uuid);
            if (previous != null && previous != role) {
                List<UUID> old = byRole.get(previous);
                if (old != null) { old.remove(uuid); if (old.isEmpty()) byRole.remove(previous); }
            }
            List<UUID> current = byRole.get(role);
            if (current != null) current.remove(uuid);
        } catch (Throwable ignored) {
            // 可选兼容层失败时保留 Harpy 原版行为，不影响 AnnaWathe 主体启动。
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = Class.forName("org.agmas.harpymodloader.Harpymodloader").getField(name);
        field.setAccessible(true); return field;
    }
}
