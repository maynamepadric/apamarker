# APK Maker

Telefonun üzerinde, bilgisayar olmadan APK üreten Android uygulaması.

- **URL ile APK:** Bir web sitesini WebView uygulamasına çevirir (masaüstü/mobil mod, üst çubukta geri/ileri/yenile, tam ekran video, dosya seçici, konum izni, sitenin simgesi otomatik alınır).
- **Kod ile APK:** Yapıştırılan Java (tek veya `// FILE: Ad.java` ile çok dosyalı) ya da HTML5 kodundan APK üretir.
- **Dosyadan APK:** `.java`, `.html`, `.zip` veya içinde kod olan `.txt` dosyalarını seç; tür otomatik algılanır (`Detect.java`).
- **Geçmiş:** Üretilen uygulamaların adı, sürümü ve simgesi.
- **DPI engelini aş (ByeDPI):** Üretilen uygulamaya `ciadpi` gömülür, trafik yerel SOCKS5 üzerinden geçer (VPN gerekmez). Port 1082'den başlayıp sırayla verilir.
- **Video/ses indirme düğmesi (URL uygulamaları):** Ağ trafiğinden video adreslerini yakalar, HLS (m3u8) indirir, sesi m4a olarak ayırır.
- **Yamalayıcı (Termux ile):** Mevcut bir uygulamaya (OkHttp/Java tabanlı) ByeDPI gömer (`scripts/patch.sh`).
- **Ayarlar (dişli):** Dil, varsayılanlar, Araç Seti durumu, geçmiş. Ayarlar kalıcıdır.

Sürüm: **1.6.2** (değişiklikler için CHANGELOG.md)

## Nasıl çalışıyor

Uygulama ilk açılışta `assets/toolchain.dat` (tar.gz) içindeki araçları `filesDir/tools` altına çıkarır:

| Araç | Görev | Nasıl çalışır |
|---|---|---|
| `aapt` (aarch64) | manifest + kaynakları paketler | alt süreç |
| `ecj.jar` | Java derleme | uygulama içinde (`PathClassLoader`) |
| `dx.jar` | `.class` → `classes.dex` | uygulama içinde (`PathClassLoader`) |
| `android.jar` (API 27) | derleme sınıf yolu | — |
| `apksigner.jar` | **artık kullanılmıyor** (cihazda lambda hatası veriyor) | — |

İmzalama `ApkSign.java` ile yapılır (bağımsız v1/JAR imzası, `CertGen.java` ile üretilen anahtarla).
Her uygulama kendi paket adını (`Reg.java`) ve kendi anahtarını (`filesDir/keys/<paket>.keystore`) alır; aynı adla tekrar üretirsen aynı paket + anahtar kullanıldığı için güncelleme olarak kurulur.

Akış (`Builder.compile`): `aapt package` → `ecj` → `dx` → `aapt add classes.dex` → `ApkSign` → `/sdcard/Download`.

> Neden `dx`/`ecj` uygulama içinde? Bazı cihazlarda `dalvikvm` alt süreci `134 (SIGABRT)` ile çöküyor. Alt süreç yalnızca `aapt` için kullanılıyor. `ecj` aynı süreçte çalıştığından **mutlak yol** kullanılmalı (göreli yollar `/` dizinine çözülür).

## ByeDPI ikilisi (DPI özellikleri için)

`app/src/main/assets/ciadpi.bin` depoda yoktur. Kendin derle (Termux, aarch64):

```bash
pkg install git clang make
git clone https://github.com/hufrea/byedpi && cd byedpi && make
cp ciadpi /yol/APKMaker/app/src/main/assets/ciadpi.bin
```

Dosya yoksa DPI seçenekleri hata verir, diğer özellikler çalışır.

## Termux'ta derleme (Android Studio olmadan)

```bash
pkg install ecj dx aapt apksigner openjdk-21
./scripts/build.sh            # çıktı: ~/storage/downloads/APKMaker-<sürüm>.apk
```

Kendi imza anahtarın için: `KS=~/anahtar.keystore KS_ALIAS=ad KS_PASS=sifre ./scripts/build.sh`
(Anahtarı ve şifreyi **depoya koyma**; `.gitignore` `*.keystore` dosyalarını dışlar.)
Aynı anahtarla imzalanmayan sürüm, kurulu olanın üstüne güncelleme olarak kurulmaz.

## Derleme (Android Studio)

Android Studio ile `APKMaker` klasörünü açın (AGP 8.x, JDK 17). Gradle wrapper dosyaları repoda yok; Android Studio oluşturur.

- `targetSdk 28` **bilerek**: araçlar `filesDir`'den çalıştırılıyor, `targetSdk 29+` buna izin vermez.
- `toolchain.dat` ~7 MB ikili dosyadır: içindeki `aapt`, `dx`, `ecj` ve `android.jar` üçüncü taraf yazılımlardır, kendi lisanslarına tabidir (Apache-2.0 / EPL-2.0).

> Not: Bu depo, yayımlanan 1.3 APK'sından geri çevrilen (decompile) kaynak üzerine kuruludur; bazı değişken adları (`str`, `i`, `linearLayout`...) otomatik üretilmiştir. Kodun büyük bölümü yapay zeka yardımıyla yazılmıştır.

## Uyarılar

- Başkasının sitesini/markasını/içeriğini paketleyip dağıtmak telif ve marka sorunu doğurabilir; sorumluluk kullanıcıdadır.
- Modlu istemciler (yamalanmış uygulamalar) ilgili hizmetin kullanım şartlarına aykırı olabilir.
- DPI atlatma yöntemi operatöre göre değişir, her hatta çalışmayabilir.

## Lisans

Kendi kodumuz MIT (LICENSE). Üçüncü taraf bileşenler için NOTICE.md'ye bak.

## Şablonlar

`app/src/main/assets/template/` altındaki `MainWeb.java`, `MainHtml.java`, `AndroidManifest.xml`, `ic_app.xml`; `__PKG__`, `__URL__`, `__DESKTOP__`, `__APPNAME__`, `__ICON__` yer tutucuları üretim sırasında doldurulur.

## Bilinen sınırlar

- Üretilen uygulamalar v1 (JAR) imzalıdır, hedef API 28.
- Anahtarlar uygulamanın verisindedir; APK Maker kaldırılırsa eski uygulamalara güncelleme yapılamaz.
- `res/` içeren Android Studio projeleri desteklenmez.

## Lisans

Henüz seçilmedi (yayımlamadan önce `LICENSE` ekleyin).
