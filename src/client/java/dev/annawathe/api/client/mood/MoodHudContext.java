package dev.annawathe.api.client.mood;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;

/** 每一帧传给扩展样式的只读心情 HUD 上下文。 */
public record MoodHudContext(PlayerEntity player, TextRenderer textRenderer, DrawContext drawContext,
                             RenderTickCounter tickCounter, GameWorldComponent gameWorld,
                             PlayerMoodComponent moodComponent, Role role, boolean hasMoodTasks,
                             float previousMood, float moodRender, float moodAlpha, float moodOffset,
                             float moodTextWidth, float warningProgress, float shakeX, float shakeY) {
    public int moodBarWidth() { return Math.max(0, Math.round(Math.max(1F, moodTextWidth - 8F) * moodRender)); }
}
