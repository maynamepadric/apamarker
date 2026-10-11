package tr.apkmaker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Bagimsiz (kutuphanesiz) APK imzalayici: v1 (JAR) imzasi.
 * apksig jar'i bu cihazda lambda (invokedynamic) yuzunden calismadigi icin kendi imzalayicimiz var.
 * minSdk 24 / targetSdk 28 uygulamalar icin v1 imza yeterlidir.
 */
public class ApkSign {
    private static final String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    static String b64(byte[] d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length; i += 3) {
            int b0 = d[i] & 255;
            int b1 = i + 1 < d.length ? d[i + 1] & 255 : 0;
            int b2 = i + 2 < d.length ? d[i + 2] & 255 : 0;
            sb.append(B64.charAt(b0 >> 2));
            sb.append(B64.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            sb.append(i + 1 < d.length ? B64.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            sb.append(i + 2 < d.length ? B64.charAt(b2 & 63) : '=');
        }
        return sb.toString();
    }

    static byte[] sha256(byte[] d) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(d);
    }

    // JAR manifest satir kurali: en fazla 72 bayt, devam satirlari bosluk ile baslar
    static void line(ByteArrayOutputStream o, String s) throws Exception {
        byte[] b = s.getBytes("UTF-8");
        int pos = 0;
        int max = 72;
        while (b.length - pos > max) {
            o.write(b, pos, max);
            o.write("\r\n ".getBytes("US-ASCII"));
            pos += max;
            max = 71;
        }
        o.write(b, pos, b.length - pos);
        o.write("\r\n".getBytes("US-ASCII"));
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            o.write(buf, 0, n);
        }
        in.close();
        return o.toByteArray();
    }

    static boolean isSigFile(String n) {
        String u = n.toUpperCase(java.util.Locale.ENGLISH);
        return u.equals("META-INF/MANIFEST.MF")
                || (u.startsWith("META-INF/") && u.indexOf('/', 9) < 0
                    && (u.endsWith(".SF") || u.endsWith(".RSA") || u.endsWith(".DSA") || u.endsWith(".EC")));
    }

    public static void sign(File keystore, File in, File out) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(keystore);
        try {
            ks.load(fis, "123456".toCharArray());
        } finally {
            fis.close();
        }
        PrivateKey pk = (PrivateKey) ks.getKey("app", "123456".toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate("app");
        if (pk == null || cert == null) {
            throw new Exception("keystore'da 'app' anahtari yok");
        }

        // 1) girdileri oku
        Map<String, byte[]> data = new LinkedHashMap<String, byte[]>();
        Map<String, Integer> method = new LinkedHashMap<String, Integer>();
        ZipFile zf = new ZipFile(in);
        try {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory() || isSigFile(e.getName())) {
                    continue;
                }
                data.put(e.getName(), readAll(zf.getInputStream(e)));
                method.put(e.getName(), Integer.valueOf(e.getMethod()));
            }
        } finally {
            zf.close();
        }

        // 2) MANIFEST.MF ve CERT.SF
        ByteArrayOutputStream mf = new ByteArrayOutputStream();
        line(mf, "Manifest-Version: 1.0");
        line(mf, "Created-By: 1.0 (APK Maker)");
        mf.write("\r\n".getBytes("US-ASCII"));
        List<String> names = new ArrayList<String>(data.keySet());
        List<byte[]> sections = new ArrayList<byte[]>();
        for (String n : names) {
            ByteArrayOutputStream sec = new ByteArrayOutputStream();
            line(sec, "Name: " + n);
            line(sec, "SHA-256-Digest: " + b64(sha256(data.get(n))));
            sec.write("\r\n".getBytes("US-ASCII"));
            byte[] sb = sec.toByteArray();
            sections.add(sb);
            mf.write(sb);
        }
        byte[] mfBytes = mf.toByteArray();

        ByteArrayOutputStream sf = new ByteArrayOutputStream();
        line(sf, "Signature-Version: 1.0");
        line(sf, "Created-By: 1.0 (APK Maker)");
        line(sf, "SHA-256-Digest-Manifest: " + b64(sha256(mfBytes)));
        sf.write("\r\n".getBytes("US-ASCII"));
        for (int i = 0; i < names.size(); i++) {
            ByteArrayOutputStream sec = new ByteArrayOutputStream();
            line(sec, "Name: " + names.get(i));
            line(sec, "SHA-256-Digest: " + b64(sha256(sections.get(i))));
            sec.write("\r\n".getBytes("US-ASCII"));
            sf.write(sec.toByteArray());
        }
        byte[] sfBytes = sf.toByteArray();

        // 3) CERT.RSA (PKCS#7 SignedData, ayrik imza)
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        sig.update(sfBytes);
        byte[] rsa = pkcs7(cert, sig.sign());

        // 4) yeni zip
        File tmp = new File(out.getParentFile(), out.getName() + ".tmp");
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(tmp));
        try {
            put(zo, "META-INF/MANIFEST.MF", mfBytes, false);
            put(zo, "META-INF/CERT.SF", sfBytes, false);
            put(zo, "META-INF/CERT.RSA", rsa, false);
            for (String n : names) {
                Integer m = method.get(n);
                put(zo, n, data.get(n), m != null && m.intValue() == ZipEntry.STORED);
            }
        } finally {
            zo.close();
        }
        if (out.exists()) {
            out.delete();
        }
        if (!tmp.renameTo(out)) {
            throw new Exception("imzali apk yazilamadi");
        }
    }

    static void put(ZipOutputStream zo, String name, byte[] d, boolean stored) throws Exception {
        ZipEntry e = new ZipEntry(name);
        e.setTime(315532800000L + 86400000L); // sabit tarih (1980-01-02)
        if (stored) {
            CRC32 crc = new CRC32();
            crc.update(d);
            e.setMethod(ZipEntry.STORED);
            e.setSize(d.length);
            e.setCompressedSize(d.length);
            e.setCrc(crc.getValue());
        } else {
            e.setMethod(ZipEntry.DEFLATED);
        }
        zo.putNextEntry(e);
        zo.write(d);
        zo.closeEntry();
    }

    static byte[] pkcs7(X509Certificate cert, byte[] signature) throws Exception {
        byte[] sha256Alg = CertGen.D.seq(CertGen.D.oid("2.16.840.1.101.3.4.2.1"), CertGen.D.nul());
        byte[] rsaAlg = CertGen.D.seq(CertGen.D.oid("1.2.840.113549.1.1.1"), CertGen.D.nul());
        byte[] issuerAndSerial = CertGen.D.seq(cert.getIssuerX500Principal().getEncoded(), CertGen.D.integer(cert.getSerialNumber()));
        byte[] signerInfo = CertGen.D.seq(
                CertGen.D.integer(BigInteger.ONE),
                issuerAndSerial,
                sha256Alg,
                rsaAlg,
                CertGen.D.wrap(4, signature));
        byte[] signedData = CertGen.D.seq(
                CertGen.D.integer(BigInteger.ONE),
                CertGen.D.wrap(0x31, sha256Alg),
                CertGen.D.seq(CertGen.D.oid("1.2.840.113549.1.7.1")),
                CertGen.D.wrap(0xA0, cert.getEncoded()),
                CertGen.D.wrap(0x31, signerInfo));
        return CertGen.D.seq(CertGen.D.oid("1.2.840.113549.1.7.2"), CertGen.D.wrap(0xA0, signedData));
    }
}
