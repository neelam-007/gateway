package com.l7tech.kerberos;

import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.InetAddress;
import java.util.Calendar;

import static junit.framework.Assert.*;

public class KerberosConfigTest {

    private static long lastModified = Calendar.getInstance().getTime().getTime();
    public static final String DEFAULT_SERVICE_PRINCIPAL_NAME = "http/ssg1.qawin2003.com@QAWIN2003.COM";
    public static final String SERVICE_PRINCIPAL_NAME = "http/ssg3.l7tech.sup@L7TECH.SUP";
    public static final String SERVICE = "http";
    public static final String HOST = "ssg3.l7tech.sup";
    public static final String REALM = "L7TECH.SUP";
    private File tmpDir;

    /*
     http/ssg1.qawin2003.com@QAWIN2003.COM
     http/ssg3.l7tech.sup@L7TECH.SUP
     http/ssg4.sup.l7tech.sup@SUP.L7TECH.SUP
    */
    public static String MULTIPLE_PRINCIPAL_KEYTAB = "BQIAAABIAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEnNzZzEucWF3aW4yMDAzLmNvbQAAAABPu/TkAgAXABCIRvfq7o+xF60Gvdgwt1hsAAAAQgACAApMN1RFQ0guU1VQAARodHRwAA9zc2czLmw3dGVjaC5zdXAAAAAAT7v05AQAFwAQjD78SGcE0u5x7r5xrxTYbAAAAEoAAgAOU1VQLkw3VEVDSC5TVVAABGh0dHAAE3NzZzQuc3VwLmw3dGVjaC5zdXAAAAAAT7v05AMAFwAQkainddsg8akB3kt8M7Gnww==";

    /*
     http/ssg1.qawin2003.com@QAWIN2003.COM
     */
    public static String SINGLE_PRINCIPAL_KEYTAB = "BQIAAABIAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEnNzZzEucWF3aW4yMDAzLmNvbQAAAAAAAAAAAwAXABCIRvfq7o+xF60Gvdgwt1hs";

    /*
     http/ssg1.qawin2003.com@QAWIN2003.COM
     http/ssg2.qawin2003.com@QAWIN2003.COM
     http/ssg3.l7tech.sup@L7TECH.SUP
     http/ssg2.qawin2003.com@QAWIN2003.COM
     */
    private static String DUPLICATE_PRINCIPAL_KEYTAB = "BQIAAABIAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEnNzZzEucWF3aW4yMDAzLmNvbQAAAABPvB+9AgAXABCIRvfq7o+xF60Gvdgwt1hsAAAASAACAA1RQVdJTjIwMDMuQ09NAARodHRwABJzc2cyLnFhd2luMjAwMy5jb20AAAAAT7wfvQIAFwAQiEb36u6PsRetBr3YMLdYbAAAAEIAAgAKTDdURUNILlNVUAAEaHR0cAAPc3NnMy5sN3RlY2guc3VwAAAAAE+8H70EABcAEIw+/EhnBNLuce6+ca8U2GwAAABIAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEnNzZzIucWF3aW4yMDAzLmNvbQAAAABPvB+9AgAXABCIRvfq7o+xF60Gvdgwt1hs";

    public static String INVALID_KEYTAB = "BQIAAABIAAIADVFBV0lOYWRmYWQyMDAzLkNPTQAEaHR0cAASc3NnMS5xYXdpbjIwMDMuY29tAABhZGZhAABPsVccAgAXABCIRvfq7o+xF60Gvdgwt1hsAAAASAACAA1RQVdJTjIwMDMuQ09NAARodHRwABJzc2cyLnFhd2luMjAwMy5jb20AAAAAT7FXHAIAFwAQiEb36u6PsRetBr3YMLdYbAAAAEIAAgAKTDdURUNILlNVUAAEaHR0cAAPc3NnMy5sYWRmYWRmN3RlY2guc3VwAAAAAE+xVxwEABcAEIw+/EhnBNLuce6+ca8U2GwK";

    /*
     http/ssg1.qawin2003.com@INVALID.COM
     */
    private static String INVALID_REALM = "BQIAAABKAAIAD0FTREZHSFpYQ1ZCLkNPTQAEaHR0cAASc3NnMS5xYXdpbjIwMDMuY29tAAAAAAAAAAACABcAEJGop3XbIPGpAd5LfDOxp8M=";


    /*
     http/ssg1.qawin2003.com@QAWIN2003.COM
     http/ssg1.qawin2003.com@ASDFGHZXCVB.COM
     */
    private static String INVALID_REALMS = "BQIAAABIAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEnNzZzIucWF3aW4yMDAzLmNvbQAAAABPvT99AgAXABCIRvfq7o+xF60Gvdgwt1hsAAAASgACAA9BU0RGR0haWENWQi5DT00ABGh0dHAAEnNzZzEucWF3aW4yMDAzLmNvbQAAAABPvT99AgAXABCRqKd12yDxqQHeS3wzsafD";

    /**
     * Setup the Kerberos Client.
     * Do not run this setup for the test which required KDC Connection.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, true);
        SyspropUtil.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, tmpDir.getPath());
        KerberosUtils.inetAddress = InetAddress.getLocalHost();
        //set kerberos files to null so that the unit tests do not interfere with each other
        KerberosConfig.kerberosFiles = null;
    }


    @Ignore
    @Test
    public void generateKeyTab() throws IOException {
        File file = new File("/home/awitrisna/tmp/kerberos.ssg6test.keytab");
        System.out.println(file.length());
        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(data);
        System.out.println(Base64.encodeBase64String(data));
    }


    @Test
    public void testMultiplePrincipalKeyTab() throws IOException, KerberosException {
        writeKeyTab(MULTIPLE_PRINCIPAL_KEYTAB, true);
        assertKerberosFileExists();
        assertEquals("QAWIN2003.COM", KerberosConfig.getConfigRealm());
        assertNotNull(KerberosConfig.getKeytab(true));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal("http/ssg1.qawin2003.com"));
        assertEquals("http/ssg3.l7tech.sup@L7TECH.SUP", KerberosConfig.getKeytabPrincipal("http/ssg3.l7tech.sup"));
        assertEquals("http/ssg4.sup.l7tech.sup@SUP.L7TECH.SUP", KerberosConfig.getKeytabPrincipal("http/ssg4.sup.l7tech.sup"));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal("http/ssg1.qawin2003.com@QAWIN2003.COM"));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal("INVALID"));
        assertTrue(hasRealm("QAWIN2003.COM"));
        assertTrue(hasRealm("L7TECH.SUP"));
        assertTrue(hasRealm("SUP.L7TECH.SUP"));
    }

    @Test
    public void testConfigurePrincipal() throws IOException, KerberosException {
        writeKeyTab(MULTIPLE_PRINCIPAL_KEYTAB, false);
        KerberosConfig.checkConfig("10.0.0.1, 10.0.0.2", "LOCALHOST.COM", false, false);
        assertTrue(hasRealm("LOCALHOST.COM"));
    }


    @Test
    public void testSinglePrincipalKeyTab() throws IOException, KerberosException {
        writeKeyTab(SINGLE_PRINCIPAL_KEYTAB, true);
        assertKerberosFileExists();
        assertEquals("QAWIN2003.COM", KerberosConfig.getConfigRealm());
        assertNotNull(KerberosConfig.getKeytab(true));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal("http/ssg1.qawin2003.com"));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal("http/ssg1.qawin2003.com@QAWIN2003.COM"));
        assertEquals("http/ssg1.qawin2003.com@QAWIN2003.COM", KerberosConfig.getKeytabPrincipal(null));
    }

    @Test
    public void testDuplicatePrincipalKeyTab() throws IOException, KerberosException, InterruptedException {
        writeKeyTab(DUPLICATE_PRINCIPAL_KEYTAB, true);
        assertNull(KerberosConfig.getKeytabPrincipal(null));
    }

    @Test(expected = KerberosException.class)
    public void testDuplicatePrincipalKeyTabWithException() throws IOException, KerberosException {
        writeKeyTab(DUPLICATE_PRINCIPAL_KEYTAB, false);
        KerberosConfig.getKeytab(false);
    }


    @Test(expected = KerberosException.class)
    public void testInvalidKeyTab() throws IOException, KerberosException {
        writeKeyTab(INVALID_KEYTAB, false);
        KerberosConfig.getKeytab(false);
    }

    @Test
    public void testInvalidRealm() throws IOException, KerberosException {
        KerberosUtils.inetAddress = null;
        writeKeyTab(INVALID_REALM, true);
        File file = KerberosTestSetup.getKrb5Conf();
        assertFalse(file.exists());
    }

    @Test
    public void testInvalidRealms() throws IOException, KerberosException {
        KerberosUtils.inetAddress = null;
        writeKeyTab(INVALID_REALMS, true);
        File file = KerberosTestSetup.getKrb5Conf();
        assertFalse(file.exists());
    }


    private void writeKeyTab(String keyTabData, boolean checkConfig) throws IOException, KerberosException {
        File file = KerberosTestSetup.getKeyTab();
        if (file.exists()) {
            file.delete();
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(Base64.decodeBase64(keyTabData));
        } catch (IOException ioe) {
            throw new KerberosException("Error writing Kerberos keytab.", ioe);
        } finally {
            ResourceUtils.closeQuietly(out);
        }
        lastModified = lastModified + 1000;
        file.setLastModified(lastModified);
        if (checkConfig) {
            KerberosConfig.checkConfig(null, null, false, false);
        }
    }

    @After
    public void tearDown() throws KerberosException {
//        FileUtils.deleteDir(tmpDir);
        KerberosUtils.inetAddress = null;
    }

    public void assertKerberosFileExists() {
        File file = KerberosTestSetup.getKeyTab();
        assertTrue(file.exists());
        file = KerberosTestSetup.getLoginConfig();
        assertTrue(file.exists());
    }

    private boolean hasRealm(String realm) throws IOException {
        File file = KerberosTestSetup.getKrb5Conf();
        BufferedReader fr = null;
        try {
            fr = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = fr.readLine()) != null) {
                if (line.contains(realm)) {
                    return true;
                }
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
        return false;
    }

}
