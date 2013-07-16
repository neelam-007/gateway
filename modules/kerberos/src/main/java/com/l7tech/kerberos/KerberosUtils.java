package com.l7tech.kerberos;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

/**
 * Kerberos Utility class.
 *
 * <p>You would not usually create an instance of this class.</p>
 */
public class KerberosUtils {


    //- PUBLIC

    public static InetAddress inetAddress = null;

    /**
     * Check if Kerberos is enabled.
     *
     * <p>This just checks if there is Kerberos configuration defined.</p>
     *
     * @return true if enabled.
     */
    public static boolean isEnabled() {
        return SyspropUtil.getProperty( "java.security.krb5.conf" ) != null;
    }

    /**
     * Create a Kerberos session identifier for the given string.
     *
     * @param sha1Base64ApReq the base64 encoded sha-1 hash of the referenced AP REQ
     * @return the session identifier
     */
    public static String getSessionIdentifier(String sha1Base64ApReq) {
        return SESSION_NAMESPACE + sha1Base64ApReq;
    }

    /**
     * Create a Kerberos session identifier for the given ticket.
     *
     * @param ticket the ticket to be identified.
     * @return the session identifier
     */
    public static String getSessionIdentifier(KerberosGSSAPReqTicket ticket) {
        return getSessionIdentifier(getBase64Sha1(ticket));
    }

    /**
     * Get the Base64 encoded SHA-1 hash of the given ticket.
     *
     * @param ticket the ticket
     * @return the hash
     */
    public static String getBase64Sha1(KerberosGSSAPReqTicket ticket) {
        return HexUtils.encodeBase64(HexUtils.getSha1Digest(ticket.toByteArray()));
    }

    /**
     * Generate a kerberos configuration file with the given kdc and realm.
     *
     * @param file  The configuration file to be created
     * @param kdc   The Kerberos Key Distribution Center
     * @param realm The Kerberos REALM
     * @throws KerberosException if an error occurs
     */
    public static void configureKerberos(File file, String kdc, String realm) throws KerberosException {
        KerberosConfig.generateKerberosConfig(file, kdc, realm);
    }

    /**
     * Regenerate kerberos configuration files using the given keytab.
     *
     * @param keytab The keytab data to use
     * @throws KerberosException if an error occurs
     */
    public static void configureKerberos(boolean deleteIfMissing, byte[] keytab, String kdc, String realm, boolean overwriteKrb5Conf ) throws KerberosException {
        if ( deleteIfMissing && keytab==null ) {
            KerberosConfig.deleteKerberosKeytab();
        }
        KerberosConfig.generateKerberosConfig(keytab, kdc, realm, overwriteKrb5Conf);
        KerberosClient.reset();
    }

    /**
     * If the kerberos configuration has been generated this will return the KDC host/ip.
     *
     * @return the KDC or null.
     */
    public static String getKerberosKdc() {
        return KerberosConfig.getConfigKdc();
    }

    /**
     * If the kerberos configuration has been generated this will return the REALM.
     *
     * @return the REALM or null.
     */
    public static String getKerberosRealm() {
        return KerberosConfig.getConfigRealm();
    }

    /**
     * Convert a kerberos principal to a gss principal.
     *
     * e.g.
     *      in : http/myserver.myrealm.com@MYREALM.COM
     *      out: http@myserver.myrealm.com
     *
     * @param kerberosName The kerberos name
     * @return The GSS name
     */
    public static String toGssName(String kerberosName) {
        String name = kerberosName;

        if (name != null && name.indexOf('/') > 0) {
            int realmStart = name.indexOf('@');
            if( realmStart > 0) {
                name = name.substring(0, realmStart);
            }
            name = name.replace('/', '@');
        }

        return name;
    }

    /**
     * Retrieve the KDC ip from the realm. (The Realm is the hostname of the KDC)
     * @param realm The realm name
     * @return The ip address of the KDC, null if we cannot lookup the ip from the realm,
     */
    public static String getKdc(String realm) {
        if (realm != null) {
            try {
                if (inetAddress == null) {
                    return InetAddress.getByName(realm).getHostAddress();
                } else {
                     return inetAddress.getHostAddress();
                }
            } catch(UnknownHostException uhe) {
                return null;
            }
        }
        return null;
    }

    /**
     * Validate the keytab file. Make sure the keytab file is in valid format. If the keytab contains multiple
     * principal, make sure the no principal name are duplicated.
     *
     * @param file The keytab file
     * @throws KerberosException when the keytab file is invalid.
     */
    public static void validateKeyTab(File file) throws KerberosException {
        //Validate the Keytab content, and make sure the keytab in valid format.
        //We may not required to valid with the com.l7tech.kerberos.Keytab class,
        //the sun.security.krb5.internal.ktab.KeyTab.isValid may sufficient enough to perform the keytab validation
        try {
            new Keytab(file);
        } catch (IOException ioe) {
            throw new KerberosException("Error reading Keytab file.", ioe);
        }
        KeyTab keyTab = KeyTab.getInstance(file);
        if (!keyTab.isValid()) {
            throw new KerberosException("Invalid Keytab file");
        }
        //For each entry in the keytab, make sure the principal name is not duplicated.
        KeyTabEntry[] keyTabEntries = keyTab.getEntries();
        for (int i = 0; i < keyTabEntries.length; i++) {
            KeyTabEntry keyTabEntry = keyTabEntries[i];
            for (int j = i + 1; j < keyTabEntries.length; j++) {
                KeyTabEntry e = keyTabEntries[j];
                if (keyTabEntry != e) {
                    if (e.getService().getNameString().equals(keyTabEntry.getService().getNameString())) {
                        throw new KerberosException("Invalid Keytab file, duplicate principal name: "
                                + e.getService().getNameString());
                    }
                }
            }
        }
    }

    //- PRIVATE

    /**
     * Namespace for kerberos session identifiers (Kerberos Key IDentifier)
     */
    private static final String SESSION_NAMESPACE = "http://www.layer7tech.com/kkid/";
    
}
