/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to approximate Unix-style transactional file replacement in Java.
 * <p/>
 * User: mike
 * Date: Jul 30, 2003
 * Time: 2:58:00 PM
 */
public class FileUtils {
    private static FileNameExtensionFilter imageFileFilter = new FileNameExtensionFilter("Image file filter", "gif", "png", "jpg", "jpeg");
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());
    private static String defaultDir;

    /**
     * @return a FileNameExtensionFilter which only accepts gif, png, jpg, or jpeg.
     */
    public static FileNameExtensionFilter getImageFileFilter() {
        return imageFileFilter;
    }

    /**
     * Delete a file.  This just calls {@link File#delete}, but translates a false return value into
     * an IOException.  Also, this method silently succeeds if the named file does not exist.
     *
     * @param file the file to delete.  Required.
     * @throws IOException if the deletion fails.
     */
    public static void delete(File file) throws IOException {
        if (!file.exists())
            return;
        if (!file.delete())
            throw new IOException("Delete failed for file: " + file);
    }

    /**
     * Create a directory.  This method just calls {@link File#mkdir}, but translates a false return value
     * into an IOException.  Also, this method silently succeeds if the named directory already exists.
     *
     * @param file the directory to create.  Required.
     * @throws IOException if the creation fails.
     */
    public static void mkdir(File file) throws IOException {
        if (file.isDirectory())
            return;
        if (!file.mkdir())
            throw new IOException("Unable to create directory: " + file);
    }

    /**
     * Rename a file.  This method just calls {@link File#renameTo}, but translates a false return value
     * into an IOException.
     *
     * @param oldName  the existing filename.  Required.
     * @param newName  the desired new filename.  Required.
     * @throws IOException if the rename fails.
     */
    public static void rename(File oldName, File newName) throws IOException {
        if (!oldName.renameTo(newName))
            throw new IOException("Unable to rename file from " + oldName + " to " + newName);
    }


    /**
     * Interface implemented by those who wish to call saveFileSafely().
     */
    public interface Saver {
        void doSave(FileOutputStream fos) throws IOException;
    }

    /**
     * A simple {@link Saver} that saves a single byte array.
     */
    public static class ByteSaver implements Saver {
        private final byte[] bytes;

        /**
         * @param bytes bytes to save.  Required.
         */
        public ByteSaver(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public void doSave(FileOutputStream fos) throws IOException {
            fos.write(bytes);
        }
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
        saveFileSafely(path, false, saver);
    }

    /**
     * Safely overwrite the specified file with new information, with sync to disk.  A lock file will be used to ensure
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
     * @param sync  if true, we will attempt to sync the file descriptor after writing the new file.  May cause a SyncFailedException if this cannot be done.
     * @param saver a Saver that will produce the bytes that are to be saved to this file.  Required.
     * @throws java.io.IOException if there is a problem saving the file
     * @throws java.io.SyncFailedException if sync is requested but the sync fails.
     */
    public static void saveFileSafely(String path, boolean sync, Saver saver) throws IOException {
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
                mkdir(curFile.getParentFile());

            // Get file lock
            //noinspection ResultOfMethodCallIgnored
            lckFile.createNewFile();
            lockRaf = new RandomAccessFile(lckFile, "rw");
            lock = lockRaf.getChannel().lock();

            // At start: any state is possible

            if (oldFile.exists() && !curFile.exists())
                rename(oldFile, curFile);
            // States 5 and 6 now ruled out

            if (newFile.exists())
                delete(newFile);
            // States 2, 4, 6 and 8 now ruled out

            if (oldFile.exists())
                delete(oldFile);
            // States 5, 6, 7, and 8 now ruled out

            // We are now in either State 1 or State 3

            // Do the actual updating the file contents.
            out = new FileOutputStream(newFile);
            saver.doSave(out);
            if (sync) {
                if (!out.getFD().valid())
                    throw new SyncFailedException("Unable to sync as requested: the Saver has already closed the output file");
                out.flush();
                out.getFD().sync();
            }
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

            delete(oldFile);

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
            //noinspection ResultOfMethodCallIgnored
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
                //noinspection ResultOfMethodCallIgnored
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
            //noinspection ResultOfMethodCallIgnored
            lckFile.delete();
        }
    }

    /**
     * Save the bytes into the given output file, replacing any existing file contents.
     * <p/>
     * This save utility can leave a partially-written file if there is a system crash during the save.
     * See {@link #saveFileSafely(String, com.l7tech.util.FileUtils.Saver)} for a slightly safer way
     * to save files (at the cost of possibly leaving behind .NEW and .OLD and .LCK files).
     *
     * @param bytes the bytes to save.  Required.
     * @param out the file to overwrite.  Required.  Needn't already exist.  Must be writable by current process.
     * @throws IOException on io error
     */
    public static void save(byte[] bytes, File out) throws IOException {
        save(new ByteArrayInputStream(bytes), out);
    }

    /**
     * Saves the inputstream into the given output file.
     *
     * Caller is responsible for closing the input stream.
     * <p/>
     * This save utility can leave a partially-written file if there is a system crash during the save.
     * See {@link #saveFileSafely(String, com.l7tech.util.FileUtils.Saver)} for a slightly safer way
     * to save files (at the cost of possibly leaving behind .NEW and .OLD and .LCK files).
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
     * Saves information into the given output file
     * using the specified Saver.
     * <p/>
     * This save utility can leave a partially-written file if there is a system crash during the save.
     * See {@link #saveFileSafely(String, com.l7tech.util.FileUtils.Saver)} for a slightly safer way
     * to save files (at the cost of possibly leaving behind .NEW and .OLD and .LCK files).
     *
     * @param out a File to which the information is to be saved.  The file will be overwritten
     *            without any confirmation or atomicity safeguards.
     * @param saver a Saver that will save information to the file.
     * @throws IOException if there is a problem saving the file.
     */
    public static void save(File out, Saver saver) throws IOException {
        save(out, false, saver);
    }

    /**
     * Saves information into the given output file
     * using the specified Saver, with optional sync to disk.
     * <p/>
     * This save utility can leave a partially-written file if there is a system crash during the save.
     * See {@link #saveFileSafely(String, com.l7tech.util.FileUtils.Saver)} for a slightly safer way
     * to save files (at the cost of possibly leaving behind .NEW and .OLD and .LCK files).
     *
     * @param out a File to which the information is to be saved.  The file will be overwritten
     *            without any confirmation or atomicity safeguards.
     * @param sync  if true, this method will call sync on the descriptor and will not return until the information
     *              has been fully written to disk.
     * @param saver a Saver that will save information to the file.
     * @throws IOException if there is a problem saving the file.
     */
    public static void save(File out, boolean sync, Saver saver) throws IOException {
        if (saver == null || out == null) {
            throw new IllegalArgumentException();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(out);
            saver.doSave(fos);
            if (sync) {
                fos.flush();
                fos.getFD().sync();
            }
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    /**
     * Read an entire file into memory.
     * <p/>
     * This method simply reads the specified file.  If you want to notice and recover from an incomplete
     * previous call to {@link #saveFileSafely(String, com.l7tech.util.FileUtils.Saver)}, use
     * {@link #loadFileSafely(String)} instead of this method.
     *
     * @param file  the file to read.  Required.
     * @return the content of the file.  May be empty but never null.
     * @throws java.io.IOException if the file wasn't found or couldn't be read.
     */
    public static byte[] load(File file) throws IOException {
        return IOUtils.slurpFile(file);
    }

    /**
     * Copies one file into output file.  It will copy the file entirely in the
     * kernel space on operating systems with  appropriate support.
     *
     * @param in  the input file, file must exist, cannot be null
     * @param out the output, file must exist, cannot be null
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
     * Convenience method to delete the contents of a direcotory. The directory itself will not be deleted
     * @param dir the directory to empty. Cannot be null. It's contents will be deleted
     * @return true if all the contents could be successfully deleted. Returns false on the first problem deleting
     * a file. Returns true if dir is not a directory
     */
    public static boolean deleteDirContents(File dir){
        if(dir == null) throw new NullPointerException("dir cannot be null");
        if(!dir.isDirectory())
            throw new IllegalArgumentException("dir must be a directory, whose contents are to be deleted");
        
        File [] files = dir.listFiles();
        for(File f: files){
            if(f.isDirectory()){
                if(!deleteDir(f)) return false;
            }else{
                if(!f.delete()) return false;
            }
        }

        return true;
    }
    /**
     * Ensures that a parent directory exists, creating intermediate directories as necessary.
     *
     * @param in the path whose directory should be ensured to exist.  Required.
     * @throws java.io.IOException if the directory cannot be created.
     */
    public static void ensurePath(File in) throws IOException {
        if (in.getParentFile() != null) {
            ensurePath(in.getParentFile());
        }
        if (!in.exists()) {
            mkdir(in);
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


    /**
     * Until it makes it into java.io.File
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4735419
     *
     * @param prefix  prefix, to insert before random portion of filename.  Required.
     * @param suffix  suffix, to append after random portion of filename, or null to default to ".tmp".
     * @param directory  directory in which to create the temp directory, or null to look up the value in the java.io.tmddir system property.
     * @param deleteOnExit  if true, the temp directory will be scheduled for deletion on exit of the JDK.
     * @return the newly-created temp directory.  Never null.
     * @throws IOException if we are unable to create a temporary file in the specified directory.
     */
    public static File createTempDirectory(String prefix, String suffix, File directory, boolean deleteOnExit) throws IOException {
        if (prefix == null) throw new NullPointerException();
        if (prefix.length() < 3)
            throw new IllegalArgumentException("Prefix string too short");
        String s = (suffix == null) ? ".tmp" : suffix;

        if (directory == null) {
            String tmpDir = SyspropUtil.getProperty( "java.io.tmpdir" );
            directory = new File(tmpDir);
        }

        if (!directory.exists())
            throw new IOException("Unable to create temporary directory within parent directory " + directory + ": directory does not exist");
        if (!directory.isDirectory())
            throw new IOException("Unable to create temporary directory within parent directory " + directory + ": not a directory");

        for (int a = 1; a < 20; a++) {
            File f = generateFile(prefix, s, directory);
            if (f.mkdir()) {
                if (deleteOnExit) f.deleteOnExit();
                return f;
            }
        }
        throw new IOException("Unable to create temporary directory within parent directory " + directory + " (possible permission problem?)");
    }

    private static final SecureRandom random = new SecureRandom();
    private static File generateFile(String prefix, String suffix, File dir) {
        long n = random.nextLong();
        return new File(dir, prefix + Long.toString(n == Long.MIN_VALUE ? 0 : Math.abs(n)) + suffix);
    }
}
