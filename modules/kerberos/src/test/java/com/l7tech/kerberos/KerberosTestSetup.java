package com.l7tech.kerberos;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSManager;

import java.io.*;

/**
 * Setup Kerberos related test
 */
public class KerberosTestSetup {

    private static final String TMPDIR = System.getProperty("java.io.tmpdir");
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String KRB5_CONF = TMPDIR + SEPARATOR + "krb5.conf";
    private static final String LOGIN_CONFIG = TMPDIR + SEPARATOR + "login.config";
    private static final String KEYTAB = TMPDIR + SEPARATOR + "kerberos.keytab";
    public static KerberosClient client;

    public static void init() throws IOException, KerberosException {
        System.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, TMPDIR);
        setupKeytab();
        setUpKrb5Conf();
    }

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

    public static void setUp() throws Exception {
        client = new KerberosClient() {
            @Override
            public GSSManager getGSSManager() {
                return new MockGSSManagerImpl();
            }
        };
        setUpLoginConfig();
        System.setProperty(KerberosConfigConstants.SYSPROP_KRB5CFG_PATH, KRB5_CONF);
    }

    /**
     * Setup the keytab file
     *
     * @throws IOException
     */
    public static void setupKeytab() throws IOException, KerberosException {
        String keytab = KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB;
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null);
    }

    public static void setupInvalidKeytab() throws KerberosException {
        String keytab = KerberosConfigTest.INVALID_KEYTAB;
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null);
    }

    /**
     * Setup the krb5.conf file
     *
     * @throws IOException
     */
    private static void setUpKrb5Conf() throws IOException {
        String krb5 = "[libdefaults]\n" +
                "default_realm = QAWIN2003.SUP\n" +
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
    public static void setUpLoginConfig() throws IOException {
        File file = new File(LOGIN_CONFIG);
        BufferedReader fr = null;
        StringBuilder sb = new StringBuilder();
        try {
            fr = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = fr.readLine()) != null) {
                if (line.contains("com.sun.security.auth.module.Krb5LoginModule")) {
                    line = line.replace("com.sun.security.auth.module.Krb5LoginModule", "com.l7tech.kerberos.MockKrb5LoginModule" );
                }
                sb.append(line);
                sb.append("\n");
            }
        } finally {
            fr.close();
        }
        file.delete();

        FileWriter fos = new FileWriter(file);
        fos.write(sb.toString());
        fos.close();
    }

}
