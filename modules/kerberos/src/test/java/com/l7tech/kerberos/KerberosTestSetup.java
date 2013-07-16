package com.l7tech.kerberos;

import com.l7tech.util.SyspropUtil;
import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSManager;

import java.io.*;

/**
 * Setup Kerberos related test
 */
public class KerberosTestSetup {

    public static KerberosClient client;

    public static void init(File tmpDir) throws IOException, KerberosException {
        SyspropUtil.setProperty(KerberosConfigConstants.SYSPROP_SSG_HOME, tmpDir.getPath());
        setupKeytab();
        setUpKrb5Conf(tmpDir);
    }

    public static void setUp(File tmpDir) throws Exception {
        client = new KerberosClient() {
            @Override
            public GSSManager getGSSManager() {
                return new MockGSSManagerImpl();
            }
        };
        setUpLoginConfig(tmpDir);
        SyspropUtil.setProperty(KerberosConfigConstants.SYSPROP_KRB5CFG_PATH, tmpDir.getPath() + File.separator + "krb5.conf");
    }

    /**
     * Setup the keytab file
     *
     * @throws IOException
     */
    private static void setupKeytab() throws IOException, KerberosException {
        String keytab = KerberosConfigTest.MULTIPLE_PRINCIPAL_KEYTAB;
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null, false);
    }

    public static void setupInvalidKeytab() throws KerberosException {
        String keytab = KerberosConfigTest.INVALID_KEYTAB;
        KerberosConfig.generateKerberosConfig(Base64.decodeBase64(keytab), null, null, false);
    }

    /**
     * Setup the krb5.conf file
     *
     * @throws IOException
     */
    private static void setUpKrb5Conf(File tmpDir) throws IOException {
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
        File krb5Conf = new File(tmpDir.getPath() + File.separator + "krb5.conf");
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
    public static void setUpLoginConfig(File tmpDir) throws IOException {
        File file = new File(tmpDir.getPath() + File.separator + "login.config");
        BufferedReader fr = null;
        StringBuilder sb = new StringBuilder();
        try {
            fr = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = fr.readLine()) != null) {
                if (line.contains("com.sun.security.auth.module.Krb5LoginModule")) {
                    line = line.replace("com.sun.security.auth.module.Krb5LoginModule", "com.l7tech.kerberos.MockKrb5LoginModule" );
                }
                if (line.contains("com.l7tech.kerberos.delegate.DelegateKrb5LoginModule")) {
                    line = line.replace("com.l7tech.kerberos.delegate.DelegateKrb5LoginModule", "com.l7tech.kerberos.MockKrb5LoginModule" );
                }
                sb.append(line);
                sb.append("\n");
            }
        } finally {
           if(fr != null)
              fr.close();
        }
        file.delete();

        FileWriter fos = new FileWriter(file);
        fos.write(sb.toString());
        fos.close();
    }
    
    public static File getKeyTab() {
        return new File(SyspropUtil.getProperty(KerberosConfigConstants.SYSPROP_SSG_HOME) + File.separator + "kerberos.keytab");
    }
    
    public static File getLoginConfig() {
        return new File(SyspropUtil.getProperty(KerberosConfigConstants.SYSPROP_SSG_HOME) + File.separator + "login.config");
    }

    public static File getKrb5Conf() {
        return new File(SyspropUtil.getProperty(KerberosConfigConstants.SYSPROP_SSG_HOME) + File.separator + "krb5.conf");
    }

}
