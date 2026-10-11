package tr.apkmaker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CertGen {
    public static void ensureKey(File file, String str) throws Exception {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, new SecureRandom());
            KeyPair generateKeyPair = keyPairGenerator.generateKeyPair();
            long currentTimeMillis = System.currentTimeMillis();
            byte[] tbs = tbs(generateKeyPair.getPublic(), "CN=" + str, new Date(currentTimeMillis - 86400000), new Date(currentTimeMillis + 946080000000L));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(generateKeyPair.getPrivate());
            signature.update(tbs);
            byte[] sign = signature.sign();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(tbs);
            byteArrayOutputStream.write(D.algId());
            D.bitstr(byteArrayOutputStream, sign);
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(D.wrap(48, byteArrayOutputStream.toByteArray())));
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("app", generateKeyPair.getPrivate(), "123456".toCharArray(), new Certificate[]{x509Certificate});
            // once gecici dosyaya yaz: yarim kalan keystore bir daha hic duzelmez
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(tmp);
            keyStore.store(fileOutputStream, "123456".toCharArray());
            fileOutputStream.close();
            if (!tmp.renameTo(file)) {
                tmp.delete();
                throw new IOException("keystore yazilamadi");
            }
        }
    }

    static byte[] tbs(PublicKey publicKey, String str, Date date, Date date2) throws Exception {
        return D.seq(D.integer(new BigInteger(63, new SecureRandom()).add(BigInteger.ONE)), D.algId(), D.name(str), D.seq(D.utc(date), D.utc(date2)), D.name(str), D.rsaPub(publicKey));
    }

    public static class D {
        D() {
        }

        static void len(OutputStream outputStream, int i) throws IOException {
            if (i >= 128) {
                if (i >= 256) {
                    outputStream.write(130);
                    outputStream.write((i >> 8) & 255);
                    outputStream.write(i & 255);
                    return;
                } else {
                    outputStream.write(129);
                    outputStream.write(i);
                    return;
                }
            }
            outputStream.write(i);
        }

        static void tlv(OutputStream outputStream, int i, byte[] bArr) throws IOException {
            outputStream.write(i);
            len(outputStream, bArr.length);
            outputStream.write(bArr);
        }

        static byte[] wrap(int i, byte[] bArr) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            tlv(byteArrayOutputStream, i, bArr);
            return byteArrayOutputStream.toByteArray();
        }

        static void seq(OutputStream outputStream, byte[] bArr) throws IOException {
            tlv(outputStream, 48, bArr);
        }

        static byte[] seq(byte[]... bArr) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (byte[] bArr2 : bArr) {
                byteArrayOutputStream.write(bArr2);
            }
            return wrap(48, byteArrayOutputStream.toByteArray());
        }

        static byte[] integer(BigInteger bigInteger) throws IOException {
            return wrap(2, bigInteger.toByteArray());
        }

        static void bitstr(OutputStream outputStream, byte[] bArr) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(bArr);
            tlv(outputStream, 3, byteArrayOutputStream.toByteArray());
        }

        static void oidInt(int i, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
            int i2;
            int[] iArr = new int[8];
            int i3 = 0;
            while (i > 0) {
                iArr[i3] = i & 127;
                i >>= 7;
                i3++;
            }
            if (i3 == 0) {
                i2 = i3 + 1;
                iArr[i3] = 0;
            } else {
                i2 = i3;
            }
            int i4 = i2 - 1;
            while (i4 >= 0) {
                byteArrayOutputStream.write((i4 != 0 ? 128 : 0) | iArr[i4]);
                i4--;
            }
        }

        static byte[] oid(String str) throws IOException {
            String[] split = str.split("\\.");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write((Integer.parseInt(split[0]) * 40) + Integer.parseInt(split[1]));
            for (int i = 2; i < split.length; i++) {
                oidInt(Integer.parseInt(split[i]), byteArrayOutputStream);
            }
            return wrap(6, byteArrayOutputStream.toByteArray());
        }

        static byte[] nul() {
            return new byte[]{5, 0};
        }

        static byte[] algId() throws IOException {
            return seq(oid("1.2.840.113549.1.1.11"), nul());
        }

        static byte[] rsaPub(PublicKey publicKey) throws Exception {
            RSAPublicKey rSAPublicKey = (RSAPublicKey) publicKey;
            byte[] seq = seq(integer(rSAPublicKey.getModulus()), integer(rSAPublicKey.getPublicExponent()));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitstr(byteArrayOutputStream, seq);
            return seq(seq(oid("1.2.840.113549.1.1.1"), nul()), byteArrayOutputStream.toByteArray());
        }

        static byte[] name(String str) throws IOException {
            if (str.startsWith("CN=")) {
                str = str.substring(3);
            }
            return seq(wrap(49, seq(oid("2.5.4.3"), wrap(12, str.getBytes("UTF-8")))));
        }

        // RFC 5280: 2050 ve sonrasi GeneralizedTime olmali (UTCTime yy=26 gibi 2026'ya sarar)
        static byte[] utc(Date date) throws IOException {
            java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(date);
            boolean gen = cal.get(java.util.Calendar.YEAR) >= 2050;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(gen ? "yyyyMMddHHmmss'Z'" : "yyMMddHHmmss'Z'", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return wrap(gen ? 24 : 23, simpleDateFormat.format(date).getBytes("US-ASCII"));
        }
    }
}
