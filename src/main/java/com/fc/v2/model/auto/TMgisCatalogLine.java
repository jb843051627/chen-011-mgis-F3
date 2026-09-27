package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 年度图幅目录册册内行对象 t_mgis_catalog_line
 *
 * <p>每一原始行都落一条（含挂着的），行次、代号、类别、属地四样带着；
 * 只增不改——不设 update/del_flag，服务层无改删入口。
 * 结局 POSTED/MERGED/HUNG 由 {@link com.fc.v2.common.submit.CatalogAccept}
 * 在核收推演时逐行定，ledger_id 指到它并进的那条总账。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_catalog_line")
@ApiModel(value = "TMgisCatalogLine", description = "年度图幅目录册册内行")
public class TMgisCatalogLine implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 册头主键 */
    @TableField("booklet_id")
    @ApiModelProperty(value = "册头主键")
    private Long bookletId;

    /** 自己在原表里的行次(从1连排) */
    @TableField("row_no")
    @ApiModelProperty(value = "原册内行次")
    private Integer rowNo;

    /** 图幅代号 */
    @TableField("item_code")
    @ApiModelProperty(value = "图幅代号")
    private String itemCode;

    /** 成果类别 */
    @TableField("prod_type")
    @ApiModelProperty(value = "成果类别")
    private String prodType;

    /** 属地名址 */
    @TableField("region")
    @ApiModelProperty(value = "属地名址")
    private String region;

    /** 本行所报幅数;挂着的存原值(含负数),进账的存所报数 */
    @TableField("qty")
    @ApiModelProperty(value = "本行所报幅数")
    private BigDecimal qty;

    /** 行结局 POSTED已进账 MERGED并入前一条 HUNG挂着 */
    @TableField("outcome")
    @ApiModelProperty(value = "行结局")
    private String outcome;

    /** 挂账缘故(挂着的行才写,只写这一遍) */
    @TableField("reason")
    @ApiModelProperty(value = "挂账缘故")
    private String reason;

    /** 并入的那一条总账主键(已进账/已合并的行指过去) */
    @TableField("ledger_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "并入的总账主键")
    private Long ledgerId;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookletId() {
        return bookletId;
    }

    public void setBookletId(Long bookletId) {
        this.bookletId = bookletId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
