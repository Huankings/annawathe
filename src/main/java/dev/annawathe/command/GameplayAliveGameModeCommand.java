package dev.annawathe.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.annawathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

/**
 * 仅用于调试的特殊游戏模式命令。
 * 普通 /gamemode 不会授予特殊存活；只有本命令在目标有本局职业时才会授予授权。
 */
public final class GameplayAliveGameModeCommand {
    private GameplayAliveGameModeCommand() {}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("annawathe:gamemode").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("mode", GameModeArgumentType.gameMode())
                        .executes(c -> execute(c.getSource(), GameModeArgumentType.getGameMode(c, "mode"), c.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player()).executes(c -> execute(c.getSource(), GameModeArgumentType.getGameMode(c, "mode"), EntityArgumentType.getPlayer(c, "player"))))));
    }
    private static int execute(ServerCommandSource source, GameMode mode, ServerPlayerEntity target) {
        if (PlayerLifeStateApi.isNonSurvivalMode(mode) && GameWorldComponent.KEY.get(target.getWorld()).getRole(target) == null) { PlayerLifeStateApi.clearAliveOverride(target); source.sendError(Text.literal("该玩家未参与当前对局")); return 0; }
        if (PlayerLifeStateApi.isNonSurvivalMode(mode)) PlayerLifeStateApi.changeGameModeAsGameplayAlive(target, mode);
        else { PlayerLifeStateApi.clearAliveOverride(target); target.changeGameMode(mode); }
        source.sendFeedback(() -> Text.literal("已切换玩家游戏模式，并更新 Wathe 玩法存活标记"), true);
        return 1;
    }
}
