package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

/**
 * Specifies that a kerberos ticket is required.
 *
 * @author $Author$
 * @version $Version: $
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssKerberos extends Assertion implements SetsVariables {

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
    @SuppressWarnings( { "UnusedDeclaration" } )
    @Deprecated
    public void setServicePrincipalName(String ignored) {
    }

    /**
     * The kerberos ticket is credential source.
     *
     * @return always true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata("kerberos.realm", false, false, "kerberos.realm", false, DataType.STRING),
        };
    }
}
