package com.l7tech.external.assertions.cassandra;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.search.Dependency;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
//TODO: replace JdbcConnectionable with DataSourceConnectionable
public class CassandraQueryAssertion extends Assertion implements CassandraConnectionable, UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(CassandraQueryAssertion.class.getName());

    public static final String CONNECTION_TIMEOUT_MILLIS = "cassandra.connectTimeoutMillis";
    public static final String KEEP_ALIVE = "cassandra.keepAlive";
    public static final String RECEIVE_BUFFER_SIZE = "cassandra.receiveBufferSize";
    public static final String REUSE_ADDRESS = "cassandra.reuseAddress";
    public static final String SEND_BUFFER_SIZE = "cassandra.sendBufferSize";
    public static final String SO_LINGER = "cassandra.soLinger";
    public static final String TCP_NO_DELAY = "cassandra.tcpNoDelay";
    public static final String DEFAULT_QUERY_PREFIX = "cassandraQuery";
    public static final String QUERYRESULT_COUNT = ".queryresult.count";
    public static final String VARIABLE_XML_RESULT = ".xmlResult";
    public static final int MAX_RECORDS_DEF = 10;

    private String connectionName;
    private String queryDocument;
    private boolean failIfNoResults = false;
    private boolean isGenerateXmlResult = false;
    private int fetchSize = MAX_RECORDS_DEF;
    private Map<String, String> namingMap = new HashMap<>();
    private long queryTimeout = 0;

    String prefix = DEFAULT_QUERY_PREFIX;
    //
    // Metadata
    //
    private static final String META_INITIALIZED = CassandraQueryAssertion.class.getName() + ".metadataInitialized";

    public String getQueryDocument() {
        return queryDocument;
    }

    public void setQueryDocument(String queryDocument) {
        this.queryDocument = queryDocument;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.CASSANDRA_CONNECTION)
    @Dependency(type = Dependency.DependencyType.CASSANDRA_CONNECTION, methodReturnType = Dependency.MethodReturnType.NAME)
    public String getConnectionName() {
        return connectionName;
    }

    @Override
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public boolean isFailIfNoResults() {
        return failIfNoResults;
    }

    public void setFailIfNoResults(boolean failIfNoResults) {
        this.failIfNoResults = failIfNoResults;
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getNamingMap() {
        return namingMap;
    }

    public void setNamingMap(Map<String, String> namedMap) {
        this.namingMap = namedMap;
    }

    public long getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(long queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public boolean isGenerateXmlResult() {
        return isGenerateXmlResult;
    }

    public void setGenerateXmlResult(boolean isGenerateXmlResult) {
        this.isGenerateXmlResult = isGenerateXmlResult;
    }
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(queryDocument);
    }

    @Override
       public VariableMetadata[] getVariablesSet() {
           List<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();
           varMeta.add(new VariableMetadata(prefix + QUERYRESULT_COUNT, false, false, null, false, DataType.INTEGER));
           Set<String> varSet = new HashSet<>();
           for(String var : namingMap.values()) {
                varSet.add(var);
           }
           for(String varName : varSet) {
                varMeta.add(new VariableMetadata(prefix + "." + varName, false, true, null, false, DataType.STRING));
           }

           return varMeta.toArray(new VariableMetadata[varMeta.size()]);
       }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CONNECTION_TIMEOUT_MILLIS, new String[] {
                "The connect timeout in milliseconds for the underlying Netty channel.",
                "n/a"
        });

        props.put(REUSE_ADDRESS, new String[] {
                "Whether to allow the same port to be bound to multiple times.",
                "n/a"
        });

        props.put(SEND_BUFFER_SIZE, new String[]{
                "A hint on the size of the buffer used to send data.",
                "n/a"
        } );

        props.put(RECEIVE_BUFFER_SIZE, new String[]{
                "A hint on the size of the buffer used to receive data.",
                "n/a"
        } );

        props.put(SO_LINGER, new String[]{
                "When specified, disables the immediate return from a call to close() on a TCP socket.",
                "n/a"
        } );

        props.put(TCP_NO_DELAY, new String[]{
                "Disables Nagle's algorithm on the underlying socket.",
                "n/a"
        } );


        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");


//        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.cassandra.CassandraAssertionModuleLoadListener");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.cassandra.console.CassandraAssertionPropertiesDialog");

        Collection<TypeMapping> othermappings = new ArrayList<>();
        othermappings.add(new BeanTypeMapping(CassandraNamedParameter.class, "namedParameter"));
        othermappings.add(new CollectionTypeMapping(List.class, CassandraNamedParameter.class , ArrayList.class , "namedParameterList"));
        othermappings.add(new BeanTypeMapping(com.datastax.driver.core.DataType.Name.class, "parameterDataType"));

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private final static String baseName = "Perform Cassandra Query";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<CassandraQueryAssertion>(){
            @Override
            public String getAssertionName( final CassandraQueryAssertion assertion, final boolean decorate) {
                if(!decorate) return baseName;

                StringBuilder builder= new StringBuilder(baseName);
                builder.append(": ").append(assertion.getQueryDocument());

                return builder.toString();
            }
        };


    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }
}
