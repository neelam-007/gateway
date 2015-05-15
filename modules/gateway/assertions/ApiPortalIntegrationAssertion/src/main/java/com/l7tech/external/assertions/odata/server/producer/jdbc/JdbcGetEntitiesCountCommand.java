package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.core4j.ThrowingFunc1;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.command.GetEntitiesCountCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JdbcGetEntitiesCountCommand extends JdbcBaseCommand implements Command<GetEntitiesCountCommandContext> {

    @Override
    public CommandResult execute(GetEntitiesCountCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        String entitySetName = context.getEntitySetName();

        final JdbcMetadataMapping mapping = jdbcContext.getBackend().getMetadataMapping();
        final EdmEntitySet entitySet = mapping.getMetadata().findEdmEntitySet(entitySetName);
        if (entitySet == null)
            throw new NotFoundException();

        GenerateCountQuery queryGen = jdbcContext.get(GenerateCountQuery.class);
        BoolCommonExpression filter = context.getQueryInfo() == null ? null : context.getQueryInfo().filter;
        final SqlStatement sqlStatement = queryGen.generate(mapping, entitySet, context.getQueryInfo(), filter, getDatabaseName(context), false);
        final CountResult countResult = new CountResult(0);
        jdbcContext.getJdbc().execute(new ThrowingFunc1<Connection, Void>() {
            @Override
            public Void apply(Connection conn) throws Exception {
                PreparedStatement stmt = sqlStatement.asPreparedStatement(conn);
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    countResult.setCount(results.getLong("ctr"));
                }
                return null;
            }
        });

        context.setResult(countResult);
        return CommandResult.CONTINUE;
    }

}