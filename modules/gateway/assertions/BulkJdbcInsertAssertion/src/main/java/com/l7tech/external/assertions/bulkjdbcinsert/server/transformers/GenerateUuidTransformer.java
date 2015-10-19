package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by moiyu01 on 15-10-19.
 */
public class GenerateUuidTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return false;
    }

    @Override
    public boolean requiresCsvField() {
        return false;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        stmt.setString(index, UUID.randomUUID().toString());
    }
}
