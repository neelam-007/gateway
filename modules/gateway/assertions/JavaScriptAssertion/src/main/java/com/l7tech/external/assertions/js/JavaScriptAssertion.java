package com.l7tech.external.assertions.js;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import javax.xml.bind.annotation.XmlValue;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.DEFAULT_EXECUTION_TIMEOUT_STRING;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Basic JavaScript Execution Assertion.
 */
public class JavaScriptAssertion extends Assertion implements UsesVariables {

    private static final String META_INITIALIZED = JavaScriptAssertion.class.getName() + ".metadataInitialized";

    private String script;
    private boolean strictModeEnabled = true;
    private String executionTimeout = DEFAULT_EXECUTION_TIMEOUT_STRING;

    @XmlValue
    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Deprecated
    public void setScriptBase64(String scriptBase64) {
        setScript(new String(HexUtils.decodeBase64(scriptBase64), Charsets.UTF8));
    }

    public boolean isStrictModeEnabled() {
        return strictModeEnabled;
    }

    public void setStrictModeEnabled(boolean strictModeEnabled) {
        this.strictModeEnabled = strictModeEnabled;
    }

    public String getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(String executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // TODO : we might need to retrieve used variables from script too.
        return Syntax.getReferencedNames(getExecutionTimeout());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if ( Boolean.TRUE.equals( meta.get( META_INITIALIZED ) ) )
            return meta;

        meta.put( SHORT_NAME, "Execute JavaScript" );
        meta.put( DESCRIPTION, "To execute Javascript code inside the Gateway's policy context.") ;
        meta.put( PALETTE_FOLDERS, new String[] { "misc" } );
        meta.put( PALETTE_NODE_ICON, "com/l7tech/resources/json.gif" );
        meta.put( POLICY_ADVICE_CLASSNAME, "auto" );
        meta.put( POLICY_NODE_ICON, "com/l7tech/resources/json.gif" );
        meta.put( PROPERTIES_ACTION_NAME, "JavaScript Execution Properties");
        meta.put( PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.js.console.JavaScriptAssertionPropertiesDialog" );
        meta.put( MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.js.server.JavaScriptModuleLoaderListener" );

        meta.put( META_INITIALIZED, Boolean.TRUE );
        return meta;
    }

}
