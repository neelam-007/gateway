/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public final class ProcessControllerMain {
    private static final Logger logger = Logger.getLogger(ProcessControllerMain.class.getName());
    private ClassPathXmlApplicationContext ctx;

    private ProcessControllerMain() { }

    public static void main(String[] args) {
        ProcessControllerMain main = new ProcessControllerMain();
        main.start();
    }

    public void start() {
        logger.info("Starting Process Controller...");
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/l7tech/server/processcontroller/processControllerApplicationContext.xml");
        ctx.registerShutdownHook();
        this.ctx = ctx;

        final ProcessController pc = (ProcessController)ctx.getBean("processController");
        Set<NodeConfig> nodes = pc.getConfigService().getGateway().getNodes();
        final NodeConfig node = nodes.iterator().next();
        logger.info("Starting node " + node.getName());
        pc.startNode((PCNodeConfig)node);
    }

    public int stop(int exitCode) {
        logger.info("Shutting down");
        try {
            ctx.close();
            return exitCode;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Caught exception on shutdown", t);
            return 1;
        }
    }
}
