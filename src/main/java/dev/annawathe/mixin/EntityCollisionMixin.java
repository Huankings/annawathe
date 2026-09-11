package dev.annawathe.mixin;

import dev.annawathe.api.collision.PlayerCollisionApi;
import dev.annawathe.api.collision.PlayerCollisionMode;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 同时处理实体墙判定与原版玩家推挤；PASS 时还要压制原版 Wathe 的旧无条件碰撞。 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @WrapMethod(method="collidesWith")
    private boolean annawathe$resolve(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity a && other instanceof PlayerEntity b) {
            PlayerCollisionMode mode = PlayerCollisionApi.resolve(a, b);
            if (mode != PlayerCollisionMode.PASS) return mode.blocksMovement();
            // 原版 Wathe 的旧 Mixin 会在对局中无条件把玩家判为可碰撞；
            // Anna 默认规则已自行判断双方是否为玩法存活，PASS 时不能再让旧逻辑把死亡/旁观者挡住。
            if (GameWorldComponent.KEY.get(self.getWorld()).isRunning()) return false;
        }
        return original.call(other);
    }
    @Inject(method="pushAwayFrom", at=@At("HEAD"), cancellable=true)
    private void annawathe$push(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity a && other instanceof PlayerEntity b && PlayerCollisionApi.suppressesPush(a, b)) ci.cancel();
    }
}
