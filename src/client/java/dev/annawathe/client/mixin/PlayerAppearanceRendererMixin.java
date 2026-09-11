package dev.annawathe.client.mixin;

import dev.annawathe.api.client.appearance.PlayerAppearanceApi;
import dev.annawathe.api.client.appearance.RoleNameApi;
import dev.annawathe.api.client.invisibility.HeldItemInvisibilityApi;
import dev.annawathe.api.client.mood.PsychosisItemApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 把隐藏手持物和幻觉手持物接入玩家手臂姿势解析。 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerAppearanceRendererMixin {
    @Inject(method="getArmPose", at=@At("TAIL"), cancellable=true)
    private static void annawathe$psychosisPose(AbstractClientPlayerEntity player, Hand hand, CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        BipedEntityModel.ArmPose pose = PsychosisItemApi.resolveRenderArmPose(MinecraftClient.getInstance().player, player, hand);
        if (pose != null) cir.setReturnValue(pose);
    }
    @ModifyExpressionValue(method="getArmPose", at=@At(value="INVOKE", target="Lnet/minecraft/client/network/AbstractClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack annawathe$visualStack(ItemStack original, AbstractClientPlayerEntity player, Hand hand) {
        ItemStack stack = HeldItemInvisibilityApi.applyInvisibility(MinecraftClient.getInstance().player, player, hand, original);
        return PsychosisItemApi.resolveRenderStack(MinecraftClient.getInstance().player, player, hand, stack);
    }
}
