package com.l7tech.kerberos;

import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.text.MessageFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class that handles the writing of the Kerberos support files to the gateway filesystem.
 */
class KerberosConfigFiles implements KerberosConfigConstants {

    /** Logger */
    private static final Logger logger = Logger.getLogger(KerberosConfigFiles.class.getName());

    private Krb5Keytab krb5Keytab;

    Krb5ConfigProperties krb5Prop = new Krb5ConfigProperties();

    private final String ls = System.getProperty(SYSPROP_LINE_SEP, "\n");

    /**
     * Constructor.
     *
     * @param realm - sets the realm value
     * @param kdc - sets the kdc value
     * @throws KerberosException when parsing the keytab file fails
     */
    public KerberosConfigFiles(String realm, String kdc) throws KerberosException {
        this.krb5Keytab = new Krb5Keytab();
        this.krb5Prop.setKdc(kdc);
        if (realm != null)
            this.krb5Prop.setRealm(realm.toUpperCase());

        init();
    }

    /**
     * Creates the login.config file on the filesystem.
     */
    public void createLoginFile() {
        // only valid if the keytab file exists
        if (krb5Keytab.exists()) {
            File loginFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_LOGINCFG);
            writeConfigFile(loginFile, formatLoginContents());
        }
    }

    /**
     * Creates the krb5.conf file on the filesystem.
     */
    public void createKrb5ConfigFile() {
        // only valid if we have sufficient information
        if (krb5Prop.canWrite()) {
            File krb5ConfFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KRB5CFG);
            writeConfigFile(krb5ConfFile, formatKrb5ConfContents());
        }
    }

    public Keytab getKeytab() {
        if (krb5Keytab != null)
            return krb5Keytab.keytab;
        return null;
    }

    private void init() {

        // login file
        File loginFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_LOGINCFG);
        logger.config("Setting Kerberos login config file as '" + loginFile.getAbsolutePath() + "'.");
        System.setProperty(SYSPROP_LOGINCFG_PATH, loginFile.getAbsolutePath());

        // krb5 file
        File krb5ConfFile = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KRB5CFG);
        logger.config("Setting Kerberos config file as '" + krb5ConfFile.getAbsolutePath() + "'.");
        System.setProperty(SYSPROP_KRB5CFG_PATH, krb5ConfFile.getAbsolutePath());

        if (krb5Prop.getRealm() == null) {
            krb5Prop.setRealm(krb5Keytab.parseRealm());
        }
        if (krb5Prop.getKdc() == null) {
            krb5Prop.setKdc(krb5Keytab.parseKdc(krb5Prop.realm));
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
        String refresh = System.getProperty(SYSPROP_KRB5_REFRESH, REFRESH_DEFAULT);

        return MessageFormat.format(LOGIN_CONFIG_TEMPLATE,
                getKeytabFile().getAbsolutePath(), refresh).replace("\n", ls);
    }

    /**
     * Formats the contents of the krb5.conf file.
     *
     * @return String with the formatted contents
     */
    private String formatKrb5ConfContents() {
        return MessageFormat.format(KRB5_CONF_TEMPLATE,
                krb5Prop.realm,
                krb5Prop.realm.toLowerCase(),
                krb5Prop.kdc_block,
                krb5Prop.getKdc(),
                krb5Prop.encTypesTkt,
                krb5Prop.encTypesTgs).replace("\n", ls);
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
            out.write(contents.getBytes("UTF-8"));
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

        List<String> kdcList = new ArrayList<String>();
        String realm;
        String encTypesTkt = System.getProperty(SYSPROP_KRB5_ENC_TKT, ENCTYPES_TKT_DEFAULT);
        String encTypesTgs = System.getProperty(SYSPROP_KRB5_ENC_TGS, ENCTYPES_TGS_DEFAULT);

        String kdc_block;

        boolean canWrite() {
            // means that there is not sufficient config info to write the krb5.conf file
            return (realm != null && !kdcList.isEmpty());
        }

        public String getRealm() {
            return realm;
        }

        public String getKdc() {
            if (!kdcList.isEmpty())
                return kdcList.get(0);
            return null;
        }

        public String getEncTypesTkt() {
            return encTypesTkt;
        }

        public String getEncTypesTgs() {
            return encTypesTgs;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public void setKdc(String kdc) {

            // parse out the kdc value
            if (kdc != null && kdc.contains(",")) {
                StringBuffer sb = new StringBuffer();

                StringTokenizer st = new StringTokenizer(kdc, ",");
                String val;
                while (st.hasMoreElements()) {

                    val = st.nextElement().toString();

                    // create one kdc entry
                    sb.append(KDC_PREFIX).append(val.trim()).append(KDC_PORT);
                    kdcList.add(val.trim());
                }

                kdc_block = sb.toString();
                
            } else if (kdc != null) {
                kdcList.add(kdc);
                kdc_block = KDC_PREFIX + kdc + KDC_PORT;
            }
        }
    }

    private class Krb5Keytab {

        File file = new File(System.getProperty(SYSPROP_SSG_HOME) + PATH_KEYTAB);
        Keytab keytab;

        private Krb5Keytab() throws KerberosException {

            if (exists()) {
                // parse the keytab file
                keytab = getKeytab(false);
            }
        }

        String parseRealm() {
            String realm = null;

            try {
                if (krb5Keytab.exists()) {
                    Keytab keytab = getKeytab(false);
                    realm = keytab.getKeyRealm();
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
                realm = System.getProperty(SYSPROP_KRB5_REALM);
            }

            if (realm != null) {
                realm = realm.toUpperCase();
            }

            return realm;
        }

        String parseKdc(String realm) {
            String kdcIp = System.getProperty(SYSPROP_KRB5_KDC);

            if (kdcIp == null && realm != null) {
                try {
                    kdcIp = InetAddress.getByName(realm).getHostAddress();
                }
                catch(UnknownHostException uhe) {
                    return null;
                }
            }

            return kdcIp;
        }

        boolean exists() {
            return file != null && file.exists();
        }

        Keytab getKeytab(boolean nullIfMissing) throws KerberosException {

            if (keytab != null) {
                return keytab;

            } else {
                try {
                    if (!file.exists()) {
                        if (nullIfMissing) return null;
                        else throw new KerberosConfigException("No Keytab");
                    }
                    return new Keytab(file);
                }
                catch(IOException ioe) {
                    throw new KerberosException("Error reading Keytab file.", ioe);
                }
            }
        }
    }
}
