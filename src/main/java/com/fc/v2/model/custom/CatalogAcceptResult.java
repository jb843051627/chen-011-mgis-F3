package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisCatalogBooklet;
import com.fc.v2.model.auto.TMgisCatalogLine;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 整批核收回执。接口怎么回、账就怎么记：册头三数与逐行明细都是落库那批东西
 * 读出来的，不是另算一份。
 *
 * <p>重递同册号时 {@code duplicated} 为真，回的是头一遍的回执——
 * 不补一条账，挂着的缘故也不补第二句。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogAcceptResult", description = "图幅目录册核收回执")
public class CatalogAcceptResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册头（表上总行数、挂着、进账三数齐着） */
    @ApiModelProperty(value = "册头")
    private final TMgisCatalogBooklet booklet;

    /** 进账的行（POSTED 与 MERGED，按原表行次排） */
    @ApiModelProperty(value = "进账的行")
    private final List<TMgisCatalogLine> acceptedLines;

    /** 挂着的行（行次、代号、缘故三样齐着，按原表行次排） */
    @ApiModelProperty(value = "挂着的行")
    private final List<TMgisCatalogLine> hungLines;

    /** 是否重递（只认最早那一遍，未再落一笔） */
    @ApiModelProperty(value = "是否重递")
    private final boolean duplicated;

    public CatalogAcceptResult(TMgisCatalogBooklet booklet, List<TMgisCatalogLine> acceptedLines,
                               List<TMgisCatalogLine> hungLines, boolean duplicated) {
        this.booklet = booklet;
        this.acceptedLines = acceptedLines == null ? Collections.<TMgisCatalogLine>emptyList() : acceptedLines;
        this.hungLines = hungLines == null ? Collections.<TMgisCatalogLine>emptyList() : hungLines;
        this.duplicated = duplicated;
    }

    public TMgisCatalogBooklet getBooklet() {
        return booklet;
    }

    public List<TMgisCatalogLine> getAcceptedLines() {
        return acceptedLines;
    }

    public List<TMgisCatalogLine> getHungLines() {
        return hungLines;
    }

    public boolean isDuplicated() {
        return duplicated;
    }
}
