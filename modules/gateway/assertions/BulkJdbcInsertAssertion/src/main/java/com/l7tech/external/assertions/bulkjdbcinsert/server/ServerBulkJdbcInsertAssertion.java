package com.l7tech.external.assertions.bulkjdbcinsert.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Server side implementation of the BulkJdbcInsertAssertion.
 *
 * @see com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion
 */
public class ServerBulkJdbcInsertAssertion extends AbstractMessageTargetableServerAssertion<BulkJdbcInsertAssertion> {
    private static Map<String,Transformer> transformerMap = new HashMap<>();//might be moved to the core so the transformers can be loaded

    private final String[] variablesUsed;
    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private final Config config;

    static{
        //initialize transformers. Custom transformers can be added later via separate assertion module
        transformerMap.put("String", new StringTransformer());
        transformerMap.put("Regex2Bool", new Regex2BoolTransformer());
        transformerMap.put("Regex2Int", new Regex2IntTransformer());
        transformerMap.put("Subtract", new SubtractTransformer());
    }

    public ServerBulkJdbcInsertAssertion( final BulkJdbcInsertAssertion assertion, final ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");

        jdbcConnectionPoolManager = context.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class);
        config = context.getBean("serverConfig", Config.class);

        if (assertion.getConnectionName() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
        }

        this.variablesUsed = assertion.getVariablesUsed();
    }

    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        //check if connection is a context variable

        final String connName = ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
        final String tableName = ExpandVariables.process(assertion.getTableName(), variableMap, getAudit());
        long startTime = System.currentTimeMillis();
        //validate that the connection exists.
        final Connection jdbcConnection = getJdbcConnection(connName);
        if (jdbcConnection == null) return AssertionStatus.FAILED;

        try {
            final MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
            if ( mimeKnob != null && message.isInitialized() ) {
                PartInfo partInfo = mimeKnob.getFirstPart();
                if(partInfo != null) {
                    //streaming support
                    InputStream inputStream;
                    if(assertion.getCompression() == BulkJdbcInsertAssertion.Compression.GZIP) {
                        inputStream = new GZIPInputStream(partInfo.getInputStream(false));
                    }
                    else {
                        inputStream = partInfo.getInputStream(false);
                    }

                    try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8)) {
                        try (final CSVParser parser = new CSVParser(reader, getCVSFormat(assertion))) {
                            List<CSVRecord> recordList = parser.getRecords();
                            if(recordList.size() > 0) {
                                Set<BulkJdbcInsertAssertion.ColumnMapper> columnMapperSet = getColumnMapperSet();
                                String sqlCommand = buildSqlStatement(tableName, columnMapperSet);
                                //column mapper set to map
                                Map<String, BulkJdbcInsertAssertion.ColumnMapper> columnMapperMap = new HashMap<>();
                                for(BulkJdbcInsertAssertion.ColumnMapper mapper : columnMapperSet) {
                                    columnMapperMap.put(mapper.getName(), mapper);
                                }
                                BulkJdbcInsertAssertion.ColumnMapper[] columnMappers = columnMapperSet.toArray(new BulkJdbcInsertAssertion.ColumnMapper[0]);
                                PreparedStatement stmt = jdbcConnection.prepareStatement(sqlCommand);
                                int currentBatchSize = 0;
                                int[] commitRespose = new int[0];
                                for (final CSVRecord record : recordList) {
                                    for(int i=0; i < columnMapperSet.size(); i++) {
                                        BulkJdbcInsertAssertion.ColumnMapper param = columnMappers[i];
                                        for (BulkJdbcInsertAssertion.ColumnMapper mapper : assertion.getColumnMapperList()) {
                                            if(mapper.equals(param)) {
                                                if(transformerMap.keySet().contains(mapper.getTransformation())) {
                                                    Transformer transformer = transformerMap.get(mapper.getTransformation());
                                                    transformer.transform(stmt, i + 1, mapper, record);
                                                }
                                                else {
                                                    logAndAudit(AssertionMessages.BULKJDBCINSERT_WARNING, "Unknown transformation " + mapper.getTransformation() + " found. Unable to process CSV");
                                                    return AssertionStatus.FAILED;
                                                }
                                            }
                                        }
                                    }
                                    stmt.addBatch();//add prepared statement to the batch
                                    currentBatchSize++;
                                    if(currentBatchSize == assertion.getBatchSize()) {
                                        int[] count = saveBatch(jdbcConnection, stmt);
                                        commitRespose = ArrayUtils.concat(commitRespose, count);
                                        currentBatchSize = 0;
                                    }
                                }

                                long processTime = System.currentTimeMillis() - startTime;
                                logAndAudit(AssertionMessages.BULKJDBCINSERT_SUCCESS, "Inserted " + commitRespose.length + " records into the table " +  tableName + " in " + processTime + " ms");
                            }
                        }
                    }
                }
                else {
                    logAndAudit(AssertionMessages.BULKJDBCINSERT_WARNING, "First part of the message is null");
                    return AssertionStatus.FALSIFIED;
                }
            } else {
                logAndAudit( AssertionMessages.BULKJDBCINSERT_WARNING, "Message not initialized" );
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchPartException nspe) {
            //logAndAudit
            logAndAudit(AssertionMessages.BULKJDBCINSERT_WARNING, new String[]{"The message contains no parts: " + nspe.getMessage()}, ExceptionUtils.getDebugException(nspe));
            return AssertionStatus.FAILED;
        } catch (SQLException sqle) {
            logAndAudit(AssertionMessages.BULKJDBCINSERT_WARNING, new String[]{"SQLException occurred: " + sqle.getMessage()}, ExceptionUtils.getDebugException(sqle));
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit(AssertionMessages.BULKJDBCINSERT_WARNING, new String[]{"Exception: " + e.getClass().getName() + " " + e.getMessage()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private int[] saveBatch(Connection jdbcConnection, PreparedStatement stmt) throws SQLException {
        int[] count = stmt.executeBatch();

        if(!jdbcConnection.getAutoCommit()) {
            jdbcConnection.commit();
            logAndAudit(AssertionMessages.BULKJDBCINSERT_FINE, count.length + " records were committed to the database");
        }
        return count;
    }


    protected Set<BulkJdbcInsertAssertion.ColumnMapper> getColumnMapperSet() {
        Set<BulkJdbcInsertAssertion.ColumnMapper> mapperSet = new TreeSet<>(new Comparator<BulkJdbcInsertAssertion.ColumnMapper>() {
            @Override
            public int compare(BulkJdbcInsertAssertion.ColumnMapper o1, BulkJdbcInsertAssertion.ColumnMapper o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });
        mapperSet.addAll(assertion.getColumnMapperList());
        return mapperSet;
    }

    protected String buildSqlStatement(final String tableName, final Set<BulkJdbcInsertAssertion.ColumnMapper> mapperSet) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();

        for(BulkJdbcInsertAssertion.ColumnMapper mapper : mapperSet) {
            if(sb1.length() > 0) sb1.append(",");
            sb1.append(mapper.getName());
            if(sb2.length() > 0) sb2.append(",");
            sb2.append("?");
        }
        return "INSERT INTO " + tableName + "(" + sb1.toString() + ") VALUES (" + sb2.toString() + ")";
    }

    //TODO: implement caching of the connection name. Possibly in the JdbcConnectionManagerImpl
    private Connection getJdbcConnection(String connName) {
        Connection connection = null;
        try {
            DataSource ds = jdbcConnectionPoolManager.getDataSource(connName);
            connection = ds.getConnection();
        } catch (NamingException ne) {

            logAndAudit(AssertionMessages.JDBC_CONNECTION_ERROR, "Failed to find JDBC connection " + connName + ". " + ne.getMessage());
            logger.log(Level.SEVERE, "Failed to find JDBC connection " + connName + ". " + ne.getMessage(), ExceptionUtils.getDebugException(ne));
        } catch(SQLException sqle) {
            logAndAudit(AssertionMessages.JDBC_CONNECTION_ERROR, "SQLException occurred for " + connName + ". " + sqle.getMessage());
            logger.log(Level.SEVERE, sqle.getMessage(), ExceptionUtils.getDebugException(sqle));
        }
        return connection;
    }

    private CSVFormat getCVSFormat(final BulkJdbcInsertAssertion assertion) {
        //TODO: configure format
        CSVFormat format = CSVFormat.DEFAULT;//start from the default format
        if(assertion.getRecordDelimiter() != null) {
            format = format.withRecordSeparator(assertion.getRecordDelimiter().equals(BulkJdbcInsertAssertion.CRLF)? "\r\n":assertion.getRecordDelimiter());
        }
        if(assertion.getFieldDelimiter() != null && assertion.getFieldDelimiter().trim().length() == 1) {
            format = format.withDelimiter(assertion.getFieldDelimiter().trim().charAt(0));
        }
        if(assertion.getEscapeQuote() != null && assertion.getEscapeQuote().trim().length() == 1) {
            format = format.withEscape(assertion.getEscapeQuote().trim().charAt(0));
        }
        if(assertion.getQuoteChar() != null && assertion.getQuoteChar().trim().length() == 1) {
            format = format.withQuote(assertion.getQuoteChar().trim().charAt(0));
        }
        return format;
    }

    private Pair<Charset,byte[]> getPartInfoBody( PartInfo partInfo) {
        Pair<Charset,byte[]> content;
        try {
            final ContentTypeHeader contentType = partInfo.getContentType();
            if ( !contentType.isTextualContentType()) {
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }
            // TODO maximum size? This could be huge and OOM
            final byte[] bytes = IOUtils.slurpStream(partInfo.getInputStream(false));
            content = new Pair<Charset,byte[]>(contentType.getEncoding(), bytes);
        } catch (IOException e) {
            throw new AssertionStatusException( AssertionStatus.FAILED );
        } catch (NoSuchPartException e) {
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }
        return content;
    }

    public static interface Transformer {
        public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException;
    }

    public static final class StringTransformer implements Transformer {
        @Override
        public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException{
            String val = record.get(mapper.getOrder());
            stmt.setString(index, val);
        }
    }

    public static final class Regex2BoolTransformer implements Transformer {
        @Override
        public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
            String val = record.get(mapper.getOrder());
            String transformParam = mapper.getTransformParam();
            stmt.setBoolean(index, convertRegex2Boolean(val, transformParam));
        }

        private boolean convertRegex2Boolean(String val, String transformParam) {
            Pattern regex = Pattern.compile(transformParam, Pattern.CASE_INSENSITIVE);
            Matcher m = regex.matcher(val);
            return m.find();
        }
    }

    public static final class Regex2IntTransformer implements Transformer {
        @Override
        public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
            String val = record.get(mapper.getOrder());
            String transformParam = mapper.getTransformParam();
            stmt.setInt(index, convertRegex2Int(val, transformParam));
        }

        private int convertRegex2Int(String val, String transformParam) {
            Pattern regex = Pattern.compile(transformParam, Pattern.CASE_INSENSITIVE);
            Matcher m = regex.matcher(val);
            return m.find()?1:0;
        }
    }

    public static final class SubtractTransformer implements Transformer {
        @Override
        public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
            try {
                long val1 = Long.parseLong(record.get(mapper.getOrder()));
                long val2 = Long.parseLong(record.get(mapper.getTransformParam()));
                stmt.setLong(index, val1 - val2);
            } catch(NumberFormatException nfe) {
                throw new SQLException("Invalid number format", nfe);
            }
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
    }
}
