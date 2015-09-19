package com.l7tech.external.assertions.js;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * New secure script assertion based on Nashorn.
 */
public class JavaScriptAssertion extends Assertion {
    private String scriptBase64;

    public String decodeScript() {
        if (scriptBase64 == null)
            return null;
        if (scriptBase64.length() < 1)
            return "";
        else
            return new String( HexUtils.decodeBase64( scriptBase64 ), Charsets.UTF8);
    }

    public void encodeScript(String script) {
        this.scriptBase64 = script == null ? null : HexUtils.encodeBase64(script.getBytes(Charsets.UTF8));
    }

    public String getScriptBase64() {
        return scriptBase64;
    }

    public void setScriptBase64(String scriptBase64) {
        this.scriptBase64 = scriptBase64;
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = JavaScriptAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if ( Boolean.TRUE.equals( meta.get( META_INITIALIZED ) ) )
            return meta;

        meta.put( AssertionMetadata.SHORT_NAME, "Custom JavaScript" );
        meta.put( AssertionMetadata.DESCRIPTION, "Custom JavaScript Assertion") ;
        meta.put( AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" } );
        meta.put( AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif" );
        meta.put( AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto" );
        meta.put( AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Edit16.gif" );
        meta.put( AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.js.console.JavaScriptAssertionPropertiesDialog" );

        //meta.put( AssertionMetadata.FEATURE_SET_NAME, "(fromClass)" );

        meta.put( META_INITIALIZED, Boolean.TRUE );
        return meta;
    }
}
