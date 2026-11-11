package com.fanok.audiobooks.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.AppThemes;

/**
 * An interactive circular HSV color wheel View.
 * Displays 360-degree hues radially and white-to-vivid saturation outwards.
 */
public class ColorWheelView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(float hue, float saturation, int rgb);
    }

    private static final int[] RAINBOW_COLORS = new int[] {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private final Paint mWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSaturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mCenterX;
    private float mCenterY;
    private float mWheelRadius;
    private float mThumbRadius;

    private float mHue = 0f;          // 0..360
    private float mSaturation = 1f;   // 0..1
    private float mValue = 1f;        // 0..1

    private OnColorChangeListener mListener;

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mWheelPaint.setStyle(Paint.Style.FILL);

        mThumbStrokePaint.setStyle(Paint.Style.STROKE);
        mThumbStrokePaint.setColor(Color.WHITE);
        mThumbStrokePaint.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        mThumbStrokePaint.setShadowLayer(4f, 0, 2f, 0x99000000);

        mThumbFillPaint.setStyle(Paint.Style.FILL);
        mThumbRadius = 12f * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2f;
        mCenterY = h / 2f;
        float padding = Math.max(mThumbRadius + 4f, 16f * getResources().getDisplayMetrics().density);
        mWheelRadius = Math.max(10f, Math.min(mCenterX, mCenterY) - padding);

        SweepGradient sweepShader = new SweepGradient(mCenterX, mCenterY, RAINBOW_COLORS, null);
        mWheelPaint.setShader(sweepShader);

        RadialGradient radialShader = new RadialGradient(
                mCenterX, mCenterY, mWheelRadius,
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        );
        mSaturationPaint.setShader(radialShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWheelRadius <= 0) return;

        // Draw base hue wheel
        canvas.drawCircle(mCenterX, mCenterY, mWheelRadius, mWheelPaint);

        // Draw white saturation gradient over it
        canvas.drawCircle(mCenterX, mCenterY, mWheelRadius, mSaturationPaint);

        // Calculate thumb coordinates
        double angleRad = Math.toRadians(mHue);
        float distance = mSaturation * mWheelRadius;
        float thumbX = (float) (mCenterX + distance * Math.cos(angleRad));
        float thumbY = (float) (mCenterY + distance * Math.sin(angleRad));

        // Draw selector thumb
        int currentColor = AppThemes.hsvToRgb(mHue, mSaturation, mValue);
        mThumbFillPaint.setColor(currentColor);

        canvas.drawCircle(thumbX, thumbY, mThumbRadius, mThumbFillPaint);
        canvas.drawCircle(thumbX, thumbY, mThumbRadius, mThumbStrokePaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                updateFromTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                updateFromTouch(event.getX(), event.getY());
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateFromTouch(float x, float y) {
        float dx = x - mCenterX;
        float dy = y - mCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360.0;
        }
        mHue = (float) angle;
        mSaturation = Math.max(0f, Math.min(1f, distance / mWheelRadius));

        int rgb = AppThemes.hsvToRgb(mHue, mSaturation, mValue);
        if (mListener != null) {
            mListener.onColorChanged(mHue, mSaturation, rgb);
        }
        invalidate();
    }

    public void setOnColorChangeListener(@Nullable OnColorChangeListener listener) {
        mListener = listener;
    }

    public float getHue() {
        return mHue;
    }

    public float getSaturation() {
        return mSaturation;
    }

    public float getValue() {
        return mValue;
    }

    public int getColor() {
        return AppThemes.hsvToRgb(mHue, mSaturation, mValue);
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        AppThemes.rgbToHsv(color, hsv);
        mHue = hsv[0];
        mSaturation = hsv[1];
        mValue = hsv[2];
        invalidate();
    }

    public void setHueAndSaturation(float hue, float saturation) {
        mHue = ((hue % 360f) + 360f) % 360f;
        mSaturation = Math.max(0f, Math.min(1f, saturation));
        invalidate();
    }

    public void setValue(float value) {
        mValue = Math.max(0f, Math.min(1f, value));
        invalidate();
    }
}
