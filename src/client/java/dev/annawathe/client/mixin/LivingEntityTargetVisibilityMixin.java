package dev.annawathe.client.mixin;

import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 把 TargetVisibility 的 TARGET 结果接入客户端实体选中流程。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTargetVisibilityMixin {
    @Inject(method="canHit", at=@At("HEAD"), cancellable=true)
    private void annawathe$target(CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) return;
        Object self = this;
        if (self instanceof PlayerBodyEntity body && !TargetVisibilityApi.canTargetBody(viewer, body)) cir.setReturnValue(false);
        else if (self instanceof PlayerEntity player && !TargetVisibilityApi.canTargetPlayer(viewer, player)) cir.setReturnValue(false);
    }
}
