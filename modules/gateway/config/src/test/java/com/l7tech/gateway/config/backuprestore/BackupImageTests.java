/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jul 6, 2009
 * Time: 2:48:06 PM
 */
package com.l7tech.gateway.config.backuprestore;

import org.junit.Test;
import org.junit.Assert;

import java.net.URL;

public class BackupImageTests {

    @Test
    public void testFiveOVersionReadCorrectly() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("fiveo_backup_with_audits.zip");
        BackupImage image = null;
        try{
            image = new BackupImage(preFiveOZip.getPath(), System.out, true);
            Assert.assertEquals("Incorrect version found", BackupImage.ImageVersion.FIVE_O, image.getImageVersion());
        }finally {
            if (image != null) image.removeTempDirectory();
        }
    }

    @Test
    public void testPostFiveOVersionReadCorrectly() throws Exception{
        final URL preFiveOZip = this.getClass().getClassLoader().getResource("image_buzzcut_with_audits.zip");
        BackupImage image = null;
        try{
            image = new BackupImage(preFiveOZip.getPath(), System.out, true);
            Assert.assertEquals("Incorrect version found", BackupImage.ImageVersion.AFTER_FIVE_O, image.getImageVersion());
        }finally {
            if (image != null) image.removeTempDirectory();
        }
    }

}
