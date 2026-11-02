package com.nbhamley.glasshome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Pieces the home screen and the screensaver share. */
final class Parts {
    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    private Parts() { }

    static Bitmap wallpaper(Context c, int mood) {
        try {
            InputStream in = c.getAssets().open("wallpapers/" + Sun.MOOD_NAMES[mood] + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(in);
            in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    /** "9:41 PM" with a small PM. */
    static CharSequence time(Calendar c) {
        int h = c.get(Calendar.HOUR_OF_DAY);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        String t = String.format(Locale.US, "%d:%02d", h12, c.get(Calendar.MINUTE));
        String full = t + " " + (h < 12 ? "AM" : "PM");
        SpannableString s = new SpannableString(full);
        s.setSpan(new RelativeSizeSpan(0.27f), t.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    static String date(Calendar c) {
        return DAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " "
                + MONTHS[c.get(Calendar.MONTH)];
    }

    static TextView thin(Context c, float size, int color) {
        TextView t = Glass.text(c, "", size, color, false);
        t.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        t.setLetterSpacing(-0.03f);
        return t;
    }

    static int tint(String name) {
        String n = name.toLowerCase(Locale.US);
        if (n.contains("netflix")) return 0xFFA30F1C;
        if (n.contains("prime")) return 0xFF0A87C4;
        if (n.contains("hotstar")) return 0xFF2A62E8;
        if (n.contains("apple")) return 0xFF3A3A3F;
        if (n.contains("youtube")) return 0xFFC21F1F;
        return 0xFF5B6B9A;
    }

    /** The Renewals glass card, 640 design-px wide. */
    static View renewals(Context c, Calendar now, int max) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Glass.panel(34));
        int padX = Glass.px(32);
        box.setPadding(padX, Glass.px(24), padX, Glass.px(18));
        TextView head = Glass.text(c, "RENEWALS", 16, 0xA0FFFFFF, true);
        head.setLetterSpacing(0.14f);
        box.addView(head);

        List<RenewalList.Item> items = RenewalList.parse(Cfg.renewals(c), now, max);
        if (items.isEmpty()) {
            TextView hint = Glass.text(c, "Add your renewal dates: scripts/set_renewals.sh", 22, 0xB0FFFFFF, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = Glass.px(16);
            lp.bottomMargin = Glass.px(10);
            box.addView(hint, lp);
        }
        for (RenewalList.Item it : items) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView letter = Glass.text(c, it.name.substring(0, 1).toUpperCase(Locale.US), 19, 0xFFFFFFFF, true);
            letter.setGravity(Gravity.CENTER);
            letter.setBackground(Glass.gradient(tint(it.name), 0xFF10142B, 11));
            row.addView(letter, new LinearLayout.LayoutParams(Glass.px(36), Glass.px(36)));

            TextView name = Glass.text(c, it.name, 24, 0xFFFFFFFF, true);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, -2, 1f);
            nlp.leftMargin = Glass.px(16);
            row.addView(name, nlp);

            TextView when = Glass.text(c, it.when, 24, 0xFFFFFFFF, true);
            row.addView(when);

            TextView chip = Glass.text(c, RenewalList.daysText(it.daysLeft), 18, 0xD0FFFFFF, false);
            chip.setBackground(Glass.tileBg(999));
            chip.setPadding(Glass.px(12), Glass.px(4), Glass.px(12), Glass.px(4));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.leftMargin = Glass.px(14);
            row.addView(chip, clp);

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, Glass.px(56));
            box.addView(row, rlp);
        }
        return box;
    }

    static void px(TextView t, float size) { t.setTextSize(TypedValue.COMPLEX_UNIT_PX, Glass.px(size)); }
}
