package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 年度图幅目录册整批核收请求。此门专走整批提交：一本一册连封面属地一起递，
 * 不许拆成一行行添；幅数收原样字符串，负数、不是数要照原样写进挂账缘故。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogSubmitRequest", description = "图幅目录册整批核收请求")
public class CatalogSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 图幅目录册号 */
    @ApiModelProperty(value = "图幅目录册号", required = true)
    private String bookletNo;

    /** 册子封面属地名址（整本一册一地） */
    @ApiModelProperty(value = "属地名址", required = true)
    private String region;

    /** 原表各行（行次即其在表中的次序，从 1 连排） */
    @ApiModelProperty(value = "原表各行", required = true)
    private List<Line> rows;

    /** 递进来的一行：图幅代号、成果类别、属地名址、幅数四样 */
    @ApiModel(value = "CatalogSubmitRequest.Line", description = "册内一行")
    public static class Line implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 图幅代号 */
        @ApiModelProperty(value = "图幅代号", required = true)
        private String itemCode;

        /** 成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记（中文名亦可） */
        @ApiModelProperty(value = "成果类别", required = true)
        private String prodType;

        /** 属地名址（与封面不符的行自己挂着） */
        @ApiModelProperty(value = "属地名址", required = true)
        private String region;

        /** 本行所报幅数（原样收；负数、不是数照挂） */
        @ApiModelProperty(value = "本行所报幅数", required = true)
        private String qty;

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public String getProdType() {
            return prodType;
        }

        public void setProdType(String prodType) {
            this.prodType = prodType;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getQty() {
            return qty;
        }

        public void setQty(String qty) {
            this.qty = qty;
        }
    }

    public String getBookletNo() {
        return bookletNo;
    }

    public void setBookletNo(String bookletNo) {
        this.bookletNo = bookletNo;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<Line> getRows() {
        return rows;
    }

    public void setRows(List<Line> rows) {
        this.rows = rows;
    }
}
