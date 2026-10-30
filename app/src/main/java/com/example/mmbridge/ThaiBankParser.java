package com.example.mmbridge;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThaiBankParser {

    public static final Map<String, String> SUPPORTED_PACKAGES = new HashMap<>();
    static {
        SUPPORTED_PACKAGES.put("ktbcs.netbank", "กรุงไทย (KTB)");
        SUPPORTED_PACKAGES.put("com.bangkokbank.mobilebanking", "กรุงเทพ (BBL)");
        SUPPORTED_PACKAGES.put("com.krungsri.kma", "กรุงศรี (KMA)");
        SUPPORTED_PACKAGES.put("com.gsb.mymo", "ออมสิน (MyMo)");
        SUPPORTED_PACKAGES.put("th.co.truemoney.wallet", "TrueMoney");
    }

    private static final String[] EXPENSE_KEYWORDS = new String[]{
        "เงินออก", "โอนเงิน", "ชำระเงิน", "สแกนจ่าย", "หักบัญชี",
        "ถอนเงิน", "จ่ายบิล", "สำเร็จ", "คุณได้ชำระ", "Paid", "Transfer", "Payment"
    };

    private static final String[] IGNORE_KEYWORDS = new String[]{
        "เงินเข้า", "รับเงิน", "OTP", "รหัสอ้างอิง", "เข้าสู่ระบบ"
    };

    private static final Pattern[] AMOUNT_PATTERNS = new Pattern[]{
        Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2}))\s*(?:บาท|บ\.|THB)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:จำนวน|ยอดเงิน|ชำระ|โอน|THB)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2}))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9]+(?:\.[0-9]{2}))\s*(?:บาท|THB)", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] MERCHANT_PATTERNS = new Pattern[]{
        Pattern.compile("(?:ไปยัง|ให้แก่|ร้านค้า|ที่|To)\s*([^\n\r0-9]{2,35})"),
        Pattern.compile("ชำระเงินให้\s*([^\n\r]{2,35})")
    };

    public static TransactionItem parseNotification(String packageName, String title, String text) {
        String bankName = SUPPORTED_PACKAGES.get(packageName);
        if (bankName == null) return null;

        String fullMessage = (title != null ? title : "") + " " + (text != null ? text : "");
        String lowerMsg = fullMessage.toLowerCase();

        for (String ig : IGNORE_KEYWORDS) {
            if (lowerMsg.contains(ig.toLowerCase())) {
                return null;
            }
        }

        boolean hasExpense = false;
        for (String kw : EXPENSE_KEYWORDS) {
            if (lowerMsg.contains(kw.toLowerCase())) {
                hasExpense = true;
                break;
            }
        }

        if (!hasExpense && !fullMessage.contains("บาท") && !lowerMsg.contains("thb")) {
            return null;
        }

        String extractedAmount = null;
        for (Pattern pat : AMOUNT_PATTERNS) {
            Matcher m = pat.matcher(fullMessage);
            if (m.find()) {
                extractedAmount = m.group(1).replace(",", "");
                break;
            }
        }

        if (extractedAmount == null) return null;

        String cleanText = text != null ? text.replace("
", " ") : "";
        if (cleanText.length() > 40) cleanText = cleanText.substring(0, 40);
        String note = bankName + ": " + cleanText;

        for (Pattern pat : MERCHANT_PATTERNS) {
            Matcher m = pat.matcher(fullMessage);
            if (m.find()) {
                note = m.group(1).trim();
                break;
            }
        }

        return new TransactionItem(bankName, extractedAmount, note);
    }
}
