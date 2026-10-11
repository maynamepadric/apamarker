#!/data/data/com.termux/files/usr/bin/bash
# APK Maker yamalayici icin Termux'u bir kerede hazirlar.
set -e
echo "[1/4] paketler kuruluyor (birkac dakika surebilir)"
pkg install -y apktool ecj dx aapt apksigner openjdk-21 curl unzip

echo "[2/4] APKEditor.jar (.apkm/.xapk birlestirmek icin)"
if [ ! -s "$HOME/APKEditor.jar" ]; then
  URL=$(curl -s https://api.github.com/repos/REAndroid/APKEditor/releases/latest | grep -o 'https://[^"]*APKEditor-[0-9.]*\.jar' | head -1)
  [ -n "$URL" ] || URL=https://github.com/REAndroid/APKEditor/releases/download/V1.4.9/APKEditor-1.4.9.jar
  curl -L -o "$HOME/APKEditor.jar" "$URL"
else
  echo "  zaten var"
fi

echo "[3/4] APK Maker'in Termux'u calistirabilmesi icin izin"
mkdir -p "$HOME/.termux"
touch "$HOME/.termux/termux.properties"
if grep -q '^allow-external-apps' "$HOME/.termux/termux.properties"; then
  sed -i 's/^allow-external-apps.*/allow-external-apps = true/' "$HOME/.termux/termux.properties"
else
  echo 'allow-external-apps = true' >> "$HOME/.termux/termux.properties"
fi
termux-reload-settings 2>/dev/null || true

echo "[4/4] depolama izni"
[ -d "$HOME/storage" ] || termux-setup-storage

echo
echo "Kontrol:"
for c in apktool ecj dx aapt apksigner keytool unzip curl; do
  if command -v $c >/dev/null; then echo "  OK    $c"; else echo "  EKSIK $c"; fi
done
[ -s "$HOME/APKEditor.jar" ] && echo "  OK    APKEditor.jar" || echo "  EKSIK APKEditor.jar"
echo
echo "Hazir. Termux'u tamamen kapatip yeniden ac (izin ayari icin), sonra APK Maker'dan yamala."
