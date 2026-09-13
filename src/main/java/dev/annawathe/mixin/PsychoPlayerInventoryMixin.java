package dev.annawathe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.annawathe.api.psycho.PsychoModeApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 服务端滚轮换栏时保持疯魔 profile 的锁定栏位。 */
@Mixin(PlayerInventory.class)
public abstract class PsychoPlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;
    @WrapMethod(method = "scrollInHotbar")
    private void annawathe$lockHotbar(double amount, Operation<Void> original) {
        int oldSlot = player.getInventory().selectedSlot;
        original.call(amount);
        if (PsychoModeApi.isActive(player)
                && PsychoModeApi.isLockedItem(player, player.getInventory().getStack(oldSlot))
                && !PsychoModeApi.isLockedItem(player, player.getInventory().getStack(player.getInventory().selectedSlot))) {
            player.getInventory().selectedSlot = oldSlot;
        }
    }
}
