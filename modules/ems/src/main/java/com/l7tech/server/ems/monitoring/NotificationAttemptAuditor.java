package com.l7tech.server.ems.monitoring;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.ems.EsmMessages;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.enterprise.SsgNodeManager;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class NotificationAttemptAuditor implements InitializingBean, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(NotificationAttemptAuditor.class.getName());

    private static final long DELAY_UNTIL_FIRST_AUDIT_CHECK = SyspropUtil.getLong("com.l7tech.server.ems.monitoring.auditCheck.delayUntilFirst", 13417L);
    private static final long DELAY_BETWEEN_AUDIT_CHECKS = SyspropUtil.getLong("com.l7tech.server.ems.monitoring.auditCheck.delayBetween", 596261L);

    private final Timer timer;
    private final PlatformTransactionManager transactionManager;
    private final SsgClusterManager ssgClusterManager;
    private final SsgNodeManager ssgNodeManager;
    private final GatewayContextFactory gatewayContextFactory;
    private final TimerTask notificationAuditorTask = makeNotificationAuditorTask();
    private ApplicationContext applicationContext;

    public NotificationAttemptAuditor(Timer timer, PlatformTransactionManager transactionManager, SsgClusterManager ssgClusterManager, SsgNodeManager ssgNodeManager, GatewayContextFactory gatewayContextFactory) {
        this.timer = timer;
        this.transactionManager = transactionManager;
        this.ssgClusterManager = ssgClusterManager;
        this.ssgNodeManager = ssgNodeManager;
        this.gatewayContextFactory = gatewayContextFactory;
    }

    private TimerTask makeNotificationAuditorTask() {
        return new TimerTask() {
            public void run() {
                AuditContextUtils.doAsSystem(new Runnable() {
                    public void run() {
                        try {
                            collectAndAuditNotifications();
                        } catch (FindException e) {
                            logger.log(Level.WARNING, "Unable to read clusters from database: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        } catch (Exception e) {
                            logger.log( Level.WARNING, "Unexpected error collecting notifications from process controllers: "+ ExceptionUtils.getMessage(e), e);
                        }
                    }
                });
            }
        };
    }

    public void afterPropertiesSet() throws Exception {
        timer.schedule(notificationAuditorTask, DELAY_UNTIL_FIRST_AUDIT_CHECK, DELAY_BETWEEN_AUDIT_CHECKS);
    }

    private void collectAndAuditNotifications() throws FindException {
        Collection<SsgCluster> clusters = ssgClusterManager.findAll();
        for (SsgCluster cluster : clusters) {
            Set<SsgNode> nodes = cluster.getNodes();
            for (SsgNode node : nodes) {
                collectAndAuditNotifications(node);
            }
        }
    }

    private void collectAndAuditNotifications(final SsgNode node) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute( new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                AuditContext auditContext = (AuditContext)applicationContext.getBean("auditContext", AuditContext.class);
                try {
                    ProcessControllerContext nodeContext = gatewayContextFactory.createProcessControllerContext(node);
                    long timeBeforeQuery = System.currentTimeMillis();
                    List<NotificationAttempt> attempts = nodeContext.getMonitoringApi().getRecentNotificationAttempts(node.getNotificationAuditTime());
                    long timeOfMostRecentNotification = auditNotificationAttempts(node, auditContext, attempts);
                    node.setNotificationAuditTime(timeOfMostRecentNotification > 0 ? timeOfMostRecentNotification : timeBeforeQuery);
                    ssgNodeManager.update(node);
                } catch (GatewayException e) {
                    logger.log(Level.INFO, "Unable to connect to process controller for node " + node.getIpAddress() + " to collect notifications: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (UpdateException e) {
                    logger.log(Level.WARNING, "Unable to update last notification time for node " + node.getIpAddress() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (SOAPFaultException e) {
                    if ( !ProcessControllerContext.isNetworkException(e) && !ProcessControllerContext.isConfigurationException(e) ) {
                        logger.log(Level.WARNING, "Unable to connect to process controller for node " + node.getIpAddress() + " to collect notifications: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            }
        });
    }

    private long auditNotificationAttempts(SsgNode node, AuditContext auditContext, List<NotificationAttempt> attempts) {
        long mostRecentTime = 0;

        if (attempts == null || attempts.isEmpty())
            return mostRecentTime;

        for (NotificationAttempt attempt : attempts) {
            long time = attempt.getTimestamp();
            if (time > mostRecentTime)
                mostRecentTime = time;
            auditNotificationAttempt(node, auditContext, attempt);
        }
        return mostRecentTime;
    }

    private void auditNotificationAttempt(SsgNode node, AuditContext auditContext, NotificationAttempt attempt) {
        SsgCluster cluster = node.getSsgCluster();
        addSingleDetail(auditContext, EsmMessages.CLUSTER_NAME, cluster.getName());
        addSingleDetail(auditContext, EsmMessages.CLUSTER_GUID, cluster.getGuid());
        addSingleDetail(auditContext, EsmMessages.NODE_NAME, node.getName());
        addSingleDetail(auditContext, EsmMessages.NODE_GUID, node.getGuid());
        addSingleDetail(auditContext, EsmMessages.NODE_IP, node.getIpAddress());
        addSingleDetail(auditContext, EsmMessages.NOTIFICATION_MESSAGE, attempt.getMessage());
        addSingleDetail(auditContext, EsmMessages.NOTIFICATION_TIME, new Date(attempt.getTimestamp()).toString());
        addSingleDetail(auditContext, EsmMessages.NOTIFICATION_STATUS, attempt.getStatus().name());

        final String nodeId = node.getGuid();
        final String shortMessage = "Node " + node.getIpAddress() + " has sent a notification";
        final Level level = attempt.getStatus().equals(NotificationAttempt.StatusType.FAILED)? Level.WARNING : Level.INFO;
        auditContext.setCurrentRecord(new SystemAuditRecord(level,
                nodeId,
                Component.ENTERPRISE_MANAGER,
                shortMessage,
                true,
                0,
                null,
                null,
                "Notification Sent",
                node.getIpAddress()));

        auditContext.flush();
    }

    private void addSingleDetail(AuditContext context, AuditDetailMessage msg, String arg) {
        context.addDetail(new AuditDetail(msg, arg == null ? "<none>" : arg), this);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
