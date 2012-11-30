package com.l7tech.server.util;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.DbUpgradeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SSG schema updater for embedded database.
 */
public class EmbeddedDbSchemaUpdater extends JdbcDaoSupport implements SchemaUpdater {
    private static final Logger logger = Logger.getLogger(EmbeddedDbSchemaUpdater.class.getName());

    /**
     * @param transactionManager     the PlatformTransactionManager.
     * @param upgradeScriptDirectory the directory which contains the derby upgrade scripts.
     * @throws IOException if unable to read the ugprade scripts.
     */
    public EmbeddedDbSchemaUpdater(@NotNull final PlatformTransactionManager transactionManager, @NotNull final Resource upgradeScriptDirectory) throws IOException {
        this.transactionManager = transactionManager;
        Validate.isTrue(upgradeScriptDirectory.exists());
        upgradeMap.putAll(DbUpgradeUtil.buildUpgradeMap(upgradeScriptDirectory.getFile()));
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
                    final String[] upgradeInfo = upgradeMap.get(dbVersion);
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
    private void upgradeSingleVersion(@NotNull final String dbVersion, @NotNull final String[] upgradeInfo, @NotNull final JdbcTemplate jdbcTemplate) {
        final String nextVersion = upgradeInfo[0];
        final String upgradeFilePath = upgradeInfo[1];
        if (StringUtils.isBlank(nextVersion) || StringUtils.isBlank(upgradeFilePath)) {
            throw new SchemaException("Unknown next version or upgrade file path");
        }
        logger.info("Upgrading db from " + dbVersion + "->" + nextVersion);
        try {
            final String[] upgradeStatements = DbUpgradeUtil.getStatementsFromFile(upgradeFilePath);
            for (final String statement : upgradeStatements) {
                jdbcTemplate.execute(statement);
            }
        } catch (final IOException e) {
            throw new SchemaException("Error reading upgrade script", e);
        } catch (final DataAccessException e) {
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
    private final Map<String, String[]> upgradeMap = new HashMap<String, String[]>();
}
