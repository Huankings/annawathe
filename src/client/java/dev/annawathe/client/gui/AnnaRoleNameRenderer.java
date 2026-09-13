package dev.annawathe.client.gui;

import dev.annawathe.api.client.gui.RoleNameHudApi;
import dev.annawathe.api.client.gui.BodyInfoHudApi;
import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.entity.NoteEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;

/** Anna 的唯一准心名字 renderer；目标、名称、同伙和额外 HUD 都来自 RoleNameHudApi。 */
public final class AnnaRoleNameRenderer {
    private static float nameAlpha, noteAlpha;
    private static Text name = Text.empty();
    private static final Text[] notes = {Text.empty(), Text.empty(), Text.empty(), Text.empty()};
    private static PlayerEntity targetPlayer;
    private static Entity targetEntity;
    private AnnaRoleNameRenderer() {}
    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter counter) {
        targetPlayer = null; targetEntity = null;
        if (!RoleNameHudApi.shouldRenderHud(player)) { nameAlpha = noteAlpha = 0; return; }
        boolean observer = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!observer && player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3 && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10) return;
        float range = RoleNameHudApi.defaultLookRange(player);
        Entity source = RoleNameHudApi.resolveRaycastSource(player);
        EntityHitResult hit = ProjectileUtil.getCollision(source, entity -> entity instanceof PlayerEntity t ? TargetVisibilityApi.canTargetPlayer(player,t) && RoleNameHudApi.shouldIncludePlayerTarget(player,t) : TargetVisibilityApi.canTargetEntity(player,entity) && RoleNameHudApi.resolveEntityName(player,entity)!=null, range) instanceof EntityHitResult result ? result : null;
        float delta = counter.getTickDelta(true);
        if (hit != null) {
            Entity entity = hit.getEntity(); targetEntity = entity; nameAlpha = MathHelper.lerp(delta/4F,nameAlpha,1F);
            if (entity instanceof PlayerEntity target) {
                targetPlayer = target; Text original = target.getDisplayName(); name = RoleNameHudApi.resolveName(player,target,original);
                GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
                boolean viewerCohort = RoleNameHudApi.countsAsCohort(player,player,game.canUseKillerFeatures(player));
                boolean targetCohort = RoleNameHudApi.showsAsCohortTarget(player,target,RoleNameHudApi.countsAsCohort(player,target,game.canUseKillerFeatures(target)));
                if (PlayerPsychoComponent.KEY.get(target).getPsychoTicks()>0) name=Text.literal("urscrewed"+"X".repeat(player.getRandom().nextInt(8))).styled(s->s.withFormatting(Formatting.OBFUSCATED,Formatting.DARK_RED));
                renderName(renderer,context,name,nameAlpha,viewerCohort&&targetCohort&&RoleNameHudApi.shouldShowCohortHint(player,target,true));
            } else { Text entityName=RoleNameHudApi.resolveEntityName(player,entity); if(entityName!=null){name=entityName;renderName(renderer,context,name,nameAlpha,false);} }
        } else nameAlpha=MathHelper.lerp(delta/4F,nameAlpha,0F);
        EntityHitResult noteHit=ProjectileUtil.getCollision(source,e->e instanceof NoteEntity,range) instanceof EntityHitResult result?result:null;
        if(noteHit!=null&&noteHit.getEntity() instanceof NoteEntity note){noteAlpha=MathHelper.lerp(delta/4F,noteAlpha,1F);nameAlpha=0F;for(int i=0;i<4;i++)notes[i]=Text.literal(note.getLines()[i]);}else noteAlpha=MathHelper.lerp(delta/4F,noteAlpha,0F);
        if(noteAlpha>.05F){context.getMatrices().push();context.getMatrices().translate(context.getScaledWindowWidth()/2F,context.getScaledWindowHeight()/2F+6,0);context.getMatrices().scale(.6F,.6F,1F);for(int i=0;i<4;i++){int w=renderer.getWidth(notes[i]);context.drawTextWithShadow(renderer,notes[i],-w/2,16+i*(renderer.fontHeight+2),MathHelper.packRgb(1F,1F,1F)|((int)(noteAlpha*255)<<24));}context.getMatrices().pop();}
        BodyInfoHudApi.render(player, renderer, context, range);
        RoleNameHudApi.renderExtraHud(new RoleNameHudApi.Context(renderer,player,context,counter,range,targetPlayer,targetEntity,name,nameAlpha,noteAlpha));
    }
    private static void renderName(TextRenderer renderer,DrawContext context,Text text,float alpha,boolean cohort){if(alpha<=.05F)return;context.getMatrices().push();context.getMatrices().translate(context.getScaledWindowWidth()/2F,context.getScaledWindowHeight()/2F+6,0);context.getMatrices().scale(.6F,.6F,1F);int w=renderer.getWidth(text);context.drawTextWithShadow(renderer,text,-w/2,16,MathHelper.packRgb(1F,1F,1F)|((int)(alpha*255)<<24));if(cohort){context.getMatrices().translate(0,20+renderer.fontHeight,0);MutableText t=Text.translatable("game.tip.cohort");int tw=renderer.getWidth(t);context.drawTextWithShadow(renderer,t,-tw/2,0,MathHelper.packRgb(1F,0F,0F)|((int)(alpha*255)<<24));}context.getMatrices().pop();}
    public static @Nullable PlayerEntity getTargetPlayer(){return targetPlayer;}
}
