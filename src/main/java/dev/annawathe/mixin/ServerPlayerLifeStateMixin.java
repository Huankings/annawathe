package dev.annawathe.mixin;

import dev.annawathe.api.PlayerLifeStateApi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 普通原版切换到 creative/spectator 时撤销特殊存活，避免管理员调试状态泄漏到后续。 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerLifeStateMixin {
    @Inject(method = "changeGameMode", at = @At("HEAD"))
    private void annawathe$clearNormalOverride(GameMode mode, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (PlayerLifeStateApi.isNonSurvivalMode(mode) && !PlayerLifeStateApi.isGameplayAliveGameModeChangeAllowed(player)) {
            PlayerLifeStateApi.clearAliveOverride(player);
        }
    }
}
