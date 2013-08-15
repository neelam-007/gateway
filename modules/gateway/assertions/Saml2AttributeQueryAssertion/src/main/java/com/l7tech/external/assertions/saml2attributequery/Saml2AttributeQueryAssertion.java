package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Saml2AttributeQueryAssertion extends RoutingAssertion implements UsesVariables {
    //- PUBLIC

    /**
     * Bean constructor
     */
    public Saml2AttributeQueryAssertion() {
        setCurrentSecurityHeaderHandling(IGNORE_SECURITY_HEADER);
    }

    public String[] getVariablesUsed() {
        return new String[] {idContextVariable};
    }

    public Goid getLdapProviderOid() {
        return ldapProviderOid;
    }

    public void setLdapProviderOid(Goid ldapProviderOid) {
        this.ldapProviderOid = ldapProviderOid;
    }

    // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.
    @Deprecated
    public void setLdapProviderOid( long ldapProviderOid ) {
        this.ldapProviderOid = (ldapProviderOid == -2) ?
                new Goid(0,-2L):
                GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG, ldapProviderOid);
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public String getIdContextVariable() {
        return idContextVariable;
    }

    public void setIdContextVariable(String idContextVariable) {
        this.idContextVariable = idContextVariable;
    }

    public String getMapClusterProperty() {
        return mapClusterProperty;
    }

    public void setMapClusterProperty(String mapClusterProperty) {
        this.mapClusterProperty = mapClusterProperty;
    }

    public boolean isWhiteList() {
        return whiteList;
    }

    public void setWhiteList(boolean whiteList) {
        this.whiteList = whiteList;
    }

    public List<String> getRestrictedAttributeList() {
        return restrictedAttributeList;
    }

    public void setRestrictedAttributeList(List<String> restrictedAttributeList) {
        this.restrictedAttributeList = restrictedAttributeList;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public int getConditionsNotBeforeSecondsInPast() {
        return conditionsNotBeforeSecondsInPast;
    }

    public void setConditionsNotBeforeSecondsInPast(int conditionsNotBeforeSecondsInPast) {
        this.conditionsNotBeforeSecondsInPast = conditionsNotBeforeSecondsInPast;
    }

    public int getConditionsNotOnOrAfterExpirySeconds() {
        return conditionsNotOnOrAfterExpirySeconds;
    }

    public void setConditionsNotOnOrAfterExpirySeconds(int conditionsNotOnOrAfterExpirySeconds) {
        this.conditionsNotOnOrAfterExpirySeconds = conditionsNotOnOrAfterExpirySeconds;
    }

    public String getAudienceRestriction() {
        return audienceRestriction;
    }

    public void setAudienceRestriction(String audienceRestriction) {
        this.audienceRestriction = audienceRestriction;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return true;
    }

    @Override
    public boolean initializesResponse() {
        return true;
    }

    @Override
    public boolean needsInitializedResponse() {
        return false;
    }

    /**
     * Get metadata for this assertion.
     *
     * @return The assertion metadata.
     */
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "SAML2 Attribute Query");
        meta.put(AssertionMetadata.LONG_NAME, "SAML2 Attribute Query");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Disable automatic properties editor
        //meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.saml2attributequery.console.Saml2AttributeQueryAssertionPropertiesEditor");

        // Disable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:CertificateAttributes" rather than "set:modularAssertions"

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


    //- PRIVATE

    private Goid ldapProviderOid = null;
    private String idFieldName = null;
    private String idContextVariable = null;
    private String mapClusterProperty = null;
    private boolean whiteList = false;
    private List<String> restrictedAttributeList = new ArrayList<String>();
    private String issuer = null;
    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    private String audienceRestriction;
    
    private static final String META_INITIALIZED = Saml2AttributeQueryAssertion.class.getName() + ".metadataInitialized";
}