package dev.annawathe.network;

import dev.annawathe.AnnaWathe;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 服务端到客户端的完整任务点快照，类型使用 Identifier，不受 enum bitmask 位数限制。 */
public record TaskPointSyncPayload(Map<BlockPos, Set<Identifier>> points) implements CustomPayload {
    public static final Id<TaskPointSyncPayload> ID = new Id<>(AnnaWathe.id("task_point_sync"));
    public static final PacketCodec<PacketByteBuf, TaskPointSyncPayload> CODEC = PacketCodec.of(TaskPointSyncPayload::write, TaskPointSyncPayload::read);
    public TaskPointSyncPayload { HashMap<BlockPos,Set<Identifier>> copy=new HashMap<>();points.forEach((p,i)->copy.put(p.toImmutable(),Set.copyOf(i)));points=copy; }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
    private void write(PacketByteBuf buf) { buf.writeVarInt(points.size());for(var e:points.entrySet()){buf.writeBlockPos(e.getKey());buf.writeVarInt(e.getValue().size());for(Identifier id:e.getValue())buf.writeIdentifier(id);} }
    private static TaskPointSyncPayload read(PacketByteBuf buf) { int size=buf.readVarInt();HashMap<BlockPos,Set<Identifier>> map=new HashMap<>();for(int i=0;i<size;i++){BlockPos p=buf.readBlockPos();int count=buf.readVarInt();LinkedHashSet<Identifier> ids=new LinkedHashSet<>();for(int j=0;j<count;j++)ids.add(buf.readIdentifier());map.put(p,ids);}return new TaskPointSyncPayload(map); }
}
