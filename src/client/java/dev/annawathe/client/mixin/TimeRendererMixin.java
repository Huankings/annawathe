package dev.annawathe.client.mixin;
import dev.annawathe.client.gui.AnnaTimeRenderer;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.font.TextRenderer; import net.minecraft.client.gui.DrawContext; import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.Inject; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TimeRenderer.class) public abstract class TimeRendererMixin {
    @Inject(method="renderHud",at=@At("HEAD"),cancellable=true) private static void annawathe$render(TextRenderer r,ClientPlayerEntity p,DrawContext c,float delta,CallbackInfo ci){AnnaTimeRenderer.renderHud(r,p,c,delta);ci.cancel();}
    @Inject(method="tick",at=@At("HEAD"),cancellable=true) private static void annawathe$tick(CallbackInfo ci){AnnaTimeRenderer.tick();ci.cancel();}
}
