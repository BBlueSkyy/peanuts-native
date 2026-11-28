package dev.nxnative.android;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Resolve DLC by full title ID, never by its filename or low bits alone. */
public final class DlcContainers {
    private DlcContainers() {}
    public static Set<Integer> indices(File file, String appId, boolean addonOnly) throws IOException {
        Set<Integer> result = new TreeSet<>();
        Nup nup = Nup.open(file);
        if (addonOnly && nup.packages.isEmpty()) throw new IOException("Empty DLC container");
        for (Nup.Pkg pkg : nup.packages) {
            if (pkg.kind != Nup.ADDON) {
                if (addonOnly) throw new IOException("Not a DLC-only container");
                continue;
            }
            int i = DlcStore.index(appId, pkg.titleId);
            if (i < 0 || pkg.romfsDir == Nup.NONE || !result.add(i))
                throw new IOException("Invalid or foreign DLC in " + file.getName());
        }
        return result;
    }
    /** Add only validated local packages; existing embedded/pack mounts take precedence. */
    static void addLinks(File dir, String appId, Map<String,String> links, List<String> problems) {
        try {
            Map<String,Set<Integer>> checked = new java.util.HashMap<>();
            for (Map.Entry<Integer,String> e : DlcStore.read(dir).entrySet()) {
                String k = Integer.toString(e.getKey());
                if (links.containsKey(k) || links.containsKey(k+".nup") || links.containsKey(k+".nsp")) continue;
                File file = new File(dir,e.getValue());
                try {
                    Set<Integer> ids = checked.get(e.getValue());
                    if (ids == null) { ids = indices(file,appId,true); checked.put(e.getValue(),ids); }
                    if (!ids.contains(e.getKey())) throw new IOException("DLC title is absent");
                    links.put(k+".nup", file.getAbsolutePath());
                } catch (IOException | RuntimeException ex) {
                    problems.add("[RED] [nxlaunch] DLC " + k + ": " + ex.getMessage());
                }
            }
        } catch (IOException | RuntimeException ex) {
            problems.add("[RED] [nxlaunch] installed DLC: " + ex.getMessage());
        }
    }
}
