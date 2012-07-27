package com.l7tech.security.token;

import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosServiceTicket;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 7/12/12
 */
public class KerberosAuthenticationSecurityToken implements KerberosSecurityToken {

    private final KerberosServiceTicket kerberosServiceTicket;

    public KerberosAuthenticationSecurityToken(KerberosServiceTicket kst) {
        kerberosServiceTicket = kst;
    }


    @Override
    public KerberosGSSAPReqTicket getTicket() {
        return kerberosServiceTicket.getGSSAPReqTicket();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public KerberosServiceTicket getServiceTicket() {
        return kerberosServiceTicket;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SecurityTokenType getType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
