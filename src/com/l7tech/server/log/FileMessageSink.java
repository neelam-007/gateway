package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.FileHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.io.IOException;

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

    FileMessageSink( final ServerConfig serverConfig, final SinkConfiguration configuration ) {
        super( configuration );
        this.handler = buildHandler( serverConfig, configuration );
    }

    void processMessage( final MessageCategory category, final LogRecord record ) {
        handler.publish( record );
    }

    //- PRIVATE

    private final FileHandler handler;

    /**
     * Construct a file handler for the given config
     */
    private FileHandler buildHandler( final ServerConfig serverConfig, final SinkConfiguration configuration ) {
        FileHandler fileHandler = null;

        //TODO [steve] cleanup, exceptions, etc
        String filenamepart = configuration.getName();
        String filelim = configuration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE);
        String filenum = configuration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT);

        String ssgHome = serverConfig.getPropertyCached(ServerConfig.PARAM_SSG_HOME_DIRECTORY);
        String filePattern = ssgHome + "/logs/" + filenamepart + "_%g_%u.log";
        int limit = Integer.parseInt(filelim);
        int count = Integer.parseInt(filenum);
        boolean append = true;

        try {
            fileHandler = new FileHandler( filePattern, limit, count, append );
            fileHandler.setFormatter(new ConfigurableLogFormatter("%1$tb %1$te, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %5$d %3$s%n%2$s: %4$s%n"));
            fileHandler.setLevel(Level.ALL); // since we check the level before passing on
        } catch (IOException ioe) {
        }

        return fileHandler;
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
