package tr.apkmaker;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** APK Maker'in kendi ByeDPI'si: site gorseli indirirken engelli siteye ulasmak icin. Ayar: Prefs "selfdpi". */
public class SelfDpi {
    private static Process proc;
    private static int port = -1;
    private static ProxySelector orig;
    private static boolean installed;

    /** Ayar aciksa ve calismiyorsa baslatir, hazir olana kadar bekler. true: proxy aktif. */
    public static synchronized boolean ensure(Context c) {
        if (!Prefs.bool(c, "selfdpi", false)) return false;
        if (proc != null && port > 0) {
            try {
                proc.exitValue();
                proc = null;
            } catch (IllegalThreadStateException alive) {
                return true;
            }
        }
        try {
            File bin = new File(c.getFilesDir(), "ciadpi_self");
            InputStream in = c.getAssets().open("ciadpi.bin");
            Builder.copyStream(in, new FileOutputStream(bin));
            bin.setExecutable(true, false);
            ServerSocket ss = new ServerSocket(0);
            int p = ss.getLocalPort();
            ss.close();
            List<String> cmd = new ArrayList<String>();
            cmd.add(bin.getAbsolutePath());
            cmd.add("-i");
            cmd.add("127.0.0.1");
            cmd.add("-p");
            cmd.add(String.valueOf(p));
            for (String a : Prefs.str(c, "args", Prefs.DEF_ARGS).trim().split("\\s+")) {
                if (a.length() > 0) cmd.add(a);
            }
            final Process pr = new ProcessBuilder(cmd).redirectErrorStream(true).start();
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
                    s.connect(new InetSocketAddress("127.0.0.1", p), 100);
                    s.close();
                    proc = pr;
                    port = p;
                    if (!installed) {
                        orig = ProxySelector.getDefault();
                        installed = true;
                    }
                    ProxySelector.setDefault(new Sel(p));
                    return true;
                } catch (Throwable t) {
                    try { Thread.sleep(50); } catch (Throwable t2) { }
                }
            }
            pr.destroy();
        } catch (Throwable t) {
        }
        return false;
    }

    public static synchronized void stop() {
        if (proc != null) proc.destroy();
        proc = null;
        port = -1;
        if (installed) {
            ProxySelector.setDefault(orig);
            installed = false;
        }
    }

    public static synchronized boolean running() {
        if (proc == null) return false;
        try {
            proc.exitValue();
            return false;
        } catch (IllegalThreadStateException alive) {
            return true;
        }
    }

    static class Sel extends ProxySelector {
        private final List<Proxy> socks;
        private final List<Proxy> direct = Collections.singletonList(Proxy.NO_PROXY);

        Sel(int p) {
            socks = Collections.singletonList(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", p)));
        }

        @Override public List<Proxy> select(URI uri) {
            String h = uri == null ? null : uri.getHost();
            if (h != null && (h.equals("localhost") || h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168."))) {
                return direct;
            }
            return socks;
        }

        @Override public void connectFailed(URI uri, SocketAddress sa, java.io.IOException e) {
        }
    }
}
