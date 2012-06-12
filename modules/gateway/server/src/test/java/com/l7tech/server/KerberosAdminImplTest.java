package com.l7tech.server;

import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.kerberos.KerberosConfigTest;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosTestSetup;
import com.l7tech.server.security.MasterPasswordManagerStub;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.*;


public class KerberosAdminImplTest {

    private KerberosAdmin kerberosAdmin;
    private MockClusterPropertyManager clusterPropertyManager;

    @Before
    public void setup() throws Exception {
        clusterPropertyManager = new MockClusterPropertyManager();
        kerberosAdmin = new KerberosAdminImpl(clusterPropertyManager,
                new MasterPasswordManagerStub("password"));
        KerberosTestSetup.init();
        KerberosTestSetup.setUp();

    }

    @After
    public void tearDown() {
        KerberosTestSetup.dispose();
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
}
