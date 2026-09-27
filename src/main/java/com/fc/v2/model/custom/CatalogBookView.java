package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisCatalogBooklet;
import com.fc.v2.model.auto.TMgisCatalogLedger;
import com.fc.v2.model.auto.TMgisCatalogLine;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 翻账页面：凭册号把行目拉出来。挂着几行、进账几行、表上一共几行，
 * 三个数当场对得上（挂着加入账等于表上总行数）；进账的行带着 ledger_id，
 * 少一笔多一笔都能追到总账上具体哪一条。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogBookView", description = "图幅目录册翻账页面")
public class CatalogBookView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册头 */
    @ApiModelProperty(value = "册头")
    private final TMgisCatalogBooklet booklet;

    /** 全部册内行（按原表行次排） */
    @ApiModelProperty(value = "全部册内行")
    private final List<TMgisCatalogLine> lines;

    /** 本册进账行指到的总账条目（按图幅代号排） */
    @ApiModelProperty(value = "本册触及的总账条目")
    private final List<TMgisCatalogLedger> ledgers;

    public CatalogBookView(TMgisCatalogBooklet booklet, List<TMgisCatalogLine> lines,
                           List<TMgisCatalogLedger> ledgers) {
        this.booklet = booklet;
        this.lines = lines == null ? Collections.<TMgisCatalogLine>emptyList() : lines;
        this.ledgers = ledgers == null ? Collections.<TMgisCatalogLedger>emptyList() : ledgers;
    }

    public TMgisCatalogBooklet getBooklet() {
        return booklet;
    }

    public List<TMgisCatalogLine> getLines() {
        return lines;
    }

    public List<TMgisCatalogLedger> getLedgers() {
        return ledgers;
    }
}
