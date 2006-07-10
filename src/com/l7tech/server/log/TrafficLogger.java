package com.l7tech.server.log;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.mime.NoSuchPartException;

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
    private Auditor auditor;
    private SoapFaultManager soapFaultManager;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public void log(PolicyEnforcementContext pec) {
        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            if (!enabled) return;
            StringBuffer tolog = new StringBuffer();
            if (varsUsed.length > 0) {
                tolog.append(ExpandVariables.process(detail, pec.getVariableMap(varsUsed, auditor)));
            } else {
                tolog.append(detail);
            }
            if (includeReq) {
                String requestXml = null;
                try {
                    Message request = pec.getRequest();
                    if (request.getKnob(MimeKnob.class) != null) {
                        byte[] req = HexUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false));
                        String encoding = request.getMimeKnob().getFirstPart().getContentType().getEncoding();
                        requestXml = new String(req, encoding);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Could not get request contents", e);
                } catch (NoSuchPartException e) {
                    logger.log(Level.WARNING, "Could not get request contents", e);
                }
                if (requestXml == null) requestXml = "Request contents not available";
                tolog.append(" ").append(requestXml);
            }
            if (includeRes) {
                String responseXml = null;
                Message response = pec.getResponse();
                if (response.getKnob(MimeKnob.class) != null) {
                    try {
                        byte[] resp = HexUtils.slurpStream(response.getMimeKnob().getFirstPart().getInputStream(false));
                        String encoding = response.getMimeKnob().getFirstPart().getContentType().getEncoding();
                        responseXml = new String(resp, encoding);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Could not get response contents", e);
                    } catch (NoSuchPartException e) {
                        logger.log(Level.WARNING, "Could not get response contents", e);
                    }
                } else {
                    AssertionStatus globalstatus = pec.getPolicyResult();
                    if (globalstatus != null && globalstatus != AssertionStatus.NONE) {
                        // if no response is yet available, we're about to return a soap fault
                        responseXml = soapFaultManager.constructReturningFault(pec.getFaultlevel(), pec);
                    } else {
                        responseXml = "No response";
                    }
                }
                if (responseXml == null) responseXml = "Response contents not available";
                tolog.append(" ").append(responseXml);
            }
            specialLogger.finest(tolog.toString());
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
                            return TrafficLogger.this.format(record);
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

    private String format(LogRecord record) {
        return record.getMessage() + LINE_SEPARATOR;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (auditor == null) {
            auditor = new Auditor(this, applicationContext, logger);
        }
        if (soapFaultManager == null) {
            soapFaultManager = (SoapFaultManager)applicationContext.getBean("soapFaultManager");
        }
    }
}
