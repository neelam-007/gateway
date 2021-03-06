package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.test.BugNumber;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Tests everything in the Backup interface except for any database related backup
 * These tests write to temporary folder in the system temp directory
 * Regardless of test outcome, no temp folders will be left over in the system temp directory. If there are its a
 * coding error
 *
 * Any test which sets a system property should unset it in a finally block, so that it doesn't cause other tests
 * to fail
 */
public class ExporterTest {

    private File tmpSecureSpanHome;
    private File tmpSsgHome;
    private File imageFileToCreate;

    private static final String OS_FILE_TO_COPY = "osfiletocopy";
    private String TEST_DEFAULT_VERSION = "5.1.0";

    @Before
    public void setUp() throws IOException {
        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();
        imageFileToCreate = new File(ImportExportUtilities.createTmpDirectory(), "image1.zip");
        SyspropUtil.setProperty( "com.l7tech.util.buildVersion", TEST_DEFAULT_VERSION );
        SyspropUtil.setProperty( "com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString( false ) );
    }

    @After
    public void tearDown(){
        if(tmpSecureSpanHome != null){
            if(tmpSecureSpanHome.exists()){
                FileUtils.deleteDir(tmpSecureSpanHome);
            }
        }

        FileUtils.deleteDir(imageFileToCreate.getParentFile());
        SyspropUtil.clearProperty( "com.l7tech.util.buildVersion" );
        SyspropUtil.clearProperty( "com.l7tech.gateway.config.backuprestore.checkversion" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.util.buildVersion",
            "com.l7tech.gateway.config.backuprestore.checkversion",
            "com.l7tech.gateway.config.backuprestore.setpostfiveo",
            Exporter.NO_UNIQUE_IMAGE_SYSTEM_PROP
        );
    }

    /**
     * Copy all test resources into our temporary directory
     * tearDown deletes the tmpSsgHome directory so do not need to worry about cleaning up files copied via this method
     * @throws IOException
     */
    public void createTestEnvironment() throws Exception {
        //Copy resources into this temp directory
        final URL nodeRes = this.getClass().getClassLoader().getResource("Gateway/node");
        final File nodeDirSrc = new File(nodeRes.getPath());
        final File nodeDirDest = new File(tmpSsgHome, "node");
        nodeDirDest.mkdir();
        ImportExportUtilities.copyDir(nodeDirSrc, nodeDirDest);

        final URL runtimeRes = this.getClass().getClassLoader().getResource("Gateway/runtime");
        final File runtimeSrc = new File(runtimeRes.getPath());
        final File runtimeDest = new File(tmpSsgHome, "runtime");

        ImportExportUtilities.copyDir(runtimeSrc, runtimeDest);

        final URL configRes = this.getClass().getClassLoader().getResource("Gateway/config");
        final File configSrc = new File(configRes.getPath());
        final File configDest = new File(tmpSsgHome, "config");

        ImportExportUtilities.copyDir(configSrc, configDest);
        
        final File osFile = createPretendOsFile();
        //config/backup/cfg/backup_manifest
        createBackupManifest(osFile);

        //Set up the esm
        final URL esmRes = this.getClass().getClassLoader().getResource("EnterpriseManager");
        final File esmDirSrc = new File(esmRes.getPath());
        final File esmDirDest = new File(tmpSecureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER);
        esmDirDest.mkdir();
        ImportExportUtilities.copyDir(esmDirSrc, esmDirDest);

    }

    @Test
    public void testConstructor() throws Exception {
        createTestEnvironment();
        final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
        Assert.assertNotNull(exporter);
    }

    @Test(expected=NullPointerException.class)
    public void testConstructorException() throws Backup.BackupException{
        new Exporter(null, System.out);
    }

    /**
     * Backup a test environment with no database or os files included. After the backup image is created, it is
     * tested for existence and is checked that it is a file and not a directory.
     */
    @Test
    public void testBackupImageCreated() throws Exception {
        createTestEnvironment();

        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final String imageZipFile = tmpDir + File.separator + "image.zip";
            programArgs.add(imageZipFile);
            final String[] args = programArgs.toArray(new String[]{});
            final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be success", Exporter.BackupResult.Status.SUCCESS, result.getStatus());
            final String uniqueImageZipFile = result.getBackUpImageName(); 

            //Check image.zip exists
            final File checkFile = new File(uniqueImageZipFile);
            Assert.assertTrue("image.zip should exist in '" + uniqueImageZipFile+"'", checkFile.exists());
            Assert.assertTrue("'" + uniqueImageZipFile+"' should not be a directory", !checkFile.isDirectory());
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Selectively backs a test environment with no database or os files included.
     * After the backup image is created, it is tested for existence and is checked that it is a file and not
     * a directory.
     * This is a selective backup and as the os files cannot be backed up, as the appliance for this test is not
     * installed, the result should be PARTIAL_SUCCESS
     */
    @Test
    public void testSelectiveBackupImageCreated() throws Exception {
        createTestEnvironment();

        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final String imageZipFile = tmpDir + File.separator + "image.zip";
            programArgs.add(imageZipFile);
            programArgs.add("-ca");
            programArgs.add("-os");
            
            final String[] args = programArgs.toArray(new String[]{});
            final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be partial success", result.getStatus(), Exporter.BackupResult.Status.PARTIAL_SUCCESS);
            final String uniqueImageZipFile = result.getBackUpImageName();

            //Check image.zip exists
            final File checkFile = new File(uniqueImageZipFile);
            Assert.assertTrue("image.zip should exist in '" + uniqueImageZipFile+"'", checkFile.exists());
            Assert.assertTrue("'" + uniqueImageZipFile+"' should not be a directory", !checkFile.isDirectory());
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Backup a test environment with no database files included. After the backup image is created, it is
     * validated to have backed up the correct files, based on the project's resources
     *
     */
    @Test
    public void testAndValidateBackupImage() throws Exception {
        createTestEnvironment();

        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final String imageZipFile = tmpDir + File.separator + "image.zip";
            programArgs.add(imageZipFile);
            final String[] args = programArgs.toArray(new String[]{});
            //this test validtes of files too
            //=> make the appliance direcotry
            final File applianceFolder = new File(tmpSecureSpanHome, ImportExportUtilities.APPLIANCE);
            applianceFolder.mkdir();
            final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be success", result.getStatus(), Exporter.BackupResult.Status.SUCCESS);

            String uniqueImageZipFile = result.getBackUpImageName();

            //Check image.zip exists
            final File checkFile = new File(uniqueImageZipFile);
            final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(checkFile));
            ZipEntry zipEntry;
            //fileToFolderMap is a map of the file name from the archive to the folder containing it
            final Map<String, String> fileToFolderMap = new HashMap<String, String>();
            while((zipEntry = zipInputStream.getNextEntry()) != null){
                final String entryName = zipEntry.getName();
                final String dirPart = ImportExportUtilities.getDirPart(entryName);
                final String prefix = (dirPart == null) ? "" : dirPart + "_";

                fileToFolderMap.put(prefix + ImportExportUtilities.getFilePart(entryName),
                        ImportExportUtilities.getDirPart(entryName));
            }

            //validate our hardcoded list of resources are in the correct places
            //First the config folder
            String key = ImportExportUtilities.ComponentType.CONFIG.getComponentName()
                    + "_" + ImportExportUtilities.NODE_PROPERTIES;
            Assert.assertTrue(ImportExportUtilities.NODE_PROPERTIES + " should exist in the config folder",
                    fileToFolderMap.get(key).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));

            key = ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "_" + ImportExportUtilities.SSGLOG_PROPERTIES;
            Assert.assertTrue(ImportExportUtilities.SSGLOG_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(key).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));

            key = ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "_" + ImportExportUtilities.SYSTEM_PROPERTIES;
            Assert.assertTrue(ImportExportUtilities.SYSTEM_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(key).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));

            key = ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "_" + ImportExportUtilities.OMP_DAT;
            Assert.assertTrue(ImportExportUtilities.OMP_DAT+" should exist in the config folder",
                    fileToFolderMap.get(key).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));

            //Custom assertions property files
            key = ImportExportUtilities.ComponentType.CA.getComponentName() + "_empty.properties";
            Assert.assertTrue("empty.properties should exist in the config folder",
                    fileToFolderMap.get(key).equals(ImportExportUtilities.ComponentType.CA.getComponentName()));

            //Custom assertions jar files
            key = ImportExportUtilities.ComponentType.CA.getComponentName() + "_empty.jar";
            Assert.assertTrue("empty.jar should exist in the config folder",
                    fileToFolderMap.get(key).equals(ImportExportUtilities.ComponentType.CA.getComponentName()));

            //Modular assertion aar files
            key = ImportExportUtilities.ComponentType.MA.getComponentName() + "_empty.aar";
            Assert.assertTrue("empty.aar should exist in the config folder",
                    fileToFolderMap.get(key).equals(ImportExportUtilities.ComponentType.MA.getComponentName()));

            //os files
            //os files retain their complete folder strucutre in the image zip. Their root folder will come after
            //the /os folder in the imagezip, which is why tmpSsgHome is used in conjunction with the OS folder name
            //in this assertion
            key = ImportExportUtilities.ComponentType.OS.getComponentName() + tmpSsgHome + "_" + OS_FILE_TO_COPY;
            Assert.assertTrue(OS_FILE_TO_COPY+" should exist in the os folder",
                    fileToFolderMap.get(key).equals(
                            ImportExportUtilities.ComponentType.OS.getComponentName()+tmpSsgHome));

            //version
            Assert.assertTrue(ImportExportUtilities.VERSION +" should exist in the zip root folder",
                    fileToFolderMap.get(ImportExportUtilities.VERSION) == null
                            && fileToFolderMap.containsKey(ImportExportUtilities.VERSION));

            //manifest.log
            Assert.assertTrue(ImportExportUtilities.MANIFEST_LOG+" should exist in the zip root folder",
                    fileToFolderMap.get(ImportExportUtilities.MANIFEST_LOG) == null
                            && fileToFolderMap.containsKey(ImportExportUtilities.MANIFEST_LOG));

        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Selectively backs up a test environment with no database files included.
     * After the backup image is created, it is validated to have backed up the correct components, based on
     * the program parameters supplied
     */
    @Test
    public void testAndValidateSelectiveBackupImage() throws Exception {
        createTestEnvironment();

        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final String imageZipFile = tmpDir + File.separator + "image.zip";
            programArgs.add(imageZipFile);
            programArgs.add("-ca");
            programArgs.add("-os");
            
            final String[] args = programArgs.toArray(new String[]{});
            //this test validtes of files too
            //=> make the appliance direcotry
            final File applianceFolder = new File(tmpSecureSpanHome, ImportExportUtilities.APPLIANCE);
            applianceFolder.mkdir();
            
            final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be success", result.getStatus(), Exporter.BackupResult.Status.SUCCESS);

            String uniqueImageZipFile = result.getBackUpImageName();

            //Check image.zip exists
            final File checkFile = new File(uniqueImageZipFile);
            final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(checkFile));
            ZipEntry zipEntry;
            //fileToFolderMap is a map of the file name from the archive to the folder containing it
            final Map<String, String> fileToFolderMap = new HashMap<String, String>();
            final Map<String, String> componentExistenceMap = new HashMap<String, String>();
            while((zipEntry = zipInputStream.getNextEntry()) != null){
                final String entryName = zipEntry.getName();
                fileToFolderMap.put(ImportExportUtilities.getFilePart(entryName), ImportExportUtilities.getDirPart(entryName));
                String component = ImportExportUtilities.getDirPart(entryName);
                if(component != null) componentExistenceMap.put(ImportExportUtilities.getDirPart(entryName), null);
            }

            //Confirm all that we EXPECT are found
            //Custom assertions property files
            Assert.assertTrue("empty.properties should exist in the config folder",
                    fileToFolderMap.get("empty.properties").equals(ImportExportUtilities.ComponentType.CA.getComponentName()));
            //Custom assertions jar files
            Assert.assertTrue("empty.jar should exist in the config folder",
                    fileToFolderMap.get("empty.jar").equals(ImportExportUtilities.ComponentType.CA.getComponentName()));

            Assert.assertTrue(OS_FILE_TO_COPY+" should exist in the os folder",
                    fileToFolderMap.get(OS_FILE_TO_COPY).equals(
                            ImportExportUtilities.ComponentType.OS.getComponentName()+tmpSsgHome));

            //version
            Assert.assertTrue(ImportExportUtilities.VERSION +" should exist in the zip root folder",
                    fileToFolderMap.get(ImportExportUtilities.VERSION) == null
                            && fileToFolderMap.containsKey(ImportExportUtilities.VERSION));

            //manifest.log
            Assert.assertTrue(ImportExportUtilities.MANIFEST_LOG+" should exist in the zip root folder",
                    fileToFolderMap.get(ImportExportUtilities.MANIFEST_LOG) == null
                            && fileToFolderMap.containsKey(ImportExportUtilities.MANIFEST_LOG));

            //Now make sure that nothing else was found that was not requrested

            //confirm no config
            Assert.assertFalse("config should not have been backedup",
                    componentExistenceMap.containsKey(ImportExportUtilities.ComponentType.CONFIG));

            //confirm no db
            Assert.assertFalse("maindb should not have been backedup",
                    componentExistenceMap.containsKey(ImportExportUtilities.ComponentType.MAINDB));

        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Tests that the Exporter correctly validates its command line arguments
     * -image is missing
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_NoExport() throws Exception {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly validates its command line arguments
     * -noexist parameter is supplied, which doesn't exist
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_InvalidArg() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException, Backup.BackupException {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-noexist");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly validates its command line arguments
     * export is missing
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_ImageNeedsValue() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException, Backup.BackupException {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly observes the
     * com.l7tech.gateway.config.backuprestore.nomodifyimagename.nouniqueimagename system property.
     */
    @Test
    public void testInvalidExporterArgs_NoUniqueImageName() throws Exception {
        createTestEnvironment();
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final String imageZipFile = tmpDir + File.separator + "image.zip";
            programArgs.add(imageZipFile);
            final String[] args = programArgs.toArray(new String[]{});
            final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
            SyspropUtil.setProperty( Exporter.NO_UNIQUE_IMAGE_SYSTEM_PROP, "true" );
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be success", result.getStatus(), Exporter.BackupResult.Status.SUCCESS);

            final String uniqueImageZipFile = result.getBackUpImageName();

            Assert.assertEquals("Returned file name should equal the supplie image name", imageZipFile, uniqueImageZipFile);
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
            SyspropUtil.clearProperty( Exporter.NO_UNIQUE_IMAGE_SYSTEM_PROP );
        }
    }

    /**
     * Tests that the file "version" is created in the tmp folder used for generating the backup image from
     * @throws IOException
     */
    @Test
    public void testVersionBackup() throws Exception {
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest", true,
                true, System.out);
        try{
            backup.backUpVersion();

            final File backupFolder = backup.getBackupFolder();
            //Check version file exists
            final File checkFile = new File(backupFolder, ImportExportUtilities.VERSION);
            Assert.assertTrue(ImportExportUtilities.VERSION + " file should exist in '"
                    + backupFolder.getAbsolutePath()+"'", checkFile.exists());
            Assert.assertTrue("'" + checkFile.getName()+"' should not be empty", checkFile.length() > 0);
        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Tests that the folder "config" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct configuration files
     * @throws IOException
     */
    @Test
    public void testConfigBackup() throws Exception {
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome,null, "notusedinthistest", true,
                true, System.out);

        try{
            backup.backUpComponentConfig();

            final File backupFolder = backup.getBackupFolder();
            //Check config dir exists
            final File configDir = new File(backupFolder, ImportExportUtilities.ComponentType.CONFIG.getComponentName());
            Assert.assertTrue(ImportExportUtilities.ComponentType.CONFIG.getComponentName() +
                    " directory should exist in '" + backupFolder.getAbsolutePath()+"'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ComponentType.CONFIG.getComponentName() +
                    " directory should be a directory '" + backupFolder.getAbsolutePath()+"'", configDir.isDirectory());

            //Test for the individual files
            final File nodeProp = new File(backupFolder.getAbsolutePath() + File.separator +
                    ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    ImportExportUtilities.NODE_PROPERTIES);
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" +
                    backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "'", nodeProp.exists());

            final File ssgLog = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    ImportExportUtilities.SSGLOG_PROPERTIES);

            Assert.assertTrue(ssgLog.getName() + " should exist in '" + backupFolder.getAbsolutePath()
                    + File.separator + ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "'",
                    ssgLog.exists());

            final File systemProp = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    ImportExportUtilities.SYSTEM_PROPERTIES);

            Assert.assertTrue(systemProp.getName() + " should exist in '" + backupFolder.getAbsolutePath()
                    + File.separator + ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "'",
                    systemProp.exists());

            final File ompDat = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    ImportExportUtilities.OMP_DAT);

            Assert.assertTrue(ompDat.getName() + " should exist in '" + backupFolder.getAbsolutePath()
                    + File.separator + ImportExportUtilities.ComponentType.CONFIG.getComponentName() + "'",
                    ompDat.exists());

        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Tests that the folder "os" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct os files.
     * The OS back up is driven by the file ssg_home/config/backup/cfg/backup_manifest. This file is created along with
     * a pretend os file in the tmpSsgHome folder and is used to drive the os backup
     * @throws IOException
     */
    @Test
    public void testOsBackup() throws Exception {
        //Make a file which will constitute the os backup
        final File osFile = createPretendOsFile();
        //config/backup/cfg/backup_manifest
        createBackupManifest(osFile);

        FileOutputStream fos = null;
        //this test validates os files too
        //=> make the appliance direcotry
        final File applianceFolder = new File(tmpSecureSpanHome, ImportExportUtilities.APPLIANCE);
        applianceFolder.mkdir();
        
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest",
                true, true, System.out);

        try{

            backup.backUpComponentOS();

            final File backupFolder = backup.getBackupFolder();
            //confirm pretend os file was copied
            final File checkOsFile = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.OS.getComponentName() +
                    File.separator + osFile.getAbsolutePath());
            Assert.assertTrue(checkOsFile.getName() + " should exist in '" +backupFolder.getAbsolutePath()
                    + File.separator + ImportExportUtilities.ComponentType.OS.getComponentName() + "'",
                    checkOsFile.exists());
        } finally{
            ResourceUtils.closeQuietly(fos);
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Tests the custom assertion backup
     * Tests that the folder "ca" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct configuration and jar files, based on this project's resources
     * @throws IOException
     */
    @Test
    public void testCaBackup() throws Exception {
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest",
                true, true, System.out);

        try{
            backup.backUpComponentCA();

            final File backupFolder = backup.getBackupFolder();
            //Check config dir exists
            final File configDir = new File(backupFolder.getAbsolutePath(),
                    ImportExportUtilities.ComponentType.CA.getComponentName());

            Assert.assertTrue(ImportExportUtilities.ComponentType.CA.getComponentName() +
                    " directory should exist in '" + backupFolder.getAbsolutePath() + "'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ComponentType.CA.getComponentName() +
                    " directory should be a directory '" + backupFolder.getAbsolutePath() + "'",
                    configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File nodeProp = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName(), "empty.properties");
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName() + "'", nodeProp.exists());

            final File ssgLog = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName(), "empty.jar");

            Assert.assertTrue(ssgLog.getName() + " should exist in '" + backupFolder.getAbsolutePath()
                    + File.separator + ImportExportUtilities.ComponentType.CA.getComponentName() + "'",
                    ssgLog.exists());
        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    @Test
    public void testEsmbackup() throws Exception{
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null,
                imageFileToCreate.getAbsolutePath(), true, true, System.out);

        try{
            backup.backUpComponentESM();
            final File backupFolder = backup.getBackupFolder();

            final File esmFolder = new File(backupFolder, ImportExportUtilities.ComponentType.ESM.getComponentName());
            Assert.assertTrue("esm folder not found", esmFolder.exists());
            Assert.assertTrue("esm folder incorrectly created", esmFolder.isDirectory());

            final File etcFolder = new File(esmFolder, "etc");
            Assert.assertTrue("etc folder not found", etcFolder.exists());
            Assert.assertTrue("etc folder incorrectly created", etcFolder.isDirectory());

            final File ompDat = new File(etcFolder, "omp.dat");
            Assert.assertTrue("omp.dat file not found", ompDat.exists());
            Assert.assertTrue("omp.dat file incorrectly created", ompDat.isFile());

            final File varFolder = new File(esmFolder, "var");
            Assert.assertTrue("var folder not found", varFolder.exists());
            Assert.assertTrue("var folder incorrectly created", varFolder.isDirectory());

            //jump ahead to a file low down
            final File dbLck = new File(varFolder, "/db/emsdb/db.lck");
            Assert.assertTrue("db.lck file not found", dbLck.exists());
            Assert.assertTrue("db.lck file incorrectly created", dbLck.isFile());

        }finally{
            backup.deleteTemporaryDirectory();
        }
    }
    
    /**
     * Test the modular assertion backup
     * Tests that the folder "ma" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct aar files, based on this project's resources
     * @throws IOException
     */
    @Test
    public void testMaBackup() throws Exception {
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest",
                true, true, System.out);

        try{
            backup.backUpComponentMA();

            final File backupFolder = backup.getBackupFolder();
            //Check config dir exists
            final File configDir = new File(backupFolder.getAbsolutePath(),
                    ImportExportUtilities.ComponentType.MA.getComponentName());

            Assert.assertTrue(ImportExportUtilities.ComponentType.MA.getComponentName() +
                    " directory should exist in '" + backupFolder.getAbsolutePath() + "'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ComponentType.MA.getComponentName() +
                    " directory should be a directory '" + backupFolder.getAbsolutePath() + "'", configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File echoAar = new File(backupFolder.getAbsolutePath() + File.separator +
                    ImportExportUtilities.ComponentType.MA.getComponentName(), "EchoRoutingAssertion-5.0.aar");
            Assert.assertTrue(echoAar.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.MA.getComponentName() + "'", echoAar.exists());

            final File emptyAar = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.MA.getComponentName(), "empty.aar");

            Assert.assertTrue(emptyAar.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.MA.getComponentName() + "'", emptyAar.exists());
        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Test the lib/ext backup
     * Tests that the folder "ext" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct jar files, based on this project's resources
     *
     * @throws IOException
     */
    @BugNumber(9070)
    @Test
    public void testExtBackup() throws Exception {
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest",
                true, true, System.out);

        try{
            backup.backUpComponentEXT();

            final File backupFolder = backup.getBackupFolder();
            //Check config dir exists
            final File configDir = new File(backupFolder.getAbsolutePath(),
                    ImportExportUtilities.ComponentType.EXT.getComponentName());

            Assert.assertTrue(ImportExportUtilities.ComponentType.EXT.getComponentName() +
                    " directory should exist in '" + backupFolder.getAbsolutePath() + "'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ComponentType.EXT.getComponentName() +
                    " directory should be a directory '" + backupFolder.getAbsolutePath() + "'", configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File jarFile = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.EXT.getComponentName(), "empty.jar");

            Assert.assertTrue(jarFile.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.EXT.getComponentName() + "'", jarFile.exists());
        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Test the creation of a zip file
     */
    @Test
    public void testCreateImageFile() throws Exception {
        createTestEnvironment();
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null,
                imageFileToCreate.getAbsolutePath(), true, true, System.out);

        try{
            backup.backUpComponentCA();//just back up something

            //zip the tmpDir
            backup.createBackupImage();

            //confirm zip file exists, is a file and is not empty
            Assert.assertTrue(imageFileToCreate.getName() + " should exist in '" + imageFileToCreate.getParent()+"'", imageFileToCreate.exists());
            Assert.assertTrue(imageFileToCreate.getName() + " should not be a directory", !imageFileToCreate.isDirectory());
            Assert.assertTrue(imageFileToCreate.getName() + " should not be empty", imageFileToCreate.length() > 0);
        }finally{
            backup.deleteTemporaryDirectory();
        }
    }

    /**
     * Test that getFtpConfig() correctly constructs a FtpClientConfig object with the parameters supplied to Exporter
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    @Test
    public void testGetFtpConfig() throws Exception {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(CommonCommandLineOptions.FTP_HOST.getName(), "donal.l7tech.com:21");
        args.put(CommonCommandLineOptions.FTP_USER.getName(), "root");
        args.put(CommonCommandLineOptions.FTP_PASS.getName(), "7layer");
        args.put(Exporter.IMAGE_PATH.getName(), "image.zip");

        final FtpClientConfig ftpConfig = ImportExportUtilities.getFtpConfig(args);
        Assert.assertNotNull(ftpConfig);

        Assert.assertEquals("Host name should equal donal.l7tech.com", "donal.l7tech.com", ftpConfig.getHost());
        Assert.assertEquals("Port number should be 21", 21, ftpConfig.getPort());
        Assert.assertEquals("User name should be root", "root", ftpConfig.getUser());
        Assert.assertEquals("User pass should be 7layer", "7layer", ftpConfig.getPass());

        FtpClientConfig cloned = (FtpClientConfig)ftpConfig.clone();
        System.out.println(cloned);
    }

    /**
     * Test that if no port is supplied with the host string for -ftp_host, that no exception is thrown
     */
    public void testValidateFtpParametersNoPort() throws BackupRestoreLauncher.InvalidProgramArgumentException {

        final Map<String, String> args = new HashMap<String, String>();
        args.put(CommonCommandLineOptions.FTP_HOST.getName(), "donal.l7tech.com");
        args.put(CommonCommandLineOptions.FTP_USER.getName(), "root");
        args.put(CommonCommandLineOptions.FTP_PASS.getName(), "7layer");
        args.put(Exporter.IMAGE_PATH.getName(), "image.zip");

        ImportExportUtilities.checkAndValidateFtpParams(args);
    }

    /**
     * Create a file called OS_FILE_TO_COPY in tmpSsgHome which can be used for testing OS backup
     * @return File called OS_FILE_TO_COPY
     * @throws IOException
     */
    private File createPretendOsFile() throws IOException {
        final File osFile = new File(tmpSsgHome, OS_FILE_TO_COPY);
        osFile.createNewFile();
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(osFile);
            fos.write("Important OS file with settings to backup".getBytes());
        } finally{
            ResourceUtils.closeQuietly(fos);
        }
        return osFile;
    }

    /**
     * Creates the backup_manifest in tmpSsgHome/config/backup/cfg and writes the absolute path of osFile to it
     * @param osFile the absolute path of this file will be written to backup_manifest
     * @throws IOException
     */
    private void createBackupManifest(File osFile) throws IOException {
        final File backupManifestFolder = new File(tmpSsgHome, "config/backup/cfg");
        FileUtils.ensurePath(backupManifestFolder);
        final File backupManifest = new File(backupManifestFolder, "backup_manifest");
        backupManifest.createNewFile();

        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(backupManifest);
            final String toWrite = osFile.getAbsolutePath() + "\n";
            fos.write(toWrite.getBytes());
        } finally{
            ResourceUtils.closeQuietly(fos);
        }
    }

    /**
     * Tests the logic of how the image name supplied by the user is made unique
     * 
     * Tests the following:
     * - In all situations the file name is made unique 
     * - In a 5.0 environment if the file name has no path part, then none is added
     * - When ftp is being used, no path part is added when the image name does not contain any
     * - When ftp is not being used, and the environment is post 5.0, then the default images folder is added to
     * the image name, if it contains no path part
     *
     * @throws Exception
     */
    @Test
    public void testGetUniqueImageFileName() throws Exception{
        final String imageNameNoPath = "image1.zip";

        //pre five o the image name should not be changed
        final Exporter fiveOExporter = new Exporter(tmpSecureSpanHome, System.out);

        final String fiveOUniqueImage = fiveOExporter.getUniqueImageFileName(imageNameNoPath, false, "atimestamp");
        //confirm no path info added
        Assert.assertNull("No path info should have been added", ImportExportUtilities.getDirPart(fiveOUniqueImage));
        //confirm that the image name has been made unique
        Assert.assertFalse("image name should have been made unique", fiveOUniqueImage.equals(imageNameNoPath));
        //confirm that the image name ends with the original
        Assert.assertTrue("File name ending should not have been changed",
                fiveOUniqueImage.endsWith(imageNameNoPath));

        SyspropUtil.setProperty( "com.l7tech.gateway.config.backuprestore.setpostfiveo", Boolean.toString( true ) );
        final Exporter exporter = new Exporter(tmpSecureSpanHome, System.out);
        final String uniqueImageNameNoPath = exporter.getUniqueImageFileName(imageNameNoPath, false, "atimestamp");
        SyspropUtil.clearProperty( "com.l7tech.gateway.config.backuprestore.setpostfiveo" );

        //Validate the directory part was created correctly
        Assert.assertEquals("Incorret file part",
                new File(tmpSsgHome, ImportExportUtilities.POST_FIVE_O_DEFAULT_BACKUP_FOLDER).getAbsolutePath(),
                ImportExportUtilities.getDirPart(uniqueImageNameNoPath));

        Assert.assertTrue("File name ending should not have been changed",
                uniqueImageNameNoPath.endsWith(imageNameNoPath));

        //confirm the image name has timestamp added
        Assert.assertFalse("image name should have been made unique", uniqueImageNameNoPath.equals(imageNameNoPath));
        //confirm the image name has timestamp added, when we remove the dir part
        Assert.assertFalse("image name should have been made unique",
                ImportExportUtilities.getFilePart(uniqueImageNameNoPath).equals(imageNameNoPath));        


        //validate no file part is added when ftp is being used
        final String ftpUniqueImageName = exporter.getUniqueImageFileName(imageNameNoPath, true, "atimestamp");
        Assert.assertNull("No dir part should have been created", ImportExportUtilities.getDirPart(ftpUniqueImageName));

        //validate that the file name generated ends with the original file name after the unique timestampe was added
        Assert.assertTrue("File name ending should not have been changed",
                uniqueImageNameNoPath.endsWith(ftpUniqueImageName));

        Assert.assertFalse("image name should have been made unique", ftpUniqueImageName.equals(imageNameNoPath));

        //validate ftp file name unique generation ok, when the image name has path information
        final String imageNameWithPath = "/home/darmstrong/image.zip";
        final String uniqueImageNameWithPath = exporter.getUniqueImageFileName(imageNameWithPath, true, "atimestamp");

        Assert.assertEquals("path part of unique ftp image name should have have been changed",
                ImportExportUtilities.getDirPart(imageNameWithPath), 
                ImportExportUtilities.getDirPart(uniqueImageNameWithPath));

        //validate that the file part of the ftp image has been made unique
        Assert.assertFalse("image name should have been made unique",
                ImportExportUtilities.getFilePart(uniqueImageNameWithPath).equals(imageNameWithPath));

        //confirm that the unique file name ends with the orignal file name
        Assert.assertTrue("File name ending should not have been changed",
                uniqueImageNameWithPath.endsWith(ImportExportUtilities.getFilePart(imageNameWithPath)));
    }

    @BugNumber(10111)
    @Test
    public void testPostVersion5Release() throws Exception{
        //pre five o the image name should not be changed
        try {
            SyspropUtil.setProperty( "com.l7tech.util.buildVersion", "6.0" );
            SyspropUtil.setProperty( "com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString( true ) );

            final URL resource = this.getClass().getClassLoader().getResource("Gateway/runtime");
            final File nodeDirSrc = new File(resource.getPath());
            final File nodeDirDest = new File(tmpSsgHome, "runtime");
            nodeDirDest.mkdir();
            ImportExportUtilities.copyDir(nodeDirSrc, nodeDirDest);

            final Exporter fiveOExporter = new Exporter(tmpSecureSpanHome, System.out);
            final Field declaredField = fiveOExporter.getClass().getDeclaredField("isPostFiveO");
            declaredField.setAccessible(true);
            final boolean isPostFiveO = declaredField.getBoolean(fiveOExporter);
            Assert.assertTrue("6.0 Gateway should be registered as post 5.0.", isPostFiveO);


        } finally {
            SyspropUtil.setProperty( "com.l7tech.util.buildVersion", TEST_DEFAULT_VERSION );//test default value
            SyspropUtil.setProperty( "com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString( false ) );
        }

    }

}
