# Üçüncü taraf yazılımlar

Bu depo aşağıdaki üçüncü taraf bileşenleri içerir ya da kullanır. Her biri kendi lisansına tabidir;
MIT lisansı (LICENSE) yalnızca bu deponun kendi kodu içindir.

| Bileşen | Nerede | Lisans (doğrulanmalı) |
|---|---|---|
| aapt, dx, android.jar | `app/src/main/assets/toolchain.dat` içinde | Apache-2.0 (AOSP) |
| ecj (Eclipse Compiler for Java) | `toolchain.dat` içinde | EPL-2.0 |
| ByeDPI (`ciadpi`) | Depoda **yok**; kullanıcı kendi derler (https://github.com/hufrea/byedpi) | Deponun kendi LICENSE dosyasına bak |
| APKEditor | Depoda yok; yamalayıcı `.apkm` birleştirmek için kullanıcı indirir | Deponun kendi lisansına bak |
| apktool, apksigner | Depoda yok; Termux paketleri olarak kurulur | Apache-2.0 (kendi sayfalarından doğrula) |

Lisans metinleri ve telif bildirimleri ilgili projelerin depolarında bulunur. Bu dosyadaki lisans
bilgileri hatırlatma amaçlıdır; dağıtmadan önce her projenin güncel LICENSE dosyasını kontrol et.
