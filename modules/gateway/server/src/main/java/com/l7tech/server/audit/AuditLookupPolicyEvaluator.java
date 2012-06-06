package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the audit lookup policy to retrieve audits.
 * <p/>
 */
public class AuditLookupPolicyEvaluator  {
    private static final Logger logger = Logger.getLogger(AuditLookupPolicyEvaluator.class.getName());

    private final Config config;
    private final PolicyCache policyCache;
    private final Map<String,AuditRecord> auditRecordsCache = new HashMap<String, AuditRecord>();
    private final AuditDetailPropertiesDomUnmarshaller detailUnmarshaller = new AuditDetailPropertiesDomUnmarshaller();

    public AuditLookupPolicyEvaluator(Config config, PolicyCache policyCache) {
        this.config = config;
        this.policyCache = policyCache;
    }

    private String loadAuditSinkPolicyGuid() {
        return config.getProperty( ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID );
    }

    public List<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException{

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        AssertionStatus assertionStatus = executeAuditLookupPolicy(criteria, context);
        if(assertionStatus != AssertionStatus.NONE){
            throw new FindException("Audit Lookup Policy Failed");
        }
        List<AuditRecordHeader> recordHeaders = makeAuditRecords(context);
        return recordHeaders;
        
    }
    
    private List<AuditRecordHeader> makeAuditRecords(PolicyEnforcementContext context){
        auditRecordsCache.clear();
        Object sizeObj = null;
        try {

            final String prefix = "recordQuery";

            sizeObj = context.getVariable(prefix + ".queryresult.count");
            List<AuditRecordHeader> recordHeaders = new ArrayList<AuditRecordHeader> ();


            Object[] id_var = (Object[])context.getVariable(prefix + ".id");
            Object[] nodeid_var = (Object[])context.getVariable(prefix+".nodeid");
            Object[] time_var = (Object[])context.getVariable(prefix+".time");
            Object[] type_var = (Object[])context.getVariable(prefix+".type");
            Object[] auditLevel_var = (Object[])context.getVariable(prefix+".audit_level");
            Object[] name_var = (Object[])context.getVariable(prefix+".name");
            Object[] message_var = (Object[])context.getVariable(prefix+".message");
            Object[] ip_addr_var = (Object[])context.getVariable(prefix+".ip_address");
            Object[] userName_var = (Object[])context.getVariable(prefix+".user_name");
            Object[] userId_var = (Object[])context.getVariable(prefix+".user_id");
            Object[] providerOid_var = (Object[])context.getVariable(prefix+".provider_oid");
            Object[] signature_var = (Object[])context.getVariable(prefix+".signature");
            Object[] entityClass_var = (Object[])context.getVariable(prefix+".entity_class");
            Object[] entityId_var = (Object[])context.getVariable(prefix+".entity_id");
            Object[] status_var = (Object[])context.getVariable(prefix+".status");
            Object[] requestId_var = (Object[])context.getVariable(prefix+".request_id");
            Object[] serviceOid_var = (Object[])context.getVariable(prefix+".service_oid");
            Object[] operationName_var = (Object[])context.getVariable(prefix+".operation_name");
            Object[] authenticated_var = (Object[])context.getVariable(prefix+".authenticated");
            Object[] authenticationType_var = (Object[])context.getVariable(prefix+".authenticationType");
            Object[] requestLength_var = (Object[])context.getVariable(prefix+".request_length");
            Object[] responseLength_var = (Object[])context.getVariable(prefix+".response_length");
            Object[] requestZip_var = (Object[])context.getVariable(prefix+".request_xml");
            Object[] responseZip_var = (Object[])context.getVariable(prefix+".response_xml");
            Object[] responseStatus_var = (Object[])context.getVariable(prefix+".response_status");
            Object[] latency_var = (Object[])context.getVariable(prefix+".routing_latency");
            Object[] properties_var = (Object[])context.getVariable(prefix+".properties");
            Object[] componentId_var = (Object[])context.getVariable(prefix+".component_id");
            Object[] action_var = (Object[])context.getVariable(prefix+".action");


            for(int i = 0 ; i < (Integer)sizeObj ; ++i){
                String id = (String)id_var[i];
                String nodeid = (String)nodeid_var[i];
                Long time = (Long)time_var[i];
                String type = (String)type_var[i];
                String auditLevel = (String)auditLevel_var[i];
                String name = (String)name_var[i];
                String message = (String)message_var[i];
                String ip_addr = (String)ip_addr_var[i];
                String userName = (String)userName_var[i];
                String userId = (String)userId_var[i];
                Long providerOid = (Long)providerOid_var[i];
                String signature = (String)signature_var[i];
                String entityClass = (String)entityClass_var[i];
                Long entityId = (Long)entityId_var[i];
                Integer status = (Integer)status_var[i];
                String requestId = (String)requestId_var[i];
                Long serviceOid = (Long)serviceOid_var[i];
                String operationName = (String)operationName_var[i];
                Boolean authenticated = (Boolean)authenticated_var[i];
                String authenticationType = (String)authenticationType_var[i];
                Integer requestLength = (Integer)requestLength_var[i];
                Integer responseLength = (Integer)responseLength_var[i];
                byte[] requestZip = (byte[])requestZip_var[i];
                byte[] responseZip = (byte[])responseZip_var[i];
                Integer responseStatus = (Integer)responseStatus_var[i];
                Integer latency = (Integer)latency_var[i];
                String properties = (String)properties_var[i];
                Integer componentId = (Integer)componentId_var[i];
                String action = (String)action_var[i];

                AuditRecord record = ExternalAuditsUtils.makeAuditRecord(
                        id,nodeid,time,type,auditLevel,name,message,ip_addr,userName,userId,providerOid,
                        entityClass,entityId,status,requestId,serviceOid,operationName,authenticated,authenticationType,
                        requestLength,responseLength,requestZip,responseZip,responseStatus,latency,componentId,action,properties);

                record.setOid(i); // todo?
                auditRecordsCache.put(id,record);
                AuditRecordGuidHeader header = new AuditRecordGuidHeader(record,id,time);
                header.setSignature(signature);

                recordHeaders.add(header);
            }
            makeAuditDetails(context);
            return recordHeaders;

        } catch (NoSuchVariableException e) {
            logger.warning("Error creating audit records, some fields not present: "+e.getMessage());
        }
        return new ArrayList<AuditRecordHeader>();
    }

    private void makeAuditDetails(PolicyEnforcementContext context) {
        Object sizeObj = null;
        try {
            int iterations = 0;
            while (true){
                String prefix = "detailQuery"+iterations;
                sizeObj = context.getVariable(prefix+".queryresult.count");

                Object[] audit_oid_var = (Object[])context.getVariable(prefix+".audit_oid");
                Object[] time_var = (Object[])context.getVariable(prefix+".time");
                Object[] componentId_var = (Object[])context.getVariable(prefix+".component_id");
                Object[] ordinal_var = (Object[])context.getVariable(prefix+".ordinal");
                Object[] messageId_var = (Object[])context.getVariable(prefix+".message_id");
                Object[] message_var = (Object[])context.getVariable(prefix+".exception_message");
                Object[] props_var = (Object[])context.getVariable(prefix+".properties");

                for(int i = 0 ; i < (Integer)sizeObj ; ++i){
                    String audit_oid = (String)audit_oid_var[i];
                    Long time = (Long)time_var[i];
                    Integer componentId = (Integer)componentId_var[i];
                    Integer ordinal = (Integer)ordinal_var[i];
                    Integer messageId = (Integer)messageId_var[i];
                    String message = (String)message_var[i];
                    String properties = (String)props_var[i];

                    Map<String,Object> props =  getDetailsPropertiesMap(properties);

                    String[] params = (props == null || props.get("params") == null )? null : (String [])props.get("params");
                    AuditDetail detail = new AuditDetail( messageId, params ,  message,  time);
                    detail.setAuditGuid(audit_oid);
                    detail.setOrdinal(ordinal);
                    detail.setComponentId(componentId);
                    detail.setOid(iterations+(iterations*i*6));  // todo [wynne] remove

                    AuditRecord record = auditRecordsCache.get(audit_oid);
                    detail.setAuditRecord(record);
                    Set<AuditDetail> details = record.getDetails();
                    details.add(detail);
                    record.setDetails(details);
                }

                ++iterations;
            }
        } catch (NoSuchVariableException e) {
            // do nothing, end of trying to get details
        }
    }
    
    public AuditRecord findByGuid(String guid) throws FindException{
        return auditRecordsCache.get(guid);
    }


    private Map<String,Object>  getDetailsPropertiesMap(String props){
        try {
            Document doc = XmlUtil.parse(props);
            return detailUnmarshaller.unmarshal(doc.getDocumentElement());
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error parsing audit detail properties: ", props);

        } catch (MarshalException e) {
            logger.log(Level.WARNING, "Error parsing audit detail properties: ", props);
        }
        return null;
    }

    /**
     * Run the current audit lookup policy to retrieve audits.
     *
     * @param criteria  the audit record search criteria
     * @param   policyContext  context for the lookup policy to use
     * @return
     */
    private AssertionStatus executeAuditLookupPolicy(AuditSearchCriteria criteria, PolicyEnforcementContext policyContext) {
        ServerPolicyHandle sph = null;
        try {
            final String guid = loadAuditSinkPolicyGuid();
            if (guid == null || guid.trim().length() < 1) {
                logger.log(Level.FINEST, "No audit lookup policy is configured");
                return null;
            }
            policyContext.setAuditLevel(Level.INFO);
            setQueryParams(criteria, policyContext);

            // Use fake service
            final PublishedService svc = new PublishedService();
            svc.setName("[Internal audit sink policy pseudo-service]");
            svc.setSoap(false);
            policyContext.setService(svc);

            sph = policyCache.getServerPolicy(guid);
            if (sph == null) {
                logger.log(Level.WARNING, "Unable to access configured audit lookup policy -- no policy with GUID {0} is present in policy cache (invalid policy?)", guid);
                return AssertionStatus.SERVER_ERROR;
            }

            AssertionStatus status = sph.checkRequest(policyContext);

            // We won't bother processing any deferred assertions because they mostly deal with response processing
            // and we intend to ignore any response from this policy.

            if (!AssertionStatus.NONE.equals(status)) {
                logger.log(Level.WARNING, "Audit lookup policy completed with assertion status of " + status);
            }

            return status;

        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING, "Failed to execute audit lookup policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            ResourceUtils.closeQuietly(sph);
            ResourceUtils.closeQuietly(policyContext);
        }
    }
    private static final Level[] LEVELS_IN_ORDER = { Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE };

    private void setQueryParams(AuditSearchCriteria criteria,PolicyEnforcementContext policyContext) throws FindException {
        final String prefix = "recordQuery.";
        long minTime = criteria.fromTime.getTime();
        long maxTime =  criteria.toTime==null ? System.currentTimeMillis(): criteria.toTime.getTime();

        int fromLevel = criteria.fromLevel == null ? Level.FINEST.intValue():  criteria.fromLevel.intValue();
        int toLevel = criteria.toLevel == null ?  Level.SEVERE.intValue():  criteria.toLevel.intValue();

        List<Integer> levels = new ArrayList<Integer>();
        if (fromLevel == toLevel) {
            levels.add(fromLevel);
        } else {
            if (fromLevel > toLevel)
                throw new FindException("fromLevel " + fromLevel + " is not lower in value than toLevel " + toLevel);

            for (Level level : LEVELS_IN_ORDER) {
                if (level.intValue() >= fromLevel && level.intValue() <= toLevel) {
                    levels.add(level.intValue());
                }
            }
        }

        String type;
        if(criteria.recordClass == null) {
            type = "%";
        } else if (criteria.recordClass.equals( MessageSummaryAuditRecord.class)) {
            type = AuditRecordUtils.TYPE_MESSAGE;
        } else if (criteria.recordClass.equals(  SystemAuditRecord.class)) {
            type = AuditRecordUtils.TYPE_SYSTEM;
        } else if (criteria.recordClass.equals( AdminAuditRecord.class))  {
            type = AuditRecordUtils.TYPE_ADMIN;
        } else {
            type = "%";
        }

        String nodeId = criteria.nodeId;
        String serviceName = criteria.serviceName;
        String message = criteria.message;
        String requestId = criteria.requestId;
        String userName = criteria.userName;
        String userIdOrDn = criteria.userIdOrDn;
        Integer  messageId = criteria.messageId;
//        String paramValue = criteria.paramValue;   // not currently supported, ignored
        String entityClassName = criteria.entityClassName;
        Long  entityId = criteria.entityId;
        // todo [wynne]
//        String nodeIdToStartMsg = criteria.nodeIdToStartMsg;
//        String  nodeIdToEndMsg = criteria.nodeIdToEndMsg;

        policyContext.setVariable(prefix+"minTime",minTime);
        policyContext.setVariable(prefix+"maxTime",maxTime);
        policyContext.setVariable(prefix+"levels",levels);
        policyContext.setVariable(prefix+"auditType",type);
        policyContext.setVariable(prefix+"nodeId",nodeId==null?"%":nodeId);
        policyContext.setVariable(prefix+"serviceName",serviceName==null?"%":serviceName);
        policyContext.setVariable(prefix+"userName",userName==null?"%":userName);
        policyContext.setVariable(prefix+"userIdOrDn",userIdOrDn==null?"%":userIdOrDn);
        policyContext.setVariable(prefix+"entityClassName",entityClassName==null?"%":entityClassName);
        policyContext.setVariable(prefix+"message",message==null?"%":message);
        policyContext.setVariable(prefix+"entityId",entityId==null?"%":entityId);
        policyContext.setVariable(prefix+"requestId",requestId==null?"%":requestId);

        policyContext.setVariable(prefix+"messageId",messageId);

    }
}