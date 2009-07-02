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
import java.util.Map;
import java.util.HashMap;

import com.l7tech.util.FileUtils;

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
                "-db", "ssg",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
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
        
        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());

        final OSConfigManager osConfigManager =
                new OSConfigManager(tmpSsgHome, true, true, System.out);

        final String tempDirectory = ImportExportUtilities.createTmpDirectory();
        try {
            //modify the root folder of where files are copied to
            System.setProperty("com.l7tech.config.backuprestore.osrootdir", tempDirectory);
            osConfigManager.finishRestoreOfFilesOnReboot();
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be partial success", Importer.RestoreMigrateResult.Status.PARTIAL_SUCCESS,
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be failure", Importer.RestoreMigrateResult.Status.FAILURE,
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be failure", Importer.RestoreMigrateResult.Status.SUCCESS,
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS,
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS,
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
            "EchoRoutingAssertion-5.1.aar"
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

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be success", Importer.RestoreMigrateResult.Status.SUCCESS,
                result.getStatus());

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            Assert.assertTrue("Config file should exist at: " + configFile.getAbsolutePath(),
                    configFile.exists());
            Assert.assertTrue("Config file '" + configFile.getAbsolutePath()+"' should be a file",
                    configFile.isFile());
        }
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

        restore.restoreComponentConfig(true, false, false);

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
     * interface. This guarantees that the config files are successfully restored, as they are deleted before the
     * restore happens
     *
     * The test resouce exclude_files lists node.properties, as isMigrate is true in this test, node.properties is
     * not copied and is excluded
     * @throws Exception
     */
    @Test
    public void testConfigRestoreWithMigrateAndExcludeFile() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final BackupImage image = new BackupImage(buzzcutImage.getPath(), System.out, true);
        final Restore restore =
                BackupRestoreFactory.getRestoreInstance(tmpSecureSpanHome, image, null, "notused", true, System.out);

        //delete all the config files that were created as part of this test set up

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            configFile.delete();
        }

        //true for migrate
        restore.restoreComponentConfig(true, true, false);

        //node.properties should not exist

        for(String fileName: ImportExportUtilities.CONFIG_FILES){
            final File configFile = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR + File.separator + fileName);
            if(fileName.equals("node.properties")){
                Assert.assertFalse("Config file should not exist at: " + configFile.getAbsolutePath(),
                        configFile.exists());
            }else{
                Assert.assertTrue("Config file should exist at: " + configFile.getAbsolutePath(),
                        configFile.exists());
                Assert.assertTrue("Config file '" + configFile.getAbsolutePath()+"' should be a file",
                        configFile.isFile());
            }
        }
    }

    @Test
    public void testESMRestore() throws Exception{
        final URL buzzcutImage = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        final BackupImage image = new BackupImage(buzzcutImage.getPath(), System.out, true);
        final Restore restore =
                BackupRestoreFactory.getRestoreInstance(tmpSecureSpanHome, image, null, "notused", true, System.out);

        final Restore.Result result = restore.restoreComponentESM(true);
        Assert.assertEquals("Incorrect result found", Restore.Result.SUCCESS, result);

    }
}
