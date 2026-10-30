package com.example.mmbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class BankNotificationService : NotificationListenerService() {

    private var slipObserver: SlipObserverHelper? = null
    private var lastTriggerTime = 0L
    private val channelId = "mm_safe_bridge_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        slipObserver = SlipObserverHelper(this) { item ->
            handleDetectedTransaction(item)
        }
        slipObserver?.startWatching()
    }

    override fun onDestroy() {
        slipObserver?.stopWatching()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        if (!ThaiBankParser.SUPPORTED_PACKAGES.containsKey(pkg)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val item = ThaiBankParser.parseNotification(pkg, title, text)
        if (item != null) {
            handleDetectedTransaction(item)
        }
    }

    private fun handleDetectedTransaction(item: TransactionItem) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 5000) return
        lastTriggerTime = now

        TransactionQueue.addTransaction(this, item)

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("amount", item.amount))
        } catch (_: Exception) {}

        showQuickActionNotification(item)
    }

    private fun showQuickActionNotification(item: TransactionItem) {
        val bridgeIntent = Intent(this, QuickBridgeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_AMOUNT", item.amount)
            putExtra("EXTRA_BANK", item.bankName)
            putExtra("EXTRA_NOTE", item.note)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            bridgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
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
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(888, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "แจ้งเตือนส่งยอดเข้าแอปหมูแดง",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "เด้งปุ่มด่วนเมื่อสแกนจ่ายหรือโอนเงินผ่านแอปธนาคาร"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
