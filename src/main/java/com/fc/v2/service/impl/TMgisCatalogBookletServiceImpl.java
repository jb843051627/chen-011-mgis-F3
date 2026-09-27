package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fc.v2.common.submit.CatalogAccept;
import com.fc.v2.common.submit.CatalogAcceptException;
import com.fc.v2.common.submit.CatalogLineOutcome;
import com.fc.v2.mapper.auto.TMgisCatalogBookletMapper;
import com.fc.v2.mapper.auto.TMgisCatalogLineMapper;
import com.fc.v2.mapper.auto.TMgisCatalogLedgerMapper;
import com.fc.v2.model.auto.TMgisCatalogBooklet;
import com.fc.v2.model.auto.TMgisCatalogLedger;
import com.fc.v2.model.auto.TMgisCatalogLine;
import com.fc.v2.model.custom.CatalogAcceptResult;
import com.fc.v2.model.custom.CatalogBookView;
import com.fc.v2.model.custom.CatalogSubmitRequest;
import com.fc.v2.service.ITMgisCatalogBookletService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 年度图幅目录册核收 Service业务层处理。
 *
 * <p>一行行拆解入账的活儿都压在这一层：控制器只收册子、只回回执，
 * 逐行校验、同册合并、总账追加、三数归置全从 {@link CatalogAccept} 推演结果
 * 过——先在内存把整册推演成定局，再一笔笔落，接口怎么回、账就怎么记。</p>
 *
 * <p>册头与册内行只立不改：本层对两张表只有 insert，没有 update/delete，
 * 不辟单条添、单条改的门。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMgisCatalogBookletServiceImpl implements ITMgisCatalogBookletService {

    @javax.annotation.Resource
    private TMgisCatalogBookletMapper mgisCatalogBookletMapper;

    @javax.annotation.Resource
    private TMgisCatalogLineMapper mgisCatalogLineMapper;

    @javax.annotation.Resource
    private TMgisCatalogLedgerMapper mgisCatalogLedgerMapper;

    /**
     * 整批核收。整册先推演后落账；重递只认最早那一遍。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatalogAcceptResult submit(CatalogSubmitRequest req, String operator) {
        if (req == null) {
            throw new IllegalArgumentException("核收请求为空");
        }
        if (req.getBookletNo() == null || req.getBookletNo().trim().isEmpty()) {
            throw new IllegalArgumentException("图幅目录册号未填报");
        }
        if (req.getRegion() == null || req.getRegion().trim().isEmpty()) {
            throw new IllegalArgumentException("属地名址未填报（整本一册一地，以封面为准）");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("递送经办未认");
        }
        String bookletNo = req.getBookletNo().trim();
        String region = req.getRegion().trim();

        // 同一册号送来第二遍：只认最早那一遍——后到的顶不掉先收的
        TMgisCatalogBooklet existing = findBooklet(bookletNo);
        if (existing != null) {
            return receipt(existing, listLines(existing.getId()), true);
        }

        List<CatalogAccept.LineInput> inputs = new ArrayList<CatalogAccept.LineInput>();
        if (req.getRows() != null) {
            for (CatalogSubmitRequest.Line line : req.getRows()) {
                inputs.add(line == null ? new CatalogAccept.LineInput(null, null, null, null)
                        : new CatalogAccept.LineInput(line.getItemCode(), line.getProdType(),
                                line.getRegion(), line.getQty()));
            }
        }
        // 白表、越线在此顶回：一笔都不落，也不给收讫回执
        CatalogAccept.Evaluation evaluation = CatalogAccept.evaluate(bookletNo, region, inputs);

        long bookletId = IdWorker.getId();
        Date now = nowSecond();
        TMgisCatalogBooklet booklet = new TMgisCatalogBooklet();
        booklet.setId(bookletId);
        booklet.setBookletNo(bookletNo);
        booklet.setRegion(region);
        // 三数出自同一处推演：挂着加入账等于表上总行数
        booklet.setTotalRows(evaluation.getTotalRows());
        booklet.setHangingCount(evaluation.getHangingCount());
        booklet.setAcceptedCount(evaluation.getAcceptedCount());
        booklet.setPostedCount(evaluation.getPostedCount());
        booklet.setAcceptedQty(evaluation.getAcceptedQty());
        booklet.setFirstSubmitBy(operator.trim());
        booklet.setFirstSubmitAt(now);
        try {
            this.mgisCatalogBookletMapper.insert(booklet);
        } catch (DuplicateKeyException e) {
            // 并发同册号：唯一键兜底，认头一遍。可重复读下用当前读拿先提交那册
            TMgisCatalogBooklet first = this.mgisCatalogBookletMapper.selectOne(
                    new QueryWrapper<TMgisCatalogBooklet>().eq("booklet_no", bookletNo).last("FOR UPDATE"));
            if (first != null) {
                return receipt(first, listLines(first.getId()), true);
            }
            throw new CatalogAcceptException("同册号正有一册在核，请核完再翻账");
        }

        List<TMgisCatalogLine> stored = new ArrayList<TMgisCatalogLine>(evaluation.getLines().size());
        // 本册代号 → 它并进的总账主键（POSTED 那行指过去，MERGED 的行指同一条）
        Map<String, Long> ledgerIdByCode = new HashMap<String, Long>();
        for (CatalogAccept.LineResult r : evaluation.getLines()) {
            TMgisCatalogLine line = new TMgisCatalogLine();
            line.setId(IdWorker.getId());
            line.setBookletId(bookletId);
            line.setRowNo(r.getRowNo());
            line.setItemCode(r.getItemCode());
            line.setProdType(r.getProdType());
            line.setRegion(r.getRegion());
            line.setOutcome(r.getOutcome().name());
            if (r.getOutcome() == CatalogLineOutcome.HUNG) {
                // 挂着：缘故三样齐着写，只写这一遍；不进总账。幅数能解析出数的原值存着
                line.setQty(r.getQty());
                line.setReason(r.getReason());
            } else {
                line.setQty(r.getQty());
                if (r.getOutcome() == CatalogLineOutcome.POSTED) {
                    // 该代号本册头一回：建/追加总账，幅数只在这一次相加
                    Long ledgerId = postLedger(booklet, r, now);
                    ledgerIdByCode.put(r.getItemCode(), ledgerId);
                    line.setLedgerId(ledgerId);
                } else {
                    // 同册同代号第二行：并成一条，幅数不加第二次
                    line.setLedgerId(ledgerIdByCode.get(r.getItemCode()));
                }
            }
            this.mgisCatalogLineMapper.insert(line);
            stored.add(line);
        }
        return receipt(booklet, stored, false);
    }

    @Override
    public CatalogBookView bookletView(String bookletNo) {
        if (bookletNo == null || bookletNo.trim().isEmpty()) {
            throw new IllegalArgumentException("图幅目录册号未填报");
        }
        TMgisCatalogBooklet booklet = findBooklet(bookletNo.trim());
        if (booklet == null) {
            throw new CatalogAcceptException("册号不在账上：" + bookletNo.trim());
        }
        List<TMgisCatalogLine> lines = listLines(booklet.getId());
        Set<Long> ledgerIds = new LinkedHashSet<Long>();
        for (TMgisCatalogLine line : lines) {
            if (line.getLedgerId() != null) {
                ledgerIds.add(line.getLedgerId());
            }
        }
        List<TMgisCatalogLedger> ledgers = ledgerIds.isEmpty()
                ? new ArrayList<TMgisCatalogLedger>()
                : this.mgisCatalogLedgerMapper.selectBatchIds(ledgerIds);
        return new CatalogBookView(booklet, lines, ledgers);
    }

    @Override
    public List<TMgisCatalogBooklet> byRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("属地名址未填报");
        }
        return this.mgisCatalogBookletMapper.selectList(new QueryWrapper<TMgisCatalogBooklet>()
                .eq("region", region.trim()).orderByDesc("first_submit_at"));
    }

    @Override
    public List<TMgisCatalogBooklet> selectBookletList(QueryWrapper<TMgisCatalogBooklet> queryWrapper) {
        return this.mgisCatalogBookletMapper.selectList(queryWrapper);
    }

    // ---------- 总账 ----------

    /**
     * 本册头一回收到该代号：总账有则追加（跨册各市各报，正常累加），
     * 无则建条。返回它并进的总账主键。同册去重在 {@link CatalogAccept} 已做，
     * 这里只可能与别的册并发撞同代号，唯一键与条件更新兜底。
     */
    private Long postLedger(TMgisCatalogBooklet booklet, CatalogAccept.LineResult r, Date now) {
        TMgisCatalogLedger ledger = this.mgisCatalogLedgerMapper.selectOne(
                new QueryWrapper<TMgisCatalogLedger>().eq("item_code", r.getItemCode()));
        if (ledger == null) {
            TMgisCatalogLedger created = new TMgisCatalogLedger();
            created.setId(IdWorker.getId());
            created.setBookletId(booklet.getId());
            created.setBookletNo(booklet.getBookletNo());
            created.setItemCode(r.getItemCode());
            created.setProdType(r.getProdType());
            created.setRegion(r.getRegion());
            created.setQty(r.getQty());
            created.setFirstAt(now);
            created.setLastBookletId(booklet.getId());
            created.setLastAt(now);
            try {
                this.mgisCatalogLedgerMapper.insert(created);
                return created.getId();
            } catch (DuplicateKeyException e) {
                // 另一市同刻先建了条：落到追加。可重复读下普通 select 仍读旧快照，
                // 用当前读 FOR UPDATE 拿先提交那一条
                ledger = this.mgisCatalogLedgerMapper.selectOne(
                        new QueryWrapper<TMgisCatalogLedger>().eq("item_code", r.getItemCode()).last("FOR UPDATE"));
                if (ledger == null) {
                    throw new CatalogAcceptException("同代号总账正有一册在落，请核完再翻账");
                }
            }
        }
        // 库面行锁兜底并发：qty = qty + n 原子自加，两市同刻各递一册同代号，
        // 一笔都不许丢；幅数来自引擎验过的 BigDecimal，拼句不带外字
        UpdateWrapper<TMgisCatalogLedger> uw = new UpdateWrapper<TMgisCatalogLedger>();
        uw.eq("id", ledger.getId())
                .set("last_booklet_id", booklet.getId())
                .set("last_at", now)
                .setSql("qty = qty + " + r.getQty().toPlainString());
        int rows = this.mgisCatalogLedgerMapper.update(null, uw);
        if (rows != 1) {
            // 条目不见了（本系统无删门，走到这必是出了怪事）：不闷声当成功
            throw new CatalogAcceptException("总账条目不见了：" + r.getItemCode());
        }
        return ledger.getId();
    }

    // ---------- 回执与翻账 ----------

    /** 回执：进账、挂着两份明细分开列，数字读的就是落库那批东西 */
    private CatalogAcceptResult receipt(TMgisCatalogBooklet booklet, List<TMgisCatalogLine> lines,
                                        boolean duplicated) {
        List<TMgisCatalogLine> accepted = new ArrayList<TMgisCatalogLine>();
        List<TMgisCatalogLine> hung = new ArrayList<TMgisCatalogLine>();
        for (TMgisCatalogLine line : lines) {
            if (CatalogLineOutcome.HUNG.name().equals(line.getOutcome())) {
                hung.add(line);
            } else {
                accepted.add(line);
            }
        }
        return new CatalogAcceptResult(booklet, accepted, hung, duplicated);
    }

    private TMgisCatalogBooklet findBooklet(String bookletNo) {
        return this.mgisCatalogBookletMapper.selectOne(
                new QueryWrapper<TMgisCatalogBooklet>().eq("booklet_no", bookletNo));
    }

    private List<TMgisCatalogLine> listLines(Long bookletId) {
        return this.mgisCatalogLineMapper.selectList(new QueryWrapper<TMgisCatalogLine>()
                .eq("booklet_id", bookletId).orderByAsc("row_no"));
    }

    /** 落笔时辰以秒计：库面 DATETIME 精确到秒，落账与回执同一口径 */
    private static Date nowSecond() {
        return new Date(System.currentTimeMillis() / 1000L * 1000L);
    }
}
