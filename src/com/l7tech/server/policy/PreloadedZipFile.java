package com.l7tech.server.policy;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.util.HexUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Holds all files preloaded from a zip file, possibly because it is a jar file nested inside another
 * jar file and we are going to set up a class loader pointing into it.
 */
public class PreloadedZipFile {
    /** Name of this PreloadedZipFile, ie "AAR-INF/lib/foo.jar". */
    private final String name;

    /** Map of path (ie, "com/yoyodyne/scoot/ScootMain$4.class") to the byte of the file. */
    private final Map<String, byte[]> preloadedFileBytes = new ConcurrentHashMap<String,byte[]>();

    /** Set of directories present within this PreloadedZipFile. */
    private final Set<String> directories = new HashSet<String>();

    private PreloadedZipFile(String name, InputStream zipStream) throws IOException {
        if (name == null) throw new NullPointerException();
        this.name = name;
        ZipInputStream zipIn = new ZipInputStream(zipStream);

        ZipEntry entry;
        while((entry = zipIn.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                directories.add(entry.getName());
                continue;
            }
            BufferPoolByteArrayOutputStream bout = new BufferPoolByteArrayOutputStream();
            try {
                HexUtils.copyStream(zipIn, bout);
                preloadedFileBytes.put(entry.getName(), bout.toByteArray());
            } finally {
                bout.close();
            }
        }
    }

    /**
     * Create a PreloadedZipFile by loading all resources from the specified input stream.
     * This will immediately load all resources from the jar into datastructures within a new NestedJarInfo instance.
     *
     * @param name name of the zip file being preloaded, ie "AAR-INF/lib/foo.jar".  Required.
     * @param zipStream an InputStream that will produce all bytes of the nested jar file.  Required.
     * @throws java.io.IOException  if there is a problem loading resources from the specified jar stream.
     * @return a new PreloadedZipFile instance containing all uncompressed data preloaded from this zip file.
     */
    public static PreloadedZipFile preloadZipFile(String name, InputStream zipStream) throws IOException {
        return new PreloadedZipFile(name, zipStream);
    }

    /** @return Name of this PreloadedZipFile, ie "AAR-INF/lib/foo.jar". */
    public String getName() {
        return name;
    }

    /**
     * Get the set of all files in this preloaded zip file, identified by their full path relative
     * to the root of the archive.
     *
     * @return a Set of full pathnames of files from the archive, ie { "com/yoyodyne/scoot/ScootMain$4.class" }.
     *         May be empty but never null.
     */
    public Set<String> getFiles() {
        return Collections.unmodifiableSet(preloadedFileBytes.keySet());
    }

    /**
     * Get the set of all directories in this preloaded zip file, identified by their full path relative to
     * the root of the archive.
     *
     * @return a Set of full pathnames of directories from the archive, ie { "com/yoyodyne/scoot" }.
     *         May be empty but never null.
     */
    public Set<String> getDirectories() {
        return Collections.unmodifiableSet(directories);
    }

    /**
     * Get the bytes of a file from the preloaded zip file.  Caller should make their own private copy before
     * modifying these bytes in any way.
     *
     * @param path  the path of the file, relative to the zip root, ie "com/yoyodyne/scoot/ScootMain$4.class".  Required.
     * @return the bytes of the requested file, which may be empty if the specified path points to a zero-length file;
     *         or null if the specified file was not found.
     */
    public byte[] getFile(String path) {
        return preloadedFileBytes.get(path);
    }
}
