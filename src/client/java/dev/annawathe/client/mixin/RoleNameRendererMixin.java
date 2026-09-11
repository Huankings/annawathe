package dev.annawathe.client.mixin;

import dev.annawathe.client.gui.AnnaRoleNameRenderer;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 完整接管原版准心名字 HUD，避免扩展继续依赖原版 renderer 的局部变量。 */
@Mixin(RoleNameRenderer.class)
public abstract class RoleNameRendererMixin {
    @Inject(method="renderHud", at=@At("HEAD"), cancellable=true)
    private static void annawathe$render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AnnaRoleNameRenderer.render(renderer, player, context, tickCounter);
        ci.cancel();
    }
}
