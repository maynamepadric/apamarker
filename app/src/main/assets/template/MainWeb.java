package __PKG__;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private WebView web;
    private Button backBtn;
    private Button fwdBtn;
    private Button modeBtn;
    private LinearLayout nav;
    private View custom;
    private WebChromeClient.CustomViewCallback customCb;
    private ValueCallback<Uri[]> fileCb;
    private GeolocationPermissions.Callback geoCb;
    private PermissionRequest micReq;
    private String pdUrl, pdUa, pdCd, pdMime;
    private String geoOrigin;
    private String START = "__URL__";
    private static final boolean DPI = __DPI__;
    private static final boolean VIDEODL = __VIDEODL__;
    private static final String VIDEO_JS =
        "(function(){\n" +
        "if(window.__apkdl)return;window.__apkdl=1;\n" +
        "var force=false;\n" +
        "var b=document.createElement('button');b.textContent='Indir';\n" +
        "b.style.cssText='position:fixed;right:10px;bottom:90px;z-index:2147483647;display:none;border:0;border-radius:22px;padding:11px 16px;font:600 14px sans-serif;color:#fff;background:rgba(20,20,20,.88);box-shadow:0 2px 8px rgba(0,0,0,.5)';\n" +
        "function urls(){var vs=document.querySelectorAll('video,audio'),r=[];\n" +
        "for(var i=0;i<vs.length;i++){var v=vs[i],s=v.currentSrc||v.src;if(s&&/^https?:/i.test(s))r.push(s);\n" +
        "var ss=v.querySelectorAll('source');for(var j=0;j<ss.length;j++){if(ss[j].src&&/^https?:/i.test(ss[j].src))r.push(ss[j].src);}}\n" +
        "return r;}\n" +
        "b.onclick=function(e){e.stopPropagation();e.preventDefault();ApkBridge.pick(JSON.stringify(urls()),document.title||'',location.href,navigator.userAgent);};\n" +
        "(document.body||document.documentElement).appendChild(b);\n" +
        "window.__apkShow=function(){force=true;};\n" +
        "setInterval(function(){b.style.display=(force||document.querySelector('video,audio'))?'block':'none';},1500);\n" +
        "})();\n";
    private static final int DPI_PORT = __DPIPORT__;
    private static final String DPI_ARGS = "__DPIARGS__";
    private volatile int proxyPort = -1;
    private Process dpiProc;
    private boolean defaultDesktop = __DESKTOP__;
    private SharedPreferences cfg;

    private static final String DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = getSharedPreferences("cfg", 0);
        web = new WebView(this);
        float d = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Ust arac cubugu: geri | ileri | yenile | mod  (esit aralikli, sayfanin ustunde, icerigi kapatmaz)
        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackgroundColor(0xFF202124);
        backBtn = navBtn("\u25C0");
        fwdBtn = navBtn("\u25B6");
        Button ref = navBtn("\u21BB");
        modeBtn = navBtn("");
        modeBtn.setTextSize(14);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (web.canGoBack()) web.goBack(); }
        });
        fwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (web.canGoForward()) web.goForward(); }
        });
        ref.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { web.reload(); }
        });
        nav.addView(backBtn, new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(fwdBtn, new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(ref, new LinearLayout.LayoutParams(0, -1, 1f));
        nav.addView(modeBtn, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(nav, new LinearLayout.LayoutParams(-1, (int) (44 * d)));
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        updateNav();

        modeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean nowDesktop = !isDesktop();
                cfg.edit().putBoolean("desktop", nowDesktop).apply();
                applyMode(nowDesktop, true);
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(true);
        s.setGeolocationEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override public void doUpdateVisitedHistory(WebView v, String url, boolean isReload) {
                updateNav();
            }
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                if (VIDEODL) { synchronized (sniffed) { sniffed.clear(); } }
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (!VIDEODL) return;
                boolean any;
                synchronized (sniffed) { any = !sniffed.isEmpty(); }
                v.evaluateJavascript(VIDEO_JS + (any ? "\nwindow.__apkShow&&window.__apkShow();" : ""), null);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (VIDEODL) sniffUrl(r.getUrl().toString());
                if (proxyPort < 0) return null;
                return fetchViaProxy(r);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("http")) { v.loadUrl(url); return true; }
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
                return true;
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) {
                try {
                    String host = Uri.parse(e.getUrl()).getHost();
                    if (host != null && host.equals(Uri.parse(START).getHost())) { h.proceed(); return; }
                } catch (Exception ex) {}
                h.cancel();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback cb) {
                if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) {
                    cb.invoke(o, true, false);
                } else {
                    geoOrigin = o;
                    geoCb = cb;
                    requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 3);
                }
            }
            @Override public void onPermissionRequest(final PermissionRequest r) {
                boolean mic = false;
                for (String s : r.getResources()) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(s)) mic = true;
                }
                if (!mic) { r.deny(); return; }
                if (checkSelfPermission("android.permission.RECORD_AUDIO") == 0) {
                    r.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                } else {
                    micReq = r;
                    requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 4);
                }
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Sec"), 1);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                custom = v;
                customCb = cb;
                nav.setVisibility(View.GONE);
                modeBtn.setVisibility(View.GONE);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                ((FrameLayout) getWindow().getDecorView()).addView(
                    v, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
            }
            @Override public void onHideCustomView() {
                if (custom == null) return;
                ((FrameLayout) getWindow().getDecorView()).removeView(custom);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                custom = null;
                web.setVisibility(View.VISIBLE);
                nav.setVisibility(View.VISIBLE);
                modeBtn.setVisibility(View.VISIBLE);
                if (customCb != null) customCb.onCustomViewHidden();
            }
        });
        web.addJavascriptInterface(new Bridge(), "ApkBridge");
        web.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String ua, String cd, String mt, long len) {
                startDownload(url, ua, cd, mt);
            }
        });

        applyMode(isDesktop(), false);
        if (DPI) {
            new Thread(new Runnable() {
                @Override public void run() {
                    startDpi();
                    runOnUiThread(new Runnable() {
                        @Override public void run() { web.loadUrl(START); }
                    });
                }
            }).start();
        } else {
            web.loadUrl(START);
        }
    }

    // ---- ByeDPI: gomulu ciadpi (libciadpi.so) yerel SOCKS5 sunucusu olarak baslatilir ----
    private int freePort(int from) {
        for (int p = from; p < from + 30; p++) {
            java.net.ServerSocket ss = null;
            try {
                ss = new java.net.ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new java.net.InetSocketAddress("127.0.0.1", p));
                ss.close();
                return p;
            } catch (Exception e) {
                try { if (ss != null) ss.close(); } catch (Exception e2) {}
            }
        }
        return -1;
    }

    private void startDpi() {
        try {
            java.io.File bin = new java.io.File(getApplicationInfo().nativeLibraryDir, "libciadpi.so");
            if (!bin.exists()) return;
            int port = freePort(DPI_PORT);
            if (port < 0) return;
            java.util.ArrayList<String> cmd = new java.util.ArrayList<String>();
            cmd.add(bin.getAbsolutePath());
            cmd.add("-i"); cmd.add("127.0.0.1");
            cmd.add("-p"); cmd.add(String.valueOf(port));
            for (String a : DPI_ARGS.trim().split("\\s+")) {
                if (a.length() > 0) cmd.add(a);
            }
            final Process pr = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            dpiProc = pr;
            Thread drain = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        byte[] buf = new byte[1024];
                        while (pr.getInputStream().read(buf) >= 0) { }
                    } catch (Exception e) {}
                }
            });
            drain.setDaemon(true);
            drain.start();
            for (int i = 0; i < 40; i++) {
                try {
                    java.net.Socket sk = new java.net.Socket();
                    sk.connect(new java.net.InetSocketAddress("127.0.0.1", port), 100);
                    sk.close();
                    proxyPort = port;
                    return;
                } catch (Exception e) {
                    try { Thread.sleep(50); } catch (Exception e2) {}
                }
            }
        } catch (Throwable t) {
        }
    }

    // WebView, ProxySelector'i okumaz; bu yuzden GET/HEAD/OPTIONS istekleri burada
    // yerel SOCKS5 (ciadpi) uzerinden cekilip WebView'e verilir.
    // POST govdesi shouldInterceptRequest'te gorunmez, o istekler normal agdan gider.
    private WebResourceResponse fetchViaProxy(WebResourceRequest r) {
        String method = r.getMethod();
        String scheme = r.getUrl().getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) return null;
        if (!("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method))) return null;
        java.net.Proxy px = new java.net.Proxy(java.net.Proxy.Type.SOCKS,
            java.net.InetSocketAddress.createUnresolved("127.0.0.1", proxyPort));
        String url = r.getUrl().toString();
        try {
            for (int hop = 0; hop < 8; hop++) {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection(px);
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestMethod(method.toUpperCase());
                for (java.util.Map.Entry<String, String> e : r.getRequestHeaders().entrySet()) {
                    String k = e.getKey();
                    if (k.equalsIgnoreCase("Accept-Encoding") || k.equalsIgnoreCase("Host") || k.equalsIgnoreCase("Connection")
                        || k.equalsIgnoreCase("Content-Length") || k.equalsIgnoreCase("Upgrade")) continue;
                    c.setRequestProperty(k, e.getValue());
                }
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) c.setRequestProperty("Cookie", ck);
                int code = c.getResponseCode();
                java.util.Map<String, String> out = new java.util.HashMap<String, String>();
                String location = null;
                for (java.util.Map.Entry<String, java.util.List<String>> e : c.getHeaderFields().entrySet()) {
                    String k = e.getKey();
                    if (k == null) continue;
                    if (k.equalsIgnoreCase("Set-Cookie")) {
                        for (String v : e.getValue()) CookieManager.getInstance().setCookie(url, v);
                        continue;
                    }
                    if (k.equalsIgnoreCase("Content-Encoding") || k.equalsIgnoreCase("Content-Length")
                        || k.equalsIgnoreCase("Transfer-Encoding") || k.equalsIgnoreCase("Connection")) continue;
                    StringBuilder sb = new StringBuilder();
                    for (String v : e.getValue()) { if (sb.length() > 0) sb.append(", "); sb.append(v); }
                    out.put(k, sb.toString());
                    if (k.equalsIgnoreCase("Location")) location = sb.toString();
                }
                if (code >= 300 && code < 400 && location != null) {
                    String next = new java.net.URL(new java.net.URL(url), location).toString();
                    c.disconnect();
                    if (r.isForMainFrame()) {
                        // WebResourceResponse 3xx kabul etmez: sayfa seviyesinde yonlendir (goreli adresler dogru cozulsun)
                        String js = next.replace("\\", "\\\\").replace("'", "\\'");
                        String html = "<html><head><meta http-equiv=\"refresh\" content=\"0;url=" + next.replace("\"", "&quot;")
                            + "\"></head><body><script>location.replace('" + js + "');</script></body></html>";
                        return new WebResourceResponse("text/html", "UTF-8", new java.io.ByteArrayInputStream(html.getBytes("UTF-8")));
                    }
                    url = next;
                    continue;
                }
                String ct = c.getContentType();
                String mime = "application/octet-stream";
                String enc = null;
                if (ct != null) {
                    String[] parts = ct.split(";");
                    mime = parts[0].trim();
                    for (int i = 1; i < parts.length; i++) {
                        String q = parts[i].trim();
                        if (q.toLowerCase().startsWith("charset=")) enc = q.substring(8).replace("\"", "").trim();
                    }
                }
                java.io.InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                if (in == null) in = new java.io.ByteArrayInputStream(new byte[0]);
                String reason = c.getResponseMessage();
                if (reason == null || reason.length() == 0) reason = "OK";
                if (code < 100 || code > 599) return null;
                if (VIDEODL && (mime.startsWith("video/") || mime.startsWith("audio/") || mime.contains("mpegurl"))) sniffAdd(url);
                return new WebResourceResponse(mime, enc, code, reason, out, in);
            }
        } catch (Throwable t) {
        }
        return null;
    }

    private Button navBtn(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextSize(20);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundColor(0x00000000);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    private void updateNav() {
        if (web == null || backBtn == null) return;
        backBtn.setAlpha(web.canGoBack() ? 1f : 0.35f);
        fwdBtn.setAlpha(web.canGoForward() ? 1f : 0.35f);
    }

    private boolean isDesktop() {
        if (cfg.contains("desktop")) return cfg.getBoolean("desktop", true);
        return defaultDesktop;
    }

    private void applyMode(boolean desktop, boolean reload) {
        WebSettings s = web.getSettings();
        if (desktop) {
            s.setUserAgentString(DESKTOP_UA);
        } else {
            s.setUserAgentString(WebSettings.getDefaultUserAgent(this));
        }
        s.setUseWideViewPort(desktop);
        s.setLoadWithOverviewMode(desktop);
        modeBtn.setText(desktop ? "M" : "D");
        if (reload) web.reload();
    }

    // ---- Indirme: dogrudan Download klasorune ----
    // ---- Site ici video/ses indirme (VIDEODL) ----
    private final java.util.LinkedHashSet<String> sniffed = new java.util.LinkedHashSet<String>();

    private void sniffAdd(String url) {
        synchronized (sniffed) {
            if (sniffed.contains(url)) return;
            sniffed.add(url);
            while (sniffed.size() > 25) {
                java.util.Iterator<String> it = sniffed.iterator();
                it.next();
                it.remove();
            }
        }
        web.post(new Runnable() {
            @Override public void run() { web.evaluateJavascript("window.__apkShow&&window.__apkShow()", null); }
        });
    }

    private void sniffUrl(String url) {
        String p = url;
        int q = p.indexOf('?');
        if (q > 0) p = p.substring(0, q);
        p = p.toLowerCase();
        String[] ok = {".mp4", ".webm", ".mkv", ".mov", ".m3u8", ".mp3", ".m4a", ".ogg", ".opus"};
        for (String e : ok) {
            if (p.endsWith(e)) { sniffAdd(url); return; }
        }
    }

    private static String typeLabel(String url) {
        String p = url;
        int q = p.indexOf('?');
        if (q > 0) p = p.substring(0, q);
        String l = p.toLowerCase();
        if (l.endsWith(".m3u8")) return "HLS";
        int d = l.lastIndexOf('.');
        return (d > 0 && l.length() - d <= 5) ? l.substring(d + 1).toUpperCase() : "VIDEO";
    }

    private static String shortName(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath();
            int sl = path.lastIndexOf('/');
            String tail = sl >= 0 ? path.substring(sl + 1) : path;
            if (tail.length() > 28) tail = tail.substring(0, 28) + "...";
            return u.getHost() + "  " + tail;
        } catch (Exception e) {
            return url.length() > 40 ? url.substring(0, 40) : url;
        }
    }

    private static String cleanName(String t) {
        if (t == null) t = "";
        t = t.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (t.length() > 80) t = t.substring(0, 80).trim();
        return t.length() == 0 ? "video" : t;
    }

    private static String extFor(String url, String ct) {
        if (ct.startsWith("video/mp4")) return "mp4";
        if (ct.startsWith("video/webm")) return "webm";
        if (ct.startsWith("video/x-matroska")) return "mkv";
        if (ct.startsWith("audio/mpeg")) return "mp3";
        if (ct.startsWith("audio/mp4") || ct.startsWith("audio/x-m4a")) return "m4a";
        if (ct.startsWith("audio/aac")) return "aac";
        if (ct.startsWith("audio/ogg")) return "ogg";
        String path = url;
        int q = path.indexOf('?');
        if (q > 0) path = path.substring(0, q);
        int d = path.lastIndexOf('.');
        if (d > 0 && path.length() - d <= 5) return path.substring(d + 1).toLowerCase();
        return "mp4";
    }

    private static String attr(String line, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[:,]" + name + "=(\"([^\"]*)\"|([^,]*))").matcher(line);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(3);
    }

    private static String resolve(String base, String u) throws Exception {
        return new java.net.URL(new java.net.URL(base), u).toString();
    }

    private static byte[] hex(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        while (s.length() < 32) s = "0" + s;
        byte[] r = new byte[16];
        for (int i = 0; i < 16; i++) r[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return r;
    }

    private static byte[] decrypt(byte[] d, byte[] key, byte[] iv, long seq) throws Exception {
        if (key == null) return d;
        byte[] v = iv;
        if (v == null) {
            v = new byte[16];
            for (int i = 0; i < 8; i++) v[15 - i] = (byte) (seq >> (8 * i));
        }
        javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"), new javax.crypto.spec.IvParameterSpec(v));
        return c.doFinal(d);
    }

    private java.net.HttpURLConnection openConn(String url, String referer, String ua) throws Exception {
        java.net.Proxy px = proxyPort >= 0
            ? new java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress.createUnresolved("127.0.0.1", proxyPort))
            : java.net.Proxy.NO_PROXY;
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection(px);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        if (ua != null && ua.length() > 0) c.setRequestProperty("User-Agent", ua);
        if (referer != null && referer.length() > 0) {
            c.setRequestProperty("Referer", referer);
            try {
                java.net.URL ru = new java.net.URL(referer);
                c.setRequestProperty("Origin", ru.getProtocol() + "://" + ru.getHost());
            } catch (Exception e) { }
        }
        String ck = CookieManager.getInstance().getCookie(url);
        if (ck != null) c.setRequestProperty("Cookie", ck);
        return c;
    }

    private byte[] httpBytes(String url, String referer, String ua) throws Exception {
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            try {
                java.net.HttpURLConnection c = openConn(url, referer, ua);
                int code = c.getResponseCode();
                if (code >= 400) throw new java.io.IOException("HTTP " + code);
                java.io.InputStream in = c.getInputStream();
                java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
                byte[] b = new byte[65536];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                in.close();
                return o.toByteArray();
            } catch (Exception e) {
                last = e;
            }
        }
        throw last;
    }

    /** audioOnly: sadece ses izini m4a'ya; degilse tum izleri mp4'e yazar. */
    private boolean remux(java.io.File src, java.io.File dst, boolean audioOnly) throws Exception {
        android.media.MediaExtractor ex = new android.media.MediaExtractor();
        android.media.MediaMuxer mx = null;
        boolean started = false;
        try {
            ex.setDataSource(src.getAbsolutePath());
            int n = ex.getTrackCount();
            int[] map = new int[n];
            boolean any = false;
            mx = new android.media.MediaMuxer(dst.getAbsolutePath(), android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            for (int i = 0; i < n; i++) {
                map[i] = -1;
                android.media.MediaFormat f = ex.getTrackFormat(i);
                String m = f.getString(android.media.MediaFormat.KEY_MIME);
                if (m == null) continue;
                boolean take = audioOnly ? m.startsWith("audio/") : (m.startsWith("audio/") || m.startsWith("video/"));
                if (!take) continue;
                ex.selectTrack(i);
                map[i] = mx.addTrack(f);
                any = true;
                if (audioOnly) break;
            }
            if (!any) return false;
            mx.start();
            started = true;
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(2 << 20);
            android.media.MediaCodec.BufferInfo bi = new android.media.MediaCodec.BufferInfo();
            while (true) {
                buf.clear();
                int sz = ex.readSampleData(buf, 0);
                if (sz < 0) break;
                int ti = ex.getSampleTrackIndex();
                if (ti >= 0 && ti < n && map[ti] >= 0) {
                    bi.offset = 0;
                    bi.size = sz;
                    bi.presentationTimeUs = ex.getSampleTime();
                    bi.flags = (ex.getSampleFlags() & android.media.MediaExtractor.SAMPLE_FLAG_SYNC) != 0 ? android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                    mx.writeSampleData(map[ti], buf, bi);
                }
                ex.advance();
            }
            mx.stop();
            started = false;
            return true;
        } finally {
            try { if (mx != null) { if (started) mx.stop(); mx.release(); } } catch (Throwable t) { }
            ex.release();
        }
    }

    private static void copyFile(java.io.File a, java.io.File b) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(a);
        java.io.FileOutputStream o = new java.io.FileOutputStream(b);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        o.close();
        in.close();
    }

    private void scan(java.io.File f) {
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
    }

    private void hls(String url, final String mode, String base, final String referer, final String ua) throws Exception {
        String pl = new String(httpBytes(url, referer, ua), "UTF-8");
        if (pl.contains("#EXT-X-STREAM-INF")) {
            String[] ls = pl.split("\\r?\\n");
            String best = null;
            long bw = -1;
            for (int i = 0; i < ls.length; i++) {
                if (!ls[i].startsWith("#EXT-X-STREAM-INF")) continue;
                long bb = 0;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("BANDWIDTH=(\\d+)").matcher(ls[i]);
                if (m.find()) bb = Long.parseLong(m.group(1));
                for (int j = i + 1; j < ls.length; j++) {
                    String t = ls[j].trim();
                    if (t.length() > 0 && !t.startsWith("#")) {
                        if (bb > bw) { bw = bb; best = t; }
                        break;
                    }
                }
            }
            if (best == null) throw new Exception("HLS listesi okunamadi");
            url = resolve(url, best);
            pl = new String(httpBytes(url, referer, ua), "UTF-8");
        }
        final java.util.ArrayList<String> segUrls = new java.util.ArrayList<String>();
        final java.util.ArrayList<byte[]> segKeys = new java.util.ArrayList<byte[]>();
        final java.util.ArrayList<byte[]> segIvs = new java.util.ArrayList<byte[]>();
        final java.util.ArrayList<Long> segSeq = new java.util.ArrayList<Long>();
        long seq = 0;
        byte[] key = null;
        byte[] iv = null;
        String mapUrl = null;
        for (String raw : pl.split("\\r?\\n")) {
            String t = raw.trim();
            if (t.length() == 0) continue;
            if (t.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                seq = Long.parseLong(t.substring(22).trim());
            } else if (t.startsWith("#EXT-X-KEY:")) {
                String m = attr(t, "METHOD");
                if ("NONE".equals(m)) {
                    key = null;
                    iv = null;
                } else if ("AES-128".equals(m)) {
                    key = httpBytes(resolve(url, attr(t, "URI")), referer, ua);
                    String ivs = attr(t, "IV");
                    iv = ivs == null ? null : hex(ivs);
                } else {
                    throw new Exception("Sifreli akis (" + m + ") desteklenmiyor");
                }
            } else if (t.startsWith("#EXT-X-MAP:")) {
                mapUrl = resolve(url, attr(t, "URI"));
            } else if (!t.startsWith("#")) {
                segUrls.add(resolve(url, t));
                segKeys.add(key);
                segIvs.add(iv);
                segSeq.add(Long.valueOf(seq++));
            }
        }
        if (segUrls.isEmpty()) throw new Exception("HLS parca bulunamadi");
        boolean fmp4 = mapUrl != null;
        java.io.File tmp = new java.io.File(getCacheDir(), "hls_" + System.currentTimeMillis() + (fmp4 ? ".mp4" : ".ts"));
        toast("HLS indiriliyor: " + segUrls.size() + " parca");
        java.io.FileOutputStream o = new java.io.FileOutputStream(tmp);
        java.util.concurrent.ExecutorService ex = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            if (mapUrl != null) o.write(httpBytes(mapUrl, referer, ua));
            int total = segUrls.size();
            int step = 8;
            int lastPct = 0;
            for (int s0 = 0; s0 < total; s0 += step) {
                java.util.ArrayList<java.util.concurrent.Future<byte[]>> fs = new java.util.ArrayList<java.util.concurrent.Future<byte[]>>();
                int end = Math.min(total, s0 + step);
                for (int i = s0; i < end; i++) {
                    final int idx = i;
                    fs.add(ex.submit(new java.util.concurrent.Callable<byte[]>() {
                        @Override public byte[] call() throws Exception {
                            byte[] d = httpBytes(segUrls.get(idx), referer, ua);
                            return decrypt(d, segKeys.get(idx), segIvs.get(idx), segSeq.get(idx).longValue());
                        }
                    }));
                }
                for (java.util.concurrent.Future<byte[]> f : fs) o.write(f.get());
                int pct = (end * 100) / total;
                if (pct / 25 > lastPct / 25 && pct < 100) toast("Indiriliyor %" + pct);
                lastPct = pct;
            }
        } finally {
            ex.shutdown();
            o.close();
        }
        try {
            if ("audio".equals(mode)) {
                java.io.File out = uniqueFile(base + ".m4a");
                if (remux(tmp, out, true)) {
                    scan(out);
                    toast("Ses indirildi: " + out.getName() + "  (Download klasoru)");
                } else {
                    out.delete();
                    toast("Ses ayrilamadi (format desteklenmiyor)");
                }
            } else {
                java.io.File out = uniqueFile(base + ".mp4");
                boolean ok = false;
                try { ok = remux(tmp, out, false); } catch (Throwable t) { ok = false; }
                if (!ok) {
                    out.delete();
                    out = uniqueFile(base + (fmp4 ? ".mp4" : ".ts"));
                    copyFile(tmp, out);
                }
                scan(out);
                toast("Indirildi: " + out.getName() + "  (Download klasoru)");
            }
        } finally {
            tmp.delete();
        }
    }

    private void videoDl(final String url, final String mode, final String title, final String referer, final String ua) {
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            runOnUiThread(new Runnable() {
                @Override public void run() { requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 6); }
            });
            toast("Depolama iznini ver, sonra tekrar bas");
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                java.io.File tmp = null;
                try {
                    toast("Indirme basladi...");
                    String base = cleanName(title);
                    String pathOnly = url;
                    int qi = pathOnly.indexOf('?');
                    if (qi > 0) pathOnly = pathOnly.substring(0, qi);
                    if (pathOnly.toLowerCase().endsWith(".m3u8")) {
                        hls(url, mode, base, referer, ua);
                        return;
                    }
                    java.net.HttpURLConnection c = openConn(url, referer, ua);
                    int code = c.getResponseCode();
                    if (code >= 400) { toast("Indirilemedi: HTTP " + code); return; }
                    String ct = c.getContentType();
                    ct = ct == null ? "" : ct.toLowerCase();
                    if (ct.contains("mpegurl")) {
                        c.disconnect();
                        hls(url, mode, base, referer, ua);
                        return;
                    }
                    String ext = extFor(url, ct);
                    boolean isAudioFile = ct.startsWith("audio/") || ext.equals("mp3") || ext.equals("m4a") || ext.equals("aac") || ext.equals("ogg") || ext.equals("wav");
                    boolean wantAudio = "audio".equals(mode);
                    java.io.InputStream in = c.getInputStream();
                    java.io.File target;
                    if (wantAudio && !isAudioFile) {
                        tmp = new java.io.File(getCacheDir(), "dl_" + System.currentTimeMillis() + ".tmp");
                        target = tmp;
                    } else {
                        target = uniqueFile(base + "." + ext);
                    }
                    java.io.FileOutputStream o = new java.io.FileOutputStream(target);
                    byte[] b = new byte[65536];
                    int n;
                    while ((n = in.read(b)) > 0) o.write(b, 0, n);
                    o.close();
                    in.close();
                    if (tmp != null) {
                        java.io.File out = uniqueFile(base + ".m4a");
                        if (remux(tmp, out, true)) {
                            scan(out);
                            toast("Ses indirildi: " + out.getName() + "  (Download klasoru)");
                        } else {
                            out.delete();
                            toast("Ses ayrilamadi (format desteklenmiyor)");
                        }
                    } else {
                        scan(target);
                        toast("Indirildi: " + target.getName() + "  (Download klasoru)");
                    }
                } catch (Throwable t) {
                    toast("Indirme hatasi: " + t);
                } finally {
                    if (tmp != null) tmp.delete();
                }
            }
        }).start();
    }

    class Bridge {
        @JavascriptInterface public void pick(String json, final String title, final String referer, final String ua) {
            final java.util.ArrayList<String> c = new java.util.ArrayList<String>();
            try {
                org.json.JSONArray a = new org.json.JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String u = a.getString(i);
                    if (!c.contains(u)) c.add(u);
                }
            } catch (Exception e) { }
            synchronized (sniffed) {
                java.util.ArrayList<String> sn = new java.util.ArrayList<String>(sniffed);
                java.util.Collections.reverse(sn);
                for (String u : sn) if (!c.contains(u)) c.add(u);
            }
            if (c.isEmpty()) {
                toast("Indirilebilir video bulunamadi (korumali veya parcali akis olabilir)");
                return;
            }
            final String[] labels = new String[c.size()];
            for (int i = 0; i < labels.length; i++) labels[i] = "[" + typeLabel(c.get(i)) + "] " + shortName(c.get(i));
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Indirilecek video")
                        .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                final String u = c.get(w);
                                new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Ne indirilsin?")
                                    .setItems(new String[]{"Video", "Ses (M4A)"}, new android.content.DialogInterface.OnClickListener() {
                                        @Override public void onClick(android.content.DialogInterface d2, int w2) {
                                            videoDl(u, w2 == 0 ? "video" : "audio", title, referer, ua);
                                        }
                                    }).show();
                            }
                        }).show();
                }
            });
        }

        @JavascriptInterface public void dl(String url, String mode, String title, String referer, String ua) {
            videoDl(url, mode, title, referer, ua);
        }

        @JavascriptInterface public void save(String dataUrl, String name) {
            try {
                int c = dataUrl.indexOf(',');
                byte[] d = android.util.Base64.decode(dataUrl.substring(c + 1), 0);
                java.io.File f = uniqueFile(name);
                java.io.FileOutputStream o = new java.io.FileOutputStream(f);
                o.write(d);
                o.close();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                toast("Indirildi: " + f.getName() + "  (Download klasoru)");
            } catch (Exception e) {
                toast("Indirme hatasi: " + e);
            }
        }
    }

    private void toast(final String m) {
        runOnUiThread(new Runnable() {
            @Override public void run() { android.widget.Toast.makeText(MainActivity.this, m, android.widget.Toast.LENGTH_LONG).show(); }
        });
    }

    private java.io.File uniqueFile(String name) {
        java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        dir.mkdirs();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        java.io.File f = new java.io.File(dir, name);
        int i = 1;
        while (f.exists()) {
            int dot = name.lastIndexOf('.');
            f = new java.io.File(dir, dot > 0 ? name.substring(0, dot) + " (" + i + ")" + name.substring(dot) : name + " (" + i + ")");
            i++;
        }
        return f;
    }

    private void startDownload(String url, String ua, String cd, String mime) {
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            pdUrl = url; pdUa = ua; pdCd = cd; pdMime = mime;
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 5);
            return;
        }
        String name = URLUtil.guessFileName(url, cd, mime);
        try {
            if (url.startsWith("blob:")) {
                web.evaluateJavascript("(function(){var x=new XMLHttpRequest();x.open('GET','" + url + "');x.responseType='blob';"
                    + "x.onload=function(){var r=new FileReader();r.onloadend=function(){ApkBridge.save(r.result,'" + name.replace("'", "") + "');};r.readAsDataURL(x.response);};x.send();})()", null);
                toast("Indiriliyor: " + name);
            } else if (url.startsWith("data:")) {
                new Bridge().save(url, name);
            } else {
                android.app.DownloadManager.Request q = new android.app.DownloadManager.Request(Uri.parse(url));
                if (mime != null) q.setMimeType(mime);
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) q.addRequestHeader("Cookie", ck);
                if (ua != null) q.addRequestHeader("User-Agent", ua);
                q.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                q.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name);
                ((android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(q);
                toast("Indiriliyor: " + name + "  (Download klasoru)");
            }
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e2) { toast("Indirilemedi: " + e); }
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && fileCb != null) {
            Uri[] r = (data == null || res != RESULT_OK) ? null : WebChromeClient.FileChooserParams.parseResult(res, data);
            fileCb.onReceiveValue(r);
            fileCb = null;
        }
    }

    @Override public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        boolean ok = results.length > 0 && results[0] == 0;
        if (req == 4 && micReq != null) {
            if (ok) micReq.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE}); else micReq.deny();
            micReq = null;
        }
        if (req == 5 && ok && pdUrl != null) {
            startDownload(pdUrl, pdUa, pdCd, pdMime);
            pdUrl = null;
        }
        if (req == 3 && geoCb != null) {
            geoCb.invoke(geoOrigin, results.length > 0 && results[0] == 0, false);
            geoCb = null;
            geoOrigin = null;
        }
    }

    @Override protected void onDestroy() {
        if (dpiProc != null) dpiProc.destroy();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
