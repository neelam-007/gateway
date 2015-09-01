package com.l7tech.external.assertions.odata;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.MapTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.TimeUnit;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
public class ODataProducerAssertion extends Assertion implements JdbcConnectionable, UsesVariables, SetsVariables {

    public static final String ASSERTION_SHORT_NAME = "OData JDBC Producer ";
    private static final String META_INITIALIZED = ODataProducerAssertion.class.getName() + ".metadataInitialized";

    private String connectionName;
    //http://www.odata.org/documentation/odata-v2-documentation/uri-conventions/#1_URI_Components
    public static final String ODATA_ROOT_URI = "${odata.serviceRootUri}";
    public static final String ODATA_RESOURCE_PATH = "${odata.resourcePath}";
    public static final String ODATA_SERVICE_PATH_INDEX = "${odata.servicePathIndex}";
    public static final String ODATA_HTTP_METHOD = "${odata.httpMethod}";
    public static final String ODATA_QUERY_OPTIONS = "${odata.queryOptions}";//options are parsed into parameters
    public static final String ODATA_SHOW_INLINE_ERROR = "${odata.showInlineError}";
    //batch related
    public static final String ODATA_ALLOW_ANY_REQUEST_BODY_FOR_BATCH = "${odata.allowAnyRequestBodyForBatch}";
    public static final String ODATA_BATCH_TRANSACTION = "${odata.batch.transaction}";//true(default) or false
    public static final String ODATA_BATCH_FAST_FAIL = "${odata.batch.fastfail}";//true(default) or false, ignored when transaction is true
    public static final String ODATA_BATCH_LAST_ENTITY_NAME = "odata.batch.last.operation.entityName";
    public static final String ODATA_BATCH_LAST_ENTITY_ID = "odata.batch.last.operation.entityId";
    public static final String ODATA_BATCH_LAST_METHOD = "odata.batch.last.operation.method";
    public static final String ODATA_BATCH_LAST_STATUS = "odata.batch.last.operation.status";
    public static final String ODATA_BATCH_LAST_BODY = "odata.batch.last.operation.body";
    public static final String ODATA_BATCH_LAST_PAYLOAD = "odata.batch.last.operation.payload";
    public static final String ODATA_BATCH_REQUEST_COUNT = "odata.batch.request.count";
    public static final String ODATA_BATCH_HAS_ERROR = "odata.batch.hasError";
    //custom entity support
    public static final String ODATA_CUSTOM_ENTITIES = "${odata.customEntitySet}";
    //http://www.odata.org/documentation/odata-v2-documentation/uri-conventions/#4_Query_String_Options
    public static final String ODATA_PARAM_INLINECOUNT = "${odata.param.inlinecount}";//$inlinecount
    public static final String ODATA_PARAM_TOP = "${odata.param.top}";//$top
    public static final String ODATA_PARAM_SKIP = "${odata.param.skip}";//$skip
    public static final String ODATA_PARAM_FILTER = "${odata.param.filter}";//$filter
    public static final String ODATA_PARAM_ORDERBY = "${odata.param.orderby}";//$orderby
    public static final String ODATA_PARAM_FORMAT = "${odata.param.format}";//$format
    public static final String ODATA_PARAM_CALLBACK = "${odata.param.callback}";//$callback
    public static final String ODATA_PARAM_SKIPTOKEN = "${odata.param.skiptoken}";//$skiptoken
    public static final String ODATA_PARAM_EXPAND = "${odata.param.expand}";//$expand
    public static final String ODATA_PARAM_SELECT = "${odata.param.select}";//$select

    public static final String CACHE_WIPE_INTERVAL = "com.l7tech.odataJdbcProducer.modelCache.cacheWipeInterval";
    public static final String PARAM_CACHE_WIPE_INTERVAL = ClusterProperty.asServerConfigPropertyName(CACHE_WIPE_INTERVAL);
    public static final long CACHE_WIPE_INTERVAL_DEFAULT = TimeUnit.DAYS.toMillis(1);

    public ODataProducerAssertion() {
    }


    @Override
    public ODataProducerAssertion clone() {
        ODataProducerAssertion copy = (ODataProducerAssertion) super.clone();

        copy.setConnectionName(connectionName);

        return copy;
    }

    public void copyFrom(final ODataProducerAssertion source) {
        setConnectionName(source.getConnectionName());

    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.JDBC_CONNECTION)
    public String getConnectionName() {
        return connectionName;
    }

    @Override
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(ODATA_BATCH_REQUEST_COUNT, false, false, null, false, DataType.INTEGER),
                new VariableMetadata(ODATA_BATCH_LAST_STATUS, false, false, null, false, DataType.INTEGER),
                new VariableMetadata(ODATA_BATCH_LAST_ENTITY_NAME), new VariableMetadata(ODATA_BATCH_LAST_ENTITY_ID),
                new VariableMetadata(ODATA_BATCH_LAST_METHOD),
                new VariableMetadata(ODATA_BATCH_LAST_BODY), new VariableMetadata(ODATA_BATCH_LAST_PAYLOAD),
                new VariableMetadata(ODATA_BATCH_HAS_ERROR, false, false, null, false, DataType.BOOLEAN)};
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(connectionName);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_SHORT_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Expose a database via a JDBC connection as OData");

        final Map<String, String[]> props = new HashMap<>();
        props.put(CACHE_WIPE_INTERVAL, new String[]{"The interval between OData cache wipes.", String.valueOf(CACHE_WIPE_INTERVAL_DEFAULT)});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, Collections.singletonMap(CACHE_WIPE_INTERVAL, new String[]{"The interval between OData cache wipes.", String.valueOf(CACHE_WIPE_INTERVAL_DEFAULT)}));

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.odata.console.ODataProducerAssertionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "OData JDBC Producer Properties");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:OData" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(new MapTypeMapping())));

        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.odata.server.ServerODataProducerAssertion");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    private Map<String, String> copyMap(Map<String, String> sourceMap) {

        Map<String, String> destMap = new TreeMap<String, String>();
        for (String key : sourceMap.keySet()) {
            destMap.put(key, sourceMap.get(key));
        }
        return destMap;
    }
}