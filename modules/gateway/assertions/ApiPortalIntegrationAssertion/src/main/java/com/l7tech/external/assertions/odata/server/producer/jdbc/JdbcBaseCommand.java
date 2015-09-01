package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.datasource.BaseJdbc;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.core4j.Enumerable;
import org.core4j.Func1;
import org.odata4j.core.*;
import org.odata4j.edm.*;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.Expression;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.command.ProducerCommandContext;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcBaseCommand {

    private static final Logger logger = Logger.getLogger(JdbcBaseCommand.class.getName());

    protected OEntity toOEntity(JdbcMetadataMapping mapping, EdmEntitySet entitySet, ResultSet results, QueryInfo queryInfo) throws SQLException {
        List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

        JdbcModel.JdbcTable jdbcTable = Util.getTable(mapping, entitySet);
        if (queryInfo != null && queryInfo.select != null && queryInfo.select.size() > 0) {//NOTES: by Richard needed to handle $select
            for (EntitySimpleProperty edmField : queryInfo.select) {
                String fieldName = edmField.getPropertyName();
                String colName = Util.getColumnName(mapping, entitySet, fieldName);
                Object value = results.getObject(colName);
                boolean isNull = false;
                if (value == null) {
                    value = "";
                    isNull = true;
                }
                OProperty<?> property = OProperties.simple(fieldName, value);
                if (isNull) {
                    property = OProperties.null_(fieldName, (EdmSimpleType) property.getType());
                }
                properties.add(property);
            }
            //the keys to be read from the result set if it's not part of the original $select, otherwise, we can't build the entityId links
            EdmEntityType eet = entitySet.getType();
            List<String> keys = eet.getKeys();
            for (String key : keys) {
                boolean found = false;
                for (OProperty<?> prop : properties) {
                    if (prop.getName().equals(key)) {
                        found = true;
                        break;
                    }
                }//end of for(prop)
                if (!found) {
                    String colName = Util.getColumnName(mapping, entitySet, key);
                    Object value = results.getObject(colName);
                    boolean isNull = false;
                    if (value == null) {
                        value = "";
                        isNull = true;
                    }
                    OProperty<?> property = OProperties.simple(key, value);
                    if (isNull) {
                        property = OProperties.null_(key, (EdmSimpleType) property.getType());
                    }
                    properties.add(property);
                }
            }//end of for(key)
        } else {
            for (EdmProperty edmProperty : entitySet.getType().getProperties()) {
                JdbcModel.JdbcColumn column = mapping.getMappedColumn(edmProperty);
                Object value = results.getObject(column.columnName);
                boolean isNull = false;
                if (value == null) {//NOTES: by Richard to handle null values from DB
                    value = "";
                    isNull = true;
                }
                OProperty<?> property;
                if (value instanceof Blob) {
                    property = OProperties.binary(edmProperty.getName(), getBlobValue((Blob) value));
                } else if (value instanceof Clob || value instanceof NClob) {
                    property = OProperties.simple(edmProperty.getName(), getClobStringValue((Clob) value));
                } else {
                    property = OProperties.simple(edmProperty.getName(), value);
                    if (isNull) {
                        property = OProperties.null_(edmProperty.getName(), (EdmSimpleType) edmProperty.getType());
                    } else if (jdbcTable.tableType.toUpperCase().indexOf("CUSTOM") >= 0 && false) {
                        //if it is a custom table, make sure we try to use the declared type
                        property = new Impl<>(edmProperty.getName(), edmProperty.getType(), value);
                    }
                }
                properties.add(property);
            }
        }

        OEntityKey entityKey = OEntityKey.infer(entitySet, properties);
        return OEntities.create(entitySet, entityKey, properties, Collections.<OLink>emptyList());
    }

    protected BoolCommonExpression prependPrimaryKeyFilter(JdbcMetadataMapping mapping, EdmEntityType entityType, OEntityKey entityKey, BoolCommonExpression filter) {
        List<BoolCommonExpression> filters = new ArrayList<BoolCommonExpression>();
        if (entityType.getKeys().size() == 1) {
            String key = entityType.getKeys().iterator().next();
            filters.add(Expression.eq(Expression.simpleProperty(key), Expression.literal(entityKey.asSingleValue())));
        } else {
            Map<String, NamedValue<?>> complexKey = Enumerable.create(entityKey.asComplexValue()).toMap(new Func1<NamedValue<?>, String>() {
                @Override
                public String apply(NamedValue<?> nv) {
                    return nv.getName();
                }
            });
            for (String key : entityType.getKeys()) {
                filters.add(Expression.eq(Expression.simpleProperty(key), Expression.literal(complexKey.get(key).getValue())));
            }
        }
        if (filter != null)
            filters.add(filter);
        BoolCommonExpression newFilter = null;
        for (BoolCommonExpression f : filters)
            newFilter = newFilter == null ? f : Expression.and(f, newFilter);
        return newFilter;
    }

    protected String getDatabaseName(ProducerCommandContext context) {
        JdbcProducerCommandContext jdbcProducerCommandContext = (JdbcProducerCommandContext) context;
        BaseJdbc jdbcDataSource = (BaseJdbc) jdbcProducerCommandContext.getJdbc();
        return jdbcDataSource.getDatabaseName().toLowerCase();
    }

    /**
     * @throws SQLException any problems reading the clob's stream or if the stream limit is exceeded
     */
    protected String getClobStringValue(final Clob clob) throws SQLException {
        Reader reader = null;
        StringWriter writer = null;
        final long maxClobSize = 10485760L;//config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_CLOB_SIZE_OUT, 10485760L);//todo: see if we can inject l7 Config here
        try {
            reader = clob.getCharacterStream();
            writer = new StringWriter(8192);
            IOUtils.copyStream(reader, writer, new Functions.UnaryVoidThrows<Long, IOException>() {
                @Override
                public void call(Long totalRead) throws IOException {
                    if (maxClobSize > 0 && totalRead > maxClobSize) {
                        throw new IOException("CLOB value has exceeded maximum allowed size of " + maxClobSize + " bytes");
                    }
                }
            });
            // todo intern to help against duplicate calls? decide when caching is implemented.
            return writer.toString();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading CLOB: '" + ExceptionUtils.getMessage(ioe) + "'.", ExceptionUtils.getDebugException(ioe));
            throw new SQLException(ExceptionUtils.getMessage(ioe));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error reading CLOB: '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw e;
        } finally {
            ResourceUtils.closeQuietly(reader);
            ResourceUtils.closeQuietly(writer);
        }
    }

    /**
     * @throws SQLException any problems reading the blob's stream or if the stream limit is exceeded
     */
    protected byte[] getBlobValue(final Blob blob) throws SQLException {
        InputStream inputStream = null;
        ByteArrayOutputStream byteOutput = null;
        final long maxBlobSize = 10485760L;//config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L);//todo: see if we can inject l7 Config here
        try {
            inputStream = blob.getBinaryStream();
            byteOutput = new ByteArrayOutputStream();
            IOUtils.copyStream(inputStream, byteOutput, new Functions.UnaryVoidThrows<Long, IOException>() {
                @Override
                public void call(Long totalRead) throws IOException {
                    if (maxBlobSize > 0 && totalRead > maxBlobSize) {
                        throw new IOException("BLOB value has exceeded maximum allowed size of " + maxBlobSize + " bytes");
                    }
                }
            });
            return byteOutput.toByteArray();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading BLOB: '" + ExceptionUtils.getMessage(ioe) + "'.", ExceptionUtils.getDebugException(ioe));
            throw new SQLException(ExceptionUtils.getMessage(ioe));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error reading BLOB: '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw e;
        } finally {
            ResourceUtils.closeQuietly(inputStream);
            ResourceUtils.closeQuietly(byteOutput);
        }
    }

    private static class Impl<T> implements OProperty<T> {

        private final String name;
        private final EdmType type;
        private final T value;

        Impl(String name, EdmType type, T value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public EdmType getType() {
            return type;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.format("OProperty[%s,%s,%s]", name, getType(), OSimpleObjects.getValueDisplayString(value));
        }
    }


}
