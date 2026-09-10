package dev.annawathe.client.mixin;

import dev.annawathe.client.gui.AnnaMoodRenderer;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 原版入口保留不变，但渲染内容全部由 AnnaWathe 统一实现。 */
@Mixin(MoodRenderer.class)
public abstract class MoodRendererMixin {
    @Inject(method="renderHud",at=@At("HEAD"),cancellable=true)
    private static void annawathe$render(PlayerEntity player, TextRenderer renderer, DrawContext context, RenderTickCounter ticks, CallbackInfo ci){AnnaMoodRenderer.render(player,renderer,context,ticks);ci.cancel();}
}
