package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import com.l7tech.external.assertions.odata.server.producer.jdbc.SqlStatement.SqlParameter;
import org.core4j.Enumerable;
import org.odata4j.core.ImmutableList;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.ExpressionVisitor;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.producer.QueryInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenerateSqlQuery {
    final static String SQL_DESC = " DESC ";
    final static String SELECT = " SELECT ";
    final static String SELECT_ALL = " SELECT * ";
    final static String SELECT_MYSQL = " SELECT SQL_CALC_FOUND_ROWS ";
    final static String SELECT_ALL_MYSQL = " SELECT SQL_CALC_FOUND_ROWS * ";
    final static String ORACLE_ROWNUM_SELECT = " SELECT * FROM ( select /*+ FIRST_ROWS(n) */ a.*, ROWNUM rnum from ( $$QUERY$$ ) a ";
    final static String ORACLE_ROWNUM_TOP = " WHERE ROWNUM <= $$TOP$$ )";
    final static String ORACLE_ROWNUM_SKIP = " WHERE rnum > $$SKIP$$ ";
    final static String OFFSET = " OFFSET ";
    final static String SPACE = " ";
    final static String PAREN_OPEN = " ( ";
    final static String PAREN_CLOSE = " ) ";
    final static String AND = " AND ";
    final static String LT_EQ = " <= ";
    final static String COMMA = " , ";
    final static String LIMIT = " LIMIT ";
    final static String GROUPBY = "groupby(";
    final static String FROM_MARKER = "$$FROM$$";

    private static final Logger logger = Logger.getLogger(GenerateSqlQuery.class.getName());

    public SqlStatement generate(JdbcMetadataMapping mapping, EdmEntitySet entitySet, QueryInfo queryInfo, BoolCommonExpression filter) {
        return this.generate(mapping, entitySet, queryInfo, filter, null);
    }

    public SqlStatement generate(JdbcMetadataMapping mapping, EdmEntitySet entitySet, QueryInfo queryInfo, BoolCommonExpression filter, String databaseTypeName) {
        JdbcTable table = mapping.getMappedTable(entitySet);
        final String SELECT_STRING_TO_USE, SELECT_ALL_STRING_TO_USE;
        if (databaseTypeName != null && databaseTypeName.indexOf("mysql") >= 0) {
            SELECT_STRING_TO_USE = SELECT_MYSQL;
            SELECT_ALL_STRING_TO_USE = SELECT_ALL_MYSQL;
        } else {
            SELECT_STRING_TO_USE = SELECT;
            SELECT_ALL_STRING_TO_USE = SELECT_ALL;
        }
        StringBuilder sb = new StringBuilder(FROM_MARKER);
        List<SqlParameter> params = new ArrayList<SqlParameter>();
        final List<OrderByExpression> orderByList = queryInfo == null ? null : queryInfo.orderBy;
        final List<EntitySimpleProperty> selectList = queryInfo == null ? null : queryInfo.select;
        if (filter != null) {
            GenerateWhereClause whereClauseGen = newWhereClauseGenerator(entitySet, mapping);
            filter.visit(whereClauseGen);
            whereClauseGen.append(sb, params);
        }
        //TODO: this is probably where we will support Odata v4 aggregate functions
        Map<String, String> customOptions = queryInfo == null ? null : queryInfo.customOptions;
        String apply = null, groupby = null;
        if (customOptions != null) {
            //TODO: in Odata v4 it will actually be $apply and will no longer be in customOptions
            apply = customOptions.get("apply");
        }
        if (apply != null && apply.length() > 0) {
            if (apply.startsWith(GROUPBY)) {
                groupby = apply.substring(GROUPBY.length(), apply.lastIndexOf(")"));
            }
        }
        if (groupby != null && groupby.length() > 0) {
            sb.append(" GROUP BY ");
            String groupby_parts[] = groupby.split(",");
            List<String> values = new LinkedList<String>();
            for (final String groupby_field : groupby_parts) {
                EntitySimpleProperty edmField = new EntitySimpleProperty() {
                    @Override
                    public String getPropertyName() {
                        return groupby_field.trim();
                    }

                    @Override
                    public void visit(ExpressionVisitor visitor) {
                        visitor.visit(this);
                    }
                };
                String colName = Util.getColumnName(mapping, entitySet, edmField);
                if (colName == null) {
                    logger.log(Level.SEVERE, "{0} is a non-existing field for {1}", new String[]{edmField.getPropertyName(), entitySet.getName()});
                    throw new BadRequestException("There was an invalid field in the groupby parameter");
                }
                values.add(colName);
            }
            sb.append(Enumerable.create(values).join(","));

        }
        if (orderByList != null && orderByList.size() > 0) {
            sb.append(" ORDER BY ");
            List<String> values = new LinkedList<String>();
            for (OrderByExpression orderBy : orderByList) {
                String colName = Util.getColumnName(mapping, entitySet, (EntitySimpleProperty) orderBy.getExpression());
                if (orderBy.getDirection() == OrderByExpression.Direction.DESCENDING) {
                    values.add(colName + SQL_DESC);
                } else
                    values.add(colName);
            }
            sb.append(Enumerable.create(values).join(","));
        }
        StringBuilder select = new StringBuilder();
        if (selectList != null && selectList.size() > 0) {
            EdmEntityType eet = entitySet.getType();
            List<String> values = new LinkedList<String>();
            for (EntitySimpleProperty edmField : selectList) {
                String colName = Util.getColumnName(mapping, entitySet, edmField);
                if (colName == null) {
                    logger.log(Level.SEVERE, "{0} is a non-existing field for {1}", new String[]{edmField.getPropertyName(), entitySet.getName()});
                    throw new BadRequestException("There was an invalid field in the $select parameter");
                }
                values.add(colName);
            }
            //the keys need to be part of the result set, otherwise, we can't build the entityId links

            List<String> keys = eet.getKeys();
            for (String key : keys) {
                //find the actual field name for the key
                EdmProperty edmProperty = eet.findDeclaredProperty(key);
                JdbcModel.JdbcColumn column = mapping.getMappedColumn(edmProperty);
                String colName = column.columnName;
                if (!values.contains(colName)) {
                    values.add(colName);
                }
            }
            select.append(SELECT_STRING_TO_USE);
            select.append(Enumerable.create(values).join(","));
            if ("CUSTOM".equalsIgnoreCase(table.tableType)) {
                if (databaseTypeName.indexOf("mysql") >= 0) {
                    select = new StringBuilder();
                    select.append(SELECT_STRING_TO_USE);
                    String normalizeQuery = Util.normalizeQuery(table.customQuery);
                    List<String> fields = Util.parseFieldsFromQuery(normalizeQuery);
                    List<String> newValues = new LinkedList<String>();
                    for (String str : values) {
                        processField(str, fields, newValues);
                    }
                    //make sure the order get's added
                    for (OrderByExpression orderBy : orderByList) {
                        String colName = Util.getColumnName(mapping, entitySet, (EntitySimpleProperty) orderBy.getExpression());
                        processField(colName, fields, newValues);
                    }
                    select.append(Enumerable.create(newValues).join(","));
                    try {
                        sb.replace(0, FROM_MARKER.length(), normalizeQuery.substring(normalizeQuery.toUpperCase().indexOf(" FROM ")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    select.append(SELECT);
                    select.append(Enumerable.create(values).join(","));
                    sb.replace(0, FROM_MARKER.length(), " FROM ( " + table.customQuery);
                    sb.append(" ) a_alias0");
                }
            }
        } else {
            if ("CUSTOM".equalsIgnoreCase(table.tableType)) {
                if (databaseTypeName.indexOf("mysql") >= 0) {
                    sb.replace(0, FROM_MARKER.length(), table.customQuery.toUpperCase().replaceAll("SELECT ", SELECT_MYSQL));
                } else {
                    sb.replace(0, FROM_MARKER.length(), table.customQuery);
                }
            } else {
                select.append(SELECT_ALL_STRING_TO_USE);
            }
        }
        select.append(sb);
        Integer maxRows = new Integer(0);
        if (queryInfo != null && (queryInfo.top != null || queryInfo.skip != null)) {
            int skip = queryInfo.skip == null ? 0 : queryInfo.skip.intValue();
            //delegate to driver for $top just in case we can't build the proper query
            if (queryInfo.top != null) {
                maxRows = queryInfo.top;//we just  delegate to driver
            }
            if (databaseTypeName != null && databaseTypeName.indexOf("oracle") >= 0) {
                StringBuilder newSelect = new StringBuilder(ORACLE_ROWNUM_SELECT);
                if (queryInfo.top != null) {
                    newSelect.append(ORACLE_ROWNUM_TOP);
                } else {
                    newSelect.append(PAREN_CLOSE);
                }
                if (skip > 0) {
                    newSelect.append(ORACLE_ROWNUM_SKIP);
                }
                String tmp = newSelect.toString();
                tmp = tmp.replace("$$QUERY$$", select.toString());
                tmp = tmp.replace("$$TOP$$", String.valueOf(skip + maxRows.intValue()));
                tmp = tmp.replace("$$SKIP$$", String.valueOf(skip));
                select = new StringBuilder(tmp);
            } else if (databaseTypeName != null && databaseTypeName.indexOf("mysql") >= 0) {
                int hasTableAlias = select.indexOf(") a_alias0");
                if (hasTableAlias > 0) {
                    select.replace(hasTableAlias, hasTableAlias + ") a_alias0".length(), "");
                }
                if (skip > 0 && maxRows.intValue() > 0) {
                    select.append(LIMIT);
                    select.append(skip);
                    select.append(COMMA);
                    select.append(maxRows.intValue());
                } else if (skip > 0) {
                    select.append(LIMIT);
                    select.append(skip);
                    select.append(COMMA);
                    select.append(Integer.MAX_VALUE);
                } else if (maxRows.intValue() > 0) {
                    select.append(LIMIT);
                    select.append(maxRows.intValue());
                }
                if (hasTableAlias > 0) {
                    select.append(") a_alias0");
                }
            }
        }

        int fromMarkerIndex = select.indexOf(FROM_MARKER);
        if (fromMarkerIndex >= 0) {
            select.replace(fromMarkerIndex, fromMarkerIndex + FROM_MARKER.length(), " FROM " + table.tableName + " ");
        }

        logger.info(select.toString());
        return new SqlStatement(select.toString(), ImmutableList.copyOf(params), maxRows);
    }

    private void processField(String fieldName, List<String> fields, List<String> newValues) {
        String fieldToCheck = " " + fieldName.toUpperCase();
        boolean found = false;
        String possibleMatch = null;
        for (String tokenField : fields) {
            if (tokenField.toUpperCase().endsWith(fieldToCheck)) {
                newValues.add(tokenField);
                found = true;
                break;//exit for
            } else if (tokenField.toUpperCase().endsWith(fieldToCheck.trim())) {
                possibleMatch = tokenField;
            }
        }
        if (!found && possibleMatch != null) {
            newValues.add(possibleMatch);
            found = true;
        }
        if (!found) {
            logger.log(Level.WARNING, fieldName + " not found on the tokenize field list");
        }
    }

    public GenerateWhereClause newWhereClauseGenerator(EdmEntitySet entitySet, JdbcMetadataMapping mapping) {
        return new GenerateWhereClause(entitySet, mapping);
    }

}
