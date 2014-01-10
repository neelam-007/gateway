package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests the ImportExportUtilitiesTest utility functions.
 * Any test which sets a system property should unset it in a finally block, so that it doesn't cause other tests
 * to fail
 */
public class ImportExportUtilitiesTest {

    @Before
    public void setUp() throws IOException {
        SyspropUtil.setProperty( "com.l7tech.util.buildVersion", "5.1.0" );
    }

    @After
    public void tearDown(){
        SyspropUtil.clearProperties( "com.l7tech.util.buildVersion", ImportExportUtilities.BASE_DIR_PROPERTY );
    }

    /**
     * Test that absolute paths are resolved correctly according to the system property ImportExportUtilities.BASE_DIR_PROPERTY
     */
    @Test
    public void testGetAbsolutePath(){
        try{
            String tmpDir = SyspropUtil.getProperty( "java.io.tmpdir" );
            SyspropUtil.setProperty( ImportExportUtilities.BASE_DIR_PROPERTY, tmpDir );

            String testFile = "testfile.txt";
            String absFile = ImportExportUtilities.getAbsolutePath(testFile);
            Assert.assertTrue("absFile's path should be '"+tmpDir+File.separator+testFile,
                    absFile.equals(tmpDir+File.separator+testFile));
        } finally {
            SyspropUtil.clearProperty( ImportExportUtilities.BASE_DIR_PROPERTY );
        }
    }

    /**
     * Tests that a runtime exception occurs when the ImportExportUtilities.BASE_DIR_PROPERTY is not set
     */
    @Test(expected = RuntimeException.class)
    public void testGetAbsolutePath_NoSystemProperty(){
        ImportExportUtilities.getAbsolutePath("notimpotant");
    }

    /**
     * Tests isHostLocal with a local and non existent hostname
     */
    @Test
    public void testisHostLocal(){
        String host = "localhost";
        Assert.assertTrue("localhost is local", ImportExportUtilities.isHostLocal(host));

        host = "doesnotexist";
        Assert.assertFalse("doesnotexist is not local", ImportExportUtilities.isHostLocal(host));
    }

    /**
     * Test that getNodeConfig() correctly constructs a DatabaseConfig, based on our project's node.properties and
     * omp.dat
     * @throws IOException
     */
    @Test
    public void testGetNodeConfig() throws IOException {

        final URL nodeRes = this.getClass().getClassLoader().getResource("Gateway/node/default/etc/conf/node.properties");
        File nodeFile = new File(nodeRes.getPath());
        final URL ompRes = this.getClass().getClassLoader().getResource("Gateway/node/default/etc/conf/omp.dat");
        File ompFile = new File(ompRes.getPath());

        DatabaseConfig dbConfig = ImportExportUtilities.getDatabaseConfig(nodeFile, ompFile);

        Assert.assertNotNull("dbConfig should not be null", dbConfig);
        Assert.assertEquals("Invalid value returned from " +
                "dbConfig.getHost()", "doesnotexist.l7tech.com", dbConfig.getHost());
        Assert.assertEquals("Invalid value returned from " +
                "dbConfig.getPort()", 3306, dbConfig.getPort());
        Assert.assertEquals("Invalid value returned from " +
                "dbConfig.getName()", "ssg_buzzcut", dbConfig.getName());
        Assert.assertEquals("Invalid value returned from " +
                "dbConfig.getNodeUsername()", "gateway", dbConfig.getNodeUsername());
    }

    /**
     * Test that getAndValidateCommandLineOptions() correctly validates command line arguments
     */
    @Test
    public void testGetAndValidateCommandLineOptions() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final String [] args = new String[]{"export","-image", "image.zip", "-ia", "-it", "mapping.xml"};
        final List<CommandLineOption> validOptions = new ArrayList<CommandLineOption>();
        validOptions.add(Exporter.IMAGE_PATH);
        validOptions.add(Exporter.IA_AUDIT_FLAG);
        validOptions.add(Exporter.MAPPING_PATH);

        final Map<String, String> params = ImportExportUtilities.getAndValidateCommandLineOptions(
                args, validOptions, Collections.<CommandLineOption>emptyList(), false, null);
        Assert.assertEquals("-image option has an incorrect value", "image.zip", params.get(Exporter.IMAGE_PATH.getName()));
    }

    @Test
    public void testGetAndValidateSingleArgument() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final String [] args = new String[]{"export","-image", "image.zip"};
        final List<CommandLineOption> validOptions = new ArrayList<CommandLineOption>();
        validOptions.add(Exporter.IMAGE_PATH);

        final String imageName = ImportExportUtilities.getAndValidateSingleArgument(
                args, Importer.IMAGE_PATH, validOptions, Collections.<CommandLineOption>emptyList());

        Assert.assertEquals("-image option has an incorrect value", "image.zip", imageName);        
    }
    /**
     * Test that getAndValidateCommandLineOptions() correctly validates command line arguments
     * In this test the -image command line option has no value supplied
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testGetAndValidateCommandLineOptions_MissingValue() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final String [] args = new String[]{"export","-image", "-ia", "-it", "mapping.xml"};
        final List<CommandLineOption> validOptions = new ArrayList<CommandLineOption>();
        validOptions.add(Exporter.IMAGE_PATH);
        validOptions.add(Exporter.IA_AUDIT_FLAG);
        validOptions.add(Exporter.MAPPING_PATH);

        ImportExportUtilities.getAndValidateCommandLineOptions(args, validOptions,
                Collections.<CommandLineOption>emptyList(), false, null);
    }

    /**
     * Test that getAndValidateCommandLineOptions() correctly validates command line arguments
     * In this test the -unknown command line option is supplied, as it's not expected and exception should be thrown
     */
    @Test(expected = BackupRestoreLauncher.InvalidProgramArgumentException.class)
    public void testGetAndValidateCommandLineOptions_UnknownValue() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final String [] args = new String[]{"export","-image", "image.zip", "-ia", "-it", "mapping.xml", "-unknown"};
        final List<CommandLineOption> validOptions = new ArrayList<CommandLineOption>();
        validOptions.add(Exporter.IMAGE_PATH);
        validOptions.add(Exporter.IA_AUDIT_FLAG);
        validOptions.add(Exporter.MAPPING_PATH);

        ImportExportUtilities.getAndValidateCommandLineOptions(args, validOptions,
                Collections.<CommandLineOption>emptyList(), false, null);
    }

    /**
     * Test that getAndValidateCommandLineOptions() correctly validates command line arguments
     * Tests that ignore options are processed correctly. In this test -it is supplied but is listed with the ignore
     * options instead of the valid options, so it should be ignored, if it wasn't listed as ignored, then an exception
     * should be thrown
     */
    @Test
    public void testGetAndValidateCommandLineOptions_IgnoreOptions() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        final String [] args = new String[]{"export","-image", "image.zip", "-ia", "-it", "mapping.xml", "-p"};
        final List<CommandLineOption> validOptions = new ArrayList<CommandLineOption>();
        validOptions.add(Exporter.IMAGE_PATH);
        validOptions.add(Exporter.IA_AUDIT_FLAG);

        final List<CommandLineOption> ignoreOptions = new ArrayList<CommandLineOption>();
        ignoreOptions.add(Exporter.MAPPING_PATH);
        ignoreOptions.add(new CommandLineOption("-p", "desc", false));

        ImportExportUtilities.getAndValidateCommandLineOptions(args, validOptions, ignoreOptions, false, null);
    }

    /**
     * Validate the method throwIfFileExists() which throws exceptions when a file exists
     */
    @Test
    public void testThrowIfFileExists() throws IOException {
        String tmpDir = SyspropUtil.getProperty( "java.io.tmpdir" );
        File testFile = new File(tmpDir + File.separator + "noexist.txt");
        ImportExportUtilities.throwIfFileExists(testFile.getAbsolutePath());
    }

    /**
     * Validate the method throwIfFileExists() which throws exceptions when a file exists
     * Tests that an IllegalArgumentException is thrown when the file already exists
     */
    @Test(expected = IllegalArgumentException.class)
    public void testThrowIfFileExists_FileExists() throws IOException {
        String tmpDir = SyspropUtil.getProperty( "java.io.tmpdir" );
        File testFile = new File(tmpDir + File.separator + "noexist.txt");
        try{
            testFile.createNewFile();
            ImportExportUtilities.throwIfFileExists(testFile.getAbsolutePath());
        }finally{
            testFile.delete();
        }
    }

    /**
     * Tests verifyCanWriteFile correctly validates that we can write to a file
     * @throws IOException
     */
    @Test
    public void testVerifyCanWriteFile() throws IOException {
        String tmpDir = SyspropUtil.getProperty( "java.io.tmpdir" );
        File f = new File(tmpDir, "testfilesdoesnotexist.txt");
        ImportExportUtilities.verifyCanWriteFile(f.getAbsolutePath());
    }

    /**
     * Tests verifyCanWriteFile correctly validates that we can write to a file. In this case we cant 
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testVerifyCanWriteFile_CantWrite() throws IOException {
        File f = new File("/gobbitygooky/testfilesdoesnotexist.txt");
        ImportExportUtilities.verifyCanWriteFile(f.getAbsolutePath());
    }

    /**
     * Test that temp directories are created correctly
     */
    @Test
    public void testcreateTmpDirectory() throws IOException {
        String tmpDir = null;
        try {
            tmpDir = ImportExportUtilities.createTmpDirectory();
            File test = new File(tmpDir);
            Assert.assertTrue("Temp directory should exist", test.exists());
            Assert.assertTrue("Temp directory should be a diretory", test.isDirectory());
        } finally{
            if(tmpDir != null) new File(tmpDir).delete();            
        }
    }

    /**
     * Tests that the file part is extacted correctly
     */
    @Test
    public void testGetFilePart(){
        String fileName = "/home/layer7/file.txt";
        String test = ImportExportUtilities.getFilePart(fileName);
        Assert.assertEquals("Incorrect file name extracted", "file.txt", test);

        fileName = "/home/";
        test = ImportExportUtilities.getFilePart(fileName);
        Assert.assertEquals("Incorrect file name extracted", "", test);
    }

    /**
     * Tests that the file part is extacted correctly
     */
    @Test
    public void testGetDirPart(){
        String fileName = "/home/layer7/file.txt";
        String test = ImportExportUtilities.getDirPart(fileName);
        Assert.assertEquals("Incorrect path extracted", "/home/layer7", test);

        fileName = "/home/";
        test = ImportExportUtilities.getDirPart(fileName);
        Assert.assertEquals("Incorrect path extracted", "/home", test);

        fileName = "filename";
        test = ImportExportUtilities.getDirPart(fileName);
        Assert.assertNull("Path should be null", test);

    }

    //getNodeConfig

    @Test
    public void testDeleteAfter(){
        File f = new File("/noexist/doesnotexist");
        System.out.println(f.getName());
    }

    /**
     * Test logic used to extract port number from a host name
     */
    @Test
    public void testExtractPortNumber() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        String host = "donal.l7tech.com";
        Assert.assertEquals("Invalid host name found", "donal.l7tech.com", (ImportExportUtilities.getDbHostAndPortPair(host)).left);
        Assert.assertEquals("Invalid port number found", "3306", (ImportExportUtilities.getDbHostAndPortPair(host)).right);

        host = "donal.l7tech.com:4567";
        Assert.assertEquals("Invalid host name found", "donal.l7tech.com", (ImportExportUtilities.getDbHostAndPortPair(host)).left);
        Assert.assertEquals("Invalid port number found", "4567", (ImportExportUtilities.getDbHostAndPortPair(host)).right);
    }

    @Test
    public void testGetDirpart(){
        final String image = "image1.zip";
        Assert.assertNull("Dir part should be null", ImportExportUtilities.getDirPart(image));
    }

    @Test
    public void testParseConfigFile() throws IOException {

        final URL auditRes = this.getClass().getClassLoader().getResource("Gateway/config/backup/cfg/backup_tables_audit");
        File auditTableFile = new File(auditRes.getPath());
        List<String> auditTables = ImportExportUtilities.processFile(auditTableFile);
        Assert.assertNotNull("auditTables should not be null", auditTables);
        Assert.assertFalse("auditTables should not be empty", auditTables.isEmpty());

        Assert.assertTrue("6 lines should have been found", auditTables.size() == 6);

        for(int i =0; i < auditTables.size(); i++){
            Assert.assertEquals("Invalid line found", "audit_table_" + (i+1), auditTables.get(i));
        }
    }

}
