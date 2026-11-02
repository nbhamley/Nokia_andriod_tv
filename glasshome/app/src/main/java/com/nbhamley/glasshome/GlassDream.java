package com.nbhamley.glasshome;

import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/**
 * The screensaver: date, a very large thin time, place and the next sun event, the renewals card, and a strip
 * that follows the day. The wallpaper changes with morning, evening and night. Drifts slightly every minute.
 */
public class GlassDream extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout column;
    private ImageView wall;
    private TextView date, time, place;
    private DayBar bar;
    private TextView caption;
    private FrameLayout slot;
    private int mood = -1;
    private int shift = 0;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        Glass.init(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B14);
        wall = new ImageView(this);
        wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wall, new FrameLayout.LayoutParams(-1, -1));

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        date = Glass.text(this, "", 44, 0xEBFFFFFF, true);
        time = Parts.thin(this, 340, 0xFFFFFFFF);
        place = Glass.text(this, "", 26, 0xC7FFFFFF, false);
        slot = new FrameLayout(this);
        column.addView(date);
        column.addView(time);
        column.addView(place);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Glass.px(640), -2);
        slp.topMargin = Glass.px(40);
        column.addView(slot, slp);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -2);
        clp.topMargin = Glass.px(90);
        root.addView(column, clp);

        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        bar = new DayBar(this);
        foot.addView(bar, new LinearLayout.LayoutParams(Glass.px(640), Glass.px(24)));
        caption = Glass.text(this, "", 20, 0xB3FFFFFF, false);
        LinearLayout.LayoutParams cap = new LinearLayout.LayoutParams(-2, -2);
        cap.topMargin = Glass.px(12);
        foot.addView(caption, cap);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-1, -2);
        flp.gravity = Gravity.BOTTOM;
        flp.bottomMargin = Glass.px(60);
        root.addView(foot, flp);

        setContentView(root);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        tick.run();
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            long ms = 60000 - (System.currentTimeMillis() % 60000) + 200;
            handler.postDelayed(this, ms);
        }
    };

    private void refresh() {
        Calendar now = Calendar.getInstance();
        double[] st = Sun.times(now, Cfg.lat(this), Cfg.lon(this));
        double nowMin = Sun.minutesNow(now);
        int m = Sun.mood(nowMin, st);
        if (m != mood) {
            mood = m;
            wall.setImageBitmap(Parts.wallpaper(this, m));
        }
        date.setText(Parts.date(now));
        time.setText(Parts.time(now));
        place.setText(Cfg.place(this) + "   •   " + Sun.nextEvent(nowMin, st));
        slot.removeAllViews();
        slot.addView(Parts.renewals(this, now, 3));
        bar.set(st, nowMin);
        String name = mood == Sun.MORNING ? "Morning" : mood == Sun.EVENING ? "Evening" : "Night";
        caption.setText(name + " • follows sunrise and sunset in " + Cfg.place(this).split(",")[0]);
        // a few pixels of drift, so no part of the panel holds one spot for hours
        shift = (shift + 1) % 4;
        int dx = new int[]{-18, 12, 18, -10}[shift];
        int dy = new int[]{0, 10, -8, 6}[shift];
        column.setTranslationX(Glass.px(dx));
        column.setTranslationY(Glass.px(dy));
    }
}
