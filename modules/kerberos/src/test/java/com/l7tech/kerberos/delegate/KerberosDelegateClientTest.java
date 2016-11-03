package com.l7tech.kerberos.delegate;

import com.l7tech.kerberos.*;
import com.l7tech.util.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;

import javax.security.auth.kerberos.KerberosTicket;
import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class KerberosDelegateClientTest {
    
    private static File tmpDir;
    private static String tmpPath;

    private static KerberosDelegateClient client;

    /**
     * TGT object captured by real KDC Connection
     */
    private static final String KERBEROS_TGT = "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";

    @BeforeClass
    public static void setup() throws IOException, KerberosException {
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, true);
        tmpPath = tmpDir.getPath();

        System.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, tmpPath);
        //http/ssg5.l7tech.dev@L7TECH.DEV
        //use "base64" to encode
        //String keytab = "BQIAAABCAAIACkw3VEVDSC5ERVYABGh0dHAAD3NzZzUubDd0ZWNoLmRldgAAAAAAAAAAAwAXABCRqKd12yDxqQHeS3wzsafD";
        //For rf.lilly.com
        //String keytab = "BQIAAABFAAIADFJGLkxJTExZLkNPTQAEaHR0cAAQc3NnLnJmLmxpbGx5LmNvbQAAAAAAAAAAAwAXABCRqKd12yDxqQHeS3wzsafD";

        //FOR KPMG
        String keytab = "BQIAAABMAAIAD0tXT1JMRC5LUE1HLkNPTQAEaHR0cAAUc3NnMS5rd29ybGQua3BtZy5jb20AAAAAAAAAAAYAFwAQkainddsg8akB3kt8M7Gnww==";

        //FOR ELI LILLY
        //String keytab = "BQIAAABEAAIAC0hSLktQTUcuQ09NAARodHRwABBzc2cxLmhyLmtwbWcuY29tAAAAAAAAAAAEABcAEJGop3XbIPGpAd5LfDOxp8M=";
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null, false);
        MockKrb5LoginModule.setKeyTabBytes(keytab);
        MockKrb5LoginModule.setUsesRainier(false);
        MockKrb5LoginModule.setKerberosTicket(KERBEROS_TGT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MockKrb5LoginModule.setKeyTabBytes(null);
        MockKrb5LoginModule.setKerberosTicket(null);
    }

    /**
     * Setup for mock test
     * @throws IOException
     */
    public static void setupMock(final String  realm) throws IOException {
        client = new KerberosDelegateClient() {
            private String defaultRealm = realm;
            @Override
            protected KerberosTicket getS4U2SelfTicket(KerberosTicket tgt, String servicePrincipal, String user , String userRealm) throws KrbException, IOException {
                //return a fake service ticket
                return tgt;
            }
            @Override
            protected KerberosTicket getS4U2ProxyTicket(KerberosTicket tgt, String servicePrincipalName, PrincipalName clientPrincipal, Object o) throws KrbException, IOException {
                return tgt;
            }
            @Override
            public PrincipalName getPrincipalName(String user, String userRealm) throws RealmException {
                PrincipalName clientPrincipalName;
                if (userRealm != null) {
                    clientPrincipalName = new PrincipalName(user, userRealm);
                } else {
                    clientPrincipalName = new PrincipalName(user, defaultRealm);
                }
                return clientPrincipalName;
            }
        };
        KerberosTestSetup.setUpLoginConfig(tmpDir);
    }

    @AfterClass
    public static void dispose() {
        FileUtils.deleteDir(tmpDir);
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2Self() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        //KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        //KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket("http/ssg.rf.lilly.com@RF.LILLY.COM", "test");
        KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket("http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "kerb");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testReferralWithKeyTab() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithReferral("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "awitrisna", "US.KWORLD.KPMG.COM", 1);
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testCacheReferralWithKeyTab() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithReferral("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "awitrisna", "US.KWORLD.KPMG.COM", 1);
        assertNotNull(serviceTicket.getGSSAPReqTicket());
        KerberosDelegateClient client2 = new KerberosDelegateClient();
        client2.getKerberosProxyServiceTicketWithReferral("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "awitrisna", "US.KWORLD.KPMG.COM", 1);
    }

    @Ignore("Require KDC Connection")
    @Test
    public void test2ReferralWithKeyTab() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithReferral("http/kpmg-ca.hr.kpmg.com@HR.KPMG.COM", "http/ssg1.hr.kpmg.com@HR.KPMG.COM", "awitrisna", "US.KWORLD.KPMG.COM", 5);
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }


    @Ignore("Require KDC Connection")
    @Test
    public void testReferralUserPassword() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithReferral("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "ssg1", "7layer]", "awitrisna", "US.KWORLD.KPMG.COM", 1);
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2SelfMock() throws Exception {
        setupMock("L7TECH.DEV");
        KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2Proxy() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithKeytab("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "kerb", "KWORLD.KPMG.COM");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2ProxyMock() throws Exception {
        setupMock("L7TECH.DEV");
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithKeytab("http/test2008.l7tech.dev@L7TECH.DEV", "http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna", "L7TECH.DEV");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2Proxy2() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket s4uSelfServiceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");

        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", new PrincipalName("awitrisna@L7TECH.DEV"), "http/ssg5.l7tech.dev@L7TECH.DEV", s4uSelfServiceTicket.getDelegatedKerberosTicket());
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2Proxy2Mock() throws Exception {
        setupMock("L7TECH.DEV");
        KerberosServiceTicket s4uSelfServiceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");

        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", new PrincipalName("awitrisna@L7TECH.DEV"), "http/ssg5.l7tech.dev@L7TECH.DEV", s4uSelfServiceTicket.getDelegatedKerberosTicket());
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2ProxyWithUserNamePassword() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicketWithCredentials("http/test2008.l7tech.dev@L7TECH.DEV", "ssg5test@L7TECH.DEV", "7layer]", "awitrisna", "L7TECH.DEV");
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2ProxyWithUserNamePasswordMock() throws Exception {
        setupMock("L7TECH.DEV");
        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicketWithCredentials("http/test2008.l7tech.dev@L7TECH.DEV", "ssg5test@L7TECH.DEV", "7layer]", "awitrisna", "L7TECH.DEV");
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }
    
}
