package dev.annawathe.mixin;

import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.annawathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 服务端近战攻击的最终可攻击性兜底，并承载 profile 近战击杀。 */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackVisibilityMixin {
    @Shadow public abstract float getAttackCooldownProgress(float baseTime);
    @WrapMethod(method="attack")
    private void annawathe$attack(Entity target, Operation<Void> original) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (!TargetVisibilityApi.canAttackEntity(self, target)) return;
        if (target instanceof PlayerEntity victim
                && GameFunctions.isPlayerAliveAndSurvival(self)
                && PsychoModeApi.isMeleeKillWeapon(self, self.getMainHandStack())
                && this.getAttackCooldownProgress(0.5F) >= 1F) {
            var profile = PsychoModeApi.getActiveProfile(self);
            var reason = profile == null ? GameConstants.DeathReasons.BAT : profile.meleeDeathReason();
            GameFunctions.killPlayer(victim, true, self, reason);
            var sound = profile == null ? WatheSounds.ITEM_BAT_HIT : profile.hitSound();
            if (sound != null) self.getEntityWorld().playSound(self, victim.getX(), victim.getEyeY(), victim.getZ(), sound, SoundCategory.PLAYERS, 3F, 1F);
            return;
        }
        original.call(target);
    }
}
