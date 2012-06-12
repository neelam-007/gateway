package com.l7tech.server;

import com.l7tech.util.DateTimeConfigUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.log.LoggingPrintStream;
import com.l7tech.util.*;
import com.l7tech.util.Functions.UnaryVoid;

import static com.l7tech.util.Functions.*;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.splitAndTransform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
        audit = auditFactory.newInstance(this, logger);
        setConfiguredContentTypes();
        setStdOutLevel(some(Level.INFO));
        setStdErrLevel(some(Level.WARNING));
        setSslDebug(); // Call after streams are configured
        setConfiguredDateFormats();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final String propertyName = event.getPropertyName();
        if ( ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES.equals(propertyName)) {
            //Configurable content-types see bug 8884
            setConfiguredContentTypes();
        } else if ( ServerConfigParams.PARAM_DEBUG_SSL.equals(propertyName) ||
                    ServerConfigParams.PARAM_DEBUG_SSL_VALUE.equals(propertyName)) {
            setSslDebug();
        } else if ( ServerConfigParams.PARAM_LOG_STDOUT_LEVEL.equals(propertyName) ) {
            setStdOutLevel(Option.<Level>none());
            setSslDebug(); // Reset SSL Debug in case this causes the provider to use the new stream
        } else if ( ServerConfigParams.PARAM_LOG_STDERR_LEVEL.equals(propertyName) ) {
            setStdErrLevel(Option.<Level>none());
            setSslDebug(); // Reset SSL Debug in case this causes the provider to use the new stream
        } else if (ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS.equals(propertyName)||
                ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS.equals(propertyName)) {
            setConfiguredDateFormats();
        }
    }

    // - PROTECTED

    /**
     * Get any content types which have been configured as textual via a cluster property.
     *
     * @return List of ContentTypeHeaders. Never null.
     */
    @NotNull
    ContentTypeHeader[] getConfiguredContentTypes() {
        final String otherTypes = config.getProperty(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES);
        final List<ContentTypeHeader> contentTypeHeaderList = splitAndTransform(otherTypes, TEXTUAL_CONTENT_TYPES_SPLIT_PATTERN, new Unary<ContentTypeHeader, String>() {
            @Override
            public ContentTypeHeader call(String type) {
                try {
                    return ContentTypeHeader.parseValue(type);
                } catch (IOException e) {
                    audit.logAndAudit(SystemMessages.INVALID_CONTENT_TYPE, new String[]{type, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                }
                return null;
            }
        });

        return contentTypeHeaderList.toArray(new ContentTypeHeader[contentTypeHeaderList.size()]);
    }

    /**
     * Get all configured Date format strings.
     *
     * The list will be comprised of any user defined formats set via the datetime.customFormats cluster property
     * followed by the formats defined in hidden cluster property datetime.autoFormats
     *
     * @return all configured date formats. Never null, may be empty.
     */
    @NotNull
    List<String> getCustomDateFormatsStrings(){
        final String customFormats = config.getProperty(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS);
        final Unary<String, String> simpleDatePredicate = new Unary<String, String>() {
            @Override
            public String call(final String input) {
                return (isValidDateFormat(input, new UnaryVoid<Exception>() {
                    @Override
                    public void call(Exception e) {
                        audit.logAndAudit(SystemMessages.INVALID_CUSTOM_DATE_FORMAT, new String[]{input, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                    }
                }))? input: null;
            }
        };
        return splitAndTransform(customFormats, DATE_FORMATS_SPLIT_PATTERN, simpleDatePredicate);
    }

    private boolean isValidDateFormat(@NotNull String input, @Nullable UnaryVoid<Exception> auditCallback) {
        boolean isValid = false;
        if (!input.isEmpty()) {
            try {
                new SimpleDateFormat(input);
                isValid = true;
            } catch (Exception e) {
                if (auditCallback != null) {
                    auditCallback.call(e);
                }
            }
        }
        return isValid;
    }

    @NotNull
    List<Pair<String, Pattern>> getAutoDateFormatsStrings(){
        final String autoFormatsProp = config.getProperty(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS);
        return grepNotNull(splitAndTransform(autoFormatsProp, AUTO_DATE_FORMATS_SPLIT_PATTERN, new Unary<Pair<String, Pattern>, String>() {
            @Override
            public Pair<String, Pattern> call(String input) {
                final String[] split = AUTO_DATE_FORMATS_PAIR_SPLIT_PATTERN.split(input);
                if (split.length == 2) {
                    // values need to be trimmed after local split
                    final String format = split[0].trim();
                    final boolean isValidFormat = isValidDateFormat(format, new UnaryVoid<Exception>() {
                        @Override
                        public void call(Exception o) {
                            audit.logAndAudit(SystemMessages.INVALID_AUTO_DATE_FORMAT, new String[]{format}, ExceptionUtils.getDebugException(o));
                        }
                    });

                    if (!isValidFormat) {
                        return null;
                    }

                    final Pattern pattern;
                    try {
                        pattern = Pattern.compile(split[1].trim());
                    } catch (Exception e) {
                        audit.logAndAudit(SystemMessages.INVALID_AUTO_DATE_FORMAT, format);
                        return null;
                    }

                    return new Pair<String, Pattern>(format, pattern);
                }
                audit.logAndAudit(SystemMessages.INVALID_AUTO_DATE_FORMAT, input);
                return null;
            }
        }));
    }


    // - PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());

    private static final String DEFAULT_SSL_DEBUG_VALUE = "ssl";

    private final Pattern TEXTUAL_CONTENT_TYPES_SPLIT_PATTERN = Pattern.compile("\\n+|\\r+|\\f+");
    private final Pattern DATE_FORMATS_SPLIT_PATTERN = Pattern.compile(";");
    private final Pattern AUTO_DATE_FORMATS_SPLIT_PATTERN = Pattern.compile("\\$");
    private final Pattern AUTO_DATE_FORMATS_PAIR_SPLIT_PATTERN = Pattern.compile("\\^");

    private Audit audit;

    @Inject
    private Config config;

    @Inject
    private AuditFactory auditFactory;

    @Inject
    private DateTimeConfigUtils dateParser;

    private void setConfiguredContentTypes() {
        final ContentTypeHeader[] headers = getConfiguredContentTypes();
        ContentTypeHeader.setConfigurableTextualContentTypes(headers);
    }

    private void setConfiguredDateFormats() {
        final List<String> formats = getCustomDateFormatsStrings();
        final List<Pair<String, Pattern>> autoDateFormatsStrings = getAutoDateFormatsStrings();

        final HashSet<String> allFormats = new HashSet<String>(formats);
        allFormats.addAll(formats);
        allFormats.addAll(map(autoDateFormatsStrings, new Unary<String, Pair<String, Pattern>>() {
            @Override
            public String call(Pair<String, Pattern> pair) {
                return pair.left;
            }
        }));

        dateParser.setCustomDateFormats(formats);
        dateParser.setAutoDateFormats(autoDateFormatsStrings);
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

}

