package dev.annawathe.task;

import dev.annawathe.cca.AnnaTaskPointWorldState;
import dev.annawathe.network.TaskPointSyncPayload;
import dev.doctor4t.wathe.api.event.GameEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** 开局、进服和调试指令共用的任务点扫描与同步调度器。 */
public final class TaskPointSyncManager {
    private static boolean initialized;
    private TaskPointSyncManager() {}
    public static void initialize(){if(initialized)return;initialized=true;
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(handler.player));
        GameEvents.ON_FINISH_INITIALIZE.register((world,game)->{if(world instanceof ServerWorld sw){if(AnnaTaskPointWorldState.KEY.get(sw).isAutoRefresh())reloadAndBroadcast(sw);else broadcast(sw);}});
    }
    public static void reloadAndBroadcast(ServerWorld world){AnnaTaskPointWorldState.KEY.get(world).replace(TaskPointScanner.scan(world));broadcast(world);}
    public static void broadcast(ServerWorld world){TaskPointSyncPayload payload=new TaskPointSyncPayload(AnnaTaskPointWorldState.KEY.get(world).snapshot());for(ServerPlayerEntity p:world.getPlayers())ServerPlayNetworking.send(p,payload);}
    public static void send(ServerPlayerEntity player){if(player.getWorld() instanceof ServerWorld sw)ServerPlayNetworking.send(player,new TaskPointSyncPayload(AnnaTaskPointWorldState.KEY.get(sw).snapshot()));}
}
