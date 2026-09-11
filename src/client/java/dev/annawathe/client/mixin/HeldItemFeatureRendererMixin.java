package dev.annawathe.client.mixin;

import dev.annawathe.api.client.invisibility.HeldItemInvisibilityApi;
import dev.annawathe.api.client.mood.PsychosisItemApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 统一处理第三人称主手/副手：先隐藏真实物品，再允许幻觉层覆盖空栈。 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
    @WrapOperation(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack annawathe$main(LivingEntity holder, Operation<ItemStack> original) {
        ItemStack stack = original.call(holder);
        stack = HeldItemInvisibilityApi.applyInvisibility(MinecraftClient.getInstance().player, holder, Hand.MAIN_HAND, stack);
        return PsychosisItemApi.resolveRenderStack(MinecraftClient.getInstance().player, holder, Hand.MAIN_HAND, stack);
    }
    @WrapOperation(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack annawathe$off(LivingEntity holder, Operation<ItemStack> original) {
        ItemStack stack = original.call(holder);
        stack = HeldItemInvisibilityApi.applyInvisibility(MinecraftClient.getInstance().player, holder, Hand.OFF_HAND, stack);
        return PsychosisItemApi.resolveRenderStack(MinecraftClient.getInstance().player, holder, Hand.OFF_HAND, stack);
    }
}
