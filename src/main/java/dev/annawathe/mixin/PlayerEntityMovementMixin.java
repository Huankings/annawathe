package dev.annawathe.mixin;

import dev.annawathe.api.movement.PlayerMovementApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/** 在原版 Wathe 的固定 0.07/0.10 速度之后统一询问扩展修正规则。 */
/** 在原版 Wathe 已计算出的 0.07/0.10 基础速度之后叠加 Anna 扩展速度规则。 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMovementMixin {
    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float annawathe$resolveSpeed(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) return original;
        float base = player.isSprinting() ? 0.10F : 0.07F;
        return PlayerMovementApi.resolveMovementSpeed(player, original, base);
    }
}
