package com.l7tech.external.assertions.csrfprotection;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 * 
 */
public class CsrfProtectionAssertion extends Assertion implements SetsVariables {
    public static final String CTX_VAR_NAME_CSRF_VALID_TOKEN = "csrf.valid.token";

    protected static final Logger logger = Logger.getLogger(CsrfProtectionAssertion.class.getName());

    private boolean enableDoubleSubmitCookieChecking = true;
    private String cookieName = null;
    private String parameterName = null;
    private HttpParameterType parameterType = HttpParameterType.POST;
    private boolean enableHttpRefererChecking = false;
    private boolean allowMissingOrEmptyReferer = false;
    private boolean onlyAllowCurrentDomain = true;
    private List<String> trustedDomains = new ArrayList<>();

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(enableDoubleSubmitCookieChecking) {
            return new VariableMetadata[] {new VariableMetadata(CTX_VAR_NAME_CSRF_VALID_TOKEN, false, false, null, false)};
        } else {
            return new VariableMetadata[0];
        }
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CsrfProtectionAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Protect Against Cross-Site Request Forgery");
        meta.put(AssertionMetadata.DESCRIPTION, "The Protect Against Cross-Site Request Forgery Assertion helps " +
                "detect and prevent CSRF (Cross-Site Request Forgery) attacks");
        meta.put(AssertionMetadata.POLICY_NODE_NAME, "Protect Against CSRF Forgery");

        Collection<TypeMapping> othermappings = new ArrayList<>();
        othermappings.add(new Java5EnumTypeMapping(HttpParameterType.class, "httpParameterType"));
        othermappings.add(new CollectionTypeMapping(List.class, String.class, ArrayList.class, "trustedDomainsList"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.csrfprotection.console.CsrfProtectionAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "CSRF Protection Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CsrfProtection" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getPropertiesDialogTitle() {
        return String.valueOf(meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME));
    }

    public boolean isEnableDoubleSubmitCookieChecking() {
        return enableDoubleSubmitCookieChecking;
    }

    public void setEnableDoubleSubmitCookieChecking(boolean enableDoubleSubmitCookieChecking) {
        this.enableDoubleSubmitCookieChecking = enableDoubleSubmitCookieChecking;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public HttpParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(HttpParameterType parameterType) {
        this.parameterType = parameterType;
    }

    public boolean isEnableHttpRefererChecking() {
        return enableHttpRefererChecking;
    }

    public void setEnableHttpRefererChecking(boolean enableHttpRefererChecking) {
        this.enableHttpRefererChecking = enableHttpRefererChecking;
    }

    public boolean isAllowMissingOrEmptyReferer() {
        return allowMissingOrEmptyReferer;
    }

    public void setAllowMissingOrEmptyReferer(boolean allowMissingOrEmptyReferer) {
        this.allowMissingOrEmptyReferer = allowMissingOrEmptyReferer;
    }

    public boolean isOnlyAllowCurrentDomain() {
        return onlyAllowCurrentDomain;
    }

    public void setOnlyAllowCurrentDomain(boolean onlyAllowCurrentDomain) {
        this.onlyAllowCurrentDomain = onlyAllowCurrentDomain;
    }

    public List<String> getTrustedDomains() {
        return trustedDomains;
    }

    public void setTrustedDomains(List<String> trustedDomains) {
        this.trustedDomains.clear();
        this.trustedDomains.addAll(trustedDomains);
    }
}
