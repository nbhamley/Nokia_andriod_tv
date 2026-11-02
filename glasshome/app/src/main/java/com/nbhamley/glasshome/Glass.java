package com.nbhamley.glasshome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Looks: everything is laid out on a 1920x1080 grid and scaled to the real screen. */
final class Glass {
    static float S = 1f;

    private Glass() { }

    static void init(Context c) {
        S = c.getResources().getDisplayMetrics().widthPixels / 1920f;
    }

    static int px(float d) { return Math.round(d * S); }

    /** Frosted glass without a blur (too heavy for this TV): dark scrim, light gradient, thin bright rim. */
    static Drawable panel(float radius) {
        GradientDrawable scrim = new GradientDrawable();
        scrim.setColor(0x520A1230);
        scrim.setCornerRadius(px(radius));
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x3CFFFFFF, 0x12FFFFFF, 0x26FFFFFF});
        glass.setCornerRadius(px(radius));
        glass.setStroke(Math.max(1, px(1.5f)), 0x45FFFFFF);
        return new LayerDrawable(new Drawable[]{scrim, glass});
    }

    static Drawable gradient(int c1, int c2, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{c1, c2});
        g.setCornerRadius(px(radius));
        return g;
    }

    static Drawable tileBg(float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x55FFFFFF, 0x26FFFFFF});
        g.setCornerRadius(px(radius));
        g.setStroke(Math.max(1, px(1.5f)), 0x40FFFFFF);
        return g;
    }

    static Drawable rim(float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(px(radius));
        g.setStroke(Math.max(2, px(4)), 0xFFFFFFFF);
        return g;
    }

    static TextView text(Context c, String s, float size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTextColor(color);
        t.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL)
                : Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setSingleLine(false);
        return t;
    }

    static FrameLayout.LayoutParams at(float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w < 0 ? -2 : px(w), h < 0 ? -2 : px(h));
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        return lp;
    }

    /** Focus = a small spring-like grow and a white rim, the way the glass tiles lift on the mockup. */
    static void focusable(final View v, final float radius, final float grow) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean has) {
                float s = has ? grow : 1f;
                view.animate().scaleX(s).scaleY(s).setDuration(220)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
                if (view instanceof FrameLayout) {
                    ((FrameLayout) view).setForeground(has ? rim(radius) : null);
                }
                view.setElevation(has ? px(24) : 0);
            }
        });
    }
}
