package dev.annawathe.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.annawathe.api.psycho.PsychoModeApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 客户端数字键换栏不能绕过 Psycho profile 的锁定栏位。 */
@Mixin(MinecraftClient.class)
public abstract class PsychoMinecraftClientMixin {
    @WrapOperation(method = "handleInputEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I"))
    private void annawathe$lockSelectedSlot(PlayerInventory inventory, int value, Operation<Void> original) {
        int oldSlot = inventory.selectedSlot;
        if (PsychoModeApi.isActive(inventory.player)
                && PsychoModeApi.isLockedItem(inventory.player, inventory.getStack(oldSlot))
                && !PsychoModeApi.isLockedItem(inventory.player, inventory.getStack(value))) return;
        original.call(inventory, value);
    }
}
