package dev.annawathe.api.collision;

/** 玩家碰撞规则结果；PASS 交给后续规则/Anna 默认规则。 */
public enum PlayerCollisionMode {
    PASS(false, true), SOLID(true, true), VANILLA_PUSH(false, true), NO_COLLISION(false, false);
    private final boolean blocksMovement;
    private final boolean allowsVanillaPush;
    PlayerCollisionMode(boolean blocksMovement, boolean allowsVanillaPush) { this.blocksMovement = blocksMovement; this.allowsVanillaPush = allowsVanillaPush; }
    public boolean blocksMovement() { return blocksMovement; }
    public boolean allowsVanillaPush() { return allowsVanillaPush; }
}
