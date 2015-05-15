package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.core4j.ThrowingFunc1;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.command.GetEntitiesCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class JdbcGetEntitiesCommand extends JdbcBaseCommand implements Command<GetEntitiesCommandContext> {

    @Override
    public CommandResult execute(GetEntitiesCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        String entitySetName = context.getEntitySetName();

        final JdbcMetadataMapping mapping = jdbcContext.getBackend().getMetadataMapping();
        final EdmEntitySet entitySet = mapping.getMetadata().findEdmEntitySet(entitySetName);
        if (entitySet == null)
            throw new NotFoundException();

        GenerateSqlQuery queryGen = jdbcContext.get(GenerateSqlQuery.class);
        final QueryInfo queryInfo = context.getQueryInfo();
        final BoolCommonExpression filter = context.getQueryInfo() == null ? null : queryInfo.filter;
        final SqlStatement sqlStatement = queryGen.generate(mapping, entitySet, context.getQueryInfo(), filter, getDatabaseName(context));
        final List<OEntity> entities = new ArrayList<OEntity>();


        final CountResult countResult = new CountResult(0);
        final SqlStatement inlineCountSqlStatement;

        if (queryInfo.inlineCount != null && queryInfo.inlineCount == InlineCount.ALLPAGES) {
            //inlineCount = new Integer(entities.size());
            GenerateCountQuery queryGen2 = jdbcContext.get(GenerateCountQuery.class);
            inlineCountSqlStatement = queryGen2.generate(mapping, entitySet, context.getQueryInfo(), filter, getDatabaseName(context), true);
        } else {
            inlineCountSqlStatement = null;
        }

        jdbcContext.getJdbc().execute(new ThrowingFunc1<Connection, Void>() {
            @Override
            public Void apply(Connection conn) throws Exception {
                PreparedStatement stmt = sqlStatement.asPreparedStatement(conn);
                ResultSet results = stmt.executeQuery();
                while (results.next()) {
                    OEntity entity = toOEntity(mapping, entitySet, results, queryInfo);
                    entities.add(entity);
                }
                if (inlineCountSqlStatement != null) {
                    PreparedStatement inlineCountStmt = inlineCountSqlStatement.asPreparedStatement(conn);
                    ResultSet inlineCountResult = inlineCountStmt.executeQuery();
                    if (inlineCountResult.next()) {
                        countResult.setCount(inlineCountResult.getLong("ctr"));
                    }
                }
                return null;
            }
        });

        Integer inlineCount = null;
        if (inlineCountSqlStatement != null) {
            inlineCount = new Integer(String.valueOf(countResult.getCount()));
        }

        String skipToken = null;//TODO: implement $skipToken, we won't have paging limit set for now, so should be fine

        EntitiesResponse response = Responses.entities(entities, entitySet, inlineCount, skipToken);
        context.setResult(response);
        return CommandResult.CONTINUE;
    }

}