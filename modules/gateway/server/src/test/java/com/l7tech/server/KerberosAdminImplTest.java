package com.l7tech.server;

import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.kerberos.*;
import com.l7tech.server.security.MasterPasswordManagerStub;
import com.l7tech.util.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.*;

import java.io.File;
import java.util.Map;

import static junit.framework.Assert.*;


public class KerberosAdminImplTest {

    private File tmpDir;
    private KerberosAdmin kerberosAdmin;
    private MockClusterPropertyManager clusterPropertyManager;

    /**
     * TGT object captured by real KDC Connection
     */
    private static final String KERBEROS_TGT = "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";


    @BeforeClass
    public static void init() throws Exception {
        MockKrb5LoginModule.setKeyTabBytes(KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB);
        MockKrb5LoginModule.setUsesRainier(false);
        MockKrb5LoginModule.setKerberosTicket(KERBEROS_TGT);
    }

    @AfterClass
    public static void dispose() {
        MockKrb5LoginModule.setKeyTabBytes(null);
        MockKrb5LoginModule.setKerberosTicket(null);
    }

    @Before
    public void setup() throws Exception {
        clusterPropertyManager = new MockClusterPropertyManager();
        kerberosAdmin = new KerberosAdminImpl(clusterPropertyManager,
                new MasterPasswordManagerStub("password"));
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, false);
        KerberosTestSetup.init(tmpDir);
        KerberosTestSetup.setUp(tmpDir);

    }

    @After
    public void tearDown() {
        FileUtils.deleteDir(tmpDir);
    }


    @Test
    public void testGetKeyTabEntryInfos() throws Exception {
        assertEquals(3, kerberosAdmin.getKeyTabEntryInfos().size());
    }

    @Test(expected = KerberosException.class)
    public void testGetKeyTabEntryInfosWithInvalidKeyTab() throws Exception {
        KerberosTestSetup.setupInvalidKeytab();
        kerberosAdmin.getKeyTabEntryInfos();
    }

    @Test
    public void testValidatePrincipal() throws Exception {
        kerberosAdmin.validatePrincipal(KerberosConfigTest.DEFAULT_SERVICE_PRINCIPAL_NAME);
    }

    @Test
    public void testGetConfiguration() throws Exception {
        Map<String, String> result = kerberosAdmin.getConfiguration();
        assertEquals("QAWIN2003.COM", result.get("realm"));
    }

    @Test
    public void testInstallKeytab() throws Exception {
        assertNull(clusterPropertyManager.getProperty("krb5.keytab"));
        kerberosAdmin.installKeytab(Base64.decodeBase64(KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB));
        assertNotNull(clusterPropertyManager.getProperty("krb5.keytab"));
    }

    @Test
    public void testDeleteKeytab() throws Exception {
        kerberosAdmin.installKeytab(Base64.decodeBase64(KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB));
        kerberosAdmin.deleteKeytab();
        assertNull(clusterPropertyManager.getProperty("krb5.keytab"));
    }

    @Test
    public void testDefaultGetKeytabValidate() throws Exception {
        assertTrue(kerberosAdmin.getKeytabValidate());
    }

    @Test
    public void testKeytabValidate() throws Exception {
        kerberosAdmin.setKeytabValidate(false);
        assertFalse(kerberosAdmin.getKeytabValidate());
        kerberosAdmin.setKeytabValidate(true);
        assertTrue(kerberosAdmin.getKeytabValidate());
    }

}
