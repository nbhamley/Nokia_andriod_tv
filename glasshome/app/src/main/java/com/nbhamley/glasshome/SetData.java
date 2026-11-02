package com.nbhamley.glasshome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Settings over adb (scripts/set_renewals.sh and set_place.sh wrap these):
 *   am broadcast -n com.nbhamley.glasshome/.SetData --es renewals "Netflix=2026-11-12;Hotstar=2026-12-03/y"
 *   am broadcast -n com.nbhamley.glasshome/.SetData --es place "Pune, India" --es lat 18.5204 --es lon 73.8567
 *   am broadcast -n com.nbhamley.glasshome/.SetData --ez reset_taste true
 */
public class SetData extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        SharedPreferences.Editor e = Cfg.p(c).edit();
        if (i.hasExtra("renewals")) e.putString("renewals", i.getStringExtra("renewals"));
        if (i.hasExtra("place")) e.putString("place", i.getStringExtra("place"));
        String lat = i.getStringExtra("lat");
        String lon = i.getStringExtra("lon");
        try {
            if (lat != null) { Double.parseDouble(lat); e.putString("lat", lat); }
            if (lon != null) { Double.parseDouble(lon); e.putString("lon", lon); }
        } catch (NumberFormatException ignored) { }
        if (i.getBooleanExtra("reset_taste", false)) {
            for (String k : Cfg.p(c).getAll().keySet()) {
                if (k.startsWith("like:") || k.startsWith("genre:")) e.remove(k);
            }
        }
        e.apply();
    }
}
