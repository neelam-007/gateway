package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Specifies that a kerberos ticket is required.
 *
 * @author $Author$
 * @version $Version: $
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssKerberos extends SecurityHeaderAddressableSupport implements SetsVariables {

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

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata("kerberos.realm", false, false, "kerberos.realm", false, DataType.STRING),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Require WS-Security Kerberos Token Profile Credentials");
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway retrieves and validates a Kerberos security token from the request");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        
        return meta;
    }
}
