package com.google.zxing.client.android;/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import java.util.Collection;
import java.util.HashSet;

public final class ViewfinderView extends View {

    private static final long ANIMATION_DELAY = 100L;
    private static final int OPAQUE = 0xFF;

    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int frameWidth;
    private final int frameThickness;
    private Collection<ResultPoint> possibleResultPoints;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        Resources resources = context.getResources();
        maskColor = resources.getColor(R.color.zxing_viewfinder_mask);
        resultColor = resources.getColor(R.color.zxing_result_view);
        frameColor = resources.getColor(R.color.zxing_viewfinder_frame);
        frameWidth = resources.getDimensionPixelSize(R.dimen.scan_qr_frame_width);
        frameThickness = resources.getDimensionPixelOffset(R.dimen.scan_qr_frame_thickness);
        possibleResultPoints = new HashSet<ResultPoint>(5);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {
            paint.setColor(Color.WHITE);
            canvas.drawRect(frame.left, frame.top, frame.right, frame.top + 1, paint);
            canvas.drawRect(frame.left, frame.top, frame.left + 1, frame.bottom, paint);
            canvas.drawRect(frame.right - 1, frame.top, frame.right, frame.bottom, paint);
            canvas.drawRect(frame.left, frame.bottom - 1, frame.right, frame.bottom, paint);
            paint.setColor(frameColor);
            canvas.drawRect(frame.left, frame.top, frame.left + frameWidth, frame.top + frameThickness, paint);
            canvas.drawRect(frame.left, frame.top, frame.left + frameThickness, frame.top + frameWidth, paint);
            canvas.drawRect(frame.right - frameWidth, frame.top, frame.right, frame.top + frameThickness, paint);
            canvas.drawRect(frame.right - frameThickness, frame.top, frame.right, frame.top + frameWidth, paint);
            canvas.drawRect(frame.left, frame.bottom - frameWidth, frame.left + frameThickness, frame.bottom, paint);
            canvas.drawRect(frame.left, frame.bottom - frameThickness, frame.left + frameWidth, frame.bottom, paint);
            canvas.drawRect(frame.right - frameThickness, frame.bottom - frameWidth, frame.right, frame.bottom, paint);
            canvas.drawRect(frame.right - frameWidth, frame.bottom - frameThickness, frame.right, frame.bottom, paint);
            Collection<ResultPoint> currentPossible = possibleResultPoints;
            if (!currentPossible.isEmpty()) {
                postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
            }
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

}
