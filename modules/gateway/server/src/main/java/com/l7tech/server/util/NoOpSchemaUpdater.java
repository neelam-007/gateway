package com.l7tech.server.util;

import java.util.logging.Logger;

/**
 * SchemaUpdater for contexts which do not require schema updates.
 */
public class NoOpSchemaUpdater implements SchemaUpdater {
    private static final Logger logger = Logger.getLogger(NoOpSchemaUpdater.class.getName());

    @Override
    public void ensureCurrentSchema() throws SchemaException {
        logger.info("No schema update required");
    }
}
