package tr.apkmaker;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** Arac durumlari. Satir: {ad, durum(ok|warn|fail), ayrinti} */
public class ToolCheck {
    private static String[] row(String n, String s, String d) {
        return new String[]{n, s, d};
    }

    public static List<String[]> run(Context ctx) {
        List<String[]> r = new ArrayList<String[]>();
        Builder.Cb quiet = new Builder.Cb() {
            public void log(String s) { }
            public void status(String s) { }
            public void done(boolean ok, String path) { }
        };
        boolean ready = false;
        try {
            ready = Builder.ensureTools(ctx, quiet);
        } catch (Throwable t) {
        }
        if (!ready) {
            r.add(row("Arac seti", "fail", "cikartilamadi (depolama dolu olabilir)"));
            return r;
        }
        r.add(row("Arac seti", "ok", "cikartildi"));

        // aapt: gercekten calistir
        try {
            final StringBuilder out = new StringBuilder();
            Builder.Cb cap = new Builder.Cb() {
                public void log(String s) { if (out.length() == 0) out.append(s.trim()); }
                public void status(String s) { }
                public void done(boolean ok, String path) { }
            };
            int rc = Builder.runCmd(cap, Builder.tools, new String[]{new File(Builder.tc, "bin/aapt").getAbsolutePath(), "version"}, new String[0]);
            r.add(row("aapt (paketleme)", rc == 0 ? "ok" : "fail", rc == 0 ? out.toString() : "calismadi (kod " + rc + ")"));
        } catch (Throwable t) {
            r.add(row("aapt (paketleme)", "fail", String.valueOf(t)));
        }

        String ecj = Builder.firstJar("ecj");
        r.add(row("ecj (Java derleyici)", ecj != null && new File(ecj).exists() ? "ok" : "fail", ecj != null ? "hazir" : "dosya yok"));
        String dx = Builder.firstJar("dx");
        r.add(row("dx (dex donusturucu)", dx != null && new File(dx).exists() ? "ok" : "fail", dx != null ? "hazir" : "dosya yok"));
        File aj = new File(Builder.tc, "share/android.jar");
        r.add(row("android.jar", aj.exists() ? "ok" : "fail", aj.exists() ? (aj.length() / 1024 / 1024) + " MB" : "dosya yok"));
        r.add(row("Imzalayici", "ok", "dahili (v1)"));

        // ByeDPI
        try {
            InputStream in = ctx.getAssets().open("ciadpi.bin");
            File f = new File(ctx.getFilesDir(), "ciadpi_test");
            Builder.copyStream(in, new FileOutputStream(f));
            f.setExecutable(true, false);
            ServerSocket ss = new ServerSocket(0);
            int port = ss.getLocalPort();
            ss.close();
            Process p = new ProcessBuilder(f.getAbsolutePath(), "-i", "127.0.0.1", "-p", String.valueOf(port)).redirectErrorStream(true).start();
            boolean up = false;
            for (int i = 0; i < 30 && !up; i++) {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress("127.0.0.1", port), 100);
                    s.close();
                    up = true;
                } catch (Throwable t) {
                    try { Thread.sleep(70); } catch (Throwable t2) { }
                }
            }
            p.destroy();
            f.delete();
            r.add(row("ByeDPI (ciadpi)", up ? "ok" : "warn", up ? "calisiyor" : "baslamadi, uretilen uygulamada yine de denenebilir"));
        } catch (java.io.FileNotFoundException e) {
            r.add(row("ByeDPI (ciadpi)", "fail", "assets/ciadpi.bin yok, DPI ozelligi kapali"));
        } catch (Throwable t) {
            r.add(row("ByeDPI (ciadpi)", "warn", "test calistirilamadi: " + t.getMessage()));
        }

        if (Prefs.bool(ctx, "selfdpi", false)) {
            r.add(row("APK Maker DPI (gorsel indirme)", SelfDpi.ensure(ctx) ? "ok" : "warn", SelfDpi.running() ? "aktif" : "baslatilamadi"));
        } else {
            r.add(row("APK Maker DPI (gorsel indirme)", "warn", "kapali (Ayarlar'dan acilir)"));
        }

        // Termux (yamalayici icin)
        try {
            ctx.getPackageManager().getPackageInfo("com.termux", 0);
            r.add(row("Termux (yamalayici)", "ok", "kurulu"));
        } catch (Throwable t) {
            r.add(row("Termux (yamalayici)", "warn", "kurulu degil, yamalama kullanilamaz"));
        }
        return r;
    }
}
