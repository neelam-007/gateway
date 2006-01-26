package com.l7tech.common.security.kerberos;

import org.w3c.dom.Element;

import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.HexUtils;

/**
 * Security token implementation for Kerberos.
 *
 * <p>Created from a BST or reference to a BST used in a previous request.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosSecurityTokenImpl implements KerberosSecurityToken {

    //- PUBLIC

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

    private final KerberosGSSAPReqTicket ticket;
}
