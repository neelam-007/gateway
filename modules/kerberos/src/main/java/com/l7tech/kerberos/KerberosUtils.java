package com.l7tech.kerberos;

import java.io.File;

import com.l7tech.util.HexUtils;

/**
 * Kerberos Utility class.
 *
 * <p>You would not usually create an instance of this class.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosUtils {

    //- PUBLIC

    /**
     * Check if Kerberos is enabled.
     *
     * <p>This just checks if there is Kerberos configuration defined.</p>
     *
     * @return true if enabled.
     */
    public static boolean isEnabled() {
        return System.getProperty("java.security.krb5.conf")!=null;
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
    public static void configureKerberos(byte[] keytab, String kdc, String realm ) throws KerberosException {
        KerberosConfig.generateKerberosConfig(keytab, kdc, realm);
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

    //- PRIVATE

    /**
     * Namespace for kerberos session identifiers (Kerberos Key IDentifier)
     */
    private static final String SESSION_NAMESPACE = "http://www.layer7tech.com/kkid/";
    
}
