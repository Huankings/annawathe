package dev.annawathe.cca;
import net.minecraft.entity.player.PlayerEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
/**
 * AnnaWathe 的 CCA 工厂总表。
 *
 * <p>新增组件必须在这里登记，否则组件 key 虽然可以编译，进入世界时仍会因为没有 factory
 * 或没有在 fabric.mod.json 声明而失败。玩家外观变形使用 CHARACTER 复制以跨重生保存，
 * 特殊存活状态则使用 NEVER_COPY，避免死亡后的旁观状态污染下一条生命。</p>
 */
public final class AnnaWatheComponents implements EntityComponentInitializer, WorldComponentInitializer, ScoreboardComponentInitializer {
    @Override public void registerEntityComponentFactories(EntityComponentFactoryRegistry r) {
        r.beginRegistration(PlayerEntity.class, PlayerInstinctComponent.KEY).respawnStrategy(RespawnCopyStrategy.CHARACTER).end(PlayerInstinctComponent::new);
        r.beginRegistration(PlayerEntity.class, PlayerLifeStateComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PlayerLifeStateComponent::new);
        r.beginRegistration(PlayerEntity.class, PlayerAppearanceOverrideComponent.KEY).respawnStrategy(RespawnCopyStrategy.CHARACTER).end(PlayerAppearanceOverrideComponent::new);
        r.beginRegistration(PlayerBodyEntity.class, AnnaBodyInfoComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AnnaBodyInfoComponent::new);
    }
    @Override public void registerWorldComponentFactories(WorldComponentFactoryRegistry r) {
        r.register(AnnaRoundEndState.KEY, AnnaRoundEndState::new);
        // 心情死亡开关需要普通 CCA 同步；任务点大表持久化在 CCA，但网络发送由专用 payload 控制。
        r.register(AnnaMoodSettings.KEY, AnnaMoodSettings::new);
        r.register(AnnaTaskPointWorldState.KEY, AnnaTaskPointWorldState::new);
        r.register(AnnaCollisionSettings.KEY, AnnaCollisionSettings::new);
    }
    @Override public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry r) { r.registerScoreboardComponent(AnnaRoundEndState.KEY, AnnaRoundEndState::new); }
}
