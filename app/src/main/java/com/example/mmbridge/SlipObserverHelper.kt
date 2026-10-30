package com.example.mmbridge

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class SlipObserverHelper(
    private val context: Context,
    private val onSlipDetected: (TransactionItem) -> Unit
) {
    private var lastProcessedId: Long = -1L
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            checkLatestImage()
        }
    }

    fun startWatching() {
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
            TransactionQueue.addLog("เริ่มระบบเฝ้าดูรูปสลิปอัตโนมัติ (Safe Mode)")
        } catch (e: Exception) {
            TransactionQueue.addLog("ไม่สามารถเริ่มเฝ้าดูรูปสลิป: " + e.message)
        }
    }

    fun stopWatching() {
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
        } catch (_: Exception) {}
    }

    private fun checkLatestImage() {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
            )
            val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: ""
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)) ?: ""

                    if (id == lastProcessedId) return

                    val bankFolderMap = mapOf(
                        "Krungthai" to "กรุงไทย (KTB)",
                        "KTB" to "กรุงไทย (KTB)",
                        "Bualuang" to "กรุงเทพ (BBL)",
                        "BBL" to "กรุงเทพ (BBL)",
                        "KMA" to "กรุงศรี (KMA)",
                        "Krungsri" to "กรุงศรี (KMA)",
                        "MyMo" to "ออมสิน (MyMo)",
                        "GSB" to "ออมสิน (MyMo)",
                        "TrueMoney" to "TrueMoney"
                    )

                    val matchedBank = bankFolderMap.entries.firstOrNull {
                        path.contains(it.key, ignoreCase = true) || name.contains(it.key, ignoreCase = true)
                    }?.value

                    if (matchedBank != null) {
                        lastProcessedId = id
                        val imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        scanSlipAmount(imageUri, matchedBank)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanSlipAmount(uri: Uri, bankName: String) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val amountRegex = Regex("([0-9]{1,3}(?:,[0-9]{3})*\\.[0-9]{2})")
                    val matches = amountRegex.findAll(rawText).map { it.groupValues[1].replace(",", "") }.toList()
                    val validAmount = matches.firstOrNull { (it.toDoubleOrNull() ?: 0.0) > 0.0 }

                    if (validAmount != null) {
                        val item = TransactionItem(
                            bankName = bankName,
                            amount = validAmount,
                            note = "สลิป " + bankName
                        )
                        onSlipDetected(item)
                    }
                }
        } catch (e: Exception) {
            TransactionQueue.addLog("อ่านสลิปผิดพลาด: " + e.message)
        }
    }
}
