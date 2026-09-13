package dev.annawathe.api.psycho;

/** 疯魔护盾仲裁结果。PASS 继续后续规则，BLOCK 消耗护盾，BYPASS 穿透护盾。 */
public enum PsychoShieldResult {
    PASS,
    BLOCK,
    BYPASS
}
