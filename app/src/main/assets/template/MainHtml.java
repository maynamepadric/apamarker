package __PKG__;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView web;
    private View custom;
    private WebChromeClient.CustomViewCallback customCb;
    private ValueCallback<Uri[]> fileCb;
    private GeolocationPermissions.Callback geoCb;
    private PermissionRequest micReq;
    private String pdUrl, pdUa, pdCd, pdMime;
    private String geoOrigin;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setGeolocationEnabled(true);
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
                if (customCb != null) customCb.onCustomViewHidden();
            }
        });
        web.setWebViewClient(new WebViewClient() {
            // DPI_HOOK_MARKER
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("file") || url.startsWith("http")) { v.loadUrl(url); return true; }
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "ApkBridge");
        web.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String ua, String cd, String mt, long len) {
                startDownload(url, ua, cd, mt);
            }
        });
        web.loadUrl("file:///android_asset/www/__ENTRY__");
    }

    // ---- Indirme: dogrudan Download klasorune ----
    class Bridge {
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
        if (web != null) web.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
