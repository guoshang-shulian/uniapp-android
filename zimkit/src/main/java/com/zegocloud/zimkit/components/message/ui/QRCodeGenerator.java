package com.zegocloud.zimkit.components.message.ui; // Adjust to match your project package

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class QRCodeGenerator {

    /**
     * Generates a clean custom QR Code Bitmap from text
     * @param text The data to encode (e.g., Group ID)
     * @param size Width and Height dimensions in pixels
     * @return Bitmap of the QR code, or null if encoding fails
     */
    public static Bitmap generate(String text, int size) {
        try {
            // Encode the string into a binary bit matrix pattern
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            // Define custom color palettes (Traditional Black & White)
            int colorBlack = Color.BLACK;
            int colorWhite = Color.WHITE;

            // Map the bit matrix array to Android color integers
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? colorBlack : colorWhite;
                }
            }

            // Construct the high-fidelity render canvas bitmap object
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
