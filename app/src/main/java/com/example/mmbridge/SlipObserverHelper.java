package com.example.mmbridge;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlipObserverHelper {

    public interface SlipCallback {
        void onSlipDetected(TransactionItem item);
    }

    private final Context context;
    private final SlipCallback callback;
    private long lastProcessedId = -1L;
    private final TextRecognizer recognizer;
    private final ContentObserver contentObserver;

    public SlipObserverHelper(Context context, SlipCallback callback) {
        this.context = context;
        this.callback = callback;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.contentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                checkLatestImage();
            }
        };
    }

    public void startWatching() {
        try {
            context.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            );
            TransactionQueue.addLog("เริ่มระบบเฝ้าดูรูปสลิปอัตโนมัติ (Safe Mode)");
        } catch (Exception e) {
            TransactionQueue.addLog("ไม่สามารถเริ่มเฝ้าดูรูปสลิป: " + e.getMessage());
        }
    }

    public void stopWatching() {
        try {
            context.getContentResolver().unregisterContentObserver(contentObserver);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void checkLatestImage() {
        Cursor cursor = null;
        try {
            String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
            };
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH));
                if (name == null) name = "";
                if (path == null) path = "";

                if (id == lastProcessedId) return;

                Map<String, String> bankFolderMap = new HashMap<>();
                bankFolderMap.put("Krungthai", "กรุงไทย (KTB)");
                bankFolderMap.put("KTB", "กรุงไทย (KTB)");
                bankFolderMap.put("Bualuang", "กรุงเทพ (BBL)");
                bankFolderMap.put("BBL", "กรุงเทพ (BBL)");
                bankFolderMap.put("KMA", "กรุงศรี (KMA)");
                bankFolderMap.put("Krungsri", "กรุงศรี (KMA)");
                bankFolderMap.put("MyMo", "ออมสิน (MyMo)");
                bankFolderMap.put("GSB", "ออมสิน (MyMo)");
                bankFolderMap.put("TrueMoney", "TrueMoney");

                String matchedBank = null;
                String lowerPath = path.toLowerCase();
                String lowerName = name.toLowerCase();
                for (Map.Entry<String, String> entry : bankFolderMap.entrySet()) {
                    String k = entry.getKey().toLowerCase();
                    if (lowerPath.contains(k) || lowerName.contains(k)) {
                        matchedBank = entry.getValue();
                        break;
                    }
                }

                if (matchedBank != null) {
                    lastProcessedId = id;
                    Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    scanSlipAmount(imageUri, matchedBank);
                }
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void scanSlipAmount(Uri uri, final String bankName) {
        try {
            InputImage image = InputImage.fromFilePath(context, uri);
            recognizer.process(image).addOnSuccessListener(visionText -> {
                String rawText = visionText.getText();
                Pattern amountPat = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*\.[0-9]{2})");
                Matcher m = amountPat.matcher(rawText);
                while (m.find()) {
                    String candidate = m.group(1).replace(",", "");
                    try {
                        double val = Double.parseDouble(candidate);
                        if (val > 0.0) {
                            TransactionItem item = new TransactionItem(bankName, candidate, "สลิป " + bankName);
                            callback.onSlipDetected(item);
                            break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            });
        } catch (Exception e) {
            TransactionQueue.addLog("อ่านสลิปผิดพลาด: " + e.getMessage());
        }
    }
}
