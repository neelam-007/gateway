package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * This assertion verifies that the soap request contained
 * an xml digital signature but does not care about which
 * elements were signed. The cert used for the signature is
 * remembered to identify the user. This cert can later
 * be used for comparaison in an identity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssX509Cert extends SecurityHeaderAddressableSupport {
    /**
     * The WSS X509 security token is credential source.
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }
}
