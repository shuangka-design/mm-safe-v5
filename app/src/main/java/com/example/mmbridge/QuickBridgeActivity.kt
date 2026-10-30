package com.example.mmbridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuickBridgeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val amount = intent.getStringExtra("EXTRA_AMOUNT") ?: ""
        val bankName = intent.getStringExtra("EXTRA_BANK") ?: ""

        if (amount.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MM_Amount", amount))

            Toast.makeText(
                this,
                "คัดลอกยอด " + amount + " บ. (" + bankName + ") แล้ว! กดวาง (Paste) ในแอปหมูแดงได้เลย",
                Toast.LENGTH_LONG
            ).show()
        }

        val launchIntent = packageManager.getLaunchIntentForPackage("com.realbyteapps.moneymanagerfree")
            ?: packageManager.getLaunchIntentForPackage("com.realbyteapps.moneymanager")

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "ไม่พบแอป Money Manager ในเครื่อง", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
