package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.submit.CatalogAcceptException;
import com.fc.v2.model.auto.TMgisCatalogBooklet;
import com.fc.v2.model.custom.CatalogSubmitRequest;
import com.fc.v2.service.ITMgisCatalogBookletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 年度图幅目录册整批核收 Controller。
 *
 * <p>此门专走「整批提交」：一本一册连封面属地一起递，逐行给结果——
 * 收下的进账、没收下的挂着，回执分两份明细；一行行拆解入账的活儿
 * 都压在服务层。单条添、单条改的门不辟；重递只认最早那一遍。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "年度图幅目录册核收")
@Controller
@RequestMapping("/mgisCatalog")
public class MgisCatalogController extends BaseController {

    @Autowired
    private ITMgisCatalogBookletService mgisCatalogBookletService;

    @Log(title = "图幅目录册整批核收", action = "submit")
    @ApiOperation(value = "整批核收", notes = "唯一写入门：逐行进账/挂账，同册同代号并一条、幅数只加一次；"
            + "重递交幂只认头一遍；越线、白表整册不收一行不落")
    @PostMapping("/submit")
    @RequiresPermissions("mgisCatalog:submit")
    @ResponseBody
    public AjaxResult submit(CatalogSubmitRequest req) {
        try {
            // 经办自登录态取，请求里说了不算
            return retobject(200, mgisCatalogBookletService.submit(req,
                    com.fc.v2.shiro.util.ShiroUtils.getLoginName()));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "凭册号翻账", notes = "册头三数 + 逐行明细：挂着加入账等于表上总行数；每行可追到总账")
    @GetMapping("/booklet/{bookletNo}")
    @RequiresPermissions("mgisCatalog:list")
    @ResponseBody
    public AjaxResult booklet(@PathVariable("bookletNo") String bookletNo) {
        try {
            return retobject(200, mgisCatalogBookletService.bookletView(bookletNo));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "凭属地翻账", notes = "该地市各册册头，挂着、进账、总行三数齐列")
    @GetMapping("/region/{region}")
    @RequiresPermissions("mgisCatalog:list")
    @ResponseBody
    public AjaxResult region(@PathVariable("region") String region) {
        try {
            return retobject(200, mgisCatalogBookletService.byRegion(region));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "图幅目录册头台账", action = "list")
    @ApiOperation(value = "册头台账", notes = "按册号、属地查")
    @GetMapping("/list")
    @RequiresPermissions("mgisCatalog:list")
    @ResponseBody
    public ResultTable list(TMgisCatalogBooklet record) {
        QueryWrapper<TMgisCatalogBooklet> queryWrapper = new QueryWrapper<TMgisCatalogBooklet>();
        if (record != null) {
            if (record.getBookletNo() != null && !record.getBookletNo().trim().isEmpty()) {
                queryWrapper.eq("booklet_no", record.getBookletNo().trim());
            }
            if (record.getRegion() != null && !record.getRegion().trim().isEmpty()) {
                queryWrapper.eq("region", record.getRegion().trim());
            }
        }
        queryWrapper.orderByDesc("first_submit_at");
        startPage();
        com.github.pagehelper.PageInfo<TMgisCatalogBooklet> page =
                new com.github.pagehelper.PageInfo<TMgisCatalogBooklet>(
                        mgisCatalogBookletService.selectBookletList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }
}
