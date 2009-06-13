package com.l7tech.security.token.http;

import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;

/**
 * Kerberos HTTP token (Negotiate / SPNEGO)
 */
public class HttpNegotiateToken implements KerberosSecurityToken {

    //- PUBLIC

    public HttpNegotiateToken( final KerberosServiceTicket kerberosServiceTicket ) {
        this.kerberosServiceTicket = kerberosServiceTicket;
    }

    public KerberosServiceTicket getServiceTicket() {
        return kerberosServiceTicket;
    }

    public KerberosGSSAPReqTicket getTicket() {
        return kerberosServiceTicket.getGSSAPReqTicket();
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_KERBEROS;
    }

    //- PRIVATE

    private final KerberosServiceTicket kerberosServiceTicket;
}
