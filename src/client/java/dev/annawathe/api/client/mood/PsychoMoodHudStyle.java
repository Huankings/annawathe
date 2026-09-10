package dev.annawathe.api.client.mood;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/** 疯魔 HUD 的身体、破损身体、眼睛、跑马文本和颜色定义。 */
public record PsychoMoodHudStyle(SpriteProvider body, SpriteProvider hitBody, SpriteProvider eyes,
                                 TextProvider text, ColorProvider textColor, ColorProvider timerColor) {
    public static PsychoMoodHudStyle defaults(Identifier body,Identifier hit,Identifier eyes){int red=MathHelper.hsvToRgb(0F,1F,.5F);return new PsychoMoodHudStyle((c,p)->body,(c,p)->hit,(c,p)->eyes,(c,p)->Text.translatable("game.psycho_mode.text"),(c,p)->red,(c,p)->red);}
    @FunctionalInterface public interface SpriteProvider{@Nullable Identifier get(MoodHudContext context,PlayerPsychoComponent psycho);}
    @FunctionalInterface public interface TextProvider{@Nullable Text get(MoodHudContext context,PlayerPsychoComponent psycho);}
    @FunctionalInterface public interface ColorProvider{int get(MoodHudContext context,PlayerPsychoComponent psycho);}
}
