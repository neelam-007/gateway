package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import org.odata4j.core.ImmutableList;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.QueryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GenerateCountQuery extends GenerateSqlQuery {

    private static final Logger logger = Logger.getLogger(GenerateCountQuery.class.getName());

    final static String SELECT_COUNT = " SELECT COUNT(*) AS ctr ";
    final static String SELECT_COUNT_MYSQL = " SELECT FOUND_ROWS() AS ctr ";

    public SqlStatement generate(JdbcMetadataMapping mapping, EdmEntitySet entitySet, QueryInfo queryInfo, BoolCommonExpression filter, String databaseTypeName, boolean isInlineCount) {
        JdbcTable table = mapping.getMappedTable(entitySet);
        logger.fine("databaseTypeName=" + databaseTypeName + ",isInlineCount=" + isInlineCount);

        SqlStatement sqlStatement = super.generate(mapping, entitySet, queryInfo, filter, databaseTypeName);
        StringBuffer sb = new StringBuffer(SELECT_COUNT);

        String normalizeQuey = Util.normalizeQuery(sqlStatement.sql);
        sb.append(" FROM ( ");
        int fromIndex = 0;
        int orderByIndex = normalizeQuey.toUpperCase().indexOf(" ORDER BY ");//order by doesn't affect total count to strip it out!!! - rraquepo
        if (orderByIndex < 0) {
            orderByIndex = normalizeQuey.length();
        }
        if (normalizeQuey.indexOf(") a_alias0") >= 0) {
            sb.append(normalizeQuey.substring(fromIndex, orderByIndex));
            if (sb.indexOf(") a_alias0") < 0) {
                sb.append(") a_alias0");
            }
        } else {
            sb.append(normalizeQuey.substring(fromIndex, orderByIndex));
        }
        sb.append(" ) ctr_alias");
        return new SqlStatement(sb.toString(), sqlStatement.params);
    }
}
