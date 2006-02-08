package com.l7tech.common.security.kerberos;

import com.l7tech.common.util.HexUtils;

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
     * <p>This just checks if there is a JAAS login configuration defined.</p>
     *
     * @return true if enabled.
     */
    public static boolean isEnabled() {
        return System.getProperty("java.security.auth.login.config")!=null;
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
        return getSessionIdentifier(HexUtils.encodeBase64(HexUtils.getSha1Digest(ticket.toByteArray())));
    }

    //- PRIVATE

    /**
     * Namespace for kerberos session identifiers (Kerberos Key IDentifier)
     */
    private static final String SESSION_NAMESPACE = "http://www.layer7tech.com/kkid/";
    
}
