package com.example.mmbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

public class BankNotificationService extends NotificationListenerService {

    private SlipObserverHelper slipObserver;
    private long lastTriggerTime = 0L;
    private static final String CHANNEL_ID = "mm_safe_bridge_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        slipObserver = new SlipObserverHelper(this, this::handleDetectedTransaction);
        slipObserver.startWatching();
    }

    @Override
    public void onDestroy() {
        if (slipObserver != null) {
            slipObserver.stopWatching();
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        if (pkg == null || !ThaiBankParser.SUPPORTED_PACKAGES.containsKey(pkg)) return;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (textCs == null) {
            textCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }

        String title = titleCs != null ? titleCs.toString() : "";
        String text = textCs != null ? textCs.toString() : "";

        TransactionItem item = ThaiBankParser.parseNotification(pkg, title, text);
        if (item != null) {
            handleDetectedTransaction(item);
        }
    }

    private void handleDetectedTransaction(TransactionItem item) {
        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < 5000) return;
        lastTriggerTime = now;

        TransactionQueue.addTransaction(this, item);

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("amount", item.amount));
            }
        } catch (Exception ignored) {}

        showQuickActionNotification(item);
    }

    private void showQuickActionNotification(TransactionItem item) {
        Intent bridgeIntent = new Intent(this, QuickBridgeActivity.class);
        bridgeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        bridgeIntent.putExtra("EXTRA_AMOUNT", item.amount);
        bridgeIntent.putExtra("EXTRA_BANK", item.bankName);
        bridgeIntent.putExtra("EXTRA_NOTE", item.note);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            (int) (System.currentTimeMillis() % 10000),
            bridgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("จ่ายผ่าน " + item.bankName + ": " + item.amount + " บาท")
            .setContentText("แตะเพื่อคัดลอกยอด " + item.amount + " บ. และเปิดแอปหมูแดง (" + item.note + ")")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "คัดลอกยอด & เปิดแอปหมูแดง",
                pendingIntent
            )
            .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(888, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "แจ้งเตือนส่งยอดเข้าแอปหมูแดง",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("เด้งปุ่มด่วนเมื่อสแกนจ่ายหรือโอนเงินผ่านแอปธนาคาร");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
