/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileLock;
import java.nio.channels.Channels;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility methods to approximate Unix-style transactional file replacement in Java.
 * <p/>
 * User: mike
 * Date: Jul 30, 2003
 * Time: 2:58:00 PM
 */
public class FileUtils {
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());
    private static String defaultDir;

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
     * @param path  the pathname of the file to atomically overwrite.  Required.
     * @param saver a Saver that will produce the bytes that are to be saved to this file.  Required.
     * @throws java.io.IOException if there is a problem saving the file
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
     * @param path  the path to the file to load.  Required.
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if there is a problem reading the file
     * @return a LastModifiedFileInputStream from which can be read the bytes of the file and its
     *         last modified time.  Never null.
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
     * Recursively deletes a non-empty directory and all its descendants.
     *
     * @param dir  the directory to delete.  Required.
     * @return true if the directory was deleted.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Ensures that a parent directory exists, creating intermediate directories as necessary.
     *
     * @param in the path whose directory should be ensured to exist.  Required.
     */
    public static void ensurePath(File in) {
        if (in.getParentFile() != null) {
            ensurePath(in.getParentFile());
        }
        if (!in.exists()) {
            in.mkdir();
        }
    }

    /**
     * Cause the specified file to exist, if it doesn't already.
     * The file will be created as a zero-length file.
     *
     * @param file the file that should be created.  Required.
     * @throws java.io.IOException if there is a problem checking for the existence of or creating the file
     */
    public static void touch(File file) throws IOException {
        if (file.getParentFile() != null)
            ensurePath(file.getParentFile());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            fos.getChannel().force(true);
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    /**
     * Breaks down a file path into its canonical absolute path components. This is system-dependent.
     *
     * <p>Example: if a path is /a/b/c/d, the components are [a, b, c, d].
     *
     * @param f a file path
     * @return a list of the individual path components
     * @throws IOException if an I/O error occurs, which is possible because the construction of the canonical pathname may require filesystem queries
     */
    private static List<String> getAbsolutePathComponents(File f) throws IOException {
        final List<String> l = new ArrayList<String>();
        File c = f.getCanonicalFile();
        while (c != null) {
            l.add(0, c.getName());
            c = c.getParentFile();
        }
        return l;
    }

    /**
     * Computes the relative path of <code>target</code> with respect to <code>home</code>.
     *
     * @param home      the reference location
     * @param target    the target location
     * @return the relative path
     * @throws IOException if an I/O error occurs, which is possible because the construction of the canonical pathname may require filesystem queries
     */
    public static String getRelativePath(File home, File target) throws IOException {
        if (home.equals(target)) {
            // Pathological case.
            return ".." + File.separator + target.getName();
        }

        final List h = getAbsolutePathComponents(home);
        final List t = getAbsolutePathComponents(target);
        final StringBuilder sb = new StringBuilder();

        // First eliminates common root levels.
        int i = 0;
		int j = 0;
        while(i < h.size() && j < t.size() && h.get(i).equals(t.get(j))) {
            ++ i;
            ++ j;
        }

		// For each remaining level in the home path, add a ..
		for (; i < h.size(); ++ i) {
			sb.append("..");
            sb.append(File.separator);
		}

		// For each remaining level in the file path, add the component.
		for (; j < t.size() - 1; ++ j) {
			sb.append(t.get(j));
            sb.append(File.separator);
		}
        sb.append(t.get(j));

        return sb.toString();
    }

    /**
     * Locate a pathname in a way that is configurable via a system property, but has defaults, and ensures
     * that the file meets certain requirements before returning.
     * <p/>
     * We will check for the file's existence and other requirements before returning.  However there is no
     * guarantee that the situation won't change in between our checks and when you go to use the resulting
     * File for something.
     *
     * @param thing  the name of the thing being sought, for constructing error messages.  required
     * @param sysprop     system property to check for an overridden path to the thing. required
     * @param defaultValue  a default value to use for the path if the system property isn't set.  required
     * @param mustBeFile    if true, IOException will be thrown if the path doesn't point at a plain file
     * @param mustBeDirectory  if true, IOException will be thrown if the path doesn't point at a directory
     * @param mustBeReadable   if true, IOException will be thrown if the path points at a file that isn't readable by the current process
     * @param mustBeWritable   if true, IOException will be thrown if the path points at a file that isn't writable by the current process
     * @param mustBeExecutable if true, IOException will be thrown if the path points at a file that isn't executable by the current process
     * @return the requested File.  Never null.
     * @throws java.io.IOException if an appropriate File couldn't be located
     */
    public static File findConfiguredFile(String thing, String sysprop, String defaultValue,
                                          boolean mustBeFile, boolean mustBeDirectory, boolean mustBeReadable, boolean mustBeWritable, boolean mustBeExecutable)
            throws IOException
    {
        String path = SyspropUtil.getString(sysprop, defaultValue);
        if (path == null || path.length() < 1)
            throw new IOException("Unable to find " + thing + ": System property " + sysprop + " is not valid");
        File file = new File(path);
        if (!file.exists())
            throw new IOException("Unable to find " + thing + " at path: "
                                  + path + ".  Set system property " + sysprop + " to override.");
        if (mustBeFile && !file.isFile())
            throw new IOException(thing + " at path: "
                                  + path + " is not a plain file.  Set system property " + sysprop + " to override.");
        if (mustBeDirectory && !file.isDirectory())
            throw new IOException(thing + " at path: "
                                  + path + " is not a directory.  Set system property " + sysprop + " to override.");
        if (mustBeReadable && !file.canRead())
            throw new IOException(thing + " at path: "
                                  + path + " is not readable by this process.  Set system property " + sysprop + " to override.");
        if (mustBeWritable && !file.canWrite())
            throw new IOException(thing + " at path: "
                                  + path + " is not writable by this process.  Set system property " + sysprop + " to override.");
        if (mustBeExecutable && !canExecute(file, true))
            throw new IOException(thing + " at path: "
                                  + path + " is not executable by this process.  Set system property " + sysprop + " to override.");
        return file;
    }

    /**
     * If this code is running in a Java 6 JVM, this method is equivalent to file.canExecute().  Otherwise,
     * it just returns fallbackResult.
     * <p/>
     * This method is currently very slow.
     *
     * @param file the file to check.  Required
     * @param fallbackResult the result to return if the file's execute status can't be determined.
     * @return the value of file.canExecute() if this is a Java 6 JVM; otherwise, fallbackResult
     */
    public static boolean canExecute(File file, boolean fallbackResult) {
        try {
            Method canex = File.class.getMethod("canExecute", new Class[0]);
            return (Boolean)canex.invoke(file);
        } catch (NoSuchMethodException e) {
            return fallbackResult;
        } catch (InvocationTargetException e) {
            return fallbackResult;
        } catch (IllegalAccessException e) {
            return fallbackResult;
        }
    }

    /**
     * Find the default directory, which is the same as the home directory in Linux/Unix, but different from the home
     * directory in Windows.  For example, the default directory in Windows is "C:\...\Usernmae\My Document" and the
     * home directory is "C:\...\Username".
     *
     * @return the default directory.
     */
    public static String getDefaultDirectory() {
        String result = defaultDir;
        if (result == null) {
            result = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath();
            defaultDir = result;
        }
        return result;
    }
}
