package dev.annawathe.api.body;

import dev.annawathe.cca.AnnaBodyInfoComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** 客户端读取的尸体信息快照；死亡摘要包含时间和死因两个字段。 */
public record BodyInfoSnapshot(Identifier deathReason, @Nullable Identifier roleId, long deathWorldTime) {
    public int elapsedSeconds(PlayerBodyEntity body) {
        long ticks = deathWorldTime >= 0 ? Math.max(0L, body.getWorld().getTime() - deathWorldTime) : Math.max(0, body.age);
        return (int)(ticks / 20L);
    }
    public static BodyInfoSnapshot from(PlayerBodyEntity body) {
        AnnaBodyInfoComponent component = AnnaBodyInfoComponent.KEY.get(body);
        return new BodyInfoSnapshot(component.getDeathReason(), component.getRoleId(), component.getDeathWorldTime());
    }
}
