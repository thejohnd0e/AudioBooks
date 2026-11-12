package com.fanok.audiobooks.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.PaletteBinder;
import com.fanok.audiobooks.model.ListeningStats;

/**
 * One bar per day, the last (today) in the accent colour and the others softer, on the scale of the longest day. No
 * numbers on it: the totals are beside it, and the screen reader gets a sentence from the screen.
 */
public class DayBarsView extends View {

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint today = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long[] values = new long[0];

    public DayBarsView(Context context) {
        this(context, null);
    }

    public DayBarsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int accent = PaletteBinder.accent(context);
        today.setColor(accent);
        bar.setColor(accent);
        bar.setAlpha(110);
        base.setColor(accent);
        base.setAlpha(60);
    }

    /** The days to draw, oldest first. */
    public void setValues(@NonNull long[] values) {
        this.values = values.clone();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = values.length;
        if (n == 0) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float gap = 2 * density;
        float width = (getWidth() - getPaddingLeft() - getPaddingRight() - gap * (n - 1)) / n;
        float baseline = getHeight() - getPaddingBottom() - 2 * density;
        float full = baseline - getPaddingTop();
        long scale = ListeningStats.scale(values);
        base.setStrokeWidth(2 * density);
        canvas.drawLine(getPaddingLeft(), baseline + density, getWidth() - getPaddingRight(), baseline + density, base);
        for (int i = 0; i < n; i++) {
            float left = getPaddingLeft() + i * (width + gap);
            float height = values[i] <= 0 ? 0 : Math.max(2 * density, full * values[i] / scale);
            rect.set(left, baseline - height, left + width, baseline);
            canvas.drawRoundRect(rect, width / 4, width / 4, i == n - 1 ? today : bar);
        }
    }
}
