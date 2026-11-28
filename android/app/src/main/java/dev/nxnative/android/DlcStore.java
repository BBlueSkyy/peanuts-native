package dev.nxnative.android;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/** Persistent index of separately imported DLC. Containers are immutable; publishing the index is the commit point. */
public final class DlcStore {
    private DlcStore() {}
    public static File dir(File files, String titleId) {
        if (titleId == null || !titleId.matches("[0-9a-fA-F]{16}")) throw new IllegalArgumentException("Invalid application ID");
        return new File(files, "installed-dlc/" + titleId.toUpperCase(java.util.Locale.ROOT));
    }
    public static int index(String appId, long tid) {
        if (appId == null || !appId.matches("[0-9a-fA-F]{16}")) return -1;
        long app = Long.parseUnsignedLong(appId, 16);
        long i = tid - app - 0x1000L;
        return (app & 0xfff) == 0 && i >= 1 && i <= 2000 && (tid & 0xfff) == i ? (int)i : -1;
    }
    private static boolean validName(String name) { return name.matches("dlc-[0-9a-f-]{36}\\.nup"); }
    public static Map<Integer,String> read(File dir) throws IOException {
        Map<Integer,String> result = new TreeMap<>();
        File index = new File(dir, "installed.tsv");
        if (!index.exists()) return result;
        for (String row : Files.readAllLines(index.toPath(), StandardCharsets.UTF_8)) {
            String[] p = row.split("\\t", -1);
            int i;
            try { i = p.length == 2 ? Integer.parseInt(p[0]) : -1; }
            catch (NumberFormatException e) { throw new IOException("Invalid DLC index", e); }
            if (i < 1 || i > 2000 || !validName(p[1]) || result.put(i, p[1]) != null)
                throw new IOException("Invalid DLC index row");
        }
        return result;
    }
    public static void write(File dir, Map<Integer,String> entries) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create DLC folder");
        StringBuilder data = new StringBuilder();
        for (Map.Entry<Integer,String> e : new TreeMap<>(entries).entrySet()) {
            if (e.getKey() < 1 || e.getKey() > 2000 || !validName(e.getValue()))
                throw new IOException("Invalid DLC entry");
            data.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        File tmp = File.createTempFile(".index-", ".tmp", dir);
        try {
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) {
                out.write(data.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            Files.move(tmp.toPath(), new File(dir, "installed.tsv").toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { tmp.delete(); }
    }
}
