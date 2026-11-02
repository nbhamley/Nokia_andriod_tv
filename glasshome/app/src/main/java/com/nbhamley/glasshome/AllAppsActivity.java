package com.nbhamley.glasshome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Every app that can be opened from a TV, in one grid: what the dock sections leave out. */
public class AllAppsActivity extends Activity {
    private static final class Entry {
        String label;
        Drawable icon;
        Intent intent;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Glass.init(this);
        final PackageManager pm = getPackageManager();
        final List<Entry> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (getPackageName().equals(pkg) || !seen.add(pkg)) continue;
                Entry e = new Entry();
                e.label = String.valueOf(ri.loadLabel(pm));
                e.icon = ri.loadIcon(pm);
                e.intent = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(pkg, ri.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                list.add(e);
            }
        }
        Collections.sort(list, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry c) { return a.label.compareToIgnoreCase(c.label); }
        });

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xF0070B14);
        TextView title = Glass.text(this, "All apps", 40, 0xFFFFFFFF, true);
        root.addView(title, Glass.at(96, 48, -1, -1));
        GridView grid = new GridView(this);
        grid.setNumColumns(7);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setVerticalSpacing(Glass.px(18));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return list.size(); }
            @Override public Object getItem(int i) { return list.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup p) {
                Entry e = list.get(i);
                LinearLayout t = new LinearLayout(AllAppsActivity.this);
                t.setOrientation(LinearLayout.VERTICAL);
                t.setGravity(Gravity.CENTER_HORIZONTAL);
                t.setBackground(Glass.tileBg(24));
                t.setPadding(Glass.px(10), Glass.px(16), Glass.px(10), Glass.px(12));
                ImageView ic = new ImageView(AllAppsActivity.this);
                ic.setImageDrawable(e.icon);
                t.addView(ic, new LinearLayout.LayoutParams(Glass.px(84), Glass.px(84)));
                TextView l = Glass.text(AllAppsActivity.this, e.label, 20, 0xFFFFFFFF, false);
                l.setSingleLine(true);
                l.setEllipsize(android.text.TextUtils.TruncateAt.END);
                l.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = Glass.px(10);
                t.addView(l, lp);
                return t;
            }
        });
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                try { startActivity(list.get(pos).intent); } catch (Exception ignored) { }
            }
        });
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private View last;
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (last != null) last.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                if (v != null) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).start();
                    last = v;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        FrameLayout.LayoutParams gp = Glass.at(96, 140, 1728, 900);
        root.addView(grid, gp);
        setContentView(root);
        grid.requestFocus();
    }
}
