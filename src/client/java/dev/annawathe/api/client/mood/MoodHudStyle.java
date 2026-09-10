package dev.annawathe.api.client.mood;

import dev.annawathe.AnnaWathe;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** 职业普通心情图标、箭头、覆盖层与心情条样式。 */
public final class MoodHudStyle {
    private final SpriteProvider sprite, up, down;
    private final OverlayProvider overlays;
    private final @Nullable IconRenderer icon;
    private final @Nullable BarRenderer bar;
    private final BarVisibility barVisibility;
    private final boolean arrows, warning;
    private MoodHudStyle(Builder b){sprite=b.sprite;up=b.up;down=b.down;overlays=b.overlays;icon=b.icon;bar=b.bar;barVisibility=b.barVisibility;arrows=b.arrows;warning=b.warning;}
    public static Builder builder(Identifier sprite){return new Builder(c->sprite);}
    public static Builder builder(SpriteProvider sprite){return new Builder(sprite);}
    public @Nullable Identifier sprite(MoodHudContext c){return sprite.get(c);}
    public @Nullable Identifier arrowUp(MoodHudContext c){return up.get(c);}
    public @Nullable Identifier arrowDown(MoodHudContext c){return down.get(c);}
    public List<Identifier> overlays(MoodHudContext c){return overlays.get(c);}
    public @Nullable IconRenderer iconRenderer(){return icon;}
    public @Nullable BarRenderer barRenderer(){return bar;}
    public boolean shouldRenderBar(MoodHudContext c){return bar!=null&&barVisibility.test(c);}
    public boolean renderArrows(){return arrows;}
    public boolean renderWarning(){return warning;}
    @FunctionalInterface public interface SpriteProvider{@Nullable Identifier get(MoodHudContext context);}
    @FunctionalInterface public interface OverlayProvider{List<Identifier> get(MoodHudContext context);}
    @FunctionalInterface public interface IconRenderer{void render(MoodHudContext context);}
    @FunctionalInterface public interface BarRenderer{void render(MoodHudContext context,int width,float alpha);}
    @FunctionalInterface public interface BarVisibility{boolean test(MoodHudContext context);}
    public static final class Builder{
        private final SpriteProvider sprite;private SpriteProvider up=c->AnnaWathe.id("unused"),down=c->AnnaWathe.id("unused");
        private OverlayProvider overlays=c->List.of();private @Nullable IconRenderer icon;private @Nullable BarRenderer bar;
        private BarVisibility barVisibility=c->c.moodAlpha()>0F;private boolean arrows;private boolean warning=true;
        private Builder(SpriteProvider sprite){this.sprite=Objects.requireNonNull(sprite);}
        public Builder arrows(Identifier up,Identifier down){this.up=c->up;this.down=c->down;this.arrows=true;return this;}
        public Builder overlays(OverlayProvider value){overlays=value;return this;}
        public Builder icon(IconRenderer value){icon=value;return this;}
        public Builder barColor(int color){bar=(c,w,a)->{if(w>0&&a>0)c.drawContext().fill(0,0,w,1,MoodHudColors.withAlpha(color,a));};return this;}
        public Builder hsvMoodBar(){bar=(c,w,a)->{if(w>0&&a>0)c.drawContext().fill(0,0,w,1,MoodHudColors.withAlpha(MathHelper.hsvToRgb(c.moodRender()/3F,1F,1F),a));};return this;}
        public Builder bar(BarRenderer value){bar=value;return this;}
        public Builder barVisibleWhen(BarVisibility value){barVisibility=value;return this;}
        public Builder hideWarning(){warning=false;return this;}
        public MoodHudStyle build(){return new MoodHudStyle(this);}
    }
}
