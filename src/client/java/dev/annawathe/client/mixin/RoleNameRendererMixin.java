package dev.annawathe.client.mixin;

import dev.annawathe.api.client.appearance.RoleNameApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 把原版 RoleNameRenderer 的显示名读取转接到 Anna 名称 API。 */
@Mixin(dev.doctor4t.wathe.client.gui.RoleNameRenderer.class)
public abstract class RoleNameRendererMixin {
    @Redirect(method="renderHud", at=@At(value="INVOKE", target="Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text annawathe$resolveName(PlayerEntity target) {
        PlayerEntity viewer = MinecraftClient.getInstance().player;
        Text original = target.getDisplayName();
        return viewer == null ? original : RoleNameApi.resolve(viewer, target, original);
    }
}
