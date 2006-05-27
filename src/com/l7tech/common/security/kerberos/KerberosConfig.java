package com.l7tech.common.security.kerberos;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.text.MessageFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;

/**
 * Generates Kerberos configuration for SecureSpan Bridge and Gateway.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class KerberosConfig {

    //- PACKAGE

    /**
     * Check / generate Kerberos configuration for SSG or SSB.
     *
     * This will detect the SSB / SSG and create the relevant config files.
     *
     * <ul>
     *   <li>krb5.conf - Kerberos configuration.</li>
     *   <li>login.config - Login Module configuration.</li>
     * </ul>
     *
     * On the SSG these files go in the conf directory, on the SSB in the ".l7tech"
     * directory.
     */
    static void checkConfig() {
        if (System.getProperty(SYSPROP_SSG_HOME) != null) {
            configSsg();
        }
        else if (System.getProperty(SYSPROP_LOGINCFG_PATH) != null &&
                 System.getProperty(SYSPROP_KRB5CFG_PATH) == null) {
            configSsb();
        }
    }

    /**
     * Can be called by a user to configure Kerberos info.
     */
    static void generateKerberosConfig(File file, String kdc, String realm) {
        generateKrb5Config(file, kdc, realm);
    }

    /**
     * Get the kdc, note that this is the cached value (so call after checkConfig).
     *
     * @return The KDC that is in use.
     */
    static String getConfigKdc() {
        return kerberosConfigKdc;
    }

    /**
     * Get the realm, note that this is the cached value (so call after checkConfig).
     *
     * @return The REALM that is in use.
     */
    static String getConfigRealm() {
        return kerberosConfigRealm;
    }

    static boolean hasKeytab() {
        return getKeytabFile().exists();
    }

    static Keytab getKeytab(boolean nullIfMissing) throws KerberosException {
        File keytabFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
        String principal = null;
        try {
            if (!keytabFile.exists()) {
                if (nullIfMissing) return null;
                else throw new KerberosConfigException("No Keytab");
            }
            return new Keytab(keytabFile);
        }
        catch(IOException ioe) {
            throw new KerberosException("Error reading Keytab file.", ioe);
        }
    }
    static String getKeytabPrincipal() throws KerberosException {
        String principal = null;

        Keytab keytab = getKeytab(false);
        String[] names = keytab.getKeyName();
        if (names.length == 1) {
            principal = names[0];
        }
        else if (names.length == 2) {
            principal = names[0] + "/" + names[1];
        }
        else {
            throw new KerberosException("Unknown name type ("+Arrays.asList(names)+")");
        }

        return principal;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosConfig.class.getName());

    private static final String SYSPROP_SSG_HOME = "com.l7tech.server.home";
    private static final String SYSPROP_LINE_SEP = "line.separator";
    private static final String SYSPROP_LOGINCFG_PATH = "java.security.auth.login.config";
    private static final String SYSPROP_KRB5CFG_PATH = "java.security.krb5.conf";
    private static final String SYSPROP_KRB5_KDC = "java.security.krb5.kdc";
    private static final String SYSPROP_KRB5_REALM = "java.security.krb5.realm";

    private static final String PATH_KEYTAB = "/etc/conf/kerberos.keytab";
    private static final String PATH_LOGINCFG = "/etc/conf/login.config";
    private static final String PATH_KRB5CFG = "/etc/conf/krb5.conf";

    private static final String RESOURCE_SSB_LOGINCFG = "/login.config";
    private static final String FILE_NAME_KRB5CFG = "krb5.conf";

    // Keep the detected values for reporting later
    private static String kerberosConfigKdc;
    private static String kerberosConfigRealm;


    /**
     * Template for the login.config file:
     *
     * 0 - Path to the Keytab file
     */
    private static final String LOGIN_CONFIG_TEMPLATE =
            "/////////////////////////////////\n" +
            "// Generated file, DO NOT EDIT //\n" +
            "/////////////////////////////////\n" +
            "\n" +
            "// Login module for SecureSpan Gateway\n" +
            "com.l7tech.common.security.kerberos.accept '{'\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"{0}\"\n" +
            "    storeKey=true;\n" +
            "};\n";

    /**
     * Template for the krb5.conf file:
     *
     *  0 - UPPERCASE realm
     *  1 - lowercase realm
     *  2 - IP for KDC
     */
    private static final String KRB5_CONF_TEMPLATE =
            "###############################\n" +
            "# Generated file, DO NOT EDIT #\n" +
            "###############################\n" +
            "\n" +
            "[libdefaults]\n" +
            "default_realm = {0}\n" +
            "\n" +
            "[realms]\n" +
            "{0} = '{'\n" +
            "kdc =  {2}:88\n" +
            "admin_server =  {2}:88\n" +
            "kpasswd_server =  {2}:464\n" +
            "default_domain = {1}\n" +
            "}\n";

    private static File getKeytabFile() {
        return new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
    }

    private static void configSsg() {
        try {
            File loginConfigFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_LOGINCFG);
            logger.config("Setting Kerberos login config file as '"+loginConfigFile.getAbsolutePath()+"'.");
            System.setProperty(SYSPROP_LOGINCFG_PATH, loginConfigFile.getAbsolutePath());

            if (loginConfigFile.exists()) {
                loginConfigFile.delete();
            }
            if (!loginConfigFile.exists()) {
                generateLoginConfig(loginConfigFile);
            }

            File krb5ConfigFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KRB5CFG);
            logger.config("Setting Kerberos config file as '"+krb5ConfigFile.getAbsolutePath()+"'.");
            System.setProperty(SYSPROP_KRB5CFG_PATH, krb5ConfigFile.getAbsolutePath());

            if (krb5ConfigFile.exists()) {
                krb5ConfigFile.delete();
            }
            if (!krb5ConfigFile.exists()) {
                generateKrb5Config(krb5ConfigFile);
            }
        }
        catch(Exception e) {
            logger.log(Level.SEVERE, "Error initializing Kerberos settings.", e);
        }
    }

    private static void configSsb() {
        try {
            File loginConfigFile = new File(System.getProperty(SYSPROP_LOGINCFG_PATH));
            File krb5ConfigFile = new File(loginConfigFile.getParentFile(), FILE_NAME_KRB5CFG);

            loginConfigFile.delete();
            if (!loginConfigFile.exists()) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = KerberosConfig.class.getResourceAsStream(RESOURCE_SSB_LOGINCFG);
                    out = new FileOutputStream(loginConfigFile);
                    HexUtils.copyStream(in, out);
                    out.flush();
                }
                finally {
                    ResourceUtils.closeQuietly(in);
                    ResourceUtils.closeQuietly(out);
                }
            }

            String principal = null;
            String realm = null;
            try {
                principal = new KerberosClient().getKerberosInitPrincipal();

                if (principal != null && principal.indexOf('@') > 0) {
                    realm = principal.substring(principal.indexOf('@')+1).toUpperCase();
                }
            }
            catch(KerberosException ke) {
                // not available (no ticket cache)
                logger.log(Level.FINE, "Could not get kerberos principal using ticket cache.", ke);
            }

            if (realm == null) {
                realm = System.getProperty(SYSPROP_KRB5_REALM);
            }

            if (realm != null) {
                generateKrb5Config(krb5ConfigFile, null, realm);
            }

            if (krb5ConfigFile.exists()) {
                System.setProperty(SYSPROP_KRB5CFG_PATH, krb5ConfigFile.getAbsolutePath());
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error initializing Kerberos settings.", e);
        }
    }

    private static void generateLoginConfig(File file) {
        File keytabFile = getKeytabFile();

        String ls = System.getProperty(SYSPROP_LINE_SEP, "\n");
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(MessageFormat.format(LOGIN_CONFIG_TEMPLATE,
                    new Object[]{keytabFile.getAbsolutePath()}).replace("\n", ls).getBytes("UTF-8"));
        }
        catch(IOException ioe) {
            logger.log(Level.SEVERE, "Error writing Kerberos login configuration.", ioe);
        }
        finally {
            ResourceUtils.closeQuietly(out);
        }
    }

    private static void generateKrb5Config(File file) {
        File keytabFile = getKeytabFile();

        String ucRealm = getRealm();
        if (ucRealm == null) return; // no information availble to create config.

        generateKrb5Config(file, null, ucRealm);
    }

    private static void generateKrb5Config(File file, String kdcIp, String ucRealm) {
        String ls = System.getProperty(SYSPROP_LINE_SEP, "\n");
        String lcRealm = ucRealm.toLowerCase();

        kerberosConfigRealm = ucRealm;

        if (kdcIp == null) {
            kdcIp = System.getProperty(SYSPROP_KRB5_KDC);
        }
        if (kdcIp == null) {
            try {
                kdcIp = InetAddress.getByName(lcRealm).getHostAddress();
            }
            catch(UnknownHostException uhe) {
                return;
            }
        }

        kerberosConfigKdc = kdcIp;

        OutputStream out = null;
        try {
            if (file.exists()) file.delete();
            out = new FileOutputStream(file);
            out.write(MessageFormat.format(KRB5_CONF_TEMPLATE,
                    new Object[]{ucRealm, lcRealm, kdcIp}).replace("\n", ls).getBytes("UTF-8"));
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "Error writing Kerberos login configuration.", ioe);
        }
        finally {
            ResourceUtils.closeQuietly(out);
        }
    }


    private static String getRealm() {
        String realm = null;

        try {
            if (hasKeytab()) {
                Keytab keytab = getKeytab(false);
                realm = keytab.getKeyRealm();
            }
            else {
                String principal = KerberosClient.getKerberosAcceptPrincipal();
                if (principal != null && principal.indexOf('@') > 0) {
                    realm = principal.substring(principal.indexOf('@')+1);
                }
            }
        }
        catch (KerberosException ke) {
            // ignore, we are just creating config if possible
        }

        if (realm == null) {
            realm = System.getProperty(SYSPROP_KRB5_REALM);
        }

        if (realm != null)
            realm = realm.toUpperCase();

        return realm;
    }
}
