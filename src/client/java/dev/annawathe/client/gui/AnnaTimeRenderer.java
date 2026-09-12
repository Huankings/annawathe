package dev.annawathe.client.gui;

import dev.annawathe.api.time.TimeHudApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

/** 原版 TimeRenderer 的 Anna 门面，保留滚动数字但把时间来源交给 TimeHudApi。 */
public final class AnnaTimeRenderer {
    private static TimeNumberRenderer view = new TimeNumberRenderer();
    private static float offsetDelta;
    private static boolean registered;
    private static Object source;
    private AnnaTimeRenderer() {}
    public static void ensureDefaultProvider() {
        if (registered) return; registered = true;
        TimeHudApi.registerDefaultProvider(dev.annawathe.AnnaWathe.id("default_game_time"), TimeHudApi.DEFAULT_PRIORITY, viewer -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld()); Role role = game.getRole(viewer);
            if (!game.isRunning() || !((role != null && role.canSeeTime()) || GameFunctions.isPlayerSpectatingOrCreative(viewer))) return TimeHudApi.TimeDisplay.pass();
            return TimeHudApi.TimeDisplay.showCountdown(GameTimeComponent.KEY.get(viewer.getWorld()).getTime(), GameConstants.getInTicks(1, 0));
        });
    }
    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, @NotNull DrawContext context, float delta) {
        ensureDefaultProvider(); TimeHudApi.TimeDisplay display = TimeHudApi.resolveDisplay(player);
        if (display.action() != TimeHudApi.TimeDisplay.Action.SHOW) { source = null; return; }
        if (!display.sourceId().equals(source)) { reset(); source = display.sourceId(); }
        int time = display.ticks(); if (Math.abs(view.target - time) > display.changeFlashThreshold()) offsetDelta = time > view.target ? .6f : -.6f;
        if (display.colorMode() == TimeHudApi.TimeDisplay.ColorMode.DYNAMIC && display.lowTimeWarningTicks() >= 0 && time < display.lowTimeWarningTicks()) offsetDelta = -.9f; else offsetDelta = MathHelper.lerp(delta / 16, offsetDelta, 0f);
        view.setTarget(time); int color = display.colorMode() == TimeHudApi.TimeDisplay.ColorMode.FIXED ? withFullAlpha(display.fixedColor()) : MathHelper.packRgb(offsetDelta > 0 ? 1f-offsetDelta : 1f, offsetDelta < 0 ? 1f+offsetDelta : 1f, 1f-Math.abs(offsetDelta)) | 0xFF000000;
        context.getMatrices().push(); context.getMatrices().translate(context.getScaledWindowWidth()/2f,6,0); view.render(renderer,context,color,delta); context.getMatrices().pop();
    }
    public static void tick(){view.update();}
    public static void reset(){view=new TimeNumberRenderer();offsetDelta=0;source=null;}
    private static int withFullAlpha(int color){return (color&0xFF000000)==0?color|0xFF000000:color;}
    private static final class TimeNumberRenderer { final ScrollingDigit m10=new ScrollingDigit(7200,false),m1=new ScrollingDigit(720,false),s10=new ScrollingDigit(120,true),s1=new ScrollingDigit(12,false); float target; void setTarget(float t){target=t;float sec=t/20f,min=sec/60f;s10.target=sec/10;s1.target=sec;m10.target=min/10;m1.target=min;}void update(){m10.update();m1.update();s10.update();s1.update();}void render(TextRenderer r,DrawContext c,int color,float d){c.getMatrices().push();c.getMatrices().translate(16,0,0);s1.render(r,c,color,d);c.getMatrices().translate(-8,0,0);s10.render(r,c,color,d);c.getMatrices().translate(-8,0,0);c.drawTextWithShadow(r,":",2,0,color);c.getMatrices().translate(-8,0,0);m1.render(r,c,color,d);c.getMatrices().translate(-8,0,0);m10.render(r,c,color,d);c.getMatrices().pop();}}
    private static final class ScrollingDigit { final int power;final boolean cap6;float target,value,last;ScrollingDigit(int power,boolean cap6){this.power=power;this.cap6=cap6;}void update(){last=value;value=MathHelper.lerp(.15f,value,target);if(Math.abs(value-target)<.01f)value=target;}void render(TextRenderer r,DrawContext c,int color,float d){float v=MathHelper.lerp(d,last,value);int digit=MathHelper.floor(v)%(cap6?6:10),next=MathHelper.floor(v+1)%(cap6?6:10);double off=Math.pow(v%1,power);int rgb=color&0xFFFFFF,parentAlpha=color>>>24;int base=rgb|MathHelper.clamp((int)Math.round(parentAlpha*(1-Math.abs(off))),0,255)<<24,nxt=rgb|MathHelper.clamp((int)Math.round(parentAlpha*Math.abs(off)),0,255)<<24;c.getMatrices().push();c.getMatrices().translate(0,-off*(r.fontHeight+2),0);/* TextRenderer 会把 1-3 的极低 alpha 当作旧式无 alpha 颜色；使用原版阈值避免下一位数字突然不透明闪烁。 */if((base&0xFC000000)!=0)c.drawTextWithShadow(r,String.valueOf(digit),0,0,base);if((nxt&0xFC000000)!=0)c.drawTextWithShadow(r,String.valueOf(next),0,r.fontHeight+2,nxt);c.getMatrices().pop();}}
}
