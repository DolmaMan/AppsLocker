package com.example.appslocker.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    public static Bitmap generateQRCode(String data, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            return toBitmap(bitMatrix);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap toBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static String createLinkData(String parentDeviceId) {
        try {
            JSONObject json = new JSONObject();
            json.put("parentDeviceId", parentDeviceId);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}