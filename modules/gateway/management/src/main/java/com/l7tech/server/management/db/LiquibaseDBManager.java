package com.l7tech.server.management.db;

import com.l7tech.util.ExceptionUtils;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.servicelocator.ServiceLocator;
import liquibase.snapshot.SnapshotGeneratorFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is used to maintain the ssg database. Creating a new one or upgrading an existing one.
 * There are is also a method for adding liquibase to a preexisting 8.2 database.
 */
public class LiquibaseDBManager {

    /**
     * This is the version a pre liquibase database needs to be at in order to have the liquibase schema applied to it.
     */
    public static final String JAVELIN_PRE_DB_VERSION = "8.3.pre";

    //This is the liquibase script to create a new database,
    private static final String ssgXML = "ssg.xml";
    //This is the liquibase script to apply to an 8.3.pre database (one in icefish) to add the liquibase tables.
    private static final String ssg82XML = "ssg-8.2.00.xml";

    // The folder containing the liquibase database files
    private final File liquibaseSchemaFolder;

    /**
     * Creates a new liquibase database manager.
     *
     * @param liquibaseSchemaFolderPath The path to the folder containing the liquibase database files.
     */
    public LiquibaseDBManager(@NotNull final String liquibaseSchemaFolderPath) {
        this.liquibaseSchemaFolder = new File(liquibaseSchemaFolderPath);
        //validate the path
        if (!liquibaseSchemaFolder.exists() || !liquibaseSchemaFolder.isDirectory()) {
            throw new RuntimeException("Could not find liquibase folder: " + liquibaseSchemaFolder);
        }

        //Add this package so that out custom logger gets used by liquibase.
        ServiceLocator.getInstance().addPackageToScan("com.l7tech.server.management.db");
    }

    /**
     * This will apply the full liquibase changeset to the given database. If the database is new (empty) a full new
     * schema will be created. If the database is an existing database then the missing changeset will be applied to
     * it.
     * DO NOT RUN THIS AGAINST A NON LIQUIBASE DB!
     * If the ssg database has not yet been liquified then run updatePreliquibaseDB() on it.
     *
     * @param connection The database connection to apply the liquibase schema to.
     * @throws LiquibaseException This is thrown if there was an error connecting tot he database or if there was an
     *                            error executing a changeset.
     */
    public void ensureSchema(@NotNull final Connection connection) throws LiquibaseException {
        //run the changeset on the database.
        final Liquibase liquibase = new Liquibase(ssgXML, new FileSystemResourceAccessor(liquibaseSchemaFolder.getPath()), new NonCommittingConnection(connection));
        liquibase.update("");
    }

    /**
     * This will apply the liquibase changelog to an existing database. The database needs to be updated to the 8.2.pre
     * version before this is called.
     *
     * @param connection The database connection to apply the liquibase schema to.
     * @throws LiquibaseException
     */
    public void updatePreliquibaseDB(@NotNull final Connection connection) throws LiquibaseException {
        //validate that the database is at version 8.3.pre
        try {
            final ResultSet result = connection.prepareStatement("SELECT current_version FROM ssg_version").executeQuery();
            if (result.next()) {
                final String currentVersion = result.getString("current_version");
                if (!JAVELIN_PRE_DB_VERSION.equals(currentVersion)) {
                    throw new RuntimeException("Database is not at correct version in order to apply liquibase. The database version needed is 8.3.pre, but the current version is: " + currentVersion);
                }
            } else {
                throw new RuntimeException("Error finding current database version. ssg_version table is empty.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding current database version. Message: " + ExceptionUtils.getMessage(e), e);
        }
        final Liquibase liquibase = new Liquibase(ssg82XML, new FileSystemResourceAccessor(liquibaseSchemaFolder.getPath()), new NonCommittingConnection(connection));
        //perform liquibase upgrade
        liquibase.changeLogSync("");
        ensureSchema(connection);
    }

    /**
     * Checks if the database has been liquified. Has it had liquibase run on it before. This will specifically check
     * for the existence of the liquibase changelog table.
     *
     * @param connection The database connection to check for liquification
     * @return True if the database has been liquified, false otherwise.
     * @throws LiquibaseException This is thrown if there was an error connecting to the database.
     */
    public boolean isLiquibaseDB(@NotNull final Connection connection) throws LiquibaseException {
        final Liquibase liquibase = new Liquibase(null, null, new JdbcConnection(connection));
        //test is the database has the liquibase change log table. We can assume the if it does not have the table then it has not yet been liquified!
        return SnapshotGeneratorFactory.getInstance().hasDatabaseChangeLogTable(liquibase.getDatabase());
    }

    /**
     * This is a connection that does nothing on commit and rollback. The reason this is used is two fold.
     *
     * 1) Liquibase will commit after every changeset executed. This is not idea because if changeset 5 false
     * changesets
     * 1-4 will already have been committed and could not be rolled back. Making commit do nothing here allows us to
     * commit an entire changelog at a time and roll back completely if there is an error at any point in the process.
     *
     * 2)In between operations (changeLogSync, update) liquibase performs a rollback. This will not work if we want to
     * run liquibase within our own transaction. For this reason rollback is ignored.
     *
     * Note that when this connection is used you must manually commit changes after executing.
     */
    private class NonCommittingConnection extends JdbcConnection {

        /**
         * Creates a new non committing connection
         *
         * @param connection The connection to wrap
         */
        public NonCommittingConnection(Connection connection) {
            super(connection);
        }

        public void commit() throws DatabaseException {
            //does nothing. See above for more info
        }

        public void rollback() throws DatabaseException {
            //does nothing. See above for more info
        }
    }
}
