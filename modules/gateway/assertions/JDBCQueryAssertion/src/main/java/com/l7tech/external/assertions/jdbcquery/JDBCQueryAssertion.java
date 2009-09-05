package com.l7tech.external.assertions.jdbcquery;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 */
public class JDBCQueryAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(JDBCQueryAssertion.class.getName());

    /* URL to use to connect to the database */
    private String connectionUrl;

    /* driver to use to connect */
    private String driver;

    /* username to use to connect */
    private String user;

    /* password to use to connect */
    private String pass;

    /* sql query */
    private String sql;

    /* prefixed used for context variable creation */
    private String variablePrefix;

    /* map of variables set by this assertion
     * key = table column name
     * value = context variable name
     */
    private Map<String, String> variableMap;

//    public static final Pattern GREP_INSERT_PATTERN = Pattern.compile("^insert into", Pattern.CASE_INSENSITIVE);
//    public static final Pattern GREP_UPDATE_PATTERN = Pattern.compile("^update", Pattern.CASE_INSENSITIVE);
//    public static final Pattern GREP_DELETE_PATTERN = Pattern.compile("^delete", Pattern.CASE_INSENSITIVE);
    public static final Pattern SELECT_PATTERN = Pattern.compile("^select", Pattern.CASE_INSENSITIVE);

    public JDBCQueryAssertion() {
        this.connectionUrl = "";
        this.driver = "";
        this.user = "";
        this.pass = "";
        this.sql = "";
        this.variablePrefix = DEFAULT_VARIABLE_PREFIX;
        this.variableMap = new HashMap<String, String>();
    }

    public JDBCQueryAssertion(String connectionUrl, String user, String pass, String driver, String sql, String variablePrefix, Map<String, String> variableMap) {
        this.connectionUrl = connectionUrl;
        this.user = user;
        this.pass = pass;
        this.driver = driver;
        this.sql = sql;
        this.variablePrefix = variablePrefix;
        this.variableMap = variableMap;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public Map<String, String> getVariableMap() {
        return variableMap;
    }

    public void setVariableMap(Map<String, String> variableMap) {
        this.variableMap = variableMap;
    }

    private static final String DEFAULT_VARIABLE_PREFIX = "jdbcQuery";
    public static final String VAR_COUNT_SUFFIX = "count";

    public VariableMetadata[] getVariablesSet() {
        ArrayList<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();

        Set<Map.Entry<String, String>> entries = variableMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            varMeta.add(new VariableMetadata(getVariablePrefix() + "." + entry.getValue(), true, false, null, false, DataType.STRING));
        }

        //add the count variable
        varMeta.add(new VariableMetadata(getVariablePrefix() + "." + VAR_COUNT_SUFFIX, false, false, null, false, DataType.STRING));

        return varMeta.toArray(new VariableMetadata[varMeta.size()]);
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(connectionUrl + driver + user + sql + variablePrefix);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JDBCQueryAssertion.class.getName() + ".metadataInitialized";

    public static final String MAX_RECORDS_CLUSTER_PROP = "jdbcQuery.maxRecords";
    
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(MAX_RECORDS_CLUSTER_PROP, new String[]{
                "The maximum number of records returned by a JDBC Query Assertion.",
                "10"
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "JDBC Query");
        meta.put(AssertionMetadata.LONG_NAME, "Query a backend database via JDBC.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/CreateIdentityProvider16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/CreateIdentityProvider16x16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jdbcquery.console.JdbcQueryAssertionDialog");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:JDBCQuery" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
