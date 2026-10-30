package com.example.mmbridge

object ThaiBankParser {

    val SUPPORTED_PACKAGES = mapOf(
        "ktbcs.netbank" to "กรุงไทย (KTB)",
        "com.bangkokbank.mobilebanking" to "กรุงเทพ (BBL)",
        "com.krungsri.kma" to "กรุงศรี (KMA)",
        "com.gsb.mymo" to "ออมสิน (MyMo)",
        "th.co.truemoney.wallet" to "TrueMoney"
    )

    private val EXPENSE_KEYWORDS = listOf(
        "เงินออก", "โอนเงิน", "ชำระเงิน", "สแกนจ่าย", "หักบัญชี",
        "ถอนเงิน", "จ่ายบิล", "สำเร็จ", "คุณได้ชำระ", "Paid", "Transfer", "Payment"
    )

    private val IGNORE_KEYWORDS = listOf(
        "เงินเข้า", "รับเงิน", "OTP", "รหัสอ้างอิง", "เข้าสู่ระบบ"
    )

    private val AMOUNT_REGEX_LIST = listOf(
        Regex("([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2}))\\s*(?:บาท|บ\\.|THB)", RegexOption.IGNORE_CASE),
        Regex("(?:จำนวน|ยอดเงิน|ชำระ|โอน|THB)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2}))", RegexOption.IGNORE_CASE),
        Regex("([0-9]+(?:\\.[0-9]{2}))\\s*(?:บาท|THB)", RegexOption.IGNORE_CASE)
    )

    private val MERCHANT_REGEX_LIST = listOf(
        Regex("(?:ไปยัง|ให้แก่|ร้านค้า|ที่|To)\\s*([^\\n\\r0-9]{2,35})"),
        Regex("ชำระเงินให้\\s*([^\\n\\r]{2,35})")
    )

    fun parseNotification(packageName: String, title: String, text: String): TransactionItem? {
        val bankName = SUPPORTED_PACKAGES[packageName] ?: return null
        val fullMessage = title + " " + text

        if (IGNORE_KEYWORDS.any { fullMessage.contains(it, ignoreCase = true) }) {
            return null
        }

        val hasExpenseKeyword = EXPENSE_KEYWORDS.any { fullMessage.contains(it, ignoreCase = true) }
        if (!hasExpenseKeyword && !fullMessage.contains("บาท") && !fullMessage.contains("THB", ignoreCase = true)) {
            return null
        }

        var extractedAmount: String? = null
        for (regex in AMOUNT_REGEX_LIST) {
            val match = regex.find(fullMessage)
            if (match != null) {
                extractedAmount = match.groupValues[1].replace(",", "")
                break
            }
        }

        if (extractedAmount == null) return null

        var note = bankName + ": " + text.take(40).replace("\n", " ")
        for (regex in MERCHANT_REGEX_LIST) {
            val match = regex.find(fullMessage)
            if (match != null) {
                note = match.groupValues[1].trim()
                break
            }
        }

        return TransactionItem(
            bankName = bankName,
            amount = extractedAmount,
            note = note
        )
    }
}
