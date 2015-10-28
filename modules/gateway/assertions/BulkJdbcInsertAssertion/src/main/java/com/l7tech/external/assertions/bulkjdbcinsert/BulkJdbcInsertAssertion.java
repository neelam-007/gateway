package com.l7tech.external.assertions.bulkjdbcinsert;

import com.l7tech.external.assertions.bulkjdbcinsert.server.Transformer;
import com.l7tech.external.assertions.bulkjdbcinsert.server.transformers.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.*;
import com.l7tech.search.Dependency;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
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

    public static final Map<String,String> recordDelimiterMap;

    public static Map<String,Transformer> transformerMap = new HashMap<>();//might be moved to the core so the transformers can be loaded

    public static String[] TRANSFORMATIONS;

    static{
        //initialize record delimiters
        Map<String, String> delimiters = new HashMap<>();
        delimiters.put("CR", "\r");
        delimiters.put("LF", "\n");
        delimiters.put("CRLF", "\r\n");
        recordDelimiterMap = Collections.unmodifiableMap(delimiters);
        //initialize transformers. Custom transformers can be added later via separate assertion module
        transformerMap.put("String", new StringTransformer());
        transformerMap.put("Regex2Bool", new Regex2BoolTransformer());
        transformerMap.put("Regex2Int", new Regex2IntTransformer());
        transformerMap.put("Subtract", new SubtractTransformer());
        transformerMap.put("Add", new AddTransformer());
        transformerMap.put("UUID", new GenerateUuidTransformer());
        transformerMap.put("SetInt", new SetIntValueTransformer());
        transformerMap.put("SetString", new SetStringTransformer());
        TRANSFORMATIONS = transformerMap.keySet().toArray(new String[0]);
        Arrays.sort(TRANSFORMATIONS);
    }

    private String connectionName;
    private String schema;
    private String tableName;
    private String recordDelimiter = "CRLF";
    private String fieldDelimiter = ",";
    private boolean quoted = false;
    private String escapeQuote = "";
    private String quoteChar = "\"";
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
        return super.doGetVariablesUsed().withExpressions(connectionName, tableName, fieldDelimiter, quoteChar, escapeQuote);
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
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Parses data from a compressed comma-separated file and then inserts them in bulk into a specified database table using a JDBC connection.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.bulkjdbcinsert.console.BulkJdbcInsertPropertiesDialog");

        // request default feature set name for our class name, since we are a known optional module
        // we want to make sure this assertion has backward compatible licensing requirements. For that we use "set:modularAssertions" instead licensing from the class
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new BeanTypeMapping(ColumnMapper.class, "columnMapper"),
                new CollectionTypeMapping(List.class, ColumnMapper.class , ArrayList.class , "columnMapperList"),
                new Java5EnumTypeMapping(Compression.class, "compression")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String baseName = "Insert JDBC Data in Bulk";
    private static final int MAX_DISPLAY_LENGTH = 120;

    private final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<BulkJdbcInsertAssertion>(){
        @Override
        public String getAssertionName( final BulkJdbcInsertAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder builder= new StringBuilder(assertion.getTargetName());
            builder.append(": ")
                    .append(baseName)
                    .append(" into ")
                    .append(assertion.tableName)
                    .append(" Table via ")
                    .append(assertion.connectionName);
            if(builder.length() > MAX_DISPLAY_LENGTH) {
                builder.replace(MAX_DISPLAY_LENGTH -1, builder.length(), "...");
            }
            return builder.toString();
        }
    };

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
