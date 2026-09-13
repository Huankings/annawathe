package dev.annawathe.api.psycho;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** 疯魔玩家的客户端皮肤和普通模型特征设置。 */
public record PsychoVisualSettings(
        @Nullable Identifier wideSkinTexture,
        @Nullable Identifier slimSkinTexture,
        boolean hideFeatures
) {
    public static PsychoVisualSettings none() {
        return new PsychoVisualSettings(null, null, false);
    }

    public static PsychoVisualSettings skin(@Nullable Identifier wide, @Nullable Identifier slim, boolean hideFeatures) {
        return new PsychoVisualSettings(wide, slim, hideFeatures);
    }

    public @Nullable Identifier texture(boolean slim) {
        return slim ? (slimSkinTexture != null ? slimSkinTexture : wideSkinTexture) : wideSkinTexture;
    }
}
