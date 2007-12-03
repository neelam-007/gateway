package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.FileHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.File;
import java.text.MessageFormat;

import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.common.util.ConfigurableLogFormatter;
import com.l7tech.server.ServerConfig;

/**
 * MessageSink that writes to a log file.
 *
 * @author Steve Jones
 */
class FileMessageSink extends MessageSinkSupport {

    //- PUBLIC

    public void close() throws IOException {
        handler.setErrorManager( new ErrorUnManager() );
        handler.close();
    }

    //- PACKAGE

    FileMessageSink( final ServerConfig serverConfig,
                     final SinkConfiguration configuration ) throws ConfigurationException {
        super( configuration );
        this.handler = buildHandler( serverConfig, configuration );
    }

    void processMessage( final MessageCategory category, final LogRecord record ) {
        handler.publish( record );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FileMessageSink.class.getName());

    private static final String DEFAULT_PARTITION_NAME = "default_";
    private static final String DEFAULT_FILE_PATTERN_TEMPLATE = "{0}-{1}_%g_%u.log";

    private final FileHandler handler;

    /**
     * Construct a file handler for the given config
     */
    private FileHandler buildHandler( final ServerConfig serverConfig,
                                      final SinkConfiguration configuration ) throws ConfigurationException {
        FileHandler fileHandler;

        String name = configuration.getName();
        String filelim = configuration.getProperty( SinkConfiguration.PROP_FILE_MAX_SIZE );
        String filenum = configuration.getProperty( SinkConfiguration.PROP_FILE_LOG_COUNT );

        try {
            String filepat = getLogFilePattern( serverConfig, name );
            int limit = parseIntWithDefault( "log file limit for " + name, filelim, 1024 * 1024 );
            int count = parseIntWithDefault( "log file count for " + name, filenum, 1 );
            boolean append = true;

            // TODO [steve] get log format from SinkConfiguration
            fileHandler = new FileHandler( filepat, limit, count, append );
            fileHandler.setFormatter(new ConfigurableLogFormatter("%1$tb %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %5$d %3$s%n%2$s: %4$s%n"));
            fileHandler.setLevel(Level.ALL); // since we check the level before passing on
        } catch (IOException ioe) {
            throw new ConfigurationException( "Error creating log file handler", ioe );
        }

        return fileHandler;
    }

    /**
     * Parse an int value with the given default
     */
    private int parseIntWithDefault( final String description,
                                     final String textValue,
                                     final int defaultValue ) {
        int value = defaultValue;

        try {
            if ( textValue != null ) {
                value = Integer.parseInt( textValue );
            }        
        } catch ( NumberFormatException nfe ) {
            logger.log(Level.WARNING,
                "Configuration error in " + description + " '"+textValue+"'. Using default value '"+defaultValue+"'.");    
        }

        return value;
    }

    /**
     * Construct a log file path pattern for JUL file handler
     */
    private String getLogFilePattern( final ServerConfig serverConfig,
                                      final String filenamepart ) throws ConfigurationException {
        // get log directory, build from home if necessary
        String ssgLogs = serverConfig.getPropertyCached( ServerConfig.PARAM_SSG_LOG_DIRECTORY );
        if ( ssgLogs == null ) {
            try {
                File ssgHome = serverConfig.getLocalDirectoryProperty( ServerConfig.PARAM_SSG_HOME_DIRECTORY, "/ssg", false );
                ssgLogs = new File(ssgHome, "logs").getAbsolutePath();
            } catch (RuntimeException re) {
                throw new ConfigurationException("Error with home directory: " + re.getMessage());
            }
        } else {
            if ( !new File(ssgLogs).exists() ) {
                throw new ConfigurationException("Log directory does not exist '" + ssgLogs + "'.");                
            }
        }

        if ( !ssgLogs.endsWith("/") ) {
            ssgLogs += "/";
        }

        String partitionName = serverConfig.getProperty( ServerConfig.PARAM_PARTITION_NAME );
        if ( partitionName == null ) {
            partitionName = DEFAULT_PARTITION_NAME;
        }

        String filePatternTemplate = serverConfig.getProperty( ServerConfig.PARAM_SSG_LOG_FILE_PATTERN_TEMPLATE );
        if ( filePatternTemplate == null ) {
            filePatternTemplate = DEFAULT_FILE_PATTERN_TEMPLATE;
        }

        try {
            return ssgLogs + MessageFormat.format( filePatternTemplate , partitionName, filenamepart, "%g", "%u" );
        } catch (IllegalArgumentException iae) {
            throw new ConfigurationException("Invalid log file pattern '" + filePatternTemplate + "'.");
        }
    }

    /**
     * Error manager that ignores all errors
     */
    private static final class ErrorUnManager extends ErrorManager {
        @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
        @Override
        public void error( String msg, Exception ex, int code ) {
        }
    }
}
