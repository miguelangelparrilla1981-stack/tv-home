package com.mihir.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

/** Glass weather card: city, condition, temperature, max/min and the next four days (Open-Meteo, Madrid). */
public class WeatherStrip extends LinearLayout {
    private static final String API = "https://api.open-meteo.com/v1/forecast?latitude=40.4168&longitude=-3.7038&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Europe%2FMadrid&forecast_days=5";
    private static final long REFRESH_MS = 60L * 60 * 1000;
    private static final long RETRY_MS = 5L * 60 * 1000;
    private static final int DAYS = 4;

    private final float dens;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final TextView cond;
    private final TextView temp;
    private final TextView hl;
    private final IconView mainIcon;
    private final TextView[] labels = new TextView[DAYS];
    private final TextView[] temps = new TextView[DAYS];
    private final IconView[] icons = new IconView[DAYS];
    private long lastTry = 0;
    private long lastOk = 0;
    private boolean loading = false;

    public WeatherStrip(Context c) {
        super(c);
        dens = c.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setVisibility(GONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(22 * dens);
        bg.setColor(0x24FFFFFF);
        bg.setStroke((int) Math.max(1, dens), 0x40FFFFFF);
        setBackground(bg);
        setPadding(px(18), px(14), px(18), px(14));

        LinearLayout top = new LinearLayout(c);
        TextView city = label(c, 12, Color.WHITE, "sans-serif-medium");
        city.setText("MADRID");
        city.setLetterSpacing(0.12f);
        cond = label(c, 12, 0xCCFFFFFF, "sans-serif-medium");
        cond.setLetterSpacing(0.12f);
        top.addView(city, new LayoutParams(0, -2, 1f));
        top.addView(cond, new LayoutParams(-2, -2));
        addView(top, new LayoutParams(-1, -2));

        LinearLayout mid = new LinearLayout(c);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        temp = label(c, 52, Color.WHITE, "sans-serif-thin");
        temp.setIncludeFontPadding(false);
        mid.addView(temp, new LayoutParams(0, -2, 1f));
        LinearLayout side = new LinearLayout(c);
        side.setOrientation(VERTICAL);
        side.setGravity(Gravity.END);
        mainIcon = new IconView(c);
        side.addView(mainIcon, new LayoutParams(px(44), px(36)));
        hl = label(c, 13, Color.WHITE, "sans-serif-medium");
        side.addView(hl, new LayoutParams(-2, -2));
        mid.addView(side, new LayoutParams(-2, -2));
        addView(mid, lp(-1, -2, 0, px(4), 0, px(4)));

        View line = new View(c);
        line.setBackgroundColor(0x40FFFFFF);
        addView(line, lp(-1, Math.max(1, px(1)), 0, px(4), 0, px(8)));

        LinearLayout days = new LinearLayout(c);
        for (int i = 0; i < DAYS; i++) {
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            labels[i] = label(c, 12, 0xCCFFFFFF, "sans-serif-medium");
            icons[i] = new IconView(c);
            temps[i] = label(c, 15, Color.WHITE, "sans-serif-medium");
            col.addView(labels[i], new LayoutParams(-2, -2));
            col.addView(icons[i], new LayoutParams(px(26), px(22)));
            col.addView(temps[i], new LayoutParams(-2, -2));
            days.addView(col, new LayoutParams(0, -2, 1f));
        }
        addView(days, new LayoutParams(-1, -2));
    }

    private int px(float v) {
        return (int) (v * dens);
    }

    private static LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LayoutParams p = new LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static TextView label(Context c, float sp, int color, String family) {
        TextView t = new TextView(c);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setTypeface(Fonts.get(c, family));
        t.setShadowLayer(8, 0, 1, 0xB3000000);
        return t;
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        if (loading) return;
        long wait = lastOk > 0 ? REFRESH_MS : RETRY_MS;
        if (lastTry != 0 && now - lastTry < wait) return;
        lastTry = now;
        loading = true;
        new Thread(() -> {
            String json = null;
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(API).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                json = sb.toString();
            } catch (Exception ignored) {
            } finally {
                if (con != null) con.disconnect();
            }
            final String result = json;
            ui.post(() -> {
                loading = false;
                if (result != null) show(result);
            });
        }).start();
    }

    private static String describe(int code) {
        if (code == 0) return "Despejado";
        if (code == 1) return "Casi despejado";
        if (code == 2) return "Nubes y claros";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Niebla";
        if (code >= 51 && code <= 57) return "Llovizna";
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return "Lluvia";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "Nieve";
        if (code >= 95) return "Tormenta";
        return "Nublado";
    }

    private void show(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject cur = root.getJSONObject("current");
            JSONObject daily = root.getJSONObject("daily");
            JSONArray codes = daily.getJSONArray("weather_code");
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray min = daily.getJSONArray("temperature_2m_min");
            Locale es = new Locale("es", "ES");
            int code = cur.getInt("weather_code");
            temp.setText(Math.round(cur.getDouble("temperature_2m")) + "\u00b0");
            cond.setText(describe(code).toUpperCase(es));
            mainIcon.setCode(code);
            hl.setText("M\u00e1x " + Math.round(max.getDouble(0)) + "\u00b0  M\u00edn " + Math.round(min.getDouble(0)) + "\u00b0");
            Calendar cal = Calendar.getInstance();
            String[] names = {"Dom", "Lun", "Mar", "Mi\u00e9", "Jue", "Vie", "S\u00e1b"};
            int n = Math.min(DAYS, codes.length() - 1);
            for (int i = 0; i < n; i++) {
                int idx = i + 1;
                int dow = (cal.get(Calendar.DAY_OF_WEEK) - 1 + idx) % 7;
                labels[i].setText(names[dow]);
                icons[i].setCode(codes.getInt(idx));
                temps[i].setText(Math.round(max.getDouble(idx)) + "\u00b0");
            }
            lastOk = System.currentTimeMillis();
            setVisibility(VISIBLE);
        } catch (Exception ignored) {
        }
    }

    private static class IconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int kind = 2;

        IconView(Context c) {
            super(c);
        }

        void setCode(int code) {
            if (code == 0 || code == 1) kind = 0;
            else if (code == 2) kind = 1;
            else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) kind = 3;
            else if ((code >= 71 && code <= 77) || code == 85 || code == 86) kind = 4;
            else if (code >= 95) kind = 5;
            else kind = 2;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float u = Math.min(w, h);
            float cx = w / 2f;
            float cy = h / 2f;
            if (kind == 0) {
                sun(c, cx, cy, u * 0.28f);
            } else if (kind == 1) {
                sun(c, cx - u * 0.12f, cy - u * 0.12f, u * 0.2f);
                cloud(c, cx + u * 0.06f, cy + u * 0.1f, u * 0.8f);
            } else if (kind == 2) {
                cloud(c, cx, cy, u * 0.9f);
            } else if (kind == 3) {
                cloud(c, cx, cy - u * 0.12f, u * 0.8f);
                drops(c, cx, cy, u);
            } else if (kind == 4) {
                cloud(c, cx, cy - u * 0.12f, u * 0.8f);
                snow(c, cx, cy, u);
            } else {
                cloud(c, cx, cy - u * 0.12f, u * 0.8f);
                bolt(c, cx, cy, u);
            }
        }

        private void sun(Canvas c, float x, float y, float r) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFF6C453);
            c.drawCircle(x, y, r, p);
        }

        private void cloud(Canvas c, float x, float y, float s) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFD5DDE8);
            c.drawOval(x - s * 0.4f, y - s * 0.05f, x + s * 0.4f, y + s * 0.3f, p);
            c.drawCircle(x - s * 0.12f, y - s * 0.02f, s * 0.2f, p);
            c.drawCircle(x + s * 0.12f, y - s * 0.05f, s * 0.17f, p);
        }

        private void drops(Canvas c, float x, float y, float u) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, u * 0.07f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xFF6FB3FF);
            c.drawLine(x - u * 0.15f, y + u * 0.2f, x - u * 0.2f, y + u * 0.4f, p);
            c.drawLine(x + u * 0.1f, y + u * 0.2f, x + u * 0.05f, y + u * 0.4f, p);
        }

        private void snow(Canvas c, float x, float y, float u) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawCircle(x - u * 0.15f, y + u * 0.3f, u * 0.05f, p);
            c.drawCircle(x + u * 0.1f, y + u * 0.3f, u * 0.05f, p);
        }

        private void bolt(Canvas c, float x, float y, float u) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, u * 0.08f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xFFF6C453);
            c.drawLine(x, y + u * 0.16f, x - u * 0.08f, y + u * 0.3f, p);
            c.drawLine(x - u * 0.08f, y + u * 0.3f, x + u * 0.04f, y + u * 0.3f, p);
            c.drawLine(x + u * 0.04f, y + u * 0.3f, x - u * 0.04f, y + u * 0.44f, p);
        }
    }
}
