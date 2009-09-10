package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspUpgradeUtilFrom21;
import com.l7tech.util.Functions;
import com.l7tech.policy.variable.VariableMetadata;
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
        this(XpathExpression.soapBodyXpathValue());
    }

    public RequireWssSignedElement(XpathExpression xpath) {
        super( TargetMessageType.REQUEST );
        setXpathExpression(xpath);
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

    @Override
    public VariableMetadata[] getVariablesSet() {
        final List<VariableMetadata> vars = new ArrayList<VariableMetadata>();
        if ( isSetVariables() ){
            vars.add(new VariableMetadata(prefixVariable(VAR_SIGNATURE_ELEMENT), false, false, prefixVariable(VAR_SIGNATURE_ELEMENT), false));
            vars.add(new VariableMetadata(prefixVariable(VAR_TOKEN_TYPE), false, false, prefixVariable(VAR_TOKEN_TYPE), false));
            vars.add(new VariableMetadata(prefixVariable(VAR_TOKEN_ELEMENT), false, false, prefixVariable(VAR_TOKEN_ELEMENT), false));
            vars.add(new VariableMetadata(prefixVariable(VAR_TOKEN_ATTRIBUTES), false, false, prefixVariable(VAR_TOKEN_ATTRIBUTES), false));
        }
        return vars.toArray(new VariableMetadata[vars.size()]);
    }

    @Override
    public String[] getVariablesUsed() {
        String[] varsUsed = super.getVariablesUsed();

        if ( signedElementsVariable != null ) {
            String[] allVarsUsed = new String[ varsUsed.length+1 ];
            System.arraycopy( varsUsed, 0, allVarsUsed, 0, varsUsed.length );
            allVarsUsed[varsUsed.length] = signedElementsVariable;
            varsUsed = allVarsUsed;
        }

        return varsUsed;
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        final String assertionName = "Require Signed Element";
        meta.put(AssertionMetadata.SHORT_NAME, assertionName);
        meta.put(AssertionMetadata.DESCRIPTION, "The message must contain one or more signed elements.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 100000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Binary<String, RequireWssSignedElement, Boolean>() {
            @Override
            public String call(final RequireWssSignedElement requestWssIntegrity, final Boolean decorate) {
                StringBuilder name = new StringBuilder(assertionName + " ");
                if (requestWssIntegrity.getXpathExpression() == null) {
                    name.append("[XPath expression not set]");
                } else {
                    name.append(requestWssIntegrity.getXpathExpression().getExpression());
                }
                return (decorate)? AssertionUtils.decorateName(requestWssIntegrity, name): assertionName;
            }
        });
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
        meta.put(AssertionMetadata.PALETTE_NODE_CLIENT_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.XpathBasedAssertionValidator");

        return meta;
    }

    //- PRIVATE

    private String variablePrefix;
    private String signedElementsVariable;
    private IdentityTarget identityTarget;

    private boolean isSetVariables() {
        return variablePrefix != null && variablePrefix.length() > 0;        
    }

    private String prefixVariable( final String variableName ) {
        return VariableMetadata.prefixName( variablePrefix, variableName );
    }
}
