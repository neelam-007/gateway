package com.l7tech.skunkworks.server;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.logging.*;
import java.io.IOException;

/**
 * Something to encapsulate the traffic logger functionality
 * (skunk work for now)
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 5, 2006<br/>
 */
public class TrafficLogger {
    private static final TrafficLogger singleton = new TrafficLogger();
    private boolean initialized = false;
    private Logger specialLogger;
    private boolean enabled = false;
    private static final Logger logger = Logger.getLogger(TrafficLogger.class.getName());

    public static TrafficLogger getInstance() {
        synchronized (singleton) {
            if (!singleton.initialized) {
                singleton.initialize();
            }
        }
        return singleton;
    }

    public void log(String msg) {
        if (!enabled) return;
        // on purpose finest to avoid getting handled by other handlers over which we have no control
        specialLogger.finest(msg);
    }

    public void log(PolicyEnforcementContext pec) {
        if (!enabled) return;
        // todo, resolve using context variables pattern recorded in the server configs
    }

    private void initialize() {
        specialLogger = Logger.getAnonymousLogger();
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(getPattern(), getLimit(), getCount(), true);
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot initialize file handler", e);
            throw new RuntimeException(e);
        }
        fileHandler.setFormatter(new Formatter() {
            public String format(LogRecord record) {
                return record.getMessage() + "\n";
            }
        });
        specialLogger.addHandler(fileHandler);
        specialLogger.setLevel(Level.ALL);
        enabled = isEnabled();
        initialized = true;
    }

    private String getPattern() {
        // todo, read it from serverconfigs
        return "/home/flascell/tmp/traffic_%g_%u.log";
    }

    private int getLimit() {
        // todo, read it from serverconfigs
        return 10000;
    }

    private int getCount() {
        // todo, read it from serverconfigs
        return 5;
    }

    private boolean isEnabled() {
        // todo, read it from serverconfigs
        return true;
    }
}
