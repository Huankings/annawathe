package dev.annawathe.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.annawathe.api.win.CustomVictory;
import dev.annawathe.api.win.CustomVictoryGroup;
import dev.annawathe.cca.AnnaRoundEndState;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * annawathe 的完整结算界面实现。布局算法来自自改版 Wathe，但所有状态读取
 * 都改为兼容原版 Wathe 的组件，并通过 AnnaRoundEndState 读取独立胜利数据。
 */
public final class AnnaRoundTextRenderer {
    private static final Map<String, Optional<GameProfile>> FAIL_CACHE = new HashMap<>();
    private static final Map<UUID, SkinTextures> SKIN_CACHE = new HashMap<>();
    private static final Set<UUID> SKIN_REQUESTS = new HashSet<>();
    private static final int WELCOME_DURATION = 200 + GameConstants.FADE_TIME * 2;
    private static final int END_DURATION = 200;
    private static final int CIVILIAN_COLUMNS = 5, DOUBLE_COLUMNS = 3, LOOSE_COLUMNS = 6;
    private static final int DYNAMIC_BASE = 6, LEFT_STEP = 8, RIGHT_STEP = 14;
    private static final int LEFT_DIRECTION = 0, RIGHT_DIRECTION = 0, LOOSE_DIRECTION = 0;
    private static final float SLOT_WIDTH = 34f, STEP_X = 30f, STEP_Y = 32f;
    private static final float HEADER_Y = 13f, GRID_Y = 24f, SECTION_GAP = 14f, EXTRA_HEADER_OFFSET = 3f;
    private static final float GROUP_GAP = 18f, SIDE_PADDING = 14f, LABEL_CENTER = SLOT_WIDTH / 2f;
    /** 与自改版 Wathe 的 RoleAnnouncementTexts.NEUTRAL 保持一致的中立标题颜色。 */
    private static final int NEUTRAL_TITLE_COLOR = 0xCC6600;
    private static final float NAME_Y = 17f, ROLE_Y = 23f, LABEL_MAX_WIDTH = 25f, SLOT_BOTTOM = 42f;
    private static final float ROOT_OFFSET = 45f, ROOT_MIN_Y = 14f, ROOT_OFFSET_X = 4f, BOTTOM_PADDING = 74f;
    private static final float HEAD_SCALE = 2f, DEATH_RIGHT_INSET = 2f, DEATH_TOP_OFFSET = 1.6f;
    private static final float DEATH_SCALE_X = 1.25f, DEATH_SCALE_Y = .70f, NAME_SCALE = .70f, ROLE_SCALE = .60f, MIN_SCALE = .30f;
    private static RoleAnnouncementTexts.RoleAnnouncementText role = RoleAnnouncementTexts.CIVILIAN;
    private static int welcomeTime, killers, targets, endTime;
    private static float hudWidth = 320f, hudHeight = 180f;
    private static int civilianCount, vigilanteCount, killerCount, neutralCount, looseCount, extraLeftCount;
    private static int extraLeftColumns = CIVILIAN_COLUMNS;

    private AnnaRoundTextRenderer() {}

    public static void startWelcome(RoleAnnouncementTexts.RoleAnnouncementText value, int killerTotal, int targetTotal) {
        role = value; killers = killerTotal; targets = targetTotal; welcomeTime = WELCOME_DURATION;
    }
    public static void startEnd() { welcomeTime = 0; endTime = END_DURATION; }
    public static boolean isEndAnimationPlaying() { return endTime > 0; }
    public static void clearEndAnimation() { endTime = 0; }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || GameWorldComponent.KEY.get(client.world).getGameMode() == WatheGameModes.DISCOVERY) return;
        ClientPlayerEntity player = client.player;
        if (welcomeTime > 0) {
            if (player != null) {
                if (welcomeTime == 200) play(player, WatheSounds.UI_RISER, 1f);
                if (welcomeTime == 180) play(player, WatheSounds.UI_PIANO, 1.25f);
                if (welcomeTime == 120) play(player, WatheSounds.UI_PIANO, 1.5f);
                if (welcomeTime == 60) play(player, WatheSounds.UI_PIANO, 1.75f);
                if (welcomeTime == 1) play(player, WatheSounds.UI_PIANO_STINGER, 1f);
            }
            welcomeTime--;
        }
        if (endTime > 0) {
            if (endTime == END_DURATION - GameConstants.FADE_TIME * 2 && player != null) {
                boolean won = GameRoundEndComponent.KEY.get(player.getWorld()).didWin(player.getUuid());
                play(player, won ? WatheSounds.UI_PIANO_WIN : WatheSounds.UI_PIANO_LOSE, 1f);
            }
            endTime--;
        }
        GameOptions options = client.options;
        if (options != null && options.playerListKey.isPressed()) endTime = Math.max(2, endTime);
    }
    private static void play(ClientPlayerEntity p, net.minecraft.sound.SoundEvent sound, float pitch) {
        p.getWorld().playSound(p, p.getX(), p.getY(), p.getZ(), sound, SoundCategory.MASTER, 10f, pitch, p.getRandom().nextLong());
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public static void renderHud(TextRenderer renderer, ClientPlayerEntity player, @NotNull DrawContext context) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        GameRoundEndComponent end = GameRoundEndComponent.KEY.get(player.getWorld());
        boolean looseEnds = game.getGameMode() == WatheGameModes.LOOSE_ENDS;
        hudWidth = context.getScaledWindowWidth(); hudHeight = context.getScaledWindowHeight();
        renderWelcome(renderer, context, looseEnds);
        if (endTime <= 0 || endTime >= END_DURATION - GameConstants.FADE_TIME * 2 || game.isRunning() || game.getGameMode() == WatheGameModes.DISCOVERY) return;
        if (end.getWinStatus() == GameFunctions.WinStatus.NONE) return;
        CustomVictory custom = AnnaRoundEndState.KEY.get(player.getWorld()).getCustomVictory();
        PlayerEntity winner = player.getWorld().getPlayerByUuid(UUID.randomUUID());
        Text endText = custom == null ? role.getEndText(end.getWinStatus(), winner == null ? Text.empty() : winner.getDisplayName()) : customAnnouncement(custom);
        if (endText == null) return;
        if (custom != null) { renderCustomVictory(renderer, context, end, custom, endText); return; }
        int civilians = 0, vigilantes = 0, killersTotal = 0, neutrals = 0, looseTotal = 0;
        if (looseEnds) looseTotal = end.getPlayers().size();
        else for (GameRoundEndComponent.RoundEndData entry : end.getPlayers()) {
            switch (endGroup(entry)) {
                case CIVILIAN -> civilians++;
                case VIGILANTE -> vigilantes++;
                case KILLER -> killersTotal++;
                case NEUTRAL -> neutrals++;
                default -> { }
            }
        }
        civilianCount = looseEnds ? 0 : civilians; vigilanteCount = looseEnds ? 0 : vigilantes;
        killerCount = looseEnds ? 0 : killersTotal; neutralCount = looseEnds ? 0 : neutrals; looseCount = looseEnds ? looseTotal : 0;
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f + ROOT_OFFSET_X, rootY(looseEnds, civilians, vigilantes, killersTotal, looseTotal), 0);
        drawCenteredScaled(renderer, context, endText, 0, -12, 2.6f, 0xFFFFFF);
        MutableText winMessage = Text.translatable("game.win." + end.getWinStatus().name().toLowerCase(Locale.ROOT));
        drawCenteredScaled(renderer, context, winMessage, 0, -4, 1.2f, 0xFFFFFF);
        if (looseEnds) renderLoose(renderer, context, end); else renderTeams(renderer, context, end, civilians, vigilantes, killersTotal, neutrals);
        context.getMatrices().pop();
    }

    private static void renderWelcome(TextRenderer r, DrawContext c, boolean loose) {
        if (welcomeTime <= 0) return;
        c.getMatrices().push(); c.getMatrices().translate(c.getScaledWindowWidth()/2f, c.getScaledWindowHeight()/2f+3.5, 0);
        int color = loose ? 0x9F0000 : 0xFFFFFF;
        if (welcomeTime <= 180) drawCenteredScaled(r,c,loose?Text.translatable("announcement.loose_ends.welcome"):role.welcomeText,0,-12,2.6f,color);
        if (welcomeTime <= 120) drawCenteredScaled(r,c,loose?Text.translatable("announcement.loose_ends.premise"):role.premiseText.apply(killers),0,0,1.2f,color);
        if (welcomeTime <= 60) drawCenteredScaled(r,c,loose?Text.translatable("announcement.loose_ends.goal"):role.goalText.apply(targets),0,14,1f,color);
        c.getMatrices().pop();
    }
    private static void renderLoose(TextRenderer r, DrawContext c, GameRoundEndComponent end) {
        Text title = RoleAnnouncementTexts.LOOSE_END.titleText;
        drawCentered(r,c,title,looseCenterX(),HEADER_Y,0xFFFFFF);
        int i=0; int columns=LOOSE_COLUMNS;
        for (var entry:end.getPlayers()) { c.getMatrices().push(); c.getMatrices().translate(looseStartX(i,end.getPlayers().size())+(i%columns)*STEP_X,GRID_Y+(i/columns)*STEP_Y,0); renderEntry(r,c,end,entry); c.getMatrices().pop(); i++; }
    }
    private static void renderTeams(TextRenderer r, DrawContext c, GameRoundEndComponent end,int civilians,int vigilantes,int killers,int neutrals) {
        int total=roundTotal(civilians,vigilantes,killers), leftCols=leftColumns(total), rightCols=rightColumns(total), neutralCols=leftCols;
        drawCentered(r,c,RoleAnnouncementTexts.CIVILIAN.titleText,civilianCenter(civilians,vigilantes,killers),HEADER_Y,0xFFFFFF);
        drawCentered(r,c,RoleAnnouncementTexts.VIGILANTE.titleText,vigilanteCenter(civilians,vigilantes,killers),HEADER_Y,0xFFFFFF);
        drawCentered(r,c,RoleAnnouncementTexts.KILLER.titleText,killerCenter(civilians,vigilantes,killers),killerHeaderY(vigilantes,civilians,killers),0xFFFFFF);
        if(neutrals>0) drawCentered(r,c,translated("announcement.title.neutral", "Neutral"),neutralCenter(neutrals,neutralCols,civilians,vigilantes,killers),neutralHeaderY(civilians,total),NEUTRAL_TITLE_COLOR);
        int ci=0,vi=0,ki=0,ni=0;
        for(var entry:end.getPlayers()) {
            c.getMatrices().push();
            switch (endGroup(entry)) {
                case CIVILIAN -> { c.getMatrices().translate(civilianStartX(ci,civilians,vigilantes,killers)+(ci%leftCols)*STEP_X,GRID_Y+(ci/leftCols)*STEP_Y,0); ci++; }
                case VIGILANTE -> { c.getMatrices().translate(vigilanteStartX(vi,civilians,vigilantes,killers)+(vi%rightCols)*STEP_X,GRID_Y+(vi/rightCols)*STEP_Y,0); vi++; }
                case KILLER -> { c.getMatrices().translate(killerStartX(ki,civilians,vigilantes,killers)+(ki%rightCols)*STEP_X,killerGridY(vigilantes,civilians,killers)+(ki/rightCols)*STEP_Y,0); ki++; }
                case NEUTRAL -> { c.getMatrices().translate(neutralStartX(ni,neutrals,neutralCols,civilians,vigilantes,killers)+(ni%neutralCols)*STEP_X,neutralGridY(civilians,total)+(ni/neutralCols)*STEP_Y,0); ni++; }
                default -> { c.getMatrices().pop(); continue; }
            }
            renderEntry(r,c,end,entry); c.getMatrices().pop();
        }
    }

    private static void renderCustomVictory(TextRenderer r,DrawContext c,GameRoundEndComponent end,CustomVictory v,Text endText){
        CustomVictoryGroup group=v.winnerGroup();int other=0,winners=0;for(var e:end.getPlayers())if(group.contains(e.player().getId()))winners++;else other++;
        civilianCount=other;vigilanteCount=winners;killerCount=neutralCount=looseCount=0;int total=roundTotal(other,winners,0),left=leftColumns(total),right=rightColumns(total);
        c.getMatrices().push();
        c.getMatrices().translate(c.getScaledWindowWidth()/2f+ROOT_OFFSET_X,rootY(false,other,winners,0,0),0);
        drawCenteredScaled(r,c,endText,0,-12,2.6f,0xFFFFFF);drawCenteredScaled(r,c,customDetail(v),0,-4,1.2f,0xFFFFFF);
        if(other>0)drawCentered(r,c,translated("announcement.title.wathe.other","Other"),civilianCenter(other,winners,0),HEADER_Y,0x808080);
        if(winners>0)drawCentered(r,c,customGroupTitle(group),vigilanteCenter(other,winners,0),HEADER_Y,group.color());
        int oi=0,wi=0;for(var e:end.getPlayers()){boolean win=group.contains(e.player().getId());c.getMatrices().push();if(win){c.getMatrices().translate(vigilanteStartX(wi,other,winners,0)+(wi%right)*STEP_X,GRID_Y+(wi/right)*STEP_Y,0);wi++;}else{c.getMatrices().translate(civilianStartX(oi,other,winners,0)+(oi%left)*STEP_X,GRID_Y+(oi/left)*STEP_Y,0);oi++;}renderEntry(r,c,end,e);c.getMatrices().pop();}
        c.getMatrices().pop();
    }
    private static void renderEntry(TextRenderer r,DrawContext c,GameRoundEndComponent end,GameRoundEndComponent.RoundEndData e){renderHead(c,e);if(e.wasDead())renderDeath(r,c);RoleDisplay d=roleDisplay(e);fitText(r,c,Text.literal(e.player().getName()),LABEL_CENTER,NAME_Y,e.wasDead()?0xAFAFAF:0xFFFFFF,NAME_SCALE);if(!d.text.getString().isEmpty())fitText(r,c,d.text,LABEL_CENTER,ROLE_Y,multiply(d.color,e.wasDead()?0.7f:1f),ROLE_SCALE);}
    private static void renderHead(DrawContext c,GameRoundEndComponent.RoundEndData e){SkinTextures s=skin(e.player());if(s==null||s.texture()==null)return;float shade=e.wasDead()?0.4f:1f;RenderSystem.enableBlend();RenderSystem.setShaderColor(shade,shade,shade,1f);float size=8f*HEAD_SCALE;c.getMatrices().push();c.getMatrices().translate(LABEL_CENTER-size/2f,0,0);c.getMatrices().scale(HEAD_SCALE,HEAD_SCALE,1);c.drawTexture(s.texture(),0,0,8,8,8,8,64,64);c.getMatrices().translate(-.5,-.5,0);c.getMatrices().scale(1.125f,1.125f,1);c.drawTexture(s.texture(),0,0,40,8,8,8,64,64);c.getMatrices().pop();RenderSystem.setShaderColor(1f,1f,1f,1f);}
    private static void renderDeath(TextRenderer r,DrawContext c){float size=8f*HEAD_SCALE;float left=LABEL_CENTER-size/2f;c.getMatrices().push();c.getMatrices().translate(left+size-DEATH_RIGHT_INSET,DEATH_TOP_OFFSET,0);c.getMatrices().scale(HEAD_SCALE*DEATH_SCALE_X,HEAD_SCALE*DEATH_SCALE_Y,1);c.drawText(r,"x",-r.getWidth("x")/2,0,0xE10000,false);c.drawText(r,"x",-r.getWidth("x")/2,1,0x550000,false);c.getMatrices().pop();}
    private record RoleDisplay(Text text,int color){}
    /**
     * 结算显示优先读取 Anna 保存的职业快照，其次才读取当前角色表。
     * HarpyModLoader 会在停局/重置时清理 GameWorldComponent，不能把当前角色表作为唯一来源。
     */
    private static RoleDisplay roleDisplay(GameRoundEndComponent.RoundEndData e){
        AnnaRoundEndState.EndRoleSnapshot snapshot = endSnapshot(e.player().getId());
        if (snapshot != null && snapshot.roleId() != null) {
            return new RoleDisplay(roleText(snapshot.roleId()), snapshot.color());
        }
        GameWorldComponent g=GameWorldComponent.KEY.get(MinecraftClient.getInstance().world);
        Role roleData=g.getRole(e.player().getId());
        if(roleData!=null) return new RoleDisplay(roleText(roleData.identifier()),roleData.color());
        RoleAnnouncementTexts.RoleAnnouncementText a=e.role();
        return new RoleDisplay(a.roleText,a.colour);
    }

    private static AnnaRoundEndState.EndRoleSnapshot endSnapshot(UUID uuid) {
        if (MinecraftClient.getInstance().world == null) return null;
        return AnnaRoundEndState.KEY.get(MinecraftClient.getInstance().world).getRoleSnapshots().get(uuid);
    }

    private static AnnaRoundEndState.EndRoleGroup endGroup(GameRoundEndComponent.RoundEndData e) {
        AnnaRoundEndState.EndRoleSnapshot snapshot = endSnapshot(e.player().getId());
        if (snapshot != null) return snapshot.group();
        if (e.role() == null) return AnnaRoundEndState.EndRoleGroup.UNKNOWN;
        return e.role() == RoleAnnouncementTexts.CIVILIAN ? AnnaRoundEndState.EndRoleGroup.CIVILIAN
                    : e.role() == RoleAnnouncementTexts.VIGILANTE ? AnnaRoundEndState.EndRoleGroup.VIGILANTE
                    : e.role() == RoleAnnouncementTexts.KILLER ? AnnaRoundEndState.EndRoleGroup.KILLER
                    : e.role() == RoleAnnouncementTexts.BLANK ? AnnaRoundEndState.EndRoleGroup.NEUTRAL
                    : e.role() == RoleAnnouncementTexts.LOOSE_END ? AnnaRoundEndState.EndRoleGroup.LOOSE_END
                    : AnnaRoundEndState.EndRoleGroup.UNKNOWN;
    }

    /** 兼容 HarpyModLoader 的完整 key、旧短 key，以及原版 Wathe 的 title key。 */
    private static Text roleText(Identifier id) {
        String[] keys = {
                "announcement.role." + id.getNamespace() + "." + id.getPath(),
                "announcement.role." + id.getPath(),
                "announcement.title." + id.getNamespace() + "." + id.getPath(),
                "announcement.title." + id.getPath()
        };
        for (String key : keys) if (Language.getInstance().hasTranslation(key)) return Text.translatable(key);
        return Text.literal(pretty(id.getPath()));
    }
    private static void fitText(TextRenderer r,DrawContext c,Text t,float x,float y,int color,float preferred){if(t.getString().isEmpty())return;int width=r.getWidth(t);if(width<=0)return;float scale=Math.min(preferred,LABEL_MAX_WIDTH/(float)width);if(scale<MIN_SCALE){scale=MIN_SCALE;t=Text.literal(trim(r,t.getString(),Math.max(1,Math.round(LABEL_MAX_WIDTH/scale))));width=r.getWidth(t);}c.getMatrices().push();c.getMatrices().translate(x,y,0);c.getMatrices().scale(scale,scale,1);c.drawTextWithShadow(r,t,-width/2,0,color);c.getMatrices().pop();}
    private static void drawCentered(TextRenderer r,DrawContext c,Text t,float x,float y,int color){c.drawTextWithShadow(r,t,(int)(x-r.getWidth(t)/2f),(int)y,color);}
    /**
     * 在当前结算/欢迎公告根坐标下绘制缩放文字。
     *
     * 自改版的坐标约定是“先缩放，再使用局部 y 坐标绘制”（标题 -12、描述 0/ -4），
     * 不能先 translate 到 y 再 scale；后者会把大标题的实际基线推到描述文字附近，
     * 正是欢迎公告和结算公告重叠的原因。
     */
    private static void drawCenteredScaled(TextRenderer r,DrawContext c,Text t,float x,float y,float scale,int color){c.getMatrices().push();c.getMatrices().scale(scale,scale,1);c.drawTextWithShadow(r,t,(int)(x-r.getWidth(t)/2f), (int)y,color);c.getMatrices().pop();}

    private static int leftColumns(int total){return dynamic(total,CIVILIAN_COLUMNS,LEFT_STEP);} private static int rightColumns(int total){return dynamic(total,DOUBLE_COLUMNS,RIGHT_STEP);} private static int dynamic(int total,int base,int step){int effective=Math.max(total,DYNAMIC_BASE);return base+Math.max(0,(effective-DYNAMIC_BASE)/step);} private static int used(int total,int max){return Math.max(1,Math.min(max,Math.max(0,total)));} private static int rows(int total,int cols){return total<=0?0:(total+cols-1)/cols;} private static int roundTotal(int c,int v,int k){return Math.max(0,c)+Math.max(0,v)+Math.max(0,k)+Math.max(0,neutralCount)+Math.max(0,extraLeftCount);}
    private static float groupWidth(int cols){return SLOT_WIDTH+Math.max(0,cols-1)*STEP_X;} private static float configuredLeft(){return groupWidth(leftColumns(roundTotal(civilianCount,vigilanteCount,killerCount)));} private static float configuredRight(){return groupWidth(rightColumns(roundTotal(civilianCount,vigilanteCount,killerCount)));} private static float gap(){return Math.min(GROUP_GAP,Math.max(0,hudWidth-SIDE_PADDING*2-configuredLeft()-configuredRight()));}
    private static float clamp(float v,float min,float max){return min>max?(min+max)/2:Math.max(min,Math.min(max,v));} private static float leftArea(){float total=configuredLeft()+gap()+configuredRight();return clamp(-total/2,-hudWidth/2+SIDE_PADDING,hudWidth/2-SIDE_PADDING-configuredLeft());} private static float rightArea(){return leftArea()+configuredLeft()+gap();} private static float looseArea(){float width=groupWidth(LOOSE_COLUMNS);return clamp(-width/2,-hudWidth/2+SIDE_PADDING,hudWidth/2-SIDE_PADDING-width);}
    private static float aligned(float area,float width,float current,int mode){return mode<=-1?area:mode==0?area+Math.max(0,(width-current)/2):area+Math.max(0,width-current);} private static float rowStart(int index,int total,int cols,float area,float width,int mode){int row=Math.max(0,index/cols), totalRows=rows(total,cols), rem=total%cols;int rowCols=totalRows>0&&row==totalRows-1&&rem>0?rem:cols;return aligned(area,width,groupWidth(rowCols),mode);}
    private static float civilianWidth(int c,int v,int k){return groupWidth(used(c,leftColumns(roundTotal(c,v,k))));} private static float civilianCenter(int c,int v,int k){float w=civilianWidth(c,v,k),s=aligned(leftArea(),configuredLeft(),w,LEFT_DIRECTION);return s+w/2;}
    private static float looseCenterX(){float w=groupWidth(used(looseCount,LOOSE_COLUMNS)),s=aligned(looseArea(),groupWidth(LOOSE_COLUMNS),w,LOOSE_DIRECTION);return s+w/2;}
    private static float vigilanteCenter(int c,int v,int k){int cols=rightColumns(roundTotal(c,v,k));float w=groupWidth(used(v,cols)),s=aligned(rightArea(),configuredRight(),w,RIGHT_DIRECTION);return s+w/2;} private static float killerCenter(int c,int v,int k){int cols=rightColumns(roundTotal(c,v,k));float w=groupWidth(used(k,cols)),s=aligned(rightArea(),configuredRight(),w,RIGHT_DIRECTION);return s+w/2;}
    private static float civilianStartX(int i,int c,int v,int k){return rowStart(i,c,leftColumns(roundTotal(c,v,k)),leftArea(),configuredLeft(),LEFT_DIRECTION);} private static float vigilanteStartX(int i,int c,int v,int k){return rowStart(i,v,rightColumns(roundTotal(c,v,k)),rightArea(),configuredRight(),RIGHT_DIRECTION);} private static float killerStartX(int i,int c,int v,int k){return rowStart(i,k,rightColumns(roundTotal(c,v,k)),rightArea(),configuredRight(),RIGHT_DIRECTION);} private static float looseStartX(int i,int total){return rowStart(i,total,LOOSE_COLUMNS,looseArea(),groupWidth(LOOSE_COLUMNS),LOOSE_DIRECTION);}
    private static float extraHeaderY(int count,int cols){return GRID_Y+rows(count,cols)*STEP_Y+EXTRA_HEADER_OFFSET;} private static float extraGridY(int count,int cols){return extraHeaderY(count,cols)+SECTION_GAP;} private static float killerHeaderY(int v,int c,int k){return extraHeaderY(v,rightColumns(roundTotal(c,v,k)));} private static float killerGridY(int v,int c,int k){return extraGridY(v,rightColumns(roundTotal(c,v,k)));} private static float neutralHeaderY(int c,int total){return extraHeaderY(c,leftColumns(total));} private static float neutralGridY(int c,int total){return extraGridY(c,leftColumns(total));}
    private static float neutralCenter(int n,int cols,int c,int v,int k){return extraCenter(n,cols,c,v,k);} private static float neutralStartX(int i,int n,int cols,int c,int v,int k){return extraStart(i,n,cols,c,v,k);} private static float extraStart(int i,int total,int cols,int c,int v,int k){return rowStart(i,total,cols,leftArea(),configuredLeft(),LEFT_DIRECTION);} private static float extraCenter(int total,int cols,int c,int v,int k){float w=groupWidth(used(total,cols)),s=aligned(leftArea(),configuredLeft(),w,LEFT_DIRECTION);return s+w/2;}
    private static float sectionBottom(int total,int cols,float grid){return total<=0?HEADER_Y+10:grid+(rows(total,cols)-1)*STEP_Y+SLOT_BOTTOM;} private static float rootY(boolean loose,int c,int v,int k,int l){float preferred=hudHeight/2-ROOT_OFFSET;int total=roundTotal(c,v,k);float bottom=loose?sectionBottom(l,LOOSE_COLUMNS,GRID_Y):Math.max(Math.max(sectionBottom(c,leftColumns(total),GRID_Y),sectionBottom(neutralCount,leftColumns(total),neutralGridY(c,total))),Math.max(sectionBottom(v,rightColumns(total),GRID_Y),sectionBottom(k,rightColumns(total),killerGridY(v,c,k))));if(!loose&&extraLeftCount>0){float extraGrid=neutralCount>0?extraGridY(c,total):extraGridY(c,leftColumns(total));bottom=Math.max(bottom,sectionBottom(extraLeftCount,extraLeftColumns,extraGrid));}return Math.max(ROOT_MIN_Y,Math.min(preferred,hudHeight-BOTTOM_PADDING-bottom));}
    public static int getEndGridColumnsCivilian(){return leftColumns(roundTotal(civilianCount,vigilanteCount,killerCount));} public static int getEndGridColumnsCivilian(int total){return dynamic(total,CIVILIAN_COLUMNS,LEFT_STEP);} public static int getEndGridColumnsDouble(int total){return dynamic(total,DOUBLE_COLUMNS,RIGHT_STEP);} public static float getEndSlotStepX(){return STEP_X;} public static float getEndSlotStepY(){return STEP_Y;} public static float getEndHeaderY(){return HEADER_Y;} public static float getTeamGridStartY(){return GRID_Y;} public static float getCivilianColumnStartX(){return civilianStartX(0,civilianCount,vigilanteCount,killerCount);} public static float getCivilianColumnStartX(int i,int c,int v,int k){return civilianStartX(i,c,v,k);} public static float getVigilanteColumnStartX(int i,int c,int v,int k){return vigilanteStartX(i,c,v,k);} public static float getKillerColumnStartX(int i,int c,int v,int k){return killerStartX(i,c,v,k);} public static float getRightColumnStartX(){return vigilanteStartX(0,civilianCount,vigilanteCount,killerCount);} public static float getLooseEndColumnStartX(){return looseStartX(0,looseCount);} public static float getLooseEndColumnStartX(int i,int total){return looseStartX(i,total);} public static float getKillerHeaderY(int v){return killerHeaderY(v,civilianCount,killerCount);} public static float getKillerHeaderY(int v,int c,int k){return killerHeaderY(v,c,k);} public static float getKillerGridStartY(int v){return killerGridY(v,civilianCount,killerCount);} public static float getKillerGridStartY(int v,int c,int k){return killerGridY(v,c,k);} public static float getExtraSectionHeaderY(int previous,int cols){return extraHeaderY(previous,cols);} public static float getExtraSectionGridStartY(int previous,int cols){return extraGridY(previous,cols);} public static float getExtraSectionHeaderYForCivilian(int previous){return extraHeaderY(previous,leftColumns(roundTotal(civilianCount,vigilanteCount,killerCount)));} public static float getExtraSectionGridStartYForCivilian(int previous){return extraGridY(previous,leftColumns(roundTotal(civilianCount,vigilanteCount,killerCount)));} public static int getRowsForCount(int count,int cols){return rows(count,cols);} public static void setExternalLeftExtraSection(int count,int cols){extraLeftCount=Math.max(0,count);extraLeftColumns=Math.max(1,cols);} public static void clearExternalLeftExtraSection(){extraLeftCount=0;extraLeftColumns=CIVILIAN_COLUMNS;} public static float getCivilianGroupCenterX(int c,int v,int k){return civilianCenter(c,v,k);} public static float getCivilianGroupCenterX(){return civilianCenter(civilianCount,vigilanteCount,killerCount);} public static float getCivilianExtraSectionColumnStartX(int i,int total,int cols){return extraStart(i,total,cols,civilianCount,vigilanteCount,killerCount);} public static float getCivilianExtraSectionColumnStartX(int i,int total,int cols,int c,int v,int k){return extraStart(i,total,cols,c,v,k);} public static float getCivilianExtraSectionGroupCenterX(int total,int cols,int c,int v,int k){return extraCenter(total,cols,c,v,k);}
    private static void fitSimple(TextRenderer r,DrawContext c,Text t,float x,float y,int color){drawCentered(r,c,t,x,y,color);}
    private static String pretty(String path){StringBuilder b=new StringBuilder();for(String s:path.split("_")){if(s.isEmpty())continue;if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));}return b.toString();}
    private static int multiply(int color,float f){return Math.min(255,Math.max(0,Math.round(((color>>16)&255)*f)))<<16|Math.min(255,Math.max(0,Math.round(((color>>8)&255)*f)))<<8|Math.min(255,Math.max(0,Math.round((color&255)*f)));}
    private static String trim(TextRenderer r,String s,int max){if(r.getWidth(s)<=max)return s;String dots="...";if(r.getWidth(dots)>=max)return "";int end=s.length();while(end>0&&r.getWidth(s.substring(0,end)+dots)>max)end--;return end<=0?"":s.substring(0,end)+dots;}
    private static Text translated(String key,String fallback){return Language.getInstance().hasTranslation(key)?Text.translatable(key):Text.literal(fallback);} private static Text customAnnouncement(CustomVictory v){return translated(v.announcementTranslationKey(),v.fallbackTitle()+" Wins").copy().withColor(v.color());} private static Text customDetail(CustomVictory v){return translated(v.detailTranslationKey(),v.fallbackTitle()+" achieved an independent victory");} private static Text customGroupTitle(CustomVictoryGroup g){return translated(g.titleTranslationKey(),g.fallbackTitle()).copy().withColor(g.color());}
    private static SkinTextures skin(GameProfile profile){PlayerListEntry p=WatheClient.PLAYER_ENTRIES_CACHE.get(profile.getId());if(p!=null&&p.getSkinTextures().texture()!=null){SKIN_CACHE.put(profile.getId(),p.getSkinTextures());return p.getSkinTextures();}if(SKIN_CACHE.containsKey(profile.getId()))return SKIN_CACHE.get(profile.getId());MinecraftClient c=MinecraftClient.getInstance();if(SKIN_REQUESTS.add(profile.getId()))c.getSkinProvider().fetchSkinTextures(profile).thenAccept(s->{SKIN_CACHE.put(profile.getId(),s);SKIN_REQUESTS.remove(profile.getId());});return c.getSkinProvider().getSkinTextures(profile);}
    public static GameProfile getGameProfile(String disguise){Optional<GameProfile> p=SkullBlockEntity.fetchProfileByName(disguise).getNow(failCache(disguise));return p.orElse(failCache(disguise).get());} public static SkinTextures getSkinTextures(String disguise){return MinecraftClient.getInstance().getSkinProvider().getSkinTextures(getGameProfile(disguise));} public static Optional<GameProfile> failCache(String name){return FAIL_CACHE.computeIfAbsent(name,n->Optional.of(new GameProfile(UUID.randomUUID(),n)));}
}
