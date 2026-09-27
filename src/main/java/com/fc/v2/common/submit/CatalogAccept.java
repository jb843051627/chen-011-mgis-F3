package com.fc.v2.common.submit;

import com.fc.v2.common.qual.QualProdType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 年度图幅目录册整批核收引擎：处里十月收册的规矩在系统里的唯一出处。
 *
 * <p>一册递进来，逐行解析、逐行给结局；收下的并进总账，没收下的挂着，
 * 挂着的把行次、代号、缘故三样齐着写。章程全收在这一个类里，
 * 服务层、控制器都从这里过，别处不许另写一套。</p>
 *
 * <ul>
 *   <li><b>一处错只挂那一行</b>：一行的缘故只坏它自己，坏一行绝不按住整册，
 *       其余行照常进账。</li>
 *   <li><b>同册同代号占两行，并成一条</b>：头一行 POSTED，后一行 MERGED，
 *       幅数只按一次相加——年报与清册两个口径才合得上。</li>
 *   <li><b>越线、白表整册不收</b>：行数越过上限整册顶回、一行不落；
 *       零行白表不许回「已核讫」。</li>
 * </ul>
 *
 * <p>本引擎只在内存里把整册推演成定局，不落一笔库；落账由服务层照着推演结果
 * 一笔笔写——接口怎么回、账就怎么记。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class CatalogAccept {

    private CatalogAccept() {
    }

    /** 单册行头上限：越线的整册不收，一行也不落 */
    public static final int MAX_ROWS = 500;

    /**
     * 递进来的一行（原表四样）。幅数收原样字符串——负数、不是数要照原样写进缘故，
     * 不许先过一道数字转换把原值丢了。
     */
    public static final class LineInput {
        private final String itemCode;
        private final String prodType;
        private final String region;
        private final String qtyText;

        public LineInput(String itemCode, String prodType, String region, String qtyText) {
            this.itemCode = itemCode;
            this.prodType = prodType;
            this.region = region;
            this.qtyText = qtyText;
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getProdType() {
            return prodType;
        }

        public String getRegion() {
            return region;
        }

        public String getQtyText() {
            return qtyText;
        }
    }

    /** 一行的推演结局 */
    public static final class LineResult {
        private final int rowNo;
        private final String itemCode;
        private final String prodType;
        private final String region;
        private final BigDecimal qty;
        private final CatalogLineOutcome outcome;
        private final String reason;

        LineResult(int rowNo, String itemCode, String prodType, String region, BigDecimal qty,
                   CatalogLineOutcome outcome, String reason) {
            this.rowNo = rowNo;
            this.itemCode = itemCode;
            this.prodType = prodType;
            this.region = region;
            this.qty = qty;
            this.outcome = outcome;
            this.reason = reason;
        }

        public int getRowNo() {
            return rowNo;
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getProdType() {
            return prodType;
        }

        public String getRegion() {
            return region;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public CatalogLineOutcome getOutcome() {
            return outcome;
        }

        public String getReason() {
            return reason;
        }

        public boolean isHung() {
            return outcome == CatalogLineOutcome.HUNG;
        }
    }

    /**
     * 整册推演结果：逐行结局 + 三数。三数只在这里算一处，册头与回执都读它：
     * {@code 挂着行数 + 进账行数 = 表上总行数}。
     */
    public static final class Evaluation {
        private final List<LineResult> lines;
        private final int totalRows;
        private final int hangingCount;
        private final int acceptedCount;
        private final int postedCount;
        private final BigDecimal acceptedQty;
        /** 本册并进总账的代号 → 头一行（POSTED），幅数在此只加一次 */
        private final Map<String, LineResult> posted;

        Evaluation(List<LineResult> lines, int totalRows, int hangingCount, int acceptedCount,
                   int postedCount, BigDecimal acceptedQty, Map<String, LineResult> posted) {
            this.lines = lines;
            this.totalRows = totalRows;
            this.hangingCount = hangingCount;
            this.acceptedCount = acceptedCount;
            this.postedCount = postedCount;
            this.acceptedQty = acceptedQty;
            this.posted = posted;
        }

        public List<LineResult> getLines() {
            return lines;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public int getHangingCount() {
            return hangingCount;
        }

        public int getAcceptedCount() {
            return acceptedCount;
        }

        public int getPostedCount() {
            return postedCount;
        }

        public BigDecimal getAcceptedQty() {
            return acceptedQty;
        }

        public Map<String, LineResult> getPosted() {
            return posted;
        }

        /** 挂着加入账，须当场对得上表上总行数 */
        public boolean countsTally() {
            return hangingCount + acceptedCount == totalRows;
        }
    }

    /**
     * 逐行解析一整册，推演成定局。
     *
     * @param bookletNo 册号（空白由调用方先顶，这里只守不守白表/越线）
     * @param region    册子封面属地，整本一册一地；行里属地与封面不符的行自己挂着
     * @param rows      原表各行（行次即其在表中的次序，从 1 连排）
     * @throws CatalogAcceptException 白表、越线——整册不收，一行也不落
     */
    public static Evaluation evaluate(String bookletNo, String region, List<LineInput> rows) {
        if (rows == null || rows.isEmpty()) {
            // 那种「已核讫」的回执本处见过：市里据此以为交完了，年底两头账各说各话
            throw new CatalogAcceptException("零行的白表不许回「已核讫」：一行都没有，整册不收");
        }
        if (rows.size() > MAX_ROWS) {
            throw new CatalogAcceptException("单册行数越过上限 " + MAX_ROWS + " 行（实交 " + rows.size()
                    + " 行）：越线的整册不收，一行也不落");
        }
        String cover = region == null ? "" : region.trim();

        List<LineResult> results = new ArrayList<LineResult>(rows.size());
        // 本册已收过的代号，保持头一回出现的次序
        Map<String, LineResult> posted = new LinkedHashMap<String, LineResult>();
        int hanging = 0;
        BigDecimal qtySum = BigDecimal.ZERO;

        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 1;
            LineInput in = rows.get(i) == null ? new LineInput(null, null, null, null) : rows.get(i);
            String code = in.getItemCode() == null ? "" : in.getItemCode().trim();
            String typeText = in.getProdType() == null ? "" : in.getProdType().trim();
            String rowRegion = in.getRegion() == null ? "" : in.getRegion().trim();

            String reason = judgeLine(cover, code, typeText, rowRegion, in.getQtyText());
            if (reason != null) {
                // 一处填错只挂那一行：整册照走，那一行自己挂着，缘故写全；
                // 幅数能解析出数的（负数、零）原值带着，浑到解析不出的才留空
                results.add(new LineResult(rowNo, code, typeText, rowRegion, parseQty(in.getQtyText()),
                        CatalogLineOutcome.HUNG, reason));
                hanging++;
                continue;
            }
            QualProdType type = QualProdType.require(typeText);
            BigDecimal qty = parseQty(in.getQtyText());

            LineResult first = posted.get(code);
            if (first == null) {
                // 该代号本册头一回：进账，幅数加这一次
                LineResult r = new LineResult(rowNo, code, type.name(), rowRegion, qty,
                        CatalogLineOutcome.POSTED, null);
                posted.put(code, r);
                results.add(r);
                qtySum = qtySum.add(qty);
            } else {
                // 同册同代号占两行：并成一条，幅数只加一次（早年两侧各挂一次，
                // 账上重复算一遍，全年幅数凭空多出一截）
                results.add(new LineResult(rowNo, code, type.name(), rowRegion, qty,
                        CatalogLineOutcome.MERGED, null));
            }
        }

        int accepted = results.size() - hanging;
        return new Evaluation(results, rows.size(), hanging, accepted, posted.size(), qtySum, posted);
    }

    /**
     * 逐行校验这一行够不够格进账。不够格返回缘故（写全），够格返回 null。
     * 四样（行次由系统按表中次序给）逐样核，幅数须为正数；一行可挂多个缺样时，
     * 按代号、类别、属地、幅数的次序写头一样——挂着只须一个说得出的缘故。
     */
    static String judgeLine(String cover, String code, String typeText, String rowRegion, String qtyText) {
        if (code.isEmpty()) {
            return "图幅代号未填报";
        }
        if (QualProdType.tryOf(typeText) == null) {
            return "成果类别不识：" + (typeText.isEmpty() ? "(空)" : typeText);
        }
        if (rowRegion.isEmpty()) {
            return "属地名址未填报";
        }
        if (!cover.isEmpty() && !rowRegion.equals(cover)) {
            return "属地名址与封面「" + cover + "」不符：" + rowRegion;
        }
        if (qtyText == null || qtyText.trim().isEmpty()) {
            return "幅数须为正数：(空)";
        }
        BigDecimal qty = parseQty(qtyText);
        if (qty == null) {
            return "幅数须为正数：" + qtyText.trim();
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            // 负数幅数那一行自己挂着，整册照走；零幅数一样没道理进账
            return "幅数须为正数：" + qty.stripTrailingZeros().toPlainString();
        }
        return null;
    }

    /** 幅数照原样解析：不是数返回 null（由调用方写缘故），不抛转换异常吞掉原值 */
    static BigDecimal parseQty(String qtyText) {
        if (qtyText == null) {
            return null;
        }
        String s = qtyText.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            BigDecimal q = new BigDecimal(s);
            // 不接受 1.2.3、空格夹号之类浑写：BigDecimal 宽松的尾部空白已 trim，再兜一道精度
            if (q.precision() > 18) {
                return null;
            }
            return q;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
