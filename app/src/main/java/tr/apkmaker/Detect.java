package tr.apkmaker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Secilen dosyalarin turunu (Java / HTML5 / zip / txt) otomatik algilar. */
public class Detect {
    public static class Result {
        public boolean java;
        public List<File> javaFiles = new ArrayList<File>();
        public File root;       // HTML5: assets/www'ye kopyalanacak klasor
        public String entry;    // HTML5: giris dosyasi (root'a gore)
        public String name = "App";
        public String summary = "";
        public String error;    // null degilse uretilemez
    }

    public static void clear(File f) {
        File[] l = f.listFiles();
        if (l != null) {
            for (File c : l) {
                clear(c);
            }
        }
        f.delete();
    }

    static String ext(File f) {
        String n = f.getName().toLowerCase(java.util.Locale.ENGLISH);
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1);
    }

    public static String readText(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            o.write(b, 0, n);
        }
        in.close();
        return new String(o.toByteArray(), "UTF-8");
    }

    static void walk(File d, List<File> out) {
        File[] l = d.listFiles();
        if (l == null) {
            return;
        }
        for (File f : l) {
            if (f.getName().startsWith(".") || f.getName().equals("__MACOSX")) {
                continue;
            }
            if (f.isDirectory()) {
                walk(f, out);
            } else {
                out.add(f);
            }
        }
    }

    // zip dosyalarini yanina acar (zip-slip korumali)
    static String expandZips(File dir) throws Exception {
        List<File> all = new ArrayList<File>();
        walk(dir, all);
        String firstZip = null;
        for (File z : all) {
            if (!ext(z).equals("zip")) {
                continue;
            }
            String base = z.getName().substring(0, z.getName().length() - 4);
            if (firstZip == null) {
                firstZip = base;
            }
            File out = new File(z.getParentFile(), base + "_zip");
            out.mkdirs();
            String root = out.getCanonicalPath() + File.separator;
            ZipFile zf = new ZipFile(z);
            try {
                Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    File t = new File(out, e.getName());
                    if (!t.getCanonicalPath().startsWith(root)) {
                        continue;
                    }
                    if (e.isDirectory()) {
                        t.mkdirs();
                        continue;
                    }
                    t.getParentFile().mkdirs();
                    InputStream in = zf.getInputStream(e);
                    FileOutputStream fo = new FileOutputStream(t);
                    byte[] b = new byte[16384];
                    int n;
                    while ((n = in.read(b)) > 0) {
                        fo.write(b, 0, n);
                    }
                    fo.close();
                    in.close();
                }
            } finally {
                zf.close();
            }
            z.delete();
        }
        return firstZip;
    }

    static int depth(File f, File base) {
        int d = 0;
        for (File p = f.getParentFile(); p != null && !p.equals(base); p = p.getParentFile()) {
            d++;
        }
        return d;
    }

    static String rel(File f, File base) {
        String s = f.getAbsolutePath().substring(base.getAbsolutePath().length());
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    public static Result scan(File dir) throws Exception {
        Result r = new Result();
        String zipName = expandZips(dir);
        List<File> all = new ArrayList<File>();
        walk(dir, all);
        List<File> jv = new ArrayList<File>();
        List<File> html = new ArrayList<File>();
        boolean studio = false;
        for (File f : all) {
            String path = f.getAbsolutePath();
            String e = ext(f);
            if (f.getName().equals("AndroidManifest.xml")) {
                studio = true;
            }
            if (path.contains("/test/") || path.contains("/androidTest/")) {
                continue;
            }
            if (e.equals("java")) {
                jv.add(f);
            } else if (e.equals("html") || e.equals("htm")) {
                html.add(f);
            } else if ((e.equals("txt") || e.length() == 0) && f.length() < 3000000) {
                // icerige bakarak turunu anla; .java/.html olarak yeniden adlandir
                String t = readText(f);
                String lt = t.toLowerCase(java.util.Locale.ENGLISH);
                if (t.contains("class MainActivity") || t.contains("extends Activity") || t.contains("// FILE:")) {
                    File n = new File(f.getParentFile(), f.getName().replaceAll("\\.[^.]*$", "") + ".java");
                    if (f.renameTo(n)) {
                        jv.add(n);
                    }
                } else if (lt.contains("<html") || lt.contains("<!doctype html") || lt.contains("<body")) {
                    File n = new File(f.getParentFile(), f.getName().replaceAll("\\.[^.]*$", "") + ".html");
                    if (f.renameTo(n)) {
                        html.add(n);
                    }
                }
            }
        }
        boolean hasMain = false;
        for (File f : jv) {
            if (readText(f).contains("class MainActivity")) {
                hasMain = true;
            }
        }
        if (hasMain) {
            r.java = true;
            r.javaFiles = jv;
            r.name = zipName != null ? zipName : "KodApp";
            r.summary = L.f("Algilandi: JAVA projesi ({0} dosya, MainActivity bulundu)", jv.size());
            if (studio) {
                r.summary += "\n" + L.t("Not: manifest ve res/ yoksayilir, yalnizca Java kullanilir");
            }
        } else if (!html.isEmpty()) {
            File entry = null;
            for (File f : html) {
                boolean idx = f.getName().equalsIgnoreCase("index.html");
                if (entry == null) {
                    entry = f;
                    continue;
                }
                boolean eidx = entry.getName().equalsIgnoreCase("index.html");
                int df = depth(f, dir), de = depth(entry, dir);
                if ((idx && !eidx) || (idx == eidx && df < de)) {
                    entry = f;
                }
            }
            r.root = entry.getParentFile();
            r.entry = entry.getName();
            List<File> files = new ArrayList<File>();
            walk(r.root, files);
            r.name = zipName;
            if (r.name == null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(readText(entry));
                if (m.find() && m.group(1).trim().length() > 0) {
                    r.name = m.group(1).trim().replaceAll("\\s+", " ");
                    if (r.name.length() > 30) {
                        r.name = r.name.substring(0, 30);
                    }
                }
            }
            if (r.name == null) {
                r.name = "HtmlApp";
            }
            r.summary = L.f("Algilandi: HTML5 projesi (giris: {0}, toplam {1} dosya)", r.entry, files.size());
        } else if (!jv.isEmpty()) {
            r.error = L.t(L.t("Java dosyasi var ama 'class MainActivity' bulunamadi"));
            r.summary = r.error;
        } else {
            r.error = L.t(L.t("Taninan dosya yok (.java, .html, .zip veya icinde kod olan .txt sec)"));
            r.summary = r.error;
        }
        return r;
    }
}
