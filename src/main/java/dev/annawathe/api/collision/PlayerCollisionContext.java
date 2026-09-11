package dev.annawathe.api.collision;

import dev.annawathe.cca.AnnaCollisionSettings;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/** 有方向的 self -> other 判定上下文；双向规则必须自行检查两边。 */
public record PlayerCollisionContext(PlayerEntity self, PlayerEntity other, World world,
                                     GameWorldComponent game, AnnaCollisionSettings settings) {}
