package com.nbhamley.glasshome;

import java.util.Calendar;
import java.util.Locale;

/** Sunrise, sunset and the time-of-day mood, computed on the TV (NOAA general solar formulas, no network). */
final class Sun {
    static final int NIGHT = 0, MORNING = 1, EVENING = 2;
    static final String[] MOOD_NAMES = {"night", "morning", "evening"};

    private Sun() { }

    /** {sunrise, sunset} in local minutes after midnight. */
    static double[] times(Calendar cal, double lat, double lon) {
        int doy = cal.get(Calendar.DAY_OF_YEAR);
        double g = 2 * Math.PI / 365.0 * (doy - 1);
        double eq = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g)
                - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
        double decl = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g)
                - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g)
                - 0.002697 * Math.cos(3 * g) + 0.00148 * Math.sin(3 * g);
        double latR = Math.toRadians(lat);
        double cosH = Math.cos(Math.toRadians(90.833)) / (Math.cos(latR) * Math.cos(decl))
                - Math.tan(latR) * Math.tan(decl);
        cosH = Math.max(-1, Math.min(1, cosH));
        double ha = Math.toDegrees(Math.acos(cosH));
        double tz = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 3600000.0;
        double rise = 720 - 4 * (lon + ha) - eq + tz * 60;
        double set = 720 - 4 * (lon - ha) - eq + tz * 60;
        return new double[]{rise, set};
    }

    static double minutesNow(Calendar cal) {
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60.0;
    }

    /** Night until half an hour before sunrise, morning until 15:30, evening until 90 minutes after sunset. */
    static int mood(double nowMin, double[] st) {
        if (nowMin < st[0] - 30 || nowMin >= st[1] + 90) return NIGHT;
        return nowMin < 15.5 * 60 ? MORNING : EVENING;
    }

    /** 6:35 AM */
    static String clock12(double minutes) {
        int m = (int) Math.round(minutes);
        m = ((m % 1440) + 1440) % 1440;
        int h = m / 60;
        String ampm = h < 12 ? "AM" : "PM";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, m % 60, ampm);
    }

    /** "Sunrise 6:35 AM" before dawn, "Sunset 6:00 PM" during the day, tomorrow's sunrise afterwards. */
    static String nextEvent(double nowMin, double[] st) {
        if (nowMin < st[0]) return "Sunrise " + clock12(st[0]);
        if (nowMin < st[1]) return "Sunset " + clock12(st[1]);
        return "Sunrise " + clock12(st[0]);
    }
}
