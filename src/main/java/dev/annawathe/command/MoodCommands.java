package dev.annawathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.annawathe.api.task.MoodTaskApi;
import dev.annawathe.cca.AnnaMoodSettings;
import dev.annawathe.cca.AnnaTaskPointWorldState;
import dev.annawathe.task.TaskPointSyncManager;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/** 与自改 Wathe 命名一致的管理员调试指令，便于两条维护线使用同一套测试步骤。 */
public final class MoodCommands {
    private MoodCommands() {}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:setMood").requires(s->s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount",FloatArgumentType.floatArg(0F,1F))
                        .executes(c->setMood(c.getSource(),List.of(c.getSource().getPlayerOrThrow()),FloatArgumentType.getFloat(c,"amount")))
                        .then(CommandManager.argument("targets",EntityArgumentType.players()).executes(c->setMood(c.getSource(),EntityArgumentType.getPlayers(c,"targets"),FloatArgumentType.getFloat(c,"amount"))))));
        dispatcher.register(CommandManager.literal("wathe:moodEffectDeath").requires(s->s.hasPermissionLevel(2))
                .executes(c->queryDeath(c.getSource()))
                .then(CommandManager.argument("enabled",BoolArgumentType.bool()).executes(c->setDeath(c.getSource(),BoolArgumentType.getBool(c,"enabled")))));
        dispatcher.register(CommandManager.literal("wathe:moodTask").requires(s->s.hasPermissionLevel(2))
                .then(CommandManager.literal("list").executes(c->listTasks(c.getSource())))
                .then(taskOperation("assign",(p,id)->MoodTaskApi.assignTask(p,id).success()))
                .then(taskOperation("remove",(p,id)->MoodTaskApi.removeTask(p,id).success()))
                .then(taskOperation("complete",(p,id)->MoodTaskApi.completeTask(p,id,true).success())));
        dispatcher.register(CommandManager.literal("wathe:taskPoints").requires(s->s.hasPermissionLevel(2))
                .executes(c->taskPointStatus(c.getSource()))
                .then(CommandManager.literal("reload").executes(c->reloadPoints(c.getSource())))
                .then(CommandManager.literal("refresh").executes(c->refreshPoints(c.getSource())))
                .then(CommandManager.literal("autoRefresh").executes(c->queryAuto(c.getSource()))
                        .then(CommandManager.argument("enabled",BoolArgumentType.bool()).executes(c->setAuto(c.getSource(),BoolArgumentType.getBool(c,"enabled"))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> taskOperation(String name, TaskAction action) {
        return CommandManager.literal(name).then(CommandManager.argument("task",IdentifierArgumentType.identifier())
                .suggests((c,b)->CommandSource.suggestIdentifiers(MoodTaskApi.getRegisteredTaskIds(),b))
                .executes(c->runTask(c.getSource(),IdentifierArgumentType.getIdentifier(c,"task"),c.getSource().getPlayerOrThrow(),name,action))
                .then(CommandManager.argument("target",EntityArgumentType.player()).executes(c->runTask(c.getSource(),IdentifierArgumentType.getIdentifier(c,"task"),EntityArgumentType.getPlayer(c,"target"),name,action))));
    }
    private static int runTask(ServerCommandSource source,Identifier id,ServerPlayerEntity target,String actionName,TaskAction action){boolean ok=action.run(target,id);var message=Text.literal((ok?"心情任务操作成功 ":"心情任务操作失败 ")+actionName+": ").append(Text.literal(id.toString()).formatted(Formatting.GOLD)).append(Text.literal(" -> ")).append(target.getDisplayName());if(ok)source.sendFeedback(()->message.formatted(Formatting.GREEN),true);else source.sendError(message.formatted(Formatting.RED));return ok?1:0;}
    private static int listTasks(ServerCommandSource source){String text=String.join(", ",MoodTaskApi.getRegisteredTaskIds().stream().map(Identifier::toString).toList());source.sendFeedback(()->Text.literal("已注册心情任务："+text),false);return MoodTaskApi.getRegisteredTaskIds().size();}
    private static int setMood(ServerCommandSource source,Collection<ServerPlayerEntity> targets,float amount){targets.forEach(p->PlayerMoodComponent.KEY.get(p).setMood(amount));source.sendFeedback(()->Text.literal("已将 "+targets.size()+" 名玩家的心情设置为 "+amount),true);return targets.size();}
    private static int setDeath(ServerCommandSource source,boolean enabled){AnnaMoodSettings.KEY.get(source.getWorld()).setMoodDeathEnabled(enabled);source.sendFeedback(()->Text.literal("心情死亡机制已设置为："+(enabled?"开启":"关闭")),true);return 1;}
    private static int queryDeath(ServerCommandSource source){boolean enabled=AnnaMoodSettings.KEY.get(source.getWorld()).isMoodDeathEnabled();source.sendFeedback(()->Text.literal("心情死亡机制当前状态："+(enabled?"开启":"关闭")),false);return 1;}
    private static int reloadPoints(ServerCommandSource source){TaskPointSyncManager.reloadAndBroadcast(source.getWorld());return pointFeedback(source,"任务点记录已重载并同步");}
    private static int refreshPoints(ServerCommandSource source){TaskPointSyncManager.broadcast(source.getWorld());return pointFeedback(source,"任务点缓存已重新同步");}
    private static int pointFeedback(ServerCommandSource source,String prefix){int count=AnnaTaskPointWorldState.KEY.get(source.getWorld()).size();source.sendFeedback(()->Text.literal(prefix+"，共 "+count+" 个坐标"),true);return count;}
    private static int setAuto(ServerCommandSource source,boolean enabled){AnnaTaskPointWorldState.KEY.get(source.getWorld()).setAutoRefresh(enabled);source.sendFeedback(()->Text.literal("任务点开局自动重载："+(enabled?"开启":"关闭")),true);return 1;}
    private static int queryAuto(ServerCommandSource source){boolean enabled=AnnaTaskPointWorldState.KEY.get(source.getWorld()).isAutoRefresh();source.sendFeedback(()->Text.literal("任务点开局自动重载当前状态："+(enabled?"开启":"关闭")),false);return 1;}
    private static int taskPointStatus(ServerCommandSource source){AnnaTaskPointWorldState s=AnnaTaskPointWorldState.KEY.get(source.getWorld());source.sendFeedback(()->Text.literal("当前任务点坐标数："+s.size()+"，开局自动重载："+(s.isAutoRefresh()?"开启":"关闭")),false);return s.size();}
    @FunctionalInterface private interface TaskAction { boolean run(ServerPlayerEntity player,Identifier id); }
}
