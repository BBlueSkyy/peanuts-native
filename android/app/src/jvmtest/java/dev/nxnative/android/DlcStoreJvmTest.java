package dev.nxnative.android;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

public final class DlcStoreJvmTest {
    private static void check(boolean v) { if (!v) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        File root=Files.createTempDirectory("dlc-test").toFile();
        File dir=DlcStore.dir(root,"0100A250097F0000");
        check(DlcStore.index("0100A250097F0000",0x0100A250097F1001L)==1);
        check(DlcStore.index("0100A250097F0000",0x0100A25009801001L)==-1);
        check(DlcStore.index("0100A250097F0000",0x0100A250097F1000L)==-1);
        check(DlcStore.read(dir).isEmpty());
        String n="dlc-12345678-1234-1234-1234-123456789abc.nup";
        Map<Integer,String> entries=new TreeMap<>();
        entries.put(1,n); entries.put(35,n);
        DlcStore.write(dir,entries);
        check(DlcStore.read(dir).equals(entries));
        entries.remove(1);
        DlcStore.write(dir,entries);
        check(DlcStore.read(dir).equals(entries));
        entries.put(2,"../evil.nup");
        try { DlcStore.write(dir,entries); throw new AssertionError(); }
        catch (java.io.IOException expected) { check(DlcStore.read(dir).size()==1); }
        Files.delete(new File(dir,"installed.tsv").toPath());
        Files.delete(dir.toPath()); Files.delete(dir.getParentFile().toPath()); Files.delete(root.toPath());
        System.out.println("DlcStore checks passed");
    }
}
