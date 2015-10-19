package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by moiyu01 on 15-10-19.
 */
public class SetStringTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return true;
    }

    @Override
    public boolean requiresCsvField() {
        return false;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        stmt.setString(index, mapper.getTransformParam());
    }
}
