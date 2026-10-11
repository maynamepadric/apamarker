#!/data/data/com.termux/files/usr/bin/bash
# APK Maker'i Termux'ta derler: ecj -> dx -> aapt -> apksigner
# Kurulum:  pkg install ecj dx aapt apksigner openjdk-21
# Kullanim: ./scripts/build.sh            (paket: tr.apkmaker)
#           PKG=tr.apkmaker.dpi ./scripts/build.sh   (eski surumun YANINDA kurulur)
set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
PKG=${PKG:-tr.apkmaker}
VNAME=1.6.2; VCODE=17
B=$ROOT/build
KS=${KS:-$HOME/apkmaker.keystore}

for c in ecj dx aapt apksigner keytool tar sed; do
  command -v $c >/dev/null || { echo "EKSIK: $c  ->  pkg install ecj dx aapt apksigner openjdk-21"; exit 1; }
done
[ -f app/src/main/assets/ciadpi.bin ] || echo "UYARI: assets/ciadpi.bin yok, 'DPI engelini as' calismaz."

rm -rf "$B"; mkdir -p "$B/cls" "$B/tc"
echo "[1/5] android.jar cikariliyor"
tar xzf app/src/main/assets/toolchain.dat -C "$B/tc" toolchain/share/android.jar
AJ=$B/tc/toolchain/share/android.jar

echo "[2/5] ecj derliyor"
ecj -proc:none -encoding UTF-8 -nowarn -d "$B/cls" -cp "$AJ" $(find app/src/main/java -name '*.java')

echo "[3/5] dx dexliyor"
dx --dex --output="$B/classes.dex" "$B/cls"

echo "[4/5] aapt paketliyor"
sed -e 's#<manifest xmlns:android="http://schemas.android.com/apk/res/android">#<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'$PKG'" android:versionCode="'$VCODE'" android:versionName="'$VNAME'"><uses-sdk android:minSdkVersion="24" android:targetSdkVersion="28"/>#' \
    -e 's#android:name="\.MainActivity"#android:name="tr.apkmaker.MainActivity"#' \
    app/src/main/AndroidManifest.xml > "$B/AndroidManifest.xml"
aapt package -f -M "$B/AndroidManifest.xml" -S app/src/main/res -A app/src/main/assets \
     -I /system/framework/framework-res.apk -0 dat -0 keystore -F "$B/unsigned.apk"
( cd "$B" && aapt add -f unsigned.apk classes.dex )

echo "[5/5] imzalaniyor"
[ -f "$KS" ] || keytool -genkey -keystore "$KS" -alias k -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass 123456 -keypass 123456 -dname "CN=apkmaker"
apksigner sign --ks "$KS" --ks-key-alias "${KS_ALIAS:-k}" --ks-pass "pass:${KS_PASS:-123456}" --key-pass "pass:${KS_PASS:-123456}" --out "$B/APKMaker-$VNAME.apk" "$B/unsigned.apk"

mkdir -p ~/storage/downloads 2>/dev/null || true
cp "$B/APKMaker-$VNAME.apk" ~/storage/downloads/ && echo "TAMAM: Downloads/APKMaker-$VNAME.apk (paket: $PKG)"
