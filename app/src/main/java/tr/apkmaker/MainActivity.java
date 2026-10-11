package tr.apkmaker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import tr.apkmaker.Builder;

public class MainActivity extends Activity {
    private CheckBox chkDpi;
    private CheckBox chkBye;
    private LinearLayout settingsP;
    private LinearLayout toolsP;
    private LinearLayout toolsList;
    private LinearLayout patchP;
    private TextView patchInfo;
    private EditText edPatchArgs;
    private String patchIn;
    private final java.util.ArrayList<Object[]> binds = new java.util.ArrayList<Object[]>();
    private CheckBox chkVid;
    private EditText edBye;
    private CheckBox chkByeCode;
    private EditText edByeCode;
    private CheckBox chkByeFile;
    private EditText edByeFile;
    private CheckBox chkHtml;
    private LinearLayout codeP;
    private LinearLayout fileP;
    private EditText edFileName;
    private TextView fileInfo;
    private Detect.Result detected;
    private LinearLayout container;
    private EditText edCode;
    private EditText edName;
    private EditText edCodeName;
    private ScrollView logScroll;
    private EditText edUrl;
    private final Handler h = new Handler();
    private File iconFile;
    private TextView iconInfo;
    private final java.util.ArrayList<TextView> iconInfos = new java.util.ArrayList<TextView>();
    private TextView logv;
    private LinearLayout mainP;
    private TextView status;
    private LinearLayout urlP;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        L.set(getSharedPreferences("cfg", 0).getInt("lang", 0));
        float f = getResources().getDisplayMetrics().density;
        int i = (int) (14.0f * f);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(i, i, i, i);
        linearLayout.setBackgroundColor(-15724010);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(0);
        head.setGravity(16);
        TextView textView = new TextView(this);
        String ver = "";
        try { ver = " v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { }
        textView.setText("APK Maker" + ver);
        textView.setTextSize(20.0f);
        textView.setTextColor(-1);
        head.addView(textView, new LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.ImageButton gear = new android.widget.ImageButton(this);
        gear.setImageResource(android.R.drawable.ic_menu_preferences);
        gear.setBackgroundColor(0);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.show("settings"); }
        });
        head.addView(gear, new LinearLayout.LayoutParams((int) (44.0f * f), (int) (44.0f * f)));
        linearLayout.addView(head);
        this.status = mkText(L.t("Hazir"), 14, -11751600);
        linearLayout.addView(this.status);
        this.container = new LinearLayout(this);
        this.container.setOrientation(1);
        ScrollView pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.addView(this.container, new ViewGroup.LayoutParams(-1, -2));
        linearLayout.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.logv = mkText("", 11, -2236963);
        ScrollView scrollView = new ScrollView(this);
        this.logScroll = scrollView;
        scrollView.addView(this.logv);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, (int) (f * 140.0f)));
        buildMain();
        buildUrlPanel();
        buildCodePanel();
        buildFilePanel();
        buildSettingsPanel();
        buildToolsPanel();
        buildPatchPanel();
        bindAll();
        this.logScroll.setVisibility(Prefs.bool(this, "showlog", true) ? 0 : 8);
        if (Prefs.bool(this, "selfdpi", false)) {
            new Thread(new Runnable() {
                @Override public void run() { SelfDpi.ensure(MainActivity.this); }
            }).start();
        }
        show("main");
        setContentView(linearLayout);
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
        }
    }

    public void show(String str) {
        this.mainP.setVisibility("main".equals(str) ? 0 : 8);
        this.urlP.setVisibility("url".equals(str) ? 0 : 8);
        this.codeP.setVisibility("code".equals(str) ? 0 : 8);
        this.fileP.setVisibility("file".equals(str) ? 0 : 8);
        this.settingsP.setVisibility("settings".equals(str) ? 0 : 8);
        this.toolsP.setVisibility("tools".equals(str) ? 0 : 8);
        this.patchP.setVisibility("patch".equals(str) ? 0 : 8);
        syncPrefs();
        if ("tools".equals(str)) runToolCheck();
    }

    private void buildMain() {
        this.mainP = panel();
        this.mainP.addView(mkText(L.t("Ne uretmek istiyorsun?"), 14, -6381922));
        this.mainP.addView(navBtn(L.t("URL ILE APK YAP  (web sitesi)"), "url"));
        this.mainP.addView(navBtn(L.t("KOD ILE APK YAP  (Java / HTML5)"), "code"));
        this.mainP.addView(navBtn(L.t("DOSYADAN APK YAP  (Java / HTML5 / zip)"), "file"));
        this.mainP.addView(navBtn(L.t("YAMALAYICI  (mevcut uygulamaya DPI ekle)"), "patch"));
        this.container.addView(this.mainP);
    }

    private Button navBtn(String text, final String target) {
        Button b = mkBtn(text);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.show(target); }
        });
        return b;
    }

    // ---- kalici ayarlar ----
    private void bindCheck(final CheckBox cb, final String key, boolean def) {
        cb.setChecked(Prefs.bool(this, key, def));
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean c) { Prefs.setBool(MainActivity.this, key, c); }
        });
        this.binds.add(new Object[]{cb, key, Boolean.valueOf(def)});
    }

    private void bindText(final EditText e, final String key, String def) {
        e.setText(Prefs.str(this, key, def));
        e.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) { Prefs.setStr(MainActivity.this, key, s.toString()); }
        });
        this.binds.add(new Object[]{e, key, def});
    }

    private void bindAll() {
        bindCheck(this.chkDpi, "desk", false);
        bindCheck(this.chkBye, "bye", false);
        bindCheck(this.chkVid, "vid", false);
        bindCheck(this.chkByeCode, "bye", false);
        bindCheck(this.chkByeFile, "bye", false);
        bindText(this.edBye, "args", Prefs.DEF_ARGS);
        bindText(this.edByeCode, "args", Prefs.DEF_ARGS);
        bindText(this.edByeFile, "args", Prefs.DEF_ARGS);
        bindText(this.edPatchArgs, "args", Prefs.DEF_ARGS);
    }

    private void syncPrefs() {
        for (Object[] b : this.binds) {
            if (b[0] instanceof CheckBox) {
                boolean v = Prefs.bool(this, (String) b[1], ((Boolean) b[2]).booleanValue());
                CheckBox c = (CheckBox) b[0];
                if (c.isChecked() != v) c.setChecked(v);
            } else {
                String v = Prefs.str(this, (String) b[1], (String) b[2]);
                EditText e = (EditText) b[0];
                if (!e.getText().toString().equals(v)) e.setText(v);
            }
        }
    }

    // ---- AYARLAR paneli ----
    private CheckBox settingCheck(String text, String key, boolean def) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setTextColor(-1118482);
        this.settingsP.addView(cb);
        bindCheck(cb, key, def);
        return cb;
    }

    private void buildSettingsPanel() {
        this.settingsP = panel();
        this.settingsP.addView(mkText(L.t("--- AYARLAR ---\nDegisiklikler otomatik kaydedilir."), 13, -11751600));
        Button langBtn = mkBtn("Dil / Language:  " + L.name());
        langBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                getSharedPreferences("cfg", 0).edit().putInt("lang", (L.cur() + 1) % L.count()).apply();
                MainActivity.this.recreate();
            }
        });
        this.settingsP.addView(langBtn);
        settingCheck(L.t("Yeni uygulamalarda DPI engelini as (varsayilan)"), "bye", false);
        this.settingsP.addView(mkText(L.t("ByeDPI argumanlari"), 12, -6381922));
        EditText ea = mkEdit(L.t("ByeDPI argumanlari"), false);
        this.settingsP.addView(ea);
        bindText(ea, "args", Prefs.DEF_ARGS);
        settingCheck(L.t("Sitedeki video/ses indirme dugmesi (varsayilan)"), "vid", false);
        settingCheck(L.t("Masaustu modu (varsayilan)"), "desk", false);
        CheckBox lg = settingCheck(L.t("Alttaki gunluk alanini goster"), "showlog", true);
        lg.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean c) {
                Prefs.setBool(MainActivity.this, "showlog", c);
                MainActivity.this.logScroll.setVisibility(c ? 0 : 8);
            }
        });
        CheckBox sd = settingCheck(L.t("APK Maker'in kendi DPI'si (engelli sitenin gorselini indirirken)"), "selfdpi", false);
        sd.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean c) {
                Prefs.setBool(MainActivity.this, "selfdpi", c);
                new Thread(new Runnable() {
                    @Override public void run() {
                        if (Prefs.bool(MainActivity.this, "selfdpi", false)) {
                            final boolean ok = SelfDpi.ensure(MainActivity.this);
                            MainActivity.this.h.post(new Runnable() {
                                @Override public void run() { toast(ok ? L.t("APK Maker DPI aktif") : L.t("Baslatilamadi (ciadpi.bin eksik olabilir)")); }
                            });
                        } else {
                            SelfDpi.stop();
                        }
                    }
                }).start();
            }
        });
        Button hist = mkBtn(L.t("GECMIS  (uretilen uygulamalar)"));
        hist.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.showHistory(); }
        });
        this.settingsP.addView(hist);
        this.settingsP.addView(navBtn(L.t("ARAC SETI  (durum)"), "tools"));
        this.settingsP.addView(backBtn());
        this.container.addView(this.settingsP);
    }

    // ---- ARAC SETI paneli ----
    private void buildToolsPanel() {
        this.toolsP = panel();
        this.toolsP.addView(mkText(L.t("--- ARAC SETI ---"), 13, -11751600));
        this.toolsList = new LinearLayout(this);
        this.toolsList.setOrientation(1);
        this.toolsP.addView(this.toolsList);
        Button re = mkBtn(L.t("YENIDEN KONTROL ET"));
        re.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.runToolCheck(); }
        });
        this.toolsP.addView(re);
        this.toolsP.addView(backBtn());
        this.container.addView(this.toolsP);
    }

    private void runToolCheck() {
        this.toolsList.removeAllViews();
        this.toolsList.addView(mkText(L.t("Kontrol ediliyor... (ilk seferde araclar cikartilir, 1-2 dk)"), 13, -6381922));
        new Thread(new Runnable() {
            @Override public void run() {
                final java.util.List<String[]> rows = ToolCheck.run(MainActivity.this);
                MainActivity.this.h.post(new Runnable() {
                    @Override public void run() {
                        MainActivity.this.toolsList.removeAllViews();
                        float dn = getResources().getDisplayMetrics().density;
                        for (String[] r : rows) {
                            LinearLayout row = new LinearLayout(MainActivity.this);
                            row.setOrientation(0);
                            row.setPadding(0, (int) (6 * dn), 0, (int) (6 * dn));
                            TextView dot = new TextView(MainActivity.this);
                            dot.setText("\u25CF ");
                            dot.setTextSize(16.0f);
                            dot.setTextColor("ok".equals(r[1]) ? -11751600 : ("warn".equals(r[1]) ? -22528 : -769226));
                            row.addView(dot);
                            LinearLayout col = new LinearLayout(MainActivity.this);
                            col.setOrientation(1);
                            TextView nm = new TextView(MainActivity.this);
                            nm.setText(r[0]);
                            nm.setTextSize(15.0f);
                            nm.setTextColor(-1);
                            col.addView(nm);
                            TextView sub = new TextView(MainActivity.this);
                            sub.setText(r[2]);
                            sub.setTextSize(12.0f);
                            sub.setTextColor(-6381922);
                            col.addView(sub);
                            row.addView(col);
                            MainActivity.this.toolsList.addView(row);
                        }
                    }
                });
            }
        }).start();
    }

    // ---- YAMALAYICI paneli (Termux ile) ----
    private void buildPatchPanel() {
        this.patchP = panel();
        this.patchP.addView(mkText(L.t("--- YAMALAYICI ---\nTelefondaki bir uygulamayi (APK / APKM / XAPK) secip ByeDPI gomer. Yamalama Termux ile yapilir.\nIlk kullanimda once asagidaki KURULUM KOMUTU dugmesine bas, komutu Termux'a yapistir (bir kerelik)."), 12, -11751600));
        Button setup = mkBtn(L.t("TERMUX KURULUM KOMUTUNU KOPYALA"));
        setup.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.copySetup(); }
        });
        this.patchP.addView(setup);
        Button pick = mkBtn(L.t("UYGULAMA DOSYASI SEC"));
        pick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent i = new Intent("android.intent.action.OPEN_DOCUMENT");
                i.addCategory("android.intent.category.OPENABLE");
                i.setType("*/*");
                MainActivity.this.startActivityForResult(i, 4);
            }
        });
        this.patchP.addView(pick);
        this.patchInfo = mkText(L.t("Henuz dosya secilmedi"), 13, -2236963);
        this.patchP.addView(this.patchInfo);
        this.patchP.addView(mkText(L.t("ByeDPI argumanlari"), 12, -6381922));
        this.edPatchArgs = mkEdit(L.t("ByeDPI argumanlari"), false);
        this.patchP.addView(this.edPatchArgs);
        Button go = mkBtn(L.t("TERMUX ILE YAMALA"));
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.doPatch(true); }
        });
        this.patchP.addView(go);
        Button cp = mkBtn(L.t("SADECE KOMUTU KOPYALA"));
        cp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MainActivity.this.doPatch(false); }
        });
        this.patchP.addView(cp);
        this.patchP.addView(backBtn());
        this.container.addView(this.patchP);
    }

    private void copySetup() {
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
            toast(L.t("Depolama iznini ver, sonra tekrar bas"));
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Kit.export(MainActivity.this);
                    final String cmd = Kit.setupCommand();
                    MainActivity.this.h.post(new Runnable() {
                        @Override public void run() {
                            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("cmd", cmd));
                            MainActivity.this.logv.setText(L.t("Panoya kopyalandi. Termux'a yapistir:") + "\n" + cmd + "\n");
                            toast(L.t("Kurulum komutu kopyalandi, Termux'a yapistir"));
                        }
                    });
                } catch (final Exception e) {
                    MainActivity.this.h.post(new Runnable() {
                        @Override public void run() { toast(L.t("Kit hazirlanamadi: ") + e.getMessage()); }
                    });
                }
            }
        }).start();
    }

    private void doPatch(final boolean run) {
        if (this.patchIn == null) {
            toast(L.t("Once uygulama dosyasi sec"));
            return;
        }
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
            toast(L.t("Depolama iznini ver, sonra tekrar bas"));
            return;
        }
        final String args = this.edPatchArgs.getText().toString().trim().length() == 0 ? Prefs.DEF_ARGS : this.edPatchArgs.getText().toString().trim();
        final int port = Reg.nextPort(this);
        this.logv.setText("");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Kit.export(MainActivity.this);
                } catch (final Exception e) {
                    MainActivity.this.h.post(new Runnable() {
                        @Override public void run() { toast(L.t("Kit hazirlanamadi: ") + e.getMessage()); }
                    });
                    return;
                }
                final String cmd = Kit.command(MainActivity.this.patchIn, port, args);
                MainActivity.this.h.post(new Runnable() {
                    @Override public void run() {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("cmd", cmd));
                        MainActivity.this.logv.append(L.t("Komut panoya kopyalandi, Termux'a yapistir:") + "\n" + cmd + "\n");
                        if (!run) { toast(L.t("Komut kopyalandi")); return; }
                        if (checkSelfPermission("com.termux.permission.RUN_COMMAND") != 0) {
                            requestPermissions(new String[]{"com.termux.permission.RUN_COMMAND"}, 7);
                            toast(L.t("Termux izni istendi, verip tekrar bas (olmazsa komutu elle yapistir)"));
                            return;
                        }
                        try {
                            Kit.runInTermux(MainActivity.this, Kit.argv(MainActivity.this.patchIn, port, args));
                            toast(L.t("Termux'ta basladi, cikti orada gorunur. Bitince Download klasorune -dpi.apk duser."));
                        } catch (Throwable t) {
                            toast(L.t("Termux baslatilamadi (allow-external-apps=true gerekir). Komut kopyalandi, elle yapistir."));
                        }
                    }
                });
            }
        }).start();
    }

    private void importPatchFile(final Intent data) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    android.net.Uri u = data.getData();
                    String name = "uygulama.apk";
                    android.database.Cursor c = getContentResolver().query(u, null, null, null, null);
                    if (c != null) {
                        int ix = c.getColumnIndex("_display_name");
                        if (ix >= 0 && c.moveToFirst()) name = c.getString(ix);
                        c.close();
                    }
                    name = name.replaceAll("[^A-Za-z0-9._-]", "_");
                    File dst = new File(Kit.inDir(), name);
                    dst.getParentFile().mkdirs();
                    InputStream in = getContentResolver().openInputStream(u);
                    FileOutputStream out = new FileOutputStream(dst);
                    byte[] b = new byte[65536];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    out.close();
                    in.close();
                    final File fd = dst;
                    MainActivity.this.h.post(new Runnable() {
                        @Override public void run() {
                            MainActivity.this.patchIn = fd.getAbsolutePath();
                            MainActivity.this.patchInfo.setText(fd.getName() + "  (" + (fd.length() / 1024 / 1024) + " MB)");
                        }
                    });
                } catch (final Exception e) {
                    MainActivity.this.h.post(new Runnable() {
                        @Override public void run() { toast(L.t("Dosya okunamadi: ") + e); }
                    });
                }
            }
        }).start();
    }

    private void buildUrlPanel() {
        this.urlP = panel();
        this.urlP.addView(mkText(L.t("--- URL ILE APK YAP ---"), 14, -11751600));
        this.edUrl = mkEdit("https://site-adresi.com", false);
        this.urlP.addView(this.edUrl);
        this.edName = mkEdit(L.t("Uygulama adi (bos = siteden al)"), false);
        this.urlP.addView(this.edName);
        this.chkDpi = new CheckBox(this);
        this.chkDpi.setText(L.t("Masaustu modu (uygulama icinde M/D tusundan degisir)"));
        this.chkDpi.setTextColor(-1118482);
        this.chkDpi.setChecked(true);
        this.urlP.addView(this.chkDpi);
        this.chkBye = new CheckBox(this);
        this.chkBye.setText(L.t("DPI engelini as (ByeDPI gomulur, port otomatik 1082, 1083...)"));
        this.chkBye.setTextColor(-1118482);
        this.urlP.addView(this.chkBye);
        this.chkVid = new CheckBox(this);
        this.chkVid.setText(L.t("Sitedeki videoyu indir dugmesi (Video / Ses)"));
        this.chkVid.setTextColor(-1118482);
        this.urlP.addView(this.chkVid);
        this.edBye = mkEdit(L.t("ByeDPI argumanlari"), false);
        this.edBye.setText("--disorder 1 --tlsrec 1+s");
        this.urlP.addView(this.edBye);
        this.urlP.addView(iconRow(true));
        Button mkBtn = mkBtn(L.t("APK YAP"));
        mkBtn.setOnClickListener(new View.OnClickListener() { // from class: tr.apkmaker.MainActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.doWeb();
            }
        });
        this.urlP.addView(mkBtn);
        this.urlP.addView(backBtn());
        this.container.addView(this.urlP);
    }

    private CheckBox byeCheck(LinearLayout panel) {
        CheckBox cb = new CheckBox(this);
        cb.setText(L.t("DPI engelini as (ByeDPI gomulur, port otomatik 1082, 1083...)"));
        cb.setTextColor(-1118482);
        panel.addView(cb);
        return cb;
    }

    private EditText byeEdit(LinearLayout panel) {
        EditText e = mkEdit(L.t("ByeDPI argumanlari"), false);
        e.setText("--disorder 1 --tlsrec 1+s");
        panel.addView(e);
        return e;
    }

    private void buildCodePanel() {
        this.codeP = panel();
        this.codeP.addView(mkText(L.t("--- KOD ILE APK YAP ---\nJava: class MainActivity (packagesiz)\nCok dosya: aralara '// FILE: Ad.java' satiri\nHTML5: asagidaki kutuyu isaretle"), 13, -11751600));
        this.edCodeName = mkEdit(L.t("Uygulama adi (bos = KodApp / HtmlApp)"), false);
        this.codeP.addView(this.edCodeName);
        this.edCode = mkEdit(L.t("Kodu buraya yapistir..."), true);
        this.chkHtml = new CheckBox(this);
        this.chkHtml.setText(L.t("HTML5 modu (icerik index.html olarak islenir)"));
        this.chkHtml.setTextColor(-1118482);
        this.codeP.addView(this.chkHtml);
        this.chkByeCode = byeCheck(this.codeP);
        this.edByeCode = byeEdit(this.codeP);
        this.codeP.addView(iconRow(false));
        Button mkBtn = mkBtn(L.t("KODDAN APK YAP"));
        mkBtn.setOnClickListener(new View.OnClickListener() { // from class: tr.apkmaker.MainActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.doCode();
            }
        });
        this.codeP.addView(mkBtn);
        this.codeP.addView(backBtn());
        // Uzun kod yapistirilsa da dugmeler yukarida kalir; kutu sabit yukseklikte, kendi icinde kayar
        this.codeP.addView(this.edCode);
        this.container.addView(this.codeP);
    }

    private void buildFilePanel() {
        this.fileP = panel();
        this.fileP.addView(mkText(L.t("--- DOSYADAN APK YAP ---\nJava (.java), HTML5 (.html/.css/.js...), .zip veya icinde kod olan .txt sec.\nTur otomatik algilanir."), 13, -11751600));
        this.edFileName = mkEdit(L.t("Uygulama adi (bos = otomatik)"), false);
        this.fileP.addView(this.edFileName);
        this.chkByeFile = byeCheck(this.fileP);
        this.edByeFile = byeEdit(this.fileP);
        this.fileP.addView(iconRow(false));
        Button pick = mkBtn(L.t("DOSYA SEC  (birden fazla secilebilir)"));
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent("android.intent.action.OPEN_DOCUMENT");
                i.addCategory("android.intent.category.OPENABLE");
                i.setType("*/*");
                i.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
                MainActivity.this.startActivityForResult(i, 3);
            }
        });
        this.fileP.addView(pick);
        this.fileInfo = mkText(L.t("Henuz dosya secilmedi"), 13, -2236963);
        this.fileP.addView(this.fileInfo);
        Button go = mkBtn(L.t("DOSYADAN APK YAP"));
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.doFile();
            }
        });
        this.fileP.addView(go);
        this.fileP.addView(backBtn());
        this.container.addView(this.fileP);
    }

    private void importFiles(final Intent data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.util.ArrayList<android.net.Uri> uris = new java.util.ArrayList<android.net.Uri>();
                    if (data.getClipData() != null) {
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            uris.add(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        uris.add(data.getData());
                    }
                    File dir = new File(getFilesDir(), "import");
                    Detect.clear(dir);
                    dir.mkdirs();
                    for (int i = 0; i < uris.size(); i++) {
                        String name = "dosya" + i;
                        android.database.Cursor c = getContentResolver().query(uris.get(i), null, null, null, null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                int ix = c.getColumnIndex("_display_name");
                                if (ix >= 0 && c.getString(ix) != null) {
                                    name = c.getString(ix);
                                }
                            }
                            c.close();
                        }
                        name = new File(name).getName();
                        InputStream in = getContentResolver().openInputStream(uris.get(i));
                        FileOutputStream out = new FileOutputStream(new File(dir, name));
                        byte[] buf = new byte[16384];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                        }
                        in.close();
                        out.close();
                    }
                    final Detect.Result r = Detect.scan(dir);
                    MainActivity.this.h.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.detected = r;
                            MainActivity.this.fileInfo.setText(r.summary);
                            if (r.error == null && MainActivity.this.edFileName.getText().toString().trim().length() == 0) {
                                MainActivity.this.edFileName.setText(r.name);
                            }
                        }
                    });
                } catch (final Exception e) {
                    MainActivity.this.h.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.toast(L.t("Dosya hatasi: ") + e);
                        }
                    });
                }
            }
        }).start();
    }

    private void doFile() {
        if (this.detected == null || this.detected.error != null) {
            toast(L.t("Once gecerli dosya sec"));
            return;
        }
        String name = this.edFileName.getText().toString().trim();
        if (name.length() == 0) {
            name = this.detected.name;
        }
        this.logv.setText("");
        Builder.buildProject(this, name, this.detected, this.chkByeFile.isChecked(), this.edByeFile.getText().toString(), this.iconFile, cb());
    }

    private View iconRow(boolean web) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        Button mkBtn = mkBtn(L.t("IKON SEC (galeri)"));
        mkBtn.setOnClickListener(new View.OnClickListener() { // from class: tr.apkmaker.MainActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.startActivityForResult(new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 2);
            }
        });
        linearLayout.addView(mkBtn, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.iconInfo = new TextView(this);
        this.iconInfo.setText(web ? L.t("ikon: sitenin simgesi (otomatik)") : L.t("ikon: varsayilan"));
        this.iconInfos.add(this.iconInfo);
        this.iconInfo.setTextSize(12.0f);
        this.iconInfo.setTextColor(-6381922);
        this.iconInfo.setGravity(16);
        linearLayout.addView(this.iconInfo, new LinearLayout.LayoutParams(0, -2, 1.0f));
        return linearLayout;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 3 && i2 == -1 && intent != null) {
            importFiles(intent);
        }
        if (i == 4 && i2 == -1 && intent != null) {
            importPatchFile(intent);
        }
        if (i == 2 && i2 == -1 && intent != null) {
            try {
                InputStream openInputStream = getContentResolver().openInputStream(intent.getData());
                Bitmap decodeStream = BitmapFactory.decodeStream(openInputStream);
                openInputStream.close();
                if (decodeStream == null) {
                    toast(L.t("Ikon okunamadi (gecersiz resim)"));
                    return;
                }
                decodeStream = Builder.squareIcon(decodeStream);
                this.iconFile = new File(getFilesDir(), "icon.png");
                FileOutputStream fileOutputStream = new FileOutputStream(this.iconFile);
                decodeStream.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.close();
                for (TextView t : this.iconInfos) {
                    t.setText(L.t("ikon: secildi (kirpildi)"));
                }
                toast(L.t("Ikon secildi"));
            } catch (Exception e) {
                toast(L.t("Ikon hatasi: ") + e);
            }
        }
    }

    private LinearLayout panel() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        return linearLayout;
    }

    private Button backBtn() {
        Button mkBtn = mkBtn(L.t("<- GERI"));
        mkBtn.setOnClickListener(new View.OnClickListener() { // from class: tr.apkmaker.MainActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.show("main");
            }
        });
        return mkBtn;
    }

    public void showHistory() {
        float dn = getResources().getDisplayMetrics().density;
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(1);
        list.setPadding(16, 16, 16, 16);
        list.addView(mkText(L.t("URETILEN UYGULAMALAR"), 13, -11751600));
        java.util.List<String[]> es = Reg.entries(this);
        if (es.isEmpty()) {
            list.addView(mkText(L.t("(henuz uretim yok)"), 13, -2236963));
        }
        for (String[] e : es) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(0);
            row.setGravity(16);
            row.setPadding(0, (int) (6 * dn), 0, (int) (6 * dn));
            android.widget.ImageView iv = new android.widget.ImageView(this);
            Bitmap bm = e[3].length() > 0 ? BitmapFactory.decodeFile(e[3]) : null;
            if (bm != null) {
                iv.setImageBitmap(bm);
            } else {
                iv.setImageDrawable(new ColorDrawable(-14575168));
            }
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            row.addView(iv, new LinearLayout.LayoutParams((int) (48 * dn), (int) (48 * dn)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(1);
            col.setPadding((int) (12 * dn), 0, 0, 0);
            TextView nm = new TextView(this);
            nm.setText(e[0]);
            nm.setTextSize(16.0f);
            nm.setTextColor(-1);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            col.addView(nm);
            int code = 0;
            try {
                code = Integer.parseInt(e[2]);
            } catch (Exception ex) {
            }
            TextView sub = new TextView(this);
            sub.setText((code > 0 ? L.t("Surum ") + Reg.versionName(code) : L.t("Surum -")) + "   " + e[1]);
            sub.setTextSize(11.0f);
            sub.setTextColor(-6381922);
            col.addView(sub);
            row.addView(col);
            list.addView(row);
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list);
        PopupWindow[] popupWindowArr = {new PopupWindow((View) scrollView, -1, (getResources().getDisplayMetrics().heightPixels * 70) / 100, true)};
        popupWindowArr[0].setBackgroundDrawable(new ColorDrawable(-233498594));
        popupWindowArr[0].setOutsideTouchable(true);
        popupWindowArr[0].showAtLocation(this.status, 80, 0, 0);
    }

    private TextView mkText(String str, int i, int i2) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        textView.setPadding(0, (int) (8.0f * getResources().getDisplayMetrics().density), 0, 4);
        return textView;
    }

    private EditText mkEdit(String str, boolean z) {
        EditText editText = new EditText(this);
        editText.setHint(str);
        editText.setTextColor(-1);
        editText.setHintTextColor(-8947849);
        if (z) {
            editText.setMinLines(8);
            editText.setMaxLines(12);
            editText.setVerticalScrollBarEnabled(true);
            editText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
                    // kutunun icinde kaydirirken sayfa kaymasin
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    if (motionEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return false;
                }
            });
            editText.setGravity(48);
            editText.setTypeface(Typeface.MONOSPACE);
        }
        return editText;
    }

    private Button mkBtn(String str) {
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, (int) (4.0f * getResources().getDisplayMetrics().density), 0, 0);
        button.setLayoutParams(layoutParams);
        return button;
    }

    private static boolean isNoise(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.length() == 0) return true;
        return t.contains("ResourceType") || t.contains("CREATING STRING CACHE") || t.startsWith("Picked up")
            || (t.startsWith("'") && t.endsWith("'...")) || t.startsWith("--- aapt") || t.startsWith("TOOLS:") || t.startsWith("LIB:");
    }

    public Builder.Cb cb() {
        return new Builder.Cb() { // from class: tr.apkmaker.MainActivity.9
            @Override // tr.apkmaker.Builder.Cb
            public void log(final String str) {
                if (isNoise(str)) {
                    return;
                }
                MainActivity.this.h.post(new Runnable() { // from class: tr.apkmaker.MainActivity.9.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.logv.append(String.valueOf(str) + "\n");
                        MainActivity.this.logScroll.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.logScroll.fullScroll(130);
                            }
                        });
                    }
                });
            }

            @Override // tr.apkmaker.Builder.Cb
            public void status(final String str) {
                MainActivity.this.h.post(new Runnable() { // from class: tr.apkmaker.MainActivity.9.2
                    @Override // java.lang.Runnable
                    public void run() {
                        MainActivity.this.status.setText(str);
                    }
                });
            }

            @Override // tr.apkmaker.Builder.Cb
            public void done(final boolean z, final String str) {
                MainActivity.this.h.post(new Runnable() { // from class: tr.apkmaker.MainActivity.9.3
                    @Override // java.lang.Runnable
                    public void run() {
                        if (z && str != null) {
                            MainActivity.this.status.setText("OK: " + str);
                            MainActivity.this.toast(L.t("APK hazir: ") + str + L.t("  (Dosyalar > Download)"));
                        } else if (z) {
                            MainActivity.this.status.setText(L.t("Test tamam"));
                        } else {
                            MainActivity.this.status.setText(L.t("BASARISIZ (log'a bak)"));
                        }
                    }
                });
            }
        };
    }

    public void doWeb() {
        String trim = this.edUrl.getText().toString().trim();
        if (trim.startsWith("http")) {
            String trim2 = this.edName.getText().toString().trim();
            if (trim2.length() == 0) {
                trim2 = trim.replace("https://", "").replace("http://", "").split("/")[0];
            }
            this.logv.setText("");
            Builder.buildWeb(this, trim, trim2, this.chkDpi.isChecked(), this.chkBye.isChecked(), this.edBye.getText().toString(), this.chkVid.isChecked(), this.iconFile, cb());
            return;
        }
        toast(L.t("URL http ile baslamali"));
    }

    public void doCode() {
        String editable = this.edCode.getText().toString();
        String trim = this.edCodeName.getText().toString().trim();
        if (trim.length() == 0) {
            trim = this.chkHtml.isChecked() ? "HtmlApp" : "KodApp";
        }
        if (editable.trim().length() >= 20) {
            this.logv.setText("");
            if (this.chkHtml.isChecked()) {
                Builder.buildHtml(this, trim, editable, this.chkByeCode.isChecked(), this.edByeCode.getText().toString(), this.iconFile, cb());
                return;
            } else if (!editable.contains("class MainActivity") && !editable.contains("// FILE:")) {
                toast(L.t("Java kodda 'class MainActivity' olmali"));
                return;
            } else {
                Builder.buildCode(this, trim, editable, this.chkByeCode.isChecked(), this.edByeCode.getText().toString(), this.iconFile, cb());
                return;
            }
        }
        toast(L.t("Kod/HTML bos gorunuyor"));
    }

    public void toast(String str) {
        Toast.makeText(this, str, 1).show();
    }
}
