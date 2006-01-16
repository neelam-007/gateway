package com.l7tech.identity.attribute;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;

// TODO something other than client principal name? :)
public class KerberosSecurityTokenMapping extends SecurityTokenMapping {
    public Object[] extractValues(SecurityToken creds) {
        if (creds instanceof KerberosSecurityToken) {
            KerberosSecurityToken kerberosSecurityToken = (KerberosSecurityToken) creds;
            KerberosServiceTicket ticket = kerberosSecurityToken.getTicket().getServiceTicket();
            if (ticket != null) {
                return new String[] { ticket.getClientPrincipalName() };
            }
        }
        return new String[0];
    }
}
