package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.core4j.ThrowingFunc1;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.command.UpdateEntityCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class JdbcUpdateEntityCommand extends JdbcBaseCommand implements Command<UpdateEntityCommandContext> {

    @Override
    public CommandResult execute(UpdateEntityCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        String entitySetName = context.getEntitySetName();

        final JdbcMetadataMapping mapping = jdbcContext.getBackend().getMetadataMapping();
        final EdmEntitySet entitySet = mapping.getMetadata().findEdmEntitySet(entitySetName);
        if (entitySet == null)
            throw new NotFoundException();

        GenerateSqlUpdate insertGen = jdbcContext.get(GenerateSqlUpdate.class);
        BoolCommonExpression filter = prependPrimaryKeyFilter(mapping, entitySet.getType(), context.getEntity().getEntityKey(), null);
        final SqlStatement sqlStatement = insertGen.generate(mapping, entitySet, context.getEntity(), filter);
        jdbcContext.getJdbc().execute(new ThrowingFunc1<Connection, Void>() {
            @Override
            public Void apply(Connection conn) throws Exception {
                PreparedStatement stmt = sqlStatement.asPreparedStatement(conn);
                int updated = stmt.executeUpdate();
                if (updated == 0)
                    throw new BadRequestException("Entity not updated");
                return null;
            }
        });


        return CommandResult.CONTINUE;
    }

}
