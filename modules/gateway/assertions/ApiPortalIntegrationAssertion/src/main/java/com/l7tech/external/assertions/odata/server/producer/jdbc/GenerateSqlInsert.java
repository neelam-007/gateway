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

import java.util.ArrayList;
import java.util.List;

public class GenerateSqlInsert {

    public SqlStatement generate(JdbcMetadataMapping mapping, EdmEntitySet entitySet, OEntity entity, final String generatedUUID) {
        JdbcTable table = mapping.getMappedTable(entitySet);
        StringBuilder sql = new StringBuilder("INSERT INTO " + table.tableName + "(");
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<SqlParameter> params = new ArrayList<SqlParameter>();
        final EdmEntityType eet = entitySet.getType();
        final List<String> keys = eet.getKeys();
        for (OProperty<?> prop : entity.getProperties()) {
            //if it's a key, it's a number type and is -1, most probably an autogen field
            //find the actual field name for the key
            //TODO: do we need to actually handle a request where the id was not part of the request?
            boolean skipThisFieldForInsert = false;
            boolean generateGUIDForThisField = false;
            for (String key : keys) {
                if (prop.getName().equalsIgnoreCase(key)) {
                    //if the value if -1,we need to skip this field
                    if (prop.getValue() == null || (prop.getValue() != null && prop.getValue().toString().equals("-1"))) {
                        skipThisFieldForInsert = true;
                    } else if (prop.getValue() != null && prop.getValue().toString().equals(JdbcCreateEntityCommand.MAGIC_STRING_UUID)) {
                        generateGUIDForThisField = true;
                    }
                    break;
                }
                break;
            }

            EdmProperty edmProp = entitySet.getType().findProperty(prop.getName());
            if (edmProp == null) {
                throw new BadRequestException("Invalid field and/or property - " + prop.getName());
            }
            JdbcColumn column = mapping.getMappedColumn(edmProp);
            if (columns.length() > 0)
                columns.append(", ");
            if (values.length() > 0)
                values.append(", ");
            columns.append(column.columnName);
            if (skipThisFieldForInsert) {
                values.append("NULL");
            } else if (generateGUIDForThisField) {
                values.append("?");
                params.add(new SqlParameter(generatedUUID, null));
            } else {
                values.append("?");
                params.add(new SqlParameter(prop.getValue(), null));
            }
        }
        sql.append(columns);
        sql.append(") VALUES (");
        sql.append(values);
        sql.append(")");

        return new SqlStatement(sql.toString(), ImmutableList.copyOf(params));
    }

}
