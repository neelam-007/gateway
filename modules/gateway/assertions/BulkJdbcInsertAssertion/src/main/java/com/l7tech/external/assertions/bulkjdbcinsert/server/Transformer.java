package com.l7tech.external.assertions.bulkjdbcinsert.server;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by moiyu01 on 15-10-19.
 */
public interface Transformer {
    boolean requiresParameter();
    boolean isParameterValid(String param);
    boolean requiresCsvField();
    void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException;
}
