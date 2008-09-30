/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public final class ProcessControllerStandalone {
    private final Logger logger = Logger.getLogger(ProcessControllerStandalone.class.getName());
    private final String[] args;

    private ProcessControllerStandalone(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws IOException {
        new ProcessControllerStandalone(args).run();
    }

    private synchronized void run() throws IOException {
        // TODO try to connect to running instance first?

        final ClassPathXmlApplicationContext spring = new ClassPathXmlApplicationContext("/com/l7tech/server/processcontroller/resources/processControllerApplicationContext.xml");
        spring.registerShutdownHook(); // in case of crash

        final ProcessController pc = (ProcessController)spring.getBean("processController");
        pc.setDaemon(false);
        pc.visitNodes(); // Detect states for any nodes that may already be running

        // batch mode
        final String verb = args[0];
        final Integer count = argParamCounts.get(verb);
        if (count == null) throw new IllegalArgumentException(verb + " is an unsupported command verb");
        if (args.length-1 < count) throw new IllegalArgumentException(verb + " requires " + count + " parameters");

        int arg = 1;
        if ("start".equalsIgnoreCase(verb)) {
            pc.startNode(args[arg++], true);
        } else if ("stop".equalsIgnoreCase(verb)) {
            pc.stopNode(args[arg++], ProcessController.DEFAULT_STOP_TIMEOUT);
        } else {
            throw new IllegalStateException("Unsupported command verb: " + verb);
        }

        try {
            spring.close();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Caught exception on shutdown", t);
        }
    }

    private static final Map<String, Integer> argParamCounts = Collections.unmodifiableMap(new HashMap<String, Integer>() {{
        put("start", 1);
        put("stop", 1);
    }});

}
