package dev.annawathe.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.annawathe.api.client.psycho.PsychoModeClientApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Psycho 视觉可选择隐藏普通模型特征，但始终保留手持物层。 */
@Mixin(LivingEntityRenderer.class)
public abstract class PsychoLivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>, X extends Entity> extends EntityRenderer<T> {
    protected PsychoLivingEntityRendererMixin(EntityRendererFactory.Context context) { super(context); }

    @WrapOperation(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"))
    private void annawathe$hideFeatures(FeatureRenderer<T, M> renderer, MatrixStack matrices, VertexConsumerProvider vertices, int light, X entity,
                                        float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch,
                                        Operation<Void> original, T livingEntity) {
        boolean hide = livingEntity instanceof AbstractClientPlayerEntity player && PsychoModeClientApi.shouldHideFeatures(player);
        if (!hide || renderer instanceof HeldItemFeatureRenderer<?, ?>) {
            original.call(renderer, matrices, vertices, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
        }
    }
}
