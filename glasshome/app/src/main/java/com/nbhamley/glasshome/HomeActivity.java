package com.nbhamley.glasshome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Home: date and time top left, Favorites top centre, Swipe (like or dislike) top right, apps in sections along
 * the bottom. Plain framework views, no network, no background service.
 */
public class HomeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root, favPanel, swipePanel;
    private ImageView wall;
    private TextView time, date, place;
    private LinearLayout favRow;
    private TextView favGenres;
    private FrameLayout deckSlot;
    private TextView deckInfo;
    private View firstFav;
    private Catalog catalog;
    private Catalog.Item current;
    private int mood = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Glass.init(this);
        catalog = new Catalog(this);
        build();
        setContentView(root);
        refreshFavorites();
        refreshDeck(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tick.run();
        focusFirst();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        refreshFavorites();
        focusFirst();
    }

    @Override
    public void onBackPressed() { /* this is Home: nowhere to go back to */ }

    private void focusFirst() {
        root.post(new Runnable() {
            @Override public void run() {
                if (firstFav != null) firstFav.requestFocus();
            }
        });
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Calendar now = Calendar.getInstance();
            double[] st = Sun.times(now, Cfg.lat(HomeActivity.this), Cfg.lon(HomeActivity.this));
            double nowMin = Sun.minutesNow(now);
            int m = Sun.mood(nowMin, st);
            if (m != mood) {
                mood = m;
                Bitmap bmp = Parts.wallpaper(HomeActivity.this, m);
                if (bmp != null) wall.setImageBitmap(bmp);
            }
            time.setText(Parts.time(now));
            date.setText(Parts.date(now));
            place.setText(Cfg.place(HomeActivity.this) + "   •   " + Sun.nextEvent(nowMin, st));
            handler.postDelayed(this, 60000 - (System.currentTimeMillis() % 60000) + 200);
        }
    };

    // ---------------------------------------------------------------- layout

    private void build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B14);
        root.setClipChildren(false);
        wall = new ImageView(this);
        wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wall, new FrameLayout.LayoutParams(-1, -1));

        time = Parts.thin(this, 176, 0xFFFFFFFF);
        root.addView(time, Glass.at(92, 36, -1, -1));
        date = Glass.text(this, "", 40, 0xFFFFFFFF, true);
        root.addView(date, Glass.at(100, 246, -1, -1));
        place = Glass.text(this, "", 26, 0xC7FFFFFF, false);
        root.addView(place, Glass.at(100, 308, -1, -1));

        buildFavorites();
        buildSwipe();
        buildApps();
    }

    private void buildFavorites() {
        favPanel = new FrameLayout(this);
        favPanel.setBackground(Glass.panel(40));
        favPanel.setClipChildren(false);
        root.addView(favPanel, Glass.at(584, 56, 800, 464));
        favPanel.addView(Glass.text(this, "Favorites", 36, 0xFFFFFFFF, true), Glass.at(34, 26, -1, -1));
        favGenres = Glass.text(this, "", 22, 0xA6FFFFFF, false);
        FrameLayout.LayoutParams g = new FrameLayout.LayoutParams(-2, -2);
        g.gravity = Gravity.TOP | Gravity.END;
        g.topMargin = Glass.px(36);
        g.rightMargin = Glass.px(34);
        favPanel.addView(favGenres, g);
        favRow = new LinearLayout(this);
        favRow.setOrientation(LinearLayout.HORIZONTAL);
        favRow.setClipChildren(false);
        favPanel.addView(favRow, Glass.at(26, 98, 752, 360));
    }

    private void buildSwipe() {
        swipePanel = new FrameLayout(this);
        swipePanel.setBackground(Glass.panel(40));
        swipePanel.setClipChildren(false);
        root.addView(swipePanel, Glass.at(1432, 56, 392, 464));
        swipePanel.addView(Glass.text(this, "Swipe", 36, 0xFFFFFFFF, true), Glass.at(30, 26, -1, -1));
        TextView hint = Glass.text(this, "Like or skip", 22, 0xA6FFFFFF, false);
        FrameLayout.LayoutParams h = new FrameLayout.LayoutParams(-2, -2);
        h.gravity = Gravity.TOP | Gravity.END;
        h.topMargin = Glass.px(36);
        h.rightMargin = Glass.px(30);
        swipePanel.addView(hint, h);

        deckSlot = new FrameLayout(this);
        deckSlot.setClipChildren(false);
        swipePanel.addView(deckSlot, Glass.at(91, 92, 210, 280));
        deckInfo = Glass.text(this, "", 22, 0xB3FFFFFF, false);
        deckInfo.setGravity(Gravity.CENTER);
        swipePanel.addView(deckInfo, Glass.at(0, 380, 392, -1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        FrameLayout dislike = circle(IconView.CROSS, 0xFFFF8A9A, 0x66C0405A);
        dislike.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { rate(-1); }
        });
        FrameLayout like = circle(IconView.HEART, 0xFF7FF0E0, 0x6620A89A);
        like.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { rate(1); }
        });
        row.addView(dislike, new LinearLayout.LayoutParams(Glass.px(64), Glass.px(64)));
        row.addView(label("Dislike", 12, 22));
        row.addView(label("Like", 22, 12));
        row.addView(like, new LinearLayout.LayoutParams(Glass.px(64), Glass.px(64)));
        swipePanel.addView(row, Glass.at(0, 392, 392, 70));
    }

    private TextView label(String s, float left, float right) {
        TextView t = Glass.text(this, s, 24, 0xFFFFFFFF, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = Glass.px(left);
        lp.rightMargin = Glass.px(right);
        t.setLayoutParams(lp);
        return t;
    }

    private FrameLayout circle(int icon, int color, int fill) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        d.setStroke(Math.max(1, Glass.px(1.5f)), 0x66FFFFFF);
        f.setBackground(d);
        IconView v = new IconView(this, icon, color);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Glass.px(34), Glass.px(34));
        lp.gravity = Gravity.CENTER;
        f.addView(v, lp);
        Glass.focusable(f, 32, 1.14f);
        return f;
    }

    // ---------------------------------------------------------------- favorites and swipe

    private FrameLayout poster(Catalog.Item it, float w, float h, float titleSize) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Glass.gradient(it.c1, it.c2, 22));
        GradientDrawable scrim = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xC8000000});
        float r = Glass.px(22);
        scrim.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        View s = new View(this);
        s.setBackground(scrim);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(-1, Glass.px(h * 0.55f));
        slp.gravity = Gravity.BOTTOM;
        f.addView(s, slp);

        TextView chip = Glass.text(this, it.provider, 17, 0xFFFFFFFF, true);
        GradientDrawable cb = new GradientDrawable();
        cb.setColor(0x80000000);
        cb.setCornerRadius(Glass.px(999));
        chip.setBackground(cb);
        chip.setPadding(Glass.px(12), Glass.px(5), Glass.px(12), Glass.px(5));
        f.addView(chip, Glass.at(12, 12, -1, -1));

        TextView t = Glass.text(this, it.title, titleSize, 0xFFFFFFFF, true);
        t.setMaxLines(3);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(-1, -2);
        tlp.gravity = Gravity.BOTTOM;
        tlp.leftMargin = Glass.px(14);
        tlp.rightMargin = Glass.px(10);
        tlp.bottomMargin = Glass.px(14);
        f.addView(t, tlp);
        return f;
    }

    private void refreshFavorites() {
        favRow.removeAllViews();
        firstFav = null;
        List<Catalog.Item> favs = catalog.favorites(4);
        StringBuilder sb = new StringBuilder();
        List<String> seen = new ArrayList<>();
        for (final Catalog.Item it : favs) {
            if (!seen.contains(it.genre)) {
                seen.add(it.genre);
                sb.append(sb.length() == 0 ? "" : " · ").append(it.genre);
            }
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setClipChildren(false);
            FrameLayout card = poster(it, 172, 264, 26);
            Glass.focusable(card, 22, 1.07f);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { launchPackage(it.pkg); }
            });
            col.addView(card, new LinearLayout.LayoutParams(Glass.px(172), Glass.px(264)));
            TextView g = Glass.text(this, it.genre, 22, 0xB3FFFFFF, false);
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-2, -2);
            glp.topMargin = Glass.px(16);
            glp.leftMargin = Glass.px(4);
            col.addView(g, glp);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Glass.px(172), -2);
            clp.rightMargin = Glass.px(22);
            favRow.addView(col, clp);
            if (firstFav == null) firstFav = card;
        }
        favGenres.setText(sb.toString());
    }

    private void refreshDeck(boolean animateIn) {
        deckSlot.removeAllViews();
        List<Catalog.Item> deck = catalog.deck();
        if (deck.isEmpty()) {
            current = null;
            TextView done = Glass.text(this, "All caught up", 28, 0xFFFFFFFF, true);
            done.setGravity(Gravity.CENTER);
            deckSlot.addView(done, new FrameLayout.LayoutParams(-1, -1));
            deckInfo.setText("Favorites are tuned to your taste");
            return;
        }
        current = deck.get(0);
        // two sheets peeking out behind the card, like a stack
        if (deck.size() > 1) deckSlot.addView(behind(deck.get(1), 10, 6, -4));
        FrameLayout card = poster(current, 210, 280, 30);
        card.setRotation(-2f);
        deckSlot.addView(card, new FrameLayout.LayoutParams(-1, -1));
        deckInfo.setText(current.genre + "  ·  " + current.provider);
        if (animateIn) {
            card.setAlpha(0f);
            card.animate().alpha(1f).setDuration(200).start();
        }
    }

    private View behind(Catalog.Item it, float dx, float dy, float rot) {
        FrameLayout f = poster(it, 210, 280, 30);
        f.setAlpha(0.55f);
        f.setRotation(rot);
        f.setTranslationX(Glass.px(dx));
        f.setTranslationY(Glass.px(dy));
        return f;
    }

    private void rate(final int value) {
        if (current == null) return;
        catalog.rate(current, value);
        View top = deckSlot.getChildAt(deckSlot.getChildCount() - 1);
        top.animate().translationX(Glass.px(value > 0 ? 330 : -330)).rotation(value > 0 ? 14 : -14)
                .alpha(0f).setDuration(240).withEndAction(new Runnable() {
                    @Override public void run() {
                        refreshDeck(true);
                        refreshFavorites();
                    }
                }).start();
    }

    // ---------------------------------------------------------------- apps

    private void buildApps() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setBackground(Glass.panel(44));
        panel.setClipChildren(false);
        panel.setPadding(Glass.px(38), Glass.px(30), Glass.px(38), Glass.px(24));
        root.addView(panel, Glass.at(96, 584, 1728, 250));

        addSection(panel, "Streaming", new String[][]{
                {Catalog.N, "Netflix"}, {Catalog.P, "Prime Video"}, {Catalog.H, "Hotstar"}, {Catalog.A, "Apple TV"}});
        addSection(panel, "Video", new String[][]{{"com.google.android.youtube.tv", "YouTube"}});
        addInputs(panel);
        addSystem(panel);
    }

    private LinearLayout section(LinearLayout panel, String title) {
        if (panel.getChildCount() > 0) {
            View d = new View(this);
            d.setBackgroundColor(0x26FFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.max(1, Glass.px(1.5f)), Glass.px(150));
            lp.leftMargin = Glass.px(14);
            lp.rightMargin = Glass.px(14);
            lp.topMargin = Glass.px(30);
            panel.addView(d, lp);
        }
        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.VERTICAL);
        sec.setClipChildren(false);
        TextView h = Glass.text(this, title.toUpperCase(), 18, 0x99FFFFFF, true);
        h.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-2, -2);
        hlp.leftMargin = Glass.px(8);
        hlp.bottomMargin = Glass.px(16);
        sec.addView(h, hlp);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        sec.addView(row);
        panel.addView(sec);
        return row;
    }

    private void addSection(LinearLayout panel, String title, String[][] apps) {
        LinearLayout row = null;
        for (final String[] a : apps) {
            Intent launch = launchIntent(a[0]);
            if (launch == null) continue;
            if (row == null) row = section(panel, title);
            ImageView icon = new ImageView(this);
            try {
                icon.setImageDrawable(getPackageManager().getApplicationIcon(a[0]));
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            final String pkg = a[0];
            row.addView(tile(icon, a[1], 76, new View.OnClickListener() {
                @Override public void onClick(View v) { launchPackage(pkg); }
            }));
        }
    }

    private void addInputs(LinearLayout panel) {
        try {
            TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tm == null) return;
            LinearLayout row = null;
            int n = 0;
            for (final TvInputInfo info : tm.getTvInputList()) {
                if (!info.isPassthroughInput()) continue;
                if (n >= 3) break;
                if (row == null) row = section(panel, "Inputs");
                CharSequence lab = info.loadCustomLabel(this);
                if (lab == null || lab.length() == 0) lab = info.loadLabel(this);
                IconView ic = new IconView(this, IconView.PLUG, 0xFFFFFFFF);
                row.addView(tile(ic, String.valueOf(lab), 60, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    TvContract.buildChannelUriForPassthroughInput(info.getId())));
                        } catch (Exception ignored) { }
                    }
                }));
                n++;
            }
        } catch (Exception ignored) { }
    }

    private void addSystem(LinearLayout panel) {
        LinearLayout row = section(panel, "System");
        row.addView(tile(new IconView(this, IconView.WIFI, 0xFFFFFFFF), "Wi-Fi", 60, new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
        }));
        row.addView(tile(new IconView(this, IconView.SLIDERS, 0xFFFFFFFF), "Settings", 60, new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent(Settings.ACTION_SETTINGS)); }
        }));
        row.addView(tile(new IconView(this, IconView.GRID, 0xFFFFFFFF), "All apps", 60, new View.OnClickListener() {
            @Override public void onClick(View v) { startSafe(new Intent(HomeActivity.this, AllAppsActivity.class)); }
        }));
    }

    private View tile(View icon, String name, float iconSize, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setClipChildren(false);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Glass.tileBg(26));
        box.setClipChildren(false);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(Glass.px(iconSize), Glass.px(iconSize));
        ilp.gravity = Gravity.CENTER;
        box.addView(icon, ilp);
        Glass.focusable(box, 26, 1.1f);
        box.setOnClickListener(click);
        t.addView(box, new LinearLayout.LayoutParams(Glass.px(100), Glass.px(100)));
        TextView l = Glass.text(this, name, 20, 0xE6FFFFFF, false);
        l.setGravity(Gravity.CENTER);
        l.setSingleLine(true);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(Glass.px(124), -2);
        llp.topMargin = Glass.px(12);
        t.addView(l, llp);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(Glass.px(128), -2);
        t.setLayoutParams(tlp);
        return t;
    }

    // ---------------------------------------------------------------- launching

    private Intent launchIntent(String pkg) {
        if (pkg == null) return null;
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    private void launchPackage(String pkg) {
        Intent i = launchIntent(pkg);
        if (i != null) startSafe(i);
    }

    private void startSafe(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) { }
    }
}
