package com.l7tech.external.assertions.mtom;

import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 
 */
public class MtomEncodeAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    //- PUBLIC

    public static void main( String[] args ) {
        System.out.println( new MtomEncodeAssertion().getFeatureSetName() );        
    }

    public boolean isAlwaysEncode() {
        return alwaysEncode;
    }

    public void setAlwaysEncode( final boolean alwaysEncode ) {
        this.alwaysEncode = alwaysEncode;
    }

    public boolean isFailIfNotFound() {
        return failIfNotFound;
    }

    public void setFailIfNotFound( final boolean failIfNotFound ) {
        this.failIfNotFound = failIfNotFound;
    }

    public int getOptimizationThreshold() {
        return optimizationThreshold;
    }

    public void setOptimizationThreshold( final int optimizationThreshold ) {
        this.optimizationThreshold = optimizationThreshold;
    }

    public XpathExpression[] getXpathExpressions() {
        return xpathExpressions;
    }

    public void setXpathExpressions( final XpathExpression[] xpathExpressions ) {
        this.xpathExpressions = xpathExpressions;
    }

    public MessageTargetableSupport getOutputTarget() {
        return outputTarget;
    }

    public void setOutputTarget( final MessageTargetableSupport outputTarget ) {
        this.outputTarget = outputTarget;
    }

    @Override
    public String[] getVariablesUsed() {
        final Set<String> variables = new LinkedHashSet<String>(Arrays.asList(super.getVariablesUsed()));

        if ( xpathExpressions != null ) {
            for ( final XpathExpression xpathExpression : xpathExpressions ) {
                if ( xpathExpression == null ) continue;

                final String expression = xpathExpression.getExpression();
                if ( expression != null ) {
                    variables.addAll( XpathUtil.getUnprefixedVariablesUsedInXpath( expression ) );
                }
            }
        }

        return variables.toArray(new String[variables.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] variables;

        if ( outputTarget!=null && outputTarget.getTarget()== TargetMessageType.OTHER ) {
            String name = outputTarget.getOtherTargetMessageVariable();
            variables = new VariableMetadata[]{ new VariableMetadata(name, false, false, name, true, DataType.MESSAGE) };
        } else {
            variables = new VariableMetadata[0];
        }

        return variables;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Encode to MTOM Format");
        meta.put(PROPERTIES_ACTION_NAME, "MTOM Encode Properties");
        meta.put(DESCRIPTION, "Convert a SOAP message to Optimized MIME Multipart/Related Serialization (MTOM) format.");
        meta.put(WSP_EXTERNAL_NAME, "MTOMEncodeAssertion");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mtom.console.MtomEncodeAssertionPropertiesDialog");

        // These are really module metadata rather than for this assertion
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mtom.server.MtomModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = MtomEncodeAssertion.class.getName() + ".metadataInitialized";

    private MessageTargetableSupport outputTarget;
    private int optimizationThreshold = 0;
    private boolean alwaysEncode = true;
    private boolean failIfNotFound;
    private XpathExpression[] xpathExpressions;
}
