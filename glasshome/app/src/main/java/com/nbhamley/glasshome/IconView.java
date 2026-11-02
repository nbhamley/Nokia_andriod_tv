package com.nbhamley.glasshome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small line icons drawn in code, on a 100x100 grid: plug (HDMI), wifi, sliders (settings), grid, heart, cross. */
final class IconView extends View {
    static final int PLUG = 0, WIFI = 1, SLIDERS = 2, GRID = 3, HEART = 4, CROSS = 5;
    private final int type;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(color);
        fill.setColor(color);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float u = Math.min(getWidth(), getHeight()) / 100f;
        cv.save();
        cv.translate((getWidth() - 100 * u) / 2f, (getHeight() - 100 * u) / 2f);
        cv.scale(u, u);
        line.setStrokeWidth(5f);
        switch (type) {
            case PLUG:
                cv.drawRoundRect(new RectF(30, 38, 70, 66), 8, 8, line);
                cv.drawLine(40, 20, 40, 38, line);
                cv.drawLine(60, 20, 60, 38, line);
                cv.drawLine(50, 66, 50, 82, line);
                break;
            case WIFI:
                for (int r = 1; r <= 3; r++) {
                    float rad = r * 15f;
                    cv.drawArc(new RectF(50 - rad, 70 - rad, 50 + rad, 70 + rad), 225, 90, false, line);
                }
                cv.drawCircle(50, 70, 4f, fill);
                break;
            case SLIDERS:
                cv.drawLine(28, 20, 28, 80, line);
                cv.drawLine(50, 20, 50, 80, line);
                cv.drawLine(72, 20, 72, 80, line);
                cv.drawLine(20, 62, 36, 62, line);
                cv.drawLine(42, 34, 58, 34, line);
                cv.drawLine(64, 52, 80, 52, line);
                break;
            case GRID:
                for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
                    cv.drawRoundRect(new RectF(24 + i * 20, 24 + j * 20, 40 + i * 20, 40 + j * 20), 4, 4, fill);
                }
                break;
            case HEART: {
                Path p = new Path();
                p.moveTo(50, 82);
                p.cubicTo(8, 52, 16, 16, 50, 34);
                p.cubicTo(84, 16, 92, 52, 50, 82);
                p.close();
                cv.drawPath(p, line);
                break;
            }
            default:
                cv.drawLine(28, 28, 72, 72, line);
                cv.drawLine(72, 28, 28, 72, line);
        }
        cv.restore();
    }
}
