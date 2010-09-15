package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Config;
import com.l7tech.util.Resolver;
import com.l7tech.util.ValidatedConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean which manages a Thread Pool that holds / manages all JMS worker threads.
 * It's purpose is to manage property change events related to the pool.
 *
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author vchan
 */

public class JmsThreadPool extends ThreadPoolBean<JmsTask> implements PropertyChangeListener {
    public JmsThreadPool(ServerConfig serverConfig) {
        super("JmsThreadPool",
                validated(serverConfig).getIntProperty(ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT, 25));
        validatedConfig = validated(serverConfig);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT.equals(evt.getPropertyName())) {
            final int maxPoolSize = validatedConfig.getIntProperty(ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT, 25);

            threadPool.setMaxPoolSize(maxPoolSize);

            String newValue = (evt.getNewValue() != null ? evt.getNewValue().toString() : null);
            logger.log(Level.CONFIG, "Updated JMS ThreadPool size to {0}.", newValue);

        } else if (PARAM_THREAD_DISTRIBUTION.equals(evt.getPropertyName())) {
            //todo
        }
    }

    // - PRIVATE

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                if(ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT.equals(key)){
                    return "jms.listenerThreadLimit";
                }
                return null;
            }
        } );

        vc.setMinimumValue( ServerConfig.PARAM_JMS_LISTENER_THREAD_LIMIT, 5 );
        return vc;
    }

    private final Config validatedConfig;
    private static Logger logger = Logger.getLogger(JmsThreadPool.class.getName());

    private static final String PARAM_THREAD_DISTRIBUTION = "jmsEndpointThreadDistribution";
}
