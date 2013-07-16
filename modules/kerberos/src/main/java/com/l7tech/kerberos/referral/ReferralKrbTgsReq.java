package com.l7tech.kerberos.referral;

import com.l7tech.kerberos.delegate.DelegateKrbTgsReq;
import sun.security.krb5.Credentials;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.KDCOptions;

import java.io.IOException;

/**
 * Implementation of TGS_REQ for Referral
 * construct a TGS_REQ message to KDC and obtain service ticket.
 */
public class ReferralKrbTgsReq extends DelegateKrbTgsReq {

    public static final int CANONICALIZE = 15;

    /**
     * Constructor for referral TGS_REQ message
     */
    public ReferralKrbTgsReq(Credentials tgtCreds,
                             PrincipalName sname)
            throws KrbException, IOException {
        super(getCanonicalize(),
                tgtCreds,
                sname, null, null);
    }

    private static KDCOptions getCanonicalize() {
        KDCOptions o = new KDCOptions();
        o.set(CANONICALIZE, true);
        o.set(KDCOptions.FORWARDABLE, true);
        return o;
    }
}
