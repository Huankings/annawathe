package dev.annawathe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.annawathe.api.psycho.PsychoModeApi;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 服务端网络换栏校验，防止恶意包绕过客户端锁栏。 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class PsychoServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;
    @WrapMethod(method = "onUpdateSelectedSlot")
    private void annawathe$validateSelectedSlot(UpdateSelectedSlotC2SPacket packet, Operation<Void> original) {
        if (PsychoModeApi.isActive(player)
                && !PsychoModeApi.isLockedItem(player, player.getInventory().getStack(packet.getSelectedSlot()))) return;
        original.call(packet);
    }
}
