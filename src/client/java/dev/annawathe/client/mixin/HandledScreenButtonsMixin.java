package dev.annawathe.client.mixin;
import dev.annawathe.api.client.inventory.InventoryButtonApi; import net.minecraft.client.MinecraftClient; import net.minecraft.client.gui.screen.ingame.HandledScreen; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.At; import org.spongepowered.asm.mixin.injection.Inject; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 原版 handled screen 的关闭清理和背包键拦截；未注册状态的箱子等界面不会受影响。 */
@Mixin(HandledScreen.class) public abstract class HandledScreenButtonsMixin {
 @Inject(method="removed",at=@At("HEAD")) private void annawathe$close(CallbackInfo ci){InventoryButtonApi.closeScreen((HandledScreen<?>)(Object)this);}
 @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true) private void annawathe$keepOpen(int keyCode,int scanCode,int modifiers,CallbackInfoReturnable<Boolean> cir){MinecraftClient client=MinecraftClient.getInstance();if(client.options.inventoryKey.matchesKey(keyCode,scanCode)&&!InventoryButtonApi.allowInventoryKeyClose((HandledScreen<?>)(Object)this,keyCode,scanCode))cir.setReturnValue(true);}
}
