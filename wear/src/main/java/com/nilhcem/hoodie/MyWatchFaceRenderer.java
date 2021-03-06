package com.nilhcem.hoodie;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.nilhcem.hoodie.core.WatchMode;
import com.nilhcem.hoodie.core.WatchShape;

public class MyWatchFaceRenderer {

    private static final int INTERACTIVE_IDX = 0;
    private static final int AMBIENT_IDX = 1;
    private static final float MIN_SWEEP_ANGLE_DEG = 2;

    private final Resources res;
    private final Path indicatorsPath = new Path();
    private final Paint[] indicatorsPaints = new Paint[2];
    private final Paint[] timeArcPaints = new Paint[2];
    private final RectF arcBounds = new RectF();

    private Bitmap lionBitmap;
    private float lionMargin;

    private float centerX;
    private float centerY;
    private int modeIdx = INTERACTIVE_IDX;

    public MyWatchFaceRenderer(Context context) {
        res = context.getResources();
        initPaintObjects();
    }

    public void setSize(int width, int height, int chinSize, WatchShape shape) {
        float newCenterX = 0.5f * width;
        float newCenterY = 0.5f * (height + chinSize);

        if (Math.abs(centerX - newCenterX) > 0.5f || Math.abs(centerY - newCenterY) > 0.5f) {
            centerX = newCenterX;
            centerY = newCenterY;

            int margin = chinSize / 2;
            int diameter = Math.min(width, height + chinSize);
            initIndicatorsPath(diameter, diameter, margin, shape);
            initArcBounds(diameter, diameter, margin);
            initBitmaps(diameter, diameter, margin);
        }
    }

    public void setMode(WatchMode mode) {
        modeIdx = (mode == WatchMode.INTERACTIVE) ? INTERACTIVE_IDX : AMBIENT_IDX;
        boolean antiAlias = mode != WatchMode.LOW_BIT;

        indicatorsPaints[AMBIENT_IDX].setAntiAlias(antiAlias);
        timeArcPaints[AMBIENT_IDX].setAntiAlias(antiAlias);
    }

    public void drawTime(Canvas canvas, float hoursRotation, float minutesRotation) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // indicators
        canvas.drawPath(indicatorsPath, indicatorsPaints[modeIdx]);

        // lion
        if (modeIdx == INTERACTIVE_IDX) {
            canvas.drawBitmap(lionBitmap, lionMargin, lionMargin, null);
        }

        // time arc
        float startAngle = hoursRotation - 90;
        float sweepAngle = (360f - hoursRotation + minutesRotation) % 360f;

        // Always keep a slight (2°) gap for readability
        if (sweepAngle < MIN_SWEEP_ANGLE_DEG) {
            startAngle -= 0.5f * MIN_SWEEP_ANGLE_DEG;
            sweepAngle += 0.5f * MIN_SWEEP_ANGLE_DEG;
        } else if (sweepAngle > 360f - MIN_SWEEP_ANGLE_DEG) {
            startAngle += 0.5f * MIN_SWEEP_ANGLE_DEG;
            sweepAngle -= 0.5f * MIN_SWEEP_ANGLE_DEG;
        }

        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, timeArcPaints[modeIdx]);
    }

    private void initIndicatorsPath(int width, int height, int additionalMargin, WatchShape shape) {
        float innerRadiusX;
        float innerRadiusY;
        float outerRadiusX;
        float outerRadiusY;
        float indicatorsHeight = res.getDimension(R.dimen.indicators_height) + additionalMargin;

        if (shape == WatchShape.SQUARE) {
            // outer radius: square diagonal / 2
            outerRadiusX = (float) Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
            outerRadiusY = outerRadiusX;

            // inner radius: top of the visible screen - indicatorsHeight
            innerRadiusX = centerX - indicatorsHeight;
            innerRadiusY = centerY - indicatorsHeight;
        } else {
            outerRadiusX = 0.5f * width;
            outerRadiusY = 0.5f * height;

            innerRadiusX = outerRadiusX - indicatorsHeight;
            innerRadiusY = outerRadiusY - indicatorsHeight;
        }

        double angleRadians;
        indicatorsPath.reset();

        for (int i = 0; i < 12; i++) {
            angleRadians = Math.PI * 2 / 12 * i;

            indicatorsPath.moveTo(
                    (float) (centerX + innerRadiusX * Math.cos(angleRadians)),
                    (float) (centerY + innerRadiusY * Math.sin(angleRadians))
            );
            indicatorsPath.lineTo(
                    (float) (centerX + outerRadiusX * Math.cos(angleRadians)),
                    (float) (centerY + outerRadiusY * Math.sin(angleRadians))
            );
        }
    }

    private void initArcBounds(int width, int height, int additionalMargin) {
        float margin = res.getDimension(R.dimen.timearc_margin) + additionalMargin;

        arcBounds.left = margin;
        arcBounds.top = margin;
        arcBounds.right = width - margin;
        arcBounds.bottom = height - margin;
    }

    private void initBitmaps(int width, int height, int additionalMargin) {
        lionMargin = res.getDimension(R.dimen.centerpic_margin) + additionalMargin;
        int margin = Math.round(2 * lionMargin);

        Bitmap goldTexture = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.gold), width, height, true);
        Bitmap lionBitmapWhite = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.lion), width - margin, height - margin, true);

        // Apply the gold texture over the lion bitmap
        lionBitmap = Bitmap.createBitmap(lionBitmapWhite.getWidth(), lionBitmapWhite.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(lionBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(goldTexture,
                new Rect(Math.round(lionMargin), Math.round(lionMargin), Math.round(width - lionMargin), Math.round(height - lionMargin)),
                new Rect(0, 0, lionBitmapWhite.getWidth(), lionBitmapWhite.getHeight()),
                null);
        canvas.drawBitmap(lionBitmapWhite, 0, 0, paint);

        // Apply the gold texture on the interactive paint objects
        BitmapShader goldShader = new BitmapShader(goldTexture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        timeArcPaints[INTERACTIVE_IDX].setShader(goldShader);
        indicatorsPaints[INTERACTIVE_IDX].setShader(goldShader);
    }

    private void initPaintObjects() {
        timeArcPaints[INTERACTIVE_IDX] = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeArcPaints[INTERACTIVE_IDX].setStyle(Paint.Style.STROKE);
        timeArcPaints[INTERACTIVE_IDX].setColor(Color.WHITE);
        timeArcPaints[INTERACTIVE_IDX].setStrokeWidth(res.getDimension(R.dimen.timearc_stroke_width));

        timeArcPaints[AMBIENT_IDX] = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeArcPaints[AMBIENT_IDX].setStyle(Paint.Style.STROKE);
        timeArcPaints[AMBIENT_IDX].setColor(Color.WHITE);
        timeArcPaints[AMBIENT_IDX].setStrokeWidth(Math.min(res.getDimension(R.dimen.timearc_stroke_width_ambient), res.getDimension(R.dimen.max_pixels_in_ambient_mode)));

        indicatorsPaints[INTERACTIVE_IDX] = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorsPaints[INTERACTIVE_IDX].setStyle(Paint.Style.STROKE);
        indicatorsPaints[INTERACTIVE_IDX].setColor(Color.WHITE);
        indicatorsPaints[INTERACTIVE_IDX].setStrokeWidth(res.getDimension(R.dimen.indicators_width));

        indicatorsPaints[AMBIENT_IDX] = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorsPaints[AMBIENT_IDX].setStyle(Paint.Style.STROKE);
        indicatorsPaints[AMBIENT_IDX].setColor(Color.WHITE);
        indicatorsPaints[AMBIENT_IDX].setStrokeWidth(res.getDimension(R.dimen.indicators_width_ambient));
    }
}
