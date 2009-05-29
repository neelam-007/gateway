package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspUpgradeUtilFrom21;
import com.l7tech.util.Functions;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.*;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class RequestWssIntegrity extends XmlSecurityAssertionBase implements IdentityTargetable, SetsVariables {

    //- PUBLIC

    public static final String VAR_SIGNATURE_ELEMENT = "element";
    public static final String VAR_TOKEN_TYPE = "token.type";
    public static final String VAR_TOKEN_ELEMENT = "token.element";
    public static final String VAR_TOKEN_ATTRIBUTES = "token.attributes";

    public RequestWssIntegrity() {
        this(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssIntegrity(XpathExpression xpath) {
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
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Require Signed Element");
        meta.put(AssertionMetadata.DESCRIPTION, "The message must contain one or more signed elements.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 100000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, RequestWssIntegrity>() {
            @Override
            public String call( final RequestWssIntegrity requestWssIntegrity ) {
                StringBuilder name = new StringBuilder("Require signed element ");
                if (requestWssIntegrity.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(requestWssIntegrity.getXpathExpression().getExpression());
                }
                return AssertionUtils.decorateName(requestWssIntegrity, name);
            }
        });
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put(WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping.getExternalName(),
                WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping);            
        }});

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
