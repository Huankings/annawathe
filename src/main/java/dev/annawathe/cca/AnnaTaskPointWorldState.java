package dev.annawathe.cca;

import dev.annawathe.AnnaWathe;
import dev.annawathe.api.task.MoodTaskPointApi;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 世界级任务点缓存；大表使用专用 payload 同步，避免跟随普通 CCA 更新反复整表发送。 */
public final class AnnaTaskPointWorldState implements AutoSyncedComponent {
    public static final ComponentKey<AnnaTaskPointWorldState> KEY = ComponentRegistry.getOrCreate(AnnaWathe.id("task_points"), AnnaTaskPointWorldState.class);
    private final HashMap<BlockPos, Set<Identifier>> points = new HashMap<>();
    private boolean autoRefresh = true;
    public AnnaTaskPointWorldState(World world) {}
    public Map<BlockPos, Set<Identifier>> snapshot() { return copy(points); }
    public void replace(Map<BlockPos, Set<Identifier>> value) { points.clear(); points.putAll(copy(value)); }
    public int size() { return points.size(); }
    public boolean isAutoRefresh() { return autoRefresh; }
    public void setAutoRefresh(boolean value) { autoRefresh = value; }
    private static HashMap<BlockPos, Set<Identifier>> copy(Map<BlockPos, Set<Identifier>> input) {
        HashMap<BlockPos, Set<Identifier>> result = new HashMap<>();
        input.forEach((pos, ids) -> result.put(pos.toImmutable(), Set.copyOf(ids)));
        return result;
    }
    @Override public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putBoolean("AutoRefresh", autoRefresh); NbtList list = new NbtList();
        for (var entry : points.entrySet()) { NbtCompound n = new NbtCompound(); n.putLong("Pos", entry.getKey().asLong()); NbtList ids = new NbtList(); for(Identifier id:entry.getValue())ids.add(NbtString.of(id.toString())); n.put("Types",ids); list.add(n); }
        tag.put("Points", list);
    }
    @Override public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        points.clear(); autoRefresh = !tag.contains("AutoRefresh") || tag.getBoolean("AutoRefresh");
        for(NbtElement e:tag.getList("Points", NbtElement.COMPOUND_TYPE)){NbtCompound n=(NbtCompound)e;LinkedHashSet<Identifier> ids=new LinkedHashSet<>();
            for(NbtElement raw:n.getList("Types",NbtElement.STRING_TYPE)){Identifier id=Identifier.tryParse(raw.asString());if(id!=null&&MoodTaskPointApi.isRegistered(id))ids.add(id);}if(!ids.isEmpty())points.put(BlockPos.fromLong(n.getLong("Pos")),ids);}
    }
}
