package com.example.mmbridge;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class QuickBridgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String amount = getIntent().getStringExtra("EXTRA_AMOUNT");
        String bankName = getIntent().getStringExtra("EXTRA_BANK");
        if (amount == null) amount = "";
        if (bankName == null) bankName = "";

        if (!amount.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("MM_Amount", amount));
            }
            Toast.makeText(
                this,
                "คัดลอกยอด " + amount + " บ. (" + bankName + ") แล้ว! กดวาง (Paste) ในแอปหมูแดงได้เลย",
                Toast.LENGTH_LONG
            ).show();
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.realbyteapps.moneymanagerfree");
        if (launchIntent == null) {
            launchIntent = getPackageManager().getLaunchIntentForPackage("com.realbyteapps.moneymanager");
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "ไม่พบแอป Money Manager ในเครื่อง", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
