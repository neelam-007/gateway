package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcColumn;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import com.l7tech.external.assertions.odata.server.producer.jdbc.SqlStatement.SqlParameter;
import org.odata4j.core.ImmutableList;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.expression.BoolCommonExpression;

import java.util.ArrayList;
import java.util.List;

public class GenerateSqlUpdate {

    public SqlStatement generate(JdbcMetadataMapping mapping, EdmEntitySet entitySet, OEntity entity, BoolCommonExpression filter) {
        JdbcTable table = mapping.getMappedTable(entitySet);
        StringBuilder sql = new StringBuilder("UPDATE " + table.tableName + " SET ");
        StringBuilder columns = new StringBuilder();
        List<SqlParameter> params = new ArrayList<SqlParameter>();
        EdmEntityType eet = entitySet.getType();
        List<String> keys = eet.getKeys();
        for (OProperty<?> prop : entity.getProperties()) {
            EdmProperty edmProp = eet.findProperty(prop.getName());
            if (edmProp == null) {
                throw new BadRequestException("Invalid field and/or property - " + prop.getName());
            }
            JdbcColumn column = mapping.getMappedColumn(edmProp);
            boolean isFieldKey = false;
            for (String key : keys) {
                //find the actual field name for the key
                EdmProperty edmProperty = eet.findDeclaredProperty(key);
                JdbcModel.JdbcColumn keyColumn = mapping.getMappedColumn(edmProperty);
                String colName = keyColumn.columnName;
                if (column.columnName.equals(colName)) {
                    isFieldKey = true;
                    break;
                }
            }
            if (!isFieldKey) {
                if (columns.length() > 0)
                    columns.append(", ");
                columns.append(column.columnName);
                columns.append(" = ?");

                params.add(new SqlParameter(prop.getValue(), null));
            }
        }
        sql.append(columns);

        int noKeysParamSize = params.size();//number of params w/o the keys included
        if (filter != null) {
            GenerateWhereClause whereClauseGen = newWhereClauseGenerator(entitySet, mapping);
            filter.visit(whereClauseGen);
            whereClauseGen.append(sql, params);
        }
        //params will have increased to include the keys submitted
        //if it's less than the expected keys define in the entity, we don't want to perform the update
        //as it may update more than one record
        if (params.size() - noKeysParamSize != keys.size()) {
            throw new BadRequestException("Need all keys to be part of the request when performing update");
        }

        return new SqlStatement(sql.toString(), ImmutableList.copyOf(params));
    }

    public GenerateWhereClause newWhereClauseGenerator(EdmEntitySet entitySet, JdbcMetadataMapping mapping) {
        return new GenerateWhereClause(entitySet, mapping);
    }

}
