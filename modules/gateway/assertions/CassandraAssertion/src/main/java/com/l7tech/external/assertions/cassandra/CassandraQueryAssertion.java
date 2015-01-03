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
public class CassandraQueryAssertion extends Assertion implements CassandraConnectionable, UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(CassandraQueryAssertion.class.getName());

    public static final String DEFAULT_QUERY_PREFIX = "cassandraQuery";
    public static final String QUERYRESULT_COUNT = ".queryresult.count";
    public static final String VARIABLE_XML_RESULT = ".xmlResult";
    public static final int DEFAULT_MAX_RECORDS = 10;
    public static final int DEFAULT_FETCH_SIZE = 5000;
    private static final int MAX_DISPLAY_LENGTH = 60;

    private String connectionName;
    private String queryDocument;
    private boolean failIfNoResults = false;
    private boolean isGenerateXmlResult = false;
    private int fetchSize = DEFAULT_FETCH_SIZE;
    private int maxRecords = DEFAULT_MAX_RECORDS;
    private Map<String, String> namingMap = new HashMap<>();
    private String queryTimeout = "0";

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

    public String getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(String queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public boolean isGenerateXmlResult() {
        return isGenerateXmlResult;
    }

    public void setGenerateXmlResult(boolean isGenerateXmlResult) {
        this.isGenerateXmlResult = isGenerateXmlResult;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }


    public int getMaxRecords() {
        return maxRecords;
    }

    public void setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(queryDocument, queryTimeout);
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
           if(isGenerateXmlResult) {
               varMeta.add(new VariableMetadata(prefix + VARIABLE_XML_RESULT, false, false, null, false, DataType.STRING));
           }
           return varMeta.toArray(new VariableMetadata[varMeta.size()]);
       }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

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
                builder.append(": ");
                String query = assertion.getQueryDocument();
                if(query.length() > MAX_DISPLAY_LENGTH) {
                    builder.append(query.substring(0, MAX_DISPLAY_LENGTH - 1)).append("...");
                }
                else {
                    builder.append(query);
                }
                builder.append(" via ").append(assertion.getConnectionName());

                return builder.toString();
        }
    };

}
