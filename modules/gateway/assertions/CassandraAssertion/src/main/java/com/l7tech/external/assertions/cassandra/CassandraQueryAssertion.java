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

    public static final String CORE_CONNECTION_PER_HOST = "cassandra.coreConnectionsPerHost";
    public static final String MAX_CONNECTION_PER_HOST = "cassandra.maxConnectionPerHost";
    public static final String MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "cassandra.maxSimultaneousRequestsPerConnectionThreshold";
    public static final String MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "cassandra.minSimultaneousRequestsPerConnectionThreshold";

    public static final int CORE_CONNECTION_PER_HOST_DEF = 1;
    public static final int MAX_CONNECTION_PER_HOST_DEF = 2;
    public static final int MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 128;
    public static final int MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD_DEF = 25;

    public static final String CONNECTION_TIMEOUT_MILLIS = "cassandra.connectTimeoutMillis";
    public static final String KEEP_ALIVE = "cassandra.keepAlive";
    public static final String RECEIVE_BUFFER_SIZE = "cassandra.receiveBufferSize";
    public static final String REUSE_ADDRESS = "cassandra.reuseAddress";
    public static final String SEND_BUFFER_SIZE = "cassandra.sendBufferSize";
    public static final String SO_LINGER = "cassandra.soLinger";
    public static final String TCP_NO_DELAY = "cassandra.tcpNoDelay";
    public static final String DEFAULT_QUERY_PREFIX = "cassandraQuery";
    public static final String QUERYRESULT_COUNT = ".queryresult.count";

    private String connectionName;
    private String queryDocument;
    private boolean failIfNoResults = false;
    String prefix = DEFAULT_QUERY_PREFIX;

    List<CassandraNamedParameter> namedParameterList = new ArrayList<>();

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

    public List<CassandraNamedParameter> getNamedParameterList() {
        return namedParameterList;
    }

    public void setNamedParameterList(List<CassandraNamedParameter> namedParameterList) {
        this.namedParameterList = namedParameterList;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(queryDocument);
    }

    @Override
       public VariableMetadata[] getVariablesSet() {
           List<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();
           varMeta.add(new VariableMetadata(prefix + QUERYRESULT_COUNT, false, false, null, false, DataType.INTEGER));

           return varMeta.toArray(new VariableMetadata[varMeta.size()]);
       }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CORE_CONNECTION_PER_HOST, new String[] {
                "The core number of connections per host.",
                "1"
        });

        props.put(MAX_CONNECTION_PER_HOST, new String[] {
                "The maximum number of connections per host.",
                "2"
        });

        props.put(MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD, new String[] {
                "The number of simultaneous requests on all connections to an host after which more connections are created.",
                "128"
        });

        props.put(MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD, new String[]{
                "The number of simultaneous requests on a connection below which connections in excess are reclaimed.",
                "25"
        } );

        props.put(CONNECTION_TIMEOUT_MILLIS, new String[] {
                "The connect timeout in milliseconds for the underlying Netty channel.",
                "n/a"
        });

        props.put(KEEP_ALIVE, new String[] {
                "",
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

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:MongoDB" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
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



}
