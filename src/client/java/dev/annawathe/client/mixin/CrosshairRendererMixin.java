package dev.annawathe.client.mixin;

import dev.annawathe.client.gui.AnnaCrosshairRenderer;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 完整接管原版 Wathe 准心 renderer，使扩展只需注册 CrosshairHudApi 而不再深层 Mixin。
 */
@Mixin(CrosshairRenderer.class)
public abstract class CrosshairRendererMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void annawathe$renderCrosshair(MinecraftClient client,
                                                  ClientPlayerEntity player,
                                                  DrawContext context,
                                                  RenderTickCounter tickCounter,
                                                  CallbackInfo ci) {
        // 参数顺序严格对应原版 Wathe 1.3.2 的静态方法 descriptor。
        AnnaCrosshairRenderer.renderCrosshair(client, player, context, tickCounter);
        ci.cancel();
    }
}
