package com.l7tech.server.log;

import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.log.syslog.SyslogManager;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Provides the ability to do CRUD operations on SinkConfiguration rows in the database.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SinkManager
        extends HibernateEntityManager<SinkConfiguration, EntityHeader>
        implements ApplicationListener {
    //- PUBLIC

    public SinkManager( final ServerConfig serverConfig,
                        final SyslogManager syslogManager ) {
        if ( serverConfig == null ) throw new IllegalArgumentException("serverConfig must not be null");
        if ( syslogManager == null ) throw new IllegalArgumentException("syslogManager must not be null");

        this.serverConfig = serverConfig;
        this.syslogManager = syslogManager;
    }

    public Class<SinkConfiguration> getImpClass() {
        return SinkConfiguration.class;
    }

    public Class<SinkConfiguration> getInterfaceClass() {
        return SinkConfiguration.class;
    }

    public String getTableName() {
        return "sink_config";
    }

    public void onApplicationEvent(final ApplicationEvent event) {
        if ( event instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent evt = (EntityInvalidationEvent) event;

            if ( SinkConfiguration.class.isAssignableFrom(evt.getEntityClass()) ) {
                rebuildLogSinks();
            }
        }
    }

    //- PROTECTED

    @Override
    protected void initDao() {
        installHandlers();
        installLogConfigurationListener();
        rebuildLogSinks();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SinkManager.class.getName());
    
    private final DispatchingMessageSink dispatchingSink = new DispatchingMessageSink();
    private final ServerConfig serverConfig;
    private final SyslogManager syslogManager;

    /**
     * Re-install logging handlers.
     */
    private void installHandlers() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(new SinkHandler(dispatchingSink, MessageCategory.LOG));

        Logger trafficLogger = Logger.getLogger("com.l7tech.traffic");
        trafficLogger.addHandler(new SinkHandler(dispatchingSink, MessageCategory.TRAFFIC));
    }

    /**
     * Install a log configuration listener to re-install our Handlers when the
     * configuration changes.
     */
    private void installLogConfigurationListener() {
        LogManager.getLogManager().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                installHandlers();
            }
        });
    }

    /**
     * Attach the configured sinks.
     */
    private void rebuildLogSinks() {
        List<MessageSink> sinks = new ArrayList();

        // close old first (so file handle released, etc)
        // this could be removed if we this is not desired behaviour
        // (in which case the old Sinks that are not used will be automatically
        // closed by the DispatchingMessageSink)
        dispatchingSink.setMessageSinks( Collections.<MessageSink>emptySet() );

        // construct new
        Collection<SinkConfiguration> sinkConfigs = loadSinkConfigurations();
        for ( SinkConfiguration sinkConfiguration : sinkConfigs ) {
            if ( sinkConfiguration.isEnabled() ) {
                if ( logger.isLoggable(Level.CONFIG) )
                    logger.log(Level.CONFIG, "Processing log sink configuration ''{0}''.", sinkConfiguration.getName());
                MessageSink sink = buildSinkForConfiguration( sinkConfiguration );
                if ( sink != null ) {
                    if ( logger.isLoggable(Level.CONFIG) )
                        logger.log(Level.CONFIG, "Installing log sink ''{0}''.", sinkConfiguration.getName());
                    sinks.add( sink );
                }
            } else {
                if ( logger.isLoggable(Level.CONFIG) )
                    logger.log(Level.CONFIG, "Ignoring disabled log sink configuration ''{0}''.", sinkConfiguration.getName());                
            }
        }

        // install new
        dispatchingSink.setMessageSinks( sinks );
    }

    /**
     * Load sink configurations 
     */
    private Collection<SinkConfiguration> loadSinkConfigurations() {
        Collection<SinkConfiguration> sinkConfigurations = Collections.EMPTY_LIST;

        try {
            sinkConfigurations = this.findAll();
        } catch ( FindException fe ) {
            logger.log(
                    Level.WARNING,
                    "Unexpected error loading messages sink configuration.",
                    fe );
        }

        return sinkConfigurations;
    }

    /**
     * Build a MessageSink for the given configuration
     */
    private MessageSink buildSinkForConfiguration( final SinkConfiguration configuration ) {
        MessageSink sink = null;
        SinkConfiguration.SinkType type = configuration.getType();

        try {
            switch ( type ) {
                case FILE:
                    sink = new FileMessageSink( serverConfig, configuration );
                    break;
                case SYSLOG:
                    sink = new SyslogMessageSink( serverConfig, configuration, syslogManager );
                    break;
                default:
                    logger.log(Level.WARNING, "Ignoring unknown type of sink ''{0}''.", type);
                    break;
            }
        } catch (MessageSinkSupport.ConfigurationException ce) {
            logger.log(Level.WARNING, "Error creating log sink '"+configuration.getName()+"'.", ce);
        }

        return sink;
    }
}
