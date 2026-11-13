package com.fanok.audiobooks.util;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import com.fanok.audiobooks.dialogs.PaletteDialogBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * A popup menu that a remote can see: on a TV the menu opens as a dialog's list (its row is drawn, and the keys reach
 * it); elsewhere it is the usual popup.
 */
public final class TvMenu {

    private TvMenu() {
    }

    public static void show(@NonNull Context context, @NonNull PopupMenu popup) {
        show(context, popup, null);
    }

    /** {@code onDismiss} runs when the dialog goes (the popup's own listener is not reachable from here). */
    public static void show(@NonNull Context context, @NonNull PopupMenu popup, @androidx.annotation.Nullable Runnable onDismiss) {
        if (!Tv.is(context)) {
            popup.show();
            return;
        }
        Menu menu = popup.getMenu();
        List<MenuItem> shown = new ArrayList<>();
        List<CharSequence> titles = new ArrayList<>();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible() && item.getTitle() != null && item.getTitle().length() > 0) {
                shown.add(item);
                titles.add(item.getTitle());
            }
        }
        new PaletteDialogBuilder(context)
                .setItems(titles.toArray(new CharSequence[0]),
                        (d, which) -> menu.performIdentifierAction(shown.get(which).getItemId(), 0))
                .setOnDismissListener(d -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .show();
    }
}
