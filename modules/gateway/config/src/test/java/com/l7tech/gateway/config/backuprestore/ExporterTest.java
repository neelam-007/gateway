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
 * Tests eveything in Exporter except for any database related backup
 * These tests write to temporary folder in the system temp directory
 * Regardless of test outcome, no temp folders will be left over in the system temp directory. If there are its a
 * coding error
 *
 * Any test which sets a system property should unset it in a finally block, so that it doesn't cause other tests
 * to fail
 */
public class ExporterTest {

    private File tmpSsgHome;
    private static final String OS_FILE_TO_COPY = "osfiletocopy";

    @Before
    public void setUp() throws IOException {
        final String tmpSsgHomeStr =ImportExportUtilities.createTmpDirectory();
        tmpSsgHome = new File(tmpSsgHomeStr);
    }

    @After
    public void tearDown(){
        if(tmpSsgHome == null) return;
        if(!tmpSsgHome.exists()) return;
        FileUtils.deleteDir(tmpSsgHome);
    }

    /**
     * Copy all test resources into our temporary directory
     * tearDown deletes the tmpSsgHome directory so do not need to worry about cleaning up files copied via this method
     * @throws IOException
     */
    public void createTestEnvironment() throws IOException {
        //Copy resources into this temp directory
        final URL nodeRes = this.getClass().getClassLoader().getResource("node");
        final File nodeDirSrc = new File(nodeRes.getPath());
        final File nodeDirDest = new File(tmpSsgHome, "node");
        nodeDirDest.mkdir();
        ImportExportUtilities.copyDir(nodeDirSrc, nodeDirDest);

        final URL runtimeRes = this.getClass().getClassLoader().getResource("runtime");
        final File runtimeSrc = new File(runtimeRes.getPath());
        final File runtimeDest = new File(tmpSsgHome, "runtime");

        ImportExportUtilities.copyDir(runtimeSrc, runtimeDest);

        final File osFile = createPretendOsFile();
        //config/backup/cfg/backup_manifest
        createBackupManifest(osFile);
    }

    @Test
    public void testConstructor(){
        final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        Assert.assertNotNull(exporter);
    }

    @Test(expected=NullPointerException.class)
    public void testConstructorException(){
        new Exporter(null, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
    }

    /**
     * Backup a test environment with no database or os files included. After the backup image is created, it is
     * tested for existence and is checked that it is a file and not a directory.
     */
    @Test
    public void testBackupImageCreated() throws IOException,
            BackupRestoreLauncher.FatalException, BackupRestoreLauncher.InvalidProgramArgumentException {
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
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
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
     * Backup a test environment with no database or os files included. After the backup image is created, it is
     * validated to have backed up the correct files, based on the project's resources
     */
    @Test
    public void testAndValidateBackupImage() throws IOException,
            BackupRestoreLauncher.FatalException, BackupRestoreLauncher.InvalidProgramArgumentException {
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
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    tmpSsgHome.getAbsolutePath()/*just used for existence check*/, true);
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
                            ImportExportUtilities.ImageDirectories.CONFIG.getDirName()));
            Assert.assertTrue(ImportExportUtilities.SSGLOG_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.SSGLOG_PROPERTIES).equals(
                            ImportExportUtilities.ImageDirectories.CONFIG.getDirName()));
            Assert.assertTrue(ImportExportUtilities.SYSTEM_PROPERTIES+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.SYSTEM_PROPERTIES).equals(
                            ImportExportUtilities.ImageDirectories.CONFIG.getDirName()));
            Assert.assertTrue(ImportExportUtilities.OMP_DAT+" should exist in the config folder",
                    fileToFolderMap.get(ImportExportUtilities.OMP_DAT).equals(
                            ImportExportUtilities.ImageDirectories.CONFIG.getDirName()));            

            //Custom assertions property files
            Assert.assertTrue("empty.properties should exist in the config folder",
                    fileToFolderMap.get("empty.properties").equals(ImportExportUtilities.ImageDirectories.CA.getDirName()));
            //Custom assertions jar files
            Assert.assertTrue("empty.jar should exist in the config folder",
                    fileToFolderMap.get("empty.jar").equals(ImportExportUtilities.ImageDirectories.CA.getDirName()));

            //Modular assertion aar files
            Assert.assertTrue("empty.aar should exist in the config folder",
                    fileToFolderMap.get("empty.aar").equals(ImportExportUtilities.ImageDirectories.MA.getDirName()));

            //os files
            //os files retain their complete folder strucutre in the image zip. Their root folder will come after
            //the /os folder in the imagezip, which is why tmpSsgHome is used in conjunction with the OS folder name
            //in this assertion
            Assert.assertTrue(OS_FILE_TO_COPY+" should exist in the os folder",
                    fileToFolderMap.get(OS_FILE_TO_COPY).equals(
                            ImportExportUtilities.ImageDirectories.OS.getDirName()+tmpSsgHome));

            //confirm pretend os file was copied
            Assert.assertTrue("empty.aar should exist in the config folder",
                    fileToFolderMap.get("empty.aar").equals(ImportExportUtilities.ImageDirectories.MA.getDirName()));

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
     * Tests that the Exporter correctly validates its command line arguments
     * -image is missing
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_NoExport() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly validates its command line arguments
     * -noexist parameter is supplied, which doesn't exist
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_InvalidArg() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-noexist");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly validates its command line arguments
     * export is missing
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testInvalidExporterArgs_ImageNeedsValue() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        final String[] args = programArgs.toArray(new String[]{});
        final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        exporter.createBackupImage(args);
    }

    /**
     * Tests that the Exporter correctly observes the
     * com.l7tech.gateway.config.backuprestore.nomodifyimagename.nouniqueimagename system property.
     */
    @Test
    public void testInvalidExporterArgs_NoUniqueImageName() throws BackupRestoreLauncher.FatalException, IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
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
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
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
    public void testVersionBackup() throws IOException {
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
            exporter.backUpVersion(tmpDir);

            //Check version file exists
            final File checkFile = new File(tmpDir, ImportExportUtilities.VERSION);
            Assert.assertTrue(ImportExportUtilities.VERSION + " file should exist in '" + tmpDir+"'", checkFile.exists());
            Assert.assertTrue("'" + checkFile.getName()+"' should not be empty", checkFile.length() > 0);
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Tests that the folder "config" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct configuration files
     * @throws IOException
     */
    @Test
    public void testConfigBackup() throws IOException {
        createTestEnvironment();
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
            exporter.backUpComponentConfig(tmpDir);

            //Check config dir exists
            final File configDir = new File(tmpDir, ImportExportUtilities.ImageDirectories.CONFIG.getDirName());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CONFIG.getDirName() +
                    " directory should exist in '" + tmpDir+"'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CONFIG.getDirName() +
                    " directory should be a directory '" + tmpDir+"'", configDir.isDirectory());

            //Test for the individual files
            final File nodeProp = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName(),
                    ImportExportUtilities.NODE_PROPERTIES);
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName()+"'",
                    nodeProp.exists());

            final File ssgLog = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName(),
                    ImportExportUtilities.SSGLOG_PROPERTIES);

            Assert.assertTrue(ssgLog.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName()+"'",
                    ssgLog.exists());

            final File systemProp = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName(),
                    ImportExportUtilities.SYSTEM_PROPERTIES);

            Assert.assertTrue(systemProp.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName()+"'",
                    systemProp.exists());

            final File ompDat = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName(),
                    ImportExportUtilities.OMP_DAT);

            Assert.assertTrue(ompDat.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CONFIG.getDirName()+"'",
                    ompDat.exists());

        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
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
    public void testOsBackup() throws IOException {
        //Make a file which will constitute the os backup
        final File osFile = createPretendOsFile();
        //config/backup/cfg/backup_manifest
        createBackupManifest(osFile);

        FileOutputStream fos = null;
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();

            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    tmpSsgHome.getAbsolutePath()/*just used for existence check*/, true);
            exporter.backUpComponentOS(tmpDir);

            //confirm pretend os file was copied
            final File checkOsFile = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.OS.getDirName() +
                    File.separator + osFile.getAbsolutePath());
            Assert.assertTrue(checkOsFile.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.OS.getDirName()+"'",
                    checkOsFile.exists());
        } finally{
            ResourceUtils.closeQuietly(fos);
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Tests the custom assertion backup
     * Tests that the folder "ca" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct configuration and jar files, based on this project's resources
     * @throws IOException
     */
    @Test
    public void testCaBackup() throws IOException {
        createTestEnvironment();
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
            exporter.backUpComponentCA(tmpDir);

            //Check config dir exists
            final File configDir = new File(tmpDir, ImportExportUtilities.ImageDirectories.CA.getDirName());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CA.getDirName() +
                    " directory should exist in '" + tmpDir+"'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CA.getDirName() +
                    " directory should be a directory '" + tmpDir+"'", configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File nodeProp = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName(),
                    "empty.properties");
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName()+"'",
                    nodeProp.exists());

            final File ssgLog = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName(),
                    "empty.jar");

            Assert.assertTrue(ssgLog.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName()+"'",
                    ssgLog.exists());
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Test the modular assertion backup
     * Tests that the folder "ma" is created in the tmp folder used for generating the backup image from, and that
     * it contains the correct aar files, based on this project's resources
     * @throws IOException
     */
    @Test
    public void testMaBackup() throws IOException {
        createTestEnvironment();
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final Exporter exporter = new Exporter(tmpSsgHome, System.out,
                    ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
            exporter.backUpComponentCA(tmpDir);

            //Check config dir exists
            final File configDir = new File(tmpDir, ImportExportUtilities.ImageDirectories.CA.getDirName());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CA.getDirName() +
                    " directory should exist in '" + tmpDir+"'", configDir.exists());
            Assert.assertTrue(ImportExportUtilities.ImageDirectories.CA.getDirName() +
                    " directory should be a directory '" + tmpDir+"'", configDir.isDirectory());

            //Test for the individual files, based on this project's resources
            final File nodeProp = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName(),
                    "empty.properties");
            Assert.assertTrue(nodeProp.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName()+"'",
                    nodeProp.exists());

            final File ssgLog = new File(tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName(),
                    "empty.jar");

            Assert.assertTrue(ssgLog.getName() +
                    " should exist in '" +
                    tmpDir+File.separator+ ImportExportUtilities.ImageDirectories.CA.getDirName()+"'",
                    ssgLog.exists());
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Test the creation of a zip file
     * @throws IOException
     */
    @Test
    public void testCreateImageFile() throws IOException {
        createTestEnvironment();
        String tmpDir = null;
        try{
            tmpDir = ImportExportUtilities.createTmpDirectory();
            final Exporter exporter = new Exporter(tmpSsgHome, System.out, tmpSsgHome.getAbsolutePath(), true);
            exporter.backUpComponentCA(tmpDir);//just back up something

            //zip the tmpDir
            final String testZipFile = "testzip.zip";
            exporter.createImageZip(tmpSsgHome+File.separator+ testZipFile, tmpDir);

            //confirm zip file exists, is a file and is not empty
            final File zipFile = new File(tmpSsgHome, testZipFile);
            Assert.assertTrue(testZipFile + " should exist in '" + tmpSsgHome+"'", zipFile.exists());
            Assert.assertTrue(testZipFile + " should not be a directory", !zipFile.isDirectory());
            Assert.assertTrue(testZipFile + " should not be empty", zipFile.length() > 0);
        }finally{
            if(tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }

    /**
     * Test that getFtpConfig() correctly constructs a FtpClientConfig object with the parameters supplied to Exporter
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    @Test
    public void testGetFtpConfig() throws IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        final Exporter export = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        final Map<String, String> args = new HashMap<String, String>();
        args.put(Exporter.FTP_HOST.name, "donal.l7tech.com:21");
        args.put(Exporter.FTP_USER.name, "root");
        args.put(Exporter.FTP_PASS.name, "7layer");
        args.put(Exporter.IMAGE_PATH.name, "image.zip");

        final FtpClientConfig ftpConfig = export.getFtpConfig(args);
        Assert.assertNotNull(ftpConfig);

        Assert.assertEquals("Host name should equal donal.l7tech.com", "donal.l7tech.com", ftpConfig.getHost());
        Assert.assertEquals("Port number should be 21", 21, ftpConfig.getPort());
        Assert.assertEquals("User name should be root", "root", ftpConfig.getUser());
        Assert.assertEquals("User pass should be 7layer", "7layer", ftpConfig.getPass());
    }

    /**
     * Test that if no port is supplied with the host string for -ftp_host, that the correct exception is thrown
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testValidateFtpParametersNoPort() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final Exporter export = new Exporter(tmpSsgHome, System.out,
                ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);

        final Map<String, String> args = new HashMap<String, String>();
        args.put(Exporter.FTP_HOST.name, "donal.l7tech.com");
        args.put(Exporter.FTP_USER.name, "root");
        args.put(Exporter.FTP_PASS.name, "7layer");
        args.put(Exporter.IMAGE_PATH.name, "image.zip");

        export.checkAndValidateFtpParams(args);
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
