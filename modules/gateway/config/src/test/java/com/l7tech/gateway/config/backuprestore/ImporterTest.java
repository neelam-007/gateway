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
    public void setUp() throws IOException {
        final String tmpImageLocation =ImportExportUtilities.createTmpDirectory();
        unzipDirectory = new File(tmpImageLocation);

        final String tmpSsgHomeLocation = ImportExportUtilities.createTmpDirectory();
        tmpSsgHome = new File(tmpSsgHomeLocation);
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
    }

    @Test
    public void testImportFiveO() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("image_ia.zip");
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
    }
}
