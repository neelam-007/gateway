package com.l7tech.common.security.kerberos;

import org.w3c.dom.Element;

import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;

/**
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
