package com.l7tech.kerberos;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import java.io.*;
import java.security.PrivilegedExceptionAction;

import static junit.framework.Assert.assertNotNull;

/**
 * User: awitrisna
 */
public class KerberosClientTest {

    private static final String LOGIN_CONTEXT_OUT_KEYTAB = "com.l7tech.common.security.kerberos.outbound.keytab";
    private static final String SERVICE_PRINCIPAL_NAME = "http/awitrisna-desktop@QAWIN2003.COM";
    private KerberosClient client;
    private static final String TMPDIR = System.getProperty("java.io.tmpdir");
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String KRB5_CONF = TMPDIR + SEPARATOR + "mockkrb5.conf";
    private static final String LOGIN_CONFIG = TMPDIR + SEPARATOR + "mocklogin.config";
    private static final String KEYTAB = TMPDIR + SEPARATOR + "kerberos.keytab";


    @BeforeClass
    public static void init() throws IOException {
        setUpKrb5Conf();
        setUpLoginConfig();
        setupKeytab();
    }

    @AfterClass
    public static void dispose() {
        File krb5Conf = new File(KRB5_CONF);
        if (krb5Conf.exists()) {
            krb5Conf.delete();
        }
        File loginConfig = new File(LOGIN_CONFIG);
        if (loginConfig.exists()) {
            loginConfig.delete();
        }
        File keyTabFile = new File(KEYTAB);
        if (keyTabFile.exists()) {
            keyTabFile.delete();
        }
    }

    /**
     * Setup the Kerberos Client.
     * Do not run this setup for the test which required KDC Connection.
     *
     * @throws Exception
     */
    public void setUp() throws Exception {
        client = new KerberosClient() {
            @Override
            public GSSManager getGSSManager() {
                return new MockGSSManagerImpl();
            }
        };
        //locate keytab file
        System.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, TMPDIR);
        System.setProperty(KerberosConfigConstants.SYSPROP_KRB5CFG_PATH, KRB5_CONF);
        System.setProperty("java.security.auth.login.config", LOGIN_CONFIG);
    }

    /**
     * Setup the krb5.conf file
     *
     * @throws IOException
     */
    private static void setUpKrb5Conf() throws IOException {
        String krb5 = "[libdefaults]\n" +
                "default_realm = QAWIN2003.COM\n" +
                "default_tkt_enctypes = rc4-hmac,des-cbc-md5\n" +
                "default_tgs_enctypes = rc4-hmac,des-cbc-md5\n" +
                "\n" +
                "[realms]\n" +
                "QAWIN2003.COM = {\n" +
                "kdc = localhost:88\n" +
                "admin_server =  localhost:88\n" +
                "kpasswd_server =  localhost:464\n" +
                "default_domain = qawin2003.com\n" +
                "}";
        File krb5Conf = new File(KRB5_CONF);
        if (krb5Conf.exists()) {
            krb5Conf.delete();
        }
        FileWriter fos = new FileWriter(krb5Conf);
        fos.write(krb5);
        fos.close();
    }

    /**
     * Setup the login.config file
     *
     * @throws IOException
     */
    private static void setUpLoginConfig() throws IOException {
        String login = "com.l7tech.common.security.kerberos.outbound.keytab {\n" +
                "    com.l7tech.kerberos.MockKrb5LoginModule required\n" +
                "    refreshKrb5Config=true\n" +
                "    storeKey=true;\n" +
                "};";

        File loginConfig = new File(LOGIN_CONFIG);
        if (loginConfig.exists()) {
            loginConfig.delete();
        }
        FileWriter fos = new FileWriter(loginConfig);
        fos.write(login);
        fos.close();
    }


    /**
     * Setup the keytab file
     *
     * @throws IOException
     */
    public static void setupKeytab() throws IOException {
        String keytab = "BQIAAABHAAIADVFBV0lOMjAwMy5DT00ABGh0dHAAEWF3aXRyaXNuYS1kZXNrdG9wAAAAAAAAAAAC" +
                "ABcAEIhG9+ruj7EXrQa92DC3WGw=";

        File keyTabFile = new File(KEYTAB);
        if (keyTabFile.exists()) {
            keyTabFile.delete();
        }
        byte[] data = Base64.decodeBase64(keytab);
        FileOutputStream fos = new FileOutputStream(keyTabFile);
        fos.write(data);
        fos.close();
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
        System.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, "/home/awitrisna/tmp");
        KerberosClient client = new KerberosClient();
        KerberosServiceTicket serviceTicket = client.getKerberosServiceTicket(SERVICE_PRINCIPAL_NAME, true);
        assertNotNull(serviceTicket);
    }

    @Ignore("Needs connection to the KDC and JDK7")
    @Test
    /**
     * Capture the data from the real KDC connection and
     * we can use the captured data to mock the response from the KDC.
     */
    public void captureLoginStaticData() throws Exception {
        System.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, "/home/awitrisna/tmp");
        KerberosConfig.checkConfig(null, null, true);

        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_OUT_KEYTAB, subject, getServerCallbackHandler(SERVICE_PRINCIPAL_NAME));
        loginContext.login();
        subject = loginContext.getSubject();
        KerberosPrincipal kerberosPrincipal = (KerberosPrincipal) subject.getPrincipals().toArray()[0];
        //The TGT
        KerberosTicket kerberosTicket = (KerberosTicket) subject.getPrivateCredentials().toArray()[0];
        //JDK1.7
        //KeyTab keyTab = (KeyTab) subject.getPrivateCredentials().toArray()[1];
        //Krb5Util.KeysFromKeyTab keysFromKeyTab = (Krb5Util.KeysFromKeyTab) subject.getPrivateCredentials().toArray()[2];
        //KerberosKey k = new KerberosKey(keysFromKeyTab.getPrincipal(), keysFromKeyTab.getEncoded(), keysFromKeyTab.getKeyType(), keysFromKeyTab.getVersionNumber());
        System.out.println("KerberosPrincipal: " + encode(kerberosPrincipal));
        System.out.println("TGT: " + encode(kerberosTicket));
        //System.out.println("KeyBytes: " + Base64.encodeBase64String(keysFromKeyTab.getEncoded()));
        //JDK1.7
        //System.out.println("KeyType: " + keysFromKeyTab.getKeyType());
        //System.out.println("VersionNumber: " + keysFromKeyTab.getVersionNumber());
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
        KerberosTicket ticket = (KerberosTicket) subject.getPrivateCredentials().toArray()[2];
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
