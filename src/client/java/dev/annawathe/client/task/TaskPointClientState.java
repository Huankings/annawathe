package dev.annawathe.client.task;

import dev.annawathe.api.task.MoodTaskApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 当前连接的任务点快照和玩家本地开关。 */
public final class TaskPointClientState {
    private static boolean enabled;
    private static final HashMap<BlockPos,Set<Identifier>> POINTS=new HashMap<>();
    private TaskPointClientState(){}
    public static boolean isEnabled(){return enabled;}
    public static boolean toggle(){enabled=!enabled;return enabled;}
    public static void replace(Map<BlockPos,Set<Identifier>> points){POINTS.clear();points.forEach((p,i)->POINTS.put(p.toImmutable(),Set.copyOf(i)));}
    public static Map<BlockPos,Set<Identifier>> snapshot(){HashMap<BlockPos,Set<Identifier>> copy=new HashMap<>();POINTS.forEach((p,i)->copy.put(p,Set.copyOf(i)));return copy;}
    public static void clear(){POINTS.clear();enabled=false;}
    public static Set<Identifier> visibleTaskTypes(PlayerEntity player){LinkedHashSet<Identifier> result=new LinkedHashSet<>();for(Identifier task:MoodTaskApi.getActiveTaskIds(player))result.addAll(MoodTaskApi.getTaskPointIds(task));return result;}
}
