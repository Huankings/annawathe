package dev.annawathe.mixin;

import dev.annawathe.api.collision.PlayerCollisionApi;
import dev.annawathe.api.collision.PlayerCollisionMode;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 同时处理实体墙判定与原版玩家推挤；PASS 时还要压制原版 Wathe 的旧无条件碰撞。 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @WrapMethod(method="collidesWith")
    private boolean annawathe$resolve(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        // PlayerBodyEntity 只是尸体展示实体，不能参与玩家移动碰撞或实体墙逻辑。
        if (other instanceof PlayerBodyEntity || self instanceof PlayerBodyEntity) return false;
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

    /** TargetVisibility 的服务端交互边界：客户端隐藏不能替代真实权限校验。 */
    @Inject(method="isInvisibleTo", at=@At("HEAD"), cancellable=true)
    private void annawathe$visibility(PlayerEntity viewer, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity target && !dev.annawathe.api.visibility.TargetVisibilityApi.canRenderPlayer(viewer, target)) cir.setReturnValue(true);
    }

    @Inject(method="interact", at=@At("HEAD"), cancellable=true)
    private void annawathe$interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!dev.annawathe.api.visibility.TargetVisibilityApi.canInteractWithEntity(player, (Entity) (Object) this)) cir.setReturnValue(ActionResult.PASS);
    }

    @Inject(method="interactAt", at=@At("HEAD"), cancellable=true)
    private void annawathe$interactAt(PlayerEntity player, Vec3d hitPos, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!dev.annawathe.api.visibility.TargetVisibilityApi.canInteractWithEntity(player, (Entity) (Object) this)) cir.setReturnValue(ActionResult.PASS);
    }
}
