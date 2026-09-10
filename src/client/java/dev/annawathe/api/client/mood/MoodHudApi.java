package dev.annawathe.api.client.mood;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** 扩展职业在客户端注册心情和疯魔 HUD 样式的稳定入口。 */
public final class MoodHudApi {
    public static final int DEFAULT_PRIORITY=0;
    private static final Map<Role,MoodHudStyle> ROLE_STYLES=new HashMap<>();
    private static final ArrayList<MoodEntry> MOOD_PROVIDERS=new ArrayList<>();
    private static final ArrayList<PsychoEntry> PSYCHO_PROVIDERS=new ArrayList<>();
    private static final Set<GameMode> MODES=new HashSet<>();private static final ArrayList<ModeEntry> MODE_PREDICATES=new ArrayList<>();private static long order;
    private MoodHudApi(){}
    public static void registerRoleStyle(Role role,MoodHudStyle style){ROLE_STYLES.put(role,style);}
    public static void registerMoodProvider(Identifier id,int priority,MoodStyleProvider provider){MOOD_PROVIDERS.removeIf(e->e.id().equals(id));MOOD_PROVIDERS.add(new MoodEntry(id,priority,order++,provider));sort(MOOD_PROVIDERS);}
    public static void registerPsychoStyle(Identifier id,int priority,PsychoStyleProvider provider){PSYCHO_PROVIDERS.removeIf(e->e.id().equals(id));PSYCHO_PROVIDERS.add(new PsychoEntry(id,priority,order++,provider));sort(PSYCHO_PROVIDERS);}
    public static void registerVisibleGameMode(GameMode mode){MODES.add(mode);}
    public static void registerVisibleGameModePredicate(Identifier id,int priority,Predicate<GameMode> predicate){MODE_PREDICATES.removeIf(e->e.id().equals(id));MODE_PREDICATES.add(new ModeEntry(id,priority,order++,predicate));sort(MODE_PREDICATES);}
    private static <T extends Prioritized> void sort(List<T> list){list.sort(Comparator.<T>comparingInt(Prioritized::priority).reversed().thenComparing(Comparator.comparingLong(Prioritized::order).reversed()));}
    public static boolean shouldRender(GameMode mode){if(mode==WatheGameModes.MURDER||MODES.contains(mode))return true;for(var e:MODE_PREDICATES)if(e.predicate().test(mode))return true;return false;}
    public static @Nullable MoodHudStyle resolve(MoodHudContext context){for(var e:MOOD_PROVIDERS){MoodHudStyle s=e.provider().get(context);if(s!=null)return s;}return ROLE_STYLES.get(context.role());}
    public static @Nullable PsychoMoodHudStyle resolvePsycho(MoodHudContext context,PlayerPsychoComponent psycho){for(var e:PSYCHO_PROVIDERS){var s=e.provider().get(context,psycho);if(s!=null)return s;}return null;}
    @FunctionalInterface public interface MoodStyleProvider{@Nullable MoodHudStyle get(MoodHudContext context);}
    @FunctionalInterface public interface PsychoStyleProvider{@Nullable PsychoMoodHudStyle get(MoodHudContext context,PlayerPsychoComponent psycho);}
    private interface Prioritized{int priority();long order();}
    private record MoodEntry(Identifier id,int priority,long order,MoodStyleProvider provider)implements Prioritized{}
    private record PsychoEntry(Identifier id,int priority,long order,PsychoStyleProvider provider)implements Prioritized{}
    private record ModeEntry(Identifier id,int priority,long order,Predicate<GameMode> predicate)implements Prioritized{}
}
