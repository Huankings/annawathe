package dev.annawathe.api.visibility;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * 统一回答玩家/尸体的渲染、准心目标、交互和攻击权限。
 * 客户端调用只影响显示；服务端物品和 C2S 接收器仍必须再次校验。
 */
public final class TargetVisibilityApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry<PlayerRule>> PLAYERS = new ArrayList<>();
    private static final List<Entry<BodyRule>> BODIES = new ArrayList<>();
    private static long order;
    private TargetVisibilityApi() {}
    public static synchronized void registerPlayerRule(Identifier id, int priority, PlayerRule rule) { register(PLAYERS,id,priority,rule); }
    public static synchronized void registerBodyRule(Identifier id, int priority, BodyRule rule) { register(BODIES,id,priority,rule); }
    private static <T> void register(List<Entry<T>> list, Identifier id, int priority, T rule) { Objects.requireNonNull(id); Objects.requireNonNull(rule); list.removeIf(e->e.id.equals(id)); list.add(new Entry<>(id,priority,order++,rule)); list.sort(Comparator.comparingInt((Entry<T> e)->e.priority).reversed().thenComparing(Comparator.comparingLong((Entry<T> e)->e.order).reversed())); }
    public static boolean canRenderPlayer(@Nullable PlayerEntity viewer, PlayerEntity target){return player(viewer,target,Action.RENDER);}
    public static boolean canTargetPlayer(@Nullable PlayerEntity viewer, PlayerEntity target){return player(viewer,target,Action.TARGET);}
    public static boolean canInteractWithPlayer(@Nullable PlayerEntity viewer, PlayerEntity target){return player(viewer,target,Action.INTERACT);}
    public static boolean canAttackPlayer(@Nullable PlayerEntity viewer, PlayerEntity target){return player(viewer,target,Action.ATTACK);}
    public static boolean canRenderBody(@Nullable PlayerEntity viewer, PlayerBodyEntity body){return body(viewer,body,Action.RENDER);}
    public static boolean canTargetBody(@Nullable PlayerEntity viewer, PlayerBodyEntity body){return body(viewer,body,Action.TARGET);}
    public static boolean canInteractWithBody(@Nullable PlayerEntity viewer, PlayerBodyEntity body){return body(viewer,body,Action.INTERACT);}
    public static boolean canAttackBody(@Nullable PlayerEntity viewer, PlayerBodyEntity body){return body(viewer,body,Action.ATTACK);}
    public static boolean canRenderEntity(@Nullable PlayerEntity viewer, Entity entity){return entity instanceof PlayerEntity p?canRenderPlayer(viewer,p):entity instanceof PlayerBodyEntity b?canRenderBody(viewer,b):true;}
    public static boolean canTargetEntity(@Nullable PlayerEntity viewer, Entity entity){return entity instanceof PlayerEntity p?canTargetPlayer(viewer,p):entity instanceof PlayerBodyEntity b?canTargetBody(viewer,b):true;}
    public static boolean canInteractWithEntity(@Nullable PlayerEntity viewer, Entity entity){return entity instanceof PlayerEntity p?canInteractWithPlayer(viewer,p):entity instanceof PlayerBodyEntity b?canInteractWithBody(viewer,b):true;}
    public static boolean canAttackEntity(@Nullable PlayerEntity viewer, Entity entity){return entity instanceof PlayerEntity p?canAttackPlayer(viewer,p):entity instanceof PlayerBodyEntity b?canAttackBody(viewer,b):true;}
    private static boolean player(@Nullable PlayerEntity viewer, PlayerEntity target, Action action){if(viewer==null)return true; List<Entry<PlayerRule>> s; synchronized(PLAYERS){s=List.copyOf(PLAYERS);} for(Entry<PlayerRule> e:s){Decision d=e.rule.resolve(new PlayerContext(viewer,target,action));if(d==Decision.ALLOW)return true;if(d==Decision.DENY)return false;}return true;}
    private static boolean body(@Nullable PlayerEntity viewer, PlayerBodyEntity target, Action action){if(viewer==null)return true; List<Entry<BodyRule>> s; synchronized(BODIES){s=List.copyOf(BODIES);} for(Entry<BodyRule> e:s){Decision d=e.rule.resolve(new BodyContext(viewer,target,action));if(d==Decision.ALLOW)return true;if(d==Decision.DENY)return false;}return true;}
    @FunctionalInterface public interface PlayerRule { Decision resolve(PlayerContext context); }
    @FunctionalInterface public interface BodyRule { Decision resolve(BodyContext context); }
    public record PlayerContext(PlayerEntity viewer, PlayerEntity target, Action action){}
    public record BodyContext(PlayerEntity viewer, PlayerBodyEntity body, Action action){}
    public enum Action { RENDER,TARGET,INTERACT,ATTACK }
    public enum Decision { PASS,ALLOW,DENY }
    private record Entry<T>(Identifier id,int priority,long order,T rule){}
}
