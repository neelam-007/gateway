package com.l7tech.kerberos;

import com.l7tech.kerberos.delegate.KerberosDelegateClient;
import com.l7tech.util.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sun.security.krb5.Credentials;
import sun.security.krb5.KrbException;
import sun.security.krb5.internal.Ticket;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class KerberosDelegateClientTest {
    
    private static File tmpDir;
    private static String tmpPath;

    private static KerberosDelegateClient client;


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
        String keytab = "BQIAAABMAAIAD0tXT1JMRC5LUE1HLkNPTQAEaHR0cAAUc3NnMS5rd29ybGQua3BtZy5jb20AAAAAAAAAAAMAFwAQkainddsg8akB3kt8M7Gnww==";
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null, false);

    }

    /**
     * Setup for mock test
     * @throws IOException
     */
    public static void setupMock() throws IOException {
        client = new KerberosDelegateClient() {
            @Override
            protected Credentials getS4U2SelfCred(Credentials tgt, String servicePrincipal, String user , String userRealm) throws KrbException, IOException {
                //return a fake service ticket
                return tgt;
            }

            @Override
            protected Credentials getS4U2ProxyCred(Credentials tgt, String servicePrincipalName, Ticket serviceTicket) throws KrbException, IOException {
                //return a fake service ticket
                return tgt;
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
    public void testReferralUserPassword() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicketWithReferral("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "ssg1", "7layer]", "awitrisna", "US.KWORLD.KPMG.COM", 1);
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2SelfMock() throws Exception {
        setupMock();
        KerberosServiceTicket serviceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2Proxy() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicket("http/kpmg2.kworld.kpmg.com@KWORLD.KPMG.COM", "http/ssg1.kworld.kpmg.com@KWORLD.KPMG.COM", "kerb");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2ProxyMock() throws Exception {
        setupMock();
        KerberosServiceTicket serviceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", "http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        assertNotNull(serviceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2Proxy2() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket s4uSelfServiceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        Ticket s4u2SelfTicket = new Ticket(s4uSelfServiceTicket.getDelegatedKerberosTicket().getEncoded());

        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", "http/ssg5.l7tech.dev@L7TECH.DEV", s4u2SelfTicket);
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2Proxy2Mock() throws Exception {
        setupMock();
        KerberosServiceTicket s4uSelfServiceTicket = client.getKerberosSelfServiceTicket("http/ssg5.l7tech.dev@L7TECH.DEV", "awitrisna");
        Ticket s4u2SelfTicket = new Ticket(s4uSelfServiceTicket.getDelegatedKerberosTicket().getEncoded());

        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", "http/ssg5.l7tech.dev@L7TECH.DEV", s4u2SelfTicket);
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Ignore("Require KDC Connection")
    @Test
    public void testS4U2ProxyWithUserNamePassword() throws Exception {
        KerberosDelegateClient client = new KerberosDelegateClient();
        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", "ssg5test@L7TECH.DEV", "7layer]", "awitrisna");
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }

    @Test
    public void testS4U2ProxyWithUserNamePasswordMock() throws Exception {
        setupMock();
        KerberosServiceTicket s4u2ProxyServiceTicket = client.getKerberosProxyServiceTicket("http/test2008.l7tech.dev@L7TECH.DEV", "ssg5test@L7TECH.DEV", "7layer]", "awitrisna");
        assertNotNull(s4u2ProxyServiceTicket.getGSSAPReqTicket());
    }
    
}
