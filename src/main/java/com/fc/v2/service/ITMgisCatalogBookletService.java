package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TMgisCatalogBooklet;
import com.fc.v2.model.custom.CatalogAcceptResult;
import com.fc.v2.model.custom.CatalogBookView;
import com.fc.v2.model.custom.CatalogSubmitRequest;

import java.util.List;

/**
 * 年度图幅目录册核收 Service接口。
 *
 * <p>此门专走整批提交：只有整册核收与翻账查询，没有单条添、单条改的门。
 * 重递只认最早那一遍；确实要改数走差异清单，核收记录里不追加自家话。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMgisCatalogBookletService {

    /**
     * 整批核收。先在内存把整册逐行推演成定局（口径唯一出处
     * {@link com.fc.v2.common.submit.CatalogAccept}），再照推演结果一笔笔落：
     * 册头、逐行明细、总账；接口怎么回、账就怎么记。
     *
     * <p>重递同册号只认最早那一遍：第二遍原样回头一遍的回执，
     * 不补一条账、不补第二句缘故；并发同册号唯一键兜底。</p>
     *
     * @param req      整册请求（册号、封面属地、逐行四样）
     * @param operator 递送经办（自登录态取，请求里说了不算）
     * @throws IllegalArgumentException   册号、属地空白
     * @throws com.fc.v2.common.submit.CatalogAcceptException 白表、越线——整册不收
     */
    CatalogAcceptResult submit(CatalogSubmitRequest req, String operator);

    /** 凭册号翻账：册头 + 全部逐行 + 本册触及的总账条目 */
    CatalogBookView bookletView(String bookletNo);

    /** 凭属地翻账：该地市各册册头，三数齐列 */
    List<TMgisCatalogBooklet> byRegion(String region);

    /** 册头台账（按册号、属地查） */
    List<TMgisCatalogBooklet> selectBookletList(QueryWrapper<TMgisCatalogBooklet> queryWrapper);
}
