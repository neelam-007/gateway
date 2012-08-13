package com.l7tech.external.assertions.kerberos.authentication;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.variable.Syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class KerberosAuthenticationAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(KerberosAuthenticationAssertion.class.getName());

    protected String realm;
    protected String servicePrincipalName;
    protected boolean lastAuthenticatedUser;
    protected String authenticatedUser;
    protected boolean krbDelegatedAuthentication = true;
    protected boolean krbUseGatewayKeytab = true;
    protected String krbConfiguredAccount;
    protected long krbSecurePasswordReference = -1L;
    protected boolean s4U2Self;
    protected boolean s4U2Proxy;

    public boolean isLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }

    public void setLastAuthenticatedUser(boolean lastAuthenticatedUser) {
        this.lastAuthenticatedUser = lastAuthenticatedUser;
    }

    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(String authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getServicePrincipalName() {
        return servicePrincipalName;
    }

    public void setServicePrincipalName(String servicePrincipalName) {
        this.servicePrincipalName = servicePrincipalName;
    }

    public boolean isKrbDelegatedAuthentication() {
        return krbDelegatedAuthentication;
    }

    public void setKrbDelegatedAuthentication(boolean krbDelegatedAuthentication) {
        this.krbDelegatedAuthentication = krbDelegatedAuthentication;
    }

    public boolean isKrbUseGatewayKeytab() {
        return krbUseGatewayKeytab;
    }

    public void setKrbUseGatewayKeytab(boolean krbUseGatewayKeytab) {
        this.krbUseGatewayKeytab = krbUseGatewayKeytab;
    }

    public String getKrbConfiguredAccount() {
        return krbConfiguredAccount;
    }

    public void setKrbConfiguredAccount(String krbConfiguredAccount) {
        this.krbConfiguredAccount = krbConfiguredAccount;
    }

    public long getKrbSecurePasswordReference() {
        return krbSecurePasswordReference;
    }

    public void setKrbSecurePasswordReference(long securePasswordOid) {
        this.krbSecurePasswordReference = securePasswordOid;
    }

    public boolean isS4U2Self() {
        return s4U2Self;
    }

    public void setS4U2Self(boolean s4U2Self) {
        this.s4U2Self = s4U2Self;
    }

    public boolean isS4U2Proxy() {
        return s4U2Proxy;
    }

    public void setS4U2Proxy(boolean s4U2Proxy) {
        this.s4U2Proxy = s4U2Proxy;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(servicePrincipalName, authenticatedUser, realm, krbConfiguredAccount);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = KerberosAuthenticationAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Require Kerberos Authentication Credentials");
        meta.put(AssertionMetadata.LONG_NAME, "Requester required to authenticate via Kerberos v5 protocol");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");//authentication.gif
        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.kerberos.authentication.console.KerberosAuthenticationDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Kerberos Authentication Credentials Properties");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");//authentication.gif

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:KerberosConstrainedDelegation" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
