package com.l7tech.kerberos;

import com.l7tech.common.io.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates Kerberos configuration for SecureSpan Bridge and Gateway.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class KerberosConfig implements KerberosConfigConstants {

    //- PUBLIC

    /**
     * Get the kdc, note that this is the cached value (so call after checkConfig).
     *
     * @return The KDC that is in use.
     */
    public static String getConfigKdc() {
        return kerberosFiles.krb5Prop.getKdc();
    }

    /**
     * Get the realm, note that this is the cached value (so call after checkConfig).
     *
     * @return The REALM that is in use.
     */
    public static String getConfigRealm() {
        return kerberosFiles.krb5Prop.getRealm();
    }

    public static Keytab getKeytab(boolean nullIfMissing) throws KerberosException {

        if (kerberosFiles != null) {
            return kerberosFiles.getKeytab();

        } else {
            File keytabFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
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
    }

    public static String getKeytabPrincipal() throws KerberosException {
        String principal;

        Keytab keytab = getKeytab(false);
        String[] names = keytab.getKeyName();
        if (names.length == 1) {
            principal = names[0];
        }
        else if (names.length == 2) {
            principal = names[0] + "/" + names[1];
        }
        else {
            throw new KerberosException("Unknown name type for keytab principal ("+Arrays.asList(names)+")");
        }

        return principal;
    }


    public static boolean hasKeytab() {

        if (kerberosFiles != null) {

            return (kerberosFiles.getKeytab() != null);

        } else {
            File ktFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
            return ktFile.exists();
        }
    }

    /**
     * Can be called by a user to configure Kerberos info.
     */
    public static void generateKerberosConfig(File file, String kdc, String realm) {
        generateKrb5Config(file, kdc, realm);
    }

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
    static void checkConfig( final String kdc, final String realm, final boolean isInit ) {
        if (!isInit || !initialized) {
            initialized = true;

            if (System.getProperty(SYSPROP_SSG_HOME) != null) {
                configSsg( kdc, realm );
            }
            else if (System.getProperty(SYSPROP_LOGINCFG_PATH) != null &&
                     System.getProperty(SYSPROP_KRB5CFG_PATH) == null) {
                configSsb();
            }
        }
    }

    /**
     * Can be called by a user to configure Kerberos info.
     */
    static void generateKerberosConfig(byte[] keytabData, String kdc, String realm) throws KerberosException {
        if ( keytabData != null ) {
            File keytab = getKeytabFile();

            OutputStream out = null;
            try {
                out = new FileOutputStream(keytab);
                out.write( keytabData );
            }
            catch(IOException ioe) {
                throw new KerberosException( "Error writing Kerberos keytab.", ioe);
            }
            finally {
                ResourceUtils.closeQuietly(out);
            }
        }

        checkConfig( kdc, realm, false );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosConfig.class.getName());

    protected static KerberosConfigFiles kerberosFiles;
    private static volatile boolean initialized = false;

    private static File getKeytabFile() {
        return new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
    }

    /**
     * Initialize Kerberos configuration for the SSG.
     */
    private static void configSsg( final String kdc, final String realm ) {

        try {
            // pull the realm and kdc values from the cluster properties
            kerberosFiles = new KerberosConfigFiles( realm, kdc );

            // create the login.config file
            kerberosFiles.createLoginFile();

            // create the krb5.conf file
            kerberosFiles.createKrb5ConfigFile();
        }
        catch(Exception e) {
            logger.log(Level.SEVERE, "Error initializing Kerberos settings.", e);
        }
    }

    /**
     * Initialize Kerberos configuration for the SSB (XML VPN Client).
     */
    private static void configSsb() {
        try {
            File loginConfigFile = new File(System.getProperty(SYSPROP_LOGINCFG_PATH));
            File krb5ConfigFile = new File(loginConfigFile.getParentFile(), FILE_NAME_KRB5CFG);

            loginConfigFile.delete();
            if (!loginConfigFile.exists()) {
                loginConfigFile.getParentFile().mkdirs();

                InputStream in = null;
                OutputStream out = null;
                try {
                    in = KerberosConfig.class.getResourceAsStream(RESOURCE_SSB_LOGINCFG);
                    if (in != null) {
                        out = new FileOutputStream(loginConfigFile);
                        IOUtils.copyStream(in, out);
                        out.flush();
                    }
                }
                finally {
                    ResourceUtils.closeQuietly(in);
                    ResourceUtils.closeQuietly(out);
                }
            }

            String principal;
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

    /**
     * This method is left here to support the configSsb() & generateKerberosConfig(...) methods
     * which are left unaltered.
     *
     * @param file the file to write to
     * @param kdcIp the KDC value to use
     * @param ucRealm the Realm value (in uppercase) to use
     */
    private static void generateKrb5Config(File file, String kdcIp, String ucRealm) {
        String ls = System.getProperty(SYSPROP_LINE_SEP, "\n");
        String lcRealm = ucRealm.toLowerCase();
        String encTypesTkt = System.getProperty(SYSPROP_KRB5_ENC_TKT, ENCTYPES_TKT_DEFAULT);
        String encTypesTgs= System.getProperty(SYSPROP_KRB5_ENC_TGS, ENCTYPES_TGS_DEFAULT);

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

        OutputStream out = null;
        try {
            if (file.exists()) file.delete();
            out = new FileOutputStream(file);
            out.write(MessageFormat.format(SSB_KRB5_CONF_TEMPLATE,
                    ucRealm, lcRealm, kdcIp, encTypesTkt, encTypesTgs).replace("\n", ls).getBytes("UTF-8"));
        }
        catch(IOException ioe) {
            String fn = (file == null? "null" : file.getName());
            logger.log(Level.WARNING, "Error writing Kerberos configuration file: " + fn, ioe);
        }
        finally {
            ResourceUtils.closeQuietly(out);
        }
    }

}
