package com.l7tech.kerberos;

import com.l7tech.util.FileUtils;
import com.l7tech.util.TestTimeSource;
import com.l7tech.util.TimeSource;
import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.*;
import org.junit.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.login.LoginContext;
import java.io.*;
import java.net.InetAddress;
import java.security.PrivilegedExceptionAction;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * User: awitrisna
 */
public class KerberosClientTest {

    private static File tmpDir;

    private static final String LOGIN_CONTEXT_OUT_KEYTAB = "com.l7tech.common.security.kerberos.outbound.keytab";
    //private static final String SERVICE_PRINCIPAL_NAME = "http/ssg1.qawin2003.com@QAWIN2003.COM";
    //private static final String SERVICE_PRINCIPAL_NAME = "http/ssg2.qawin2003.com@QAWIN2003.COM";
    //private static final String SERVICE_PRINCIPAL_NAME = "http/ssg4.sup.l7tech.sup@SUP.L7TECH.SUP";
    private static final String DEFAULT_SERVICE_PRINCIPAL_NAME = KerberosConfigTest.DEFAULT_SERVICE_PRINCIPAL_NAME;
    private static final String SERVICE_PRINCIPAL_NAME = KerberosConfigTest.SERVICE_PRINCIPAL_NAME;
    public static final String SERVICE = KerberosConfigTest.SERVICE;
    public static final String HOST = KerberosConfigTest.HOST;

    private static final String SIMPLE_SERVICE_PRINCIPAL_NAME = "http/ssg3.l7tech.sup";
    private static TimeSource TIME_SOURCE;

    /**
     * TGT object captured by real KDC Connection
     */
    private static final String KERBEROS_TGT = "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";

    private static final String AES256_PRINCIPAL_NAME = "http/rainier.redmond.local@REDMOND.LOCAL";
    private static final String AES256_KEYTAB = "BQIAAABbAAIADVJFRE1PTkQuTE9DQUwABGh0dHAAFXdvbmNoMTRtYWMxMDQ4LmNhLmNvbQAAAAAAAAAACwASACChMPc41UwbopaTaM/cn17IsCzw+G46+3M1gi/4r9Tfag==";
    private static final String AES256_TGT = "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";
    public KerberosClient client;


    @Before
    public void init() throws IOException, KerberosException {
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, true);
        MockKrb5LoginModule.setKeyTabBytes(KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB);
        MockKrb5LoginModule.setUsesRainier(false);
        MockKrb5LoginModule.setKerberosTicket(KERBEROS_TGT);
        KerberosTestSetup.init(tmpDir);
        TIME_SOURCE = new TestTimeSource();
    }

    @After
    public void dispose() {
        //FileUtils.deleteDir(tmpDir);
        MockKrb5LoginModule.setKeyTabBytes(null);
        MockKrb5LoginModule.setKerberosTicket(null);
    }

    /**
     * Setup the Kerberos Client.
     * Do not run this setup for the test which required KDC Connection.
     *
     * @throws Exception
     */
    public void setUp() throws Exception {
        KerberosTestSetup.setUp(tmpDir);
        client = KerberosTestSetup.client;
    }

    @Test
    public void testAES256TicketDecryption() throws Exception {
        MockKrb5LoginModule.setKeyTabBytes(AES256_KEYTAB);
        MockKrb5LoginModule.setKerberosTicket(AES256_TGT);
        MockKrb5LoginModule.setUsesRainier(true);
        setUp();
        KerberosServiceTicket ticket = client.getKerberosServiceTicket(AES256_PRINCIPAL_NAME, true);
        assertNotNull(ticket.getEncData());
    }

    @Test
    public void testGetKerberosServiceTicketMock() throws Exception {
        setUp();
        KerberosServiceTicket ticket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        assertNotNull(ticket);
        assertNotNull(ticket.getEncData());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetKerberosServiceTicketWrongPrinciple() throws Exception {
        setUp();
        client.getKerberosServiceTicket("INVALID", true);
    }

    @Test
    public void testGetKerberosAcceptPrincipal() throws Exception {
        setUp();
        String acceptPrincipal  = KerberosClient.getKerberosAcceptPrincipal(SERVICE_PRINCIPAL_NAME, true);
        assertNotNull(acceptPrincipal);
        acceptPrincipal = KerberosClient.getKerberosAcceptPrincipal(SIMPLE_SERVICE_PRINCIPAL_NAME, true);
        assertNotNull(acceptPrincipal);
    }

    @Test
    public void testGetKerberosAcceptPrincipalWithDomainMatch() throws Exception {
        setUp();
        String acceptPrincipal  = KerberosClient.getKerberosAcceptPrincipal("http", "abc.l7tech.sup", true);
        assertEquals(SERVICE_PRINCIPAL_NAME, acceptPrincipal);
    }

    @Test
    public void testGetKerberosAcceptPrincipalWithHttps() throws Exception {
        setUp();
        String acceptPrincipal  = KerberosClient.getKerberosAcceptPrincipal("https", "abc.l7tech.sup", true);
        assertEquals(SERVICE_PRINCIPAL_NAME, acceptPrincipal);
    }

    @Test
    public void testGetKerberosAcceptPrincipalWithNull() throws Exception {
        setUp();
        String acceptPrincipal  = KerberosClient.getKerberosAcceptPrincipal(null, true);
        assertEquals(DEFAULT_SERVICE_PRINCIPAL_NAME, acceptPrincipal);
    }

    @Test
    public void testGetKerberosUnAcceptPrincipal() throws Exception {
        setUp();
        String acceptPrincipal  = KerberosClient.getKerberosAcceptPrincipal("http/doesNotExist", true);
        assertEquals(DEFAULT_SERVICE_PRINCIPAL_NAME, acceptPrincipal);
    }

    @Test
    public void testGetKerberosPrincipal() throws Exception {
        setUp();
        assertEquals(SERVICE_PRINCIPAL_NAME, KerberosClient.getKerberosAcceptPrincipal(SERVICE, HOST, true));
    }

    @Test
    public void testGetInvalidKerberosPrincipal() throws Exception {
        setUp();
        assertEquals(DEFAULT_SERVICE_PRINCIPAL_NAME, KerberosClient.getKerberosAcceptPrincipal("DOES", "NOT_EXIST", true));
    }

    @Test
    public void testValidateKerberosPrincipal() throws Exception {
        setUp();
        KerberosClient.validateKerberosPrincipals();
    }

    @Ignore("Needs connection to the KDC")
    @Test
    public void getDelegatedServiceTicket() throws Exception {
        KerberosClient client = new KerberosClient();
        KerberosClient.setTimeSource(TIME_SOURCE);
        KerberosTicketRepository.setTimeSource(TIME_SOURCE);
        KerberosServiceTicket serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        KerberosServiceTicket delegateTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, InetAddress.getByName("localhost"), serviceTicket.getGSSAPReqTicket() );
        assertNotNull(delegateTicket);

    }

    @Ignore("Needs connection to the KDC")
    @Test
    /**
     * Under SYSPROP_SSG_HOME should contains the following files:
     * 1. logon.config (will be generated)
     * 2. krb5.conf (will be generated)
     * 3. kerberos.keytab
     *  a. Create user to the AD Server (e.g login = abc)
     *  b. Run "setspn -a service/server.domain.com@DOMAIN.COM login" (e.g setspn -a http/myworkstation@QAWIN2003.COM abc)
     *  c. Run "ktpass -princ service/server.domain.com@DOMAIN.COM -mapuser QAWIN2003\login -pass password -out kerberos.keytab"
     *      (e.g "ktpass -princ http/myworkstation@QAWIN2003.COM -mapuser QAWIN2003\abc -pass password -out kerberos.keytab"
     *  d. Download the keytab
     */
    public void testGetKerberosServiceTicket() throws Exception {
        KerberosClient client = new KerberosClient();
        KerberosClient.setTimeSource(TIME_SOURCE);
        KerberosTicketRepository.setTimeSource(TIME_SOURCE);
        final KerberosServiceTicket serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);


        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext("com.l7tech.common.security.kerberos.accept", subject, getServerCallbackHandler(SERVICE_PRINCIPAL_NAME));
        loginContext.login();
        Subject.doAs(subject, new PrivilegedExceptionAction() {
            @Override
            public Object run() throws Exception {
                GSSManager manager = GSSManager.getInstance();
                GSSContext context = manager.createContext((GSSCredential) null);
                //verify the ticket
                byte[] token = context.acceptSecContext(serviceTicket.getGSSAPReqTicket().toByteArray(), 0, serviceTicket.getGSSAPReqTicket().toByteArray().length);
                assertNull(token);
                return context.getSrcName().toString();
            }
        });
        assertNotNull(serviceTicket);
    }

    @Ignore("Needs connection to the KDC")
    @Test
    public void testValidateKerberosServiceTicket() throws Exception {
        KerberosClient client = new KerberosClient();
        KerberosClient.setTimeSource(TIME_SOURCE);
        KerberosTicketRepository.setTimeSource(TIME_SOURCE);
        KerberosServiceTicket serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, InetAddress.getByName("localhost"), serviceTicket.getGSSAPReqTicket());
        assertNotNull(serviceTicket);
    }

    @Ignore("Needs connection to the KDC, also broken in JDK 8")
    @Test
    /**
     * Capture the data from the real KDC connection and
     * we can use the captured data to mock the response from the KDC.
     */
    public void captureLoginStaticData() throws Exception {
        KerberosConfig.checkConfig(null, null, true, false);

        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_OUT_KEYTAB, subject, getServerCallbackHandler(SERVICE_PRINCIPAL_NAME));
        loginContext.login();
        subject = loginContext.getSubject();
        KerberosPrincipal kerberosPrincipal = (KerberosPrincipal) subject.getPrincipals().toArray()[0];
        //The TGT
        KerberosTicket kerberosTicket = (KerberosTicket) subject.getPrivateCredentials().toArray()[0];
        KeyTab keyTab = (KeyTab) subject.getPrivateCredentials().toArray()[1];
/*  JDK 8 compatibilty:  the KeysFromKeyTab inner classes no longer exists
        Krb5Util.KeysFromKeyTab keysFromKeyTab = (Krb5Util.KeysFromKeyTab) subject.getPrivateCredentials().toArray()[2];
        KerberosKey k = new KerberosKey(keysFromKeyTab.getPrincipal(), keysFromKeyTab.getEncoded(), keysFromKeyTab.getKeyType(), keysFromKeyTab.getVersionNumber());
        System.out.println("KerberosPrincipal: " + encode(kerberosPrincipal));
        System.out.println("TGT: " + encode(kerberosTicket));
        System.out.println("KeyBytes: " + Base64.encodeBase64String(keysFromKeyTab.getEncoded()));
        System.out.println("KeyType: " + keysFromKeyTab.getKeyType());
        System.out.println("VersionNumber: " + keysFromKeyTab.getVersionNumber());
*/
        Subject.doAs(subject, new PrivilegedExceptionAction<KerberosServiceTicket>() {
            @Override
            public KerberosServiceTicket run() throws Exception {
                Oid kerberos5Oid = KerberosClient.getKerberos5Oid();
                GSSManager manager = GSSManager.getInstance();

                GSSCredential credential = null;
                GSSContext context = null;
                try {
                    credential = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, kerberos5Oid, GSSCredential.INITIATE_ONLY);
                    String gssPrincipal = KerberosUtils.toGssName(SERVICE_PRINCIPAL_NAME);
                    GSSName serviceName = manager.createName(gssPrincipal, GSSName.NT_HOSTBASED_SERVICE, kerberos5Oid);

                    context = manager.createContext(serviceName, kerberos5Oid, credential, 60);
                    context.requestMutualAuth(false);
                    context.requestConf(true);

                    byte[] bytes = context.initSecContext(new byte[0], 0, 0);
                    System.out.println("Context: " + Base64.encodeBase64String(bytes));

                } finally {
                    if (context != null) context.dispose();
                    if (credential != null) credential.dispose();
                }
                return null;
            }
        });
        //The Service Ticket
        KerberosTicket ticket = (KerberosTicket) subject.getPrivateCredentials().toArray()[3];
        System.out.println("Service Ticket: " + encode(ticket));
    }

    public static String encode(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.close();
        return Base64.encodeBase64String(bos.toByteArray());
    }

    public static Object decode(String str) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decodeBase64(str);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        return ois.readObject();
    }

    private static CallbackHandler getServerCallbackHandler(final String servicePrincipalName) {
        return new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName(servicePrincipalName);
                    }
                }
            }
        };
    }
}
