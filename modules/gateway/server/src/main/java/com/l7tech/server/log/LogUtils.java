package com.l7tech.server.log;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.CausedIOException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Package private constants for logging.
 */
class LogUtils {

    private static final Logger logger = Logger.getLogger( LogUtils.class.getName() );

    static final String DEFAULT_FILE_PATTERN_TEMPLATE = "{1}_%g_%u.log";
    static final String DEFAULT_LOG_FORMAT_STANDARD = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %2$-7s %5$d %3$s: %4$s%n";
    static final String DEFAULT_LOG_FORMAT_VERBOSE = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL%1$tz %2$-7s %5$d %3$s %6$s: %4$s%n";
    static final String DEFAULT_LOG_FORMAT_RAW = "%4$s%n";
    static final String LOG_SER_FILE = "logs_local.dat";

    /**
     * Construct a log file path pattern for JUL file handler
     */
    static String getLogFilePattern( final ServerConfig serverConfig,
                                     final String filenamepart,
                                     final String filepath,
                                     final boolean useDefaultTemplate ) throws IOException {
        // get log directory, build from home if necessary
        String ssgLogs = filepath != null ?
                filepath :
                serverConfig.getProperty( ServerConfigParams.PARAM_SSG_LOG_DIRECTORY );
        if ( ssgLogs == null ) {
            try {
                File ssgHome = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_SSG_HOME_DIRECTORY, false );
                ssgLogs = new File(ssgHome, "var/logs").getAbsolutePath();
            } catch (RuntimeException re) {
                throw new CausedIOException("Error with home directory: " + re.getMessage());
            }
        } else {
            if ( !new File(ssgLogs).exists() ) {
                throw new CausedIOException("Log directory does not exist '" + ssgLogs + "'.");
            }
        }

        if ( !ssgLogs.endsWith("/") ) {
            ssgLogs += "/";
        }

        String filePatternTemplate = useDefaultTemplate ? null : serverConfig.getProperty( ServerConfigParams.PARAM_SSG_LOG_FILE_PATTERN_TEMPLATE );
        if ( filePatternTemplate == null ) {
            filePatternTemplate = DEFAULT_FILE_PATTERN_TEMPLATE;
        }

        try {
            return ssgLogs + MessageFormat.format( filePatternTemplate , "default_", filenamepart, "%g", "%u" );
        } catch (IllegalArgumentException iae) {
            throw new CausedIOException("Invalid log file pattern '" + filePatternTemplate + "'.");
        }
    }

    static int readLoggingThreshold( final String propertyName ) {
        int level = 0;

        String levelStr = LogManager.getLogManager().getProperty( propertyName );
        if ( levelStr != null ) {
            try {
                level = Level.parse( levelStr ).intValue();
            } catch ( IllegalArgumentException iae ) {
                logger.warning( "Ignoring invalid logging level value '"+levelStr+"' for '"+propertyName+"'." );
            }
        }

        return level;
    }
}
