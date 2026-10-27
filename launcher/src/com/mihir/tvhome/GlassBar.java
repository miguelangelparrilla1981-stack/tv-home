package com.mihir.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Glass capsule with four round icon buttons: search, inputs, Wi-Fi, settings. */
public class GlassBar extends LinearLayout {
    public final GlassButton search;
    public final GlassButton inputs;
    public final GlassButton wifi;
    public final GlassButton settings;
    public final List<View> buttons = new ArrayList<>();
    public BiConsumer<View, Boolean> onFocus;

    public GlassBar(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        int pad = (int) (6 * d);
        setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(30 * d);
        bg.setColor(0x24FFFFFF);
        bg.setStroke((int) Math.max(1, d), 0x40FFFFFF);
        setBackground(bg);
        search = add(c, 0, "Buscar", d);
        inputs = add(c, 1, "Entradas", d);
        wifi = add(c, 2, "Wi-Fi", d);
        settings = add(c, 3, "Ajustes", d);
    }

    private GlassButton add(Context c, int kind, String label, float d) {
        GlassButton b = new GlassButton(c, kind);
        b.setTag(label);
        b.setId(View.generateViewId());
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, has) -> {
            if (onFocus != null) onFocus.accept(v, has);
        });
        LayoutParams lp = new LayoutParams((int) (42 * d), (int) (42 * d));
        lp.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
        addView(b, lp);
        buttons.add(b);
        return b;
    }

    /** The Wi-Fi button's label shows the network name while it is focused. */
    public void setWifiName(String name) {
        wifi.setTag(name);
    }

    static final class GlassButton extends View {
        private final int kind;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        GlassButton(Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        @Override
        protected void onFocusChanged(boolean gain, int direction, Rect prev) {
            super.onFocusChanged(gain, direction, prev);
            float s = gain ? 1.14f : 1f;
            animate().scaleX(s).scaleY(s).setInterpolator(new OvershootInterpolator(2f)).setDuration(220).start();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float u = Math.min(w, h);
            float cx = w / 2f;
            float cy = h / 2f;
            boolean f = isFocused();
            if (f) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xF2FFFFFF);
                c.drawCircle(cx, cy, u / 2f, p);
            }
            p.setColor(f ? 0xFF111111 : Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, u * 0.065f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            if (kind == 0) {
                c.drawCircle(cx - u * 0.05f, cy - u * 0.05f, u * 0.17f, p);
                c.drawLine(cx + u * 0.08f, cy + u * 0.08f, cx + u * 0.22f, cy + u * 0.22f, p);
            } else if (kind == 1) {
                r.set(cx - u * 0.24f, cy - u * 0.2f, cx + u * 0.24f, cy + u * 0.08f);
                c.drawRoundRect(r, u * 0.05f, u * 0.05f, p);
                c.drawLine(cx - u * 0.12f, cy + u * 0.23f, cx + u * 0.12f, cy + u * 0.23f, p);
                c.drawLine(cx, cy + u * 0.08f, cx, cy + u * 0.23f, p);
            } else if (kind == 2) {
                float oy = cy + u * 0.18f;
                for (int i = 1; i <= 3; i++) {
                    float rad = u * 0.11f * i;
                    r.set(cx - rad, oy - rad, cx + rad, oy + rad);
                    c.drawArc(r, 225f, 90f, false, p);
                }
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, oy, u * 0.04f, p);
            } else {
                c.drawLine(cx - u * 0.25f, cy - u * 0.1f, cx + u * 0.25f, cy - u * 0.1f, p);
                c.drawLine(cx - u * 0.25f, cy + u * 0.1f, cx + u * 0.25f, cy + u * 0.1f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - u * 0.08f, cy - u * 0.1f, u * 0.075f, p);
                c.drawCircle(cx + u * 0.08f, cy + u * 0.1f, u * 0.075f, p);
            }
        }
    }
}
