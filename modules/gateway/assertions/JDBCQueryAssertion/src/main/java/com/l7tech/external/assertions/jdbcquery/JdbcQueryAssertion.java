package com.l7tech.external.assertions.jdbcquery;

import com.l7tech.gateway.common.jdbc.JdbcAdmin;
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
import com.l7tech.search.Dependency;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
public class JdbcQueryAssertion extends Assertion implements JdbcConnectionable, UsesVariables, SetsVariables {
    public static final String VARIABLE_COUNT = "queryresult.count";
    public static final String VARIABLE_RESULTSET = ".resultSet";
    public static final String MULTIPLE_RESULTSET_COUNT = "multipleResultSet.count";
    public static final String MULTIPLE_VARIABLE_COUNT = "multipleResultSet.queryresult.count";
    public static final String DEFAULT_VARIABLE_PREFIX = "jdbcQuery";
    public static final String ASSERTION_SHORT_NAME = "Perform JDBC Query";
    public static final String VARIABLE_XML_RESULT = ".xmlResult";

    private static final String META_INITIALIZED = JdbcQueryAssertion.class.getName() + ".metadataInitialized";

    private String connectionName;
    private String sqlQuery;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
    private String queryName = "";
    private int maxRecords = JdbcAdmin.ORIGINAL_MAX_RECORDS;
    private boolean assertionFailureEnabled = true;
    private Map<String, String> namingMap = new TreeMap<String, String>();
    private List<String> resolveAsObjectList = new ArrayList<String>();
    private boolean generateXmlResult;
    private String schema;
    private String queryTimeout;
    private boolean saveResultsAsContextVariables = true;

    //default to true to support pre-fangtooth assertions
    private boolean convertVariablesToStrings = true;

    public JdbcQueryAssertion() {
    }

    @Override
    public JdbcQueryAssertion clone() {
        JdbcQueryAssertion copy = (JdbcQueryAssertion) super.clone();

        copy.setConnectionName(connectionName);
        copy.setSqlQuery(sqlQuery);
        copy.setVariablePrefix(variablePrefix);
        copy.setMaxRecords(maxRecords);
        copy.setQueryTimeout(queryTimeout);
        copy.setAssertionFailureEnabled(assertionFailureEnabled);
        copy.setQueryName(queryName);
        copy.setNamingMap(copyMap(namingMap));
        copy.setSchema(schema);
        copy.setGenerateXmlResult(generateXmlResult);
        copy.setSaveResultsAsContextVariables(saveResultsAsContextVariables);
        return copy;
    }

    public void copyFrom(final JdbcQueryAssertion source) {
        setConnectionName(source.getConnectionName());
        setSqlQuery(source.getSqlQuery());
        setVariablePrefix(source.getVariablePrefix());
        setMaxRecords(source.getMaxRecords());
        setQueryTimeout(source.getQueryTimeout());
        setAssertionFailureEnabled(source.isAssertionFailureEnabled());
        setQueryName(source.getQueryName());
        setNamingMap(copyMap(source.getNamingMap()));
        setEnabled(source.isEnabled());
        setSchema(source.getSchema());
        setGenerateXmlResult(source.isGenerateXmlResult());
        setSaveResultsAsContextVariables(source.isSaveResultsAsContextVariables());
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.JDBC_CONNECTION)
    @Dependency(type = Dependency.DependencyType.JDBC_CONNECTION, methodReturnType = Dependency.MethodReturnType.NAME)
    public String getConnectionName() {
        return connectionName;
    }

    @Override
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public int getMaxRecords() {
        return maxRecords;
    }

    public void setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
    }

    public String getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(String queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public boolean isAssertionFailureEnabled() {
        return assertionFailureEnabled;
    }

    public void setAssertionFailureEnabled(boolean assertionFailureEnabled) {
        this.assertionFailureEnabled = assertionFailureEnabled;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public Map<String, String> getNamingMap() {
        return namingMap;
    }

    public void setNamingMap(Map<String, String> namingMap) {
        this.namingMap = namingMap;
    }

    public boolean isConvertVariablesToStrings() {
        return convertVariablesToStrings;
    }

    public void setConvertVariablesToStrings(boolean convertVariablesToStrings) {
        this.convertVariablesToStrings = convertVariablesToStrings;
    }

    @Deprecated
    public void setAllowMultiValuedVariables(boolean allowMultiValuedVariables) {
        this.convertVariablesToStrings = !allowMultiValuedVariables;
    }

    public boolean isGenerateXmlResult() {
        return generateXmlResult;
    }

    public void setGenerateXmlResult(boolean generateXmlResult) {
        this.generateXmlResult = generateXmlResult;
    }

    public String getSchema() {
        return schema;
    }

    // only used for oracle databases ( ie: driver class contains "oracle")
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Warning: Hidden property which was added for external audits in Fangtooth.
     *
     * Do not expose via the UI. This property is ignored unless it originated via the External audit system.
     */
    public List<String> getResolveAsObjectList() {
        return resolveAsObjectList;
    }

    public void setResolveAsObjectList(List<String> resolveAsObjectList) {
        this.resolveAsObjectList = resolveAsObjectList;
    }

    public boolean isSaveResultsAsContextVariables() {
        return saveResultsAsContextVariables;
    }

    public void setSaveResultsAsContextVariables(boolean saveResultsAsContextVariables) {
        this.saveResultsAsContextVariables = saveResultsAsContextVariables;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        List<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();
        varMeta.add(new VariableMetadata(variablePrefix + "." + VARIABLE_COUNT, false, false, null, false, DataType.INTEGER));

        if(saveResultsAsContextVariables){
            for (String key : namingMap.keySet()) {
                String varName = namingMap.get(key);
                boolean multi_valued = !key.endsWith(VARIABLE_COUNT);
                varMeta.add(new VariableMetadata(variablePrefix + "." + varName, false, multi_valued, null, false, DataType.STRING));
            }
        }
        
        if(generateXmlResult){
            varMeta.add(new VariableMetadata(variablePrefix + VARIABLE_XML_RESULT, false, false, null, false, DataType.STRING));
        }
        return varMeta.toArray(new VariableMetadata[varMeta.size()]);
    }


    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(connectionName, sqlQuery, variablePrefix, queryTimeout, schema);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, ASSERTION_SHORT_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Query an external database via a JDBC connection.");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertionAdvice");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<JdbcQueryAssertion>() {
            @Override
            public String getAssertionName(JdbcQueryAssertion assertion, boolean decorate) {
                if (!decorate) return ASSERTION_SHORT_NAME;

                String queryName = assertion.getQueryName();
                if (queryName == null || queryName.trim().isEmpty()) return ASSERTION_SHORT_NAME;
                else return AssertionUtils.decorateName(assertion, ASSERTION_SHORT_NAME + " - " + queryName);
            }
        });
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jdbcquery.console.JdbcQueryAssertionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "JDBC Query Properties");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:JDBCQuery" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new MapTypeMapping()
        )));

        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion");
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