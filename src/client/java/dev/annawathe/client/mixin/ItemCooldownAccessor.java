package dev.annawathe.client.mixin;
import net.minecraft.entity.player.ItemCooldownManager; import net.minecraft.item.Item; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.gen.Accessor; import java.util.Map;
/** 读取原版 ItemCooldownManager 的当前条目，供 Tooltip 显示实际剩余时间。 */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownAccessor { @Accessor("entries") Map<Item, ?> annawathe$entries(); @Accessor("tick") int annawathe$tick(); }
