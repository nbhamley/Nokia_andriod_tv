package com.nbhamley.glasshome;

import android.content.Context;
import android.content.SharedPreferences;

/** Small settings kept on the TV: place, coordinates, renewals and the like/dislike history. */
final class Cfg {
    private Cfg() { }

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("glasshome", Context.MODE_PRIVATE);
    }

    static String place(Context c) { return p(c).getString("place", "Pune, India"); }

    static double lat(Context c) { return Double.parseDouble(p(c).getString("lat", "18.5204")); }

    static double lon(Context c) { return Double.parseDouble(p(c).getString("lon", "73.8567")); }

    static String renewals(Context c) { return p(c).getString("renewals", ""); }
}
