package com.l7tech.server.util;

import com.l7tech.server.management.db.LiquibaseDBManager;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import liquibase.exception.LiquibaseException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG schema updater for embedded database.
 */
public class EmbeddedDbSchemaUpdater extends JdbcDaoSupport implements SchemaUpdater {
    private static final Logger logger = Logger.getLogger(EmbeddedDbSchemaUpdater.class.getName());
    private LiquibaseDBManager dbManager;

    /**
     * Only valid upgrade scripts in the given directory will be used.
     *
     * @param transactionManager the PlatformTransactionManager to use for the schema update transaction.
     * @param resourceDirectory  the resource directory which contains the upgrade scripts.
     * @throws IOException
     */
    public EmbeddedDbSchemaUpdater(@NotNull final PlatformTransactionManager transactionManager, @NotNull final String resourceDirectory, @NotNull final LiquibaseDBManager dbManager) throws IOException {
        this.transactionManager = transactionManager;
        try {
            //get the sql upgrade files
            final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(resourceDirectory + "/*.sql");
            for (final Resource resource : resources) {
                if (resource.exists()) {
                    final String resourcePath = resource.getURL().getPath();
                    final int slashIndex = resourcePath.lastIndexOf('/');
                    final String resourceName = resourcePath.substring(slashIndex + 1);
                    final Triple<String, String, String> upgradeInfo = DbUpgradeUtil.isUpgradeScript(resourceName);
                    if (upgradeInfo != null) {
                        if (upgradeMap.containsKey(upgradeInfo.left)) {
                            Triple<String, Resource, Resource> info = upgradeMap.get(upgradeInfo.left);
                            upgradeMap.put(upgradeInfo.left, new Triple<>(upgradeInfo.middle, info.middle, resource));
                        } else {
                            upgradeMap.put(upgradeInfo.left, new Triple<String, Resource, Resource>(upgradeInfo.middle, resource, null));
                        }

                    }
                }
            }
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException(resourceDirectory + " does not exist.");
        }
        this.dbManager = dbManager;
    }

    /**
     * Upgrades the SSG embedded database in a single transaction if necessary. Errors will cause the transaction to
     * roll back.
     * <p/>
     * Will perform a legacy upgrade for pre 8.3 versions.
     * <p/>
     *
     * @throws SchemaException if the upgrade could not be completed.
     */
    @Override
    public void ensureCurrentSchema() throws SchemaException {
        //gets the current product version
        final String newVersion = getProductVersion();
        if (StringUtils.isBlank(newVersion)) {
            throw new SchemaException("Error reading current build version");
        }
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                Connection connection = getConnection();
                try {
                    connection.setAutoCommit(false);
                    if (!dbManager.isLiquibaseDB(connection)) {
                        //if the connection is not managed by liquibase then it is an older db version and needs to have the legacy upgrade performed on it.
                        legacyUpgrade(connection, newVersion);
                        dbManager.updatePreliquibaseDB(connection);
                    } else {
                        //perform the liquibase upgrade
                        dbManager.ensureSchema(connection);
                    }

                    String dbVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(connection);
                    if (!newVersion.equals(dbVersion)) {
                        String msg = "Could not upgrade to: " + newVersion;
                        logger.warning(msg);
                        throw new SchemaException(msg);
                    }
                } catch (LiquibaseException e) {
                    String msg = "Could not apply upgrade: " + ExceptionUtils.getMessage(e);
                    logger.warning(msg);
                    throw new SchemaException(msg, e);
                } catch (Throwable t) {
                    throw new SchemaException("Error executing upgrade", t);
                } finally {
                    releaseConnection(connection);
                }
            }
        });
    }

    private void legacyUpgrade(@NotNull final Connection connection, @NotNull final String newVersion) {
        String dbVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(connection);
        if (StringUtils.isBlank(dbVersion)) {
            throw new SchemaException("Error reading current version from database");
        }
        while (!LiquibaseDBManager.JAVELIN_PRE_DB_VERSION.equals(dbVersion)) {
            final Triple<String, Resource, Resource> upgradeInfo = upgradeMap.get(dbVersion);
            if (upgradeInfo == null) {
                final String msg = "No upgrade path from \"" + dbVersion + "\" to \"" + newVersion + "\"";
                logger.warning(msg);
                throw new SchemaException(msg);
            } else {
                upgradeSingleVersion(dbVersion, upgradeInfo, connection);
                dbVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(connection);
            }
        }
    }

    /**
     * Performs one version upgrade.
     */
    private void upgradeSingleVersion(@NotNull final String dbVersion, @NotNull final Triple<String, Resource, Resource> upgradeInfo, @NotNull final Connection connection) {
        final String nextVersion = upgradeInfo.left;
        final Resource upgradeResource;
        if (upgradeInfo.right == null) {
            upgradeResource = upgradeInfo.middle;
        } else {
            upgradeResource = upgradeInfo.middle.getFilename().contains(DbUpgradeUtil.UPGRADE_TRY_SUFFIX) ? upgradeInfo.middle : upgradeInfo.right;
        }

        if (StringUtils.isBlank(nextVersion) || upgradeResource == null) {
            throw new SchemaException("Unknown next version or upgrade resource");
        }
        logger.info("Upgrading db from " + dbVersion + "->" + nextVersion);
        try {
            DerbyDbHelper.runScripts(connection, new Resource[]{upgradeResource}, false);
        } catch (final SQLException e) {
            if (upgradeInfo.right != null) {
                final Resource checkSuccessResource = upgradeInfo.middle.getFilename().contains(DbUpgradeUtil.UPGRADE_SUCCESS_SUFFIX) ? upgradeInfo.middle : upgradeInfo.right;
                try {
                    String[] statements = DerbyDbHelper.getSqlStatements(checkSuccessResource);
                    logger.info("Using upgrade script: " + checkSuccessResource.getFilename());
                    Statement statement = null;
                    for (int i = 0; i < statements.length - 1; ++i) {
                        String sql = statements[i];
                        statement = connection.createStatement();
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Running statement: " + sql);
                        }
                        statement.executeUpdate(sql);
                    }

                    String lastLine = statements[statements.length - 1];
                    logger.log(Level.FINE, "Running statement: " + lastLine);
                    statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(lastLine);
                    if (!result.next()) {
                        throw new SQLException("Error checking success, no result returned.");
                    }

                } catch (SQLException e1) {
                    throw new SchemaException("Error executing upgrade", e1);
                } catch (IOException e2) {
                    throw new SchemaException("Error executing upgrade", e2);
                }
            } else {
                throw new SchemaException("Error executing upgrade", e);
            }
        }
        logger.info("Completed upgrade from " + dbVersion + "->" + nextVersion);
    }

    /**
     * Overridden in unit tests.
     */
    protected String getProductVersion() {
        return BuildInfo.getFormalProductVersion();
    }

    private final PlatformTransactionManager transactionManager;
    private final Map<String, Triple<String, Resource, Resource>> upgradeMap = new HashMap<>();
}
