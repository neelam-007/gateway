package com.l7tech.common.util;

import java.io.IOException;
import java.io.FileOutputStream;

/**
 * Saves arbitrary information into a dropfile.
 */
public class FileDropper {
    private static final long start = System.currentTimeMillis();
    private static int index = 0;

    /**
     * Save the specified data into a new drop file, whose name will begin with prefix.
     *
     * @param pathPrefix
     * @param data
     * @throws IOException if the drop file could not be saved.
     */
    public static void save(String pathPrefix, byte[] data) throws IOException {
        if (!pathPrefix.startsWith("/"))
                pathPrefix = "/tmp/" + pathPrefix;
        if (!pathPrefix.endsWith("/"))
                pathPrefix += "/";
        String fname = pathPrefix + start + "_" + nextIndex();

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
            os.write(data);
        } finally {
            if (os != null) try { os.close(); } catch (Exception e) {}
        }
    }

    private synchronized static int nextIndex() {
        return index++;
    }
}
