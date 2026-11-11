package com.fanok.audiobooks.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.fanok.audiobooks.AppThemes;
import com.fanok.audiobooks.PaletteBinder;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.view.ColorWheelView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public final class ColorPickerDialog {

    public interface OnColorPickedListener {
        void onColorPicked(int color);
    }

    private ColorPickerDialog() {
    }

    public static void show(@NonNull Context context, @NonNull String title, int initialColor,
            boolean isDarkLuminanceClamped, @NonNull OnColorPickedListener listener) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        View previewCurrent = view.findViewById(R.id.colorPreviewCurrent);
        View previewNew = view.findViewById(R.id.colorPreviewNew);
        ColorWheelView colorWheel = view.findViewById(R.id.colorWheel);
        SeekBar brightnessSlider = view.findViewById(R.id.brightnessSlider);
        TextInputEditText hexInput = view.findViewById(R.id.hexInput);

        float[] initialHsv = new float[3];
        AppThemes.rgbToHsv(initialColor, initialHsv);

        final float[] currentHsv = new float[] { initialHsv[0], initialHsv[1], initialHsv[2] };
        final int[] selectedColor = new int[] { initialColor };
        final boolean[] isInternalChange = new boolean[] { false };

        previewCurrent.setBackgroundColor(initialColor);
        previewNew.setBackgroundColor(initialColor);

        // Darkness clamp max brightness to 38% for background and surface slots
        int maxBrightness = isDarkLuminanceClamped ? 38 : 100;
        brightnessSlider.setMax(maxBrightness);

        int initialProgress = Math.round(currentHsv[2] * 100f);
        if (initialProgress > maxBrightness) {
            initialProgress = maxBrightness;
            currentHsv[2] = initialProgress / 100f;
            selectedColor[0] = AppThemes.hsvToRgb(currentHsv[0], currentHsv[1], currentHsv[2]);
            previewNew.setBackgroundColor(selectedColor[0]);
        }
        brightnessSlider.setProgress(initialProgress);

        colorWheel.setHueAndSaturation(currentHsv[0], currentHsv[1]);
        colorWheel.setValue(currentHsv[2]);

        hexInput.setText(AppThemes.formatHex(selectedColor[0]));

        // ColorWheel listener: updates Hue and Saturation
        colorWheel.setOnColorChangeListener((hue, sat, rgb) -> {
            currentHsv[0] = hue;
            currentHsv[1] = sat;
            int newColor = AppThemes.hsvToRgb(currentHsv[0], currentHsv[1], currentHsv[2]);
            selectedColor[0] = newColor;
            previewNew.setBackgroundColor(newColor);

            isInternalChange[0] = true;
            hexInput.setText(AppThemes.formatHex(newColor));
            isInternalChange[0] = false;
        });

        // Brightness slider listener: updates Value
        brightnessSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentHsv[2] = progress / 100f;
                    colorWheel.setValue(currentHsv[2]);
                    int newColor = AppThemes.hsvToRgb(currentHsv[0], currentHsv[1], currentHsv[2]);
                    selectedColor[0] = newColor;
                    previewNew.setBackgroundColor(newColor);

                    isInternalChange[0] = true;
                    hexInput.setText(AppThemes.formatHex(newColor));
                    isInternalChange[0] = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Direct HEX input listener
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isInternalChange[0] || s == null) return;
                String text = s.toString().trim();
                Integer parsed = AppThemes.parseHexColor(text);
                if (parsed != null) {
                    float[] hsv = new float[3];
                    AppThemes.rgbToHsv(parsed, hsv);
                    if (isDarkLuminanceClamped && hsv[2] > 0.38f) {
                        hsv[2] = 0.38f;
                    }
                    if (hsv[1] > 0.001f) {
                        currentHsv[0] = hsv[0];
                    }
                    currentHsv[1] = hsv[1];
                    currentHsv[2] = hsv[2];

                    int newColor = AppThemes.hsvToRgb(currentHsv[0], currentHsv[1], currentHsv[2]);
                    selectedColor[0] = newColor;
                    previewNew.setBackgroundColor(newColor);

                    colorWheel.setHueAndSaturation(currentHsv[0], currentHsv[1]);
                    colorWheel.setValue(currentHsv[2]);
                    brightnessSlider.setProgress(Math.round(currentHsv[2] * 100f));
                }
            }
        });

        AlertDialog dialog = new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(context)
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
