package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.core4j.ThrowingFunc1;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.command.GetEntityCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JdbcGetEntityCommand extends JdbcBaseCommand implements Command<GetEntityCommandContext> {

    @Override
    public CommandResult execute(final GetEntityCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        String entitySetName = context.getEntitySetName();

        final JdbcMetadataMapping mapping = jdbcContext.getBackend().getMetadataMapping();
        final EdmEntitySet entitySet = mapping.getMetadata().findEdmEntitySet(entitySetName);
        if (entitySet == null)
            throw new NotFoundException();

        GenerateSqlQuery queryGen = jdbcContext.get(GenerateSqlQuery.class);
        BoolCommonExpression filter = context.getQueryInfo() == null ? null : context.getQueryInfo().filter;
        filter = prependPrimaryKeyFilter(mapping, entitySet.getType(), context.getEntityKey(), filter);
        final QueryInfo queryInfo = context.getQueryInfo();
        final SqlStatement sqlStatement = queryGen.generate(mapping, entitySet, context.getQueryInfo(), filter);
        OEntity entity = jdbcContext.getJdbc().execute(new ThrowingFunc1<Connection, OEntity>() {
            @Override
            public OEntity apply(Connection conn) throws Exception {
                PreparedStatement stmt = sqlStatement.asPreparedStatement(conn);
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    return toOEntity(mapping, entitySet, results, queryInfo);
                }
                return null;
            }
        });

        if (entity == null)
            throw new NotFoundException();

        EntityResponse response = Responses.entity(entity);
        context.setResult(response);
        return CommandResult.CONTINUE;
    }

}