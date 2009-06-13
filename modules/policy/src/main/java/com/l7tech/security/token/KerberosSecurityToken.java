package com.l7tech.security.token;

import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosServiceTicket;

/**
 * Kerberos security token.
 */
public interface KerberosSecurityToken extends SecurityToken {
    public KerberosGSSAPReqTicket getTicket();
    public KerberosServiceTicket getServiceTicket();
}
