package com.example.mmbridge;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(48, 48, 48, 48);

        TextView titleView = new TextView(this);
        titleView.setText("MM AutoBridge (Safe Mode 100%)
ไร้ AccessibilityService ไม่กระทบแอปธนาคาร
(กรุงไทย, กรุงเทพ, กรุงศรี, ออมสิน, TrueMoney)");
        titleView.setTextSize(17f);
        titleView.setPadding(0, 0, 0, 32);
        rootLayout.addView(titleView);

        Button btnNotif = new Button(this);
        btnNotif.setText("1. เปิดสิทธิ์อ่านแจ้งเตือนธนาคาร (Notification)");
        btnNotif.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        rootLayout.addView(btnNotif);

        Button btnStorage = new Button(this);
        btnStorage.setText("2. อนุญาตให้อ่านรูปสลิป & แสดงปุ่มด่วน");
        btnStorage.setOnClickListener(v -> requestPermissionsIfNeeded());
        rootLayout.addView(btnStorage);

        Button btnTest = new Button(this);
        btnTest.setText("ทดสอบคัดลอกยอด 150.00 บ. & เปิดแอปหมูแดง");
        btnTest.setOnClickListener(v -> {
            TransactionItem testItem = new TransactionItem(
                "กรุงไทย (KTB)",
                "150.00",
                "ทดสอบสแกนจ่ายร้านค้า"
            );
            TransactionQueue.addTransaction(MainActivity.this, testItem);
            refreshLogs();

            Intent bridgeIntent = new Intent(MainActivity.this, QuickBridgeActivity.class);
            bridgeIntent.putExtra("EXTRA_AMOUNT", testItem.amount);
            bridgeIntent.putExtra("EXTRA_BANK", testItem.bankName);
            bridgeIntent.putExtra("EXTRA_NOTE", testItem.note);
            startActivity(bridgeIntent);
        });
        rootLayout.addView(btnTest);

        Button btnExport = new Button(this);
        btnExport.setText("สร้างไฟล์ .xls (สำหรับ Import เข้าแอปหมูแดงทีเดียว)");
        btnExport.setOnClickListener(v -> exportToDownloads());
        rootLayout.addView(btnExport);

        logTextView = new TextView(this);
        logTextView.setText("ประวัติการทำงาน:
- ยังไม่มีรายการ");
        logTextView.setTextSize(14f);
        logTextView.setPadding(0, 32, 0, 0);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(logTextView);
        rootLayout.addView(scrollView);

        setContentView(rootLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogs();
    }

    private void refreshLogs() {
        if (!TransactionQueue.historyLogs.isEmpty()) {
            StringBuilder sb = new StringBuilder("ประวัติรายการที่ดักจับได้ล่าสุด:
");
            for (String log : TransactionQueue.historyLogs) {
                sb.append(log).append("
");
            }
            logTextView.setText(sb.toString());
        }
    }

    private void exportToDownloads() {
        try {
            String content = TransactionQueue.exportTsvContent(this);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, "MoneyManager_AutoImport.xls");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(
                this,
                "บันทึกไฟล์ที่ Download/MoneyManager_AutoImport.xls แล้ว!",
                Toast.LENGTH_LONG
            ).show();
        } catch (Exception e) {
            Toast.makeText(this, "เกิดข้อผิดพลาดในการสร้างไฟล์: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissionsIfNeeded() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 101);
    }
}
