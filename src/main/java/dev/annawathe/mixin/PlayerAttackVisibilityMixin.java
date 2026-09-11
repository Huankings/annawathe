package dev.annawathe.mixin;

import dev.annawathe.api.visibility.TargetVisibilityApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** 服务端近战攻击的最终可攻击性兜底；枪械/匕首 C2S 仍应在各自接收器中再次校验。 */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackVisibilityMixin {
    @WrapMethod(method="attack")
    private void annawathe$attack(Entity target, Operation<Void> original) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (TargetVisibilityApi.canAttackEntity(self, target)) original.call(target);
    }
}
