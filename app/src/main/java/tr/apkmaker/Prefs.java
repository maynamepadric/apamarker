package tr.apkmaker;

import android.content.Context;

/** Kalici ayarlar ("cfg" tercihleri). */
public class Prefs {
    public static final String DEF_ARGS = "--disorder 1 --tlsrec 1+s";

    public static boolean bool(Context c, String key, boolean def) {
        return c.getSharedPreferences("cfg", 0).getBoolean(key, def);
    }

    public static void setBool(Context c, String key, boolean v) {
        c.getSharedPreferences("cfg", 0).edit().putBoolean(key, v).apply();
    }

    public static String str(Context c, String key, String def) {
        return c.getSharedPreferences("cfg", 0).getString(key, def);
    }

    public static void setStr(Context c, String key, String v) {
        c.getSharedPreferences("cfg", 0).edit().putString(key, v).apply();
    }
}
