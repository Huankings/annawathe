package dev.annawathe.mixin;

import dev.annawathe.api.collision.PlayerCollisionApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** LivingEntity 扫描附近实体时的推挤兜底，保证 NO_COLLISION 不被另一条调用链重新推走。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCollisionMixin {
    @Inject(method="pushAway", at=@At("HEAD"), cancellable=true)
    private void annawathe$push(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity a && other instanceof PlayerEntity b && PlayerCollisionApi.suppressesPush(a, b)) ci.cancel();
    }
}
