/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 19, 2009
 * Time: 10:14:23 AM
 */
package com.l7tech.gateway.config.backuprestore;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;

import java.io.IOException;
import java.io.File;
import java.net.URL;

import com.l7tech.util.FileUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.test.BugNumber;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;

public class ImporterTest {

    private File tmpSecureSpanHome;
    private File tmpSsgHome;

    @Before
    public void setUp() throws Exception {
        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();

        createTestEnvironment();
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

        //Set up the esm
        final URL esmRes = this.getClass().getClassLoader().getResource("EnterpriseManager");
        final File esmDirSrc = new File(esmRes.getPath());
        final File esmDirDest = new File(tmpSecureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER);
        esmDirDest.mkdir();
        ImportExportUtilities.copyDir(esmDirSrc, esmDirDest);
    }


    /**
     * Test that node.properties is managed correctly
     * @throws Exception
     */
//    @Test
//    public void testNodeProperties() throws Exception{
//        final URL preFiveOZip = this.getClass().getClassLoader().getResource("fiveo_backup_with_audits.zip");
//        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
//        final String [] args = new String[]{"import",
//                "-image", preFiveOZip.getPath(),
//                "-dbu", "layer7",
//                "-dbp", "7layer",
//                "-v",
//                "-dbh", "donal.l7tech.com",
//                "-db", "ssg_buzzcut",
//                "-cp", "111111",
//                "-gdbu", "gateway",
//                "-gdbp", "7layer"
//        };
//
//        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
//        Assert.assertNotNull("result should not be null", result);
//
//        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
//
//        //check node.properties
//
//        final File nodeProp = new File(tmpSsgHome,
//                ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);
//
//        final PropertiesConfiguration returnConfig = new PropertiesConfiguration();
//        returnConfig.load(nodeProp);
//
//        //Check dbh has been changed correctly
//        final String dbHost = returnConfig.getString("node.db.config.main.host");
//        Assert.assertEquals("Invalid host found in node.properties", "donal.l7tech.com", dbHost);
//    }

    @Test
    public void testImportFiveO() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("fiveo_backup_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-dbu", "layer7",
                "-dbp", "7layer",
                "-v",
                "-dbh", "donal.l7tech.com",
                "-db", "ssg_buzzcut",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * 5.0 and Buzzcut have the same structure for os files
     * @throws Exception
     */
    @Test
    public void testOsFileInport() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("fiveo_backup_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-os"
        };

        //this test validtes of files too
        //=> make the appliance direcotry
        final File applianceFolder = new File(tmpSecureSpanHome, ImportExportUtilities.APPLIANCE);
        applianceFolder.mkdir();

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the entire process of restoring os files. This will 1) retore files and 2) mimic the gateway restart
     * Also tests the buzzcut image
     * @throws Exception
     */
    @Test
    public void testOsCompleteRoundTrip() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os",
                "-v"
        };

        //this test validtes of files too
        //=> make the appliance direcotry
        final File applianceFolder = new File(tmpSecureSpanHome, ImportExportUtilities.APPLIANCE);
        applianceFolder.mkdir();
        
        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());

        final OSConfigManager osConfigManager =
                new OSConfigManager(tmpSsgHome, true, true, System.out);

        final String tempDirectory = ImportExportUtilities.createTmpDirectory();
        try {
            //modify the root folder of where files are copied to
            System.setProperty("com.l7tech.config.backuprestore.osrootdir", tempDirectory);
            final boolean filesCopied = osConfigManager.finishRestoreOfFilesOnReboot();
            Assert.assertTrue("Files should have been copied", filesCopied);
        } finally{
            FileUtils.deleteDir(new File(tempDirectory));
            System.clearProperty("com.l7tech.config.backuprestore.osrootdir");
        }
    }

    /**
     * We explicitly ask for os, but we have no os in the backup image. As we don't use -halt, no exception is thrown
     * and partial success is reported
     * @throws Exception
     */
    @Test
    public void testOsNotApplicable() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_no_OS.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be partial success", ImportExportUtilities.UtilityResult.Status.PARTIAL_SUCCESS,
                result.getStatus());
    }

    /**
     * We explicitly ask for os, but we have no os in the backup image. As -halt is used, an exception
     * should be thrown
     * @throws Exception
     */
    @Test
    public void testOsFailure() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_no_OS.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be failure", ImportExportUtilities.UtilityResult.Status.FAILURE,
                result.getStatus());
    }

    /**
     * Test partial success when a selective backup is done, when an attempt to back up os will be made,
     * but as no os is contained, the result should be partial success
     *
     * This test is here to comlete the main code paths through the os restore code 
     * @throws Exception
     */
    @Test
    public void testOsPartialSuccess_NotRequired() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_no_OS.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "notused",
                "-dbp", "notused"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be failure", ImportExportUtilities.UtilityResult.Status.SUCCESS,
                result.getStatus());
    }

    /**
     * Test the restore of the CA component, when it's asked for
     * @throws Exception
     */
    @Test
    public void testCARestore() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-ca",
                "-halt",
                "-v"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS,
                result.getStatus());

        //confirm the symantext jar and properties file were correctly copied
        final File symantecJar =
                new File(tmpSsgHome, ImportExportUtilities.CA_JAR_DIR + File.separator + "symantec_antivirus.jar");
        Assert.assertTrue("symantec_antivirus should exist at: " + symantecJar.getAbsolutePath(), symantecJar.exists());
        Assert.assertFalse("File '" + symantecJar.getAbsolutePath()+"' should not be a directory",
                symantecJar.isDirectory());

        //confirm the property file was copied correctly
        final File symantecProperties = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator
                + "symantec_scanengine_client.properties");

        Assert.assertTrue("symantec_antivirus property file should exist at: " + symantecProperties.getAbsolutePath(),
                symantecProperties.exists());
        Assert.assertTrue("File '" + symantecProperties.getAbsolutePath()+"' should be a file",
                symantecProperties.isFile());
    }

    /**
     * Test the restore of the MA component, when it's asked for
     *
     * This also tests that when a modular asssertion is found on a target, it is not overwritten by whats in the
     * back up image. EchoRoutingAssertion is a test resource and causes the newer 5.1 from the back up image
     * to be ignored
     * @throws Exception
     */
    @Test
    public void testMARestore() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-ma",
                "-halt",
                "-v"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS,
                result.getStatus());

        //test the following jars were created

        final String [] allMaAars = new String[]{
            "LDAPQueryAssertion-5.1.aar",
            "RateLimitAssertion-5.1.aar",
            "IdentityAttributesAssertion-5.1.aar",
            "CertificateAttributesAssertion-5.1.aar",
            "WsAddressingAssertion-5.1.aar",
            "SnmpTrapAssertion-5.1.aar",
            "NcesDecoratorAssertion-5.1.aar",
            "MessageContextAssertion-5.1.aar",
            "SamlpAssertion-5.1.aar",
            "FtpCredentialAssertion-5.1.aar",
            "EsmAssertion-5.1.aar",
            "FtpRoutingAssertion-5.1.aar",
            "NcesValidatorAssertion-5.1.aar",
            "XacmlPdpAssertion-5.1.aar",
            "ComparisonAssertion-5.1.aar",
            "EchoRoutingAssertion-5.0.aar"
        };

        for(String fileName: allMaAars){
            final File maAar = new File(tmpSsgHome, ImportExportUtilities.MA_AAR_DIR + File.separator + fileName);
            Assert.assertTrue("Modular assertion file should exist at: " + maAar.getAbsolutePath(),
                    maAar.exists());
            Assert.assertTrue("Modular assertion aar file '" + maAar.getAbsolutePath()+"' should be a file",
                    maAar.isFile());

        }
    }

    /**
     * Test the restore of the config component, when it's asked for. This tests the code path, but cannot
     * detect if files were successfully deleted and recreated
     * @throws Exception
     */
    @Test
    public void testConfigRestore_CodePath() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-config",
                "-halt",
                "-v"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS,
                result.getStatus());

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            Assert.assertTrue("Config file should exist at: " + configFile.getAbsolutePath(),
                    configFile.exists());
            Assert.assertTrue("Config file '" + configFile.getAbsolutePath()+"' should be a file",
                    configFile.isFile());
        }
        
        //read node.properties and confirm that the files node.properties and omp.dat were copied to the restore host
        final File nodeProperties = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);
        final File ompFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.OMP_DAT);
        final DatabaseConfig config = ImportExportUtilities.getDatabaseConfig(nodeProperties, ompFile);

        //Test that node.properties and omp.dat were copied successfull.
        //the restore host's node.properties has a value of doesnotexist.l7tech.com for the hostname
        Assert.assertEquals("Incorrect properties value found", "localhost", config.getHost());
        Assert.assertEquals("Incorrect properties value found", "gateway", config.getNodeUsername());
        //Confirm that passwords and cluster passphrase can be decrypted successfully
        Assert.assertEquals("Incorrect properties value found", "7layer", config.getNodePassword());

        //Confirm the cluster passphrase
        final MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodeProperties, true);
        Assert.assertEquals("Incorrect properties value found", "111111", new String(decryptor.decryptPassword(nodeConfig.getClusterPassphrase())));

    }

    /**
     * Test the restore of the config component, explicitly by working with the Restore interface. This guarantees
     * that the config files are successfully restored, as they are deleted before the restore happens
     * @throws Exception
     */
    @Test
    public void testConfigRestore_RestorePath() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final BackupImage image = new BackupImage(buzzcutImage.getPath(), System.out, true);
        final Restore restore =
                BackupRestoreFactory.getRestoreInstance(tmpSecureSpanHome, image, null, "notused", true, System.out);

        //delete all the config files that were created as part of this test set up

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            configFile.delete();
        }

        restore.restoreComponentConfig(false, false);

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            Assert.assertTrue("Config file should exist at: " + configFile.getAbsolutePath(),
                    configFile.exists());
            Assert.assertTrue("Config file '" + configFile.getAbsolutePath()+"' should be a file",
                    configFile.isFile());
        }
    }

    /**
     * Test the restore of the config component, with the migrate capability, explicitly by working with the Restore
     * interface. This guarantees that the config files are successfully restored, as they are deleted by the test case
     * before the restore happens
     * <p/>
     * Ensures that node.identity files - node.properties and omp.dat are not copied when doing a migrate
     * <p/>
     * The test resouce exclude_files lists ssglog.properties, as isMigrate is true in this test, this file is
     * not copied and is excluded
     *
     * @throws Exception
     */
    @Test
    @BugNumber(7741)
    public void testConfigRestoreWithMigrateAndExcludeFile() throws Exception {
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final BackupImage image = new BackupImage(buzzcutImage.getPath(), System.out, true);
        final Restore restore =
                BackupRestoreFactory.getRestoreInstance(tmpSecureSpanHome, image, null, "notused", true, System.out);

        //delete all the config files that were created as part of this test set up

        for (String fileName : ImportExportUtilities.CONFIG_FILES) {
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            configFile.delete();
        }

        //true for migrate
        restore.restoreComponentConfig(true, false);

        //node.properties should not exist

        for (String fileName : ImportExportUtilities.CONFIG_FILES) {
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            if (fileName.equals(ImportExportUtilities.NODE_PROPERTIES)) {
                Assert.assertFalse("Config file should never be copied for migrate and should not exist at: "
                        + configFile.getAbsolutePath(), configFile.exists());
            } else if (fileName.equals(ImportExportUtilities.OMP_DAT)) {
                Assert.assertFalse("Config file should never be copied for migrate and should not exist at: "
                        + configFile.getAbsolutePath(), configFile.exists());
            } else if (fileName.equals("ssglog.properties")) {
                Assert.assertFalse("Config file should not exist at: " + configFile.getAbsolutePath(), configFile.exists());
            } else {
                Assert.assertTrue("Config file should exist at: " + configFile.getAbsolutePath(),
                        configFile.exists());
                Assert.assertTrue("Config file '" + configFile.getAbsolutePath() + "' should be a file",
                        configFile.isFile());
            }
        }
    }

    /**
     * Tests that when a migrate is done with -config, that the db command line params are always written to the
     * migrate hosts node.properties
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    @Test
    @BugNumber(7741)
    public void testMigrateParamsWrittenOnConfigOnly() throws Exception {
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-config",
                "-migrate",
                "-dbh", "localhost", // this is a different value to the canned value in test resources node.properties
                "-db", "ssg_buzzcut", //this has to match the canned node.properties value as otherwise -newdb is required
                "-dbu", "root",
                "-dbp", "7layer",
                "-gdbu", "nodeuser", // this is a different value to the canned value in test resources node.properties
                "-gdbp", "nodepwd", // this is a different value to the canned value in test resources node.properties
                "-cp", "clusterpp", // this is a different value to the canned value in test resources node.properties
                "-halt",
                "-v"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", ImportExportUtilities.UtilityResult.Status.SUCCESS,
                result.getStatus());

        //read node.properties and confirm that the above command line options were written
        final File nodeProperties = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);
        final File ompFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.OMP_DAT);
        final DatabaseConfig config = ImportExportUtilities.getDatabaseConfig(nodeProperties, ompFile);

        Assert.assertEquals("Incorrect properties value found", "localhost", config.getHost());
        Assert.assertEquals("Incorrect properties value found", "nodeuser", config.getNodeUsername());
        Assert.assertEquals("Incorrect properties value found", "nodepwd", config.getNodePassword());

        //Confirm the cluster passphrase
        final MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodeProperties, true);
        Assert.assertEquals("Incorrect properties value found", "clusterpp", new String(decryptor.decryptPassword(nodeConfig.getClusterPassphrase())));

    }

    @Test
    public void testESMRestore() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final BackupImage image = new BackupImage(buzzcutImage.getPath(), System.out, true);
        final Restore restore =
                BackupRestoreFactory.getRestoreInstance(tmpSecureSpanHome, image, null, "notused", true, System.out);

        final ComponentResult result = restore.restoreComponentESM();
        Assert.assertEquals("Incorrect result found", ComponentResult.Result.SUCCESS, result.getResult());

    }
}
