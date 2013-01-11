package com.l7tech.server.ems.setup;

import com.l7tech.server.util.DerbyDbHelper;
import com.l7tech.server.util.SchemaUpdater;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Upgrades the ESM database schema.
 */
public class SchemaUpdaterImpl extends JdbcDaoSupport implements SchemaUpdater {

    //- PUBLIC

    public SchemaUpdaterImpl( final PlatformTransactionManager transactionManager,
                              final Map<Integer,Resource> scriptsBySchemaVersion ) {
        this.transactionManager = transactionManager;
        this.scriptsBySchemaVersion = scriptsBySchemaVersion;
    }

    @Override
    public void ensureCurrentSchema() {
        new TransactionTemplate(transactionManager).execute( new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                final int version = getSchemaVersion();
                if ( version < SCHEMA_VERSION ) {
                    upgradeDatabaseSchema( scriptsBySchemaVersion, version, SCHEMA_VERSION );

                    final int upgradedVersion = getSchemaVersion();
                    if ( SCHEMA_VERSION != upgradedVersion ) {
                        throw new SchemaVersionException( SCHEMA_VERSION, upgradedVersion );
                    }
                } else if ( version > SCHEMA_VERSION ) {
                    throw new SchemaVersionException( SCHEMA_VERSION, version );
                }
            }
        } );
    }

    //- PRIVATE

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger( SchemaUpdaterImpl.class.getName() );

    /**
     * Schema version is incremented whenever the version changes, not for every release.
     *
     * Schema versions to first used ESM version:
     * -  0 : 1.0
     * -  1 : 1.5
     * -  2 : 1.6
     * -  3 : 1.8 (Fangtooth)
     * -  4 : (Goatfish)
     * The version should match the value in "version.sql"
     */
    private static final int SCHEMA_VERSION = 4;

    private final PlatformTransactionManager transactionManager;
    private final Map<Integer,Resource> scriptsBySchemaVersion;

    private int getSchemaVersion() {
        int version = 0;

        try {
            version = getJdbcTemplate().queryForInt( "select current_version from schema_version" );
        } catch ( BadSqlGrammarException e) {
            // This is expected since there is no schema_version table in the version zero schema
            logger.log( Level.FINE, "Error querying for schema version '"+ ExceptionUtils.getMessage( e )+"'", ExceptionUtils.getDebugException( e ) );
        } catch ( EmptyResultDataAccessException e ) {
            throw new SchemaUpgradeException("Error updating database schema, version not found", e);
        } catch ( IncorrectResultSizeDataAccessException e ) {
            throw new SchemaUpgradeException("Error updating database schema, multiple versions found", e);
        }

        return version;
    }

    private void upgradeDatabaseSchema( final Map<Integer,Resource> scriptsBySchemaVersion,
                                        final int fromVersion,
                                        final int toVersion ) {
        Connection connection = null;
        try {
            connection = getConnection();
            DerbyDbHelper.runScripts( connection, resources( scriptsBySchemaVersion, fromVersion, toVersion ), false );
        } catch ( SQLException e ) {
            throw new SchemaUpgradeException("Error updating database schema '"+ ExceptionUtils.getMessage(e) +"'", e);
        } finally {
            releaseConnection( connection );
        }
    }

    private Resource[] resources( final Map<Integer,Resource> scriptsBySchemaVersion,
                                  final int fromVersion,
                                  final int toVersion ) {
        final List<Resource> resources = Functions.reduce(
                scriptsBySchemaVersion.entrySet(),
                new ArrayList<Resource>(),
                new Functions.Binary<List<Resource>,List<Resource>,Map.Entry<Integer,Resource>>(){
            @Override
            public List<Resource> call( final List<Resource> resources,
                                        final Map.Entry<Integer, Resource> resourceEntry ) {
                if ( resourceEntry.getKey() > fromVersion && resourceEntry.getKey() <= toVersion ) {
                    resources.add( resourceEntry.getValue() );
                }
                return resources;
            }
        } );

        if ( resources.isEmpty() ) {
            throw new SchemaVersionException( fromVersion, toVersion );
        }

        return resources.toArray( new Resource[resources.size()] );
    }
}
