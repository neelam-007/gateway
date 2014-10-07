package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.jboss.util.file.FilenameSuffixFilter;
import org.junit.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This will test backup and restore on the current version of the database.
 *
 * This will catch issues like: SSG-7730
 *
 * @author Victor Kazakov
 */
public class BackupRestoreDatabaseTest {
    private String HOST_NAME = "localhost";
    private int PORT = 3306;
    private String DB_USER_NAME = "gateway";
    private String DB_USER_PASSWORD = "7layer";
    private String ADMIN_USER_NAME = "root";
    private String ADMIN_USER_PASSWORD = "7layer";

    private File tmpSecureSpanHome;
    private File tmpSsgHome;
    private File imageFileToCreate;

    private static final String OS_FILE_TO_COPY = "osfiletocopy";
    private DatabaseConfig newDBConfig;
    private Set<String> hosts = CollectionUtils.set("localhost");


    DBActions dbActions = new DBActions();
    private static final String DB_NAME = "backup_restore_db";
    private String db_version;

    @Before
    public void setUp() throws IOException {
        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/liquibase/ssg.xml", false);
        Assert.assertEquals("Could not create mysql backup_restore_db database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        db_version = dbActions.checkDbVersion(newDBConfig);

        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();
        imageFileToCreate = new File(ImportExportUtilities.createTmpDirectory(), "image1.zip");
        SyspropUtil.setProperty("com.l7tech.util.buildVersion", db_version);
        SyspropUtil.setProperty("com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString(false));
    }

    @After
    public void tearDown() {
        if (tmpSecureSpanHome != null) {
            if (tmpSecureSpanHome.exists()) {
                FileUtils.deleteDir(tmpSecureSpanHome);
            }
        }

        FileUtils.deleteDir(imageFileToCreate.getParentFile());
        SyspropUtil.clearProperty("com.l7tech.util.buildVersion");
        SyspropUtil.clearProperty("com.l7tech.gateway.config.backuprestore.checkversion");

        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
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
     * Copy all test resources into our temporary directory tearDown deletes the tmpSsgHome directory so do not need to
     * worry about cleaning up files copied via this method
     *
     * @throws IOException
     */
    public void createTestEnvironment() throws Exception {
        //Copy resources into this temp directory
        final URL nodeRes = this.getClass().getClassLoader().getResource("Gateway/node");
        final File nodeDirSrc = new File(nodeRes.getPath());
        final File nodeDirDest = new File(tmpSsgHome, "node");
        nodeDirDest.mkdir();
        ImportExportUtilities.copyDir(nodeDirSrc, nodeDirDest);

        final File nodePropertiesFile = new File(nodeDirDest, "default/etc/conf/node.properties");
        String content = new String(Files.readAllBytes(nodePropertiesFile.toPath()));
        content = content.replaceAll("node.db.config.main.host=doesnotexist.l7tech.com", "node.db.config.main.host=localhost");
        content = content.replaceAll("node.db.config.main.name=ssg_buzzcut", "node.db.config.main.name=backup_restore_db");
        Files.write(nodePropertiesFile.toPath(), content.getBytes());

        final URL runtimeRes = this.getClass().getClassLoader().getResource("Gateway/runtime");
        final File runtimeSrc = new File(runtimeRes.getPath());
        final File runtimeDest = new File(tmpSsgHome, "runtime");

        ImportExportUtilities.copyDir(runtimeSrc, runtimeDest);

        final URL configRes = this.getClass().getClassLoader().getResource("Gateway/config");
        final File configSrc = new File(configRes.getPath());
        final File configDest = new File(tmpSsgHome, "config");

        ImportExportUtilities.copyDir(configSrc, configDest);

        File ssgLiquibaseDir = new File(configDest, "etc/db");
        ssgLiquibaseDir.mkdirs();
        File liquibaseLocalDir = new File("etc/db/liquibase");
        for(File liquibaseFile : liquibaseLocalDir.listFiles(new FilenameSuffixFilter(".xml"))){
            FileUtils.copyFile(liquibaseFile, new File(ssgLiquibaseDir, liquibaseFile.getName()));
        }

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

    /**
     * Create a file called OS_FILE_TO_COPY in tmpSsgHome which can be used for testing OS backup
     *
     * @return File called OS_FILE_TO_COPY
     * @throws IOException
     */
    private File createPretendOsFile() throws IOException {
        final File osFile = new File(tmpSsgHome, OS_FILE_TO_COPY);
        osFile.createNewFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(osFile);
            fos.write("Important OS file with settings to backup".getBytes());
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
        return osFile;
    }

    /**
     * Creates the backup_manifest in tmpSsgHome/config/backup/cfg and writes the absolute path of osFile to it
     *
     * @param osFile the absolute path of this file will be written to backup_manifest
     * @throws IOException
     */
    private void createBackupManifest(File osFile) throws IOException {
        final File backupManifestFolder = new File(tmpSsgHome, "config/backup/cfg");
        FileUtils.ensurePath(backupManifestFolder);
        final File backupManifest = new File(backupManifestFolder, "backup_manifest");
        backupManifest.createNewFile();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(backupManifest);
            final String toWrite = osFile.getAbsolutePath() + "\n";
            fos.write(toWrite.getBytes());
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    @Test
    public void testBackupRestore() throws Exception {
        createTestEnvironment();

        final List<String> programArgs = new ArrayList<String>();
        programArgs.add("export");
        programArgs.add("-image");
        String tmpDir = null;
        try {
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
            Assert.assertTrue("image.zip should exist in '" + uniqueImageZipFile + "'", checkFile.exists());
            Assert.assertTrue("'" + uniqueImageZipFile + "' should not be a directory", !checkFile.isDirectory());

            //Import Test
            final Importer importer = new Importer(tmpSecureSpanHome, System.out);
            final String[] importArgs = new String[]{"import",
                    "-image", checkFile.getPath(),
                    "-dbu", ADMIN_USER_NAME,
                    "-dbp", ADMIN_USER_PASSWORD
            };

            final ImportExportUtilities.UtilityResult importResult = importer.restoreOrMigrateBackupImage(importArgs);
            Assert.assertNotNull("result should not be null", importResult);

            Assert.assertEquals("result should be success: Failed: " + (importResult.getFailedComponents() != null ? importResult.getFailedComponents().toString() : null) + " Error: " + (importResult.getException() != null ? importResult.getException().getMessage() : null), ImportExportUtilities.UtilityResult.Status.SUCCESS, importResult.getStatus());

        } finally {
            if (tmpDir != null) FileUtils.deleteDir(new File(tmpDir));
        }
    }
}
