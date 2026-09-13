package dev.annawathe.mixin;

import dev.annawathe.api.psycho.PsychoModeApi;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 服务端禁止中途丢弃 profile 授予的锁定物品。 */
@Mixin(ServerPlayerEntity.class)
public abstract class PsychoServerPlayerMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void annawathe$preventLockedDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (PsychoModeApi.shouldPreventDrop(player, player.getMainHandStack())) cir.setReturnValue(false);
    }
}
