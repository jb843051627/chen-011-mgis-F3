package com.fc.v2.submit;

import com.fc.v2.common.submit.CatalogAccept;
import com.fc.v2.common.submit.CatalogAcceptException;
import com.fc.v2.common.submit.CatalogLineOutcome;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 整册核收引擎单测：处里五问逐一对号——
 * 逐行给结果、错一行只挂那行、同代号并一条幅数只加一次、白表越线整册不收、
 * 挂着加入账等于总行数。
 */
class CatalogAcceptTest {

    private static CatalogAccept.LineInput row(String code, String type, String region, String qty) {
        return new CatalogAccept.LineInput(code, type, region, qty);
    }

    private static List<CatalogAccept.LineInput> rows(CatalogAccept.LineInput... rs) {
        return new ArrayList<CatalogAccept.LineInput>(Arrays.asList(rs));
    }

    private CatalogAccept.LineResult byRow(CatalogAccept.Evaluation e, int rowNo) {
        return e.getLines().get(rowNo - 1);
    }

    @Test
    void 好册逐行进账_三数对得上() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("H49G001", "TOPO_MAP", "临川市", "10"),
                row("H49G002", "地形图", "临川市", "2.5")));
        assertEquals(2, e.getTotalRows());
        assertEquals(0, e.getHangingCount());
        assertEquals(2, e.getAcceptedCount());
        assertEquals(2, e.getPostedCount());
        assertEquals(0, new BigDecimal("12.5").compareTo(e.getAcceptedQty()));
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 1).getOutcome());
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 2).getOutcome());
        assertEquals(1, byRow(e, 1).getRowNo());
        assertEquals("H49G001", byRow(e, 1).getItemCode());
        assertTrue(e.countsTally());
    }

    @Test
    void 负数幅数那一行自己挂着_其余行照常进账() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("H49G001", "TOPO_MAP", "临川市", "10"),
                row("H49G002", "TOPO_MAP", "临川市", "-3"),
                row("H49G003", "TOPO_MAP", "临川市", "5")));
        assertEquals(3, e.getTotalRows());
        assertEquals(1, e.getHangingCount());
        assertEquals(2, e.getAcceptedCount());
        assertEquals(2, e.getPostedCount());
        assertEquals(0, new BigDecimal("15").compareTo(e.getAcceptedQty()));
        assertTrue(e.countsTally());
        CatalogAccept.LineResult bad = byRow(e, 2);
        assertEquals(CatalogLineOutcome.HUNG, bad.getOutcome());
        assertEquals("H49G002", bad.getItemCode());
        // 挂账缘故三样齐着：行次、代号、缘故，原值照带
        assertTrue(bad.getReason().contains("幅数须为正数"));
        assertTrue(bad.getReason().contains("-3"));
        // 负数能解析出数，原值照带在行上（只是不进总账）
        assertEquals(0, new BigDecimal("-3").compareTo(bad.getQty()));
        // 前后两行不受连累
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 1).getOutcome());
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 3).getOutcome());
    }

    @Test
    void 零幅数空幅数浑数一律挂() {
        for (String qty : new String[]{"0", "", "  ", "abc", "1.2.3"}) {
            CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市",
                    Collections.singletonList(row("H49G001", "TOPO_MAP", "临川市", qty)));
            assertEquals(1, e.getHangingCount(), "幅数「" + qty + "」应挂");
            assertEquals(0, e.getAcceptedCount());
            assertEquals(0, e.getAcceptedQty().compareTo(BigDecimal.ZERO));
            assertTrue(e.getLines().get(0).getReason().startsWith("幅数须为正数"));
        }
    }

    @Test
    void 四样缺样与类别不识属地不符各自挂着() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("  ", "TOPO_MAP", "临川市", "1"),
                row("H49G002", "外星图", "临川市", "1"),
                row("H49G003", "TOPO_MAP", "  ", "1"),
                row("H49G004", "TOPO_MAP", "临川市", "1"),
                row("H49G005", "TOPO_MAP", "青云市", "1")));
        assertEquals(5, e.getTotalRows());
        assertEquals(4, e.getHangingCount());
        assertEquals(1, e.getAcceptedCount());
        assertTrue(byRow(e, 1).getReason().contains("图幅代号未填报"));
        assertTrue(byRow(e, 2).getReason().contains("成果类别不识：外星图"));
        assertTrue(byRow(e, 3).getReason().contains("属地名址未填报"));
        assertTrue(byRow(e, 5).getReason().contains("与封面「临川市」不符"));
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 4).getOutcome());
        assertTrue(e.countsTally());
    }

    @Test
    void 同册同代号占两行并成一条_幅数只加一次() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("H49DUP", "TOPO_MAP", "临川市", "10"),
                row("H49DUP", "TOPO_MAP", "临川市", "99"),
                row("H49OK", "TOPO_MAP", "临川市", "7")));
        // 挂着 0、进账 3 行（每一原始行非挂即进），并成总账 2 条
        assertEquals(0, e.getHangingCount());
        assertEquals(3, e.getAcceptedCount());
        assertEquals(2, e.getPostedCount());
        // 全年幅数不多出一截：第二行的 99 不加
        assertEquals(0, new BigDecimal("17").compareTo(e.getAcceptedQty()));
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 1).getOutcome());
        assertEquals(CatalogLineOutcome.MERGED, byRow(e, 2).getOutcome());
        assertEquals(CatalogLineOutcome.POSTED, byRow(e, 3).getOutcome());
        // 进账的恰好两个代号
        assertEquals(2, e.getPosted().size());
        assertTrue(e.getPosted().containsKey("H49DUP"));
        assertTrue(e.countsTally());
    }

    @Test
    void 错行与重号挤在一册_挂着加入账仍对得上() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("H49A", "TOPO_MAP", "临川市", "10"),
                row("H49A", "TOPO_MAP", "临川市", "10"),
                row("H49B", "TOPO_MAP", "临川市", "-1"),
                row("H49C", "TOPO_MAP", "临川市", "3")));
        assertEquals(4, e.getTotalRows());
        assertEquals(1, e.getHangingCount());
        assertEquals(3, e.getAcceptedCount());
        assertEquals(2, e.getPostedCount());
        assertEquals(0, new BigDecimal("13").compareTo(e.getAcceptedQty()));
        assertTrue(e.countsTally());
    }

    @Test
    void 零行白表不许回已核讫() {
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class,
                () -> CatalogAccept.evaluate("C001", "临川市", Collections.<CatalogAccept.LineInput>emptyList()));
        assertTrue(e.getMessage().contains("白表"));
        assertThrows(CatalogAcceptException.class,
                () -> CatalogAccept.evaluate("C001", "临川市", null));
    }

    @Test
    void 行数越过上限整册不收() {
        List<CatalogAccept.LineInput> tooMany = new ArrayList<CatalogAccept.LineInput>();
        for (int i = 0; i < CatalogAccept.MAX_ROWS + 1; i++) {
            tooMany.add(row("H" + i, "TOPO_MAP", "临川市", "1"));
        }
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class,
                () -> CatalogAccept.evaluate("C001", "临川市", tooMany));
        assertTrue(e.getMessage().contains("上限"));
    }

    @Test
    void 恰好五百行收_五百零一行不收() {
        List<CatalogAccept.LineInput> atLimit = new ArrayList<CatalogAccept.LineInput>();
        for (int i = 0; i < CatalogAccept.MAX_ROWS; i++) {
            atLimit.add(row("H" + i, "TOPO_MAP", "临川市", "1"));
        }
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", atLimit);
        assertEquals(CatalogAccept.MAX_ROWS, e.getTotalRows());
        assertEquals(CatalogAccept.MAX_ROWS, e.getAcceptedCount());
        assertTrue(e.countsTally());
    }

    @Test
    void 行次由系统按表中次序给() {
        CatalogAccept.Evaluation e = CatalogAccept.evaluate("C001", "临川市", rows(
                row("H1", "TOPO_MAP", "临川市", "1"),
                row("H2", "TOPO_MAP", "临川市", "1"),
                row("H3", "TOPO_MAP", "临川市", "1")));
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, byRow(e, i).getRowNo());
        }
        assertFalse(e.getLines().isEmpty());
    }
}
