package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcSchema;
import org.core4j.Enumerable;
import org.odata4j.core.Action1;

public class LimitJdbcModelToDefaultSchema implements Action1<JdbcModel> {

    @Override
    public void apply(JdbcModel model) {
        for (JdbcSchema schema : Enumerable.create(model.schemas).toList()) {
            if (!schema.isDefault)
                model.schemas.remove(schema);
        }
    }

}
