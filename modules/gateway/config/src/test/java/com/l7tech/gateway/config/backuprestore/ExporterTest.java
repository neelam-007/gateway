/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: May 25, 2009
 * Time: 10:16:11 AM
 */
package com.l7tech.gateway.config.backuprestore;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.net.URL;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;

/**
 * Tests eveything in the Backup interface except for any database related backup
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

    @Before
    public void setUp() throws IOException {
        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();
        imageFileToCreate = new File(ImportExportUtilities.createTmpDirectory(), "image1.zip");
        System.setProperty("com.l7tech.util.buildVersion", "5.1.0");
        System.setProperty("com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString(false));
    }

    @After
    public void tearDown(){
        if(tmpSecureSpanHome != null){
            if(tmpSecureSpanHome.exists()){
                FileUtils.deleteDir(tmpSecureSpanHome);
            }
        }

        FileUtils.deleteDir(imageFileToCreate.getParentFile());
        System.clearProperty("com.l7tech.util.buildVersion");
        System.clearProperty("com.l7tech.gateway.config.backuprestore.checkversion");
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
            Assert.assertEquals("Status should be success", result.getStatus(), Exporter.BackupResult.Status.SUCCESS);
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
                fileToFolderMap.put(ImportExportUtilities.getFilePart(entryName), ImportExportUtilities.getDirPart(entryName));
            }

            //validate our hardcoded list of resources are in the correct places
            //First the config folder
            Assert.assertTrue(ImportExportUtilities.NODE_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.NODE_PROPERTIES).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));
            Assert.assertTrue(ImportExportUtilities.SSGLOG_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.SSGLOG_PROPERTIES).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));
            Assert.assertTrue(ImportExportUtilities.SYSTEM_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.SYSTEM_PROPERTIES).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));
            Assert.assertTrue(ImportExportUtilities.OMP_DAT+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.OMP_DAT).equals(
                            ImportExportUtilities.ComponentType.CONFIG.getComponentName()));

            //Custom assertions property files
            Assert.assertTrue("empty.properties should exist in the config folder",
                    fileToFolderMap.get("empty.properties").equals(ImportExportUtilities.ComponentType.CA.getComponentName()));
            //Custom assertions jar files
            Assert.assertTrue("empty.jar should exist in the config folder",
                    fileToFolderMap.get("empty.jar").equals(ImportExportUtilities.ComponentType.CA.getComponentName()));

            //Modular assertion aar files
            Assert.assertTrue("empty.aar should exist in the config folder",
                    fileToFolderMap.get("empty.aar").equals(ImportExportUtilities.ComponentType.MA.getComponentName()));

            //os files
            //os files retain their complete folder strucutre in the image zip. Their root folder will come after
            //the /os folder in the imagezip, which is why tmpSsgHome is used in conjunction with the OS folder name
            //in this assertion
            Assert.assertTrue(OS_FILE_TO_COPY+" should exist in the os folder",
                    fileToFolderMap.get(OS_FILE_TO_COPY).equals(
                            ImportExportUtilities.ComponentType.OS.getComponentName()+tmpSsgHome));

            //confirm pretend os file was copied
            Assert.assertTrue("empty.aar should exist in the config folder",
                    fileToFolderMap.get("empty.aar").equals(ImportExportUtilities.ComponentType.MA.getComponentName()));

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
            System.setProperty(Exporter.NO_UNIQUE_IMAGE_SYSTEM_PROP, "true");
            final Exporter.BackupResult result = exporter.createBackupImage(args);
            Assert.assertEquals("Status should be success", result.getStatus(), Exporter.BackupResult.Status.SUCCESS);

            final String uniqueImageZipFile = result.getBackUpImageName();

            Assert.assertEquals("Returned file name should equal the supplie image name", imageZipFile, uniqueImageZipFile);
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
            System.clearProperty(Exporter.NO_UNIQUE_IMAGE_SYSTEM_PROP);
        }
    }

    /**
     * Tests that the file "version" is created in the tmp folder used for generating the backup image from
     * @throws IOException
     */
    @Test
    public void testVersionBackup() throws Exception {
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome, null, "notusedinthistest",
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
        final Backup backup = BackupRestoreFactory.getBackupInstance(tmpSecureSpanHome,null, "notusedinthistest",
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
                true, System.out);

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
                true, System.out);

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
                true, System.out);

        try{
            backup.backUpComponentCA();

            final File backupFolder = backup.getBackupFolder();
            //Check config dir exists
            final File configDir = new File(backupFolder.getAbsolutePath(),
                    ImportExportUtilities.ComponentType.CA.getComponentName());

            Assert.assertTrue(ImportExportUtilities.ComponentType.CA.getComponentName() +
                    " directory should exist in '" + backupFolder.getAbsolutePath() + "'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ComponentType.CA.getComponentName() +
                    " directory should be a directory '" + backupFolder.getAbsolutePath() + "'", configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File nodeProp = new File(backupFolder.getAbsolutePath() + File.separator +
                    ImportExportUtilities.ComponentType.CA.getComponentName(), "empty.properties");
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName() + "'", nodeProp.exists());

            final File ssgLog = new File(backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName(), "empty.jar");

            Assert.assertTrue(ssgLog.getName() +
                    " should exist in '" + backupFolder.getAbsolutePath() + File.separator
                    + ImportExportUtilities.ComponentType.CA.getComponentName() + "'", ssgLog.exists());
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
                imageFileToCreate.getAbsolutePath(), true, System.out);

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
     * Test that if no port is supplied with the host string for -ftp_host, that the correct exception is thrown
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
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
}
