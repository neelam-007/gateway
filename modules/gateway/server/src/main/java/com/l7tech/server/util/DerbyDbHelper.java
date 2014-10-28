package com.l7tech.server.util;

import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.server.management.db.LiquibaseDBManager;
import liquibase.exception.LiquibaseException;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DerbyDbHelper {

    //- PUBLIC

    public static void runScripts( final Connection connection,
                                   final Resource[] scripts,
                                   final boolean deleteOnComplete ) throws SQLException {
        for ( Resource scriptResource : scripts ) {
            if ( !scriptResource.exists() ) continue;

            logger.config("Running DB script '"+scriptResource.getDescription()+"'.");
            try{
                String[] sqlStatements = getSqlStatements(scriptResource);
                for(String sql: sqlStatements ){
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        if ( logger.isLoggable( Level.FINE ) ) {
                            logger.log( Level.FINE, "Running statement: " + sql );
                        }
                        statement.executeUpdate( sql );

                        SQLWarning warning = statement.getWarnings();
                        while ( warning != null ) {
                            logger.warning( "SQL Warning "+warning.getSQLState()+" ("+warning.getErrorCode()+"): " + warning.getMessage() );
                            warning = warning.getNextWarning();
                        }
                    } catch (SQLException e) {
                        logger.log( Level.INFO, "Last SQL statement attempted: " + sql, ExceptionUtils.getDebugException(e) );
                        throw e;
                    } finally {
                        ResourceUtils.closeQuietly( statement );
                    }
                }

                if ( deleteOnComplete && scriptResource.getFile()!=null ) {
                        if ( scriptResource.getFile().delete() ) {
                            logger.info( "Deleted DB script '"+scriptResource.getDescription()+"'." );
                        } else {
                            logger.warning( "Deletion failed for DB script '"+scriptResource.getDescription()+"'." );
                        }
                }
            } catch (IOException ioe) {
                logger.log( Level.WARNING, "Error processing DB script.", ioe );
            }
        }
    }

    /**
     * Extract sql statements and put them into a string array
     * @param scriptResource Resource to read from
     * @return Array of string sql statements
     * @throws IOException
     */
    public static String[] getSqlStatements(Resource scriptResource) throws IOException {
        final Collection<String> statements = new ArrayList<String>();
        String sql = null;
        StreamTokenizer tokenizer;
        tokenizer = new StreamTokenizer( new InputStreamReader(scriptResource.getInputStream(), Charsets.UTF8) );
        tokenizer.eolIsSignificant(true);
        for ( int i='0'; i<='9'; i++) tokenizer.ordinaryChar( i );
        tokenizer.quoteChar('\'');
        tokenizer.ordinaryChar( '.' );
        tokenizer.ordinaryChar( '-' );
        tokenizer.ordinaryChar( ' ' );
        tokenizer.wordChars(16, 31);
        tokenizer.wordChars(33, 38);
        tokenizer.wordChars(40, 45); // was 44 to exclude -
        tokenizer.wordChars(46,126);
        tokenizer.ordinaryChar( ';' );

        int token;
        StringBuilder builder = new StringBuilder();
        while( (token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF ) {
            if ( token == StreamTokenizer.TT_WORD ) {
                builder.append( tokenizer.sval );
            } else if ( token == ';' ) {
                sql = builder.toString().trim();
                final int lastNewLine = builder.lastIndexOf( "\n" );
                final int lastComment = builder.indexOf( "--", lastNewLine );
                if ( lastComment >= 0 ) {
                    builder.append( ";" );
                    continue;
                }
                builder.setLength( 0 );

                statements.add(sql);
            } else if ( token == StreamTokenizer.TT_EOL && builder.length() > 0 ) {
                //builder.replaceAll( "(?m)^[ \t]*--.*$", "" )
                final int lastNewLine = builder.lastIndexOf( "\n" );
                final int lastComment = builder.indexOf( "--", lastNewLine );
                if ( lastComment >= 0 ) {
                    builder.setLength( lastComment );
                }
                builder.append( "\n" );
            } else if ( token == ' ' && builder.length() > 0 ) {
                builder.append( " " );
            } else if ( token == '\'' ) {
                builder.append( "'" );
                builder.append( tokenizer.sval );
                builder.append( "'" );
            }
        }
        return statements.toArray( new String[statements.size()] );
    }


    /**
     * Ensures the given datasource exists, if it does not it is created (with the current schema).
     *
     * <p>This will cause failure of the server if the database cannot be accessed.</p>
     *
     * <p>This test avoids an issue with Derby issuing SQL warnings when using the
     * createdb connection option and the database already exists.</p>
     *
     * @param dataSource The datasource to test
     */
    @SuppressWarnings("UnusedDeclaration") //This is referenced in embeddedDBContext.xml
    public static void ensureDataSource(final DataSource dataSource,
                                        final LiquibaseDBManager dbManager,
                                        final Resource[] createScripts) {
        Connection connection = null;

        boolean created = true;
        try {
            connection = dataSource.getConnection();
            SQLWarning warning = connection.getWarnings();
            while ( warning != null ) {
                if ( "01J01".equals(warning.getSQLState()) ) {
                    created = false;
                } else {
                    logger.log( Level.WARNING, "SQL Warning: " + warning.getErrorCode() + ", SQLState: " + warning.getSQLState() + ", Message: " + warning.getMessage());
                }

                warning = warning.getNextWarning();
            }

            if ( created ) {
                //create the new schema
                dbManager.ensureSchema(connection);
                //run any additional scripts needed when creating the derby database.
                //This will be the script to change the policy manager admin username/password.
                //SSG-9502
                runScripts( connection, createScripts, false );
                connection.commit();
            }
        } catch ( SQLException se ) {
            throw new RuntimeException( "Could not connect to database.", se );
        } catch (LiquibaseException e) {
            throw new RuntimeException( "Could create database.", e );
        } finally {
            ResourceUtils.closeQuietly(connection);
        }

        if ( created ) {
            logger.config( "Created new database." );
        } else {
            logger.config( "Using existing database." );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(DerbyDbHelper.class.getName());
}
