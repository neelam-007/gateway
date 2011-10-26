package com.l7tech.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.log.LoggingPrintStream;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.some;
import com.l7tech.util.TextUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to put cluster property event handling code for monitoring cluster properties where no other bean exists which
 * can manage the cluster property.
 * The usage is to configure this class to receive events when a cluster property changes and to handle the event. This
 * can involve calling into non server module code to provide runtime configuration.
 * <p/>
 * Configure this bean via the ServerConfig in the application context with the properties to receive events for.
 * If this class gets many usages beyond it's original use case it can be made into more of a generic bean where the
 * event processing code could be specified via spring configuration as a map of cluster properties to the class to process the
 * events.
 *
 * @author darmstrong
 */
public class SimplePropertyChangeHandler implements PropertyChangeListener, InitializingBean {

    //- PUBLIC

    @Override
    public void afterPropertiesSet() throws Exception {
        setConfiguredContentTypes();
        setStdOutLevel(some(Level.INFO));
        setStdErrLevel(some(Level.WARNING));
        setSslDebug(); // Call after streams are configured
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ( ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES.equals(event.getPropertyName())) {
            //Configurable content-types see bug 8884
            setConfiguredContentTypes();
        } else if ( ServerConfigParams.PARAM_DEBUG_SSL.equals(event.getPropertyName()) ||
                    ServerConfigParams.PARAM_DEBUG_SSL_VALUE.equals(event.getPropertyName())) {
            setSslDebug();
        } else if ( ServerConfigParams.PARAM_LOG_STDOUT_LEVEL.equals(event.getPropertyName()) ) {
            setStdOutLevel(Option.<Level>none());
            setSslDebug(); // Reset SSL Debug in case this causes the provider to use the new stream
        } else if ( ServerConfigParams.PARAM_LOG_STDERR_LEVEL.equals(event.getPropertyName()) ) {
            setStdErrLevel(Option.<Level>none());
            setSslDebug(); // Reset SSL Debug in case this causes the provider to use the new stream
        }
    }

    // - PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());

    private static final String DEFAULT_SSL_DEBUG_VALUE = "ssl";

    @Inject
    private Config config;

    private void setConfiguredContentTypes() {
        final ContentTypeHeader[] headers = getConfiguredContentTypes();
        ContentTypeHeader.setConfigurableTextualContentTypes(headers);
    }

    private void setSslDebug() {
        final boolean enableSslDebug = config.getBooleanProperty( ServerConfigParams.PARAM_DEBUG_SSL, false );
        final String debugValue = config.getProperty( ServerConfigParams.PARAM_DEBUG_SSL_VALUE, DEFAULT_SSL_DEBUG_VALUE );
        if ( enableSslDebug ) {
            JceProvider.getInstance().setDebugOptions( Collections.singletonMap("ssl", debugValue) );
        } else {
            JceProvider.getInstance().setDebugOptions( Collections.<String,String>singletonMap("ssl", null) );
        }
    }

    private void setStdOutLevel( final Option<Level> ignoreIfMatches ) {
        configureStream( ignoreIfMatches, ServerConfigParams.PARAM_LOG_STDOUT_LEVEL, "STDOUT", Level.INFO, new UnaryVoid<PrintStream>(){
            @Override
            public void call( final PrintStream printStream ) {
                if ( System.out instanceof LoggingPrintStream )
                    System.setOut( printStream ); // only update if already configured for logging
            }
        } );
    }

    private void setStdErrLevel( final Option<Level> ignoreIfMatches ) {
        configureStream( ignoreIfMatches, ServerConfigParams.PARAM_LOG_STDERR_LEVEL, "STDERR", Level.WARNING, new UnaryVoid<PrintStream>(){
            @Override
            public void call( final PrintStream printStream ) {
                if ( System.err instanceof LoggingPrintStream )
                    System.setErr( printStream ); // only update if already configured for logging
            }
        } );
    }

    private void configureStream( final Option<Level> ignoreIfMatches,
                                  final String propertyName,
                                  final String loggerName,
                                  final Level defaultLoggerLevel,
                                  final UnaryVoid<PrintStream> streamSetter ) {
        final String levelText = config.getProperty( propertyName, defaultLoggerLevel.getName() );
        Level level = defaultLoggerLevel;
        try {
            level = Level.parse( levelText );
        } catch ( IllegalArgumentException e ) {
            logger.warning( "Error parsing level " + levelText + " using default level " + defaultLoggerLevel + " for " + loggerName);
        }

        if ( ignoreIfMatches.isSome() && ignoreIfMatches.some().equals( level ) ) {
            logger.finer( "Not configuring stream " + loggerName + " with default level " + level );
        } else {
            logger.config( "Configuring stream " + loggerName + " with level " + level);
            streamSetter.call( new LoggingPrintStream(Logger.getLogger(loggerName), level) );
        }
    }

    /**
     * Get any content types which have been configured as textual via a cluster property.
     *
     * @return List of ContentTypeHeaders. Never null.
     */
    private ContentTypeHeader[] getConfiguredContentTypes() {
        final String otherTypes = config.getProperty( ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES );

        List<String> types = TextUtils.getTokensFromString(otherTypes, "\n\r\f");
        List<ContentTypeHeader> returnList = new ArrayList<ContentTypeHeader>();
        for (String type : types) {
            try {
                returnList.add(ContentTypeHeader.parseValue(type));
            } catch (IOException e) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Cannot parse content-type value '" + type + "' from cluster property. " +
                            "Reason: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        return returnList.toArray(new ContentTypeHeader[returnList.size()]);
    }
}

