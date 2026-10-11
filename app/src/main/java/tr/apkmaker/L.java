package tr.apkmaker;

/** Basit coklu dil: anahtar = Turkce metin. 0=Turkce 1=English 2=Deutsch 3=Russian */
public class L {
    static final String[] NAMES = {"Türkçe", "English", "Deutsch", "Русский"};
    private static int cur = 0;
    private static java.util.HashMap<String, String[]> map;

    private static final String[][] T = {
        {"Hazir", "Ready", "Bereit", "Готово"},
        {"Ne uretmek istiyorsun?", "What do you want to build?", "Was möchtest du erstellen?", "Что вы хотите создать?"},
        {"URL ILE APK YAP  (web sitesi)", "BUILD APK FROM URL  (website)", "APK AUS URL ERSTELLEN  (Webseite)", "APK ИЗ ССЫЛКИ  (сайт)"},
        {"KOD ILE APK YAP  (Java / HTML5)", "BUILD APK FROM CODE  (Java / HTML5)", "APK AUS CODE ERSTELLEN  (Java / HTML5)", "APK ИЗ КОДА  (Java / HTML5)"},
        {"DOSYADAN APK YAP  (Java / HTML5 / zip)", "BUILD APK FROM FILES  (Java / HTML5 / zip)", "APK AUS DATEIEN ERSTELLEN  (Java / HTML5 / zip)", "APK ИЗ ФАЙЛОВ  (Java / HTML5 / zip)"},
        {"GECMIS  (uretilen uygulamalar)", "HISTORY  (built apps)", "VERLAUF  (erstellte Apps)", "ИСТОРИЯ  (созданные приложения)"},
        {"ARAC TESTI", "TOOL TEST", "WERKZEUGTEST", "ТЕСТ ИНСТРУМЕНТОВ"},
        {"--- URL ILE APK YAP ---", "--- BUILD APK FROM URL ---", "--- APK AUS URL ---", "--- APK ИЗ ССЫЛКИ ---"},
        {"Uygulama adi (bos = siteden al)", "App name (empty = take from site)", "App-Name (leer = von der Seite)", "Название (пусто = с сайта)"},
        {"Masaustu modu (uygulama icinde M/D tusundan degisir)", "Desktop mode (switch with the M/D button in the app)", "Desktop-Modus (in der App mit der M/D-Taste umschaltbar)", "Режим ПК (переключается кнопкой M/D в приложении)"},
        {"APK YAP", "BUILD APK", "APK ERSTELLEN", "СОЗДАТЬ APK"},
        {"--- KOD ILE APK YAP ---\nJava: class MainActivity (packagesiz)\nCok dosya: aralara '// FILE: Ad.java' satiri\nHTML5: asagidaki kutuyu isaretle", "--- BUILD APK FROM CODE ---\nJava: class MainActivity (no package line)\nMultiple files: put a '// FILE: Name.java' line between them\nHTML5: tick the box below", "--- APK AUS CODE ---\nJava: class MainActivity (ohne package)\nMehrere Dateien: Zeile '// FILE: Name.java' dazwischen\nHTML5: Kästchen unten ankreuzen", "--- APK ИЗ КОДА ---\nJava: class MainActivity (без package)\nНесколько файлов: между ними строка '// FILE: Name.java'\nHTML5: отметьте флажок ниже"},
        {"Uygulama adi (bos = KodApp / HtmlApp)", "App name (empty = KodApp / HtmlApp)", "App-Name (leer = KodApp / HtmlApp)", "Название (пусто = KodApp / HtmlApp)"},
        {"Kodu buraya yapistir...", "Paste the code here...", "Code hier einfügen...", "Вставьте код сюда..."},
        {"HTML5 modu (icerik index.html olarak islenir)", "HTML5 mode (content is used as index.html)", "HTML5-Modus (Inhalt wird als index.html verwendet)", "Режим HTML5 (содержимое станет index.html)"},
        {"KODDAN APK YAP", "BUILD APK FROM CODE", "APK AUS CODE ERSTELLEN", "СОЗДАТЬ APK ИЗ КОДА"},
        {"--- DOSYADAN APK YAP ---\nJava (.java), HTML5 (.html/.css/.js...), .zip veya icinde kod olan .txt sec.\nTur otomatik algilanir.", "--- BUILD APK FROM FILES ---\nPick Java (.java), HTML5 (.html/.css/.js...), .zip or a .txt containing code.\nThe type is detected automatically.", "--- APK AUS DATEIEN ---\nWähle Java (.java), HTML5 (.html/.css/.js...), .zip oder eine .txt mit Code.\nDer Typ wird automatisch erkannt.", "--- APK ИЗ ФАЙЛОВ ---\nВыберите Java (.java), HTML5 (.html/.css/.js...), .zip или .txt с кодом.\nТип определяется автоматически."},
        {"Uygulama adi (bos = otomatik)", "App name (empty = automatic)", "App-Name (leer = automatisch)", "Название (пусто = авто)"},
        {"DOSYA SEC  (birden fazla secilebilir)", "PICK FILES  (multiple allowed)", "DATEIEN WÄHLEN  (mehrere möglich)", "ВЫБРАТЬ ФАЙЛЫ  (можно несколько)"},
        {"Henuz dosya secilmedi", "No file picked yet", "Noch keine Datei gewählt", "Файлы ещё не выбраны"},
        {"DOSYADAN APK YAP", "BUILD APK FROM FILES", "APK AUS DATEIEN ERSTELLEN", "СОЗДАТЬ APK ИЗ ФАЙЛОВ"},
        {"Dosya hatasi: ", "File error: ", "Dateifehler: ", "Ошибка файла: "},
        {"Once gecerli dosya sec", "Pick a valid file first", "Wähle zuerst eine gültige Datei", "Сначала выберите подходящий файл"},
        {"IKON SEC (galeri)", "PICK ICON (gallery)", "ICON WÄHLEN (Galerie)", "ВЫБРАТЬ ИКОНКУ (галерея)"},
        {"ikon: sitenin simgesi (otomatik)", "icon: site's icon (automatic)", "Icon: Symbol der Seite (automatisch)", "иконка: значок сайта (авто)"},
        {"ikon: varsayilan", "icon: default", "Icon: Standard", "иконка: по умолчанию"},
        {"ikon: secildi (kirpildi)", "icon: selected (cropped)", "Icon: gewählt (zugeschnitten)", "иконка: выбрана (обрезана)"},
        {"Ikon okunamadi (gecersiz resim)", "Icon could not be read (invalid image)", "Icon nicht lesbar (ungültiges Bild)", "Не удалось прочитать иконку (неверное изображение)"},
        {"Ikon secildi", "Icon selected", "Icon gewählt", "Иконка выбрана"},
        {"Ikon hatasi: ", "Icon error: ", "Icon-Fehler: ", "Ошибка иконки: "},
        {"<- GERI", "<- BACK", "<- ZURÜCK", "<- НАЗАД"},
        {"URETILEN UYGULAMALAR", "BUILT APPS", "ERSTELLTE APPS", "СОЗДАННЫЕ ПРИЛОЖЕНИЯ"},
        {"(henuz uretim yok)", "(nothing built yet)", "(noch nichts erstellt)", "(пока ничего нет)"},
        {"Surum ", "Version ", "Version ", "Версия "},
        {"Surum -", "Version -", "Version -", "Версия -"},
        {"APK hazir: ", "APK ready: ", "APK fertig: ", "APK готов: "},
        {"  (Dosyalar > Download)", "  (Files > Download)", "  (Dateien > Download)", "  (Файлы > Download)"},
        {"Test tamam", "Test done", "Test abgeschlossen", "Тест завершён"},
        {"BASARISIZ (log'a bak)", "FAILED (see log)", "FEHLGESCHLAGEN (siehe Log)", "ОШИБКА (см. журнал)"},
        {"URL http ile baslamali", "URL must start with http", "URL muss mit http beginnen", "URL должен начинаться с http"},
        {"Java kodda 'class MainActivity' olmali", "Java code must contain 'class MainActivity'", "Java-Code muss 'class MainActivity' enthalten", "Java-код должен содержать 'class MainActivity'"},
        {"Kod/HTML bos gorunuyor", "Code/HTML looks empty", "Code/HTML wirkt leer", "Код/HTML выглядит пустым"},
        {"Araclar cikartiliyor (ilk sefer 1-2 dk)...", "Extracting tools (first time 1-2 min)...", "Werkzeuge werden entpackt (erstmals 1-2 Min.)...", "Распаковка инструментов (первый раз 1-2 мин)..."},
        {"Araclar hazir", "Tools ready", "Werkzeuge bereit", "Инструменты готовы"},
        {"Toolchain guncelleniyor, eski siliniyor...", "Updating toolchain, removing old one...", "Toolchain wird aktualisiert, alte wird gelöscht...", "Обновление инструментов, удаление старых..."},
        {"aapt: paketleniyor", "aapt: packaging", "aapt: Paketierung", "aapt: упаковка"},
        {"ecj: derleniyor (in-proc)", "ecj: compiling (in-process)", "ecj: Kompilierung (im Prozess)", "ecj: компиляция (в процессе)"},
        {"dx: dexleniyor", "dx: dexing", "dx: Dexing", "dx: dex-конвертация"},
        {"ozel anahtar + imza", "own key + signing", "eigener Schlüssel + Signatur", "свой ключ + подпись"},
        {"ecj derleme hatasi", "ecj compile error", "ecj-Kompilierfehler", "ошибка компиляции ecj"},
        {"ecj .class uretmedi (obj bos)", "ecj produced no .class (obj empty)", "ecj erzeugte keine .class (obj leer)", "ecj не создал .class (obj пуст)"},
        {"APK kaydedilemedi (depolama izni?)", "APK could not be saved (storage permission?)", "APK konnte nicht gespeichert werden (Speicherberechtigung?)", "Не удалось сохранить APK (разрешение на память?)"},
        {"Site simgesi araniyor...", "Looking for the site icon...", "Seitensymbol wird gesucht...", "Поиск значка сайта..."},
        {"Site simgesi bulunamadi, varsayilan ikon kullanilacak", "Site icon not found, default icon will be used", "Seitensymbol nicht gefunden, Standard-Icon wird verwendet", "Значок сайта не найден, будет значок по умолчанию"},
        {"Algilandi: JAVA projesi ({0} dosya, MainActivity bulundu)", "Detected: JAVA project ({0} files, MainActivity found)", "Erkannt: JAVA-Projekt ({0} Dateien, MainActivity gefunden)", "Обнаружен JAVA-проект ({0} файлов, найден MainActivity)"},
        {"Not: manifest ve res/ yoksayilir, yalnizca Java kullanilir", "Note: manifest and res/ are ignored, only Java is used", "Hinweis: Manifest und res/ werden ignoriert, nur Java wird verwendet", "Примечание: manifest и res/ игнорируются, используется только Java"},
        {"Algilandi: HTML5 projesi (giris: {0}, toplam {1} dosya)", "Detected: HTML5 project (entry: {0}, {1} files in total)", "Erkannt: HTML5-Projekt (Einstieg: {0}, insgesamt {1} Dateien)", "Обнаружен HTML5-проект (вход: {0}, всего файлов: {1})"},
        {"Java dosyasi var ama 'class MainActivity' bulunamadi", "There is a Java file but 'class MainActivity' was not found", "Java-Datei vorhanden, aber 'class MainActivity' nicht gefunden", "Есть Java-файл, но 'class MainActivity' не найден"},
        {"Taninan dosya yok (.java, .html, .zip veya icinde kod olan .txt sec)", "No recognized file (pick .java, .html, .zip or a .txt containing code)", "Keine erkannte Datei (wähle .java, .html, .zip oder .txt mit Code)", "Нет подходящих файлов (выберите .java, .html, .zip или .txt с кодом)"}
    };

    public static void set(int i) {
        cur = (i < 0 || i >= NAMES.length) ? 0 : i;
    }

    public static int cur() {
        return cur;
    }

    public static int count() {
        return NAMES.length;
    }

    public static String name() {
        return NAMES[cur];
    }

    public static String t(String tr) {
        if (cur == 0) {
            return tr;
        }
        if (map == null) {
            map = new java.util.HashMap<String, String[]>();
            for (String[] r : T) {
                map.put(r[0], r);
            }
        }
        String[] r = map.get(tr);
        return r == null ? tr : r[cur];
    }

    public static String f(String tr, Object... a) {
        String s = t(tr);
        for (int i = 0; i < a.length; i++) {
            s = s.replace("{" + i + "}", String.valueOf(a[i]));
        }
        return s;
    }
}
