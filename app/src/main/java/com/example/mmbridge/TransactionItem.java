package com.example.mmbridge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionItem {
    public final String bankName;
    public final String amount;
    public final String note;
    public final String dateStr;
    public final long timestamp;

    public TransactionItem(String bankName, String amount, String note) {
        this.bankName = bankName;
        this.amount = amount;
        this.note = note;
        this.dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        this.timestamp = System.currentTimeMillis();
    }
}
