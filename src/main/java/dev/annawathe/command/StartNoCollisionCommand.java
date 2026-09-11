package dev.annawathe.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.annawathe.cca.AnnaCollisionSettings;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** 管理员调试 ACTIVE 开始后的玩家免碰撞保护时间。 */
public final class StartNoCollisionCommand {
    private StartNoCollisionCommand() {}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("annawathe:startnoCollision").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0)).executes(c -> { int seconds = IntegerArgumentType.getInteger(c, "seconds"); AnnaCollisionSettings.KEY.get(c.getSource().getWorld()).setStartDelaySeconds(seconds); c.getSource().sendFeedback(() -> Text.literal("开局免碰撞时间已更新"), true); return 1; }))
                .executes(c -> { AnnaCollisionSettings settings = AnnaCollisionSettings.KEY.get(c.getSource().getWorld()); c.getSource().sendFeedback(() -> Text.literal("开局免碰撞：" + settings.getStartDelaySeconds() + " 秒"), false); return 1; }));
    }
}
