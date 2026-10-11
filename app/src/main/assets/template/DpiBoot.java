package dpi;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Gomulu ByeDPI (libciadpi.so) baslatir ve JVM varsayilan ProxySelector'ini yerel SOCKS5'e cevirir.
 *  Ayar: assets/dpi.cfg  ->  port=1082 / args=--disorder 1 --tlsrec 1+s */
public class Boot {
    private static volatile int port = -1;
    private static boolean started;
    private static Process proc;

    public static synchronized void start(Context c) {
        if (started) return;
        started = true;
        try {
            int want = 1082;
            String args = "--disorder 1 --tlsrec 1+s";
            try {
                InputStream in = c.getAssets().open("dpi.cfg");
                Properties p = new Properties();
                p.load(in);
                in.close();
                want = Integer.parseInt(p.getProperty("port", "1082").trim());
                args = p.getProperty("args", args).trim();
            } catch (Throwable t) {
            }
            File bin = new File(c.getApplicationInfo().nativeLibraryDir, "libciadpi.so");
            if (!bin.exists()) return;
            int use = freePort(want);
            if (use < 0) return;
            List<String> cmd = new ArrayList<String>();
            cmd.add(bin.getAbsolutePath());
            cmd.add("-i");
            cmd.add("127.0.0.1");
            cmd.add("-p");
            cmd.add(String.valueOf(use));
            for (String a : args.split("\\s+")) {
                if (a.length() > 0) cmd.add(a);
            }
            final Process pr = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            proc = pr;
            Thread drain = new Thread(new Runnable() {
                public void run() {
                    try {
                        byte[] b = new byte[1024];
                        while (pr.getInputStream().read(b) >= 0) { }
                    } catch (Throwable t) {
                    }
                }
            });
            drain.setDaemon(true);
            drain.start();
            for (int i = 0; i < 40; i++) {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress("127.0.0.1", use), 100);
                    s.close();
                    port = use;
                    ProxySelector.setDefault(new Sel(use));
                    return;
                } catch (Throwable t) {
                    try { Thread.sleep(50); } catch (Throwable t2) { }
                }
            }
        } catch (Throwable t) {
        }
    }

    public static int port() {
        return port;
    }

    private static int freePort(int from) {
        for (int p = from; p < from + 30; p++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress("127.0.0.1", p));
                ss.close();
                return p;
            } catch (Throwable t) {
                try { if (ss != null) ss.close(); } catch (Throwable t2) { }
            }
        }
        return -1;
    }

    static class Sel extends ProxySelector {
        private final List<Proxy> socks;
        private final List<Proxy> direct = Collections.singletonList(Proxy.NO_PROXY);

        Sel(int port) {
            socks = Collections.singletonList(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", port)));
        }

        @Override public List<Proxy> select(URI uri) {
            String h = uri == null ? null : uri.getHost();
            if (h != null && (h.equals("localhost") || h.startsWith("127.") || h.equals("[::1]") || h.equals("::1")
                    || h.startsWith("10.") || h.startsWith("192.168."))) {
                return direct;
            }
            return socks;
        }

        @Override public void connectFailed(URI uri, SocketAddress sa, java.io.IOException e) {
        }
    }

    /** WebView icin: GET/HEAD/OPTIONS isteklerini SOCKS5 uzerinden ceker. Hazir degilse null (normal ag). */
    public static WebResourceResponse intercept(WebResourceRequest r) {
        int pp = port;
        if (pp < 0) return null;
        String method = r.getMethod();
        String scheme = r.getUrl().getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) return null;
        if (!("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method))) return null;
        Proxy px = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", pp));
        String url = r.getUrl().toString();
        try {
            for (int hop = 0; hop < 8; hop++) {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(px);
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestMethod(method.toUpperCase());
                for (Map.Entry<String, String> e : r.getRequestHeaders().entrySet()) {
                    String k = e.getKey();
                    if (k.equalsIgnoreCase("Accept-Encoding") || k.equalsIgnoreCase("Host") || k.equalsIgnoreCase("Connection")
                            || k.equalsIgnoreCase("Content-Length") || k.equalsIgnoreCase("Upgrade")) continue;
                    c.setRequestProperty(k, e.getValue());
                }
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) c.setRequestProperty("Cookie", ck);
                int code = c.getResponseCode();
                Map<String, String> out = new HashMap<String, String>();
                String location = null;
                for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                    String k = e.getKey();
                    if (k == null) continue;
                    if (k.equalsIgnoreCase("Set-Cookie")) {
                        for (String v : e.getValue()) CookieManager.getInstance().setCookie(url, v);
                        continue;
                    }
                    if (k.equalsIgnoreCase("Content-Encoding") || k.equalsIgnoreCase("Content-Length")
                            || k.equalsIgnoreCase("Transfer-Encoding") || k.equalsIgnoreCase("Connection")) continue;
                    StringBuilder sb = new StringBuilder();
                    for (String v : e.getValue()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(v);
                    }
                    out.put(k, sb.toString());
                    if (k.equalsIgnoreCase("Location")) location = sb.toString();
                }
                if (code >= 300 && code < 400 && location != null) {
                    String next = new URL(new URL(url), location).toString();
                    c.disconnect();
                    if (r.isForMainFrame()) {
                        String js = next.replace("\\", "\\\\").replace("'", "\\'");
                        String html = "<html><head><meta http-equiv=\"refresh\" content=\"0;url=" + next.replace("\"", "&quot;")
                                + "\"></head><body><script>location.replace('" + js + "');</script></body></html>";
                        return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(html.getBytes("UTF-8")));
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
                InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                if (in == null) in = new ByteArrayInputStream(new byte[0]);
                String reason = c.getResponseMessage();
                if (reason == null || reason.length() == 0) reason = "OK";
                if (code < 100 || code > 599) return null;
                return new WebResourceResponse(mime, enc, code, reason, out, in);
            }
        } catch (Throwable t) {
        }
        return null;
    }
}
