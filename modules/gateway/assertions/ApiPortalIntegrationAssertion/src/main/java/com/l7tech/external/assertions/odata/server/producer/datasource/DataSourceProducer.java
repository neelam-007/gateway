package com.l7tech.external.assertions.odata.server.producer.datasource;

import com.l7tech.external.assertions.odata.server.JdbcModelCache;
import com.l7tech.external.assertions.odata.server.producer.jdbc.Jdbc;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcProducerBackend;
import org.odata4j.command.Command;
import org.odata4j.command.CommandContext;
import org.odata4j.command.CommandExecution;
import org.odata4j.core.Throwables;
import org.odata4j.producer.command.CommandProducer;
import org.odata4j.producer.command.ProducerCommandContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An OData producer based on a DataSource
 *
 * @author rraquepo, 8/15/13
 */
public class DataSourceProducer extends CommandProducer {

    public static class Builder {

        private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();
        private final Map<Class<?>, List<Command<?>>> preCommands = new HashMap<Class<?>, List<Command<?>>>();
        private final Map<Class<?>, List<Command<?>>> postCommands = new HashMap<Class<?>, List<Command<?>>>();

        private BaseJdbc jdbc;
        private JdbcModelCache modelCache;

        public Builder jdbc(final BaseJdbc jdbc, final JdbcModelCache modelCache) {
            this.jdbc = jdbc;
            this.modelCache = modelCache;
            return this;
        }

        public <TContext extends ProducerCommandContext<?>> Builder insert(Class<TContext> contextType, Command<?> command) {
            return preOrPost(contextType, command, preCommands);
        }

        public <TContext extends ProducerCommandContext<?>> Builder append(Class<TContext> contextType, Command<?> command) {
            return preOrPost(contextType, command, postCommands);
        }

        private <TContext extends ProducerCommandContext<?>> Builder preOrPost(Class<TContext> contextType, Command<?> command, Map<Class<?>, List<Command<?>>> map) {
            if (!map.containsKey(contextType))
                map.put(contextType, new ArrayList<Command<?>>());
            map.get(contextType).add(command);
            return this;
        }

        public DataSourceProducer build() {
            if (jdbc == null)
                throw new IllegalArgumentException("Jdbc is mandatory");

            JdbcProducerBackend jdbcBackend = new JdbcProducerBackend(modelCache) {

                @Override
                public CommandExecution getCommandExecution() {
                    return CommandExecution.DEFAULT;
                }

                @Override
                public Jdbc getJdbc() {
                    return jdbc;
                }

                @Override
                protected <TContext extends CommandContext> List<Command<?>> getPreCommands(Class<TContext> contextType) {
                    return preCommands.get(contextType);
                }

                @Override
                protected <TContext extends CommandContext> List<Command<?>> getPostCommands(Class<TContext> contextType) {
                    return postCommands.get(contextType);
                }

                @SuppressWarnings("unchecked")
                @Override
                protected <T> T get(Class<T> instanceType) {
                    Object rt = instances.get(instanceType);
                    if (rt == null) {
                        try {
                            rt = instanceType.newInstance();
                        } catch (Exception e) {
                            throw Throwables.propagate(e);
                        }
                    }
                    return (T) rt;
                }

            };
            return new DataSourceProducer(jdbcBackend);
        }

        public <T> Builder register(Class<T> instanceType, T instance) {
            instances.put(instanceType, instance);
            return this;
        }

    }

    private final JdbcProducerBackend jdbcBackend;

    protected DataSourceProducer(JdbcProducerBackend jdbcBackend) {
        super(jdbcBackend);
        this.jdbcBackend = jdbcBackend;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Jdbc getJdbc() {
        return jdbcBackend.getJdbc();
    }

}

