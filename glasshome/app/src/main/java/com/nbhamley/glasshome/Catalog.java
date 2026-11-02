package com.nbhamley.glasshome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What to suggest. The list is a starter set in assets/catalog.json (edit it, availability changes by month and
 * region). Likes and dislikes are stored only on this TV and move the genres up or down. No network.
 */
final class Catalog {
    static final class Item {
        String id, title, provider, genre;
        String pkg;
        int c1, c2;
    }

    static final String N = "com.netflix.ninja";
    static final String P = "com.amazon.amazonvideo.livingroom";
    static final String H = "in.startv.hotstar";
    static final String A = "com.apple.atve.androidtv.appletv";

    private final List<Item> all = new ArrayList<>();
    private final SharedPreferences prefs;

    Catalog(Context c) {
        prefs = Cfg.p(c);
        try {
            InputStream in = c.getAssets().open("catalog.json");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            JSONArray arr = new JSONArray(bo.toString("UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.title = o.getString("title");
                it.provider = o.getString("provider");
                it.genre = o.getString("genre");
                it.id = it.provider + ":" + it.title;
                it.pkg = pkgFor(it.provider);
                int[] pal = palette(it.genre);
                it.c1 = pal[0];
                it.c2 = pal[1];
                all.add(it);
            }
        } catch (Exception ignored) { }
    }

    static String pkgFor(String provider) {
        if ("Netflix".equals(provider)) return N;
        if ("Prime Video".equals(provider)) return P;
        if ("Hotstar".equals(provider)) return H;
        if ("Apple TV".equals(provider)) return A;
        return null;
    }

    static int[] palette(String genre) {
        if ("Thriller".equals(genre)) return new int[]{0xFF1F6F78, 0xFF07101C};
        if ("Action".equals(genre)) return new int[]{0xFFE07A4F, 0xFF26091A};
        if ("Animation".equals(genre)) return new int[]{0xFF9B4FD6, 0xFF1A0A38};
        return new int[]{0xFF3A5BD0, 0xFF0A0F2A};
    }

    int rating(Item it) { return prefs.getInt("like:" + it.id, 0); }

    private int genreScore(String g) { return prefs.getInt("genre:" + g, 0); }

    void rate(Item it, int value) {
        int old = rating(it);
        int delta = value - old;
        prefs.edit().putInt("like:" + it.id, value)
                .putInt("genre:" + it.genre, genreScore(it.genre) + delta).apply();
    }

    /** Titles not yet rated, best genres first: the Swipe deck. */
    List<Item> deck() {
        List<Item> out = new ArrayList<>();
        for (Item it : all) if (rating(it) == 0) out.add(it);
        sortByGenre(out);
        return out;
    }

    /** Four picks for the Favorites row: never a disliked title, at most two per genre, best genres first. */
    List<Item> favorites(int count) {
        List<Item> pool = new ArrayList<>();
        for (Item it : all) if (rating(it) >= 0) pool.add(it);
        sortByGenre(pool);
        List<Item> out = new ArrayList<>();
        Map<String, Integer> used = new HashMap<>();
        for (Item it : pool) {
            Integer u = used.get(it.genre);
            if (u != null && u >= 2) continue;
            out.add(it);
            used.put(it.genre, u == null ? 1 : u + 1);
            if (out.size() == count) break;
        }
        return out;
    }

    private void sortByGenre(List<Item> list) {
        final List<Item> order = all;
        Collections.sort(list, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) {
                int s = Integer.compare(genreScore(b.genre), genreScore(a.genre));
                if (s != 0) return s;
                int r = Integer.compare(rating(b), rating(a));
                if (r != 0) return r;
                return Integer.compare(order.indexOf(a), order.indexOf(b));
            }
        });
    }
}
