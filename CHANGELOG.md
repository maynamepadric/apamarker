# Değişiklikler

## 1.6.2
- Yamalayıcı: "TERMUX KURULUM KOMUTUNU KOPYALA" düğmesi. `setup.sh` tek seferde paketleri, APKEditor.jar'ı, `allow-external-apps` ayarını ve depolama iznini hazırlar. `scripts/setup.sh` olarak da var.

## 1.6.1
- Ayarlar: "APK Maker'in kendi DPI'si". Açıkken site görseli (favicon) indirilirken yerel ByeDPI kullanılır, engelli sitelerin simgesi de inebilir (`SelfDpi`). Araç Seti'nde durumu görünür.

## 1.6.0
- Sağ üstte ayar (dişli) simgesi: dil, varsayılan DPI/argümanlar/video indirme/masaüstü modu, günlük alanı aç-kapa, Geçmiş ve Araç Seti. Tüm ayarlar kalıcı (`Prefs`), paneller arası senkron.
- Araç Seti paneli: aapt ve ByeDPI gerçekten çalıştırılarak test edilir, diğer araçların durumu renkli satırlarla düz biçimde gösterilir.
- Ayrı YAMALAYICI paneli: dosya seç, Termux'ta `patch.sh` ile yamala (RUN_COMMAND), olmazsa komut panoya kopyalanır. Kit `Download/APKMakerKit` klasörüne çıkarılır.
- Alt günlükten aapt/araç gürültüsü ayıklandı; başlıktaki sürüm gerçek sürümden okunur.
- Video indirme: ağ trafiğinden video adresi yakalama (mp4/webm/m3u8/mp3...), tek "Indir" düğmesi + liste, HLS (m3u8) indirme (AES-128, fMP4, paralel parça, mp4'e birleştirme), ses ayırma m4a.

## 1.5.0
- URL ile APK: "Sitedeki videoyu indir dugmesi": sayfadaki video/ses icin Video indir ve Ses indir dugmeleri (ses, m4a olarak ayrilir). DPI aciksa indirme de ByeDPI uzerinden gecer. blob/HLS/DRM'li videolar desteklenmez.
- "DPI engelini aş" artık **Kod ile APK** (Java ve HTML5) ve **Dosyadan APK** bölümlerinde de var (`Builder.addDpi`). `dpi.Boot` + `dpi.App` projeye eklenir, manifestte `android:name="dpi.App"` olur; HTML5 uygulamalarında WebView istekleri de SOCKS5 üzerinden alınır.
- Yerel adresler (localhost, 127.x, 10.x, 192.168.x) proxy dışında bırakılır.
- `scripts/patch.sh`: Termux'ta mevcut bir APK/.apkm'ye (Discord vb.) ByeDPI gömen otomatik yamalayıcı (birleştir, aç, hook, derle, dex ekle, imzala).
- `scripts/build.sh`: Termux'ta APK Maker'ı derler.

## 1.4.0
- URL ile APK: "DPI engelini aş" seçeneği. ByeDPI (`ciadpi`) üretilen APK'nın içine `lib/arm64-v8a/libciadpi.so` olarak gömülür, uygulama açılırken yerel SOCKS5 sunucusu olarak başlar.
- Port sayacı: her DPI'lı uygulamaya sırayla 1082, 1083, 1084... verilir (`Reg.nextPort`). Çalışırken port doluysa uygulama sonraki boş portu seçer.
- WebView proxy ayarını okumadığı için GET/HEAD/OPTIONS istekleri `shouldInterceptRequest` ile SOCKS5 üzerinden çekilir (`MainWeb.java`). POST gövdesi bu yolla alınamaz.
- Gerekli: `app/src/main/assets/ciadpi.bin` (aarch64 `ciadpi` binary'si, ByeDPI MIT lisanslı).

## 1.3.2
- Çoklu dil: Türkçe, English, Deutsch, Русский. Ana ekrandaki "Dil / Language" düğmesiyle değişir, seçim saklanır (`L.java`; anahtar = Türkçe metin, yeni dil eklemek için tabloya sütun eklenir).

## 1.3.1
- Üretilen web/HTML uygulamaları mikrofon izni isteyip verebilir (`RECORD_AUDIO`, web sayfasının `getUserMedia` isteği).
- Uygulama içi indirme: bağlantıya tıklanınca dosya doğrudan Download klasörüne iner (DownloadManager; `blob:` ve `data:` bağlantıları da desteklenir).

## 1.3
- Dosyadan APK: çoklu dosya / zip seç, Java ve HTML5 otomatik algılanır (`.txt` içine bakılır, Java dosya adı `public class` adına uyarlanır, zip içindeki `_next` gibi alt çizgili klasörler korunur).
- Galeriden seçilen ikon otomatik ortadan kare kırpılır ve 192 px yapılır.
- Geçmiş: uygulama adı, sürüm ve ikon. Aynı adla yeniden üretince sürüm artar (versionCode/versionName), güncelleme olarak kurulur.

## 1.2.10
- Kod ekranında uzun kod düğmeleri itmiyor (düğmeler üstte, kod kutusu sabit yükseklikte, sayfa kaydırılabilir).

## 1.2.9
- Web uygulamasında üst araç çubuğu (geri/ileri/yenile/mod); aşağı çekince yenileme kaldırıldı.
- URL uygulamasına sitenin simgesi otomatik alınıyor; paket adı karma kodu 8 karakter.

## 1.2.8
- Bağımsız v1 APK imzalayıcı (`ApkSign`).

## 1.2.7
- `dx` ve imzalama uygulama içinde.

## 1.2.6
- ecj çıktı klasörü mutlak yol (`/obj` hatası).

## 1.2.5
- `__PKG__` şablon hatası, sertifika (DER NULL, geçerlilik tarihi), paket adı kuralı, kod modu ad alanı, UTF-8, kayıt yedekleri, şablon düzeltmeleri.
