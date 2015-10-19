package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by moiyu01 on 15-10-19.
 */
public final class StringTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return false;
    }

    @Override
    public boolean requiresCsvField() {
        return true;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        String val = record.get(mapper.getOrder());
        stmt.setString(index, val);
    }
}
