/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 12:17:11 PM
 */
package com.l7tech.skunkworks.backuprestore;

import com.l7tech.gateway.config.backuprestore.ImportExportUtilities;
import com.l7tech.gateway.config.backuprestore.Importer;
import com.l7tech.gateway.config.backuprestore.MigrateToRestoreConvertor;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImporterTestsOffline {

    private File unzipDirectory;
    private File tmpSsgHome;
    private File tmpSecureSpanHome;

    @Before
    public void setUp() throws IOException {
        final String tmpImageLocation = ImportExportUtilities.createTmpDirectory();
        unzipDirectory = new File(tmpImageLocation);

        final String tmpSecureSpanHomeStr = ImportExportUtilities.createTmpDirectory();
        tmpSecureSpanHome = new File(tmpSecureSpanHomeStr);
        tmpSsgHome = new File(tmpSecureSpanHome, ImportExportUtilities.GATEWAY);
        tmpSsgHome.mkdir();

        setUpEnvironment();
        SyspropUtil.setProperty( "com.l7tech.util.buildVersion", "5.1.0" );
        SyspropUtil.setProperty( "com.l7tech.gateway.config.backuprestore.checkversion", Boolean.toString( false ) );
    }

    @After
    public void tearDown(){
        if(unzipDirectory != null){
            if(unzipDirectory.exists()){
                FileUtils.deleteDir(unzipDirectory);
            }
        }

        if(tmpSecureSpanHome != null){
            if(tmpSecureSpanHome.exists()){
                FileUtils.deleteDir(tmpSecureSpanHome);
            }
        }

        SyspropUtil.clearProperty( "com.l7tech.util.buildVersion" );
        SyspropUtil.clearProperty( "com.l7tech.gateway.config.backuprestore.checkversion" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.util.buildVersion",
            "com.l7tech.gateway.config.backuprestore.checkversion",
            "com.l7tech.config.backuprestore.mycnfdir",
            "com.l7tech.config.backup.localDbOnly"
        );
    }

    @Test
    public void testImportFiveOIntoBuzzcut()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the contained node.properties
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodeProperties()
            throws Exception {
        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if (!projectSqlFile.exists() || projectSqlFile.isDirectory())
            throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String[] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v"
        };

        try{
            SyspropUtil.setProperty( "com.l7tech.config.backuprestore.mycnfdir", tmpSsgHome.getAbsolutePath() );
            final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
            Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
        }finally{
            SyspropUtil.clearProperty( "com.l7tech.config.backuprestore.mycnfdir" );
        }
    }

    /**
     * Test the restore capability to download the image from an ftp server
     * vsftpd is an ftp server you can use for testing
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_FTP()
            throws Exception {
        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if (!projectSqlFile.exists() || projectSqlFile.isDirectory())
            throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final String tmpImageLocation = ImportExportUtilities.createTmpDirectory();
        final File tmpImage = new File(tmpImageLocation, "image.zip");
        FileUtils.copyFile(new File(buzzcutImage.getPath()), tmpImage);

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, when the new selective restore options are being used 
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodePropertiesSelective()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v"     ,
                "-maindb"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the command line (easier for testing) and the
     * database is remote. This should succeed due to setting the system property:
     * com.l7tech.config.backup.localDbOnly=false
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_NodeProperties_RemoteDatabase()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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
            SyspropUtil.setProperty( "com.l7tech.config.backup.localDbOnly", Boolean.toString( false ) );
            final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
            Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
        }finally{
            SyspropUtil.clearProperty( "com.l7tech.config.backup.localDbOnly" );
        }
    }

    /**
     * Test the restore capability with a Buzzcut image, db info comes from the command line
     */
    @Test
    public void testImportBuzzcutOIntoBuzzcut_DbCommandLIne()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
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

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test migrating a buzzcut image with mappings. Make sure the db exists before running this test
     */
    @Test
    public void testMigrateBuzzcutIntoBuzzcut_DbExistsAndMappings()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL buzzcutImage = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test that when the exclude tables happens, we don't try and restore the license if cluster_properties was
     * not recreated
     */
    @Test
    public void testMigrateBuzzcutIntoBuzzcut_ClusterPropertiesExcluded()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
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

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final URL mappingFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/mapping_buzzcut.xml");

        final String [] args = new String[]{"import",
                "-image", buzzcutImage.getPath(),
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-mapping", mappingFile.getPath()
        };

        final String [] convertedArgs = MigrateToRestoreConvertor.getConvertedArguments(args, null);
        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(convertedArgs);
        if(result.getException() != null) throw result.getException();

        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test creating the new db and loading the image
     */
    @Test
    public void testMigrateFiveOIntoBuzzcut_DbNoExist()
            throws Exception {

        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");
        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test loading mapping after a migrate. The target database must exist
     */
    @Test
    public void testMigrateFiveOIntoBuzzcut_Mappings()
            throws Exception {

        final URL preFiveOZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/fiveo_backup_with_audits.zip");
        final URL mappingFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/mapping.xml");

        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
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

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    //==Testing node.properties and how it's managed==

    /**
     * Test when the targets node.properties and omp.dat are used because they are not in the backup image
     * and no db info is supplied on the command line
     * @throws Exception
     */
    @Test
    public void testDbRestoreNoConfigFolder() throws Exception{
        setUpEnvironment();
        final URL buzzCutZipNoConfig = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/buzzcut_db_noconfig.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzCutZipNoConfig.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test when the node.properties and omp.dat from the image are used
     * and no db info is supplied on the command line
     * @throws Exception
     */
    @Test
    public void testDbRestoreConfigFolder() throws Exception{
        final URL buzzCutZipNoConfig = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzCutZipNoConfig.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test when the node.properties and omp.dat from the image are used
     * and no db info is supplied on the command line
     * @throws Exception
     */
    @Test
    public void testDbRestoreConfigFolder_NoOmpDat() throws Exception{
        setUpEnvironment();
        final URL buzzCutZipNoConfig = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_omp_missing.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzCutZipNoConfig.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-v",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test when the node.properties and omp.dat from the image are used and updated with command line arguments
     *
     * The zip used should have incorrect database configuration so that we know the command line got merged
     * @throws Exception
     */
    @Test
    public void testDbRestoreConfigFolder_MergeWithCommandLine() throws Exception{
        setUpEnvironment();
        final URL buzzCutZipNoConfig = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_invalid_node_prop.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzCutZipNoConfig.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-v",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());

        //Read node.properties from the ssgHome and confirm the database users property has been corrected
        final PropertiesConfiguration nodeProps = new PropertiesConfiguration();
        nodeProps.setAutoSave(false);
        nodeProps.setListDelimiter((char) 0);
        nodeProps.load(new File(tmpSsgHome + File.separator + ImportExportUtilities.NODE_CONF_DIR, ImportExportUtilities.NODE_PROPERTIES));

        final String databaseUser = nodeProps.getString("node.db.config.main.user");
        Assert.assertEquals("node.properties was not correctly merged", "gateway", databaseUser);
    }

    /**
     * Test that when no config is used (selective restore below, same when image doesn't contain the config folder)
     * and all params are supplied on the command line
     * @throws Exception
     */
    @Test
    public void testDbRestoreNoConfigFolder_CommandLine() throws Exception{
        setUpEnvironment();
        final URL buzzCutZipNoConfig = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", buzzCutZipNoConfig.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-maindb",
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-v",
                "-halt"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    /**
     * Test the restore of an image with -migrate and all command line args supplied
     * @throws Exception
     */
    @Test
    public void testDbRestore_Migrate() throws Exception{
        setUpEnvironment();
        final URL imageZip = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/image_buzzcut_with_audits.zip");
        final Importer importer = new Importer(tmpSecureSpanHome, System.out);
        final String [] args = new String[]{"import",
                "-image", imageZip.getPath(),
                "-dbu", "root",
                "-dbp", "7layer",
                "-db", "ssg_buzzcut",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "localhost",
                "-cp", "111111",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-v",
                "-halt",
                "-migrate"
        };

        final ImportExportUtilities.UtilityResult result = importer.restoreOrMigrateBackupImage(args);
        if(result.getException() != null) throw result.getException();
        Assert.assertEquals("Incorrect result found", ImportExportUtilities.UtilityResult.Status.SUCCESS, result.getStatus());
    }

    private void setUpEnvironment() throws IOException {
        //Copy ssg.xml for test
        final File projectSqlFile = new File("etc/db/liquibase/ssg.xml");
        if(!projectSqlFile.exists() || projectSqlFile.isDirectory()) throw new RuntimeException("Cannot run without ssg.xml");

        final File sqlDir = new File(tmpSsgHome, ImportExportUtilities.getDirPart(ImportExportUtilities.SSG_DB_XML));
        FileUtils.ensurePath(sqlDir);
        final File ssgSql = new File(sqlDir, ImportExportUtilities.getFilePart(ImportExportUtilities.SSG_DB_XML));
        ssgSql.createNewFile();

        FileUtils.copyFile(projectSqlFile, ssgSql);

        final URL ompFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/omp.dat");
        final URL nodePropFile = this.getClass().getClassLoader().getResource("com/l7tech/skunkworks/backuprestore/node.properties");

        final File confDir = new File(tmpSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        FileUtils.ensurePath(confDir);
        FileUtils.copyFile(new File(ompFile.getPath()), new File(confDir, ImportExportUtilities.OMP_DAT));
        FileUtils.copyFile(new File(nodePropFile.getPath()), new File(confDir, ImportExportUtilities.NODE_PROPERTIES));

        final URL runtimeRes = this.getClass().getClassLoader().getResource("Gateway/runtime");
        final File runtimeSrc = new File(runtimeRes.getPath());
        final File runtimeDest = new File(tmpSsgHome, "runtime");

        ImportExportUtilities.copyDir(runtimeSrc, runtimeDest);
        
    }
}
