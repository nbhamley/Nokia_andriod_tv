package com.nbhamley.glasshome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** The 24-hour strip of the screensaver: night, morning, evening, and a dot at "now". */
final class DayBar extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[] st = {390, 1080};
    private double now = 0;

    DayBar(Context c) { super(c); }

    void set(double[] sunTimes, double nowMinutes) {
        st = sunTimes;
        now = nowMinutes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float bh = Glass.px(8), top = (h - bh) / 2f;
        float x0 = (float) Math.max(0, (st[0] - 30) / 1440.0) * w;
        float x1 = (float) (15.5 * 60 / 1440.0) * w;
        float x2 = (float) Math.min(1440, st[1] + 90) / 1440f * w;
        RectF all = new RectF(0, top, w, top + bh);
        p.setShader(null);
        p.setColor(0xFF232A7A);
        cv.drawRoundRect(all, bh / 2, bh / 2, p);
        p.setShader(new LinearGradient(x0, 0, x1, 0, 0xFF2FA6A0, 0xFFE8A07A, Shader.TileMode.CLAMP));
        cv.drawRect(x0, top, x1, top + bh, p);
        p.setShader(new LinearGradient(x1, 0, x2, 0, 0xFF7A55E0, 0xFFD95C9A, Shader.TileMode.CLAMP));
        cv.drawRect(x1, top, x2, top + bh, p);
        p.setShader(null);
        float cx = (float) (now / 1440.0) * w;
        float r = Glass.px(9);
        p.setColor(0x40FFFFFF);
        cv.drawCircle(cx, h / 2f, r + Glass.px(4), p);
        p.setColor(0xFFFFFFFF);
        cv.drawCircle(cx, h / 2f, r, p);
    }
}
