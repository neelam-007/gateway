package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by moiyu01 on 15-10-19.
 */
public class SetIntValueTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return true;
    }

    @Override
    public boolean requiresCsvField() {
        return false;
    }

    @Override
    public boolean isParameterValid(String param) {
        try{
            Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        try {
            int val = Integer.parseInt(mapper.getTransformParam());
            stmt.setLong(index, val);
        } catch(NumberFormatException nfe) {
            throw new SQLException("Invalid number format", nfe);
        }
    }
}
