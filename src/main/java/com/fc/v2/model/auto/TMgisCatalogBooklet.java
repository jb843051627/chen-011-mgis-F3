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
 * 年度图幅目录册核收册头对象 t_mgis_catalog_booklet
 *
 * <p>同一册号只认最早那一遍：后到的顶不掉先收的（uk_catalog_booklet_no 兜底并发）。
 * 三数由系统在核收推演那一处算定：挂着行数 + 进账行数 = 表上总行数；
 * 另有同册合并后的总账条数。头遍经办、头遍时刻落笔即定，重递交幂不改它。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_catalog_booklet")
@ApiModel(value = "TMgisCatalogBooklet", description = "年度图幅目录册核收册头")
public class TMgisCatalogBooklet implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 图幅目录册号 */
    @TableField("booklet_no")
    @ApiModelProperty(value = "图幅目录册号")
    private String bookletNo;

    /** 属地名址(整本一册一地,以封面为准) */
    @TableField("region")
    @ApiModelProperty(value = "属地名址")
    private String region;

    /** 表上总行数(含挂着的那几行) */
    @TableField("total_rows")
    @ApiModelProperty(value = "表上总行数")
    private Integer totalRows;

    /** 挂着几行 */
    @TableField("hanging_count")
    @ApiModelProperty(value = "挂着几行")
    private Integer hangingCount;

    /** 进账几行(好行;挂着加入账=总行数) */
    @TableField("accepted_count")
    @ApiModelProperty(value = "进账几行")
    private Integer acceptedCount;

    /** 进账总账条数(同册同代号并一条后) */
    @TableField("posted_count")
    @ApiModelProperty(value = "进账总账条数")
    private Integer postedCount;

    /** 进账总幅数(重号只加一次) */
    @TableField("accepted_qty")
    @ApiModelProperty(value = "进账总幅数")
    private BigDecimal acceptedQty;

    /** 头一遍递送经办 */
    @TableField("first_submit_by")
    @ApiModelProperty(value = "头一遍递送经办")
    private String firstSubmitBy;

    /** 头一遍核收时刻(以秒计) */
    @TableField("first_submit_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "头一遍核收时刻")
    private Date firstSubmitAt;

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

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getHangingCount() {
        return hangingCount;
    }

    public void setHangingCount(Integer hangingCount) {
        this.hangingCount = hangingCount;
    }

    public Integer getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(Integer acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public Integer getPostedCount() {
        return postedCount;
    }

    public void setPostedCount(Integer postedCount) {
        this.postedCount = postedCount;
    }

    public BigDecimal getAcceptedQty() {
        return acceptedQty;
    }

    public void setAcceptedQty(BigDecimal acceptedQty) {
        this.acceptedQty = acceptedQty;
    }

    public String getFirstSubmitBy() {
        return firstSubmitBy;
    }

    public void setFirstSubmitBy(String firstSubmitBy) {
        this.firstSubmitBy = firstSubmitBy;
    }

    public Date getFirstSubmitAt() {
        return firstSubmitAt;
    }

    public void setFirstSubmitAt(Date firstSubmitAt) {
        this.firstSubmitAt = firstSubmitAt;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
