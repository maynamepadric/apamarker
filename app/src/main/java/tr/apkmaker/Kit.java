package tr.apkmaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Termux yamalayici kiti: Download/APKMakerKit */
public class Kit {
    public static File dir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "APKMakerKit");
    }

    public static File inDir() {
        return new File(dir(), "in");
    }

    private static void asset(Context c, String name, File dst) throws Exception {
        InputStream in = c.getAssets().open(name);
        dst.getParentFile().mkdirs();
        Builder.copyStream(in, new FileOutputStream(dst));
    }

    /** Kiti hazirlar. Hata olursa istisna firlatir. */
    public static void export(Context c) throws Exception {
        File d = dir();
        d.mkdirs();
        asset(c, "kit/patch.sh", new File(d, "patch.sh"));
        asset(c, "kit/setup.sh", new File(d, "setup.sh"));
        asset(c, "template/DpiBoot.java", new File(d, "DpiBoot.java"));
        asset(c, "template/DpiApp.java", new File(d, "DpiApp.java"));
        asset(c, "ciadpi.bin", new File(d, "ciadpi"));
        if (!Builder.ensureTools(c, new Builder.Cb() {
            public void log(String s) { }
            public void status(String s) { }
            public void done(boolean ok, String p) { }
        })) {
            throw new Exception("arac seti hazirlanamadi");
        }
        Builder.copyStream(new java.io.FileInputStream(new File(Builder.tc, "share/android.jar")), new FileOutputStream(new File(d, "android.jar")));
    }

    public static String setupCommand() {
        return "bash '" + new File(dir(), "setup.sh").getAbsolutePath() + "'";
    }

    public static String[] argv(String input, int port, String args) {
        return new String[]{new File(dir(), "patch.sh").getAbsolutePath(), input, String.valueOf(port), args};
    }

    public static String command(String input, int port, String args) {
        return "bash '" + new File(dir(), "patch.sh").getAbsolutePath() + "' '" + input + "' " + port + " '" + args + "'";
    }

    /** Termux RUN_COMMAND ile calistirir. Basarisizsa istisna firlatir. */
    public static void runInTermux(Activity a, String[] argv) {
        Intent i = new Intent("com.termux.RUN_COMMAND");
        i.setClassName("com.termux", "com.termux.app.RunCommandService");
        i.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", argv);
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        a.startService(i);
    }
}
