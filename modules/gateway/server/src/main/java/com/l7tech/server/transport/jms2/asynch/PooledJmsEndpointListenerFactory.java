package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.JmsBlockPolicy;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointListenerFactory;
import com.l7tech.server.util.ThreadPoolBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author vchan
 */
public class PooledJmsEndpointListenerFactory implements JmsEndpointListenerFactory, ApplicationContextAware {



    public PooledJmsEndpointListenerFactory(ThreadPoolBean threadPoolBean) {
        this.threadPoolBean = threadPoolBean;
    }

    /**
     * Creates JmsEndpointListener implemented by the PooledJmsEndpointListener type.
     *
     * @param endpointConfig the configuration properties for one Jms endpoint
     * @return a JmsEndpointListener instance
     */
    @Override
    public JmsEndpointListener createListener(final JmsEndpointConfig endpointConfig) {

        ThreadPoolBean threadPool = this.threadPoolBean;

        if (JmsUtil.isDedicatedThreadPool(endpointConfig.getConnection())) {
            String poolSize = endpointConfig.getConnection().properties().getProperty(JmsConnection.PROP_DEDICATED_POOL_SIZE);
            JmsBlockPolicy blockPolicy = applicationContext.getBean("jmsBlockPolicy", JmsBlockPolicy.class);
            threadPool = new ThreadPoolBean(ServerConfig.getInstance(), endpointConfig.getEndpoint().getName() +
                    " Thread Pool", "", "", Integer.parseInt(poolSize), blockPolicy);
            threadPool.start();
        }
        return new PooledJmsEndpointListenerImpl(endpointConfig, threadPool);
    }

    // - PRIVATE
    private final ThreadPoolBean threadPoolBean;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}