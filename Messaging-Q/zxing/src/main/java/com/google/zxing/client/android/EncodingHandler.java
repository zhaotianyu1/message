package com.google.zxing.client.android;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;

public final class EncodingHandler {
    public static Bitmap createQRCode(Context context, String content, int width, int backgroundColor, int pointColor) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        HashMap<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, width, hints);
        return bitMatrix2Bitmap(context, matrix, backgroundColor, pointColor, false);
    }

    // must in child thread
    public static Bitmap createQRCodeWithAvatar(Context context, String content, int width, int backgroundColor, int pointColor) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        HashMap<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, width, hints);
        return bitMatrix2Bitmap(context, matrix, backgroundColor, pointColor, true);
    }

    private static Bitmap bitMatrix2Bitmap(Context context, BitMatrix matrix, int backgroundColor, int pointColor, boolean needAddAvatar) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int[] rawData = new int[w * h];
        if (needAddAvatar) {
//            Bitmap avatar = getAvatarBitmap(context);
//            for (int x = 0; x < w; x++) {
//                for (int y = 0; y < h; y++) {
//                    if (x > w / 2 - avatar.getWidth() / 2
//                            && x < w / 2 + avatar.getWidth() / 2
//                            && y > h / 2 - avatar.getHeight() / 2
//                            && y < h / 2 + avatar.getHeight() / 2) {
//                        rawData[x + (y * w)] = avatar.getPixel(x - w / 2 + avatar.getHeight() / 2, y - h / 2 + avatar.getHeight() / 2);
//                    } else {
//                        int color = backgroundColor;
//                        if (matrix.get(x, y)) {
//                            color = pointColor;
//                        }
//                        rawData[x + (y * w)] = color;
//                    }
//                }
//            }
        } else {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int color = backgroundColor;
                    if (matrix.get(x, y)) {
                        color = pointColor;
                    }
                    rawData[x + (y * w)] = color;
                }
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(rawData, 0, w, 0, 0, w, h);
        return bitmap;
    }

    // instead of justalk logo if avatar is null
//    private static Bitmap getAvatarBitmap(Context context) {
//        JTProfileManager profileManager = JTProfileManager.getInstance();
//        String local = profileManager.getThumbnailLocal();
//        String url = profileManager.getThumbnailUrl();
//        int size = context.getResources().getDimensionPixelOffset(R.dimen.share_qrcode_avatar_size);
//
//        File localFile = null;
//        if (!TextUtils.isEmpty(local)) {
//            localFile = new File(local);
//            if (!(localFile.exists() && localFile.length() > 0)) {
//                localFile = null;
//            }
//        }
//
//        Bitmap bitmap = null;
//        try {
//            if (localFile != null) {
////                bitmap = Picasso.with(context)
////                        .load(localFile)
////                        .transform(new CropCircleTransformation())
////                        .resize(size, size)
////                        .get();
//
//            } else {
////                bitmap = Picasso.with(context)
////                        .load(ImageLoader.formatUrl(url))
////                        .transform(new CropCircleTransformation())
////                        .resize(size, size)
////                        .get();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        if (bitmap == null) {
//            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.share_qrcode_logo);
//        }
//        return bitmap;
//    }
}
