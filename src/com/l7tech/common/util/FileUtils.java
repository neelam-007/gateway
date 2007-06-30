/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileLock;
import java.nio.channels.Channels;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility methods to approximate Unix-style transactional file replacement in Java.
 * <p/>
 * User: mike
 * Date: Jul 30, 2003
 * Time: 2:58:00 PM
 */
public class FileUtils {
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

    /**
     * Interface implemented by those who wish to call saveFileSafely().
     */
    public interface Saver {
        void doSave(FileOutputStream fos) throws IOException;
    }

    /**
     * Safely overwrite the specified file with new information.  A lock file will be used to ensure
     * that only one thread at a time will be attempting to save or load the file at the same time.
     * <p/>
     * <pre>
     *    oldFile   curFile  newFile  Description                    Action to take
     *    --------  -------  -------  -----------------------------  --------------------------------
     *  1    -         -        -     Newly created store file       (>newFile) => curFile
     *  2    -         -        +     Create was interrupted         -newFile; (do #1)
     *  3    -         +        -     Normal operation               curFile => oldFile; (do #1); -oldFile
     *  4    -         +        +     Update was interrupted         -newFile; (do #3)
     *  5    +         -        -     Update was interrupted         oldFile => curFile; (do #3)
     *  6    +         -        +     Update was interrupted         -newFile; (do #5)
     *  7    +         +        -     Update was interrupted         -oldFile; (do #3)
     *  8    +         +        +     Invalid; can't happen          -newFile; -oldFile; (do #3)
     * </pre>
     * <p/>
     * We guarantee to end up in state #3 if we complete successfully.
     */
    public static void saveFileSafely(String path, Saver saver) throws IOException {
        FileOutputStream out = null;
        RandomAccessFile lockRaf = null;
        FileLock lock = null;

        try {
            File lckFile = new File(path + ".LCK");
            File oldFile = new File(path + ".OLD");
            File curFile = new File(path);
            File newFile = new File(path + ".NEW");

            if (curFile.isDirectory())
                throw new IllegalArgumentException("Unable to save file " + path + ": it is a directory");

            // Make directory if needed
            if (curFile.getParentFile() != null)
                curFile.getParentFile().mkdir();

            // Get file lock
            lckFile.createNewFile();
            lockRaf = new RandomAccessFile(lckFile, "rw");
            lock = lockRaf.getChannel().lock();

            // At start: any state is possible

            if (oldFile.exists() && !curFile.exists())
                oldFile.renameTo(curFile);
            // States 5 and 6 now ruled out

            if (newFile.exists())
                newFile.delete();
            // States 2, 4, 6 and 8 now ruled out

            if (oldFile.exists())
                oldFile.delete();
            // States 5, 6, 7, and 8 now ruled out

            // We are now in either State 1 or State 3

            // Do the actual updating the file contents.
            out = new FileOutputStream(newFile);
            saver.doSave(out);
            out.close();
            out = null;

            // If interrupted here, we end up in State 4 (or State 2 if no existing file)

            if (curFile.exists())
                if (!curFile.renameTo(oldFile))
                // If we need to do this, it has to succeed
                    throw new IOException("Unable to rename " + curFile + " to " + oldFile);

            // If interrupted here, we end up in State 6 (or State 2 if was no existing file)

            if (!newFile.renameTo(curFile))
            // This must succeed in order for the update to complete
                throw new IOException("Unable to rename " + newFile + " to " + curFile);

            // If interrupted here, we end up in State 7 (or State 3 if was no existing file)

            oldFile.delete();

            // We are now in State 3 (invariant)

        } finally {
            if (out != null)
                try { out.close(); }
                catch (IOException e) {
                    // Could happen normally if there was an earlier IOException
                    logger.log(Level.WARNING, "Unable to close file: " + e.getMessage(), e);
                }
            if (lock != null)
                try { lock.release(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to release lock: " + e.getMessage(), e);
                }
            if (lockRaf != null)
                try { lockRaf.close(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to close lock file: " + e.getMessage(), e);
                }
        }
    }

    public static class LastModifiedFileInputStream extends FileInputStream {
        private final long lastModified;

        LastModifiedFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.lastModified = file.lastModified();
        }

        public long getLastModified() {
            return lastModified;
        }
    }

    /**
     * Safely open a file that was saved with saveFileSafely().  Handles recovery in case the most recent
     * save to the file was interrupted.
     *
     * @param path
     * @throws FileNotFoundException
     */
    public static LastModifiedFileInputStream loadFileSafely(String path) throws IOException {
        RandomAccessFile lockRaf = null;
        FileLock lock = null;
        LastModifiedFileInputStream in = null;
        try {
            // Get file lock
            File lckFile = new File(path + ".LCK");
            lckFile.createNewFile();
            lockRaf = new RandomAccessFile(lckFile, "rw");
            lock = lockRaf.getChannel().lock();

            in = new LastModifiedFileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            // Check for an interrupted update operation
            in = new LastModifiedFileInputStream(new File(path + ".OLD"));
        } finally {
            if (lock != null)
                try { lock.release(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to release lock: " + e.getMessage(), e);
                }
            if (lockRaf != null)
                try { lockRaf.close(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to close lock file: " + e.getMessage(), e);
                }
        }
        return in;
    }

    /**
     * Ensure that path is deleted, along with any .OLD or .NEW files that might be laying around.
     * The actual file will be deleted last, guaranteeing that the delete will be atomic (ie, no future call
     * to loadFileSafely() will recover an out-of-date version of the file if we are interrupted in mid-delete).
     *
     * @param path the path to delete
     * @return true if some files were deleted, or false if none were found
     */
    public static boolean deleteFileSafely(String path) {
        File lckFile = new File(path + ".LCK");
        RandomAccessFile lockRaf = null;
        FileLock lock = null;
        try {
            boolean deletes = false;

            // Get file lock
            try {
                lckFile.createNewFile();
                lockRaf = new RandomAccessFile(lckFile, "rw");
                lock = lockRaf.getChannel().lock();
            } catch (FileNotFoundException e) {
                deletes = true; // can't happen
            } catch (IOException e) {
                deletes = true; // we tried; now we'll just try to delete the lock file
            }

            if (new File(path + ".OLD").delete())
                deletes = true;
            if (new File(path + ".NEW").delete())
                deletes = true;
            if (new File(path).delete())
                deletes = true;
            return deletes;
        } finally {
            if (lock != null) {
                try { lock.release(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to release lock: " + e.getMessage(), e);
                }
            }
            if (lockRaf != null) {
                try { lockRaf.close(); }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to close lock file: " + e.getMessage(), e);
                }
            }
            lckFile.delete();
        }
    }

    /**
     * Saves the inputstream into the given output file.
     *
     * Caller is responsible for closing the input stream.
     *
     * @param in  the input file
     * @param out the output file
     * @throws IOException on io error
     */
    public static void save(InputStream in, File out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException();
        }
        final ReadableByteChannel sourceChannel;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = Channels.newChannel(in);
            destinationChannel = new FileOutputStream(out).getChannel();
            destinationChannel.transferFrom(sourceChannel, 0, Integer.MAX_VALUE);
        }
        finally {
            ResourceUtils.closeQuietly(destinationChannel);
        }
    }

    /**
     * Copies one file into output file.  It will copy the file entirely in the
     * kernel space on operating systems with  appropriate support.
     *
     * @param in  the input file
     * @param out the output
     * @throws IOException in io error
     */
    public static void copyFile(File in, File out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException();
        }
        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(in).getChannel();
            destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        }
        finally {
            ResourceUtils.closeQuietly(sourceChannel);
            ResourceUtils.closeQuietly(destinationChannel);
        }
    }

    /**
     * deletes a non-empty directory
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                String child = children[i];
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void ensurePath(File in) {
        if (in.getParentFile() != null) {
            ensurePath(in.getParentFile());
        }
        if (!in.exists()) {
            in.mkdir();
        }
    }
}
