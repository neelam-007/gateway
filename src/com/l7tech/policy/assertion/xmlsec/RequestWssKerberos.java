package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 * Specifies that a kerberos ticket is required.
 *
 * @author $Author$
 * @version $Version: $
 */
public class RequestWssKerberos extends Assertion {

    //- PUBLIC

    /**
     *
     */
    public RequestWssKerberos() {
    }

    /**
     * Get the service principal name for the service.
     *
     * Currently this returns a system wide name.
     *
     * @return the name
     */
    public String getServicePrincipalName() {
        return null;
    }

    /**
     * Dummy setter for service principal name.
     *
     * @deprecated Only for use by assertion serializer
     */
    public void setServicePrincipalName(String ignored) {
    }

    /**
     * The kerberos ticket is credential source.
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }
}
