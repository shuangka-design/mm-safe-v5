package com.example.mmbridge;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionQueue {
    public static final List<TransactionItem> transactions = new ArrayList<>();
    public static final List<String> historyLogs = new ArrayList<>();

    public static void addTransaction(Context context, TransactionItem item) {
        transactions.add(0, item);
        addLog("พบรายจ่าย " + item.bankName + ": " + item.amount + " บ. (" + item.note + ")");
        saveToPrefs(context, item);
    }

    public static void addLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        historyLogs.add(0, "[" + time + "] " + msg);
        if (historyLogs.size() > 50) {
            historyLogs.remove(historyLogs.size() - 1);
        }
    }

    private static void saveToPrefs(Context context, TransactionItem item) {
        SharedPreferences prefs = context.getSharedPreferences("mm_bridge_history", Context.MODE_PRIVATE);
        String existing = prefs.getString("tsv_rows", "");
        if (existing == null) existing = "";
        String newRow = item.dateStr + "	" + item.bankName + "	อาหาร/ทั่วไป		" + item.note + "	" + item.amount + "	Expense	";
        String updated = existing.isEmpty() ? newRow : existing + "
" + newRow;
        prefs.edit().putString("tsv_rows", updated).apply();
    }

    public static String exportTsvContent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("mm_bridge_history", Context.MODE_PRIVATE);
        String rows = prefs.getString("tsv_rows", "");
        if (rows == null) rows = "";
        String header = "Date	Account	Category	Subcategory	Note	Amount	Income/Expense	Description";
        return rows.isEmpty() ? header : header + "
" + rows;
    }
}
