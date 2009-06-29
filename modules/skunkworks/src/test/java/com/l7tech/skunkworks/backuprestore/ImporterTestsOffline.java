/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 12:17:11 PM
 */
package com.l7tech.skunkworks.backuprestore;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import com.l7tech.gateway.config.backuprestore.*;
import com.l7tech.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImporterTestsOffline {

    private File unzipDirectory;
    private File tmpSsgHome;

    @Before
    public void setUp() throws IOException {
        final String tmpImageLocation = ImportExportUtilities.createTmpDirectory();
        unzipDirectory = new File(tmpImageLocation);

        final String tmpSsgHomeLocation = ImportExportUtilities.createTmpDirectory();
        tmpSsgHome = new File(tmpSsgHomeLocation);
        setUpEnvironment();
        System.setProperty("com.l7tech.util.buildVersion", "5.1.0");
        System.setProperty("com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString(false));
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
        System.clearProperty("com.l7tech.gateway.config.backuprestore.checkversion");
    }

    @Test
    public void testImportFiveOIntoBuzzcut()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-db", "ssg_buzzcut",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the contained node.properties
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodeProperties()
            throws Exception {
        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if (!projectSqlFile.exists() || projectSqlFile.isDirectory())
            throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String[] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v"
        };

        try{
            System.setProperty("com.l7tech.config.backuprestore.mycnfdir", tmpSsgHome.getAbsolutePath());
            final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
            Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
        }finally{
            System.clearProperty("com.l7tech.config.backuprestore.mycnfdir");
        }
    }

    /**
     * Test the restore capability to download the image from an ftp server
     * vsftpd is an ftp server you can use for testing
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_FTP()
            throws Exception {
        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if (!projectSqlFile.exists() || projectSqlFile.isDirectory())
            throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final String tmpImageLocation = ImportExportUtilities.createTmpDirectory();
        final File tmpImage = new File(tmpImageLocation, "image.zip");
        FileUtils.copyFile(new File(buzzcutImage.getPath()), tmpImage);

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String[] args = new String[]{"import",
                "-image", tmpImage.getAbsolutePath(),/*ftp will extract the directory from the image path*/
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-ftp_host", "irishman.l7tech.com",
                "-ftp_user", "layer7",
                "-ftp_pass", "layer7",
                "-maindb"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, when the new selective restore options are being used 
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodePropertiesSelective()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v"     ,
                "-maindb"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the command line (easier for testing) and the
     * database is remote. This should succeed due to setting the system property:
     * com.l7tech.config.backup.localDbOnly=false
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodeProperties_RemoteDatabase()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "donal.l7tech.com", /*remote db*/
                "-db", "ssg_buzzcut1",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-maindb"
        };

        try{
            System.setProperty("com.l7tech.config.backup.localDbOnly", Boolean.toString(false));
            final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
            Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
        }finally{
            System.clearProperty("com.l7tech.config.backup.localDbOnly");            
        }
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the command line
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_DbCommandLIne()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-db", "ssg_buzzcut",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-maindb"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    //------------------------------------------------------------------------------------------------------------------
    //Migrate Tests
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Test creating the new db and migrating a buzzcut image. Make sure the db doesn't exist before running
     * this test
     */
    @Test
    public void testMigrateBuzzcutIntoBuzzcut_DbNoExist()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-migrate",
                "-newdb", "ssg_buzzcut"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test migrating a buzzcut image with mappings. Make sure the db exists before running this test
     */
    @Test
    public void testMigrateBuzzcutIntoBuzzcut_DbExistsAndMappings()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final URL mappingFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/mapping_buzzcut.xml");

        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-migrate",
                "-mapping", mappingFile.getPath()
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test that when the exclude tables happens, we don't try and restore the license if cluster_properties was
     * not recreated
     */
    @Test
    public void testMigrateBuzzcutIntoBuzzcut_ClusterPropertiesExcluded()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        //require exclude_tables in tmpSsgHome
        final URL excludeTables = this.getClass().getClassLoader().getResource("config/backup/cfg/exclude_tables");
        FileUtils.ensurePath(new File(tmpSsgHome, "config/backup/cfg"));
        FileUtils.copyFile(new File(excludeTables.getPath()), new File(tmpSsgHome + File.separator + "config/backup/cfg", "exclude_tables"));
        
        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final URL mappingFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/mapping_buzzcut.xml");

        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-migrate",
                "-mapping", mappingFile.getPath()
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test creating the new db and loading the image
     */
    @Test
    public void testMigrateFiveOIntoBuzzcut_DbNoExist()
            throws Exception {

        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-migrate",
                "-newdb", "ssg_buzzcut"
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test loading mapping after a migrate. The target database must exist
     */
    @Test
    public void testMigrateFiveOIntoBuzzcut_Mappings()
            throws Exception {

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");
        final URL mappingFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/mapping.xml");

        final Importer importer = new Importer(tmpSsgHome, System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
        final String [] args = new String[]{"import",
                "-image", preFiveOZip.getPath(),
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-migrate",
                "-mapping", mappingFile.getPath()
        };

        final Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", Importer.RestoreMigrateResult.Status.SUCCESS, result.getStatus());
    }

    private void setUpEnvironment() throws IOException {
        //Copy ssg.sql for test
        final File projectSqlFile = new File("etc/db/mysql/ssg.sql");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.sql");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_SQL));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_SQL));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));
    }
}
