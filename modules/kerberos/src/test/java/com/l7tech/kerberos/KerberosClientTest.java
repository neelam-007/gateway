package com.l7tech.kerberos;

import com.l7tech.util.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sun.security.jgss.krb5.Krb5Util;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.kerberos.KerberosKey;
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
    public KerberosClient client;


    @BeforeClass
    public static void init() throws IOException, KerberosException {
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, true);

        KerberosTestSetup.init(tmpDir);
    }

/*    @AfterClass
    public static void dispose() {
        FileUtils.deleteDir(tmpDir);
    }*/

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
    public void testGetKerberosServiceTicketMock() throws Exception {
        setUp();
        KerberosServiceTicket ticket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        assertNotNull(ticket);
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
        KerberosServiceTicket serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, InetAddress.getByName("localhost"), serviceTicket.getGSSAPReqTicket());
        assertNotNull(serviceTicket);
    }

    @Ignore("Needs connection to the KDC")
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
        Krb5Util.KeysFromKeyTab keysFromKeyTab = (Krb5Util.KeysFromKeyTab) subject.getPrivateCredentials().toArray()[2];
        KerberosKey k = new KerberosKey(keysFromKeyTab.getPrincipal(), keysFromKeyTab.getEncoded(), keysFromKeyTab.getKeyType(), keysFromKeyTab.getVersionNumber());
        System.out.println("KerberosPrincipal: " + encode(kerberosPrincipal));
        System.out.println("TGT: " + encode(kerberosTicket));
        System.out.println("KeyBytes: " + Base64.encodeBase64String(keysFromKeyTab.getEncoded()));
        System.out.println("KeyType: " + keysFromKeyTab.getKeyType());
        System.out.println("VersionNumber: " + keysFromKeyTab.getVersionNumber());
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
