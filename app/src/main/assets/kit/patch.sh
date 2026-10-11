#!/data/data/com.termux/files/usr/bin/bash
# Mevcut bir uygulamaya (Discord vb.) ByeDPI gomer.
# Kurulum (bir kere): pkg install apktool ecj dx aapt apksigner openjdk-21   (+ ~/APKEditor.jar, .apkm/.xapk icin)
# Kullanim: patch.sh uygulama.apk|.apkm|.xapk [port] ["ciadpi argumanlari"]
#   Iki modda calisir: APKMakerKit klasorunden (DpiBoot.java yaninda) ya da proje icindeki scripts/ klasorunden.
set -e
IN=$(readlink -f "${1:?Kullanim: patch.sh dosya.apk|apkm|xapk [port] [argumanlar]}")
PORT=${2:-1082}
ARGS=${3:---disorder 1 --tlsrec 1+s}
HERE=$(cd "$(dirname "$0")" && pwd)
KS=${KS:-$HOME/patcher.keystore}; KS_ALIAS=${KS_ALIAS:-k}; KS_PASS=${KS_PASS:-123456}
export _JAVA_OPTIONS=-Xmx2g

if [ -f "$HERE/DpiBoot.java" ]; then
  BOOT=$HERE/DpiBoot.java; APPJ=$HERE/DpiApp.java; AJ=$HERE/android.jar
  CIADPI=${CIADPI:-$HERE/ciadpi}; W=$HOME/patchwork
else
  ROOT=$(cd "$HERE/.." && pwd)
  BOOT=$ROOT/app/src/main/assets/template/DpiBoot.java; APPJ=$ROOT/app/src/main/assets/template/DpiApp.java
  CIADPI=${CIADPI:-$HOME/byedpi/ciadpi}; W=$ROOT/patchwork; AJ=$W/android.jar
fi
OUTDIR=${OUTDIR:-/sdcard/Download}; [ -d "$OUTDIR" ] || OUTDIR=$HOME/storage/downloads

for c in apktool ecj dx aapt apksigner keytool unzip; do
  command -v $c >/dev/null || { echo "EKSIK: $c  ->  pkg install apktool ecj dx aapt apksigner openjdk-21"; exit 1; }
done
[ -f "$CIADPI" ] || { echo "ciadpi bulunamadi: $CIADPI"; exit 1; }
rm -rf "$W"; mkdir -p "$W"
if [ ! -f "$AJ" ]; then
  tar xzf "$ROOT/app/src/main/assets/toolchain.dat" -C "$W" toolchain/share/android.jar && mv "$W/toolchain/share/android.jar" "$AJ"
fi

echo "[1/7] girdi hazirlaniyor"
case "$IN" in
  *.apkm|*.xapk|*.apks)
    [ -f "$HOME/APKEditor.jar" ] || { echo "~/APKEditor.jar yok (apkm/xapk icin gerekli)"; exit 1; }
    java -jar "$HOME/APKEditor.jar" m -i "$IN" -o "$W/in.apk" ;;
  *) cp "$IN" "$W/in.apk" ;;
esac

echo "[2/7] apktool aciyor (buyuk uygulamada birkac dakika)"
D=$W/d
apktool d -f -j 2 "$W/in.apk" -o "$D"

echo "[3/7] hook ekleniyor"
APP=$(grep -o '<application[^>]*>' "$D/AndroidManifest.xml" | grep -o 'android:name="[^"]*"' | head -1 | sed 's/android:name="//;s/"$//')
if [ -z "$APP" ]; then
  APP=dpi.App
  sed -i '0,/<application /s//<application android:name="dpi.App" /' "$D/AndroidManifest.xml"
  echo "  Application sinifi yok, dpi.App eklendi"
else
  case "$APP" in .*) PK=$(grep -o 'package="[^"]*"' "$D/AndroidManifest.xml" | head -1 | sed 's/package="//;s/"$//'); APP=$PK$APP;; esac
  SM=$(find "$D" -path "*/$(echo "$APP" | tr . /).smali" | head -1)
  [ -n "$SM" ] || { echo "Application smali bulunamadi: $APP"; exit 1; }
  echo "  Application: $APP"
  HOOK='    invoke-static {p1}, Ldpi/Boot;->start(Landroid/content/Context;)V'
  N=$(grep -n 'invoke-super.*->attachBaseContext(Landroid/content/Context;)V' "$SM" | head -1 | cut -d: -f1)
  if [ -n "$N" ]; then
    sed -i "${N}a\\
$HOOK" "$SM"
    echo "  attachBaseContext icine eklendi (satir $N)"
  else
    SUP=$(grep -m1 '^\.super' "$SM" | sed 's/^\.super //')
    cat >> "$SM" <<SMALI

.method protected attachBaseContext(Landroid/content/Context;)V
    .locals 0
    invoke-super {p0, p1}, $SUP->attachBaseContext(Landroid/content/Context;)V
$HOOK
    return-void
.end method
SMALI
    echo "  attachBaseContext yeni eklendi (super: $SUP)"
  fi
fi
if grep -q 'android:extractNativeLibs=' "$D/AndroidManifest.xml"; then
  sed -i 's/android:extractNativeLibs="false"/android:extractNativeLibs="true"/' "$D/AndroidManifest.xml"
else
  sed -i '0,/<application /s//<application android:extractNativeLibs="true" /' "$D/AndroidManifest.xml"
fi
mkdir -p "$D/lib/arm64-v8a" "$D/assets"
cp "$CIADPI" "$D/lib/arm64-v8a/libciadpi.so"
printf 'port=%s\nargs=%s\n' "$PORT" "$ARGS" > "$D/assets/dpi.cfg"

echo "[4/7] dpi sinifi derleniyor"
mkdir -p "$W/src/dpi" "$W/cls"
cp "$BOOT" "$W/src/dpi/Boot.java"
cp "$APPJ" "$W/src/dpi/App.java"
ecj -proc:none -encoding UTF-8 -nowarn -d "$W/cls" -cp "$AJ" "$W/src/dpi/Boot.java" "$W/src/dpi/App.java"
dx --dex --output="$W/dpi.dex" "$W/cls"

echo "[5/7] apktool derliyor"
find "$D/res" -name '*$*' -type f 2>/dev/null | while read -r f; do mv "$f" "$(echo "$f" | sed 's/\$/dollar_/g')"; done
grep -rl 'drawable/\$' "$D/res" 2>/dev/null | xargs -r sed -i 's#drawable/\$#drawable/dollar_#g'
grep -rl 'name="\$' "$D/res" 2>/dev/null | xargs -r sed -i 's#name="\$#name="dollar_#g'
OK=0
for i in 1 2 3 4 5 6; do
  if apktool b -j 2 "$D" -o "$W/u.apk" > "$W/build.log" 2>&1; then OK=1; break; fi
  REFS=$(grep -o 'resource android:[a-z]*/[A-Za-z0-9_.]* is private' "$W/build.log" | sed 's/^resource //;s/ is private$//' | sort -u)
  [ -z "$REFS" ] && { tail -25 "$W/build.log"; exit 1; }
  for r in $REFS; do grep -rl "@$r" "$D/res" | xargs -r sed -i "s#@$r#@*$r#g"; echo "  gizli kaynak duzeltildi: $r"; done
done
[ $OK = 1 ] || { tail -25 "$W/build.log"; exit 1; }

echo "[6/7] dpi.dex ekleniyor"
CNT=$(unzip -l "$W/u.apk" | grep -c ' classes[0-9]*\.dex$' || true)
NEW=classes$((CNT+1)).dex
cp "$W/dpi.dex" "$W/$NEW"
( cd "$W" && aapt add -f u.apk "$NEW" )

echo "[7/7] imzalaniyor"
[ -f "$KS" ] || keytool -genkey -keystore "$KS" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=patcher"
mkdir -p "$OUTDIR"
OUT=$OUTDIR/$(basename "${IN%.*}")-dpi.apk
apksigner sign --ks "$KS" --ks-key-alias "$KS_ALIAS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" --out "$OUT" "$W/u.apk"
echo "TAMAM: $OUT  (port $PORT)"
echo "Not: resmi surum kuruluysa once kaldir."
