package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.xml.SupportedDigestMethods;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspUpgradeUtilFrom21;
import com.l7tech.util.Functions;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import java.util.*;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class RequireWssSignedElement extends XmlSecurityAssertionBase implements IdentityTargetable, SetsVariables, UsesEntities {

    //- PUBLIC

    public static final String VAR_SIGNATURE_ELEMENT = "element";
    public static final String VAR_TOKEN_TYPE = "token.type";
    public static final String VAR_TOKEN_ELEMENT = "token.element";
    public static final String VAR_TOKEN_ATTRIBUTES = "token.attributes";

    public RequireWssSignedElement() {
        this( compatOrigDefaultXpathValue() );
    }

    public RequireWssSignedElement(XpathExpression xpath) {
        super( TargetMessageType.REQUEST, false );
        setXpathExpression(xpath);
        List<String> allDigestIds = SupportedDigestMethods.getAlgorithmIds();
        setAcceptedDigestAlgorithms(allDigestIds.toArray(new String[allDigestIds.size()]));
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget(IdentityTarget identityTarget) {
        this.identityTarget = identityTarget;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public String getSignedElementsVariable() {
        return signedElementsVariable;
    }

    public void setSignedElementsVariable(String signedElementsVariable) {
        this.signedElementsVariable = signedElementsVariable;
    }

    public String[] getVariableSuffixes() {
        return new String[] {
            VAR_SIGNATURE_ELEMENT,
            VAR_TOKEN_TYPE,
            VAR_TOKEN_ELEMENT,
            VAR_TOKEN_ATTRIBUTES
        };
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }

    /**
     * @param digestAlgorithmId message digest algorithm identifier
     * @return true if the provided digest algorithm is accepted by this assertion, false otherwise 
     */
    public boolean acceptsDigest(String digestAlgorithmId) {
        return acceptedDigests.contains(digestAlgorithmId);
    }

    public void setAcceptedDigestAlgorithms(String[] accepted) {
        if (accepted != null) {
            Set<String> acceptedDigests = new LinkedHashSet<String>();
            for (String digest : accepted) {
                acceptedDigests.add(SupportedDigestMethods.fromAlgorithmId(digest).getIdentifier()); // make sure it's supported
            }
            this.acceptedDigests = acceptedDigests;
        }
    }

    public String[] getAcceptedDigestAlgorithms() {
        return acceptedDigests == null ? new String[0] : acceptedDigests.toArray(new String[acceptedDigests.size()]);
    }

    final static String baseName = "Require Signed Element";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequireWssSignedElement>(){
        @Override
        public String getAssertionName( final RequireWssSignedElement assertion, final boolean decorate) {
            StringBuilder name = new StringBuilder(baseName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            return (decorate)? AssertionUtils.decorateName(assertion, name): baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The message must contain one or more signed elements.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 100000);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Signed Element Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new XpathBasedAssertionFactory<RequireWssSignedElement>(RequireWssSignedElement.class));
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, RequireWssSignedElement>(){
            @Override
            public Set<ValidatorFlag> call(RequireWssSignedElement assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION, ValidatorFlag.REQUIRE_SIGNATURE);
            }
        });
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put(WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping.getExternalName(),
                WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping);
        }});
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssIntegrity");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.XpathBasedAssertionValidator");

        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesSet doGetVariablesSet() {
        final VariablesSet set = super.doGetVariablesSet();
        if ( isSetVariables() ){
            set.addVariables(
                new VariableMetadata(prefixVariable(VAR_SIGNATURE_ELEMENT), false, false, prefixVariable(VAR_SIGNATURE_ELEMENT), false, DataType.ELEMENT),
                new VariableMetadata(prefixVariable(VAR_TOKEN_TYPE), false, false, prefixVariable(VAR_TOKEN_TYPE), false),
                new VariableMetadata(prefixVariable(VAR_TOKEN_ELEMENT), false, false, prefixVariable(VAR_TOKEN_ELEMENT), false, DataType.ELEMENT),
                new VariableMetadata(prefixVariable(VAR_TOKEN_ATTRIBUTES), false, false, prefixVariable(VAR_TOKEN_ATTRIBUTES), false)
            );
        }
        return set;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withVariables( signedElementsVariable );
    }

    //- PRIVATE

    private String variablePrefix;
    private String signedElementsVariable;
    private IdentityTarget identityTarget;
    private Set<String> acceptedDigests;

    private boolean isSetVariables() {
        return variablePrefix != null && variablePrefix.length() > 0;
    }

    private String prefixVariable( final String variableName ) {
        return VariableMetadata.prefixName( variablePrefix, variableName );
    }
}
