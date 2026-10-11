package tr.apkmaker;

public class Esc {
    public static String xml(String str) {
        return str == null ? "" : str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static String java(String str) {
        return str == null ? "" : str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
