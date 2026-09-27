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
 * 年度图幅目录进账总账对象 t_mgis_catalog_ledger
 *
 * <p>同一图幅代号全局一条（uk_catalog_ledger_code）：同册重号只加一次
 * （册内行打成 MERGED，不追加幅数）；不同册的同代号是各市各报，正常累加。
 * 头回收进与最近追加都留痕，年报与清册读的是同一份账。</p>
 *
 * @author fuce
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_catalog_ledger")
@ApiModel(value = "TMgisCatalogLedger", description = "年度图幅目录进账总账")
public class TMgisCatalogLedger implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 头一回收进该代号的册头主键 */
    @TableField("booklet_id")
    @ApiModelProperty(value = "头一回收进的册头主键")
    private Long bookletId;

    /** 头一回收进的册号(快照) */
    @TableField("booklet_no")
    @ApiModelProperty(value = "头一回收进的册号")
    private String bookletNo;

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

    /** 累计幅数(同册重号只加一次,跨册各报累加) */
    @TableField("qty")
    @ApiModelProperty(value = "累计幅数")
    private BigDecimal qty;

    /** 头一回进账时刻(以秒计) */
    @TableField("first_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "头一回进账时刻")
    private Date firstAt;

    /** 最近一次追加的册头主键 */
    @TableField("last_booklet_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "最近一次追加的册头主键")
    private Long lastBookletId;

    /** 最近一次追加时刻(以秒计) */
    @TableField("last_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "最近一次追加时刻")
    private Date lastAt;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

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

    public String getBookletNo() {
        return bookletNo;
    }

    public void setBookletNo(String bookletNo) {
        this.bookletNo = bookletNo;
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

    public Date getFirstAt() {
        return firstAt;
    }

    public void setFirstAt(Date firstAt) {
        this.firstAt = firstAt;
    }

    public Long getLastBookletId() {
        return lastBookletId;
    }

    public void setLastBookletId(Long lastBookletId) {
        this.lastBookletId = lastBookletId;
    }

    public Date getLastAt() {
        return lastAt;
    }

    public void setLastAt(Date lastAt) {
        this.lastAt = lastAt;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
