package com.l7tech.server.audit;

import com.l7tech.common.io.WhirlycacheFactory;
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
import com.l7tech.util.*;
import com.whirlycott.cache.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the audit lookup policy to retrieve audits.
 * <p/>
 */
public class AuditLookupPolicyEvaluator implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(AuditLookupPolicyEvaluator.class.getName());

    private final Config config;
    private ValidatedConfig validatedConfig;
    private final PolicyCache policyCache;
    private final Cache auditRecordsCache;  // guid -> audit record
    private final Cache auditRecordsIDCache; // guid -> oid for retrieved audit record
//    private final AuditDetailPropertiesDomUnmarshaller detailUnmarshaller = new AuditDetailPropertiesDomUnmarshaller();
    private static final AtomicLong nextFakeOid = new AtomicLong(100);
    private long messageLimitSize;

    public AuditLookupPolicyEvaluator(Config config, PolicyCache policyCache) {
        this.config = config;
        validatedConfig = new ValidatedConfig( config, logger);
        this.policyCache = policyCache;
        this.auditRecordsCache =
                WhirlycacheFactory.createCache("AuditLookupPolicyCache", 10000, 120, WhirlycacheFactory.POLICY_LRU);
        this.auditRecordsIDCache =
                WhirlycacheFactory.createCache("AuditLookupPolicyIDCache", 100000, 120, WhirlycacheFactory.POLICY_LRU);

        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE, 0);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE, Long.MAX_VALUE);
        this.messageLimitSize = this.validatedConfig.getLongProperty( ServerConfigParams.PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE, 10485760);  // 10MB

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE)) {
            this.messageLimitSize = this.validatedConfig.getLongProperty( ServerConfigParams.PARAM_AUDIT_LOOKUP_CACHE_MESSAGE_LIMIT_SIZE, 10485760);
        }
    }

    private String loadAuditSinkPolicyGuid() {
        return config.getProperty( ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID );
    }

    /**
     * Find {@link com.l7tech.gateway.common.audit.AuditRecordHeader}s that match the given criteria.
     * @param criteria criteria The search settings (must not be null)
     * @return The collection of audit records (not null)
     * @throws FindException If an error occurs
     */
    @NotNull
    public List<AuditRecordHeader> findHeaders(@NotNull final AuditSearchCriteria criteria) throws FindException{
        try {
            // Make sure a logging-only audit context is active while running the audit sink policy, so we don't
            // try to add details to the record that is currently being flushed via this very policy
            final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);

            AssertionStatus assertionStatus = AuditContextFactory.doWithCustomAuditContext(AuditContextFactory.createLogOnlyAuditContext(), new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    return executeAuditLookupPolicy(new AuditLookupSearchCriteria(criteria), context);
                }
            });

            if(assertionStatus == null){
                return Collections.emptyList();
            }

            if(assertionStatus != AssertionStatus.NONE){
                throw new FindException("Audit Lookup Policy Failed");
            }
            List<AuditRecordHeader> recordHeaders = makeAuditRecordHeaders(context);
            return recordHeaders;



        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit lookup policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new FindException("Failed to execute audit lookup policy",e);
        }
    }


    /**
     *
     * @param context policy context containing the query results
     * @return a map of audit records with messages too large to save in the cache
     */
    private Map<String , AuditRecord> makeAuditRecords(PolicyEnforcementContext context){
        Map<String , AuditRecord> largeMessageAudits = new HashMap <String , AuditRecord>();
        try {

            final String prefix = "recordQuery";

            Object sizeObj = context.getVariable(prefix + ".queryresult.count");
            List<AuditRecord> records = new ArrayList<AuditRecord> ();
            if((Integer)sizeObj == 0){
                return largeMessageAudits;
            }

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
                String id = ExternalAuditsUtils.getStringData(id_var[i]);
                String nodeid = ExternalAuditsUtils.getStringData(nodeid_var[i]);
                Long time = ExternalAuditsUtils.getLongData(time_var[i]);
                String type = ExternalAuditsUtils.getStringData(type_var[i]);
                String auditLevel = ExternalAuditsUtils.getStringData(auditLevel_var[i]);
                String name = ExternalAuditsUtils.getStringData(name_var[i]);
                String message = ExternalAuditsUtils.getStringData(message_var[i]);
                String ip_addr = ExternalAuditsUtils.getStringData(ip_addr_var[i]);
                String userName = ExternalAuditsUtils.getStringData(userName_var[i]);
                String userId = ExternalAuditsUtils.getStringData(userId_var[i]);
                String providerOid = ExternalAuditsUtils.getStringData(providerOid_var[i]);
                String signature = ExternalAuditsUtils.getStringData(signature_var[i]);
                String entityClass = ExternalAuditsUtils.getStringData(entityClass_var[i]);
                String entityId = ExternalAuditsUtils.getStringData(entityId_var[i]);
                Integer status = ExternalAuditsUtils.getIntegerData(status_var[i]);
                String requestId = ExternalAuditsUtils.getStringData(requestId_var[i]);
                String serviceOid = ExternalAuditsUtils.getStringData(serviceOid_var[i]);
                String operationName = ExternalAuditsUtils.getStringData(operationName_var[i]);
                Boolean authenticated =  ExternalAuditsUtils.getBooleanData(authenticated_var[i]);
                String authenticationType = ExternalAuditsUtils.getStringData(authenticationType_var[i]);
                Integer requestLength = ExternalAuditsUtils.getIntegerData(requestLength_var[i]);
                Integer responseLength = ExternalAuditsUtils.getIntegerData(responseLength_var[i]);
                byte[] requestZip = ExternalAuditsUtils.getByteArrayData(requestZip_var[i]);
                byte[] responseZip =  ExternalAuditsUtils.getByteArrayData(responseZip_var[i]);
                Integer responseStatus = ExternalAuditsUtils.getIntegerData(responseStatus_var[i]);
                Integer latency = ExternalAuditsUtils.getIntegerData(latency_var[i]);
                String properties = ExternalAuditsUtils.getStringData(properties_var[i]);
                Integer componentId = ExternalAuditsUtils.getIntegerData(componentId_var[i]);
                String action = ExternalAuditsUtils.getStringData(action_var[i]);

                AuditRecord record = (AuditRecord) auditRecordsCache.retrieve(id) ;
                if(record == null){
                    // response length may be null
                    if(responseLength == null)
                        responseLength = -1;
                    if(requestLength == null)
                        requestLength = -1;

                    record = ExternalAuditsUtils.makeAuditRecord(
                            id,nodeid,time,type,auditLevel,name,message,ip_addr,userName,userId,providerOid,signature,
                            entityClass,entityId,status,requestId,serviceOid,operationName,authenticated,authenticationType,
                            requestLength,responseLength,requestZip,responseZip,responseStatus,latency,componentId,action,properties);

                    Long oid = (Long)auditRecordsIDCache.retrieve(id);
                    record.setOid(oid== null? nextFakeOid.incrementAndGet():oid);
                    if(messageLimitSize > 0L && (requestLength > messageLimitSize || responseLength > messageLimitSize)){
                        auditRecordsIDCache.store(id,oid);
                        largeMessageAudits.put(id, record);
                    }else{
                        auditRecordsCache.store(id,record);
                    }
                }

                records.add(record);
            }
            makeAuditDetails(context,largeMessageAudits);

        } catch (NoSuchVariableException e) {
            logger.warning("Error creating audit records, some fields not present: "+e.getMessage());
        } catch (ClassCastException e){
            logger.warning("Error creating audit records, field type mismatch: "+e.getMessage());
        }

        return largeMessageAudits;

    }
        

    private List<AuditRecordHeader> makeAuditRecordHeaders(PolicyEnforcementContext context){
        try {

            final String prefix = "recordQuery";

            Object sizeObj = context.getVariable(prefix + ".queryresult.count");
            List<AuditRecordHeader> recordHeaders = new ArrayList<AuditRecordHeader> ();
            if((Integer)sizeObj == 0){
                return new ArrayList<AuditRecordHeader>();
            }

            Object[] id_var = (Object[])context.getVariable(prefix + ".id");
            Object[] nodeid_var = (Object[])context.getVariable(prefix+".nodeid");
            Object[] time_var = (Object[])context.getVariable(prefix+".time");
            Object[] auditLevel_var = (Object[])context.getVariable(prefix+".audit_level");
            Object[] name_var = (Object[])context.getVariable(prefix+".name");
            Object[] message_var = (Object[])context.getVariable(prefix+".message");
            Object[] signature_var = (Object[])context.getVariable(prefix+".signature");
            Object[] type_var = (Object[])context.getVariable(prefix+".type");

            for(int i = 0 ; i < (Integer)sizeObj ; ++i){
                String id = ExternalAuditsUtils.getStringData(id_var[i]);
                ExternalAuditRecordHeader header;

                // try getting id from audit records
                AuditRecord record = getAuditRecordFromCache(id,null);
                Long oid ;
                if(record == null ){
                    Object get  = auditRecordsIDCache.retrieve(id);
                    if(get == null){
                        oid = nextFakeOid.incrementAndGet();
                        // save guid -> fake id mapping
                        auditRecordsIDCache.store(id,oid);
                    }else {
                        oid = (Long)get;
                    }

                    String nodeid = ExternalAuditsUtils.getStringData(nodeid_var[i]);
                    Long time = ExternalAuditsUtils.getLongData(time_var[i]);
                    String auditLevel = ExternalAuditsUtils.getStringData(auditLevel_var[i]);
                    String name = ExternalAuditsUtils.getStringData(name_var[i]);
                    String message = ExternalAuditsUtils.getStringData(message_var[i]);
                    String signature = ExternalAuditsUtils.getStringData(signature_var[i]);
                    String type = ExternalAuditsUtils.getStringData(type_var[i]);

                    header = new ExternalAuditRecordHeader(
                            id,
                            type,
                            oid,
                            type.equals("message")? name : "",
                            message,
                            null,
                            signature==null || signature.isEmpty()? null: signature,
                            nodeid,
                            time,
                            Level.parse(auditLevel),
                            0);

                }else{
                    header = new ExternalAuditRecordHeader(record,id);


                }
                recordHeaders.add(header);
            }
            return recordHeaders;

        } catch (NoSuchVariableException e) {
            logger.warning("Error creating audit records, some fields not present: "+e.getMessage());
        } catch (ClassCastException e){
            logger.warning("Error creating audit records, field type mismatch: "+e.getMessage());
        }

        return new ArrayList<AuditRecordHeader>();
    }

    private void makeAuditDetails(PolicyEnforcementContext context, Map<String , AuditRecord> largeAuditMessages) {
        String prefix = "detailQuery";
        Integer numDetails = 0;
        try {
            Object sizeObj = context.getVariable(prefix+".queryresult.count");
            numDetails = (Integer)sizeObj;
            // no audit details to get
            if(numDetails == 0)
                return;
        } catch (NoSuchVariableException e) {
            // do nothing, no audit details to get
            return;
        }

        try{
            Object[] audit_oid_var = (Object[])context.getVariable(prefix+".audit_oid");
            Object[] time_var = (Object[])context.getVariable(prefix+".time");
            Object[] componentId_var = (Object[])context.getVariable(prefix+".component_id");
            Object[] ordinal_var = (Object[])context.getVariable(prefix+".ordinal");
            Object[] messageId_var = (Object[])context.getVariable(prefix+".message_id");
            Object[] message_var = (Object[])context.getVariable(prefix+".exception_message");
            Object[] props_var = (Object[])context.getVariable(prefix+".properties");

            for(int i = 0 ; i < numDetails ; ++i){
                String audit_oid = ExternalAuditsUtils.getStringData(audit_oid_var[i]);
                Long time = ExternalAuditsUtils.getLongData(time_var[i]);
                Integer componentId = ExternalAuditsUtils.getIntegerData(componentId_var[i]);
                Integer ordinal = ExternalAuditsUtils.getIntegerData(ordinal_var[i]);
                Integer messageId = ExternalAuditsUtils.getIntegerData(messageId_var[i]);
                String message = ExternalAuditsUtils.getStringData(message_var[i]);
                String properties = ExternalAuditsUtils.getStringData(props_var[i]);

                AuditDetailPropertiesHandler handler =   parseDetailsProperties(properties);

                String[] params = (handler == null)? null : handler.getParameters();
                AuditDetail detail = new AuditDetail();
                detail.setMessageId(messageId);
                detail.setParams(params);
                detail.setTime(time);
                detail.setException(message);

                detail.setAuditGuid(audit_oid);
                detail.setOrdinal(ordinal);
                detail.setComponentId(componentId);
                detail.setOid(ordinal);

                AuditRecord record = getAuditRecordFromCache(audit_oid,largeAuditMessages);
                if(record == null){
                    logger.warning("Error creating audit record id: "+audit_oid);
                    continue;
                }
                detail.setAuditRecord(record);
                Set<AuditDetail> details = record.getDetails();
                details.add(detail);
                record.setDetails(details);
            }
        } catch (NoSuchVariableException e) {
            logger.warning("Failed to retrieve audit details");
        } catch (ClassCastException e){
            logger.warning("Error creating audit records, field type mismatch: "+e.getMessage());
        }
    }

    public AuditRecord findByGuid(final String guid) throws FindException{
        AuditRecord record = getAuditRecordFromCache(guid,null);
        if(record !=null)
            return record;

        try {
            // Make sure a logging-only audit context is active while running the audit sink policy, so we don't
            // try to add details to the record that is currently being flushed via this very policy
            final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);

            AssertionStatus assertionStatus = AuditContextFactory.doWithCustomAuditContext(AuditContextFactory.createLogOnlyAuditContext(), new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    return executeAuditLookupPolicy(new AuditLookupSearchCriteria(new String[]{guid},null), context);
                }
            });

            if(assertionStatus == null){
                return null;
            }

            if(assertionStatus != AssertionStatus.NONE){
                throw new FindException("Audit Lookup Policy Failed");
            }
            Map<String , AuditRecord> largeAudits = makeAuditRecords(context);
            record = getAuditRecordFromCache(guid,largeAudits);
            return record;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit lookup policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new FindException("Failed to execute audit lookup policy",e);
        }
    }


    private AuditDetailPropertiesHandler  parseDetailsProperties(String props){
        try {
            AuditDetailPropertiesHandler handler = new AuditDetailPropertiesHandler();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            StringReader sr = new StringReader(props);
            xr.parse(new InputSource(sr));
            return handler;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error parsing audit detail properties: ", props);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error parsing audit detail properties: ", props);
        }
        return null;
    }

    /**
     * Run the current audit lookup policy to retrieve audits.
     *
     * @param criteria  the audit record search criteria
     * @param   policyContext  context for the lookup policy to use
     * @return  null if policy does not exsist, policy assertion status if policy exsists
     */
    private AssertionStatus executeAuditLookupPolicy(AuditLookupSearchCriteria criteria, PolicyEnforcementContext policyContext) {
        ServerPolicyHandle sph = null;
        try {
            final String guid = loadAuditSinkPolicyGuid();
            if (guid == null || guid.trim().length() < 1) {
                logger.log(Level.FINEST, "No audit lookup policy is configured");
                return null;
            }

            final AuditLookupPolicyEnforcementContext lookupContext = new AuditLookupPolicyEnforcementContext(criteria,policyContext);
            lookupContext.setAuditLevel(Level.INFO);

            // Use fake service
            final PublishedService svc = new PublishedService();
            svc.setName("[Internal audit lookup policy pseudo-service]");
            svc.setSoap(false);
            lookupContext.setService(svc);

            sph = policyCache.getServerPolicy(guid);
            if (sph == null) {
                logger.log(Level.WARNING, "Unable to access configured audit lookup policy -- no policy with GUID {0} is present in policy cache (invalid policy?)", guid);
                return AssertionStatus.SERVER_ERROR;
            }

            AssertionStatus status = sph.checkRequest(lookupContext);

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
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } finally {
            ResourceUtils.closeQuietly(sph);
            ResourceUtils.closeQuietly(policyContext);
        }
    }

    public Map<String, byte[]> getDigestForAuditRecords(Collection<String> auditRecordIds) throws FindException  {
        final Map<String, byte[]> returnMap = new HashMap<String, byte[]>();

        final int maxRecords = validatedConfig.getIntProperty( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 100);
        final List<String> auditsToRetrieve = new ArrayList<String>();
        for(String guid: auditRecordIds){
            AuditRecord record = getAuditRecordFromCache(guid,null);
            if(record !=null && record.getSignature()!=null)
                returnMap.put(guid, record.computeSignatureDigest());
            else if(auditsToRetrieve.size()< maxRecords)
                auditsToRetrieve.add(guid);
        }

        if(auditsToRetrieve.isEmpty())
            return returnMap;

        final Long maxMessageSize =  validatedConfig.getLongProperty( ServerConfigParams.PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE, 2621440L);


        try {
            // Make sure a logging-only audit context is active while running the audit sink policy, so we don't
            // try to add details to the record that is currently being flushed via this very policy
            final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);

            AssertionStatus assertionStatus = AuditContextFactory.doWithCustomAuditContext(AuditContextFactory.createLogOnlyAuditContext(), new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    return executeAuditLookupPolicy(new AuditLookupSearchCriteria(auditsToRetrieve.toArray(new String[auditsToRetrieve.size()]),maxMessageSize), context);
                }
            });

            if(assertionStatus == null){
                return returnMap;
            }

            if(assertionStatus != AssertionStatus.NONE){
                throw new FindException("Audit Lookup Policy Failed");
            }
            makeAuditRecords(context);

            for(String guid: auditsToRetrieve){
                AuditRecord record = getAuditRecordFromCache(guid,null);
                if(record !=null && record.getSignature()!=null){
                    returnMap.put(guid,record.computeSignatureDigest());
                }
            }

            return returnMap;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit lookup policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new FindException("Failed to execute audit lookup policy",e);
        }
    }

    private AuditRecord getAuditRecordFromCache(String guid, @Nullable Map<String , AuditRecord> otherSource){
        Object get = auditRecordsCache.retrieve(guid);
        if(otherSource!= null && get == null){
            return otherSource.get(guid);
        }
        return (AuditRecord)get;
    }

}