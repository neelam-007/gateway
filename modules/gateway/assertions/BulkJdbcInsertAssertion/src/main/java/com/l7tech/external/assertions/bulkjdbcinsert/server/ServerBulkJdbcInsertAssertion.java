package com.l7tech.external.assertions.bulkjdbcinsert.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
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
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
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

import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;

/**
 * Server side implementation of the BulkJdbcInsertAssertion.
 *
 * @see com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion
 */
public class ServerBulkJdbcInsertAssertion extends AbstractMessageTargetableServerAssertion<BulkJdbcInsertAssertion> {
    private final String[] variablesUsed;
    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private final Config config;

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
                                                if(BulkJdbcInsertAssertion.transformerMap.keySet().contains(mapper.getTransformation())) {
                                                    Transformer transformer = BulkJdbcInsertAssertion.transformerMap.get(mapper.getTransformation());
                                                    transformer.transform(stmt, i + 1, mapper, record);
                                                }
                                                else {
                                                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Bulk JDBC Insert Assertion failed due to: Unknown transformation " + mapper.getTransformation() + " found. Unable to process CSV");
                                                    return AssertionStatus.FAILED;
                                                }
                                            }
                                        }
                                    }
                                    stmt.addBatch();//add prepared statement to the batch
                                    currentBatchSize++;
                                    if(currentBatchSize == assertion.getBatchSize()) {
                                        int[] count = stmt.executeBatch();
                                        logger.log(Level.FINE, "Executed the batch of " + count.length +" records");
                                        commitRespose = concatArrays(commitRespose, count);
                                        currentBatchSize = 0;
                                    }
                                }
                                //make sure we executed the batch
                                if(currentBatchSize > 0) {
                                    int[] count = stmt.executeBatch();
                                    logger.log(Level.FINE, "Executed the batch of " + count.length +" records");
                                    commitRespose = concatArrays(commitRespose, count);
                                }
                                if(!jdbcConnection.getAutoCommit()) {
                                    jdbcConnection.commit();
                                    logAndAudit(AssertionMessages.USERDETAIL_FINE, "Bulk JDBC Insert Assertion - " + commitRespose.length + " records were committed to the database");
                                }
                                logger.log(Level.FINE, "Total amount of records processed: " + recordList.size());
                                long processTime = System.currentTimeMillis() - startTime;
                                logAndAudit(AssertionMessages.USERDETAIL_INFO, "Bulk JDBC Insert Assertion successfully processed data: inserted " + commitRespose.length + " records into the table " +  tableName + " in " + processTime + " ms");
                            }
                        }
                    }
                }
                else {
                    logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Bulk JDBC Insert Assertion failed due to: First part of the message is null");
                    return AssertionStatus.FALSIFIED;
                }
            } else {
                logAndAudit( AssertionMessages.USERDETAIL_WARNING, "Bulk JDBC Insert Assertion failed due to: Message not initialized" );
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchPartException nspe) {
            logAndAudit(Messages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"The message contains no parts: " + nspe.getMessage()}, ExceptionUtils.getDebugException(nspe));
            return AssertionStatus.FAILED;
        } catch (SQLException sqle) {
            try {
                if(!jdbcConnection.getAutoCommit()) {
                    jdbcConnection.rollback();
                }
            } catch (SQLException e) {
                logger.log(Level.FINE, "Unable to rollback transaction for connection " + connName);
            }
            logAndAudit(Messages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Bulk JDBC Insert Assertion failed due to: SQLException occurred: " + sqle.getMessage()}, ExceptionUtils.getDebugException(sqle));
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit(Messages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Bulk JDBC Insert Assertion failed due to: Exception: " + e.getClass().getName() + " " + e.getMessage()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } finally {
            if(jdbcConnection != null) {
                try {
                    jdbcConnection.close();
                } catch (SQLException e) {
                    logAndAudit(AssertionMessages.JDBC_CONNECTION_ERROR, "SQLException occurred for " + connName + ". " + e.getMessage());
                    logger.log(Level.SEVERE, e.getMessage(), ExceptionUtils.getDebugException(e));
                }
            }
        }

        return AssertionStatus.NONE;
    }


    protected Set<BulkJdbcInsertAssertion.ColumnMapper> getColumnMapperSet() {
        Set<BulkJdbcInsertAssertion.ColumnMapper> mapperSet = new TreeSet<>(new Comparator<BulkJdbcInsertAssertion.ColumnMapper>() {
            @Override
            public int compare(BulkJdbcInsertAssertion.ColumnMapper o1, BulkJdbcInsertAssertion.ColumnMapper o2) {
                int result = o1.getName().hashCode() - o2.getName().hashCode();
                if(result == 0) {
                    result = o1.getOrder() - o2.getOrder();
                }
                return result;
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
            if(StringUtils.isNotBlank(connName)) {
                DataSource ds = jdbcConnectionPoolManager.getDataSource(connName);
                connection = ds.getConnection();
            }
            else {
                logAndAudit(AssertionMessages.JDBC_CONNECTION_ERROR, "JDBC connection name is blank");
            }
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
        //configure default format with ',' field delimiter, CRLF record delimiter
        CSVFormat format = CSVFormat.newFormat(',').withIgnoreEmptyLines(true);//CSVFormat.DEFAULT;//start from the default format
        if(assertion.getRecordDelimiter() != null) {
            format = configureDelimiter(assertion, format);
        }
        if(assertion.getFieldDelimiter() != null && assertion.getFieldDelimiter().trim().length() == 1) {
            format = format.withDelimiter(assertion.getFieldDelimiter().trim().charAt(0));
        }
        if(assertion.isQuoted()) {
            if (assertion.getQuoteChar() != null && assertion.getQuoteChar().trim().length() == 1) {
                format = format.withQuote(assertion.getQuoteChar().trim().charAt(0));
            }
        }
        else {
            format = format.withQuote(null);
        }

        if (assertion.getEscapeQuote() != null && assertion.getEscapeQuote().trim().length() == 1) {
            char escapeChar = assertion.getEscapeQuote().trim().charAt(0);
            format = (format.getQuoteCharacter() != null && escapeChar == format.getQuoteCharacter()) ? format.withEscape(null) : format.withEscape(escapeChar);
        }

        return format;
    }

    private CSVFormat configureDelimiter(BulkJdbcInsertAssertion assertion, CSVFormat format) {
        String delimiter;
        if("CRLF".equals(assertion.getRecordDelimiter())) {
            delimiter = BulkJdbcInsertAssertion.CRLF;
        }
        else if("CR".equals(assertion.getRecordDelimiter())) {
            delimiter = BulkJdbcInsertAssertion.CR;
        }
        else if("LF".equals(assertion.getRecordDelimiter())) {
            delimiter = BulkJdbcInsertAssertion.LF;
        }
        else {
            delimiter = assertion.getRecordDelimiter();
        }
        return format.withRecordSeparator(delimiter);
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

     static int[] concatArrays( final int[] data1, final int[] data2 ) {
        final int data1Length = getLength( data1 );
        final int data2Length = getLength( data2 );
        final Object copy = newInstance( int.class, data1Length + data2Length );
        System.arraycopy(data1, 0, copy, 0, data1Length);
        System.arraycopy(data2, 0, copy, data1Length, data2Length);
        return (int[])copy;
    }
    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
    }
}
