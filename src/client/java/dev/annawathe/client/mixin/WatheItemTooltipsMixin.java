package dev.annawathe.client.mixin;
import dev.doctor4t.wathe.client.util.WatheItemTooltips; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 禁止原版注册旧 callback，避免与 annawathe 的准确冷却 tooltip 重复。 */
@Mixin(WatheItemTooltips.class)
public abstract class WatheItemTooltipsMixin { @Inject(method="addTooltips",at=@At("HEAD"),cancellable=true) private static void annawathe$disable(CallbackInfo ci){ci.cancel();} }
