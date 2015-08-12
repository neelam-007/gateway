package com.l7tech.server.policy.module;

import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Rule;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

/**
 * Base class for {@link com.l7tech.server.policy.module.CustomAssertionsScannerTest CustomAssertionsScannerTest}
 * and {@link com.l7tech.server.policy.module.ModularAssertionsScannerTest ModularAssertionsScannerTest}.<br/>
 * Holds shared and utility methods implementation.
 */
@Ignore
public abstract class ModulesScannerTestBase {

    /**
     * Declare conditional ignore rule.<br/>
     * Combine with {@link com.l7tech.test.conditional.ConditionalIgnore ConditionalIgnore} annotation.
     * Add the annotation next to each unit-test method that should be conditionally ignored.
     */
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    // variable holding all temporary folder which are created by the unit-test,
    // so that they can be gracefully deleted @AfterClass
    protected static final Map<String, File> tmpFiles = new HashMap<>();

    // this variable defines the maximum amount of temporary dates to be stored in the array.
    // Increase when needed.
    private static final int DATES_TO_PRESERVE = 100;
    // this specifies the minimum difference between two dates.
    private static final long MS_BETWEEN_DATES = 1200L;
    // array of dates for temporary modules folder and files.
    protected static Long[] tmpFilesDates;
    // variable holding the current date position in the array.
    protected static int currentDatePosition;

    /**
     * On systems with one second resolution of File.lastModified method (like linux, OSX etc.),
     * quick folder modifications will not be detected, which is common in most unit-tests.<br/>
     * This will help overcoming the issue, by pre-allocating certain amount of dates ({@link #DATES_TO_PRESERVE} per unit-test),
     * so that we can manually set the modules folder and files modification date as needed.
     * <p/>
     * This method should be called per unit-test i.e. @Before.
     * <p/>
     * {@link #copy_all_files(java.io.File, int, com.l7tech.server.policy.module.ModulesScannerTestBase.CopyData[]) copy_all_files}
     * and {@link #delete_all_files(java.io.File, int, String[]) delete_all_files} will use this mechanism to modify
     * modules folder and file date-stamp.
     *
     */
    protected static void setupDates() {
        // get current time
        final long timeNow = (new Date()).getTime();

        // create the dates collection
        //noinspection serial
        SortedSet<Long> dates = new TreeSet<Long>() {{
            for (int i = 1; i <= DATES_TO_PRESERVE; ++i) {
                add(timeNow - i * MS_BETWEEN_DATES);
            }
        }};

        tmpFilesDates = dates.toArray(new Long[dates.size()]);
        Assert.assertEquals(DATES_TO_PRESERVE, tmpFilesDates.length);

        // reset current position to zero
        currentDatePosition = 0;
    }

    /**
     * @return the next date from {@link #tmpFilesDates}
     */
    private static long getNextDate() {
        final int datesSize = tmpFilesDates.length;
        return (datesSize > 0) ? tmpFilesDates[(currentDatePosition++)%datesSize] : 0;
    }

    /**
     * Extracts the folder from specified file.<br/>
     * The method will try to load the file through the <code>ClassLoader</code>,
     * will fail if the file is not in the class path or is not a resource (images, audio, text, etc)
     *
     * @param fileInDir    a class-relative path to the file, whose folder should be extracted from.
     * @return <code>String</code> containing the
     */
    protected static String extractFolder(@NotNull final String fileInDir) {
        // Get the resource directory of this unit test. This is where Custom Assertions will be loaded from during startup.
        final URL fileDirUrl = ModulesScannerTestBase.class.getClassLoader().getResource(fileInDir);
        Assert.assertNotNull(fileDirUrl);
        String fileDirPath = fileDirUrl.getPath();
        fileDirPath = fileDirPath.substring(0, fileDirPath.lastIndexOf("/"));
        Assert.assertNotNull(fileDirPath);
        Assert.assertTrue(new File(fileDirPath).exists());
        return fileDirPath;
    }

    /**
     * Creates a temporary folder, named with a specified <tt>prefix</tt>.<br/>
     * The method will append a hex representation of <code>currentTimeMillis</code>.<br/>
     * If the folder exists, then it will be deleted.<br/>
     * If the deletion failed then this method will be called recursively with modified prefix, appending a hex 64-bit random number.<br/>
     * All temporary folders are marked for deletion on exit, in addition they are kept in a <tt>tmpFiles</tt> map and
     * will be deleted once this class finish with execution (i.e. @AfterClass)
     * <p/>
     * <b>Note on Windows:</b> Even though the method keeps track of created folders and deletes them @AfterClass,
     * due to the mandatory file locking on Windows platform, folders will probably need to be manually deleted.
     *
     * @param prefix    the temporary folder prefix.
     * @return a <code>File</code> object containing the newly created temporary folder.
     */
    protected static File getTempFolder(final String prefix) {
        //noinspection SpellCheckingInspection
        final String tmpDirPath = SyspropUtil.getProperty("java.io.tmpdir");
        final File tmpDir = new File(tmpDirPath);
        final File moduleTmpDir = new File(
                tmpDir,
                prefix + "-" + Long.toHexString(System.currentTimeMillis())
        );

        if (moduleTmpDir.exists()) {
            if (!FileUtils.deleteDir(moduleTmpDir)) {
                return getTempFolder(prefix + "-" + Long.toHexString(Double.doubleToLongBits(Math.random())));
            }
        }
        //noinspection ResultOfMethodCallIgnored
        moduleTmpDir.mkdir();
        moduleTmpDir.deleteOnExit();

        // keep track of the new folder, so that it will be deleted @AfterClass.
        tmpFiles.put(moduleTmpDir.getAbsolutePath(), moduleTmpDir);

        return moduleTmpDir;
    }

    /**
     * On Windows platform jar files are locked by JVM, therefore they cannot be cleaned up on exit.<br/>
     * This method is called on start, so that all previously created temporary folders will be deleted.<br/>
     * This means that at a worst case scenario we will only end up with files from a single run.
     */
    protected static void cleanUpTemporaryFilesFromPreviousRuns(String ... tmpFolderPrefixes) {
        //noinspection SpellCheckingInspection
        final String tmpDirPath = SyspropUtil.getProperty("java.io.tmpdir");
        final File tmpFolder = new File(tmpDirPath);

        for (final String tampFolderPrefix : tmpFolderPrefixes) {
            File[] files = tmpFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name != null && !name.isEmpty() && name.matches(tampFolderPrefix + ".*");
                }
            });
            for (final File file : files) {
                if (file.isDirectory()) {
                    if (!FileUtils.deleteDir(file)) {
                        System.err.println( "Can't remove " + file.getAbsolutePath() );
                    }
                }
            }
        }
    }

    /**
     * Utility class defining file-template used in
     * {@link #copy_all_files(java.io.File, int, com.l7tech.server.policy.module.ModulesScannerTestBase.CopyData[]) copy_all_files}
     */
    protected static class CopyData {
        /**
         * Specifies the source folder
         */
        final String sourceFolder;
        /**
         * Specifies the source template file name, optionally having # sign
         */
        final String sourceFileNameTemplate;
        /**
         * Specifies the destination template file name, optionally having # sign
         */
        final String destinationFileNameTemplate;

        public CopyData(final String sourceFolder, final String sourceFileNameTemplate) {
            this.sourceFolder = sourceFolder;
            this.sourceFileNameTemplate = sourceFileNameTemplate;
            this.destinationFileNameTemplate = sourceFileNameTemplate;
        }

        public CopyData(final File sourceFolderPath, final String sourceFileNameTemplate) {
            this(sourceFolderPath.getAbsolutePath(), sourceFileNameTemplate);
        }

        public CopyData(final String sourceFolder, final String sourceFileNameTemplate, final String destinationFileNameTemplate) {
            this.sourceFolder = sourceFolder;
            this.sourceFileNameTemplate = sourceFileNameTemplate;
            this.destinationFileNameTemplate = destinationFileNameTemplate;
        }

        public CopyData(final File sourceFolderPath, final String sourceFileNameTemplate, final String destinationFileNameTemplate) {
            this(sourceFolderPath.getAbsolutePath(), sourceFileNameTemplate, destinationFileNameTemplate);
        }
    }

    /**
     * Utility function for copying files, or file-templates defined in <tt>copyDataValues</tt>, into destination folder.
     */
    protected static void copy_all_files(
            final File destinationFolder,
            final int numOfFilesToCopy,
            final CopyData[] copyDataValues
    ) throws Exception {
        Assert.assertTrue("Destination folder exists", destinationFolder.exists());

        for (final CopyData copyData : copyDataValues) {
            Assert.assertNotNull(destinationFolder.listFiles());
            //noinspection ConstantConditions
            final int initialNumberOfFiles = destinationFolder.listFiles().length;
            for (int i = 1; i <= numOfFilesToCopy; ++i) {
                final File inFile = new File(copyData.sourceFolder + File.separator + copyData.sourceFileNameTemplate.replace("#", String.valueOf(i)));
                Assert.assertTrue(inFile.exists());
                final File outFile = new File(destinationFolder.getAbsoluteFile() + File.separator + copyData.destinationFileNameTemplate.replace("#", String.valueOf(i)));
                Assert.assertFalse(outFile.exists());

                FileUtils.copyFile(inFile, outFile);
                // modify output file folder timestamp
                Assert.assertTrue("outFile timestamp is successfully set", outFile.setLastModified(getNextDate()));
            }
            Assert.assertNotNull(destinationFolder.listFiles());
            //noinspection ConstantConditions
            Assert.assertEquals("All modules are successfully copied", initialNumberOfFiles + numOfFilesToCopy, destinationFolder.listFiles().length);
        }

        // modify destination folder timestamp
        Assert.assertTrue("Destination folder timestamp is successfully set", destinationFolder.setLastModified(getNextDate()));
    }

    /**
     * Utility function for deleting files, or file-templates defined in <tt>fileTemplates</tt>, from source folder.
     */
    protected static void delete_all_files(
            final File sourceFolder,
            final int numOfFilesToDelete,
            final String[] fileTemplates
    ) throws Exception {
        Assert.assertTrue("Source folder exists", sourceFolder.exists());

        for (final String fileTemplate : fileTemplates) {
            Assert.assertNotNull(sourceFolder.listFiles());
            //noinspection ConstantConditions
            final int initialNumberOfFiles = sourceFolder.listFiles().length;
            for (int i = 1; i <= numOfFilesToDelete; ++i) {
                final File fileToDelete = new File(sourceFolder.getAbsoluteFile() + File.separator + fileTemplate.replace("#", String.valueOf(i)));
                Assert.assertTrue(fileToDelete.exists());

                FileUtils.delete(fileToDelete);
                Assert.assertFalse(fileToDelete.exists());
            }
            Assert.assertNotNull(sourceFolder.listFiles());
            //noinspection ConstantConditions
            Assert.assertEquals("All modules are successfully deleted", initialNumberOfFiles - numOfFilesToDelete, sourceFolder.listFiles().length);
        }

        // modify destination folder timestamp
        Assert.assertTrue("Source folder timestamp is successfully set", sourceFolder.setLastModified(getNextDate()));
    }
}
