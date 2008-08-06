/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.node.PCServiceNodeConfig;
import com.l7tech.server.management.config.node.ServiceNodeConfig;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import java.util.BitSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ProcessControllerMain implements WrapperListener {
    private static final Logger logger = Logger.getLogger(ProcessControllerMain.class.getName());
    private ClassPathXmlApplicationContext ctx;

    public ProcessControllerMain() {
    }

    public static void main(String[] args) {
        new BitSet(Integer.MAX_VALUE);
        WrapperManager.start(new ProcessControllerMain(), args);
        ProcessControllerMain main = new ProcessControllerMain();
        main.start(args);
    }

    public Integer start(String[] strings) {
        logger.info("Starting Process Controller...");
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/l7tech/server/processcontroller/processControllerApplicationContext.xml");
        ctx.registerShutdownHook();
        this.ctx = ctx;

        final ProcessController pc = (ProcessController)ctx.getBean("processController");
        Set<ServiceNodeConfig> nodes = pc.getConfigService().getGateway().getServiceNodes();
        final ServiceNodeConfig node = nodes.iterator().next();
        logger.info("Starting node " + node.getName());
        pc.startNode((PCServiceNodeConfig)node);
        return 0;
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

    public void controlEvent(int i) {
        if (WrapperManager.isControlledByNativeWrapper()) {
            logger.log(Level.INFO, "controlEvent({0}) handled by native wrapper", i);
        } else {
            logger.log(Level.INFO, "controlEvent({0})", i);
        }
    }
}
