package com.l7tech.external.assertions.mtom;

import com.l7tech.policy.assertion.*;
import com.l7tech.xml.NamespaceMigratable;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
public class MtomEncodeAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables, NamespaceMigratable {

    //- PUBLIC

    public MtomEncodeAssertion() {
        // Primary target is only read; outputTarget is modified
        super(false);
    }

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
        if (outputTarget != null)
            outputTarget.setTargetModifiedByGateway(true);
    }

    @Override
    public void migrateNamespaces(Map<String, String> nsUriSourceToDest) {
        if (xpathExpressions != null) {
            for (XpathExpression xpathExpression : xpathExpressions) {
                if (xpathExpression != null)
                    xpathExpression.migrateNamespaces(nsUriSourceToDest);
            }
        }
    }

    @Override
    public Set<String> findNamespaceUrisUsed() {
        Set<String> ret = new HashSet<String>();
        if (xpathExpressions != null) {
            for (XpathExpression xpathExpression : xpathExpressions) {
                if (xpathExpression != null)
                    ret.addAll(xpathExpression.findNamespaceUrisUsed());
            }
        }
        return ret;
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
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.NamespaceMigratableAssertionValidator");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mtom.console.MtomEncodeAssertionPropertiesDialog");

        // These are really module metadata rather than for this assertion
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mtom.server.MtomModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed variablesUsed = super.doGetVariablesUsed();

        if ( xpathExpressions != null ) {
            for ( final XpathExpression xpathExpression : xpathExpressions ) {
                if ( xpathExpression == null ) continue;

                final String expression = xpathExpression.getExpression();
                if ( expression != null ) {
                    variablesUsed.addVariables( XpathUtil.getUnprefixedVariablesUsedInXpath( expression ) );
                }
            }
        }

        return variablesUsed;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().with( outputTarget==null ? null : outputTarget.getMessageTargetVariablesSet() );
    }

    //- PRIVATE

    private static final String META_INITIALIZED = MtomEncodeAssertion.class.getName() + ".metadataInitialized";

    private MessageTargetableSupport outputTarget;
    private int optimizationThreshold = 0;
    private boolean alwaysEncode = true;
    private boolean failIfNotFound;
    private XpathExpression[] xpathExpressions;
}
