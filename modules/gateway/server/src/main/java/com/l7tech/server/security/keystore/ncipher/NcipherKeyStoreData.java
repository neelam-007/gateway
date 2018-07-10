package com.l7tech.server.security.keystore.ncipher;

import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.KeyStoreException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Holds keystore metadata (40-character hex identifier string) and relevant keystore and module fileset for saving/restoring
 * an nCipher Java keystore within an already-programmed nCipher security world (in /opt/nfast/kmdata/local).
 * <p/>
 * We make no attempt to zip or otherwise compress the files in the fileset becuase this would just be a waste
 * of CPU cycles because the files are already encrypted and hence uncompressible.
 */
class NcipherKeyStoreData implements Serializable {
    private static final Logger logger = Logger.getLogger(NcipherKeyStoreData.class.getName());
    private static final long serialVersionUID = -1890805696377529829L;

    private static final int MAX_DELETED_FILENAMES_TO_TRACK = ConfigFactory.getIntProperty( "com.l7tech.keystore.ncipher.maxDeletedFilenames", 5000 );

    private static final Pattern PAT_ALPHANUM = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern PAT_ALPHANUM_WITH_DASH_AND_UNDERSCORE = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static final String APP_PREFIX_KEY_JCECSP = "key_jcecsp_"; // The nCipher Java KeyStore uses "key_jcecsp" as its application name for key blob storage purposes

    String keystoreMetadata;
    Map<String, byte[]> fileset = new HashMap<String,byte[]>();
    Set<String> deletedFilenames;
    // TODO: update the whitelist classes below (methid createClassFilter) if any of the above field types changes or adding new fields

    private NcipherKeyStoreData(String keystoreMetadata, Set<String> deletedFilenames) {
        this.keystoreMetadata = keystoreMetadata;
        this.deletedFilenames = new LinkedHashSet<String>(deletedFilenames);
    }

    /**
     * Pack this instance into bytes.
     *
     * @return the current instance in byte form.  Never null.
     * @throws java.security.KeyStoreException if the instance is not currently valid.
     */
    byte[] toBytes() throws KeyStoreException {
        validate();
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(8192);
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.flush();
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } finally {
            ResourceUtils.closeQuietly(baos);
        }
    }

    /**
     * Get a list of all key_jcecsp files that appear to be represented in the kmdata/local directory on the current node.
     *
     * @param kmdataLocalDir  the directory to load from, ie "/opt/nfast/kmdata/local".  Required.  Must be readable by the current process.
     * @param toIgnore keystore identifiers that should be ignored and omitted from the returned list, or null.
     * @return a list of 40-character keystore identifier strings, one for each file in the specified directory matching the pattern
     *         "^key_jcecsp_[0-9a-f]{40}}$" (where key_jcecsp_ is whatever is in use as the {@link #APP_PREFIX_KEY_JCECSP}).
     */
    static List<String> readKeystoreIdentifiersFromLocalDisk(File kmdataLocalDir, Set<String> toIgnore) {
        if (toIgnore == null)
            toIgnore = Collections.emptySet();
        final String prefix = APP_PREFIX_KEY_JCECSP;
        final int prefixLen = prefix.length();
        final Pattern idpattern = Pattern.compile("^[0-9a-f]{40}$");
        File[] files = kmdataLocalDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(prefix);
            }
        });

        List<String> ret = new ArrayList<String>();
        for (File file : files) {
            final String name = file.getName();
            if (!name.startsWith(prefix))
                continue;
            if (name.length() < prefixLen + 40)
                continue;
            String id = name.substring(prefixLen);
            if (!idpattern.matcher(id).matches())
                continue;
            if (toIgnore.contains(id))
                continue;
            ret.add(id);
        }
        return ret;
    }

    /**
     * Read an NcipherKeyStoreData instance from the local disk, assuming the specified keystoreIdentifier.
     * <p/>
     * This method should only be used to create a new NcipherKeyStoreData instance, when there is not already
     * a NcipherKeyStoreData in the database.
     *
     * @param keystoreIdentifier the keystore identifier.  Required.
     * @param kmdataLocalDir  the directory to load from, ie "/opt/nfast/kmdata/local".  Required.  Must be readable by the current process.
     * @return an NcipherKeyStoreData instance.  Never null.
     * @throws IOException if a file cannot be read.
     * @throws KeyStoreException if an invalid filename is found on disk with our application prefix.
     */
    static NcipherKeyStoreData createFromLocalDisk(String keystoreIdentifier, File kmdataLocalDir) throws IOException, KeyStoreException {
        final NcipherKeyStoreData nksd = new NcipherKeyStoreData(keystoreIdentifier, Collections.<String>emptySet());
        nksd.loadFilesetFromLocalDisk(kmdataLocalDir);
        return nksd;
    }

    /**
     * Read an NcipherKeyStoreData instance from the specified bytes.
     *
     * @param bytes the bytes to read.  Required.
     * @return an NcipherKeyStoreData instance.  Never null.
     * @throws IOException if the bytes cannot be decoded.
     */
    static NcipherKeyStoreData createFromBytes(byte[] bytes) throws IOException {
        if (bytes == null) throw new NullPointerException("nCipher keystore bytes is null");
        if (bytes.length < 1) throw new IOException("nCipher keystore bytes is empty");
        try (final ObjectInputStream ois = new ClassFilterObjectInputStream(new ByteArrayInputStream(bytes), CLASS_FILTER)) {
            Object obj = ois.readObject();
            if (obj == null)
                throw new IOException("Ncipher keystore data deserialized to null");
            if (!(obj instanceof NcipherKeyStoreData))
                throw new IOException("Ncipher keystore data deserialized to unexpected type: " + obj.getClass());
            return (NcipherKeyStoreData) obj;

        } catch (ClassNotFoundException | ClassNotPermittedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Check whether this instance appears to contain enough data to have a hope of reconstructing a valid nCipher keystore.
     * If this method returns normally, then at least the following tests have passed for this instance:
     * <ul>
     * <li>The keystore identifier is non-empty.
     * <li>The keystore identifier is at least 40 characters long.
     * <li>The keystore identifier contains no characters other than ASCII letters or digits.
     * <li>No files in the fileset are missing names or contents.
     * <li>No files in the fileset have names containing characters other than a-z, A-Z, 0-9, dashes, and underscores.
     * <li>All files in the fileset have names beginning with the application prefix "key_jcecsp_".
     * <li>No "module_*" or "world" files are present in the fileset. (separate rule in case previous rule restricting filenames to key_jcecsp_* is someday relaxed)
     * </ul>
     * Notably, this validation does <em>not</em> require that any "key_jcecsp_*" fileset be present in the fileset.
     * @throws java.security.KeyStoreException if any validation test fails
     */
    void validate() throws KeyStoreException {
        if (keystoreMetadata == null)
            throw new KeyStoreException("NcipherKeyStoreData validation failed: No keystoreMetadata present.");
        if (keystoreMetadata.length() < 40)
            throw new KeyStoreException("NcipherKeyStoreData validation failed: keystoreMetadata is too short.");
        if (!PAT_ALPHANUM.matcher(keystoreMetadata).matches())
            throw new KeyStoreException("NcipherKeyStoreData validation failed: keystoreMetadata contains illegal characters");

        if (fileset == null || fileset.isEmpty())
            return;

        for (Map.Entry<String, byte[]> entry : fileset.entrySet()) {
            String name = entry.getKey();
            byte[] bytes = entry.getValue();

            // We will permit zero-length files.
            if (bytes == null)
                throw new KeyStoreException("NcipherKeyStoreData validation failed: fileset contains file with null contents");

            validateName(name, "NcipherKeyStoreData validation failed: fileset contains invalid filename: ");
        }
    }

    private static void validateName(String name, String errPrefix) throws KeyStoreException {
        if (name == null)
            throw new KeyStoreException(errPrefix + "name is null");
        if (name.length() < 1)
            throw new KeyStoreException(errPrefix + "name is empty");
        if (!PAT_ALPHANUM_WITH_DASH_AND_UNDERSCORE.matcher(name).matches())
            throw new KeyStoreException(errPrefix + "filename contains illegal characters: " + name);
        if ("world".equalsIgnoreCase(name))
            throw new KeyStoreException(errPrefix + "fileset contains \"world\" file: " + name);
        if (name.toLowerCase().startsWith("module_"))
            throw new KeyStoreException(errPrefix + "fileset contains \"module\" file: " + name);
        if (name.toLowerCase().startsWith("card_"))
            throw new KeyStoreException(errPrefix + "fileset contains \"card\" file: " + name);
        if (!name.startsWith(APP_PREFIX_KEY_JCECSP))
            throw new KeyStoreException(errPrefix + "filename does not begin with \"" + APP_PREFIX_KEY_JCECSP + "\": " + name);
    }

    /**
     * Load the fileset from the local disk.  This information will be merged with any current fileset.
     *
     * @param kmdataLocalDir  the directory to load from, ie "/opt/nfast/kmdata/local".  Required.  Must be readable by the current process.
     * @throws KeyStoreException if an invalid filename is found on disk with our application prefix.  A fileset may have been partially loaded.
     * @throws IOException if a file cannot be read.  A fileset may have been partially loaded.
     */
    void loadFilesetFromLocalDisk(File kmdataLocalDir) throws IOException, KeyStoreException {
        final String prefix = APP_PREFIX_KEY_JCECSP + keystoreMetadata;
        File[] files = kmdataLocalDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(prefix);
            }
        });

        fileset = new HashMap<String,byte[]>();
        for (File file : files) {
            final String name = file.getName();
            validateName(name, "Unable to load nCipher keystore fileset from local disk: fileset contains invalid filename: ");
            if (deletedFilenames != null && deletedFilenames.contains(name)) // Ignore files already marked as deleted
                continue;
            byte[] bytes = IOUtils.slurpFile(file);
            if (fileset.containsKey(name))
                throw new KeyStoreException("Unable to load nCipher keystore fileset from local disk: fileset contains duplicate filename: " + name);
            fileset.put(name, bytes);
        }
    }

    /**
     * Note that some files have been explicitly deleted, and so should not be recreated by any cluster node.
     *
     * @param moreDeleted filenames that have been explicitly deleted.  Required.
     */
    void addDeletedFiles(Set<String> moreDeleted) {
        if (moreDeleted == null)
            throw new NullPointerException("moreDeleted must be provided");
        if (deletedFilenames == null)
            deletedFilenames = new LinkedHashSet<String>();
        deletedFilenames.addAll(moreDeleted);
    }

    /**
     * Begin the process of saving the fileset to the specified local directory, if this instance is currently valid.
     * Any existing files in this directory are left in place (unless they are overwritten by new data using
     * the same name).
     * <p/>
     * The save operation is done in two stages.  In stage one, the files from the fileset are written out to new files with "_new_" prefixes,
     * and checks are done (to the extent possible) whether a future attempt would succeed at moving the "_new_" files into their proper place (possibly
     * overwriting existing files).
     * <p/>
     * If it looks like a future commit will succeed, this method will return a set of written files than must later be passed to either
     * {@link #commitWrittenFiles(java.io.File, java.util.Set)} to commit the changes, or to {@link #rollbackWrittenFiles(java.io.File, java.util.Set)} to roll back the changes.
     * <p/>
     * If the initial writing fails for any reason, or if it looks like a future commit would fail, this method immediate rolls back any changed
     * files and throws an exception rather than returning.
     *
     * @param kmdataLocalDir  the directory to save to, ie "/opt/nfast/kmdata/local".  Required. Must be writable by the current process.
     * @throws IOException  if there is a problem writing a file.  While this method will try to avoid this, it is possible that files have been partially written.
     * @throws KeyStoreException  if the current instance is not valid.  In this case, no files have yet been written to disk.
     * @return the set of written files, which can then be committed or rolled back by passing them to either {@link #commitWrittenFiles(java.io.File, java.util.Set)} or
     * {@link #rollbackWrittenFiles(java.io.File, java.util.Set)}.
     */
    Set<String> saveFilesetToLocalDisk(File kmdataLocalDir) throws IOException, KeyStoreException {
        validate();
        if (!kmdataLocalDir.isDirectory())
            throw new IOException("Not a directory: " + kmdataLocalDir);
        if (!kmdataLocalDir.canWrite())
            throw new IOException("Unable to write to directory: " + kmdataLocalDir);

        /*
           File will initially be "written" but not "committed".

           A file is "written" when a file name "_new_"+filename has begun to be written out,
           and its unprefixed name filename is confirmed to be available for writing or overwriting.

           A file is "committed" when its "_new_"+filename file is renamed to just filename.

           If this operation fails before all files are written, all written files are deleted before we return.

           It is hoped that renaming all of the _new_ files at the very end is extremely unlikely to fail in
           the middle since we check what we can in advance.
         */

        // Files that have been written but not yet committed; contains the unprefixed fileset file name.
        final Set<String> written = new HashSet<String>();
        boolean rollback = true;

        try {
            // First write all files
            for (Map.Entry<String, byte[]> entry : fileset.entrySet()) {
                String name = entry.getKey();
                if (deletedFilenames != null && deletedFilenames.contains(name)) {
                    // Avoid writing out key blob that is known to have been deleted.  Commit phase will take care of actually deleting it.
                    continue;
                }

                byte[] bytes = entry.getValue();

                File newFile = new File(kmdataLocalDir, "_new_" + name);
                File curFile = new File(kmdataLocalDir, name);

                if (curFile.isDirectory())
                    throw new IOException("Unable to save file because a directory with the same name is in the way: " + curFile);
                if (curFile.exists() && !curFile.canWrite())
                    throw new IOException("Unable to overwrite file: " + curFile);

                written.add(name);
                FileUtils.save(newFile, true, new FileUtils.ByteSaver(bytes));
            }

            // Ready to commit
            rollback = false;
            return written;

        } finally {
            if (rollback)
                rollbackWrittenFiles(kmdataLocalDir, written);
        }
    }

    /**
     * Commit written files from an earlier successful call to {@link #saveFilesetToLocalDisk(java.io.File)}.
     *
     * @param kmdataLocalDir  the directory to save to, ie "/opt/nfast/kmdata/local".  Required. Must be writable by the current process.
     * @param written the set of written files.  Required.  If empty, this method will succeed, but will take no action.
     * @throws IOException if the commit fails.  In this unfortunate case some files may have already been overwritten.
     *                     Caller may choose to leave any not-yet-committed "_new_" files as they are, or may call {@link #rollbackWrittenFiles(java.io.File, java.util.Set)} to clean them up.
     */
    void commitWrittenFiles(File kmdataLocalDir, Set<String> written) throws IOException {
        // Then commit all files that were written
        Iterator<String> it = written.iterator();
        while (it.hasNext()) {
            String name = it.next();
            File newFile = new File(kmdataLocalDir, "_new_" + name);
            File curFile = new File(kmdataLocalDir, name);
            if (curFile.exists()) {
                if (!curFile.delete())
                    logger.warning("Failed to delete existing file (will attempt to rename over top): " + curFile);
            }
            if (newFile.renameTo(curFile)) {
                it.remove();
            } else {
                logger.warning("Failed to rename from " + newFile + " to " + curFile);
            }
        }

        if (deletedFilenames != null) for (String name : deletedFilenames) {
            File file = new File(kmdataLocalDir, name);
            if (file.exists()) {
                boolean result = file.delete();
                if (!result) {
                    logger.warning("Failed to delete file " + file);
                }
            }
        }

        if (!written.isEmpty())
            throw new IOException("Not all files in the fileset could be renamed into place (permission problem?)");

        if (deletedFilenames != null) {
            Iterator<String> fit = deletedFilenames.iterator();
            while (fit.hasNext() && deletedFilenames.size() > MAX_DELETED_FILENAMES_TO_TRACK) {
                fit.next();
                fit.remove();
            }
        }
    }

    /**
     * Roll back written files from an earlier successful call to {@link #saveFilesetToLocalDisk(java.io.File)}.  This method always succeeds.
     *
     * @param kmdataLocalDir  the directory to save to, ie "/opt/nfast/kmdata/local".  Required. Must be writable by the current process.
     * @param written the set of written files.  Required.
     */
    static void rollbackWrittenFiles(File kmdataLocalDir, Set<String> written) {
        // Attempt to roll back any written but not-yet-committed files.
        for (String name : written) {
            File newFile = new File(kmdataLocalDir, "_new_" + name);
            if (newFile.exists() && !newFile.delete())
                logger.warning("Failed to delete file on rollback: " + newFile);
        }
    }

    @NotNull
    private static final ClassFilter CLASS_FILTER = createClassFilter();

    /**
     * IMPORTANT: update the whitelist classes below, if class fields type changes
     *
     * @return a new instance of {@link ClassFilter} whitelisting:
     * NcipherKeyStoreData, String, LinkedHashSet, HashSet, HashMap and byte array.
     */
    @NotNull
    private static ClassFilter createClassFilter() {
        // allow only:
        // keystoreMetadata => String
        // fileset => HashMap, String and byte array ("[B")
        // deletedFilenames => LinkedHashSet and String
        return new ClassFilterBuilder().addClasses(
                false,
                NcipherKeyStoreData.class.getName(),
                String.class.getName(),
                LinkedHashSet.class.getName(),
                HashSet.class.getName(),
                HashMap.class.getName(),
                "[B"
        ).build();
    }
}
