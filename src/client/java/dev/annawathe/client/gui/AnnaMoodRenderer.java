package dev.annawathe.client.gui;

import dev.annawathe.api.client.mood.MoodHudApi;
import dev.annawathe.api.client.mood.MoodHudColors;
import dev.annawathe.api.client.mood.MoodHudContext;
import dev.annawathe.api.client.mood.MoodHudStyle;
import dev.annawathe.api.client.mood.PsychoMoodHudStyle;
import dev.annawathe.api.psycho.PsychoModeApi;
import dev.annawathe.api.task.MoodTaskApi;
import dev.annawathe.cca.AnnaMoodSettings;
import dev.annawathe.client.bridge.DrawContextBridge;
import dev.annawathe.mood.MoodTaskState;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 完整替代原版 MoodRenderer 的客户端 renderer。 */
public final class AnnaMoodRenderer {
    private static final Identifier ARROW_UP=Identifier.of("wathe","hud/arrow_up"),ARROW_DOWN=Identifier.of("wathe","hud/arrow_down");
    private static final Identifier HAPPY=Identifier.of("wathe","hud/mood_happy"),MID=Identifier.of("wathe","hud/mood_mid"),DEPRESSIVE=Identifier.of("wathe","hud/mood_depressive"),KILLER=Identifier.of("wathe","hud/mood_killer");
    private static final Identifier PSYCHO=Identifier.of("wathe","hud/mood_psycho"),PSYCHO_HIT=Identifier.of("wathe","hud/mood_psycho_hit"),PSYCHO_EYES=Identifier.of("wathe","hud/mood_psycho_eyes");
    private static final Random RANDOM=new Random();
    private static final LinkedHashMap<Identifier,TaskRenderer> TASK_RENDERERS=new LinkedHashMap<>();
    private static final MoodHudStyle REAL_STYLE=MoodHudStyle.builder(c->c.moodRender()<GameConstants.DEPRESSIVE_MOOD_THRESHOLD?DEPRESSIVE:c.moodRender()<GameConstants.MID_MOOD_THRESHOLD?MID:HAPPY).arrows(ARROW_UP,ARROW_DOWN).hsvMoodBar().build();
    private static final MoodHudStyle FAKE_STYLE=MoodHudStyle.builder(KILLER).barColor(MathHelper.hsvToRgb(0F,1F,.6F)).build();
    private AnnaMoodRenderer(){}

    public static void render(PlayerEntity player,TextRenderer textRenderer,DrawContext draw,RenderTickCounter ticks){
        GameWorldComponent game=GameWorldComponent.KEY.get(player.getWorld());
        Role role=game.getRole(player);
        /*
         * 原版 InGameHud 调用 MoodRenderer 前已经用 trainComponent.hasHud() 限制了 HUD 场景，
         * 这里不能再依赖客户端 game.isRunning() 或 GameMode 对象身份做第二次门禁。
         * 这两个 CCA 字段可能比服务端任务/心情状态晚一拍同步；先前我们仍然无条件取消原版
         * renderer，便会出现“服务端正常掉心情甚至精神崩溃，但所有客户端左上角完全空白”。
         *
         * Mood HUD 真正需要的稳定条件只有：本地玩家仍按 Wathe 玩法参与，且职业拥有
         * REAL/FAKE 心情。MoodType.NONE、尚未收到职业同步以及旁观/创造状态都不绘制。
         */
        if(!GameFunctions.isPlayerAliveAndSurvival(player)||role==null||role.getMoodType()==Role.MoodType.NONE)return;
        PlayerMoodComponent mood=PlayerMoodComponent.KEY.get(player);PlayerPsychoComponent psycho=PlayerPsychoComponent.KEY.get(player);
        List<Identifier> active=MoodTaskApi.getActiveTaskIds(player);float old=MoodRenderer.moodRender;
        MoodRenderer.moodRender=MathHelper.lerp(ticks.getTickDelta(true)/8F,MoodRenderer.moodRender,mood.getMood());
        float warning=warning(mood,player);float[] shake=shake(warning);
        MoodRenderer.moodAlpha=MathHelper.lerp(ticks.getTickDelta(true)/16F,MoodRenderer.moodAlpha,(TASK_RENDERERS.isEmpty()&&warning<=0F)?0F:1F);
        MoodHudContext context=context(player,textRenderer,draw,ticks,game,mood,role,!active.isEmpty(),old,warning,shake);
        if(psycho.getPsychoTicks()>0){renderPsycho(player,textRenderer,draw,ticks,psycho,context);return;}

        for(Identifier id:active)if(!TASK_RENDERERS.containsKey(id)){for(TaskRenderer renderer:TASK_RENDERERS.values())renderer.index++;TASK_RENDERERS.put(id,new TaskRenderer());}
        ArrayList<Identifier> removals=new ArrayList<>();boolean fake=role!=null&&role.getMoodType()==Role.MoodType.FAKE;
        for(var entry:new ArrayList<>(TASK_RENDERERS.entrySet()))if(entry.getValue().tick(entry.getKey(),active.contains(entry.getKey()),ticks.getTickDelta(true),fake))removals.add(entry.getKey());
        removals.forEach(TASK_RENDERERS::remove);if(!removals.isEmpty())reindex();
        TaskRenderer max=null;for(TaskRenderer renderer:TASK_RENDERERS.values())if(max==null||renderer.offset>max.offset)max=renderer;
        if(max!=null){MoodRenderer.moodOffset=MathHelper.lerp(ticks.getTickDelta(true)/8F,MoodRenderer.moodOffset,max.offset);MoodRenderer.moodTextWidth=MathHelper.lerp(ticks.getTickDelta(true)/32F,MoodRenderer.moodTextWidth,textRenderer.getWidth(max.text));}
        else{MoodRenderer.moodOffset=MathHelper.lerp(ticks.getTickDelta(true)/8F,MoodRenderer.moodOffset,0F);MoodRenderer.moodTextWidth=MathHelper.lerp(ticks.getTickDelta(true)/32F,MoodRenderer.moodTextWidth,100F);}
        context=context(player,textRenderer,draw,ticks,game,mood,role,!active.isEmpty(),old,warning,shake);
        for(TaskRenderer renderer:TASK_RENDERERS.values()){draw.getMatrices().push();draw.getMatrices().translate(shake[0],shake[1],0);draw.getMatrices().translate(0,10*renderer.offset,0);draw.drawTextWithShadow(textRenderer,renderer.text,22,6,MoodHudColors.withAlpha(0xFFFFFF,renderer.alpha));draw.getMatrices().pop();}
        if(role!=null){MoodHudStyle style=MoodHudApi.resolve(context);if(style==null)style=role.getMoodType()==Role.MoodType.REAL?REAL_STYLE:role.getMoodType()==Role.MoodType.FAKE?FAKE_STYLE:null;if(style!=null)renderStyle(context,style);}
        MoodRenderer.arrowProgress=MathHelper.lerp(ticks.getTickDelta(true)/24F,MoodRenderer.arrowProgress,0F);
    }

    private static MoodHudContext context(PlayerEntity p,TextRenderer tr,DrawContext d,RenderTickCounter t,GameWorldComponent g,PlayerMoodComponent m,Role r,boolean has,float old,float warning,float[] shake){return new MoodHudContext(p,tr,d,t,g,m,r,has,old,MoodRenderer.moodRender,MoodRenderer.moodAlpha,MoodRenderer.moodOffset,MoodRenderer.moodTextWidth,warning,shake[0],shake[1]);}
    private static void reindex(){ArrayList<TaskRenderer> list=new ArrayList<>(TASK_RENDERERS.values());list.sort(java.util.Comparator.comparingDouble(a->a.offset));for(int i=0;i<list.size();i++)list.get(i).index=i;}
    public static void reset(){TASK_RENDERERS.clear();MoodRenderer.moodOffset=0;MoodRenderer.moodTextWidth=0;MoodRenderer.moodAlpha=0;}

    private static float warning(PlayerMoodComponent component,PlayerEntity player){Role role=GameWorldComponent.KEY.get(player.getWorld()).getRole(player);if(role==null||role.getMoodType()!=Role.MoodType.REAL||!AnnaMoodSettings.KEY.get(player.getWorld()).isMoodDeathEnabled())return 0F;float mood=component.getMood();return mood>MoodTaskState.BREAKDOWN_WARNING_THRESHOLD?0F:MathHelper.clamp((MoodTaskState.BREAKDOWN_WARNING_THRESHOLD-mood)/MoodTaskState.BREAKDOWN_WARNING_THRESHOLD,0F,1F);}
    private static float[] shake(float progress){if(progress<=0)return new float[]{0,0};RANDOM.setSeed(System.nanoTime());float strength=progress*3F;return new float[]{MathHelper.clamp((float)RANDOM.nextGaussian()*strength,-6F,6F),MathHelper.clamp((float)RANDOM.nextGaussian()*strength,-6F,6F)};}

    private static void renderStyle(MoodHudContext c,MoodHudStyle style){DrawContext d=c.drawContext();TextRenderer tr=c.textRenderer();d.getMatrices().push();d.getMatrices().translate(c.shakeX(),c.shakeY(),0);d.getMatrices().translate(0,3*c.moodOffset(),0);
        if(style.renderArrows()&&MoodRenderer.arrowProgress<.1F){if(c.previousMood()>=GameConstants.DEPRESSIVE_MOOD_THRESHOLD&&c.moodRender()<GameConstants.DEPRESSIVE_MOOD_THRESHOLD)MoodRenderer.arrowProgress=-1F;else if(c.previousMood()>=GameConstants.MID_MOOD_THRESHOLD&&c.moodRender()<GameConstants.MID_MOOD_THRESHOLD)MoodRenderer.arrowProgress=-1F;}
        if(style.iconRenderer()!=null)style.iconRenderer().render(c);else{Identifier sprite=style.sprite(c);if(sprite!=null)d.drawGuiTexture(sprite,5,6,14,17);}if(style.renderArrows())renderArrow(c,style);for(Identifier overlay:style.overlays(c))if(overlay!=null)d.drawGuiTexture(overlay,5,6,14,17);d.getMatrices().pop();
        if(style.shouldRenderBar(c)){d.getMatrices().push();d.getMatrices().translate(c.shakeX(),c.shakeY(),0);d.getMatrices().translate(0,10*c.moodOffset(),0);d.getMatrices().translate(26,8+tr.fontHeight,0);style.barRenderer().render(c,c.moodBarWidth(),c.moodAlpha());d.getMatrices().pop();}
        if(c.warningProgress()>0&&style.renderWarning()){d.getMatrices().push();d.getMatrices().translate(c.shakeX(),c.shakeY(),0);d.getMatrices().translate(0,10*c.moodOffset(),0);float pulse=(float)(Math.sin(System.currentTimeMillis()/90D)*.5+.5);int color=MathHelper.hsvToRgb(MathHelper.lerp(c.warningProgress(),.08F,0F),MathHelper.lerp(c.warningProgress(),.75F,1F),MathHelper.lerp(c.warningProgress(),.85F-pulse*.1F,.65F+pulse*.35F));d.drawTextWithShadow(tr,Text.translatable("annawathe.hud.mood.breakdown_warning"),22,12+tr.fontHeight,MoodHudColors.withAlpha(color,c.moodAlpha()));d.getMatrices().pop();}}
    private static void renderArrow(MoodHudContext c,MoodHudStyle style){if(Math.abs(MoodRenderer.arrowProgress)<=.01F)return;boolean up=MoodRenderer.arrowProgress>0;Identifier sprite=up?style.arrowUp(c):style.arrowDown(c);if(sprite==null)return;DrawContext d=c.drawContext();d.getMatrices().push();if(!up)d.getMatrices().translate(0,4,0);d.getMatrices().translate(0,MoodRenderer.arrowProgress*4,0);d.drawSprite(7,6,0,10,13,((DrawContextBridge)(Object)d).annawathe$getGuiAtlasManager().getSprite(sprite),1,1,1,(float)Math.sin(Math.abs(MoodRenderer.arrowProgress)*Math.PI));d.getMatrices().pop();}

    private static void renderPsycho(PlayerEntity player,TextRenderer tr,DrawContext d,RenderTickCounter ticks,PlayerPsychoComponent psycho,MoodHudContext c){PsychoMoodHudStyle style=MoodHudApi.resolvePsycho(c,psycho);if(style==null)style=PsychoMoodHudStyle.defaults(PSYCHO,PSYCHO_HIT,PSYCHO_EYES);Text text=style.text().get(c,psycho);int textColor=style.textColor().get(c,psycho),barColor=style.timerColor().get(c,psycho);RANDOM.setSeed(System.currentTimeMillis());
        if(text!=null){int width=tr.getWidth(text);d.getMatrices().push();d.getMatrices().translate(RANDOM.nextGaussian()/3,RANDOM.nextGaussian()/3,0);d.enableScissor(22,6,180,23);for(int i=-1;i<=3;i++){float value=1-((player.age+ticks.getTickDelta(true))/64)%1;d.getMatrices().push();d.getMatrices().translate(value*(width+4),6,0);d.drawTextWithShadow(tr,text,i*(width+4),0,MoodHudColors.withAlpha(textColor,1));d.getMatrices().pop();}d.disableScissor();d.getMatrices().pop();}
        d.getMatrices().push();d.getMatrices().translate(26,8+tr.fontHeight,0);float duration=Math.max(1F,psycho.getPsychoTicks()-ticks.getTickDelta(true))/Math.max(1F,PsychoModeApi.getMaxTicks(player));d.getMatrices().scale(150*duration,1,1);d.fill(0,0,1,1,MoodHudColors.withAlpha(barColor,.9F));d.getMatrices().pop();
        d.getMatrices().push();for(int i=1;i<=12;i++){if((player.age-i)%2!=0)continue;RANDOM.setSeed((player.age-i)*40L);float alpha=(12-i)/12F;d.getMatrices().push();int initialArmour=Math.max(1,PsychoModeApi.getInitialArmour(player));float bodyScale=.2F+(initialArmour-psycho.getArmour())*.8F;d.getMatrices().translate((RANDOM.nextFloat()-RANDOM.nextFloat())*bodyScale*i,(RANDOM.nextFloat()-RANDOM.nextFloat())*bodyScale*i,-i*3);Identifier body=(psycho.getArmour()>0?style.body():style.hitBody()).get(c,psycho);if(body!=null)d.drawSprite(5,6,0,14,17,((DrawContextBridge)(Object)d).annawathe$getGuiAtlasManager().getSprite(body),1,1,1,alpha);d.getMatrices().translate((RANDOM.nextFloat()-RANDOM.nextFloat())*.8F*i,(RANDOM.nextFloat()-RANDOM.nextFloat())*.8F*i,1);Identifier eyes=style.eyes().get(c,psycho);if(eyes!=null)d.drawSprite(5,6,0,14,17,((DrawContextBridge)(Object)d).annawathe$getGuiAtlasManager().getSprite(eyes),1,1,1,alpha);d.getMatrices().pop();}d.getMatrices().pop();}

    private static final class TaskRenderer{int index;float offset=-1F,alpha=.075F;Text text=Text.empty();boolean tick(Identifier id,boolean present,float delta,boolean fake){if(present)text=Text.translatable(fake?"annawathe.task.fake":"annawathe.task.feel").append(Text.translatable(MoodTaskApi.getTranslationKey(id)));alpha=MathHelper.lerp(delta/16F,alpha,present?1F:0F);offset=MathHelper.lerp(delta/32F,offset,index);return !present&&(alpha<.075F||(((int)(alpha*255)<<24)&0xFC000000)==0);}}
}
