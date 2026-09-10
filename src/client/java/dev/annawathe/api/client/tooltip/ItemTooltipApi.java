package dev.annawathe.api.client.tooltip;
import net.minecraft.entity.player.PlayerEntity; import net.minecraft.item.Item; import net.minecraft.item.ItemStack; import net.minecraft.util.Identifier;
/** 对外公开的 tooltip API 门面；实现位于客户端内部包，避免扩展依赖内部实现类。 */
public final class ItemTooltipApi {
    public static final int COOLDOWN_COLOR=dev.annawathe.client.tooltip.ItemTooltipApi.COOLDOWN_COLOR;
    private ItemTooltipApi(){}
    public static void initialize(){dev.annawathe.client.tooltip.ItemTooltipApi.initialize();}
    public static void registerItem(Item item){dev.annawathe.client.tooltip.ItemTooltipApi.registerItem(item);}
    public static void registerItems(Item... items){dev.annawathe.client.tooltip.ItemTooltipApi.registerItems(items);}
    public static void registerAppender(Identifier id,int priority,Item item,dev.annawathe.client.tooltip.ItemTooltipApi.TooltipAppender app){dev.annawathe.client.tooltip.ItemTooltipApi.registerAppender(id,priority,item,app);}
    public static int getRemainingCooldownTicks(PlayerEntity player,Item item){return dev.annawathe.client.tooltip.ItemTooltipApi.getRemainingCooldownTicks(player,item);}
    public static String formatCooldownTicks(int ticks){return dev.annawathe.client.tooltip.ItemTooltipApi.formatCooldownTicks(ticks);}
}
