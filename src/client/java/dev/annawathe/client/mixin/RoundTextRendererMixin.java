package dev.annawathe.client.mixin;
import dev.annawathe.client.gui.AnnaRoundTextRenderer; import dev.doctor4t.wathe.client.gui.RoundTextRenderer; import net.minecraft.client.font.TextRenderer; import net.minecraft.client.gui.DrawContext; import net.minecraft.client.network.ClientPlayerEntity; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.*;
/** 以 Anna 的统一结算 renderer 替换原版 HUD 绘制。 */
@Mixin(RoundTextRenderer.class)
public abstract class RoundTextRendererMixin {
 @Inject(method="renderHud",at=@At("HEAD"),cancellable=true) private static void annawathe$render(TextRenderer r,ClientPlayerEntity p,DrawContext c,CallbackInfo ci){AnnaRoundTextRenderer.renderHud(r,p,c);ci.cancel();}
 @Inject(method="startWelcome",at=@At("HEAD")) private static void annawathe$welcome(dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts.RoleAnnouncementText role,int killers,int targets,CallbackInfo ci){AnnaRoundTextRenderer.startWelcome(role,killers,targets);}
 @Inject(method="startEnd",at=@At("HEAD")) private static void annawathe$end(CallbackInfo ci){AnnaRoundTextRenderer.startEnd();}
}
