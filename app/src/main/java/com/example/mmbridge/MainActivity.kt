package com.example.mmbridge

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titleView = TextView(this).apply {
            text = "MM AutoBridge (Safe Mode 100%)
ไร้ AccessibilityService ไม่กระทบแอปธนาคาร
(กรุงไทย, กรุงเทพ, กรุงศรี, ออมสิน, TrueMoney)"
            textSize = 17f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleView)

        val btnNotif = Button(this).apply {
            text = "1. เปิดสิทธิ์อ่านแจ้งเตือนธนาคาร (Notification)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        rootLayout.addView(btnNotif)

        val btnStorage = Button(this).apply {
            text = "2. อนุญาตให้อ่านรูปสลิป & แสดงปุ่มด่วน"
            setOnClickListener {
                requestPermissionsIfNeeded()
            }
        }
        rootLayout.addView(btnStorage)

        val btnTest = Button(this).apply {
            text = "ทดสอบคัดลอกยอด 150.00 บ. & เปิดแอปหมูแดง"
            setOnClickListener {
                val testItem = TransactionItem(
                    bankName = "กรุงไทย (KTB)",
                    amount = "150.00",
                    note = "ทดสอบสแกนจ่ายร้านค้า"
                )
                TransactionQueue.addTransaction(this@MainActivity, testItem)
                refreshLogs()

                val bridgeIntent = Intent(this@MainActivity, QuickBridgeActivity::class.java).apply {
                    putExtra("EXTRA_AMOUNT", testItem.amount)
                    putExtra("EXTRA_BANK", testItem.bankName)
                    putExtra("EXTRA_NOTE", testItem.note)
                }
                startActivity(bridgeIntent)
            }
        }
        rootLayout.addView(btnTest)

        val btnExport = Button(this).apply {
            text = "สร้างไฟล์ .xls (สำหรับ Import เข้าแอปหมูแดงทีเดียว)"
            setOnClickListener {
                exportToDownloads()
            }
        }
        rootLayout.addView(btnExport)

        logTextView = TextView(this).apply {
            text = "ประวัติการทำงาน:
- ยังไม่มีรายการ"
            textSize = 14f
            setPadding(0, 32, 0, 0)
        }

        val scrollView = ScrollView(this).apply {
            addView(logTextView)
        }
        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
    }

    private fun refreshLogs() {
        if (TransactionQueue.historyLogs.isNotEmpty()) {
            logTextView.text = "ประวัติรายการที่ดักจับได้ล่าสุด:
" + TransactionQueue.historyLogs.joinToString("
")
        }
    }

    private fun exportToDownloads() {
        try {
            val content = TransactionQueue.exportTsvContent(this)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "MoneyManager_AutoImport.xls")
            file.writeText(content, Charsets.UTF_8)
            Toast.makeText(
                this,
                "บันทึกไฟล์ที่ Download/MoneyManager_AutoImport.xls แล้ว!",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "เกิดข้อผิดพลาดในการสร้างไฟล์: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
    }
}
