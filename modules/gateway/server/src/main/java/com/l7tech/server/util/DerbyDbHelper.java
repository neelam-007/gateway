package com.l7tech.server.util;

import com.l7tech.util.*;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
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
        String sql = null;
        for ( Resource scriptResource : scripts ) {
            if ( !scriptResource.exists() ) continue;

            StreamTokenizer tokenizer;
            try {
                logger.config("Running DB script '"+scriptResource.getDescription()+"'.");
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
                        } finally {
                            ResourceUtils.closeQuietly( statement );
                        }
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

                if ( deleteOnComplete && scriptResource.getFile()!=null ) {
                    if ( scriptResource.getFile().delete() ) {
                        logger.info( "Deleted DB script '"+scriptResource.getDescription()+"'." );
                    } else {
                        logger.warning( "Deletion failed for DB script '"+scriptResource.getDescription()+"'." );
                    }
                }
            } catch (IOException ioe) {
                logger.log( Level.WARNING, "Error processing DB script.", ioe );
            } catch (SQLException e) {
                logger.log( Level.INFO, "Last SQL statement attempted: " + sql, ExceptionUtils.getDebugException(e) );
                throw e;
            }
        }
    }

    /**
     * Test the given datasource.
     *
     * <p>This will cause failure of the server if the database cannot be accessed.</p>
     *
     * <p>This test avoids an issue with Derby issuing SQL warnings when using the
     * createdb connection option and the database already exists.</p>
     *
     * @param dataSource The datasource to test
     */
    public static void testDataSource( final DataSource dataSource,
                                       final Config config,
                                       final Resource[] createScripts,
                                       final Resource[] updateScripts ) {
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
                runScripts( connection, createScripts, false );
            } else if ( config.getBooleanProperty("em.server.db.updates", false) || SyspropUtil.getBoolean("com.l7tech.server.db.updates", false) ) {
                runScripts( connection, updateScripts, config.getBooleanProperty( "em.server.db.updatesDeleted", true ) );
            }
        } catch ( SQLException se ) {
            throw new RuntimeException( "Could not connect to database.", se );
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
