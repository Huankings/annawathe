package dev.annawathe.client.mixin;

import dev.annawathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 尸体纹理读取最终 appearance UUID，不读取临时覆盖过的玩家实体皮肤。 */
@Mixin(PlayerBodyEntityRenderer.class)
public abstract class PlayerBodyTextureMixin {
    @Inject(method="getTexture", at=@At("HEAD"), cancellable=true)
    private void annawathe$bodyTexture(PlayerBodyEntity body, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(PlayerAppearanceApi.resolveBodySkin(body).texture());
    }
}
