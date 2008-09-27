package com.l7tech.console.util;

import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.logging.LogMessage;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

/**
 * This class performs retrieval of the actual audit records from the database.
 * <p/>
 * construct() is called when a row is clicked on in the LogPanel's table and the associated SSGLogRecord is retrieved
 * if it does not yet exist on the client.
 */
public class AuditRecordWorker extends SwingWorker {
    private AuditAdmin auditAdminService = null;
    private LogMessage logMessage = null;
    private AuditSearchCriteria asc = null;
    private Map<Long, AuditRecord> auditRecords;

    private static Logger logger = Logger.getLogger(AuditRecordWorker.class.getName());

    //use this constructor if we only want to retrieve 1 log record
    //used when a record row is selected in the table
    public AuditRecordWorker(AuditAdmin auditAdminService, LogMessage logMessage) {
        if (auditAdminService == null || logMessage == null) throw new IllegalArgumentException();
        this.auditAdminService = auditAdminService;
        this.logMessage = logMessage;
    }

    //use this constructor if we want multiple audit records, used for audit export
    public AuditRecordWorker(AuditAdmin auditAdminService, LogRequest lr, ClusterStatusAdmin clusterStatusAdmin, Map<Long, AuditRecord> auditRecords) {
        if (auditAdminService == null || lr == null || clusterStatusAdmin == null) throw new IllegalArgumentException();
        this.auditAdminService = auditAdminService;
        this.auditRecords = auditRecords;

        Map<String, String> nodeNameIdMap = new HashMap<String, String>();

        // retrieve node status
        ClusterNodeInfo[] clusterNodes = new ClusterNodeInfo[0];

        try {
            clusterNodes = clusterStatusAdmin.getClusterStatus();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find cluster status from server", e);
        }

//        if (clusterNodes == null) {
//            return null;
//        }

        for (ClusterNodeInfo nodeInfo : clusterNodes) {
            GatewayStatus nodeStatus = new GatewayStatus(nodeInfo);
            String clusterNodeId = nodeStatus.getNodeId();
            if (clusterNodeId != null) {
                nodeNameIdMap.put(nodeStatus.getName(), nodeStatus.getNodeId());
            }
        }

        this.asc = new AuditSearchCriteria.Builder().
                fromTime(lr.getStartMsgDate()).
                toTime(lr.getEndMsgDate()).
                fromLevel(lr.getLogLevel()).
                nodeId(nodeNameIdMap.get(lr.getNodeName())).
                serviceName(lr.getServiceName()).
                message(lr.getMessage()).
                requestId(lr.getRequestId()).build();
    }

    public Object construct() {
        if (asc == null) {//then we only want a single record
            return findByOid();
        }

        //then we want the full audit records for every auditRecordHeader
        Collection<AuditRecord> rawAudits = new ArrayList<AuditRecord>();
        try{
            rawAudits = auditAdminService.find(asc);
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Unable to retrieve audit records from server.");
        }

        //build a map of oid to auditRecord
        for(AuditRecord ar : rawAudits){
            auditRecords.put(ar.getOid(), ar);
        }

        if(auditRecords.size() > 0){
            return auditRecords;
        }

        return null;
    }

    private Object findByOid() {
        AuditRecord auditRecord = null;

        try {
            //retrieve the full AuditRecord associated with this AuditRecordHeader
            auditRecord = auditAdminService.findByPrimaryKey(logMessage.getHeader().getOid());
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Unable to retrieve audit record from server", e);
        }

        //add the AuditRecord to the LogMessage
        if (auditRecord != null) {
            logMessage.setLog(auditRecord);
            return logMessage;
        }

        return null;
    }

    public LogMessage getUpdatedLogMessage(){
        return logMessage;
    }
}
