package dev.annawathe.api.instinct;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
/** 本能资格与目标颜色的优先级注册表。扩展只注册规则，不直接 Mixin WatheClient。 */
public final class InstinctApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry<AvailabilityHandler>> AVAIL = new ArrayList<>();
    private static final List<Entry<HighlightHandler>> HIGHLIGHT = new ArrayList<>();
    private static long order;
    private InstinctApi() {}
    public static synchronized void registerAvailability(Identifier id,int priority,AvailabilityHandler h){ put(AVAIL,id,priority,h); }
    public static synchronized void registerHighlight(Identifier id,int priority,HighlightHandler h){ put(HIGHLIGHT,id,priority,h); }
    private static <T> void put(List<Entry<T>> list, Identifier id,int p,T h){
        list.removeIf(e->e.id.equals(id));
        list.add(new Entry<>(id,p,order++,h));
        /*
         * 先按 priority 降序，再按注册顺序倒序。
         * 不能在 thenComparingLong 之后直接调用 reversed()，否则会把整个
         * comparator 再反转一次，导致低 priority 的 Anna 默认规则先短路返回。
         */
        list.sort(Comparator.<Entry<T>>comparingInt(e->e.priority).reversed()
                .thenComparing(Comparator.comparingLong((Entry<T> e)->e.order).reversed()));
    }
    public static AvailabilityResult resolveAvailability(PlayerEntity p){ for(Entry<AvailabilityHandler> e: List.copyOf(AVAIL)){ AvailabilityResult r=e.handler.get(p); if(r!=null&&r!=AvailabilityResult.PASS)return r;} return AvailabilityResult.PASS; }
    public static HighlightResult resolveHighlight(PlayerEntity p,Entity t){ for(Entry<HighlightHandler> e: List.copyOf(HIGHLIGHT)){ HighlightResult r=e.handler.get(p,t); if(r!=null&&r.action()!=HighlightResult.Action.PASS)return r;} return HighlightResult.pass(); }
    @FunctionalInterface public interface AvailabilityHandler { AvailabilityResult get(PlayerEntity viewer); }
    public enum AvailabilityResult { PASS, ENABLE, DISABLE }
    @FunctionalInterface public interface HighlightHandler { HighlightResult get(PlayerEntity viewer,Entity target); }
    public record HighlightResult(Action action,int color){ public static HighlightResult pass(){return new HighlightResult(Action.PASS,-1);} public static HighlightResult color(int c){return new HighlightResult(Action.COLOR,c);} public static HighlightResult hide(){return new HighlightResult(Action.HIDE,-1);} public enum Action{PASS,COLOR,HIDE} }
    private record Entry<T>(Identifier id,int priority,long order,T handler){}
}
