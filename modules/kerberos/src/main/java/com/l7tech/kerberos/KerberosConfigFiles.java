package com.l7tech.kerberos;

import com.l7tech.util.*;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that handles the writing of the Kerberos support files to the gateway filesystem.
 */
class KerberosConfigFiles implements KerberosConfigConstants {

    /** Logger */
    private static final Logger logger = Logger.getLogger(KerberosConfigFiles.class.getName());

    private Krb5Keytab krb5Keytab;

    Krb5ConfigProperties krb5Prop = new Krb5ConfigProperties();

    private final String ls = SyspropUtil.getString( SYSPROP_LINE_SEP, "\n" );

    /**
     * Constructor.
     *
     * @param realm - sets the realm value
     * @param kdc - sets the kdc value
     * @throws KerberosException when parsing the keytab file fails
     */
    public KerberosConfigFiles(String realm, String kdc) throws KerberosException {
        this.krb5Keytab = new Krb5Keytab();
        if (realm != null)
            this.krb5Prop.addRealm(realm.toUpperCase(), kdc);

        init();
    }

    /**
     * Creates the login.config file on the filesystem.
     */
    public void createLoginFile() {
        // only valid if the keytab file exists
        if (krb5Keytab.exists()) {
            File loginFile = new File( ConfigFactory.getProperty( SYSPROP_SSG_HOME ) + PATH_LOGINCFG);
            writeConfigFile(loginFile, formatLoginContents());
        }
    }

    /**
     * Creates the krb5.conf file on the filesystem.
     */
    public void createKrb5ConfigFile(boolean overwriteKrb5Conf) {
        // only valid if we have sufficient information
        if (krb5Prop.canWrite()) {
            File krb5ConfFile = new File( ConfigFactory.getProperty( SYSPROP_SSG_HOME ) + PATH_KRB5CFG);
            if (!krb5ConfFile.exists() || overwriteKrb5Conf) {
                writeConfigFile(krb5ConfFile, formatKrb5ConfContents());
            }
        }
    }

    public KeyTab getKeytab() {
        if (krb5Keytab != null)
            return krb5Keytab.keytab;
        return null;
    }

    public List<String> getRealms() {
        return krb5Keytab.realms;
    }

    public String getDefaultPrinciple() {
        if (krb5Keytab != null)
            return krb5Keytab.getDefaultPrincipal();
        return null;
    }

    private void init() {

        // login file
        File loginFile = new File( ConfigFactory.getProperty( SYSPROP_SSG_HOME ) + PATH_LOGINCFG);
        logger.config("Setting Kerberos login config file as '" + loginFile.getAbsolutePath() + "'.");
        SyspropUtil.setProperty( SYSPROP_LOGINCFG_PATH, loginFile.getAbsolutePath() );

        // krb5 file
        File krb5ConfFile = new File( ConfigFactory.getProperty( SYSPROP_SSG_HOME ) + PATH_KRB5CFG);
        logger.config("Setting Kerberos config file as '" + krb5ConfFile.getAbsolutePath() + "'.");
        SyspropUtil.setProperty( SYSPROP_KRB5CFG_PATH, krb5ConfFile.getAbsolutePath() );

        if (krb5Prop.getDefaultRealm() == null) {
            krb5Prop.addRealm(krb5Keytab.getDefaultRealm(), krb5Keytab.parseDefaultKdc(krb5Prop.defaultRealm));
            for (String realm : krb5Keytab.getNonDefaultRealms()) {
                if (realm != null && realm.length() > 0)
                    krb5Prop.addRealm(realm, KerberosUtils.getKdc(realm));
            }
        }
        if (krb5Prop.getDefaultKdc() == null) {
            krb5Prop.addRealm(krb5Prop.defaultRealm, krb5Keytab.parseDefaultKdc(krb5Prop.defaultRealm));
        }
    }

    /**
     * Gets the Kerberos keytab File object.
     *
     * @return the keytab File object
     */
    private File getKeytabFile() {
        return krb5Keytab.file;
    }

    /**
     * Formats the contents of the login.config file.
     *
     * @return String with the formatted contents
     */
    private String formatLoginContents() {
        String refresh = ConfigFactory.getProperty( SYSPROP_KRB5_REFRESH, REFRESH_DEFAULT );

        return MessageFormat.format(LOGIN_CONFIG_TEMPLATE,
                getKeytabFile().getAbsolutePath(), refresh).replace("\n", ls);
    }

    /**
     * Formats the contents of the krb5.conf file.
     *
     * @return String with the formatted contents
     */
    private String formatKrb5ConfContents() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format(KRB5_CONF_TEMPLATE,
                krb5Prop.defaultRealm,
                krb5Prop.defaultRealm.toLowerCase(),
                krb5Prop.formatKDCBlock(krb5Prop.getDefaultKdc()),
                krb5Prop.getFirstDefaultKdc(),
                krb5Prop.encTypesTkt,
                krb5Prop.encTypesTgs).replace("\n", ls));

        Iterator<Map.Entry<String,String>> iter = krb5Prop.getRealms().entrySet().iterator();
        //skip the default realm
        if (iter.hasNext()) iter.next();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry =  iter.next();
            if (entry.getKey() != null && entry.getValue() != null) {
                sb.append(MessageFormat.format(KRB5_CONF_REALMS_TEMPLATE,
                        entry.getKey(),
                        entry.getKey().toLowerCase(),
                        krb5Prop.formatKDCBlock(entry.getValue()),
                        entry.getValue().replace("\n", ls)));
            }
        }

        if (krb5Prop.getRealms().size() > 1) {
            sb.append(KRB5_CONF_DOMAIN_REALM);
            iter = krb5Prop.getRealms().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry =  iter.next();
                if (entry.getKey() != null && entry.getValue() != null) {
                    sb.append(MessageFormat.format(KRB5_CONF_DOMAIN_REALM_TEMPLATE,
                            entry.getKey(),
                            entry.getKey().toLowerCase()));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Writes the specified file with the provided contents to the file system.
     *
     * @param file the file to write to
     * @param contents the contents to put into the file
     */
    private void writeConfigFile(File file, String contents) {

        // if the file already exists, delete it before writing to it
        if (file.exists()) {
            file.delete();
        }

        OutputStream out = null;
        try {
            if (file.exists()) file.delete();
            out = new FileOutputStream(file);
            out.write(contents.getBytes(Charsets.UTF8));
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "Error writing Kerberos configuration file: " + file.getName(), ioe);
        }
        finally {
            ResourceUtils.closeQuietly(out);
        }
    }

    /**
     * Properties used in the krb5 configuration file.
     */
    class Krb5ConfigProperties {

        static final String KDC_PREFIX = "kdc = ";
        static final String KDC_PORT = ":88\n";

        LinkedHashMap<String, String> realms = new LinkedHashMap<String, String>();

        String defaultRealm;
        String encTypesTkt = ConfigFactory.getProperty( SYSPROP_KRB5_ENC_TKT, ENCTYPES_TKT_DEFAULT );
        String encTypesTgs = ConfigFactory.getProperty( SYSPROP_KRB5_ENC_TGS, ENCTYPES_TGS_DEFAULT );

        /**
         * Not write the krb5.conf file if any of the KDC ip cannot be resolved.
         * @return true when there is sufficient config infor to write the krb5.conf file, otherwise false.
         */
        boolean canWrite() {
            // means that there is not sufficient config info to write the krb5.conf file
            Collection<String> values = getRealms().values();
            for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
                String next =  iterator.next();
                if (next == null) {
                    return false;
                }
            }
            return (defaultRealm != null);
        }

        public String getDefaultRealm() {
            return defaultRealm;
        }

        public String getDefaultKdc() {
            return realms.get(defaultRealm);
        }

        public String getFirstDefaultKdc() {
            String kdc = realms.get(defaultRealm);
            if (kdc!= null && kdc.contains(",")) {
                StringTokenizer st = new StringTokenizer(kdc, ",");
                return st.nextElement().toString().trim();
            }
            return kdc;
        }
        
        public String getEncTypesTkt() {
            return encTypesTkt;
        }

        public String getEncTypesTgs() {
            return encTypesTgs;
        }

        public String formatKDCBlock(String kdc) {
            // parse out the kdc value
            if (kdc != null && kdc.contains(",")) {
                StringBuffer sb = new StringBuffer();

                StringTokenizer st = new StringTokenizer(kdc, ",");
                String val;
                while (st.hasMoreElements()) {

                    val = st.nextElement().toString();

                    // create one kdc entry
                    sb.append(KDC_PREFIX).append(val.trim()).append(KDC_PORT);
                }

                return sb.toString();
            } else if (kdc != null) {
                return KDC_PREFIX + kdc + KDC_PORT;
            }
            return null;
        }

        /**
         * @param realm The realm with the key = realm name and value = kdc ip address
         */
        public void addRealm(String realm, String kdc) {
            if (realm == null) {
                return;
            }
            if (realms.size() == 0) {
                defaultRealm = realm;
            }
            realms.put(realm, kdc);
        }

        public LinkedHashMap<String, String> getRealms() {
            return realms;
        }
    }

    private class Krb5Keytab {

        File file = new File( ConfigFactory.getProperty( SYSPROP_SSG_HOME ) + PATH_KEYTAB);
        KeyTab keytab;
        List<String> realms = new ArrayList<String>();

        private Krb5Keytab() throws KerberosException {

            if (exists()) {
                // parse the keytab file
                keytab = getKeytab(false);
                parseRealms();
            }
        }
        
        private void parseRealms() {
            if (keytab != null) {
                KeyTabEntry[] keyTabEntries = keytab.getEntries();
                if (keyTabEntries != null && keyTabEntries.length > 0) {
                    realms = new ArrayList<String>();
                    for (int i = 0; i < keyTabEntries.length; i++) {
                        PrincipalName principalName = keyTabEntries[i].getService();
                        if (principalName != null && principalName.getRealmString() != null) {
                            if (!realms.contains(principalName.getRealmString().toUpperCase())) {
                                realms.add(principalName.getRealmString().toUpperCase());
                            }
                        }
                    }
                }
            }
        }

        /**
         * Assume the first keytab entry is the default principal
         * @return The first principal defined in the keytab file.
         */
        String getDefaultPrincipal() {
            if (keytab != null && keytab.getEntries().length > 0) {
                return keytab.getEntries()[0].getService().getName();
            }
            return null;
        }
        
        List<String> getNonDefaultRealms() {
            if (realms != null && realms.size() > 1)
                return realms.subList(1, realms.size());
            return new ArrayList<String>();
        }
        
        String getDefaultRealm() {
            String realm = null;

            try {
                //Assume the first one in the keytab file is the default realm
                if (realms != null && realms.size() > 0) {
                    realm = realms.get(0);
                }
                else {
                    // this call also seems to look for a keytab -- is this really needed?
                    String principal = KerberosClient.getKerberosAcceptPrincipal(true);
                    if (principal != null && principal.indexOf('@') > 0) {
                        realm = principal.substring(principal.indexOf('@')+1);
                    }
                }
            }
            catch (KerberosException ke) {
                // ignore, we are just creating config if possible
            }

            if (realm == null) {
                // check the system property
                realm = SyspropUtil.getProperty( SYSPROP_KRB5_REALM );
            }

            if (realm != null) {
                realm = realm.toUpperCase();
            }

            return realm;
        }

        String parseDefaultKdc(String realm) {
            String kdcIp = SyspropUtil.getProperty( SYSPROP_KRB5_KDC );

            if (kdcIp == null) {
                kdcIp = KerberosUtils.getKdc(realm);
            }

            return kdcIp;
        }
        
        boolean exists() {
            return file != null && file.exists();
        }

        KeyTab getKeytab(boolean nullIfMissing) throws KerberosException {

            if (keytab != null) {
                return keytab;

            } else {
                if (!file.exists()) {
                    if (nullIfMissing) return null;
                    else throw new KerberosConfigException("No Keytab");
                }
                //Perform keytab file validation
                KerberosUtils.validateKeyTab(file);
                return KeyTab.getInstance(file);
            }
        }
    }
}
