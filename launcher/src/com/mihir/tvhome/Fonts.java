package com.mihir.tvhome;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/** Loads Outfit (variable font in assets/fonts) at the weight a legacy family name asks for. */
final class Fonts {
    private static final Map<Integer, Typeface> CACHE = new HashMap<>();

    private Fonts() { }

    static Typeface get(Context c, String family) {
        int w = family.contains("thin") ? 100 : family.contains("light") ? 300 : family.contains("medium") ? 500 : 400;
        Typeface t = CACHE.get(w);
        if (t != null) return t;
        try {
            t = new Typeface.Builder(c.getAssets(), "fonts/Outfit.ttf")
                    .setFontVariationSettings("'wght' " + w).build();
        } catch (RuntimeException e) {
            t = null;
        }
        if (t == null) t = Typeface.create(family, Typeface.NORMAL);
        CACHE.put(w, t);
        return t;
    }
}
