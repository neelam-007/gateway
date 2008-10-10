/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.IOUtils;

import java.io.*;
import java.util.*;

/**
 * A StashManager that caches each Part in its own file.
 */
public class FileStashManager implements StashManager {
    private final File parentDirectory;
    private final String uniqueFilenamePrefix;

    private ArrayList<FileInfo> stashed = new ArrayList<FileInfo>();
    private ArrayList<IOException> thrown = new ArrayList<IOException>();

    /**
     * Create a new FileStashManager that will stash files into the specified parent directory, creating
     * files by appending the ordinal to the specified unique filename prefix.  Caller is responsible
     * for ensuring that the parentDirectory already exists and is writable by the current process, and
     * that no two FileStashManagers will ever be using the same directory and unique prefix simultaneously.
     * <p>
     * Stashed InputStreams will be stored in files named prefix_ordinal.part where prefix is the uniqueFilenamePrefix
     * and ordinal is the ordinal that was used to identify a particular stashed InputStream.
     *
     * @param parentDirectory      the directory in which to store the stash files
     * @param uniqueFilenamePrefix the unique filename prefix with which to disambiguate the files
     * @throws IOException         if the parent directory does not exist or is not writable
     */
    public FileStashManager(File parentDirectory, String uniqueFilenamePrefix) throws IOException {
        if (uniqueFilenamePrefix == null || uniqueFilenamePrefix.length() < 1)
            throw new IllegalArgumentException("Unique filename prefix is missing or empty");
        if (parentDirectory == null)
            throw new IllegalArgumentException("parentDirectory must not be null");
        if (!parentDirectory.isDirectory() || !parentDirectory.canWrite())
            throw new IOException("parentDirectory must be an already-existing writable directory");
        this.parentDirectory = parentDirectory;
        this.uniqueFilenamePrefix = uniqueFilenamePrefix;
    }

    public void stash(int ordinal, InputStream in) throws IOException {
        try {
            while (stashed.size() <= ordinal) stashed.add(null);
            while (thrown.size() <= ordinal) thrown.add(null);
            unstash(ordinal);
            FileInfo fi = new FileInfo(makeFile(ordinal));
            final OutputStream outputStream = fi.getOutputStream();
            IOUtils.copyStream(in, outputStream);
            outputStream.flush();
            outputStream.close();
            fi.closeOutputStream();
            stashed.set(ordinal, fi);
            thrown.set(ordinal, null);
        } catch (IOException e) {
            thrown.set(ordinal, e);
            throw e;
        }
    }

    public void stash(int ordinal, byte[] in) throws IOException {
        stash(ordinal, new ByteArrayInputStream(in)); // byte array doesn't help us in this case
    }

    /** @return a File pointed at the name uniqueFilenamePrefix_ordinal.part in parentDirectory */
    private File makeFile(int ordinal) {
        return new File(parentDirectory, uniqueFilenamePrefix + "_" + ordinal + ".part");
    }

    /**
     * Ensure that the specified file is not present, by deleting it if it's there.
     *
     * @param ordinal the ordinal that will be unstashed.
     */
    public void unstash(int ordinal) {
        FileInfo fi = getFileInfo(ordinal);
        if (fi == null) {
            makeFile(ordinal).delete(); // make sure no old file is in the way
            return;
        }
        fi.close();
        stashed.set(ordinal, null);
        if (ordinal >= 0 && ordinal < thrown.size())
            thrown.set(ordinal, null);
    }

    /** @return the stashed FileInfo for the specified ordinal, or null if there isn't one. */
    private FileInfo getFileInfo(int ordinal) {
        if (stashed.size() <= ordinal || ordinal < 0)
            return null;
        return stashed.get(ordinal);
    }

    public long getSize(int ordinal) {
        FileInfo fi = getFileInfo(ordinal);
        return fi == null ? -1 : fi.getSize();
    }

    public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
        rethrowStashIOException(ordinal);
        FileInfo fi = getFileInfo(ordinal);
        if (fi == null)
            throw new NoSuchPartException("No part stashed with ordinal " + ordinal);

        return fi.getInputStream();
    }

    // re-throws the IOException that was thrown during stash(), if any
    private void rethrowStashIOException(int ordinal) throws IOException {
        if (thrown.size() <= ordinal || ordinal < 0)
            return;

        IOException ioex = thrown.get(ordinal);
        if (ioex != null) throw ioex;
    }

    public boolean isByteArrayAvailable(int ordinal) {
        return false;
    }

    public byte[] recallBytes(int ordinal) throws NoSuchPartException {
        throw new NoSuchPartException("Parts cached in files do not keep the byte array in memory");
    }

    public boolean peek(int ordinal) {
        return getFileInfo(ordinal) != null;
    }

    public int getMaxOrdinal() {
        return stashed.size();
    }

    public void close() {
        close0();
    }

    private void close0() {
        for (FileInfo fileInfo: stashed) {
            if (fileInfo != null) fileInfo.close();
        }
        stashed.clear();
    }

    private static class FileInfo {
        private File file;
        private Long size = null;
        private Map<InputStream,FileInfo> inputStreams = new WeakHashMap<InputStream,FileInfo>(); // map of outstanding inputstreams so they can be closed if necessary
        private OutputStream outputStream = null; // stored here while file is being stashed so it can be closed if necessary

        private FileInfo(File file) {
            if (file == null) throw new NullPointerException();
            this.file = file;
        }

        /**
         * Prepare an OutputStream ready to write to this file.
         *
         * @return an OutputStream ready to write to the file. Never null.
         * @throws FileNotFoundException if the file exists but is a directory
         *                   rather than a regular file, does not exist but cannot
         *                   be created, or cannot be opened for any other reason.
         * @throws IllegalStateException if there is already an OutputStream active on this FileInfo.
         */
        private OutputStream getOutputStream() throws FileNotFoundException {
            if (file == null) throw new IllegalStateException("FileInfo is closed");
            if (outputStream != null) throw new IllegalStateException("Already have an active OutputStream");
            return outputStream = new FileOutputStream(file);
        }

        /**
         * Indicate unambiguously that the output stream has been completely written, and that InputStreams
         * can now be generated.  If this has not been called before the first call to getInputStream it will
         * be assumed automatically at that point.
         * <p>
         * Since this method eats any IOExceptions that might occur when the OutputStream is closed,
         * callers are advised to close the OutputStream themselves before calling this method.
         */
        private void closeOutputStream() {
            try { outputStream.close(); } catch (IOException e) { /* expected to throw */ }
            outputStream = null;
        }

        /**
         * Prepare an InputStream ready to read from this file.  A call to this method will cause FileInfo to
         * assume that the OutputStream has already been completely written -- if an OutputStream is currently
         * active, it will be closed before the InputStream is created.
         *
         * @return an InputStream ready to read from the file.  Multiple InputStreams can exist reading the
         *         same file at the same time.  Might be closed on you if someone calls FileInfo.close().
         *         Will never return null.
         * @throws FileNotFoundException if the file does not exist,
         *                   is a directory rather than a regular file,
         *                   or for some other reason cannot be opened for
         *                   reading.
         */
        private InputStream getInputStream() throws FileNotFoundException {
            if (file == null) throw new IllegalStateException("FileInfo is closed");
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* expected to throw */ }
                outputStream = null;
            }
            InputStream is = new FileInputStream(file);
            inputStreams.put(is, this);
            return is;
        }

        /**
         * Close all streams referring to the this file, and then close the file and delete it.
         */
        private void close() {
            close0();
        }

        private void close0() {
            if (outputStream != null) {
                try {outputStream.close();} catch (IOException e) { /* expected to throw */ }
                outputStream = null;
            }
            if (inputStreams != null) {
                for (InputStream inputStream : inputStreams.keySet()) {
                    if (inputStream != null)
                        try {
                            inputStream.close();
                        } catch (IOException e) { /* expected to throw */ }
                }
                inputStreams.clear();
                inputStreams = null;
            }
            if (file != null) {
                file.delete();
                file = null;
            }
        }

        /** @return the size of this file, if known, or -1 if it's not available. */
        public long getSize() {
            if (file == null) throw new IllegalStateException("FileInfo is closed");
            if (size == null) {
                if (outputStream != null)
                    return -1; // can't check size while outputstream is running
                if (!file.exists())
                    return -1;
                size = file.length();
            }
            return size;
        }

        protected void finalize() throws Throwable {
            close0(); // doesn't need sync
            super.finalize();
        }
    }
}
