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

public class ImporterTest {

    private File unzipDirectory;
    private File tmpSsgHome;

    @Before
    public void setUp() throws Exception {
        final String tmpImageLocation =ImportExportUtilities.createTmpDirectory();
        unzipDirectory = new File(tmpImageLocation);

        final String tmpSsgHomeLocation = ImportExportUtilities.createTmpDirectory();
        tmpSsgHome = new File(tmpSsgHomeLocation);

        createTestEnvironment();
        System.setProperty("com.l7tech.util.buildVersion", "5.1.0");
    }

    @After
    public void tearDown(){
        if(unzipDirectory != null){
            if(unzipDirectory.exists()){
                FileUtils.deleteDir(unzipDirectory);
            }
        }

        if(tmpSsgHome != null){
            if(tmpSsgHome.exists()){
                FileUtils.deleteDir(tmpSsgHome);    
            }
        }
        System.clearProperty("com.l7tech.util.buildVersion");
    }

    /**
     * Copy all test resources into our temporary directory
     * tearDown deletes the tmpSsgHome directory so do not need to worry about cleaning up files copied via this method
     * @throws IOException
     */
    public void createTestEnvironment() throws Exception {
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

        final URL configRes = this.getClass().getClassLoader().getResource("config");
        final File configSrc = new File(configRes.getPath());
        final File configDest = new File(tmpSsgHome, "config");

        ImportExportUtilities.copyDir(configSrc, configDest);

    }
    
    @Test
    public void testImportFiveO() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("fiveo_backup_with_audits.zip");
        Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE, true);
        String [] args = new String[]{"import",
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

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
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
        Importer importer = new Importer(tmpSsgHome, System.out,
                tmpSsgHome.getAbsolutePath()/*meet os restore requirement*/
                , true);
        String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-os"
        };

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
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
        final Importer importer = new Importer(tmpSsgHome, System.out,
                tmpSsgHome.getAbsolutePath()/*meet os restore requirement*/
                , true);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os"
        };

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
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
        final Importer importer = new Importer(tmpSsgHome, System.out,
                tmpSsgHome.getAbsolutePath()/*meet os restore requirement*/
                , true);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os"
        };

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
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
        final Importer importer = new Importer(tmpSsgHome, System.out,
                tmpSsgHome.getAbsolutePath()/*meet os restore requirement*/
                , true);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-os",
                "-halt"
        };

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
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
        final Importer importer = new Importer(tmpSsgHome, System.out,
                tmpSsgHome.getAbsolutePath()/*meet os restore requirement*/
                , true);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "notused",
                "-dbp", "notused"
        };

        Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertNotNull("result should not be null", result);

        Assert.assertEquals("result should be failure", Importer.RestoreMigrateResult.Status.SUCCESS,
                result.getStatus());
    }

}
