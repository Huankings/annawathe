package dev.annawathe.client.mixin;

import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 尸体渲染入口的 RENDER 过滤。 */
@Mixin(PlayerBodyEntityRenderer.class)
public abstract class PlayerBodyVisibilityRendererMixin {
    @Inject(method="render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable=true)
    private void annawathe$hideBody(PlayerBodyEntity body, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (!TargetVisibilityApi.canRenderBody(MinecraftClient.getInstance().player, body)) ci.cancel();
    }
}
