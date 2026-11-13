package com.fanok.audiobooks.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.R;
import java.net.URLEncoder;

/**
 * «Спросить AI»: hands a question about the book to the AI app the user chose in the settings. Nothing is sent by
 * the app itself and no key is needed: the question (the title and the author only) goes through the system's share,
 * straight to that app; without the app installed, its site opens with the question in the address. The pure parts
 * (the apps, the question, the site address) are tested; {@link #ask} is the thin Android part.
 */
public final class AskAi {

    public static final String PREF = "pref_ai_app";

    /** The apps on offer; the first is the default. {@code web} takes the question after "q=". */
    public enum App {
        CHATGPT("chatgpt", "com.openai.chatgpt", "https://chatgpt.com/?q="),
        CLAUDE("claude", "com.anthropic.claude", "https://claude.ai/new?q="),
        GROK("grok", "ai.x.grok", "https://grok.com/?q="),
        PERPLEXITY("perplexity", "ai.perplexity.app.android", "https://www.perplexity.ai/search?q=");

        public final String id;
        public final String pkg;
        final String web;

        App(String id, String pkg, String web) {
            this.id = id;
            this.pkg = pkg;
            this.web = web;
        }

        /** The app of {@code id}; anything else is the default. */
        @NonNull
        public static App parse(@Nullable String id) {
            for (App app : values()) {
                if (app.id.equals(id)) {
                    return app;
                }
            }
            return CHATGPT;
        }
    }

    private AskAi() {
    }

    /** The question about the book: its title and, when known, its author. Never the user's own data. */
    @NonNull
    public static String question(@NonNull String template, @Nullable String title, @Nullable String author) {
        String name = title == null ? "" : title.trim();
        String by = author == null ? "" : author.trim();
        return String.format(template, name, by.isEmpty() ? "—" : by);
    }

    /** The site address of {@code app} with the question in it. */
    @NonNull
    public static String webAddress(@NonNull App app, @NonNull String question) {
        try {
            return app.web + URLEncoder.encode(question, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return app.web;
        }
    }

    /** Opens the chosen app with the question; its site when the app is not installed. */
    public static void ask(@NonNull Activity activity, @NonNull SharedPreferences prefs, @Nullable String title,
            @Nullable String author) {
        App app = App.parse(prefs.getString(PREF, null));
        String question = question(activity.getString(R.string.ask_ai_question), title, author);
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, question)
                .setPackage(app.pkg);
        try {
            activity.startActivity(send);
            return;
        } catch (ActivityNotFoundException | SecurityException e) {
            // not installed (or does not take shared text): the site below
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress(app, question))));
        } catch (ActivityNotFoundException e) {
            android.widget.Toast.makeText(activity, R.string.ask_ai_failed, android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
