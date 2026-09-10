package dev.annawathe.cca;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
public final class AnnaWatheComponents implements EntityComponentInitializer, WorldComponentInitializer, ScoreboardComponentInitializer {
    @Override public void registerEntityComponentFactories(EntityComponentFactoryRegistry r) { r.beginRegistration(PlayerEntity.class, PlayerInstinctComponent.KEY).respawnStrategy(RespawnCopyStrategy.CHARACTER).end(PlayerInstinctComponent::new); }
    @Override public void registerWorldComponentFactories(WorldComponentFactoryRegistry r) { r.register(AnnaRoundEndState.KEY, AnnaRoundEndState::new); }
    @Override public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry r) { r.registerScoreboardComponent(AnnaRoundEndState.KEY, AnnaRoundEndState::new); }
}
