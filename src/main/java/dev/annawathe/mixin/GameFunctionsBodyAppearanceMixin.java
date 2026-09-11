package dev.annawathe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.annawathe.api.appearance.BodyAppearanceApi;
import dev.annawathe.bridge.PlayerBodyAppearanceBridge;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版生成尸体前解析视觉外观，确保尸体 owner/回放逻辑仍使用真实死者。 */
@Mixin(dev.doctor4t.wathe.game.GameFunctions.class)
public abstract class GameFunctionsBodyAppearanceMixin {
    @Inject(method="killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at=@At(value="INVOKE", target="Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void annawathe$applyBodyAppearance(PlayerEntity victim, boolean spawnBody, @Nullable PlayerEntity killer, Identifier deathReason, CallbackInfo ci, @Local PlayerBodyEntity body) {
        if (body instanceof PlayerBodyAppearanceBridge bridge) {
            bridge.annawathe$setAppearanceUuid(BodyAppearanceApi.resolveAppearanceUuid(victim, killer, deathReason));
        }
    }
}
