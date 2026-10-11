package tr.apkmaker;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Uretilen uygulamalar kaydi. Satir: ad|paket|surum|ikonYolu  (eski kayitlar: ad|paket) */
public class Reg {
    private static SharedPreferences p(Context context) {
        return context.getSharedPreferences("apps", 0);
    }

    private static String clean(String s) {
        return s.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    /** {ad, paket, surum, ikon} listesi (en yeni ustte) */
    public static List<String[]> entries(Context context) {
        List<String[]> res = new ArrayList<String[]>();
        for (String line : p(context).getString("items", "").split("\n")) {
            String[] a = line.split("\\|", -1);
            if (a.length < 2 || a[0].length() == 0) {
                continue;
            }
            res.add(new String[]{a[0], a[1], a.length > 2 ? a[2] : "0", a.length > 3 ? a[3] : ""});
        }
        return res;
    }

    private static void save(Context context, List<String[]> es) {
        StringBuilder sb = new StringBuilder();
        for (String[] e : es) {
            sb.append(e[0]).append('|').append(e[1]).append('|').append(e[2]).append('|').append(e[3]).append('\n');
        }
        p(context).edit().putString("items", sb.toString()).apply();
    }

    public static String pkgFor(Context context, String name, String type) {
        name = clean(name);
        List<String[]> es = entries(context);
        for (String[] e : es) {
            if (e[0].equals(name)) {
                return e[1];
            }
        }
        String base = name.toLowerCase(java.util.Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
        // paket parcasi harfle baslamali (ornek: "2048" -> "app2048")
        if (base.length() == 0 || !Character.isLetter(base.charAt(0))) {
            base = "app" + base;
        }
        if (base.length() < 3) {
            base = base + "app";
        }
        if (base.length() > 12) {
            base = base.substring(0, 12);
        }
        String hex = Integer.toHexString(name.hashCode());
        String pkg = "com." + type + "." + base + hex.substring(0, Math.min(8, hex.length()));
        es.add(0, new String[]{name, pkg, "0", ""});
        save(context, es);
        return pkg;
    }

    /** ByeDPI port sayaci: her DPI'li uygulamaya sirayla 1082, 1083, 1084... */
    public static int nextPort(Context context) {
        SharedPreferences sp = context.getSharedPreferences("dpi", 0);
        int port = sp.getInt("next", 1082);
        sp.edit().putInt("next", port + 1).apply();
        return port;
    }

    /** Bir sonraki uretimin surum numarasi (1, 2, 3...) = versionCode */
    public static int nextVersion(Context context, String name) {
        name = clean(name);
        for (String[] e : entries(context)) {
            if (e[0].equals(name)) {
                try {
                    return Integer.parseInt(e[2]) + 1;
                } catch (Exception ex) {
                    return 1;
                }
            }
        }
        return 1;
    }

    public static String versionName(int code) {
        return "1." + (code - 1);
    }

    /** Basarili uretimden sonra surumu artirir ve ikonu gecmis icin saklar */
    public static void record(Context context, String name, String pkg, File icon) {
        name = clean(name);
        int ver = nextVersion(context, name);
        String iconPath = "";
        try {
            File dir = new File(context.getFilesDir(), "icons");
            dir.mkdirs();
            File out = new File(dir, pkg + ".png");
            if (icon != null && icon.exists()) {
                FileInputStream in = new FileInputStream(icon);
                FileOutputStream o = new FileOutputStream(out);
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) {
                    o.write(b, 0, n);
                }
                in.close();
                o.close();
                iconPath = out.getAbsolutePath();
            } else {
                out.delete();
            }
        } catch (Exception e) {
        }
        List<String[]> es = entries(context);
        for (int i = 0; i < es.size(); i++) {
            if (es.get(i)[0].equals(name)) {
                es.remove(i);
                break;
            }
        }
        es.add(0, new String[]{name, pkg, String.valueOf(ver), iconPath});
        save(context, es);
    }
}
