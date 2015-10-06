package com.l7tech.external.assertions.bulkjdbcinsert;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.*;
import com.l7tech.search.Dependency;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 * 
 */
public class BulkJdbcInsertAssertion extends MessageTargetableAssertion implements JdbcConnectionable,UsesVariables {
    protected static final Logger logger = Logger.getLogger(BulkJdbcInsertAssertion.class.getName());
    public enum Compression {
        NONE, GZIP, DEFLATE;

        public static String[] valuesToString() {
            List<String> l = new ArrayList<>(values().length);
            for(Compression value : values()) {
                l.add(value.toString());
            }
            return l.toArray(new String[0]);
        }
    }

    public static String[] TRANSFORMATIONS = new String[]{"String","Regex2Bool","Regex2Int","Subtract"};

    private String connectionName;
    private String schema;
    private String tableName;
    private String recordDelimiter = "<CR><LF>";
    private String fieldDelimiter = ",";
    private boolean quoted = false;
    private String escapeQuote = "";
    private String quoteChar = "";
    private Compression compression = Compression.GZIP;
    private int batchSize = 100;
    private List<ColumnMapper> columnMapperList;


    public BulkJdbcInsertAssertion() {
        setTarget(TargetMessageType.REQUEST);
        setTargetModifiedByGateway(true);
    }


    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.JDBC_CONNECTION)
    @Dependency(type = Dependency.DependencyType.JDBC_CONNECTION, methodReturnType = Dependency.MethodReturnType.NAME)
    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    public void setRecordDelimiter(String recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
    }

    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    public boolean isQuoted() {
        return quoted;
    }

    public void setQuoted(boolean quoted) {
        this.quoted = quoted;
    }

    public String getEscapeQuote() {
        return escapeQuote;
    }

    public void setEscapeQuote(String escapeQuote) {
        this.escapeQuote = escapeQuote;
    }

    public String getQuoteChar() {
        return quoteChar;
    }

    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Compression getCompression() {
        return compression;
    }

    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public List<ColumnMapper> getColumnMapperList() {
        return columnMapperList;
    }

    public void setColumnMapperList(List<ColumnMapper> columnMapperList) {
        this.columnMapperList = columnMapperList;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(connectionName);
    }
    //
    // Metadata
    //
    private static final String META_INITIALIZED = BulkJdbcInsertAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Bulk JDBC Insert");
        meta.put(AssertionMetadata.DESCRIPTION, "Insert bulk data in csv format to the specified database table");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.bulkjdbcinsert.console.BulkJdbcInsertPropertiesDialog");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:BulkJdbcInsert" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new BeanTypeMapping(ColumnMapper.class, "columnMapper"),
                new CollectionTypeMapping(List.class, ColumnMapper.class , ArrayList.class , "columnMapperList"),
                new Java5EnumTypeMapping(Compression.class, "compression")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class ColumnMapper {
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getTransformation() {
            return transformation;
        }

        public void setTransformation(String transformation) {
            this.transformation = transformation;
        }

        public String getTransformParam() {
            return transformParam;
        }

        public void setTransformParam(String transformParam) {
            this.transformParam = transformParam;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ColumnMapper that = (ColumnMapper) o;

            if (order != that.order) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + order;
            return result;
        }

        private String name;
        private int order;
        private String transformation;
        private String transformParam;
    }

}
