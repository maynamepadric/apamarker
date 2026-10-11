package tr.apkmaker;

import dalvik.system.PathClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;

public class InProc {

    // Satir satir log: bayt olarak biriktirip UTF-8 cozer (Turkce karakterler bozulmaz)
    static class LineLog extends OutputStream {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final Builder.Cb cb;

        LineLog(Builder.Cb cb) {
            this.cb = cb;
        }

        @Override public void write(int i) {
            if (i == 10) {
                flushLine();
            } else {
                buf.write(i);
            }
        }

        @Override public void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }

        @Override public void flush() {
        }

        void flushLine() {
            String line;
            try {
                line = buf.toString("UTF-8").trim();
            } catch (Exception e) {
                line = buf.toString().trim();
            }
            buf.reset();
            if (line.length() > 0) {
                cb.log(line);
            }
        }

        void finish() {
            flushLine();
        }
    }

    public static boolean compileEcj(String jar, String[] args, Builder.Cb cb) {
        boolean ok = false;
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        LineLog ll = new LineLog(cb);
        PrintStream ps = new PrintStream(ll, true);
        try {
            System.setOut(ps);
            System.setErr(ps);
            Class<?> cls = Class.forName("org.eclipse.jdt.internal.compiler.batch.Main", true,
                    new PathClassLoader(jar, InProc.class.getClassLoader()));
            Constructor<?> ctor = null;
            for (Constructor<?> c : cls.getConstructors()) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length >= 3 && t[0] == PrintWriter.class && t[1] == PrintWriter.class && t[2] == Boolean.TYPE) {
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) {
                cb.log("ECJ EX: uygun kurucu bulunamadi");
            } else {
                Object[] a = new Object[ctor.getParameterTypes().length];
                PrintWriter out = new PrintWriter(ps, true);
                PrintWriter err = new PrintWriter(ps, true);
                a[0] = out;
                a[1] = err;
                a[2] = Boolean.FALSE;
                Object r = cls.getMethod("compile", String[].class).invoke(ctor.newInstance(a), (Object) args);
                out.flush();
                err.flush();
                ps.flush();
                ok = Boolean.TRUE.equals(r);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            cb.log("ECJ EX: " + (cause != null ? cause.toString() : e.toString()));
        } catch (Exception e) {
            cb.log("ECJ EX: " + e);
        } finally {
            ll.finish();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        return ok;
    }

    private static String why(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        StackTraceElement[] st = c.getStackTrace();
        return c.toString() + (st.length > 0 ? " @ " + st[0] : "");
    }

    // dx'i uygulama icinde calistirir (class klasoru -> classes.dex). Mutlak yol ver!
    public static boolean dex(String jar, String outDex, String inDir, Builder.Cb cb) {
        boolean ok = false;
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        LineLog ll = new LineLog(cb);
        PrintStream ps = new PrintStream(ll, true);
        try {
            System.setOut(ps);
            System.setErr(ps);
            ClassLoader cl = new PathClassLoader(jar, InProc.class.getClassLoader());
            Class<?> ctxC = Class.forName("dx.dx.command.dexer.DxContext", true, cl);
            Object ctx = ctxC.getConstructor(OutputStream.class, OutputStream.class).newInstance(ps, ps);
            Class<?> argC = Class.forName("dx.dx.command.dexer.Main$Arguments", true, cl);
            Object args = argC.getConstructor(ctxC).newInstance(ctx);
            Method parse = argC.getDeclaredMethod("parse", String[].class);
            parse.setAccessible(true);
            parse.invoke(args, (Object) new String[]{"--output=" + outDex, inDir});
            Class<?> mainC = Class.forName("dx.dx.command.dexer.Main", true, cl);
            Object r = mainC.getMethod("run", argC).invoke(null, args);
            ps.flush();
            ok = (r instanceof Integer) && ((Integer) r).intValue() == 0 && new File(outDex).length() > 0;
            if (!ok) {
                cb.log("DX sonuc: " + r);
            }
        } catch (Throwable e) {
            cb.log("DX EX: " + why(e));
        } finally {
            ll.finish();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        return ok;
    }

    // Kendi v1 imzalayicimizla imzalar (toolchain'deki apksig jar'i bu cihazda lambda hatasi veriyor)
    public static boolean sign(String jar, File keystore, File in, File out, Builder.Cb cb) {
        try {
            ApkSign.sign(keystore, in, out);
            return out.exists() && out.length() > 0;
        } catch (Throwable e) {
            cb.log("IMZA EX: " + why(e));
            return false;
        }
    }
}
