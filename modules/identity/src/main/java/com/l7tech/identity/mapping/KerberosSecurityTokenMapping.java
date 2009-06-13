package com.l7tech.identity.mapping;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.kerberos.KerberosServiceTicket;

// TODO something other than client principal name? :)
public class KerberosSecurityTokenMapping extends SecurityTokenMapping {
    public Object[] extractValues(SecurityToken creds) {
        if (creds instanceof KerberosSecurityToken) {
            KerberosSecurityToken kerberosSecurityToken = (KerberosSecurityToken) creds;
            KerberosServiceTicket ticket = kerberosSecurityToken.getServiceTicket();
            if (ticket != null) {
                return new String[] { ticket.getClientPrincipalName() };
            }
        }
        return new String[0];
    }
}
