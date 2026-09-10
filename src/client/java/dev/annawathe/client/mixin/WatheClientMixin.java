package dev.annawathe.client.mixin;
import dev.annawathe.api.instinct.InstinctApi; import net.minecraft.client.MinecraftClient; import net.minecraft.entity.Entity; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(dev.doctor4t.wathe.client.WatheClient.class)
public abstract class WatheClientMixin {
 @Inject(method="isInstinctEnabled",at=@At("HEAD"),cancellable=true) private static void annawathe$enabled(CallbackInfoReturnable<Boolean> cir){var p=MinecraftClient.getInstance().player;if(p!=null)cir.setReturnValue(InstinctApi.resolveAvailability(p)==InstinctApi.AvailabilityResult.ENABLE);}
 @Inject(method="getInstinctHighlight",at=@At("HEAD"),cancellable=true) private static void annawathe$highlight(Entity target,CallbackInfoReturnable<Integer> cir){var p=MinecraftClient.getInstance().player;if(p==null)return;var r=InstinctApi.resolveHighlight(p,target);if(r.action()==InstinctApi.HighlightResult.Action.COLOR)cir.setReturnValue(r.color());else if(r.action()==InstinctApi.HighlightResult.Action.HIDE)cir.setReturnValue(-1);}
}
