package com.l7tech.server.policy.module;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Holds all files preloaded from a zip file, possibly because it is a jar file nested inside another
 * jar file and we are going to set up a class loader pointing into it.
 * <P/>
 * Closing a NestedZipFile frees any cache memory being used, but does <em>not</em> close the parent zip file.
 */
class NestedZipFile implements Closeable {
    protected static final Logger logger = Logger.getLogger(NestedZipFile.class.getName());

    /** Parent ZipFile, from which this one can be reconstructed as needed. */
    private final ZipFile parent;

    /** Name of this PreloadedZipFile, ie "AAR-INF/lib/foo.jar". */
    private final String entryName;

    /** Map of path (ie, "com/yoyodyne/scoot/ScootMain$4.class") to the byte of the file. */
    private final Map<String, Reference<byte[]>> cachedFileBytes = new ConcurrentHashMap<String, Reference<byte[]>>();

    /** Set of directories present within this PreloadedZipFile. */
    private final Set<String> directories;

    /** Directory of files present in the nested zip.  Map of path to size of file in bytes. */
    private final Map<String, Long> files;

    /** True if this library should be flagged as private. */
    private final boolean privateLibrary;

    /**
     * Create a PreloadedZipFile by loading all resources from the specified input stream.
     * This will immediately load all resources from the jar into datastructures within a new NestedJarInfo instance.
     *
     * @param parent the parent zip file from which the nested zip is to be read.  Required.
     * @param entryName path of the zip file being preloaded, relative to root of parent, ie "AAR-INF/lib/foo.jar".  Required.
     * @param privateLibrary if true, this zip should be flagged as private, so classes in it shouldn't be disclosed outside the Gateway
     * @throws java.io.IOException  if there is a problem loading resources from the specified jar stream.
     */
    public NestedZipFile(ZipFile parent, String entryName, boolean privateLibrary) throws IOException {
        if (parent == null || entryName == null) throw new NullPointerException();
        this.parent = parent;
        this.entryName = entryName;

        // Build index
        ZipEntry ourEntry = parent.getEntry(entryName);
        if (ourEntry == null)
            throw new IOException("Unable to find nested zipfile entry: " + entryName + " in zip file: " + parent.getName());

        InputStream compressedIn = parent.getInputStream(ourEntry);
        if (compressedIn == null)
            throw new IOException("null InputStream for nested zipfile entry: " + entryName + " in zip file: " + parent.getName()); // can't happen

        Set<String> d = new HashSet<String>();
        Map<String, Long> f = new HashMap<String, Long>();

        scanNestedZip(null, d, f);

        this.directories = Collections.unmodifiableSet(d);
        this.files = Collections.unmodifiableMap(f);
        this.privateLibrary = privateLibrary;
    }

    /**
     * Rereads the zip and cache any likely-looking files, optionally fetching
     *
     * @param wantedFile  the path of a file to fetch on the way by, or null if no particular file is needed right now.
     * @param recordDirectories  a Set< String > to which will be added every directory seen in the nested zip, or null if not required.
     * @param recordFiles        a Map< String, Long > to which will be added every file seen and its size, or null if not required.
     * @return the bytes of the specified wantedFile, if it was specified and was found in the zip, or null if not specified or not found.
     * @throws IOException if there is a problem reading the parent or nested zip file
     */
    private synchronized byte[] scanNestedZip(String wantedFile, Set<String> recordDirectories, Map<String, Long> recordFiles) throws IOException {
        // Build index
        ZipEntry ourEntry = parent.getEntry(entryName);
        if (ourEntry == null)
            throw new IOException("Unable to find nested zipfile entry: " + entryName + " in zip file: " + parent.getName());

        InputStream compressedIn = parent.getInputStream(ourEntry);
        if (compressedIn == null)
            throw new IOException("null InputStream for nested zipfile entry: " + entryName + " in zip file: " + parent.getName()); // can't happen

        byte[] wantedFileBytes = null;
        ZipInputStream zipIn = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            zipIn = new ZipInputStream(compressedIn);

            ZipEntry entry;
            //noinspection NestedAssignment
            while((entry = zipIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    if (recordDirectories != null)
                        recordDirectories.add(entry.getName());
                    continue;
                }

                if (recordFiles != null)
                    recordFiles.put(entry.getName(), entry.getSize());

                PoolByteArrayOutputStream bout = new PoolByteArrayOutputStream();
                try {
                    IOUtils.copyStream(zipIn, bout);
                    byte[] fileBytes = bout.toByteArray();
                    cachedFileBytes.put(entry.getName(), new SoftReference<byte[]>(fileBytes));
                    if (wantedFile != null && wantedFile.equals(entry.getName()))
                        wantedFileBytes = fileBytes;
                } finally {
                    bout.close();
                }
            }
        } finally {
            ResourceUtils.closeQuietly(zipIn);
        }

        return wantedFileBytes;
    }

    /** @return the parent ZipFile from which this one will load its data. */
    ZipFile getParent() {
        return parent;
    }

    /** @return Name of this PreloadedZipFile relative to its parent, ie "AAR-INF/lib/foo.jar". */
    public String getEntryName() {
        return entryName;
    }

    /**
     * Get the set of all files in this preloaded zip file, identified by their full path relative
     * to the root of the archive.
     *
     * @return a Set of full pathnames of files from the archive, ie { "com/yoyodyne/scoot/ScootMain$4.class" }.
     *         May be empty but never null.
     */
    public Set<String> getFiles() {
        return Collections.unmodifiableSet(cachedFileBytes.keySet());
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
     * @throws java.io.IOException if an error is encountered reading the inner or outer zipfile
     */
    public byte[] getFile(String path) throws IOException {
        Reference<byte[]> ref = cachedFileBytes.get(path);
        byte[] bytes = ref == null ? null : ref.get();
        if (bytes != null)
            return bytes;

        if (!files.containsKey(path))
            return null;

        // File exists, but isn't cached.  Need to rescan the zip
        return scanNestedZip(path, null, null);
    }

    public void close() throws IOException {
        cachedFileBytes.clear();
    }

    public boolean isPrivateLibrary() {
        return privateLibrary;
    }
}
