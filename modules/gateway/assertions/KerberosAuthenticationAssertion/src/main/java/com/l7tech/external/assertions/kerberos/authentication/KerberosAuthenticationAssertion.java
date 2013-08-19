package com.l7tech.external.assertions.kerberos.authentication;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

/**
 * 
 */
public class KerberosAuthenticationAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(KerberosAuthenticationAssertion.class.getName());

    public static final Pattern spnPattern = Pattern.compile("^([a-zA-Z0-9]+)\\/([a-zA-Z0-9\\.-]+[^\\W])(@([a-zA-Z0-9-\\.]*[^\\W])){0,1}$");

    protected String realm;
    protected String servicePrincipalName;
    protected boolean lastAuthenticatedUser;
    protected String authenticatedUser;
    protected boolean krbDelegatedAuthentication = true;
    protected boolean krbUseGatewayKeytab = true;
    protected String krbConfiguredAccount;
    protected Goid krbSecurePasswordReference = SecurePassword.DEFAULT_GOID;
    protected boolean s4U2Self;
    protected boolean s4U2Proxy;
    protected String userRealm;

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

    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getKrbSecurePasswordReference() {
        return krbSecurePasswordReference;
    }

    /**
     * @deprecated passwords now use Goid not long oid's
     */
    @Deprecated
    public void setKrbSecurePasswordReference(long securePasswordOid) {
        this.krbSecurePasswordReference = GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, securePasswordOid);
    }

    public void setKrbSecurePasswordReference(Goid securePasswordGoid) {
        this.krbSecurePasswordReference = securePasswordGoid;
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

    public String getUserRealm() {
        return userRealm;
    }

    public void setUserRealm(String userRealm) {
        this.userRealm = userRealm;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(servicePrincipalName, authenticatedUser, realm, krbConfiguredAccount, userRealm);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = KerberosAuthenticationAssertion.class.getName() + ".metadataInitialized";

    @Override
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
        meta.put(AssertionMetadata.SHORT_NAME, "Retrieve Kerberos Authentication Credentials");
        meta.put(AssertionMetadata.LONG_NAME, "Gateway retrieves and validates requester credentials from KDC via Kerberos v5 protocol");

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
        meta.put(POLICY_VALIDATOR_CLASSNAME, KerberosAuthenticationAssertion.Validator.class.getName());

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:KerberosAuthentication" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "assertion:KerberosAuthentication");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


    public static class Validator implements AssertionValidator{
       private final KerberosAuthenticationAssertion assertion;


        public Validator(KerberosAuthenticationAssertion assertion) {
           this.assertion = assertion;
        }
        /**
         * Validate the assertion in the given path, service and store the result in the
         * validator result.
         *
         * @param path   the assertion path where the assertion is located
         * @param pvc    information about the context in which the assertion appears
         * @param result the result where the validation warnings or errors are collected
         */
        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            int foundCredentialSource = -1;
            int foundIdentity = -1;
            int foundKerberosCredentialSource = -1;
            Assertion[] assertions = path.getPath();
            for(int i=0; i < assertions.length; i++) {
                Assertion ass = assertions[i];
                if(ass.isEnabled()) {
                    if (ass == assertion) {
                        if (foundCredentialSource == -1 || foundCredentialSource > i) {
                            result.addError(new PolicyValidatorResult.Error(assertion, "Must be preceded by a credential source", null));

                        }
                        else if(!assertion.isS4U2Self() && (foundKerberosCredentialSource == -1 || foundKerberosCredentialSource > i)) {
                            result.addError(new PolicyValidatorResult.Error(assertion, "Must be preceded by either Require Windows Integrated Authentication Credentials or Require WS-Security Kerberos Token Profile Credentials", null));
                        }
                        else if (assertion.isS4U2Self() && (foundIdentity == -1 || foundIdentity > i)) {
                            result.addError(new PolicyValidatorResult.Error(assertion, "Must be preceded by an identity assertion (e.g. Authenticate User or Group)", null));
                        }

                        return;
                    }
                    else if(ass.isCredentialSource()) {
                        foundCredentialSource = i;
                        if(ass instanceof HttpNegotiate || ass instanceof RequestWssKerberos)  {
                            foundKerberosCredentialSource = i;
                        }
                    }
                    else if(ass instanceof IdentityAssertion) {
                        foundIdentity = i;
                    }
                }
            }
        }
    }
}
