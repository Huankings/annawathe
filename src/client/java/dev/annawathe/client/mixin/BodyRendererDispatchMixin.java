package dev.annawathe.client.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.Local;
import dev.annawathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

/**
 * 尸体 slim/wide 模型也跟随最终 appearance UUID，而不是只看真实 owner。
 * 该 Mixin 自己维护 renderer map，不 Shadow Wathe 私有字段，避免和原版 renderer Mixin 耦合。
 */
@Mixin(EntityRenderDispatcher.class)
@SuppressWarnings("unchecked")
public abstract class BodyRendererDispatchMixin {
    @Unique private Map<SkinTextures.Model, EntityRenderer<? extends PlayerBodyEntity>> annawathe$bodyRenderers = Map.of();
    @Inject(method="reload", at=@At("TAIL"))
    private void annawathe$reload(ResourceManager manager, CallbackInfo ci, @Local EntityRendererFactory.Context context) {
        ImmutableMap.Builder<SkinTextures.Model, EntityRenderer<? extends PlayerBodyEntity>> builder = ImmutableMap.builder();
        builder.put(SkinTextures.Model.WIDE, new PlayerBodyEntityRenderer<>(context, false));
        builder.put(SkinTextures.Model.SLIM, new PlayerBodyEntityRenderer<>(context, true));
        annawathe$bodyRenderers = builder.build();
    }
    @Inject(method="getRenderer", at=@At("HEAD"), cancellable=true)
    private <T extends Entity> void annawathe$resolveBodyRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (!(entity instanceof PlayerBodyEntity body)) return;
        EntityRenderer<? extends PlayerBodyEntity> renderer = annawathe$bodyRenderers.get(PlayerAppearanceApi.resolveBodySkin(body).model());
        if (renderer == null) renderer = annawathe$bodyRenderers.get(SkinTextures.Model.WIDE);
        if (renderer != null) cir.setReturnValue((EntityRenderer<? super T>) renderer);
    }
}
