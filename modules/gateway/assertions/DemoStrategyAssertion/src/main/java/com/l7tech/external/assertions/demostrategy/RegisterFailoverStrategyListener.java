package com.l7tech.external.assertions.demostrategy;

import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.Service;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

public class RegisterFailoverStrategyListener {

    private static final Service[] FS = new Service[]{new Service()};
    public static final DemoStrategy STRATEGY = new DemoStrategy(FS);

    private static FailoverStrategyFactory failoverStrategyFactory;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        failoverStrategyFactory = (FailoverStrategyFactory)context.getBean("failoverStrategyFactory");
        failoverStrategyFactory.registerStrategy(STRATEGY);
        logger.info(STRATEGY.getDescription() + " strategy is registered to Failover Strategy Factory.");
    }

    public static synchronized void onModuleUnloaded() {
        failoverStrategyFactory.unregisterStrategy(STRATEGY);
        logger.info(STRATEGY.getDescription() + " strategy is unregistered from Failover Strategy Factory.");
    }

    private static final Logger logger = Logger.getLogger(RegisterFailoverStrategyListener.class.getName());
}
