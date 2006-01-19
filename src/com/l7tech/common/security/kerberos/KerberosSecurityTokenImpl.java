package com.l7tech.common.security.kerberos;

import org.w3c.dom.Element;

import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.HexUtils;

/**
 * @author $Author$
 * @version $Revision$
 */
public class KerberosSecurityTokenImpl implements KerberosSecurityToken {

    //- PUBLIC

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
     * TODO [steve] move this elsewhere
     *
     * @param ticket the ticket to be identified.
     * @return the session identifier
     */
    public static String getSessionIdentifier(KerberosGSSAPReqTicket ticket) {
        return getSessionIdentifier(HexUtils.encodeBase64(HexUtils.getSha1().digest(ticket.toByteArray())));
    }

    public KerberosSecurityTokenImpl(KerberosGSSAPReqTicket ticket) {
        this.ticket = ticket;
    }

    public KerberosGSSAPReqTicket getTicket() {
        return ticket;
    }

    public String getElementId() {
        return null;
    }

    public Element asElement() {
        return null;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_KERBEROS_BST;
    }

    //- PRIVATE

    /**
     * Namespace for kerberos session identifiers (Kerberos Key IDentifier)
     */
    private static final String SESSION_NAMESPACE = "http://www.layer7tech.com/kkid/";

    private final KerberosGSSAPReqTicket ticket;
}
