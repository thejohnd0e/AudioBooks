package com.fanok.audiobooks.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.fanok.audiobooks.AppThemes;
import com.fanok.audiobooks.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public final class ColorPickerDialog {

    public interface OnColorPickedListener {
        void onColorPicked(int color);
    }

    private static final int[] DARK_SWATCHES = new int[] {
            0xFF000000, 0xFF121212, 0xFF161B18, 0xFF1A1614, 0xFF14181D, 0xFF0E141F,
            0xFF19121A, 0xFF1F2620, 0xFF26211D, 0xFF1F242B, 0xFF1A222C, 0xFF261C14
    };

    private static final int[] ACCENT_SWATCHES = new int[] {
            0xFF7EAC91, 0xFFD4A373, 0xFF7293A6, 0xFFE07A5F, 0xFF2EC4B6, 0xFF3A86FF,
            0xFFF4A261, 0xFF8338EC, 0xFFE63946, 0xFFFFB703, 0xFFADB5BD, 0xFFFFFFFF
    };

    private ColorPickerDialog() {
    }

    public static void show(@NonNull Context context, @NonNull String title, int initialColor,
            boolean isDarkLuminanceClamped, @NonNull OnColorPickedListener listener) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        View previewCurrent = view.findViewById(R.id.colorPreviewCurrent);
        View previewNew = view.findViewById(R.id.colorPreviewNew);
        GridLayout swatchesGrid = view.findViewById(R.id.swatchesGrid);
        TextInputEditText hexInput = view.findViewById(R.id.hexInput);

        final int[] selectedColor = new int[] { initialColor };

        previewCurrent.setBackgroundColor(initialColor);
        previewNew.setBackgroundColor(initialColor);
        hexInput.setText(AppThemes.formatHex(initialColor));

        int[] swatches = isDarkLuminanceClamped ? DARK_SWATCHES : ACCENT_SWATCHES;
        int size = (int) (36 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (4 * context.getResources().getDisplayMetrics().density);

        for (int swatch : swatches) {
            View swatchView = new View(context);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            swatchView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(swatch);
            drawable.setStroke(2, 0x44FFFFFF);
            swatchView.setBackground(drawable);

            swatchView.setOnClickListener(v -> {
                selectedColor[0] = swatch;
                previewNew.setBackgroundColor(swatch);
                hexInput.setText(AppThemes.formatHex(swatch));
            });
            swatchesGrid.addView(swatchView);
        }

        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) return;
                String text = s.toString().trim();
                int parsed = AppThemes.parseHex(text, -1);
                if (parsed != -1) {
                    if (isDarkLuminanceClamped) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(parsed, hsv);
                        if (hsv[2] > 0.38f) {
                            hsv[2] = 0.38f;
                            parsed = Color.HSVToColor(hsv);
                        }
                    }
                    selectedColor[0] = parsed;
                    previewNew.setBackgroundColor(parsed);
                }
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    listener.onColorPicked(selectedColor[0]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.show();
    }
}
