package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.JdbcModelCache;
import org.odata4j.command.ChainCommand;
import org.odata4j.command.Command;
import org.odata4j.command.CommandContext;
import org.odata4j.command.CommandExecution;
import org.odata4j.core.*;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.command.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

public abstract class JdbcProducerBackend implements CommandProducerBackend {

    private JdbcModelCache modelCache;

    public JdbcProducerBackend() {
    }

    public JdbcProducerBackend(JdbcModelCache modelCache) {
        this.modelCache = modelCache;
    }

    @Override
    abstract public CommandExecution getCommandExecution();

    abstract public Jdbc getJdbc();

    abstract protected <TContext extends CommandContext> List<Command<?>> getPreCommands(Class<TContext> contextType);

    abstract protected <TContext extends CommandContext> List<Command<?>> getPostCommands(Class<TContext> contextType);

    abstract protected <T> T get(Class<T> instanceType);

    public JdbcMetadataMapping getMetadataMapping() {
        GetMetadataCommandContext context = newGetMetadataCommandContext();
        try {
            getCommand(GetMetadataCommandContext.class).execute(context);
            return (JdbcMetadataMapping) context.getResult();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public JdbcProducerCommandContext newJdbcCommandContext() {
        return new JdbcProducerCommandContext() {

            @Override
            public Jdbc getJdbc() {
                return JdbcProducerBackend.this.getJdbc();
            }

            @Override
            public JdbcProducerBackend getBackend() {
                return JdbcProducerBackend.this;
            }

            @Override
            public <T> T get(Class<T> instanceType) {
                return JdbcProducerBackend.this.get(instanceType);
            }
        };
    }

    @Override
    public <TContext extends CommandContext> Command<TContext> getCommand(Class<TContext> contextType) {

        ChainCommand.Builder<TContext> chain = ChainCommand.newBuilder();
        chain.addAll(getPreCommands(ProducerCommandContext.class));
        if (CloseCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(CloseCommandContext.class));
            chain.addAll(getPostCommands(CloseCommandContext.class));
        } else if (GetMetadataCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(GetMetadataCommandContext.class));
            chain.add(new JdbcGetMetadataCommand(modelCache));
            chain.addAll(getPostCommands(GetMetadataCommandContext.class));
        } else if (GetEntitiesCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(GetEntitiesCommandContext.class));
            chain.add(new JdbcGetEntitiesCommand());
            chain.addAll(getPostCommands(GetEntitiesCommandContext.class));
        } else if (GetEntityCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(GetEntityCommandContext.class));
            chain.add(new JdbcGetEntityCommand());
            chain.addAll(getPostCommands(GetEntityCommandContext.class));
        } else if (CreateEntityCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(CreateEntityCommandContext.class));
            chain.add(new JdbcCreateEntityCommand());
            chain.addAll(getPostCommands(CreateEntityCommandContext.class));
        } else if (DeleteEntityCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(DeleteEntityCommandContext.class));
            chain.add(new JdbcDeleteEntityCommand());
            chain.addAll(getPostCommands(DeleteEntityCommandContext.class));
        } else if (GetEntitiesCountCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(GetEntitiesCountCommandContext.class));
            chain.add(new JdbcGetEntitiesCountCommand());
            chain.addAll(getPostCommands(GetEntitiesCountCommandContext.class));
        } else if (UpdateEntityCommandContext.class.isAssignableFrom(contextType)) {
            chain.addAll(getPreCommands(UpdateEntityCommandContext.class));
            chain.add(new JdbcUpdateEntityCommand());
            chain.addAll(getPostCommands(UpdateEntityCommandContext.class));
        } else {
            throw new UnsupportedOperationException("TODO implement: " + contextType.getSimpleName());
        }
        chain.addAll(getPostCommands(ProducerCommandContext.class));
        return chain.build();
    }

    @SuppressWarnings("unchecked")
    private <T> T newContext(Class<?> contextType, Object... args) {
        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{contextType, JdbcProducerCommandContext.class},
                new JdbcProducerBackendInvocationHandler(this, contextType, args));
    }

    @Override
    public CloseCommandContext newCloseCommandContext() {
        return newContext(CloseCommandContext.class);
    }

    @Override
    public GetMetadataCommandContext newGetMetadataCommandContext() {
        return newContext(GetMetadataCommandContext.class);
    }

    @Override
    public GetEntitiesCommandContext newGetEntitiesCommandContext(String entitySetName, QueryInfo queryInfo) {
        return newContext(GetEntitiesCommandContext.class,
                "entitySetName", entitySetName,
                "queryInfo", queryInfo);
    }

    @Override
    public GetEntityCommandContext newGetEntityCommandContext(String entitySetName, OEntityKey entityKey, EntityQueryInfo queryInfo) {
        return newContext(GetEntityCommandContext.class,
                "entitySetName", entitySetName,
                "entityKey", entityKey,
                "queryInfo", queryInfo);
    }

    @Override
    public GetMetadataProducerCommandContext newGetMetadataProducerCommandContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetEntitiesCountCommandContext newGetEntitiesCountCommandContext(String entitySetName, QueryInfo queryInfo) {
        return newContext(GetEntitiesCountCommandContext.class,
                "entitySetName", entitySetName,
                "queryInfo", queryInfo);
    }

    @Override
    public GetNavPropertyCommandContext newGetNavPropertyCommandContext(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetNavPropertyCountCommandContext newGetNavPropertyCountCommandContext(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateEntityCommandContext newCreateEntityCommandContext(String entitySetName, OEntity entity) {
        return newContext(CreateEntityCommandContext.class,
                "entitySetName", entitySetName,
                "entity", entity);
    }

    @Override
    public CreateEntityAtPropertyCommandContext newCreateEntityAtPropertyCommandContext(String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteEntityCommandContext newDeleteEntityCommandContext(String entitySetName, OEntityKey entityKey) {
        return newContext(DeleteEntityCommandContext.class,
                "entitySetName", entitySetName,
                "entityKey", entityKey);
    }

    @Override
    public MergeEntityCommandContext newMergeEntityCommandContext(String entitySetName, OEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpdateEntityCommandContext newUpdateEntityCommandContext(String entitySetName, OEntity entity) {
        return newContext(UpdateEntityCommandContext.class,
                "entitySetName", entitySetName,
                "entity", entity);
    }

    @Override
    public GetLinksCommandContext newGetLinksCommandContext(OEntityId sourceEntity, String targetNavProp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateLinkCommandContext newCreateLinkCommandContext(OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpdateLinkCommandContext newUpdateLinkCommandContext(OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteLinkCommandContext newDeleteLinkCommandContext(OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CallFunctionCommandContext newCallFunctionCommandContext(EdmFunctionImport name, Map<String, OFunctionParameter> params, QueryInfo queryInfo) {
        throw new UnsupportedOperationException();
    }

}