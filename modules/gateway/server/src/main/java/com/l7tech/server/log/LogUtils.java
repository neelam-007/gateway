package com.l7tech.server.log;

import com.l7tech.server.ServerConfig;
import com.l7tech.util.CausedIOException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Package private constants for logging.
 */
class LogUtils {

    static final String DEFAULT_FILE_PATTERN_TEMPLATE = "{1}_%g_%u.log";
    static final String DEFAULT_LOG_FORMAT_STANDARD = "%1$tb %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %5$d %3$s%n%2$s: %4$s%n";
    static final String DEFAULT_LOG_FORMAT_VERBOSE = "%1$tb %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %5$d %3$s %6$s%n%2$s: %4$s%n";
    static final String DEFAULT_LOG_FORMAT_RAW = "%4$s%n";
    static final String LOG_SER_FILE = "logs_local.dat";

    /**
     * Construct a log file path pattern for JUL file handler
     */
    static String getLogFilePattern( final ServerConfig serverConfig,
                                     final String filenamepart,
                                     final String filepath ) throws IOException {
        // get log directory, build from home if necessary
        String ssgLogs = filepath != null ?
                filepath :
                serverConfig.getPropertyCached( ServerConfig.PARAM_SSG_LOG_DIRECTORY );
        if ( ssgLogs == null ) {
            try {
                File ssgHome = serverConfig.getLocalDirectoryProperty( ServerConfig.PARAM_SSG_HOME_DIRECTORY, false );
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

        String filePatternTemplate = serverConfig.getProperty( ServerConfig.PARAM_SSG_LOG_FILE_PATTERN_TEMPLATE );
        if ( filePatternTemplate == null ) {
            filePatternTemplate = DEFAULT_FILE_PATTERN_TEMPLATE;
        }

        try {
            return ssgLogs + MessageFormat.format( filePatternTemplate , "default_", filenamepart, "%g", "%u" );
        } catch (IllegalArgumentException iae) {
            throw new CausedIOException("Invalid log file pattern '" + filePatternTemplate + "'.");
        }
    }
}
