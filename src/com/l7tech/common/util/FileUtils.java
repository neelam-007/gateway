/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.io.InputStreamChannel;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Utility methods to approximate Unix-style transactional file replacement in Java.
 * <p/>
 * User: mike
 * Date: Jul 30, 2003
 * Time: 2:58:00 PM
 */
public class FileUtils {

    /**
     * Interface implemented by those who wish to call saveFileSafely().
     */
    public interface Saver {
        void doSave(FileOutputStream fos) throws IOException;
    }

    /**
     * Safely overwrite the specified file with new information.
     * Caller is responsible for ensuring that only one thread will be attempting to save or load
     * the same file at the same time.
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

        try {
            File oldFile = new File(path + ".OLD");
            File curFile = new File(path);
            File newFile = new File(path + ".NEW");

            if (curFile.isDirectory())
                throw new IllegalArgumentException("Unable to save file " + path + ": it is a directory");

            // Make directory if needed
            if (curFile.getParentFile() != null)
                curFile.getParentFile().mkdir();

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
                out.close();
        }
    }

    /**
     * Safely open a file that was saved with saveFileSafely().  Handles recovery in case the most recent
     * save to the file was interrupted.  Caller is responsible for ensuring that only one thread attempts
     * to save or load the same file at any given time.
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     */
    public static FileInputStream loadFileSafely(String path) throws FileNotFoundException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            // Check for an interrupted update operation
            in = new FileInputStream(path + ".OLD");
        }
        return in;
    }

    /**
     * Ensure that path is deleted, along with any .OLD or .NEW files that might be laying around.
     * The actual file will be deleted last, guaranteeing that the delete will be atomic (ie, no future call
     * to loadFileSafely() will recover an out-of-date version of the file if we are interrupted in mid-delete).
     * Caller is responsible for ensuring that only one thread attempts to save or load the same file at any time.
     *
     * @param path the path to delete
     * @return true if some files were deleted, or false if none were found
     */
    public static boolean deleteFileSafely(String path) {
        boolean deletes = false;
        if (new File(path + ".OLD").delete())
            deletes = true;
        if (new File(path + ".NEW").delete())
            deletes = true;
        if (new File(path).delete())
            deletes = true;
        return deletes;
    }

    /**
     * Saves the inputstream into the given output file.
     * On successfull operation closes the input stream <code>in</code>.
     *
     * @param in  the input file
     * @param out the output file
     * @throws IOException on io error
     */
    public static void save(InputStream in, File out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException();
        }
        ReadableByteChannel sourceChannel = new InputStreamChannel(in);
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        destinationChannel.transferFrom(sourceChannel, 0, Integer.MAX_VALUE);
        sourceChannel.close();
        destinationChannel.close();
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
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }
}
