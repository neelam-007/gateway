package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.JdbcModelCache;
import com.l7tech.external.assertions.odata.server.producer.datasource.BaseJdbc;
import org.odata4j.command.Command;
import org.odata4j.command.CommandResult;
import org.odata4j.producer.command.GetMetadataCommandContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcGetMetadataCommand implements Command<GetMetadataCommandContext> {

    private static final Logger logger = Logger.getLogger(JdbcModelCache.class.getName());

    private JdbcModelCache modelCache;

    public JdbcGetMetadataCommand(JdbcModelCache modelCache) {
        this.modelCache = modelCache;
    }

    @Override
    public CommandResult execute(GetMetadataCommandContext context) throws Exception {
        JdbcProducerCommandContext jdbcContext = (JdbcProducerCommandContext) context;

        // 1. generate jdbc model
        JdbcModel model = generateModel(jdbcContext);

        // 2. apply model cleanup
        cleanupModel(model);

        // 3. project jdbc model into edm metadata
        JdbcMetadataMapping mapping = modelToMapping(jdbcContext, model);

        context.setResult(mapping);
        return CommandResult.CONTINUE;
    }

    public JdbcModel generateModel(JdbcProducerCommandContext jdbcContext) {
        String hashCode = String.valueOf(((BaseJdbc) jdbcContext.getJdbc()).getDataSource().hashCode());
        JdbcModel model = null;
        if (modelCache != null) {
            model = modelCache.get(hashCode);
        }
        if (model == null) {
            logger.log(Level.INFO, "{0} cache miss", new String[]{hashCode});
            model = jdbcContext.getJdbc().execute(new GenerateJdbcModel());
            modelCache.put(hashCode, model);
        } else {
            logger.log(Level.INFO, "{0} cache hit", new String[]{hashCode});
        }
        return model;
    }

    public void cleanupModel(JdbcModel model) {
        new LimitJdbcModelToDefaultSchema().apply(model);
    }

    public JdbcMetadataMapping modelToMapping(JdbcProducerCommandContext jdbcContext, JdbcModel model) {
        return jdbcContext.get(JdbcModelToMetadata.class).apply(model);
    }

}