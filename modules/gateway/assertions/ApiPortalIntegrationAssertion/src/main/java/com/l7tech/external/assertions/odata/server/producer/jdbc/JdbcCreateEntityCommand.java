package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.core4j.ThrowingFunc1;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.Responses;
import org.odata4j.producer.command.CreateEntityCommandContext;
import org.odata4j.producer.command.GetEntityCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JdbcCreateEntityCommand implements Command<CreateEntityCommandContext> {

    public static final String MAGIC_STRING_UUID = "{{GENERATED_GUID}}";

    @Override
    public CommandResult execute(CreateEntityCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        String entitySetName = context.getEntitySetName();

        final JdbcMetadataMapping mapping = jdbcContext.getBackend().getMetadataMapping();
        final EdmEntitySet entitySet = mapping.getMetadata().findEdmEntitySet(entitySetName);
        if (entitySet == null)
            throw new NotFoundException();

        GenerateSqlInsert insertGen = jdbcContext.get(GenerateSqlInsert.class);
        final OEntity oEntity = context.getEntity();
        String generatedUUID = null;
        final EdmEntityType eet = entitySet.getType();
        final List<String> keys = eet.getKeys();
        final BooleanHolder keyHolder = new BooleanHolder();
        //TODO: we can only check for the first key at the moment, for auto-increment or auto-generated
        for (String key : keys) {
            keyHolder.keyName = key;
            for (OProperty<?> prop : oEntity.getProperties()) {
                if (prop.getName().equals(key) && prop.getValue() != null) {
                    if (prop.getValue().toString().equals(MAGIC_STRING_UUID)) {
                        generatedUUID = UUID.randomUUID().toString();
                    } else if (!prop.getValue().toString().equals("-1")) {
                        keyHolder.notInRequestFlag = false;
                    }
                    break;
                }
            }
            break;
        }
        final SqlStatement sqlStatement = insertGen.generate(mapping, entitySet, context.getEntity(), generatedUUID);
        final boolean hasGeneratedId = generatedUUID != null;
        jdbcContext.getJdbc().execute(new ThrowingFunc1<Connection, Void>() {
            @Override
            public Void apply(Connection conn) throws Exception {
                PreparedStatement stmt = sqlStatement.asPreparedStatement(conn);
                int updated = stmt.executeUpdate();
                if (updated == 0)
                    throw new BadRequestException("Entity not inserted");

                if (!hasGeneratedId && keyHolder.notInRequestFlag) {
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    boolean foundKeys = false;
                    if (generatedKeys.next()) {
                        keyHolder.id = generatedKeys.getLong(1);
                        foundKeys = true;
                    }
                    if (!foundKeys) {
                        PreparedStatement psIdentity = conn.prepareStatement("CALL IDENTITY()");
                        ResultSet result = psIdentity.executeQuery();
                        if (result.next()) {
                            keyHolder.id = result.getLong(1);
                        }
                    }
                }

                return null;
            }
        });

        // now re-query for inserted entity
        OEntityKey entityKey;
        if (generatedUUID != null) {
            HashMap<String, Object> entityKeys = new HashMap();
            entityKeys.put(keyHolder.keyName, generatedUUID);
            entityKey = OEntityKey.create(entityKeys);
        } else if (keyHolder.notInRequestFlag && keyHolder.id > 0) {
            HashMap<String, Object> entityKeys = new HashMap();
            entityKeys.put(keyHolder.keyName, keyHolder.id);
            entityKey = OEntityKey.create(entityKeys);
        } else {
            entityKey = OEntityKey.infer(entitySet, context.getEntity().getProperties());
        }
        GetEntityCommandContext getEntityCommandContext = jdbcContext.getBackend().newGetEntityCommandContext(entitySetName, entityKey, null);
        jdbcContext.getBackend().getCommand(GetEntityCommandContext.class).execute(getEntityCommandContext);
        OEntity newEntity = getEntityCommandContext.getResult().getEntity();

        EntityResponse response = Responses.entity(newEntity);
        context.setResult(response);
        return CommandResult.CONTINUE;
    }

    protected class BooleanHolder {
        protected boolean notInRequestFlag = true;
        protected String keyName;
        protected long id = -1;
    }

}


