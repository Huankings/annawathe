package dev.annawathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.annawathe.api.appearance.PlayerTransformApi;
import dev.annawathe.cca.PlayerAppearanceOverrideComponent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 外观调试命令：只改在线玩家的客户端外观 UUID，时间单位为秒。
 * permanent 状态写入 CCA/NBT，跨回合、重生和重启保留。
 */
public final class PlayerTransformCommand {
    private PlayerTransformCommand() {}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root = CommandManager.literal("annawathe:transform").requires(s -> s.hasPermissionLevel(2));
        root.then(CommandManager.literal("clearAll").executes(c -> { PlayerTransformApi.clearAll(c.getSource().getServer()); c.getSource().sendFeedback(() -> Text.literal("已解除所有玩家变形"), true); return 1; }));
        root.then(CommandManager.literal("clear").then(CommandManager.argument("player", EntityArgumentType.player()).executes(c -> { ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player"); PlayerTransformApi.clear(player); c.getSource().sendFeedback(() -> Text.literal("已解除玩家变形"), true); return 1; })));
        root.then(CommandManager.literal("query").then(CommandManager.argument("player", EntityArgumentType.player()).executes(c -> { ServerPlayerEntity player = EntityArgumentType.getPlayer(c, "player"); var state = PlayerAppearanceOverrideComponent.KEY.get(player); c.getSource().sendFeedback(() -> Text.literal(state.isActive() ? "玩家当前处于变形状态" : "玩家当前未变形"), false); return 1; })));
        var target = CommandManager.argument("player", EntityArgumentType.player());
        var appearance = CommandManager.argument("appearance", EntityArgumentType.player());
        appearance.then(CommandManager.literal("permanent").executes(c -> apply(c.getSource(), EntityArgumentType.getPlayer(c, "player"), EntityArgumentType.getPlayer(c, "appearance"), -1)));
        appearance.then(CommandManager.argument("seconds", IntegerArgumentType.integer(1)).executes(c -> apply(c.getSource(), EntityArgumentType.getPlayer(c, "player"), EntityArgumentType.getPlayer(c, "appearance"), IntegerArgumentType.getInteger(c, "seconds"))));
        target.then(appearance);
        root.then(target);
        root.then(CommandManager.literal("all").then(CommandManager.argument("appearance", EntityArgumentType.player())
                .then(CommandManager.literal("permanent").executes(c -> applyAll(c.getSource(), EntityArgumentType.getPlayer(c, "appearance"), -1)))
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1)).executes(c -> applyAll(c.getSource(), EntityArgumentType.getPlayer(c, "appearance"), IntegerArgumentType.getInteger(c, "seconds"))))));
        dispatcher.register(root);
    }
    private static int apply(ServerCommandSource source, ServerPlayerEntity player, ServerPlayerEntity appearance, int seconds) { if (player == appearance) { source.sendError(Text.literal("不能变形成自己")); return 0; } PlayerTransformApi.transform(player, appearance.getUuid(), seconds < 0 ? -1 : seconds * 20L); source.sendFeedback(() -> Text.literal("已设置玩家外观变形"), true); return 1; }
    private static int applyAll(ServerCommandSource source, ServerPlayerEntity appearance, int seconds) { int count = 0; for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) if (player != appearance) { PlayerTransformApi.transform(player, appearance.getUuid(), seconds < 0 ? -1 : seconds * 20L); count++; } final int applied = count; source.sendFeedback(() -> Text.literal("已设置 " + applied + " 名在线玩家变形"), true); return applied; }
}
