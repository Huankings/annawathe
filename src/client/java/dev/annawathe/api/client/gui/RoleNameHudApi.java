package dev.annawathe.api.client.gui;

import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** 完整准心名字 HUD 扩展门面：射线、目标过滤、名称、同伙与额外 HUD。 */
@Environment(EnvType.CLIENT)
public final class RoleNameHudApi {
    public static final int DEFAULT_PRIORITY=0;
    private static final List<Entry<?>> ALL=new ArrayList<>();
    private static long order;
    private RoleNameHudApi(){}
    public static synchronized void registerHudVisibility(Identifier id,int p,HudVisibilityHandler h){add(id,p,h);}
    public static synchronized void registerRaycastSource(Identifier id,int p,RaycastSourceHandler h){add(id,p,h);}
    public static synchronized void registerPlayerTargetFilter(Identifier id,int p,PlayerTargetFilter h){add(id,p,h);}
    public static synchronized void registerName(Identifier id,int p,NameHandler h){add(id,p,h);}
    public static synchronized void registerEntityName(Identifier id,int p,EntityNameHandler h){add(id,p,h);}
    public static synchronized void registerCohortState(Identifier id,int p,CohortStateHandler h){add(id,p,h);}
    public static synchronized void registerCohortTargetState(Identifier id,int p,CohortTargetStateHandler h){add(id,p,h);}
    public static synchronized void registerCohortHint(Identifier id,int p,CohortHintHandler h){add(id,p,h);}
    public static synchronized void registerExtraHud(Identifier id,int p,ExtraHudRenderer h){add(id,p,h);}
    private static void add(Identifier id,int p,Object h){ALL.removeIf(e->e.id.equals(id));ALL.add(new Entry<>(id,p,order++,h));ALL.sort(Comparator.comparingInt((Entry<?> e)->e.priority).reversed().thenComparing(Comparator.comparingLong((Entry<?> e)->e.order).reversed()));}
    private static <T> List<T> handlers(Class<T> type){synchronized(ALL){return ALL.stream().filter(e->type.isInstance(e.handler)).map(e->type.cast(e.handler)).toList();}}
    public static boolean shouldRenderHud(ClientPlayerEntity p){for(HudVisibilityHandler h:handlers(HudVisibilityHandler.class)){VisibilityResult r=h.shouldRender(p);if(r==VisibilityResult.HIDE)return false;if(r==VisibilityResult.SHOW)return true;}return true;}
    public static Entity resolveRaycastSource(ClientPlayerEntity p){for(RaycastSourceHandler h:handlers(RaycastSourceHandler.class)){Entity e=h.getRaycastSource(p);if(e!=null)return e;}return p;}
    public static boolean shouldIncludePlayerTarget(ClientPlayerEntity v,PlayerEntity t){for(PlayerTargetFilter h:handlers(PlayerTargetFilter.class)){TargetResult r=h.shouldInclude(v,t);if(r==TargetResult.DENY)return false;if(r==TargetResult.ALLOW)return true;}return true;}
    public static Text resolveName(ClientPlayerEntity v,PlayerEntity t,Text original){for(NameHandler h:handlers(NameHandler.class)){Text r=h.getName(v,t,original);if(r!=null)return r;}return original;}
    public static @Nullable Text resolveEntityName(ClientPlayerEntity v,Entity t){for(EntityNameHandler h:handlers(EntityNameHandler.class)){Text r=h.getName(v,t);if(r!=null)return r;}return null;}
    public static boolean countsAsCohort(ClientPlayerEntity v,PlayerEntity s,boolean vanilla){for(CohortStateHandler h:handlers(CohortStateHandler.class)){Boolean r=h.countsAsCohort(v,s,vanilla);if(r!=null)return r;}return vanilla;}
    public static boolean showsAsCohortTarget(ClientPlayerEntity v,PlayerEntity t,boolean vanilla){for(CohortTargetStateHandler h:handlers(CohortTargetStateHandler.class)){Boolean r=h.showsAsCohortTarget(v,t,vanilla);if(r!=null)return r;}return vanilla;}
    public static boolean shouldShowCohortHint(ClientPlayerEntity v,PlayerEntity t,boolean vanilla){for(CohortHintHandler h:handlers(CohortHintHandler.class)){VisibilityResult r=h.shouldShow(v,t,vanilla);if(r==VisibilityResult.HIDE)return false;if(r==VisibilityResult.SHOW)return true;}return vanilla;}
    public static void renderExtraHud(Context c){for(ExtraHudRenderer h:handlers(ExtraHudRenderer.class))h.render(c);}
    public static @Nullable PlayerBodyEntity findLookedAtBody(ClientPlayerEntity p,float range){var hit=ProjectileUtil.getCollision(p,e->e instanceof PlayerBodyEntity b&&TargetVisibilityApi.canTargetBody(p,b),range);return hit instanceof EntityHitResult e&&e.getEntity() instanceof PlayerBodyEntity b?b:null;}
    public static float defaultLookRange(PlayerEntity p){return p.isSpectator()||p.isCreative()?8F:2F;}
    public record Context(TextRenderer renderer,ClientPlayerEntity player,DrawContext drawContext,RenderTickCounter tickCounter,float range,@Nullable PlayerEntity targetPlayer,@Nullable Entity targetEntity,@Nullable Text displayedTargetName,float nametagAlpha,float noteAlpha){}
    @FunctionalInterface public interface HudVisibilityHandler{VisibilityResult shouldRender(ClientPlayerEntity player);}@FunctionalInterface public interface RaycastSourceHandler{Entity getRaycastSource(ClientPlayerEntity player);}@FunctionalInterface public interface PlayerTargetFilter{TargetResult shouldInclude(ClientPlayerEntity viewer,PlayerEntity target);}@FunctionalInterface public interface NameHandler{@Nullable Text getName(ClientPlayerEntity viewer,PlayerEntity target,Text original);}@FunctionalInterface public interface EntityNameHandler{@Nullable Text getName(ClientPlayerEntity viewer,Entity target);}@FunctionalInterface public interface CohortStateHandler{@Nullable Boolean countsAsCohort(ClientPlayerEntity viewer,PlayerEntity subject,boolean vanilla);}@FunctionalInterface public interface CohortTargetStateHandler{@Nullable Boolean showsAsCohortTarget(ClientPlayerEntity viewer,PlayerEntity target,boolean vanilla);}@FunctionalInterface public interface CohortHintHandler{VisibilityResult shouldShow(ClientPlayerEntity viewer,PlayerEntity target,boolean vanilla);}@FunctionalInterface public interface ExtraHudRenderer{void render(Context context);}
    public enum VisibilityResult{PASS,SHOW,HIDE} public enum TargetResult{PASS,ALLOW,DENY} private record Entry<T>(Identifier id,int priority,long order,T handler){}
}
