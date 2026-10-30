package com.example.mmbridge

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionItem(
    val bankName: String,
    val amount: String,
    val note: String,
    val dateStr: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val timestamp: Long = System.currentTimeMillis()
)

object TransactionQueue {
    val transactions = mutableListOf<TransactionItem>()
    val historyLogs = mutableListOf<String>()

    fun addTransaction(context: Context, item: TransactionItem) {
        transactions.add(0, item)
        addLog("พบรายจ่าย " + item.bankName + ": " + item.amount + " บ. (" + item.note + ")")
        saveToPrefs(context, item)
    }

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        historyLogs.add(0, "[" + time + "] " + msg)
        if (historyLogs.size > 50) {
            historyLogs.removeAt(historyLogs.size - 1)
        }
    }

    private fun saveToPrefs(context: Context, item: TransactionItem) {
        val prefs = context.getSharedPreferences("mm_bridge_history", Context.MODE_PRIVATE)
        val existing = prefs.getString("tsv_rows", "") ?: ""
        val newRow = item.dateStr + "	" + item.bankName + "	อาหาร/ทั่วไป		" + item.note + "	" + item.amount + "	Expense	"
        val updated = if (existing.isEmpty()) newRow else existing + "
" + newRow
        prefs.edit().putString("tsv_rows", updated).apply()
    }

    fun exportTsvContent(context: Context): String {
        val prefs = context.getSharedPreferences("mm_bridge_history", Context.MODE_PRIVATE)
        val rows = prefs.getString("tsv_rows", "") ?: ""
        val header = "Date	Account	Category	Subcategory	Note	Amount	Income/Expense	Description"
        return if (rows.isEmpty()) header else header + "
" + rows
    }
}
