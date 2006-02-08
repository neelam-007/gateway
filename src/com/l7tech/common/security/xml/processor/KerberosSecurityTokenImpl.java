package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import org.w3c.dom.Element;

/**
 * Security token implementation for Kerberos.
 *
 * <p>Created from a BST or reference to a BST used in a previous request.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosSecurityTokenImpl extends SigningSecurityTokenImpl implements KerberosSecurityToken {

    //- PUBLIC

    public KerberosSecurityTokenImpl(KerberosGSSAPReqTicket ticket, String wsuId, Element element) {
        super(element);
        this.ticket = ticket;
        this.wsuId = wsuId;
    }

    public KerberosGSSAPReqTicket getTicket() {
        return ticket;
    }

    public String getElementId() {
        return wsuId;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_KERBEROS_BST;
    }

    //- PRIVATE

    private final KerberosGSSAPReqTicket ticket;
    private final String wsuId;
}
