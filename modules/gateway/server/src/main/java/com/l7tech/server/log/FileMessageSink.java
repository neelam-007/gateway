package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.FileHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectStreamException;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.ExceptionUtils;

/**
 * MessageSink that writes to a log file.
 *
 * @author Steve Jones
 */
class FileMessageSink extends MessageSinkSupport implements Serializable {

    //- PUBLIC

    @Override
    public void close() throws IOException {
        handler.setErrorManager( new ErrorUnManager() );
        handler.close();
    }

    //- PACKAGE

    FileMessageSink( final ServerConfig serverConfig,
                     final SinkConfiguration configuration ) throws ConfigurationException {
        super( configuration );
        this.logFileConfiguration = buildLogFileConfiguration( serverConfig, configuration );
        this.handler = buildHandler( logFileConfiguration );
    }

    @Override
    void processMessage( final MessageCategory category, final LogRecord record ) {
        handler.publish( record );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FileMessageSink.class.getName());

    private static final String PROP_FILE_LOG_PATH = "file.logPath";

    private final LogFileConfiguration logFileConfiguration;
    private final FileHandler handler;

    /**
     * Construct a file handler for the given config
     */
    private FileHandler buildHandler( final LogFileConfiguration logFileConfiguration ) throws ConfigurationException {

        FileHandler fileHandler;
        try {
            fileHandler = logFileConfiguration.buildFileHandler();
        } catch (IOException ioe) {
            throw new ConfigurationException( "Error creating log file handler", ioe );
        }

        return fileHandler;
    }

    private LogFileConfiguration buildLogFileConfiguration( final ServerConfig serverConfig,
                                                            final SinkConfiguration configuration ) throws ConfigurationException {
        String name = configuration.getName();
        String filelim = configuration.getProperty( SinkConfiguration.PROP_FILE_MAX_SIZE );
        String filenum = configuration.getProperty( SinkConfiguration.PROP_FILE_LOG_COUNT );
        String filepath = configuration.getProperty( PROP_FILE_LOG_PATH );
        String format = configuration.getProperty( SinkConfiguration.PROP_FILE_FORMAT );

        try {
            String filepat = LogUtils.getLogFilePattern( serverConfig, name, filepath, false );
            int limit = parseIntWithDefault( "log file limit for " + name, filelim, 1024 ) * 1024;
            int count = parseIntWithDefault( "log file count for " + name, filenum, 2 );
            boolean append = true;
            String formatPattern = getFormatPattern(format);
            int level = getThreshold();

            return new LogFileConfiguration( filepat, limit, count, append, level, formatPattern );
        } catch ( IOException ioe ) {
            throw new ConfigurationException( ExceptionUtils.getMessage(ioe), ioe );
        }
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
     * Get the format pattern for the given name.
     */
    private String getFormatPattern( final String name ) {
        String formatPattern;

        // see if customized in logging properties
        formatPattern = LogManager.getLogManager().getProperty("sink.format." + name);

        // use default if not set
        if ( formatPattern == null ) {
            if ( SinkConfiguration.FILE_FORMAT_RAW.equals(name) ) {
                formatPattern = LogUtils.DEFAULT_LOG_FORMAT_RAW;
            } else if ( SinkConfiguration.FILE_FORMAT_VERBOSE.equals(name) ) {
                formatPattern = LogUtils.DEFAULT_LOG_FORMAT_VERBOSE;
            } else {
                formatPattern = LogUtils.DEFAULT_LOG_FORMAT_STANDARD;
            }
        }

        return formatPattern;
    }

    private Object writeReplace() throws ObjectStreamException {
        return logFileConfiguration;
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
