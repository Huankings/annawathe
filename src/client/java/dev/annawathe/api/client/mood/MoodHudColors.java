package dev.annawathe.api.client.mood;

import net.minecraft.util.math.MathHelper;

/** 统一兼容 0xRRGGBB 和 java.awt.Color#getRGB() 返回的 0xAARRGGBB。 */
public final class MoodHudColors {
    private MoodHudColors() {}
    public static int withAlpha(int rgbOrArgb, float alpha) {
        int rgb = rgbOrArgb & 0x00FFFFFF;
        int alphaByte = MathHelper.floor(MathHelper.clamp(alpha, 0F, 1F) * 255F);
        return rgb | alphaByte << 24;
    }
}
