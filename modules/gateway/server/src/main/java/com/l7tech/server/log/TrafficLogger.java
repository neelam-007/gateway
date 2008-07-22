package com.l7tech.server.log;

import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.io.IOUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SoapFaultManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

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
public class TrafficLogger implements ApplicationContextAware, PropertyChangeListener {

    //- PUBLIC

    /**
     * serverconfig property name meaning
     * what we actually log for each request (may contain context variables)
     */
    public static final String SERVCFG_DETAIL = "trafficLoggerDetail";

    /**
     * serverconfig property name meaning
     * whether or not to append at the end of each record the actual
     * contents of the request received by the ssg
     */
    public static final String SERVCFG_ADDREQ = "trafficLoggerRecordReq";

    /**
     * serverconfig property name meaning
     * whether or not to append at the end of each record the actual contents
     * of the response returned by the ssg
     */
    public static final String SERVCFG_ADDRES = "trafficLoggerRecordRes";

    /**
     *
     */
    public TrafficLogger(final ServerConfig serverConfig,
                         final SoapFaultManager soapFaultManager) {
        this.serverConfig = serverConfig;
        this.soapFaultManager = soapFaultManager;
    }

    /**
     *
     */
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if (auditor == null) {
            auditor = new Auditor(this, applicationContext, logger);
        }
    }

    /**
     *
     */
    public void propertyChange(final PropertyChangeEvent evt) {
        updateSettings();
    }

    /**
     * Enable or disable traffic logging.
     *
     * @param enabled True to enable traffic logging
     */
    public void setEnabled(final boolean enabled) {
        this.enabled.set(enabled);
    }

    /**
     * Logs request to the traffic logger as per server setting. By default, this functionality is
     * disabled in which case, calling this will return immediatly.
     * @param pec the request context to record in the traffic logger
     */
    public void log(final PolicyEnforcementContext pec) {
        if (!enabled.get())
            return;

        // grab config
        String detail;
        String[] varsUsed;
        boolean includeReq;
        boolean includeRes;

        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            detail = this.detail;
            varsUsed = this.varsUsed;
            includeReq = this.includeReq;
            includeRes = this.includeRes;
        } finally {
            lock.unlock();
        }

        StringBuilder tolog = new StringBuilder();
        if (varsUsed.length > 0) {
            tolog.append(ExpandVariables.process(detail, pec.getVariableMap(varsUsed, auditor), auditor));
        } else {
            tolog.append(detail);
        }

        if (includeReq) {
            String requestXml = getMessageText(pec.getRequest(), "request");
            if (requestXml == null) requestXml = "Request contents not available";
            tolog.append(" ").append(requestXml);
        }

        if (includeRes) {
            String responseXml = null;
            Message response = pec.getResponse();
            if (response.getKnob(MimeKnob.class) != null) {
                responseXml = getMessageText(response, "response");
            } else {
                AssertionStatus globalstatus = pec.getPolicyResult();
                if (globalstatus != null && globalstatus != AssertionStatus.NONE) {
                    // if no response is yet available, we're about to return a soap fault
                    responseXml = soapFaultManager.constructReturningFault(pec.getFaultlevel(), pec);
                } else {
                    // sometimes, there is just no response
                    responseXml = "No response";
                }
            }
            if (responseXml == null) responseXml = "Response contents not available";
            tolog.append(" ").append(responseXml);
        }

        // log it
        trafficLogger.info(tolog.toString());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(TrafficLogger.class.getName());
    private static final Logger trafficLogger = Logger.getLogger("com.l7tech.traffic");

    private final ServerConfig serverConfig;
    private final SoapFaultManager soapFaultManager;
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private Auditor auditor;

    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private String detail = "${request.time}, ${request.soap.namespace}, ${request.soap.operationname}, ${response.http.status}";
    private String[] varsUsed = Syntax.getReferencedNames(detail);
    private boolean includeReq = false;
    private boolean includeRes = false;

    /**
     *
     */
    private String getMessageText(final Message message, final String description) {
        String text = null;

        try {
            if (message.getKnob(MimeKnob.class) != null) {
                byte[] req = IOUtils.slurpStream(message.getMimeKnob().getFirstPart().getInputStream(false));
                String encoding = message.getMimeKnob().getFirstPart().getContentType().getEncoding();
                text = new String(req, encoding);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not get "+description+" contents", e);
        } catch (NoSuchPartException e) {
            logger.log(Level.WARNING, "Could not get "+description+" contents", e);
        }

        return text;
    }

    /**
     * 
     */
    private void updateSettings() {
        if ( logger.isLoggable(Level.CONFIG) ) {
            logger.log(Level.CONFIG, "Updating traffic logger settings");
        }

        String tmpdetail = serverConfig.getProperty(SERVCFG_DETAIL);
        boolean tmpIncludeReq = Boolean.parseBoolean(serverConfig.getProperty(SERVCFG_ADDREQ));
        boolean tmpIncludeRes = Boolean.parseBoolean(serverConfig.getProperty(SERVCFG_ADDRES));

        ReentrantReadWriteLock.WriteLock lock = cacheLock.writeLock();
        lock.lock();
        try {
            includeReq = tmpIncludeReq;
            includeRes = tmpIncludeRes;
            detail = tmpdetail;
            varsUsed = Syntax.getReferencedNames(detail);
            if (varsUsed == null) {
                varsUsed = new String[0];
            }
        } finally {
            lock.unlock();
        }
    }
}
