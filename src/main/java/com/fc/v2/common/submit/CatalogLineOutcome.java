package com.fc.v2.common.submit;

/**
 * 册内行结局：每一原始行非挂即进，进账的再分「头一条」与「并入前一条」。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum CatalogLineOutcome {

    /** 已进账：该代号在本册头一回出现的好行，建总账或向跨册总账追加，幅数计入一次 */
    POSTED("已进账"),
    /** 并入前一条：同册内代号重复的好行，并到同一条总账上，幅数不再加 */
    MERGED("并入前一条"),
    /** 挂着：四样缺样、类别不识、幅数不是正数、行次重复——缘故写全，不进总账 */
    HUNG("挂着");

    private final String label;

    CatalogLineOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
