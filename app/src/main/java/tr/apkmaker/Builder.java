package tr.apkmaker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Builder {
    private static final String TOOLS_VER = "2";
    static File tc;
    static File tools;

    public interface Cb {
        void done(boolean z, String str);

        void log(String str);

        void status(String str);
    }

    public static synchronized boolean ensureTools(Context context, Cb cb) {
        tools = new File(context.getFilesDir(), "tools");
        tc = new File(tools, "toolchain");
        File ok = new File(tools, ".ok");
        if (ok.exists()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(ok));
                String v = r.readLine();
                r.close();
                if (TOOLS_VER.equals(v == null ? null : v.trim())) {
                    return true;
                }
            } catch (Exception e) {
            }
            cb.status(L.t("Toolchain guncelleniyor, eski siliniyor..."));
            deleteDir(tools);
        } else if (tools.exists()) {
            // yarim kalmis kurulum: temiz basla
            deleteDir(tools);
        }
        try {
            cb.status(L.t("Araclar cikartiliyor (ilk sefer 1-2 dk)..."));
            tools.mkdirs();
            TarGz.extract(context.getAssets().open("toolchain.dat"), tools);
            FileWriter w = new FileWriter(ok);
            w.write(TOOLS_VER);
            w.close();
            cb.status(L.t("Araclar hazir"));
            return true;
        } catch (Exception e) {
            cb.log("HATA: " + e);
            return false;
        }
    }

    private static void deleteDir(File file) {
        File[] listFiles;
        if (file != null && file.exists()) {
            if (file.isDirectory() && (listFiles = file.listFiles()) != null) {
                for (File file2 : listFiles) {
                    deleteDir(file2);
                }
            }
            file.delete();
        }
    }

    public static void testTools(final Context context, final Cb cb) {
        new Thread(new Runnable() { // from class: tr.apkmaker.Builder.1
            @Override // java.lang.Runnable
            public void run() {
                if (Builder.ensureTools(context, cb)) {
                    Builder.runCmd(cb, Builder.tools, new String[]{"sh", "-c", "echo TOOLS:; ls " + Builder.tc.getAbsolutePath() + "; echo LIB:; ls -l " + new File(Builder.tc, "lib").getAbsolutePath() + " 2>&1"}, new String[0]);
                    cb.log("--- aapt (saf rpath testi) ---");
                    Builder.runCmd(cb, Builder.tools, new String[]{new File(Builder.tc, "bin/aapt").getAbsolutePath(), "version"}, new String[0]);
                    String firstJar = Builder.firstJar("ecj");
                    if (firstJar != null) {
                        Builder.runCmd(cb, Builder.tools, new String[]{"dalvikvm", "-Xcompiler-option", "--compiler-filter=speed", "-Xmx256m", "-cp", firstJar, "org.eclipse.jdt.internal.compiler.batch.Main"}, new String[0]);
                    }
                    cb.done(true, null);
                    return;
                }
                cb.done(false, null);
            }
        }).start();
    }

    public static void buildWeb(Context context, String str, String str2, boolean z, File file, Cb cb) {
        buildWeb(context, str, str2, z, false, "", false, file, cb);
    }

    /** bye = true: ByeDPI (ciadpi) uygulamanin icine gomulur, trafik yerel SOCKS5 uzerinden gecer. */
    public static void buildWeb(Context context, String str, String str2, boolean z, boolean bye, String byeArgs, File file, Cb cb) {
        buildWeb(context, str, str2, z, bye, byeArgs, false, file, cb);
    }

    /** vid = true: sitedeki video/ses icin "indir" dugmeleri eklenir. */
    public static void buildWeb(final Context context, final String str, final String str2, final boolean z, final boolean bye, final String byeArgs, final boolean vid, final File file, final Cb cb) {
        new Thread(new Runnable() { // from class: tr.apkmaker.Builder.2
            @Override // java.lang.Runnable
            public void run() {
                if (Builder.ensureTools(context, cb)) {
                    try {
                        String pkgFor = Reg.pkgFor(context, str2, "w");
                        Builder.cleanBuilds(context);
                        File file2 = new File(context.getFilesDir(), "build/" + System.currentTimeMillis());
                        File icon = file;
                        if (icon == null) {
                            icon = Builder.fetchFavicon(context, str, cb);
                        }
                        int dpiPort = 0;
                        String dpiArgs = "";
                        if (bye) {
                            if (!Builder.hasAsset(context, "ciadpi.bin")) {
                                Builder.fail(cb, "assets/ciadpi.bin yok: Termux'ta derledigin ciadpi dosyasini bu adla assets klasorune koy.");
                                return;
                            }
                            dpiPort = Reg.nextPort(context);
                            dpiArgs = (byeArgs == null || byeArgs.trim().length() == 0) ? "--disorder 1 --tlsrec 1+s" : byeArgs.trim();
                            cb.log("ByeDPI: port " + dpiPort + ", arguman: " + dpiArgs);
                        }
                        String tpl = Builder.readAsset(context, "template/MainWeb.java").replace("__URL__", Esc.java(str)).replace("__DESKTOP__", z ? "true" : "false").replace("__DPIPORT__", String.valueOf(dpiPort)).replace("__DPIARGS__", Esc.java(dpiArgs)).replace("__DPI__", bye ? "true" : "false").replace("__VIDEODL__", vid ? "true" : "false");
                        Builder.writeProject(context, file2, pkgFor, str2, tpl, icon);
                        if (bye) {
                            File so = new File(file2, "lib/arm64-v8a/libciadpi.so");
                            so.getParentFile().mkdirs();
                            InputStream in = context.getAssets().open("ciadpi.bin");
                            copyStream(in, new FileOutputStream(so));
                            in.close();
                        }
                        Builder.compile(context, file2, str2, pkgFor, cb);
                        return;
                    } catch (Exception e) {
                        cb.log("HATA: " + e);
                        cb.done(false, null);
                        return;
                    }
                }
                cb.done(false, null);
            }
        }).start();
    }

    public static void buildHtml(Context context, String str, String str2, File file, Cb cb) {
        buildHtml(context, str, str2, false, "", file, cb);
    }

    public static void buildHtml(final Context context, final String str, final String str2, final boolean bye, final String byeArgs, final File file, final Cb cb) {
        new Thread(new Runnable() { // from class: tr.apkmaker.Builder.3
            @Override // java.lang.Runnable
            public void run() {
                if (Builder.ensureTools(context, cb)) {
                    try {
                        String pkgFor = Reg.pkgFor(context, str, "h");
                        Builder.cleanBuilds(context);
                        File file2 = new File(context.getFilesDir(), "build/" + System.currentTimeMillis());
                        Builder.writeProject(context, file2, pkgFor, str, Builder.readAsset(context, "template/MainHtml.java").replace("__ENTRY__", "index.html"), file);
                        File file3 = new File(file2, "assets/www");
                        file3.mkdirs();
                        Builder.write(new File(file3, "index.html"), str2);
                        if (bye) Builder.addDpi(context, file2, pkgFor, byeArgs);
                        Builder.compile(context, file2, str, pkgFor, cb);
                        return;
                    } catch (Exception e) {
                        cb.log("HATA: " + e);
                        cb.done(false, null);
                        return;
                    }
                }
                cb.done(false, null);
            }
        }).start();
    }

    public static void buildCode(Context context, String str, String str2, File file, Cb cb) {
        buildCode(context, str, str2, false, "", file, cb);
    }

    public static void buildCode(final Context context, final String str, final String str2, final boolean bye, final String byeArgs, final File file, final Cb cb) {
        new Thread(new Runnable() { // from class: tr.apkmaker.Builder.4
            @Override // java.lang.Runnable
            public void run() {
                if (Builder.ensureTools(context, cb)) {
                    try {
                        String pkgFor = Reg.pkgFor(context, str, "k");
                        Builder.cleanBuilds(context);
                        File file2 = new File(context.getFilesDir(), "build/" + System.currentTimeMillis());
                        Builder.writeProject(context, file2, pkgFor, str, null, file);
                        File file3 = new File(file2, "src/" + pkgFor.replace('.', '/'));
                        file3.mkdirs();
                        ArrayList splitFiles = Builder.splitFiles(str2);
                        for (int i = 0; i < splitFiles.size(); i++) {
                            String str3 = ((String[]) splitFiles.get(i))[1];
                            Builder.write(new File(file3, ((String[]) splitFiles.get(i))[0]), Builder.fixPackage(str3, pkgFor));
                        }
                        if (bye) Builder.addDpi(context, file2, pkgFor, byeArgs);
                        Builder.compile(context, file2, str, pkgFor, cb);
                        return;
                    } catch (Exception e) {
                        cb.log("HATA: " + e);
                        cb.done(false, null);
                        return;
                    }
                }
                cb.done(false, null);
            }
        }).start();
    }

    private static final java.util.regex.Pattern PKG_RE = java.util.regex.Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    // Kullanicinin package satiri varsa uygulamanin paketiyle degistirir, yoksa ekler
    public static String fixPackage(String code, String pkg) {
        java.util.regex.Matcher m = PKG_RE.matcher(code);
        if (m.find()) {
            String old = m.group(1);
            String res = code.substring(0, m.start()) + "package " + pkg + ";" + code.substring(m.end());
            if (!old.equals(pkg)) {
                res = res.replaceAll("(?m)^(\\s*import\\s+(?:static\\s+)?)" + java.util.regex.Pattern.quote(old) + "\\.", "$1" + java.util.regex.Matcher.quoteReplacement(pkg) + ".");
            }
            return res;
        }
        return "package " + pkg + ";\n" + code;
    }

    public static String safeName(String n) {
        n = new File(n).getName().replaceAll("[^A-Za-z0-9_.$-]", "_");
        if (!n.endsWith(".java")) {
            n = n + ".java";
        }
        return n;
    }

    public static ArrayList<String[]> splitFiles(String str) {
        String str2;
        ArrayList<String[]> arrayList = new ArrayList<>();
        String[] split = str.split("\n");
        String str3 = null;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < split.length) {
            String trim = split[i].trim();
            if (trim.startsWith("// FILE:")) {
                if (str3 != null && sb.toString().trim().length() > 0) {
                    arrayList.add(new String[]{str3, sb.toString()});
                }
                str2 = safeName(trim.substring(8).trim());
                sb = new StringBuilder();
            } else {
                if (str3 == null) {
                    str3 = "MainActivity.java";
                }
                sb.append(split[i]).append('\n');
                str2 = str3;
            }
            i++;
            str3 = str2;
        }
        if (str3 != null) {
            arrayList.add(new String[]{str3, sb.toString()});
        }
        return arrayList;
    }

    public static void writeProject(Context context, File file, String str, String str2, String str3, File file2) throws Exception {
        String replace = readAsset(context, "template/AndroidManifest.xml").replace("__PKG__", str).replace("__APPNAME__", Esc.xml(str2)).replace("__ICON__", file2 != null ? "@drawable/icon" : "@drawable/ic_app").replace("__VCODE__", String.valueOf(Reg.nextVersion(context, str2))).replace("__VNAME__", Reg.versionName(Reg.nextVersion(context, str2)));
        File file3 = new File(file, "src/" + str.replace('.', '/'));
        file3.mkdirs();
        new File(file, "res/drawable").mkdirs();
        new File(file, "obj").mkdirs();
        write(new File(file, "AndroidManifest.xml"), replace);
        if (str3 != null) {
            write(new File(file3, "MainActivity.java"), str3.replace("__PKG__", str));
        }
        write(new File(file, "res/drawable/ic_app.xml"), readAsset(context, "template/ic_app.xml"));
        if (file2 != null) {
            copyStream(new FileInputStream(file2), new FileOutputStream(new File(file, "res/drawable/icon.png")));
        }
    }

    public static void compile(Context context, File file, String str, String str2, Cb cb) throws Exception {
        String absolutePath = new File(tc, "bin/aapt").getAbsolutePath();
        String absolutePath2 = new File(tc, "lib").getAbsolutePath();
        String absolutePath3 = new File(tc, "share/android.jar").getAbsolutePath();
        String firstJar = firstJar("ecj");
        String firstJar2 = firstJar("dx");
        String joinJars = joinJars("apksigner");
        if (firstJar == null || firstJar2 == null || joinJars.length() == 0) {
            cb.log("HATA: toolchain jar bulunamadi");
            cb.done(false, null);
            return;
        }
        ArrayList arrayList = new ArrayList();
        collectJava(new File(file, "src"), arrayList);
        cb.status(L.t("aapt: paketleniyor"));
        ArrayList arrayList2 = new ArrayList(Arrays.asList(absolutePath, "package", "-f", "-M", "AndroidManifest.xml", "-I", "/system/framework/framework-res.apk", "--ignore-assets", "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~", "-S", "res", "-F", "unsigned.apk"));
        if (new File(file, "assets").exists()) {
            arrayList2.add("-A");
            arrayList2.add("assets");
        }
        int runCmd = runCmd(cb, file, (String[]) arrayList2.toArray(new String[0]), "LD_LIBRARY_PATH=" + absolutePath2);
        if (runCmd == 0) {
            cb.status(L.t("ecj: derleniyor (in-proc)"));
            ArrayList arrayList3 = new ArrayList();
            arrayList3.add("-proc:none");
            arrayList3.add("-encoding");
            arrayList3.add("UTF-8");
            arrayList3.add("-7");
            arrayList3.add("-d");
            // ecj ayni surecte (in-proc) calisir: goreli "obj" surecin cwd'sine ("/") cozulur, mutlak yol sart
            arrayList3.add(new File(file, "obj").getAbsolutePath());
            arrayList3.add("-classpath");
            arrayList3.add(absolutePath3);
            arrayList3.addAll(arrayList);
            if (InProc.compileEcj(firstJar, (String[]) arrayList3.toArray(new String[0]), cb)) {
                String[] objFiles = new File(file, "obj").list();
                if (objFiles == null || objFiles.length == 0) {
                    fail(cb, L.t("ecj .class uretmedi (obj bos)"));
                    return;
                }
                cb.status(L.t("dx: dexleniyor"));
                // dalvikvm alt sureci bazi cihazlarda 134 (SIGABRT) ile coktugu icin dx ve imza uygulama icinde (in-proc) calisir
                int runCmd2 = InProc.dex(firstJar2, new File(file, "classes.dex").getAbsolutePath(), new File(file, "obj").getAbsolutePath(), cb) ? 0 : 1;
                if (runCmd2 == 0) {
                    int runCmd4 = runCmd(cb, file, new String[]{absolutePath, "add", "-f", "unsigned.apk", "classes.dex"}, "LD_LIBRARY_PATH=" + absolutePath2);
                    if (runCmd4 != 0) {
                        fail(cb, "aapt add hata " + runCmd4);
                        return;
                    }
                    if (new File(file, "lib/arm64-v8a/libciadpi.so").exists()) {
                        int runCmd5 = runCmd(cb, file, new String[]{absolutePath, "add", "-f", "unsigned.apk", "lib/arm64-v8a/libciadpi.so"}, "LD_LIBRARY_PATH=" + absolutePath2);
                        if (runCmd5 != 0) {
                            fail(cb, "aapt add (ciadpi) hata " + runCmd5);
                            return;
                        }
                    }
                    cb.status(L.t("ozel anahtar + imza"));
                    File file2 = new File(context.getFilesDir(), "keys");
                    file2.mkdirs();
                    File file3 = new File(file2, String.valueOf(str2) + ".keystore");
                    CertGen.ensureKey(file3, str);
                    int runCmd3 = InProc.sign(joinJars, file3, new File(file, "unsigned.apk"), new File(file, "signed.apk"), cb) ? 0 : 1;
                    if (runCmd3 == 0) {
                        String saved = saveApk(context, new File(file, "signed.apk"), str.replaceAll("[^A-Za-z0-9 _-]", "_") + ".apk", cb);
                        if (saved == null) {
                            fail(cb, L.t("APK kaydedilemedi (depolama izni?)"));
                            return;
                        }
                        Reg.record(context, str, str2, new File(file, "res/drawable/icon.png"));
                        cb.status("OK");
                        cb.done(true, saved);
                        return;
                    }
                    fail(cb, "imza hata " + runCmd3);
                    return;
                }
                fail(cb, "dx hata " + runCmd2);
                return;
            }
            fail(cb, L.t("ecj derleme hatasi"));
            return;
        }
        fail(cb, "aapt hata " + runCmd);
    }

    // Once /sdcard/Download, olmazsa MediaStore (Android 10+), o da olmazsa uygulamanin kendi dis klasoru
    private static String saveApk(Context context, File src, String name, Cb cb) {
        try {
            File dir = new File("/sdcard/Download");
            dir.mkdirs();
            File out = new File(dir, name);
            copyStream(new FileInputStream(src), new FileOutputStream(out));
            try {
                Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                intent.setData(Uri.fromFile(out));
                context.sendBroadcast(intent);
            } catch (Exception e) {
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            cb.log("Download klasorune yazilamadi: " + e);
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues v = new android.content.ContentValues();
                v.put("_display_name", name);
                v.put("mime_type", "application/vnd.android.package-archive");
                v.put("relative_path", "Download");
                Uri uri = context.getContentResolver().insert(Uri.parse("content://media/external/downloads"), v);
                if (uri != null) {
                    copyStream(new FileInputStream(src), context.getContentResolver().openOutputStream(uri));
                    return "Download/" + name;
                }
            }
        } catch (Exception e) {
            cb.log("MediaStore kaydi basarisiz: " + e);
        }
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                dir.mkdirs();
                File out = new File(dir, name);
                copyStream(new FileInputStream(src), new FileOutputStream(out));
                return out.getAbsolutePath();
            }
        } catch (Exception e) {
            cb.log("Dis klasore yazilamadi: " + e);
        }
        return null;
    }

    // ---- Site simgesi (favicon) ----
    private static final String UA = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static byte[] httpBytes(String url, int max) {
        try {
            String cur = url;
            for (int hop = 0; hop < 6; hop++) {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(cur).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent", UA);
                int code = c.getResponseCode();
                if (code >= 300 && code < 400) {
                    String loc = c.getHeaderField("Location");
                    c.disconnect();
                    if (loc == null) {
                        return null;
                    }
                    cur = new java.net.URL(new java.net.URL(cur), loc).toString();
                    continue;
                }
                if (code != 200) {
                    c.disconnect();
                    return null;
                }
                InputStream in = c.getInputStream();
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0 && o.size() < max) {
                    o.write(buf, 0, n);
                }
                in.close();
                c.disconnect();
                return o.toByteArray();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static String attr(String tag, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b" + name + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(tag);
        if (!m.find()) {
            return null;
        }
        return m.group(1) != null ? m.group(1) : (m.group(2) != null ? m.group(2) : m.group(3));
    }

    // Sitenin simgesini bulup 192px PNG olarak files/favicon.png'ye yazar; bulamazsa null (varsayilan ikon kullanilir)
    public static File fetchFavicon(Context context, String pageUrl, Cb cb) {
        if (SelfDpi.ensure(context)) {
            cb.log("Gorsel indirme: ByeDPI uzerinden");
        }
        try {
            cb.log(L.t("Site simgesi araniyor..."));
            java.net.URL base = new java.net.URL(pageUrl);
            final ArrayList<String> urls = new ArrayList<String>();
            final ArrayList<Integer> scores = new ArrayList<Integer>();
            byte[] page = httpBytes(pageUrl, 1500000);
            if (page != null) {
                String html = new String(page, "UTF-8");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<link\\b[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
                while (m.find()) {
                    String tag = m.group();
                    String rel = attr(tag, "rel");
                    String href = attr(tag, "href");
                    if (rel == null || href == null || rel.toLowerCase().indexOf("icon") < 0) {
                        continue;
                    }
                    if (href.toLowerCase().split("[?#]")[0].endsWith(".svg")) {
                        continue;
                    }
                    int score = rel.toLowerCase().indexOf("apple-touch") >= 0 ? 1000 : 0;
                    String sizes = attr(tag, "sizes");
                    if (sizes != null) {
                        java.util.regex.Matcher sm = java.util.regex.Pattern.compile("(\\d+)\\s*x\\s*\\d+").matcher(sizes);
                        while (sm.find()) {
                            score += Math.min(Integer.parseInt(sm.group(1)), 512);
                        }
                    }
                    try {
                        String abs = new java.net.URL(base, href.trim()).toString();
                        int at = 0;
                        while (at < scores.size() && scores.get(at).intValue() >= score) {
                            at++;
                        }
                        urls.add(at, abs);
                        scores.add(at, Integer.valueOf(score));
                    } catch (Exception e) {
                    }
                }
            }
            urls.add(new java.net.URL(base, "/favicon.ico").toString());
            urls.add("https://www.google.com/s2/favicons?domain=" + base.getHost() + "&sz=128");
            android.graphics.Bitmap best = null;
            for (int i = 0; i < urls.size(); i++) {
                byte[] d = httpBytes(urls.get(i), 3000000);
                if (d == null || d.length == 0) {
                    continue;
                }
                android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(d, 0, d.length);
                if (bm == null || bm.getWidth() < 16) {
                    continue;
                }
                if (best == null || bm.getWidth() > best.getWidth()) {
                    best = bm;
                }
                if (best.getWidth() >= 96) {
                    break;
                }
            }
            if (best == null) {
                cb.log(L.t("Site simgesi bulunamadi, varsayilan ikon kullanilacak"));
                return null;
            }
            int w = best.getWidth();
            int h = best.getHeight();
            float f = Math.min(1.0f, 192.0f / Math.max(w, h));
            if (f < 1.0f) {
                best = android.graphics.Bitmap.createScaledBitmap(best, Math.max(1, (int) (w * f)), Math.max(1, (int) (h * f)), true);
            }
            File out = new File(context.getFilesDir(), "favicon.png");
            FileOutputStream fo = new FileOutputStream(out);
            best.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo);
            fo.close();
            cb.log("Site simgesi alindi (" + best.getWidth() + "x" + best.getHeight() + ")");
            return out;
        } catch (Throwable e) {
            cb.log("Simge alinamadi: " + e);
            return null;
        }
    }

    private static final java.util.regex.Pattern PUB = java.util.regex.Pattern.compile("(?m)^\\s*public\\s+(?:final\\s+|abstract\\s+)*(?:class|interface|enum)\\s+(\\w+)");

    private static void copyTree(File from, File to) throws Exception {
        File[] l = from.listFiles();
        if (l == null) {
            return;
        }
        to.mkdirs();
        for (File f : l) {
            if (f.getName().startsWith(".") || f.getName().equals("__MACOSX")) {
                continue;
            }
            if (f.isDirectory()) {
                copyTree(f, new File(to, f.getName()));
            } else {
                copyStream(new FileInputStream(f), new FileOutputStream(new File(to, f.getName())));
            }
        }
    }

    /** Detect.scan sonucundan (Java veya HTML5) APK uretir */
    public static void buildProject(Context context, String name, Detect.Result d, File icon, Cb cb) {
        buildProject(context, name, d, false, "", icon, cb);
    }

    public static void buildProject(final Context context, final String name, final Detect.Result d, final boolean bye, final String byeArgs, final File icon, final Cb cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!Builder.ensureTools(context, cb)) {
                    cb.done(false, null);
                    return;
                }
                try {
                    String pkg = Reg.pkgFor(context, name, d.java ? "k" : "h");
                    Builder.cleanBuilds(context);
                    File p = new File(context.getFilesDir(), "build/" + System.currentTimeMillis());
                    if (d.java) {
                        Builder.writeProject(context, p, pkg, name, null, icon);
                        File srcDir = new File(p, "src/" + pkg.replace('.', '/'));
                        srcDir.mkdirs();
                        for (File f : d.javaFiles) {
                            String code = Detect.readText(f);
                            boolean marked = code.contains("// FILE:");
                            ArrayList<String[]> parts = Builder.splitFiles(code);
                            for (int i = 0; i < parts.size(); i++) {
                                String fn = parts.get(i)[0];
                                String body = parts.get(i)[1];
                                java.util.regex.Matcher m = PUB.matcher(body);
                                if (m.find()) {
                                    fn = m.group(1) + ".java";
                                } else if (!marked) {
                                    fn = Builder.safeName(f.getName());
                                }
                                Builder.write(new File(srcDir, fn), Builder.fixPackage(body, pkg));
                            }
                        }
                    } else {
                        String entry = android.net.Uri.encode(d.entry, "/");
                        Builder.writeProject(context, p, pkg, name, Builder.readAsset(context, "template/MainHtml.java").replace("__ENTRY__", entry), icon);
                        Builder.copyTree(d.root, new File(p, "assets/www"));
                    }
                    if (bye) Builder.addDpi(context, p, pkg, byeArgs);
                    Builder.compile(context, p, name, pkg, cb);
                } catch (Exception e) {
                    cb.log("HATA: " + e);
                    cb.done(false, null);
                }
            }
        }).start();
    }

    /** Resmi ortadan kare kirpar, en fazla 192x192 yapar */
    public static android.graphics.Bitmap squareIcon(android.graphics.Bitmap b) {
        int w = b.getWidth();
        int h = b.getHeight();
        int s = Math.min(w, h);
        android.graphics.Bitmap c = android.graphics.Bitmap.createBitmap(b, (w - s) / 2, (h - s) / 2, s, s);
        if (s > 192) {
            c = android.graphics.Bitmap.createScaledBitmap(c, 192, 192, true);
        }
        return c;
    }

    // Eski derleme klasorleri birikmesin
    private static void cleanBuilds(Context context) {
        deleteDir(new File(context.getFilesDir(), "build"));
    }

    private static void fail(Cb cb, String str) {
        cb.log(str);
        cb.done(false, null);
    }

    public static int runCmd(Cb cb, File file, String[] strArr, String... strArr2) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(strArr);
            processBuilder.directory(file);
            processBuilder.redirectErrorStream(true);
            for (String str : strArr2) {
                int indexOf = str.indexOf(61);
                processBuilder.environment().put(str.substring(0, indexOf), processBuilder.environment().get(str.substring(0, indexOf)) == null ? str.substring(indexOf + 1) : str.substring(indexOf + 1));
            }
            Process start = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(start.getInputStream()));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    cb.log(readLine);
                } else {
                    return start.waitFor();
                }
            }
        } catch (Exception e) {
            cb.log("HATA: " + e);
            return -1;
        }
    }

    public static String firstJar(String str) {
        File[] listFiles = new File(tc, "share/" + str).listFiles();
        if (listFiles == null) {
            return null;
        }
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].getName().endsWith(".jar")) {
                return listFiles[i].getAbsolutePath();
            }
        }
        return null;
    }

    private static String joinJars(String str) {
        File[] listFiles = new File(tc, "share/" + str).listFiles();
        if (listFiles == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].getName().endsWith(".jar")) {
                if (sb.length() > 0) {
                    sb.append(':');
                }
                sb.append(listFiles[i].getAbsolutePath());
            }
        }
        return sb.toString();
    }

    private static void collectJava(File file, ArrayList<String> arrayList) {
        File[] listFiles = file.listFiles();
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                if (listFiles[i].isDirectory()) {
                    collectJava(listFiles[i], arrayList);
                } else if (listFiles[i].getName().endsWith(".java")) {
                    arrayList.add(listFiles[i].getAbsolutePath());
                }
            }
        }
    }

    public static void write(File file, String str) throws Exception {
        java.io.OutputStreamWriter fileWriter = new java.io.OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        fileWriter.write(str);
        fileWriter.close();
    }

    /** Kod/HTML/dosya uretimlerine ByeDPI ekler: dpi.Boot + dpi.App, assets/dpi.cfg, libciadpi.so, manifest android:name. */
    public static void addDpi(Context context, File proj, String pkg, String args) throws Exception {
        if (!hasAsset(context, "ciadpi.bin")) {
            throw new Exception("assets/ciadpi.bin yok: Termux'ta derledigin ciadpi dosyasini bu adla assets klasorune koy.");
        }
        int port = Reg.nextPort(context);
        String a = (args == null || args.trim().length() == 0) ? "--disorder 1 --tlsrec 1+s" : args.trim();
        File dpiDir = new File(proj, "src/dpi");
        dpiDir.mkdirs();
        write(new File(dpiDir, "Boot.java"), readAsset(context, "template/DpiBoot.java"));
        write(new File(dpiDir, "App.java"), readAsset(context, "template/DpiApp.java"));
        File as = new File(proj, "assets");
        as.mkdirs();
        write(new File(as, "dpi.cfg"), "port=" + port + "\nargs=" + a + "\n");
        File so = new File(proj, "lib/arm64-v8a/libciadpi.so");
        so.getParentFile().mkdirs();
        InputStream in = context.getAssets().open("ciadpi.bin");
        copyStream(in, new FileOutputStream(so));
        File man = new File(proj, "AndroidManifest.xml");
        String m = new String(readAll(man), "UTF-8");
        write(man, m.replace("<application ", "<application android:name=\"dpi.App\" "));
        File act = new File(proj, "src/" + pkg.replace('.', '/') + "/MainActivity.java");
        if (act.exists()) {
            String t = new String(readAll(act), "UTF-8");
            if (t.contains("// DPI_HOOK_MARKER")) {
                write(act, t.replace("// DPI_HOOK_MARKER", "@Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView v, android.webkit.WebResourceRequest r) { return dpi.Boot.intercept(r); }"));
            }
        }
        cbLog("ByeDPI: port " + port + ", arguman: " + a);
    }

    private static void cbLog(String s) {
    }

    private static byte[] readAll(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n);
        in.close();
        return o.toByteArray();
    }

    public static boolean hasAsset(Context context, String name) {
        try {
            InputStream in = context.getAssets().open(name);
            in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String readAsset(Context context, String str) throws Exception {
        InputStream open = context.getAssets().open(str);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        while (true) {
            int read = open.read(bArr);
            if (read > 0) {
                byteArrayOutputStream.write(bArr, 0, read);
            } else {
                open.close();
                return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
            }
        }
    }

    static void copyStream(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] bArr = new byte[8192];
        while (true) {
            int read = inputStream.read(bArr);
            if (read > 0) {
                outputStream.write(bArr, 0, read);
            } else {
                inputStream.close();
                outputStream.close();
                return;
            }
        }
    }
}
