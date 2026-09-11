package dev.annawathe.api;

import dev.annawathe.cca.PlayerLifeStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** AnnaWathe 的“非生存模式仍属于局内存活”公开 API。 */
/**
 * 特殊存活授权门面。它不改变原版 GameMode 权限，只改变 Wathe 的玩法存活判断。
 */
public final class PlayerLifeStateApi {
    private static final ThreadLocal<Set<UUID>> AUTHORIZED_CHANGES = ThreadLocal.withInitial(HashSet::new);

    private PlayerLifeStateApi() {}

    public static boolean hasAliveOverride(PlayerEntity player) {
        return player != null && PlayerLifeStateComponent.KEY.get(player).isAliveInNonSurvivalMode();
    }

    public static void setAliveInCurrentGameMode(ServerPlayerEntity player, boolean alive) {
        if (player != null) PlayerLifeStateComponent.KEY.get(player).setAliveInNonSurvivalMode(alive);
    }

    public static void clearAliveOverride(PlayerEntity player) {
        if (player != null) PlayerLifeStateComponent.KEY.get(player).clearAliveInNonSurvivalMode();
    }

    public static boolean changeGameModeAsGameplayAlive(ServerPlayerEntity player, GameMode mode) {
        if (player == null || mode == null) return false;
        if (isNonSurvivalMode(mode)) setAliveInCurrentGameMode(player, true);
        else clearAliveOverride(player);
        Set<UUID> ids = AUTHORIZED_CHANGES.get();
        ids.add(player.getUuid());
        try {
            return player.changeGameMode(mode);
        } finally {
            ids.remove(player.getUuid());
            if (ids.isEmpty()) AUTHORIZED_CHANGES.remove();
        }
    }

    public static boolean isGameplayAliveGameModeChangeAllowed(ServerPlayerEntity player) {
        return player != null && AUTHORIZED_CHANGES.get().contains(player.getUuid());
    }

    public static boolean isNonSurvivalMode(GameMode mode) {
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }
}
