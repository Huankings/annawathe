package dev.annawathe.client.mixin;

import dev.annawathe.api.client.mood.PsychosisItemApi;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 默认枪械托举必须读取视觉手持物，并让扩展显式 ArmPose 优先。 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart head;
    @Inject(method="positionRightArm", at=@At("TAIL")) private void annawathe$right(T entity, CallbackInfo ci) { if (holdingGun(entity, Arm.RIGHT)) hold(rightArm, head, true); }
    @Inject(method="positionLeftArm", at=@At("TAIL")) private void annawathe$left(T entity, CallbackInfo ci) { if (holdingGun(entity, Arm.LEFT)) hold(leftArm, head, false); }
    @Unique private boolean holdingGun(T entity, Arm arm) { Hand hand = entity.getMainArm() == arm ? Hand.MAIN_HAND : Hand.OFF_HAND; if (PsychosisItemApi.resolveRenderArmPose(MinecraftClient.getInstance().player, entity, hand) != null) return false; ItemStack stack = PsychosisItemApi.resolveRenderStack(MinecraftClient.getInstance().player, entity, hand, hand == Hand.MAIN_HAND ? entity.getMainHandStack() : entity.getOffHandStack()); return stack.isIn(WatheItemTags.GUNS); }
    @Unique private static void hold(ModelPart arm, ModelPart head, boolean right) { arm.yaw = (right ? -0.3F : 0.3F) + head.yaw; arm.pitch = (float)(-Math.PI / 2) + head.pitch + 0.1F; }
}
