package com.l7tech.external.assertions.jsonjolt;

import com.l7tech.policy.assertion.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class JsonJoltAssertion extends MessageTargetableAssertion {
    protected static final Logger logger = Logger.getLogger(JsonJoltAssertion.class.getName());
    private static final String META_INITIALIZED = JsonJoltAssertion.class.getName() + ".metadataInitialized";

    private String schemaExpression;

    public JsonJoltAssertion() {
        super( true );
    }

    public String getSchemaExpression() {
        return schemaExpression;
    }

    public void setSchemaExpression( String schemaExpression ) {
        this.schemaExpression = schemaExpression;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( schemaExpression );
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Transform JSON using JOLT");
        meta.put(AssertionMetadata.LONG_NAME, "Transform a JSON message into a different JSON message using a JOLT transform.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put( AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jsonjolt.console.JsonJoltAssertionPropertiesDialog" );

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
