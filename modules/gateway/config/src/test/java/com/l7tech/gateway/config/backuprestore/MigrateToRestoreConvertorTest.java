package com.l7tech.gateway.config.backuprestore;

import org.junit.Test;
import org.junit.Assert;

/**
 * Test the conversion of all known migrate command line arguments, to the new selective restore command
 * line arguments
 */
public class MigrateToRestoreConvertorTest {


    /**
     * Test the standard convert, where no -os, -newdb or -config specified
     * @throws Exception
     */
    @Test
    public void testStandardConvert() throws Exception {
        final String[] args = new String[]{
                "migrate",
                "-image", "image1.zip",
                "-db", "dbname",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "host.l7tech.com",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-cp", "111111"
        };

        final String [] converted = MigrateToRestoreConvertor.getConvertedArguments(args);

        Assert.assertEquals("-image should be at this index", "-image", converted[1]);
        Assert.assertEquals("image1.zip should be at this index", "image1.zip", converted[2]);
        Assert.assertEquals("-migrate should be at this index", "-migrate", converted[3]);
        Assert.assertEquals("-v should be at this index", "-v", converted[4]);
        Assert.assertEquals("-halt should be at this index", "-halt", converted[5]);

        Assert.assertEquals("-dbu should be at this index", "-dbu", converted[6]);
        Assert.assertEquals("root should be at this index", "root", converted[7]);
        Assert.assertEquals("-dbp should be at this index", "-dbp", converted[8]);
        Assert.assertEquals("root password should be at this index", "7layer", converted[9]);

        Assert.assertEquals("-dbh should be at this index", "-dbh", converted[10]);
        Assert.assertEquals("host name should be at this index", "host.l7tech.com", converted[11]);
        Assert.assertEquals("-gdbu should be at this index", "-gdbu", converted[12]);
        Assert.assertEquals("gateway user should be at this index", "gateway", converted[13]);
        Assert.assertEquals("-gdbp should be at this index", "-gdbp", converted[14]);
        Assert.assertEquals("gateway password should be at this index", "7layer", converted[15]);
        Assert.assertEquals("-cp should be at this index", "-cp", converted[16]);
        Assert.assertEquals("cluster passphrase should be at this index", "111111", converted[17]);

        Assert.assertEquals("-maindb should be at this index", "-maindb", converted[18]);
        Assert.assertEquals("-audits should be at this index", "-audits", converted[19]);

        Assert.assertEquals("-config should be at this index", "-config", converted[20]);
        //db params, always required
        Assert.assertEquals("-db should be at this index", "-db", converted[21]);
        Assert.assertEquals("dbname should be at this index", "dbname", converted[22]);
        
        Assert.assertEquals("Too many args converted", 23, converted.length);

    }

    /**
     * Test that no db components specified when -config is used
     * @throws Exception
     */
    @Test
    public void testConvertConfig() throws Exception {
        final String[] args = new String[]{
                "migrate",
                "-image", "image1.zip",
                "-db", "dbname",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "host.l7tech.com",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-cp", "111111",
                "-config"
        };

        final String [] converted = MigrateToRestoreConvertor.getConvertedArguments(args);

        Assert.assertEquals("-image should be at this index", "-image", converted[1]);
        Assert.assertEquals("image1.zip should be at this index", "image1.zip", converted[2]);
        Assert.assertEquals("-migrate should be at this index", "-migrate", converted[3]);
        Assert.assertEquals("-v should be at this index", "-v", converted[4]);
        Assert.assertEquals("-halt should be at this index", "-halt", converted[5]);

        Assert.assertEquals("-dbu should be at this index", "-dbu", converted[6]);
        Assert.assertEquals("root should be at this index", "root", converted[7]);
        Assert.assertEquals("-dbp should be at this index", "-dbp", converted[8]);
        Assert.assertEquals("root password should be at this index", "7layer", converted[9]);

        Assert.assertEquals("-dbh should be at this index", "-dbh", converted[10]);
        Assert.assertEquals("host name should be at this index", "host.l7tech.com", converted[11]);
        Assert.assertEquals("-gdbu should be at this index", "-gdbu", converted[12]);
        Assert.assertEquals("gateway user should be at this index", "gateway", converted[13]);
        Assert.assertEquals("-gdbp should be at this index", "-gdbp", converted[14]);
        Assert.assertEquals("gateway password should be at this index", "7layer", converted[15]);
        Assert.assertEquals("-cp should be at this index", "-cp", converted[16]);
        Assert.assertEquals("cluster passphrase should be at this index", "111111", converted[17]);

        Assert.assertEquals("-config should be at this index", "-config", converted[18]);
        //db params, always required
        Assert.assertEquals("-db should be at this index", "-db", converted[19]);
        Assert.assertEquals("dbname should be at this index", "dbname", converted[20]);

        Assert.assertEquals("Too many args converted", 21, converted.length);

    }

    /**
     * Test that the -os component option is translated correctly
     * @throws Exception
     */
    @Test
    public void testOSConvert() throws Exception {
        final String[] args = new String[]{
                "migrate",
                "-image", "image1.zip",
                "-db", "dbname",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "host.l7tech.com",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-cp", "111111",
                "-os"
        };

        final String [] converted = MigrateToRestoreConvertor.getConvertedArguments(args);

        Assert.assertEquals("-image should be at this index", "-image", converted[1]);
        Assert.assertEquals("image1.zip should be at this index", "image1.zip", converted[2]);
        Assert.assertEquals("-migrate should be at this index", "-migrate", converted[3]);
        Assert.assertEquals("-v should be at this index", "-v", converted[4]);
        Assert.assertEquals("-halt should be at this index", "-halt", converted[5]);

        Assert.assertEquals("-dbu should be at this index", "-dbu", converted[6]);
        Assert.assertEquals("root should be at this index", "root", converted[7]);
        Assert.assertEquals("-dbp should be at this index", "-dbp", converted[8]);
        Assert.assertEquals("root password should be at this index", "7layer", converted[9]);

        Assert.assertEquals("-dbh should be at this index", "-dbh", converted[10]);
        Assert.assertEquals("host name should be at this index", "host.l7tech.com", converted[11]);
        Assert.assertEquals("-gdbu should be at this index", "-gdbu", converted[12]);
        Assert.assertEquals("gateway user should be at this index", "gateway", converted[13]);
        Assert.assertEquals("-gdbp should be at this index", "-gdbp", converted[14]);
        Assert.assertEquals("gateway password should be at this index", "7layer", converted[15]);
        Assert.assertEquals("-cp should be at this index", "-cp", converted[16]);
        Assert.assertEquals("cluster passphrase should be at this index", "111111", converted[17]);

        Assert.assertEquals("-maindb should be at this index", "-maindb", converted[18]);
        Assert.assertEquals("-audits should be at this index", "-audits", converted[19]);

        Assert.assertEquals("-config should be at this index", "-config", converted[20]);

        Assert.assertEquals("-os should be at this index", "-os", converted[21]);

        Assert.assertEquals("-db should be at this index", "-db", converted[22]);
        Assert.assertEquals("dbname should be at this index", "dbname", converted[23]);

        Assert.assertEquals("Too many args converted", 24, converted.length);

    }

    /**
     * Test that the -newdb component option is translated correctly
     * @throws Exception
     */
    @Test
    public void testNewDbConvert() throws Exception {
        final String[] args = new String[]{
                "migrate",
                "-image", "image1.zip",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "host.l7tech.com",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-cp", "111111",
                "-newdb", "newdbname"
        };

        final String [] converted = MigrateToRestoreConvertor.getConvertedArguments(args);

        Assert.assertEquals("-image should be at this index", "-image", converted[1]);
        Assert.assertEquals("image1.zip should be at this index", "image1.zip", converted[2]);
        Assert.assertEquals("-migrate should be at this index", "-migrate", converted[3]);
        Assert.assertEquals("-v should be at this index", "-v", converted[4]);
        Assert.assertEquals("-halt should be at this index", "-halt", converted[5]);

        Assert.assertEquals("-dbu should be at this index", "-dbu", converted[6]);
        Assert.assertEquals("root should be at this index", "root", converted[7]);
        Assert.assertEquals("-dbp should be at this index", "-dbp", converted[8]);
        Assert.assertEquals("root password should be at this index", "7layer", converted[9]);

        Assert.assertEquals("-dbh should be at this index", "-dbh", converted[10]);
        Assert.assertEquals("host name should be at this index", "host.l7tech.com", converted[11]);
        Assert.assertEquals("-gdbu should be at this index", "-gdbu", converted[12]);
        Assert.assertEquals("gateway user should be at this index", "gateway", converted[13]);
        Assert.assertEquals("-gdbp should be at this index", "-gdbp", converted[14]);
        Assert.assertEquals("gateway password should be at this index", "7layer", converted[15]);
        Assert.assertEquals("-cp should be at this index", "-cp", converted[16]);
        Assert.assertEquals("cluster passphrase should be at this index", "111111", converted[17]);

        Assert.assertEquals("-maindb should be at this index", "-maindb", converted[18]);
        Assert.assertEquals("-audits should be at this index", "-audits", converted[19]);

        Assert.assertEquals("-config should be at this index", "-config", converted[20]);

        Assert.assertEquals("-newdb should be at this index", "-newdb", converted[21]);
        Assert.assertEquals("newdb name should be at this index", "newdbname", converted[22]);

        Assert.assertEquals("Too many args converted", 23, converted.length);

    }

    /**
     * Test that the -mapping option is translated correctly
     * @throws Exception
     */
    @Test
    public void testMappingFile() throws Exception {
        final String[] args = new String[]{
                "migrate",
                "-image", "image1.zip",
                "-dbu", "root",
                "-dbp", "7layer",
                "-dbh", "host.l7tech.com",
                "-gdbu", "gateway",
                "-gdbp", "7layer",
                "-cp", "111111",
                "-newdb", "newdbname",
                "-mapping", "mapping.xml"
        };

        final String [] converted = MigrateToRestoreConvertor.getConvertedArguments(args);

        Assert.assertEquals("-image should be at this index", "-image", converted[1]);
        Assert.assertEquals("image1.zip should be at this index", "image1.zip", converted[2]);
        Assert.assertEquals("-migrate should be at this index", "-migrate", converted[3]);
        Assert.assertEquals("-v should be at this index", "-v", converted[4]);
        Assert.assertEquals("-halt should be at this index", "-halt", converted[5]);

        Assert.assertEquals("-dbu should be at this index", "-dbu", converted[6]);
        Assert.assertEquals("root should be at this index", "root", converted[7]);
        Assert.assertEquals("-dbp should be at this index", "-dbp", converted[8]);
        Assert.assertEquals("root password should be at this index", "7layer", converted[9]);

        Assert.assertEquals("-dbh should be at this index", "-dbh", converted[10]);
        Assert.assertEquals("host name should be at this index", "host.l7tech.com", converted[11]);
        Assert.assertEquals("-gdbu should be at this index", "-gdbu", converted[12]);
        Assert.assertEquals("gateway user should be at this index", "gateway", converted[13]);
        Assert.assertEquals("-gdbp should be at this index", "-gdbp", converted[14]);
        Assert.assertEquals("gateway password should be at this index", "7layer", converted[15]);
        Assert.assertEquals("-cp should be at this index", "-cp", converted[16]);
        Assert.assertEquals("cluster passphrase should be at this index", "111111", converted[17]);

        Assert.assertEquals("-maindb should be at this index", "-maindb", converted[18]);
        Assert.assertEquals("-audits should be at this index", "-audits", converted[19]);

        Assert.assertEquals("-config should be at this index", "-config", converted[20]);

        Assert.assertEquals("-mapping should be at this index", "-mapping", converted[21]);
        Assert.assertEquals("mapping file name should be at this index", "mapping.xml", converted[22]);

        Assert.assertEquals("-newdb should be at this index", "-newdb", converted[23]);
        Assert.assertEquals("newdb name should be at this index", "newdbname", converted[24]);

        Assert.assertEquals("Too many args converted", 25, converted.length);

    }

}
