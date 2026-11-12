package com.fanok.audiobooks.adapter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.fanok.audiobooks.net.Covers;
import com.fanok.audiobooks.policy.DefaultCover;
import com.fanok.audiobooks.policy.EmbeddedCover;
import com.fanok.audiobooks.policy.ParentalControl;
import com.fanok.audiobooks.pojo.BookPOJO;
import java.io.File;

/**
 * A book's cover in a view, by the same rules as the lists' cards (BooksListAddapter.bindCover): the parental
 * control's mask first, then the default cover of a site that has none (the one read from the audio when it is
 * known), then the cover file, content URI or the site's URL with its Referer. Unlike the list it does not ask for
 * a cover that is not read yet: it shows the default one.
 */
public final class CoverBinder {

    private CoverBinder() {
    }

    public static void bind(@NonNull ImageView view, @NonNull BookPOJO book) {
        String photo = book.getPhoto();
        Drawable mask = ParentalControl.coverMask(view.getContext(), book);
        if (mask != null) {
            Glide.with(view).load(mask).into(view);
        } else if (DefaultCover.replaces(photo)
                || (photo == null || photo.isEmpty()) && DefaultCover.replaces(book.getUrl())) {
            Drawable gradient = DefaultCover.gradient(view.getContext(), book.getUrl());
            Bitmap read = EmbeddedCover.memory(book.getUrl());
            File saved = read == null ? EmbeddedCover.saved(view.getContext(), book.getUrl()) : null;
            RequestManager glide = Glide.with(view);
            (read != null ? glide.load(read) : saved != null ? glide.load(saved) : glide.load(gradient))
                    .placeholder(gradient).into(view);
        } else if (photo != null && !photo.isEmpty()) {
            if (photo.startsWith("content://")) {
                Glide.with(view).load(Uri.parse(photo)).into(view);
            } else if (photo.startsWith("/") || photo.contains(":\\")) {
                Glide.with(view).load(new File(photo)).into(view);
            } else {
                Glide.with(view).load(Covers.glide(photo)).into(view);
            }
        } else {
            view.setImageDrawable(ContextCompat.getDrawable(view.getContext(), android.R.drawable.ic_menu_gallery));
        }
    }
}
