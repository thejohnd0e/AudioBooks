package com.fanok.audiobooks.dialogs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import com.fanok.audiobooks.PaletteBinder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * A Material dialog builder that paints every dialog it creates with the active palette.
 *
 * <p>Theme attributes live in compiled resources, so a palette chosen at runtime cannot reach
 * them: a dialog keeps the palette's built-in surface (which Material lightens by the dialog's
 * elevation) and its built-in accent. This builder repaints both once the dialog's window exists.
 */
public class PaletteDialogBuilder extends MaterialAlertDialogBuilder {

    public PaletteDialogBuilder(@NonNull Context context) {
        super(context);
    }

    public PaletteDialogBuilder(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(d -> PaletteBinder.applyPaletteToDialog(dialog));
        return dialog;
    }
}
