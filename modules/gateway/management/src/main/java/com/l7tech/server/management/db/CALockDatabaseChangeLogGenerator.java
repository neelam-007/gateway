package com.l7tech.server.management.db;

import com.l7tech.util.ConfigFactory;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.util.NetUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;

/**
 * This is used to replace the liquibase LockDatabaseChangeLogGenerator. It crashes when the gateway hostname is not
 * properly configured on the network.
 * See SSG-9563 and SSG-9564
 */
@SuppressWarnings("UnusedDeclaration")
public class CALockDatabaseChangeLogGenerator extends AbstractSqlGenerator<LockDatabaseChangeLogStatement> {

    @Override
    public ValidationErrors validate(LockDatabaseChangeLogStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        return new ValidationErrors();
    }

    protected static final String hostname;
    protected static final String hostaddress;

    static {
        try {
            hostname = getHostname();
            hostaddress = NetUtil.getLocalHostAddress();
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    private static String getHostname() {
        String hostname = ConfigFactory.getProperty("hostname", null);

        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
            } catch (UnknownHostException e) {
                try {
                    hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
                } catch (UnknownHostException e1) {
                    hostname = "UnknownGateway";
                }
            }
        }

        return hostname;
    }

    @Override
    public Sql[] generateSql(LockDatabaseChangeLogStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String liquibaseSchema = database.getLiquibaseSchemaName();
        String liquibaseCatalog = database.getLiquibaseCatalogName();


        UpdateStatement updateStatement = new UpdateStatement(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogLockTableName());
        updateStatement.addNewColumnValue("LOCKED", true);
        updateStatement.addNewColumnValue("LOCKGRANTED", new Timestamp(new java.util.Date().getTime()));
        updateStatement.addNewColumnValue("LOCKEDBY", hostname + " (" + hostaddress + ")");
        updateStatement.setWhereClause(database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "ID") + " = 1 AND " + database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "LOCKED") + " = " + DataTypeFactory.getInstance().fromDescription("boolean", database).objectToSql(false, database));

        return SqlGeneratorFactory.getInstance().generateSql(updateStatement, database);

    }
}