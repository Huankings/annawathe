package dev.annawathe.client.mixin;

import dev.annawathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 统一接管 getSkinTextures，使第三人称、披风和第一人称手臂使用同一外观结果。 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerAppearanceMixin {
    @Inject(method="getSkinTextures", at=@At("HEAD"), cancellable=true)
    private void annawathe$resolveSkin(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures skin = PlayerAppearanceApi.resolvePlayerSkin((AbstractClientPlayerEntity) (Object) this);
        if (skin != null) cir.setReturnValue(skin);
    }
}
