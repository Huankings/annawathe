package dev.annawathe.client.mixin;

import dev.annawathe.api.client.inventory.InventoryButtonApi;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 限制背包基类上的 tick、关闭和背包键拦截。 */
@Mixin(LimitedHandledScreen.class)
public abstract class LimitedHandledScreenButtonsMixin {
    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void annawathe$tickInventoryExtensions(CallbackInfo ci) {
        InventoryButtonApi.tickScreen((LimitedHandledScreen<?>) (Object) this);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void annawathe$closeInventoryExtensions(CallbackInfo ci) {
        InventoryButtonApi.closeScreen((LimitedHandledScreen<?>) (Object) this);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void annawathe$keepOpenForExtension(int keyCode, int scanCode, int modifiers,
                                                CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.inventoryKey.matchesKey(keyCode, scanCode)
                && !InventoryButtonApi.allowInventoryKeyClose((LimitedHandledScreen<?>) (Object) this, keyCode, scanCode)) {
            cir.setReturnValue(true);
        }
    }
}
