package com.l7tech.server.log;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.common.audit.Auditor;

import java.util.logging.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * Handles the traffic logger functionality. Records traffic information to a log. Where and what to log
 * is controlled by server configs either through serverconfig.properties overrides or the ssm cluster
 * properties table. Those settings can be changed on the fly and will eventually take effect.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 7, 2006<br/>
 */
public class TrafficLogger implements ApplicationContextAware {
    private boolean enabled = false;
    private String detail = "${request.time}, ${request.soap.namespace}, ${request.soap.operationname}, ${response.http.status}";
    private String pattern = "/ssg/logs/traffic_%g_%u.log";
    private int limit = 5242880;
    private int count = 2;
    private boolean includeReq = false;
    private boolean includeRes = false;
    private Logger specialLogger;
    private String[] varsUsed;
    private static final Logger logger = Logger.getLogger(TrafficLogger.class.getName());
    private ServerConfig serverConfig;
    private final Timer checker = new Timer(true);
    private ApplicationContext applicationContext;
    private Auditor auditor;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public void log(PolicyEnforcementContext pec) {
        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            if (!enabled) return;
            String tolog = detail;
            if (varsUsed.length > 0) {
                tolog = ExpandVariables.process(tolog, pec.getVariableMap(varsUsed, auditor));
            }
            specialLogger.finest(tolog);
        } finally {
            lock.unlock();
        }
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        // initialize the timer which checks for config changes
        TimerTask task = new TimerTask() {
            public void run() {
                TrafficLogger.this.updateSettings();
            }
        };
        checker.schedule(task, 5000, 30000);
    }

    private void updateSettings() {
        boolean somethingChanged = false;
        boolean loggerChanged = false;
        boolean tmpenabled = Boolean.parseBoolean(serverConfig.getProperty("trafficLoggerEnabled"));
        if (tmpenabled != enabled) {
            somethingChanged = true;
        }
        String tmpdetail = serverConfig.getProperty("trafficLoggerDetail");
        if (!tmpdetail.equals(detail)) {
            somethingChanged = true;
        }
        String tmpPattern = serverConfig.getProperty("trafficLoggerPattern");
        if (!tmpPattern.equals(pattern)) {
            somethingChanged = true;
            loggerChanged = true;
        }
        int tmpLimit = Integer.parseInt(serverConfig.getProperty("trafficLoggerLimit"));
        if (tmpLimit != limit) {
            somethingChanged = true;
            loggerChanged = true;
        }
        int tmpCount = Integer.parseInt(serverConfig.getProperty("trafficLoggerCount"));
        if (tmpCount != count) {
            somethingChanged = true;
            loggerChanged = true;
        }
        boolean tmpIncludeReq = Boolean.parseBoolean(serverConfig.getProperty("trafficLoggerRecordReq"));
        if (tmpIncludeReq != includeReq) {
            somethingChanged = true;
        }
        boolean tmpIncludeRes = Boolean.parseBoolean(serverConfig.getProperty("trafficLoggerRecordRes"));
        if (tmpIncludeRes != includeRes) {
            somethingChanged = true;
        }
        if (somethingChanged) {
            logger.finest("updating traffic logger settings");
            ReentrantReadWriteLock.WriteLock lock = cacheLock.writeLock();
            lock.lock();
            try {
                if (loggerChanged || specialLogger == null) {
                    Logger tmpSpecialLogger = Logger.getAnonymousLogger();
                    FileHandler fileHandler;
                    try {
                        fileHandler = new FileHandler(tmpPattern, tmpLimit, tmpCount, true);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "cannot initialize file handler", e);
                        return;
                    }
                    fileHandler.setFormatter(new Formatter() {
                        public String format(LogRecord record) {
                            return record.getMessage() + "\n";
                        }
                    });
                    tmpSpecialLogger.addHandler(fileHandler);
                    tmpSpecialLogger.setLevel(Level.ALL);
                    specialLogger = tmpSpecialLogger;
                }
                enabled = tmpenabled;
                pattern = tmpPattern;
                limit = tmpLimit;
                count = tmpCount;
                includeReq = tmpIncludeReq;
                includeRes = tmpIncludeRes;
                if (!tmpdetail.equals(detail)) {
                    detail = tmpdetail;
                    varsUsed = ExpandVariables.getReferencedNames(detail);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (auditor == null) {
            auditor = new Auditor(this, applicationContext, logger);
        }
    }
}
