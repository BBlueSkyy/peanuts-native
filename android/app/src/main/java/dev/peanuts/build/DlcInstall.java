package dev.peanuts.build;

import android.content.Context;
import dev.nxnative.android.DlcContainers;
import dev.nxnative.android.DlcStore;
import dev.peanuts.PeanutsPrefs;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Import verified DLC dumps without modifying the game NUP or its pack. */
public final class DlcInstall {
    private DlcInstall() {}
    public static synchronized int install(Context c, String id, File game, List<File> sources) throws Exception {
        if (BuildPipeline.busy()) throw new IOException("Finish the current build first");
        File dir = DlcStore.dir(c.getFilesDir(),id);
        Map<Integer,String> entries = DlcStore.read(dir);
        Set<Integer> existing = DlcContainers.indices(game,id,false);
        existing.addAll(entries.keySet());
        if (Importer.keys(PeanutsPrefs.keysFile(c)) != 0) throw new IOException("Cannot load keys: " + Importer.lastError());
        Set<Integer> expected = new TreeSet<>();
        List<File> inputs = new ArrayList<>();
        for (File f : sources) {
            if (!f.isFile()) throw new IOException("DLC file missing: " + f.getName());
            org.json.JSONArray titles = new org.json.JSONArray(Importer.probe(f));
            Set<Integer> ids = new TreeSet<>();
            for (int j=0;j<titles.length();j++) {
                org.json.JSONObject t=titles.getJSONObject(j);
                String tid=t.optString("title_id");
                if (t.optInt("kind",-1)!=2 || !id.equalsIgnoreCase(t.optString("app_id")) || !tid.matches("[0-9a-fA-F]{16}"))
                    throw new IOException("Not a DLC-only dump for this game: " + f.getName());
                int index=DlcStore.index(id,Long.parseUnsignedLong(tid,16));
                if (index<0) throw new IOException("Invalid DLC title ID: "+tid);
                ids.add(index);
            }
            if (ids.isEmpty()) throw new IOException("No DLC in "+f.getName());
            if (!existing.containsAll(ids) && !inputs.contains(f)) { inputs.add(f); expected.addAll(ids); }
        }
        if (inputs.isEmpty()) return 0;
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create DLC folder");
        File out=new File(dir,"dlc-"+UUID.randomUUID()+".nup");
        boolean committed=false;
        try {
            Importer.cancel(false);
            if (Importer.writeNup(out,inputs,id,(step,done,total)->{}) != 0)
                throw new IOException("DLC import failed: "+Importer.lastError());
            Set<Integer> actual=DlcContainers.indices(out,id,true);
            if (!actual.equals(expected)) throw new IOException("Imported DLC titles do not match the selected dumps");
            actual.removeAll(existing);
            for (int i:actual) entries.put(i,out.getName());
            DlcStore.write(dir,entries);
            committed=true;
            return actual.size();
        } finally { if (!committed) out.delete(); }
    }
    public static synchronized void disable(Context c,String id,int index) throws IOException {
        File dir=DlcStore.dir(c.getFilesDir(),id);
        Map<Integer,String> entries=DlcStore.read(dir);
        entries.remove(index);
        DlcStore.write(dir,entries);
        // Keep immutable containers: a running game may still be reading them.
    }
}
