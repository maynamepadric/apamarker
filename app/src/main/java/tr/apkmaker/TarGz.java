package tr.apkmaker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class TarGz {
    public static void extract(InputStream inputStream, File file) throws Exception {
        int read;
        GZIPInputStream gZIPInputStream = new GZIPInputStream(inputStream);
        byte[] bArr = new byte[512];
        while (readFull(gZIPInputStream, bArr) >= 512 && bArr[0] != 0) {
            String trim = new String(bArr, 0, 100, "UTF-8").trim();
            String trim2 = new String(bArr, 345, 155, "UTF-8").trim();
            if (trim2.length() > 0) {
                trim = String.valueOf(trim2) + "/" + trim;
            }
            long parseLong = Long.parseLong(new String(bArr, 124, 12, "UTF-8").trim(), 8);
            byte b = bArr[156];
            File file2 = new File(file, trim);
            if (!file2.getCanonicalPath().startsWith(file.getCanonicalPath())) {
                throw new IOException("gecersiz yol: " + trim);
            }
            long j = (512 - (parseLong % 512)) % 512;
            if (b == 53) {
                file2.mkdirs();
                skip(gZIPInputStream, parseLong + j);
            } else if (b == 48 || b == 0) {
                file2.getParentFile().mkdirs();
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                byte[] bArr2 = new byte[8192];
                long j2 = parseLong;
                while (j2 > 0 && (read = gZIPInputStream.read(bArr2, 0, (int) Math.min(8192L, j2))) >= 0) {
                    fileOutputStream.write(bArr2, 0, read);
                    j2 -= read;
                }
                fileOutputStream.close();
                file2.setExecutable(true, false);
                skip(gZIPInputStream, j);
            } else {
                skip(gZIPInputStream, parseLong + j);
            }
        }
        gZIPInputStream.close();
    }

    private static int readFull(InputStream inputStream, byte[] bArr) throws IOException {
        int read;
        int i = 0;
        while (i < bArr.length && (read = inputStream.read(bArr, i, bArr.length - i)) >= 0) {
            i += read;
        }
        return i;
    }

    private static void skip(InputStream inputStream, long j) throws IOException {
        int read;
        byte[] bArr = new byte[4096];
        while (j > 0 && (read = inputStream.read(bArr, 0, (int) Math.min(4096L, j))) >= 0) {
            j -= read;
        }
    }
}
