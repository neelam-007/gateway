package com.l7tech.external.assertions.jsonschema;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class JSONSchemaAssertion extends MessageTargetableAssertion implements UsesVariables, UsesResourceInfo, SetsVariables {
    public static final String JSON_SCHEMA_FAILURE_VARIABLE = "jsonschema.failure";

    public static final String CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES = "json.schemaCache.maxEntries";
    public static final String CPROP_JSON_SCHEMA_CACHE_MAX_AGE = "json.schemaCache.maxAge";
    public static final String CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE = "json.schemaCache.maxDownloadSize";
    public static final String PARAM_JSON_SCHEMA_CACHE_MAX_ENTRIES = ClusterProperty.asServerConfigPropertyName(CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES);
    public static final String PARAM_JSON_SCHEMA_CACHE_MAX_AGE = ClusterProperty.asServerConfigPropertyName(CPROP_JSON_SCHEMA_CACHE_MAX_AGE);
    public static final String PARAM_JSON_SCHEMA_MAX_DOWNLOAD_SIZE = ClusterProperty.asServerConfigPropertyName(CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE);

    @Override
    public VariableMetadata[] getVariablesSet() {
        return mergeVariablesSet(new VariableMetadata[] {
                new VariableMetadata(JSON_SCHEMA_FAILURE_VARIABLE, false, true, JSON_SCHEMA_FAILURE_VARIABLE, false, DataType.STRING)
        });
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> variables = new ArrayList<String>();

        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            String doc = sri.getDocument();
            if (doc != null) {
                variables.addAll(Arrays.asList(Syntax.getReferencedNames(doc)));
            }

        }

        if ( getTarget() == TargetMessageType.OTHER ) {
            variables.add(getOtherTargetMessageVariable());
        }

        return variables.toArray( new String[variables.size()] );
    }


    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml", "threatProtection"});

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CPROP_JSON_SCHEMA_CACHE_MAX_AGE, new String[] {
                "Maximum age of cached JSON Schema documents loaded from URLs (Milliseconds). Requires gateway restart.",
                "30000"
        });

        props.put(CPROP_JSON_SCHEMA_CACHE_MAX_ENTRIES, new String[] {
                "Maximum number of cached JSON schema documents loaded from URLs, 0 for no caching (Integer).",
                "100"
        });

        props.put(CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE, new String[] {
                "Maximum size in bytes of a JSON Schema document download, or 0 for unlimited (Integer)",
                "${documentDownload.maxSize}"
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Validate JSON Schema");
        meta.put(AssertionMetadata.LONG_NAME, "Validate JSON data against a JSON Schema");
        meta.put(AssertionMetadata.DESCRIPTION, "Validate structure, properties and values of JSON data against a JSON Schema");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "JSON Schema Validation Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    @Override
    public void setResourceInfo(AssertionResourceInfo resourceInfo) throws IllegalArgumentException {
        this.resourceInfo = resourceInfo;
    }

    // - PRIVATE
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private static final String META_INITIALIZED = JSONSchemaAssertion.class.getName() + ".metadataInitialized";
    private static final Logger logger = Logger.getLogger(JSONSchemaAssertion.class.getName());
}
