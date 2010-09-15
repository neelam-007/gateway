package com.l7tech.server.transport.email.asynch;

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
 * Bean which manages a Thread Pool that holds / manages all email listener worker threads.
 * It's purpose is to manage property change events related to the pool.
 */
public class EmailListenerThreadPool extends ThreadPoolBean<EmailTask> implements PropertyChangeListener{

    public EmailListenerThreadPool(ServerConfig serverConfig) {
        super("EmailListenerThreadPool",
                validated(serverConfig).getIntProperty(ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT, 25));
        validatedConfig = validated(serverConfig);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT.equals(evt.getPropertyName())) {
            final int maxPoolSize = validatedConfig.getIntProperty(ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT, 25);

            threadPool.setMaxPoolSize(maxPoolSize);

            String newValue = (evt.getNewValue() != null ? evt.getNewValue().toString() : null);
            logger.log(Level.CONFIG, "Updated Email Listener ThreadPool size to {0}.", newValue);

        }
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                if(ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT.equals(key)){
                    return "email.listenerThreadLimit";
                }
                return null;
            }
        } );

        vc.setMinimumValue( ServerConfig.PARAM_EMAIL_LISTENER_THREAD_LIMIT, 5 );
        return vc;
    }

    private final Config validatedConfig;
    private static final Logger logger = Logger.getLogger(EmailListenerThreadPool.class.getName());
}
