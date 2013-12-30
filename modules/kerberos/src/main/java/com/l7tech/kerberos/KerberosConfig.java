package com.l7tech.kerberos;

import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates Kerberos configuration for SecureSpan Bridge and Gateway.
 */
public class KerberosConfig implements KerberosConfigConstants {

    //- PUBLIC

    /**
     * Get the kdc, note that this is the cached value (so call after checkConfig).
     *
     * @return The KDC that is in use.
     */
    public static String getConfigKdc() {
        return kerberosFiles.krb5Prop.getDefaultKdc();
    }
    
    /**
     * Get the realm, note that this is the cached value (so call after checkConfig).
     *
     * @return The REALM that is in use.
     */
    public static String getConfigRealm() {
        return kerberosFiles.krb5Prop.getDefaultRealm();
    }

    public static Map<String, String> getRealms() {
        return kerberosFiles.krb5Prop.getRealms();
    }

    public static KeyTab getKeytab(boolean nullIfMissing) throws KerberosException {

        if (kerberosFiles != null) {
            KeyTab keyTab = kerberosFiles.getKeytab();
            if (keyTab == null)  {
                if (nullIfMissing) return null;
                else throw new KerberosConfigException("No Keytab");
            }
            return keyTab;
        } else {
            File keytabFile = new File( SyspropUtil.getProperty( SYSPROP_SSG_HOME ) + PATH_KEYTAB);
            if (!keytabFile.exists()) {
                if (nullIfMissing) return null;
                else throw new KerberosConfigException("No Keytab");
            }
            KerberosUtils.validateKeyTab(keytabFile);
            return KeyTab.getInstance(keytabFile);
        }
    }

    /**
     * Retrieve the fully qualify service principal Name from the keytab file.
     *
     * @param principal the service principal, the principal may not be a fully qualify service principal
     *                  The service may only contain the service and host name (e.g http/gateway.l7tech.com),
     *                  null for lookup the default qualified service principal name
     * @return The fully qualify service principal name from the keytab file.
     * @throws KerberosException
     */
    public static String getKeytabPrincipal(String principal) throws KerberosException {
        String principalName = null;
        //determine if the principal is actually matching the realm and convert it to lower case
        if(principal != null) {
            Matcher m = SPN_PATTERN.matcher(principal);
            if(m.find() && m.groupCount() == 3){
              if(m.group(1) == null && m.group(3) != null) {
                principalName = principalCache.get(m.group(3).toLowerCase());
              }
            }
        }
        // it's not a realm so it might be a principal
        if(principalName == null) {
            principalName = principalCache.get(principal);
        }
        // neither of the above so we need to match the domain
        if (principalName == null && kerberosFiles != null) {
            if ( principal != null) {
                Map<String, String> realms = kerberosFiles.krb5Prop.getRealms();
                for (Iterator iterator = realms.keySet().iterator(); iterator.hasNext(); ) {
                    String next =  (String)iterator.next();
                    if (principal.toLowerCase().endsWith(next.toLowerCase())) {
                        principalName = principalCache.get(next.toLowerCase());
                        break;
                    }
                }
            }
            if (principalName == null) {
                return kerberosFiles.getDefaultPrinciple(); //when nothing matches get default principal
            }
        }
        return principalName;
    }
    
    public static boolean hasKeytab() {

        if (kerberosFiles != null) {

            return (kerberosFiles.getKeytab() != null);

        } else {
            File ktFile = new File( SyspropUtil.getProperty( SYSPROP_SSG_HOME ) + PATH_KEYTAB);
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
    static void checkConfig( final String kdc, final String realm, final boolean isInit, final boolean overwriteKrb5Conf ) {
        if (!isInit || !initialized) {
            initialized = true;

            if ( SyspropUtil.getProperty( SYSPROP_SSG_HOME ) != null) {
                configSsg( kdc, realm, overwriteKrb5Conf);
            }
            else if ( SyspropUtil.getProperty( SYSPROP_LOGINCFG_PATH ) != null &&
                    SyspropUtil.getProperty( SYSPROP_KRB5CFG_PATH ) == null) {
                configSsb();
            }
        }
    }

    /**
     * Can be called by a user to configure Kerberos info.
     */
    public static void generateKerberosConfig(byte[] keytabData, String kdc, String realm, boolean overwriteKrb5Conf) throws KerberosException {
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

        checkConfig( kdc, realm, false, overwriteKrb5Conf );
    }

    /**
     * Can be called by a user to delete Kerberos info.
     */
    static void deleteKerberosKeytab() throws KerberosException {
        final File keytab = getKeytabFile();
        if ( keytab.exists() && !keytab.delete() ) {
            throw new KerberosException( "Unable to delete kerberos keytab" );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosConfig.class.getName());

    private static final Pattern SPN_PATTERN = Pattern.compile("((\\w+/[A-Za-z0-9\\-\\.]+)@?)*([A-Z0-9\\-\\.]*[^\\W]){0,1}");

    protected static KerberosConfigFiles kerberosFiles;
    protected static Map<String, String> principalCache = new HashMap<String, String>();
    private static volatile boolean initialized = false;

    private static File getKeytabFile() {
        return new File( SyspropUtil.getProperty( SYSPROP_SSG_HOME ) + PATH_KEYTAB);
    }

    /**
     * Initialize Kerberos configuration for the SSG.
     */
    private static void configSsg( final String kdc, final String realm, final boolean overwriteKrb5Conf ) {

        try {
            //Clean up the cache
            principalCache.clear();
            kerberosFiles = null;

            // pull the realm and kdc values from the cluster properties
            kerberosFiles = new KerberosConfigFiles( realm, kdc );

            // create the login.config file
            kerberosFiles.createLoginFile();

            // create the krb5.conf file
            kerberosFiles.createKrb5ConfigFile(overwriteKrb5Conf);

            if  (kerberosFiles.getKeytab() != null) {
                KeyTabEntry[] keyTabEntries = kerberosFiles.getKeytab().getEntries();
                for (int i = 0; i < keyTabEntries.length; i++) {
                    KeyTabEntry keyTabEntry = keyTabEntries[i];
                    principalCache.put(keyTabEntry.getService().getNameString(), keyTabEntry.getService().getName());
                    principalCache.put(keyTabEntry.getService().getName(), keyTabEntry.getService().getName());
                    principalCache.put(keyTabEntry.getService().getRealm().toString().toLowerCase(), keyTabEntry.getService().getName());
                }
            }
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
            File loginConfigFile = new File( SyspropUtil.getProperty( SYSPROP_LOGINCFG_PATH ) );
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
                realm = SyspropUtil.getProperty( SYSPROP_KRB5_REALM );
            }

            if (realm != null) {
                generateKrb5Config(krb5ConfigFile, null, realm);
            }

            if (krb5ConfigFile.exists()) {
                SyspropUtil.setProperty( SYSPROP_KRB5CFG_PATH, krb5ConfigFile.getAbsolutePath() );
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
        String ls = SyspropUtil.getString( SYSPROP_LINE_SEP, "\n" );
        String lcRealm = ucRealm.toLowerCase();
        String encTypesTkt = ConfigFactory.getProperty( SYSPROP_KRB5_ENC_TKT, ENCTYPES_TKT_DEFAULT );
        String encTypesTgs = ConfigFactory.getProperty( SYSPROP_KRB5_ENC_TGS, ENCTYPES_TGS_DEFAULT );

        if (kdcIp == null) {
            kdcIp = SyspropUtil.getProperty( SYSPROP_KRB5_KDC );
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
                    ucRealm, lcRealm, kdcIp, encTypesTkt, encTypesTgs).replace("\n", ls).getBytes(Charsets.UTF8));
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
