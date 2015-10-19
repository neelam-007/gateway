package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import org.apache.commons.csv.CSVRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by moiyu01 on 15-10-19.
 */
public final class Regex2IntTransformer extends AbstractTransformer {
    @Override
    public boolean requiresParameter() {
        return true;
    }

    @Override
    public boolean requiresCsvField() {
        return true;
    }

    @Override
    public void transform(PreparedStatement stmt, int index, BulkJdbcInsertAssertion.ColumnMapper mapper, CSVRecord record) throws SQLException {
        String val = record.get(mapper.getOrder());
        String transformParam = mapper.getTransformParam();
        stmt.setInt(index, convertRegex2Int(val, transformParam));
    }

    private int convertRegex2Int(String val, String transformParam) {
        Pattern regex = Pattern.compile(transformParam, Pattern.CASE_INSENSITIVE);
        Matcher m = regex.matcher(val);
        return m.find()?1:0;
    }
}
