package com.l7tech.server.transport.jms2;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JmsBlockPolicy extends ThreadPoolExecutor.AbortPolicy {

    private static final Logger logger = Logger.getLogger(JmsBlockPolicy.class.getName());
    protected static final int WAIT = 30; // 30 seconds

    public JmsBlockPolicy() { }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        logger.log(Level.INFO, JmsMessages.INFO_THREADPOOL_LIMIT_REACHED_DELAY);
        try {
            if ( !e.getQueue().offer(r, WAIT, TimeUnit.SECONDS) ) {
                super.rejectedExecution(r, e);
            }
        } catch (InterruptedException e1) {
            super.rejectedExecution(r, e);
        }
    }
}
