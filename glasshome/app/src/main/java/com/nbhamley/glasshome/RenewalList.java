package com.nbhamley.glasshome;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Subscription renewals the owner typed in. Spec format: "Netflix=2026-11-12;Prime Video=2026-11-28/y".
 * A date repeats every month, or every year with the "/y" suffix, so it rolls forward by itself.
 */
final class RenewalList {
    static final class Item {
        String name;
        Calendar next;
        int daysLeft;
        String when; // "12 Nov"
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private RenewalList() { }

    static List<Item> parse(String spec, Calendar now, int max) {
        List<Item> out = new ArrayList<>();
        if (spec == null) return out;
        Calendar today = (Calendar) now.clone();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        for (String part : spec.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String name = part.substring(0, eq).trim();
            String date = part.substring(eq + 1).trim();
            boolean yearly = false;
            if (date.endsWith("/y")) {
                yearly = true;
                date = date.substring(0, date.length() - 2);
            }
            String[] p = date.split("-");
            if (name.isEmpty() || p.length != 3) continue;
            int y, mo, d;
            try {
                y = Integer.parseInt(p[0].trim());
                mo = Integer.parseInt(p[1].trim());
                d = Integer.parseInt(p[2].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (mo < 1 || mo > 12 || d < 1 || d > 31) continue;
            Calendar c = (Calendar) today.clone();
            c.clear();
            c.set(y, mo - 1, d, 0, 0, 0);
            int guard = 0;
            while (c.before(today) && guard++ < 600) {
                c.add(yearly ? Calendar.YEAR : Calendar.MONTH, 1);
            }
            Item it = new Item();
            it.name = name;
            it.next = c;
            it.daysLeft = (int) Math.round((c.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
            it.when = String.format(Locale.US, "%d %s", c.get(Calendar.DAY_OF_MONTH), MONTHS[c.get(Calendar.MONTH)]);
            out.add(it);
        }
        Collections.sort(out, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) { return Integer.compare(a.daysLeft, b.daysLeft); }
        });
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    static String daysText(int d) {
        if (d <= 0) return "today";
        if (d == 1) return "tomorrow";
        return "in " + d + " days";
    }
}
