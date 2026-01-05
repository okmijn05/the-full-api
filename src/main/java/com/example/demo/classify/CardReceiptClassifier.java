package com.example.demo.classify;

import com.example.demo.model.CardReceiptType;
import com.google.cloud.documentai.v1.Document;

public class CardReceiptClassifier {

    public static class Classified {
        public CardReceiptType type;
        public double confidence;

        public Classified(CardReceiptType type, double confidence) {
            this.type = type;
            this.confidence = confidence;
        }
    }

    public Classified classify(Document doc) {
        String text = (doc == null || doc.getText() == null) ? "" : doc.getText();
        String upper = text.toUpperCase();

        // 점수 기반(가벼운 룰 + confidence 산출)
        int coupangApp = 0;
        int coupangCard = 0;
        int convenience = 0;
        int mart = 0;
        int slip = 0;

        // 1) 쿠팡 앱 결제내역
        if (isCoupangApp(text)) coupangApp += 10;

        // 2) 쿠팡 카드영수증
        if (upper.contains("쿠팡") || upper.contains("COUPANG")) coupangCard += 3;
        if (text.contains("주문번호")) coupangCard += 2;
        if (text.contains("카드영수증") || text.contains("구매정보")) coupangCard += 3;

        // 3) 편의점(브랜드 신호)
        if (upper.contains("GS25")) convenience += 6;
        if (upper.contains("7-ELEVEN") || text.contains("세븐일레븐")) convenience += 6;
        if (containsCuStrong(upper)) convenience += 5;

        // 4) 마트형(과세/면세/공급가액/부가세 동시)
        if (text.contains("과세")) mart += 2;
        if (text.contains("면세")) mart += 2;
        if (text.contains("공급가액")) mart += 3;
        if (text.contains("부가세") || upper.contains("VAT")) mart += 3;
        if (text.contains("과세물품") || text.contains("면세물품") || text.contains("과세합계") || text.contains("면세합계")) mart += 2;

        // 5) 일반 카드전표(CARD_SLIP_GENERIC) 신호
        // - 승인번호/승인일시/일시불/할부/가맹점번호/마스킹 카드번호 등
        // - OCR에서 "승인번호"가 "승 인 번 호" 같은 형태로 깨질 수 있어서 regex로 판단
        if (looksLikeApprovalNo(text) || containsApprovalKeyword(text)) slip += 5; // 가장 강한 신호
        if (looksLikeApprovedDateTime(text)) slip += 2;

        if (text.contains("일시불") || text.contains("할부")) slip += 2;

        // 가맹점번호는 '가맹점NO'처럼도 많이 나옴
        if (text.contains("가맹점번호") || text.contains("가맹점NO") || text.contains("가맹점 NO") || text.contains("가맹점NO.")) slip += 2;

        // 단말기/TID/거래번호 등
        if (text.contains("단말기") || text.contains("TID") || text.contains("거래번호")) slip += 1;

        // 매입사/VAN
        if (text.contains("매입사") || upper.contains("VAN")) slip += 1;

        // NO-CVM
        if (upper.contains("NO-CVM") || upper.contains("NOCVM")) slip += 2;

        // 카드번호 마스킹 형태
        if (looksLikeMaskedCardNo(text)) slip += 2;

        // 최종 선택
        // 우선순위: 쿠팡앱 > 편의점 > 쿠팡카드 > 마트 > 카드전표
        if (coupangApp >= 10) return new Classified(CardReceiptType.COUPANG_APP, 0.95);

        int best = max(convenience, coupangCard, mart, slip);
        if (best <= 0) return new Classified(CardReceiptType.UNKNOWN, 0.10);

        if (best == convenience) return new Classified(CardReceiptType.CONVENIENCE, conf(best));
        if (best == coupangCard) return new Classified(CardReceiptType.COUPANG_CARD, conf(best));
        if (best == mart) return new Classified(CardReceiptType.MART_ITEMIZED, conf(best));
        if (best == slip) return new Classified(CardReceiptType.CARD_SLIP_GENERIC, conf(best));

        return new Classified(CardReceiptType.UNKNOWN, 0.20);
    }

    private boolean isCoupangApp(String text) {
        boolean hasCoupay = text.contains("쿠팡(쿠페이)");
        boolean hasMemo = text.contains("거래메모");
        boolean hasCardReceipt = text.contains("카드영수증") || text.contains("구매정보");
        return hasCoupay && hasMemo && !hasCardReceipt;
    }

    // "CU"는 오탐이 많아서 강한 패턴만 인정
    private boolean containsCuStrong(String upper) {
        return upper.contains("CU점") || upper.matches(".*\\bCU\\b.*(점|STORE).*");
    }

    private boolean containsApprovalKeyword(String text) {
        // '신용승인', '승인' 단독은 오탐 가능성이 있어서 '신용승인'만 강하게 인정
        // (승인번호는 looksLikeApprovalNo에서 처리)
        return text.contains("신용승인");
    }

    private boolean looksLikeMaskedCardNo(String text) {
        // 1234****5678 / 1234-**-****-5678 / 5585-0313-****-199* 등 허용
        // - 마지막 3~4자리에 숫자뿐 아니라 * / X 도 섞일 수 있음
        return text.matches("(?s).*\\b\\d{4}[\\s\\-]*([*Xx]{2,}|\\d{0,2})[\\s\\-*Xx0-9]{2,12}[\\d*Xx]{3,4}\\b.*");
    }

    private boolean looksLikeApprovalNo(String text) {
        // 승인번호 / 승인 번호 / 승 인 번 호 등 OCR 깨짐까지 허용
        return text.matches("(?s).*승\\s*인\\s*번\\s*호\\s*[:\\-]?\\s*\\d{6,12}.*");
    }

    private boolean looksLikeApprovedDateTime(String text) {
        // 2025/12/18 12:58:36 형태 (슬래시/점/하이픈 모두 허용)
        return text.matches("(?s).*\\b\\d{4}[./-]\\d{2}[./-]\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\b.*");
    }

    private int max(int... arr) {
        int m = Integer.MIN_VALUE;
        for (int v : arr) m = Math.max(m, v);
        return m;
    }

    private double conf(int score) {
        // 대충 0.35~0.95로 맵핑
        if (score >= 10) return 0.95;
        if (score >= 8) return 0.85;
        if (score >= 6) return 0.75;
        if (score >= 4) return 0.60;
        return 0.45;
    }
}
