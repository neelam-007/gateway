package com.l7tech.server.util;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SSG schema updater for embedded database.
 */
public class EmbeddedDbSchemaUpdater extends JdbcDaoSupport implements SchemaUpdater {
    private static final Logger logger = Logger.getLogger(EmbeddedDbSchemaUpdater.class.getName());

    /**
     * Only valid upgrade scripts in the given directory will be used.
     *
     * @param transactionManager the PlatformTransactionManager to use for the schema update transaction.
     * @param resourceDirectory the resource directory which contains the upgrade scripts.
     * @throws IOException
     */
    public EmbeddedDbSchemaUpdater(@NotNull final PlatformTransactionManager transactionManager, @NotNull final String resourceDirectory) throws IOException {
        this.transactionManager = transactionManager;
        try {
            final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(resourceDirectory + "/*.sql");
            for (final Resource resource : resources) {
                if (resource.exists()) {
                    final String resourcePath = resource.getURL().getPath();
                    final int slashIndex = resourcePath.lastIndexOf('/');
                    final String resourceName = resourcePath.substring(slashIndex + 1);
                    final Pair<String, String> upgradeInfo = DbUpgradeUtil.isUpgradeScript(resourceName);
                    if (upgradeInfo != null) {
                        upgradeMap.put(upgradeInfo.getKey(), new Pair<String, Resource>(upgradeInfo.getValue(), resource));
                    }
                }
            }
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException(resourceDirectory + " does not exist.");
        }
    }

    /**
     * Upgrades the SSG embedded database in a single transaction if necessary. Errors will cause the transaction to roll back.
     * <p/>
     * Determines if upgrade is necessary by querying the db for the current ssg version and comparing it to the current software version.
     * <p/>
     * Does not use the same db version checkers as the mysql DatabaseUpgrader because embedded derby db was only added in Fangtooth (7.0.0)
     * after the addition of the 'ssg_version' table. Therefore the db version can be reliably checked by querying the ssg_version table.
     *
     * @throws SchemaException if the upgrade could not be completed.
     */
    @Override
    public void ensureCurrentSchema() throws SchemaException {
        final JdbcTemplate jdbcTemplate = getJdbcTemplate();
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                final String newVersion = getProductVersion();
                if (StringUtils.isBlank(newVersion)) {
                    throw new SchemaException("Error reading current build version");
                }
                final Connection connection = getConnection();
                String dbVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(connection);
                if (StringUtils.isBlank(dbVersion)) {
                    throw new SchemaException("Error reading current version from database");
                }
                while (!dbVersion.equals(newVersion)) {
                    final Pair<String, Resource> upgradeInfo = upgradeMap.get(dbVersion);
                    if (upgradeInfo == null) {
                        final String msg = "No upgrade path from \"" + dbVersion + "\" to \"" + newVersion + "\"";
                        logger.warning(msg);
                        throw new SchemaException(msg);
                    } else {
                        upgradeSingleVersion(dbVersion, upgradeInfo, jdbcTemplate);
                        dbVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(connection);
                    }
                }
                releaseConnection(connection);
            }
        });
    }

    /**
     * Performs one version upgrade.
     */
    private void upgradeSingleVersion(@NotNull final String dbVersion, @NotNull final Pair<String, Resource> upgradeInfo, @NotNull final JdbcTemplate jdbcTemplate) {
        final String nextVersion = upgradeInfo.getKey();
        final Resource upgradeResource = upgradeInfo.getValue();
        if (StringUtils.isBlank(nextVersion) || upgradeResource == null) {
            throw new SchemaException("Unknown next version or upgrade resource");
        }
        logger.info("Upgrading db from " + dbVersion + "->" + nextVersion);
        try {
            DerbyDbHelper.runScripts(getConnection(), new Resource[]{upgradeResource}, false);
        } catch (final SQLException e) {
            throw new SchemaException("Error executing upgrade", e);
        }
        logger.info("Completed upgrade from " + dbVersion + "->" + nextVersion);
    }

    /**
     * Overridden in unit tests.
     */
    String getProductVersion() {
        return BuildInfo.getFormalProductVersion();
    }

    private final PlatformTransactionManager transactionManager;
    private final Map<String, Pair<String, Resource>> upgradeMap = new HashMap<String, Pair<String, Resource>>();
}
