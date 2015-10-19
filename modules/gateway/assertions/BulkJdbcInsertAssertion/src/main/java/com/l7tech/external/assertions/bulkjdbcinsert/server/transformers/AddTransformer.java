package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by moiyu01 on 15-10-19.
 */
public final class AddTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return true;
    }

    @Override
    public boolean requiresCsvField() {
        return true;
    }

    @Override
    public boolean isParameterValid(String param) {
        try{
            int val = Integer.parseInt(param);
            if(val < 0) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        try {
            long val1 = Long.parseLong(record.get(mapper.getOrder()));
            long val2 = Long.parseLong(record.get(Integer.parseInt(mapper.getTransformParam())));
            stmt.setLong(index, val1 + val2);
        } catch(NumberFormatException nfe) {
            throw new SQLException("Invalid number format", nfe);
        }
    }
}