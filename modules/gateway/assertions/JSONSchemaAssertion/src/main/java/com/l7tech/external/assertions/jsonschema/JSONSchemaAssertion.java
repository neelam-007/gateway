package com.l7tech.external.assertions.jsonschema;

import com.l7tech.json.JsonSchemaVersion;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.gateway.common.cluster.ClusterProperty.asServerConfigPropertyName;

/**
 * Configuration for the 'Validate JSON Schema' assertion. JSON schema can be configured in advance or obtained from a
 * URL which can either be configured in advance or obtained via the target message, if it matches a white list.
 *
 * @author darmstrong 
 */
public class JSONSchemaAssertion extends MessageTargetableAssertion implements UsesVariables, UsesResourceInfo, SetsVariables {

    public static final String JSON_SCHEMA_FAILURE_VARIABLE = "jsonschema.failure";
    public static final String CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES = "json.schemaCache.maxEntries";
    public static final String CPROP_JSON_SCHEMA_CACHE_MAX_AGE = "json.schemaCache.maxAge";
    public static final String CPROP_JSON_SCHEMA_CACHE_MAX_STALE_AGE = "json.schemaCache.maxStaleAge";
    public static final String CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE = "json.schemaCache.maxDownloadSize";
    public static final String CPROP_JSON_SCHEMA_VERSION_STRICT = "json.schemaVersionStrict.enabled";
    public static final String PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES = asServerConfigPropertyName(CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES);
    public static final String PARAM_JSON_SCHEMA_CACHE_MAX_AGE = asServerConfigPropertyName(CPROP_JSON_SCHEMA_CACHE_MAX_AGE);
    public static final String PARAM_JSON_SCHEMA_CACHE_MAX_STALE_AGE = asServerConfigPropertyName(CPROP_JSON_SCHEMA_CACHE_MAX_STALE_AGE);
    public static final String PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE = asServerConfigPropertyName(CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE);
    public static final String PARAM_JSON_SCHEMA_VERSION_STRICT = asServerConfigPropertyName(CPROP_JSON_SCHEMA_VERSION_STRICT);

    private static final String META_INITIALIZED = JSONSchemaAssertion.class.getName() + ".metadataInitialized";
    private static final String CLASS_NAME_ADVICE = "com.l7tech.external.assertions.jsonschema.JSONSchemaAssertionAdvice";

    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.DRAFT_V2;

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(JSON_SCHEMA_FAILURE_VARIABLE, false, true, JSON_SCHEMA_FAILURE_VARIABLE,
                        false, DataType.STRING)
        );
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        String resourceExpression = null;
        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            resourceExpression = sri.getDocument();
        } else if (resourceInfo instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            resourceExpression = suri.getUrl();
        }
        return super.doGetVariablesUsed().withExpressions( resourceExpression );
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml", "threatProtection"});

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        props.put(CPROP_JSON_SCHEMA_CACHE_MAX_AGE, new String[] {
                "Maximum age of cached JSON Schema documents loaded from URLs (Milliseconds). Requires gateway restart.",
                "300000"
        });

        props.put(CPROP_JSON_SCHEMA_CACHE_MAX_STALE_AGE, new String[] {
                "Maximum age of stale (expired) cached JSON schema documents loaded from URLs, -1 for no expiry (Milliseconds). Requires gateway restart.",
                "-1"
        });

        props.put(CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES, new String[] {
                "Maximum number of cached JSON schema documents loaded from URLs, 0 for no caching (Integer). Requires gateway restart.",
                "100"
        });

        props.put(CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE, new String[] {
                "Maximum size in bytes of a JSON Schema document download, or 0 for unlimited (Integer).",
                "${documentDownload.maxSize}"
        });

        props.put(CPROP_JSON_SCHEMA_VERSION_STRICT, new String[] {
                "If true, the $schema property in the JSON schema must match with the policy configured schema version in the assertion (Boolean). Requires gateway restart",
                "false"
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate JSON Schema");
        meta.put(AssertionMetadata.LONG_NAME, "Validate JSON data against a JSON Schema");
        meta.put(AssertionMetadata.DESCRIPTION, "Validate structure, properties and values of JSON data against a JSON Schema");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "JSON Schema Validation Properties");

        // Enable policy advice to default newly dragged policies to JSON Schema version draft v4
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, CLASS_NAME_ADVICE);

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Collections.singletonList(
                new Java5EnumTypeMapping(JsonSchemaVersion.class, "jsonSchemaVersion")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    @Override
    public void setResourceInfo(final AssertionResourceInfo resourceInfo) throws IllegalArgumentException {
        this.resourceInfo = resourceInfo;
    }

    public JsonSchemaVersion getJsonSchemaVersion() {
        return jsonSchemaVersion;
    }

    public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
        this.jsonSchemaVersion = jsonSchemaVersion;
    }

}
