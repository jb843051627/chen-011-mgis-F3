package com.fc.v2.submit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.fc.v2.service.impl.TMgisCatalogBookletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 整批核收工序单测：逐行进账、错行单行挂账、同代号并一条、重递交幂、
 * 跨册累加、白表越线整册不落、凭册号凭属地翻账。
 */
class TMgisCatalogBookletServiceImplTest {

    private TMgisCatalogBookletMapper bookletMapper;
    private TMgisCatalogLineMapper lineMapper;
    private TMgisCatalogLedgerMapper ledgerMapper;
    private TMgisCatalogBookletServiceImpl service;

    private List<TMgisCatalogBooklet> booklets;
    private List<TMgisCatalogLine> lines;
    private List<TMgisCatalogLedger> ledgers;

    /** 置真：下一次总账建条撞唯一键（两市同刻并送） */
    private boolean ledgerInsertDuplicatesOnce;

    @BeforeEach
    void setUp() {
        bookletMapper = mock(TMgisCatalogBookletMapper.class);
        lineMapper = mock(TMgisCatalogLineMapper.class);
        ledgerMapper = mock(TMgisCatalogLedgerMapper.class);
        service = new TMgisCatalogBookletServiceImpl();
        ReflectionTestUtils.setField(service, "mgisCatalogBookletMapper", bookletMapper);
        ReflectionTestUtils.setField(service, "mgisCatalogLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "mgisCatalogLedgerMapper", ledgerMapper);

        booklets = new ArrayList<TMgisCatalogBooklet>();
        lines = new ArrayList<TMgisCatalogLine>();
        ledgers = new ArrayList<TMgisCatalogLedger>();

        when(bookletMapper.insert(any())).thenAnswer(inv -> {
            booklets.add(inv.getArgument(0));
            return 1;
        });
        when(bookletMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object val = qw.getParamNameValuePairs().values().iterator().next();
            String key = String.valueOf(val);
            for (TMgisCatalogBooklet b : booklets) {
                if (b.getBookletNo().equals(key)) {
                    return b;
                }
            }
            return null;
        });
        when(bookletMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object val = qw.getParamNameValuePairs().values().iterator().next();
            String key = String.valueOf(val);
            List<TMgisCatalogBooklet> hit = new ArrayList<TMgisCatalogBooklet>();
            for (TMgisCatalogBooklet b : booklets) {
                if (b.getRegion().equals(key) || b.getBookletNo().equals(key)) {
                    hit.add(b);
                }
            }
            return hit;
        });

        when(lineMapper.insert(any())).thenAnswer(inv -> {
            lines.add(inv.getArgument(0));
            return 1;
        });
        when(lineMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object val = qw.getParamNameValuePairs().values().iterator().next();
            long bookletId = ((Number) val).longValue();
            List<TMgisCatalogLine> hit = new ArrayList<TMgisCatalogLine>();
            for (TMgisCatalogLine l : lines) {
                if (l.getBookletId().equals(bookletId)) {
                    hit.add(l);
                }
            }
            hit.sort((a, b) -> a.getRowNo().compareTo(b.getRowNo()));
            return hit;
        });

        when(ledgerMapper.insert(any())).thenAnswer(inv -> {
            if (ledgerInsertDuplicatesOnce) {
                ledgerInsertDuplicatesOnce = false;
                throw new DuplicateKeyException("uk_catalog_ledger_code");
            }
            ledgers.add(inv.getArgument(0));
            return 1;
        });
        when(ledgerMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object val = qw.getParamNameValuePairs().values().iterator().next();
            String code = String.valueOf(val);
            for (TMgisCatalogLedger l : ledgers) {
                if (l.getItemCode().equals(code)) {
                    return l;
                }
            }
            return null;
        });
        when(ledgerMapper.selectBatchIds(any())).thenAnswer(inv -> {
            Iterable<?> ids = inv.getArgument(0);
            List<TMgisCatalogLedger> hit = new ArrayList<TMgisCatalogLedger>();
            for (Object o : ids) {
                long id = ((Number) o).longValue();
                for (TMgisCatalogLedger l : ledgers) {
                    if (l.getId().equals(id)) {
                        hit.add(l);
                    }
                }
            }
            return hit;
        });
        when(ledgerMapper.update(isNull(), any())).thenAnswer(inv -> {
            UpdateWrapper<?> uw = inv.getArgument(1);
            uw.getSqlSegment();
            Object idVal = uw.getParamNameValuePairs().values().iterator().next();
            long id = ((Number) idVal).longValue();
            Matcher m = Pattern.compile("qty\\s*=\\s*qty\\s*\\+\\s*(-?[0-9.]+)")
                    .matcher(uw.getSqlSet());
            assertTrue(m.find(), "总账追加须是 qty = qty + n 原子自加");
            BigDecimal add = new BigDecimal(m.group(1));
            for (TMgisCatalogLedger l : ledgers) {
                if (l.getId().equals(id)) {
                    l.setQty(l.getQty().add(add));
                    return 1;
                }
            }
            return 0;
        });
    }

    // ---------- 帮手 ----------

    private static CatalogSubmitRequest.Line line(String code, String type, String region, String qty) {
        CatalogSubmitRequest.Line l = new CatalogSubmitRequest.Line();
        l.setItemCode(code);
        l.setProdType(type);
        l.setRegion(region);
        l.setQty(qty);
        return l;
    }

    private static CatalogSubmitRequest req(String no, String region, CatalogSubmitRequest.Line... rows) {
        CatalogSubmitRequest r = new CatalogSubmitRequest();
        r.setBookletNo(no);
        r.setRegion(region);
        r.setRows(new ArrayList<CatalogSubmitRequest.Line>(Arrays.asList(rows)));
        return r;
    }

    private TMgisCatalogLine storedLine(long bookletId, int rowNo) {
        for (TMgisCatalogLine l : lines) {
            if (l.getBookletId().equals(bookletId) && l.getRowNo() == rowNo) {
                return l;
            }
        }
        return null;
    }

    private TMgisCatalogLedger ledgerOf(String code) {
        for (TMgisCatalogLedger l : ledgers) {
            if (l.getItemCode().equals(code)) {
                return l;
            }
        }
        return null;
    }

    // ---------- 逐行进账 ----------

    @Test
    void 好册逐行进账_回执三数与账同一处() {
        CatalogAcceptResult r = service.submit(
                req("C001", "临川市",
                        line("H49G001", "TOPO_MAP", "临川市", "10"),
                        line("H49G002", "地形图", "临川市", "5")),
                "张三");
        assertFalse(r.isDuplicated());
        TMgisCatalogBooklet b = r.getBooklet();
        assertEquals("C001", b.getBookletNo());
        assertEquals("临川市", b.getRegion());
        assertEquals(2, b.getTotalRows());
        assertEquals(0, b.getHangingCount());
        assertEquals(2, b.getAcceptedCount());
        assertEquals(2, b.getPostedCount());
        assertEquals(0, new BigDecimal("15").compareTo(b.getAcceptedQty()));
        assertEquals("张三", b.getFirstSubmitBy());
        assertNotNull(b.getFirstSubmitAt());

        assertEquals(2, r.getAcceptedLines().size());
        assertEquals(0, r.getHungLines().size());
        // 总账两条，行指得过去
        assertEquals(2, ledgers.size());
        assertNotNull(storedLine(b.getId(), 1).getLedgerId());
        assertEquals(CatalogLineOutcome.POSTED.name(), storedLine(b.getId(), 1).getOutcome());
        assertEquals(0, ledgerOf("H49G001").getQty().compareTo(new BigDecimal("10")));
    }

    @Test
    void 负数幅数那一行自己挂着_其余照常进账() {
        CatalogAcceptResult r = service.submit(
                req("C001", "临川市",
                        line("H49G001", "TOPO_MAP", "临川市", "10"),
                        line("H49G002", "TOPO_MAP", "临川市", "-3"),
                        line("H49G003", "TOPO_MAP", "临川市", "5")),
                "张三");
        TMgisCatalogBooklet b = r.getBooklet();
        // 挂着加入账等于表上总行数
        assertEquals(3, b.getTotalRows());
        assertEquals(1, b.getHangingCount());
        assertEquals(2, b.getAcceptedCount());
        assertEquals(2, b.getPostedCount());
        assertEquals(0, new BigDecimal("15").compareTo(b.getAcceptedQty()));

        assertEquals(2, r.getAcceptedLines().size());
        assertEquals(1, r.getHungLines().size());
        TMgisCatalogLine hung = r.getHungLines().get(0);
        assertEquals(2, hung.getRowNo());
        assertEquals("H49G002", hung.getItemCode());
        assertTrue(hung.getReason().contains("幅数须为正数"));
        assertTrue(hung.getReason().contains("-3"));
        // 挂着的不进总账、无总账可指；能解析出数的负幅数原值存着
        assertEquals(null, ledgerOf("H49G002"));
        TMgisCatalogLine hungStored = storedLine(b.getId(), 2);
        assertEquals(null, hungStored.getLedgerId());
        assertEquals(0, new BigDecimal("-3").compareTo(hungStored.getQty()));
    }

    @Test
    void 同册同代号两行并成一条_幅数只加一次() {
        CatalogAcceptResult r = service.submit(
                req("C001", "临川市",
                        line("H49DUP", "TOPO_MAP", "临川市", "10"),
                        line("H49DUP", "TOPO_MAP", "临川市", "99"),
                        line("H49OK", "TOPO_MAP", "临川市", "7")),
                "张三");
        TMgisCatalogBooklet b = r.getBooklet();
        assertEquals(3, b.getAcceptedCount());
        assertEquals(2, b.getPostedCount());
        assertEquals(0, new BigDecimal("17").compareTo(b.getAcceptedQty()));
        // 总账只有两条；重复代号一条，幅数是头一行的 10，第二行的 99 没加
        assertEquals(2, ledgers.size());
        assertEquals(0, ledgerOf("H49DUP").getQty().compareTo(new BigDecimal("10")));
        // 两行指同一条总账：POSTED 与 MERGED
        Long firstId = storedLine(b.getId(), 1).getLedgerId();
        Long secondId = storedLine(b.getId(), 2).getLedgerId();
        assertEquals(firstId, secondId);
        assertEquals(CatalogLineOutcome.POSTED.name(), storedLine(b.getId(), 1).getOutcome());
        assertEquals(CatalogLineOutcome.MERGED.name(), storedLine(b.getId(), 2).getOutcome());
    }

    // ---------- 重递交幂 ----------

    @Test
    void 同一册号送来第二遍只认最早那一遍() {
        CatalogAcceptResult first = service.submit(
                req("C001", "临川市", line("H49G001", "TOPO_MAP", "临川市", "10")),
                "张三");
        int linesAfterFirst = lines.size();
        int ledgerAfterFirst = ledgers.size();

        // 第二遍：换了经办、改了幅数、又添一行——一样顶不掉先收的
        CatalogAcceptResult second = service.submit(
                req("C001", "临川市",
                        line("H49G001", "TOPO_MAP", "临川市", "999"),
                        line("H49NEW", "TOPO_MAP", "临川市", "8")),
                "李四");
        assertTrue(second.isDuplicated());
        assertEquals(first.getBooklet().getId(), second.getBooklet().getId());
        assertEquals("张三", second.getBooklet().getFirstSubmitBy());
        assertEquals(1, second.getAcceptedLines().size());
        // 一笔都没再落，挂账缘故也不补第二句
        assertEquals(linesAfterFirst, lines.size());
        assertEquals(ledgerAfterFirst, ledgers.size());
        assertEquals(0, ledgerOf("H49G001").getQty().compareTo(new BigDecimal("10")));
        assertEquals(null, ledgerOf("H49NEW"));
        verify(bookletMapper, org.mockito.Mockito.times(1)).insert(any());
    }

    @Test
    void 两市同刻并送同册号_唯一键兜底仍只认头一遍() {
        // 先把先收那册摆进账，再让后到这册的册头 insert 撞唯一键
        TMgisCatalogBooklet winner = new TMgisCatalogBooklet();
        winner.setId(888L);
        winner.setBookletNo("C001");
        winner.setRegion("临川市");
        winner.setTotalRows(1);
        winner.setHangingCount(0);
        winner.setAcceptedCount(1);
        winner.setPostedCount(1);
        winner.setAcceptedQty(new BigDecimal("10"));
        winner.setFirstSubmitBy("先收的人");
        booklets.add(winner);
        when(bookletMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_catalog_booklet_no"));

        CatalogAcceptResult r = service.submit(
                req("C001", "临川市", line("H49X", "TOPO_MAP", "临川市", "1")), "后到的人");
        assertTrue(r.isDuplicated());
        assertEquals("先收的人", r.getBooklet().getFirstSubmitBy());
    }

    // ---------- 跨册累加 ----------

    @Test
    void 跨册各市各报同代号_总账正常累加() {
        service.submit(req("C001", "临川市", line("H49A", "TOPO_MAP", "临川市", "10")), "张三");
        service.submit(req("C002", "青云市", line("H49A", "IMAGE_MAP", "青云市", "7")), "李四");
        // 同代号全局一条：跨册是各市各报，幅数累加
        assertEquals(1, ledgers.size());
        TMgisCatalogLedger l = ledgerOf("H49A");
        assertEquals(0, l.getQty().compareTo(new BigDecimal("17")));
        // 头回收进的快照不动，最近追加留痕
        assertEquals("C001", l.getBookletNo());
        assertEquals("临川市", l.getRegion());
        assertNotNull(l.getLastAt());
    }

    @Test
    void 跨册并发建同代号条_撞唯一键后落到追加() {
        ledgerInsertDuplicatesOnce = true;
        // 账上先摆一条别的市刚建好的
        TMgisCatalogLedger existing = new TMgisCatalogLedger();
        existing.setId(777L);
        existing.setBookletId(700L);
        existing.setBookletNo("C000");
        existing.setItemCode("H49A");
        existing.setProdType("TOPO_MAP");
        existing.setRegion("青云市");
        existing.setQty(new BigDecimal("5"));
        ledgers.add(existing);

        CatalogAcceptResult r = service.submit(
                req("C001", "临川市", line("H49A", "TOPO_MAP", "临川市", "10")), "张三");
        assertFalse(r.isDuplicated());
        assertEquals(1, ledgers.size());
        assertEquals(0, ledgers.get(0).getQty().compareTo(new BigDecimal("15")));
    }

    // ---------- 白表越线 ----------

    @Test
    void 零行白表整册不落() {
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class,
                () -> service.submit(req("C001", "临川市"), "张三"));
        assertTrue(e.getMessage().contains("白表"));
        assertEquals(0, booklets.size());
        assertEquals(0, lines.size());
        verify(bookletMapper, never()).insert(any());
    }

    @Test
    void 行数越过上限整册不落() {
        CatalogSubmitRequest.Line[] many = new CatalogSubmitRequest.Line[501];
        for (int i = 0; i < many.length; i++) {
            many[i] = line("H" + i, "TOPO_MAP", "临川市", "1");
        }
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class,
                () -> service.submit(req("C001", "临川市", many), "张三"));
        assertTrue(e.getMessage().contains("上限"));
        assertEquals(0, booklets.size());
        assertEquals(0, ledgers.size());
    }

    @Test
    void 册号属地经办缺样当场顶回_不落账() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(req("  ", "临川市", line("H1", "TOPO_MAP", "临川市", "1")), "张三"));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(req("C001", " ", line("H1", "TOPO_MAP", "临川市", "1")), "张三"));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(req("C001", "临川市", line("H1", "TOPO_MAP", "临川市", "1")), " "));
        assertEquals(0, booklets.size());
    }

    // ---------- 翻账 ----------

    @Test
    void 凭册号凭属地都翻得动() {
        service.submit(req("C001", "临川市",
                line("H49A", "TOPO_MAP", "临川市", "10"),
                line("H49B", "TOPO_MAP", "临川市", "-1")), "张三");
        service.submit(req("C002", "临川市",
                line("H49C", "TOPO_MAP", "临川市", "3")), "李四");

        CatalogBookView view = service.bookletView("C001");
        TMgisCatalogBooklet b = view.getBooklet();
        assertEquals(2, view.getLines().size());
        assertEquals(1, view.getLedgers().size());
        // 三数当场对得上：挂着加入账等于表上总行数
        assertEquals(b.getHangingCount() + b.getAcceptedCount(), b.getTotalRows());

        List<TMgisCatalogBooklet> byRegion = service.byRegion("临川市");
        assertEquals(2, byRegion.size());
        assertThrows(CatalogAcceptException.class, () -> service.bookletView("C404"));
    }
}
