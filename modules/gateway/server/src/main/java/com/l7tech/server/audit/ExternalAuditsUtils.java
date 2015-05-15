package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityTypeRegistry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.jdbc.JdbcQueryUtils;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.CompressedStringType;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.security.*;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class ExternalAuditsUtils {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static String[]  resolveAsObjectList  = {"audit.resZip","audit.reqZip","audit.status","audit.authenticated","audit.savedResponseContentLength","audit.savedRequestContentLength","audit.responseStatus","audit.routingLatency","audit.componentId"};

    static public String testSystemAuditRecord(String connectionName, String dbType, String auditRecordTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        Object result;
        boolean success;
        String guid = UUID.randomUUID().toString();
        SystemAuditRecord sar = new SystemAuditRecord(
                Level.FINE,
                UUID.randomUUID().toString(),
                Component.GW_AUDIT_SYSTEM,
                "message",
                false,
                new Goid(456,123),
                "user name",
                "user id",
                "action",
                "adminId");
        signAuditRecord(sar,defaultKey);
        context = new AuditSinkPolicyEnforcementContext(sar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query =  ExternalAuditsCommonUtils.saveRecordQuery(dbType,auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query, Arrays.asList(resolveAsObjectList),  context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Map && !((Map) result).isEmpty();
        if(!success)
            return "Failed to get a system audit record";

        boolean verify = verifyRetreievedAuditRecord(result,sar.getGoid(),defaultKey);

        // delete
        result =  deleteRow(jdbcQueryingManager,auditRecordTable,guid,connectionName);

        if(!verify )
            return "Audit message signature verify failed";
        return result == null? "" : result.toString();
    }

    static public String testAdminAuditRecord(String connectionName, String dbType,String auditRecordTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        Object result;
        boolean success;

        // test save
        String guid = UUID.randomUUID().toString();
        AdminAuditRecord aar = new AdminAuditRecord(
                Level.FINE,
                UUID.randomUUID().toString(),
                new Goid(457,123),
                "entityClassname",
                "name",
                AdminAuditRecord.ACTION_OTHER,
                "msg",
                new Goid(456,123),
                "adminLogin",
                "adminId",
                "0.0.0.0");
        signAuditRecord(aar,defaultKey);
        context = new AuditSinkPolicyEnforcementContext(aar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query =  ExternalAuditsCommonUtils.saveRecordQuery(dbType,auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Map && !((Map) result).isEmpty();
        if(!success)
            return "Failed to get an admin audit record";

        boolean verify = verifyRetreievedAuditRecord(result,aar.getGoid(),defaultKey);

        // delete
        result =  deleteRow(jdbcQueryingManager,auditRecordTable,guid,connectionName);
        if(!verify )
            return "Audit message signature verify failed";
        return result == null? "" : result.toString();
    }



    static private void signAuditRecord(AuditRecord auditRecord, DefaultKey defaultKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException {
        SignerInfo signerInfo = defaultKey.getAuditSigningInfo();
        if (signerInfo == null) signerInfo = defaultKey.getSslInfo();

        PrivateKey pk = signerInfo.getPrivate();
        new AuditRecordSigner(pk).signAuditRecord(auditRecord);
    }

    static public String testMessageSummaryRecord(String connectionName, String dbType, String auditRecordTable, String auditDetailTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
        boolean success;
        // create record
        String requestXML = "<request>blah</request>";
        String responseXML = "<response>blah</response>";
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(requestXML));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(responseXML));

        String guid = UUID.randomUUID().toString();
        MessageSummaryAuditRecord record =new MessageSummaryAuditRecord(Level.WARNING, UUID.randomUUID().toString() , "requestId", AssertionStatus.NOT_APPLICABLE,
                "clientAddr",requestXML, requestXML.length(),
                responseXML, responseXML.length() , 1234, 5678,
                new Goid(0,7), "serviceName", "operationNameHaver",
                true, SecurityTokenType.UNKNOWN, new Goid(789,123),
                "userName", "userId",null);
        signAuditRecord(record,defaultKey);

        AuditSinkPolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(record,
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response));

        String query = ExternalAuditsCommonUtils.saveRecordQuery(dbType,auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        Object result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName, query,Arrays.asList(resolveAsObjectList), context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Map && !((Map) result).isEmpty();
        if(!success)
            return "Failed to get an message summary  audit record";

        boolean verify = verifyRetreievedAuditRecord(result,record.getGoid(),defaultKey);

        // save detail
        AuditDetail detail = new AuditDetail(Messages.EXCEPTION_SEVERE,new String[]{"param1","param2"}, new RuntimeException("message"));
        detail.setOrdinal(9);
        detail.setAuditGuid(guid);
        detail.setAuditRecord(record);
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);

        query = ExternalAuditsCommonUtils.saveDetailQuery(auditDetailTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        query = query.replaceAll("i.current", "audit.details.0");
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName, query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        query = "select * from "+auditDetailTable+" where audit_oid='"+guid+"' and ordinal="+detail.getOrdinal();
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Map && !((Map) result).isEmpty();
        if(!success)
            return "Failed to get an audit detail";

        // delete
        result =  deleteRow(jdbcQueryingManager,auditRecordTable,guid,connectionName);
        success = result == null;
        if(!success)
            return "Failed to delete a message summary audit record: "+result.toString();

        // check if detail is also deleted

        query = "select * from "+auditDetailTable+" where audit_oid ='"+detail.getAuditGuid()+"' " ;
        result = JdbcQueryUtils.performJdbcQuery( jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),null, new LoggingAudit(logger));
        success = result instanceof Map && ((Map) result).isEmpty();

        if(!success){
            // try to clean up audit detail
            query = "delete from "+auditDetailTable+" where id='"+guid+"' and ordinal="+detail.getOrdinal();
            result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
            return "Audit detail failed to delete with audit record";
        }


        if(!verify )
            return "Audit message signature verify failed";

        return success?"":result.toString();
    }

    // return null if successful
    static private Object deleteRow(JdbcQueryingManager jdbcQueryingManager, String table, String id, String connectionName) throws Exception {
        String query;
        Object result;
        boolean success;
        query = "delete from " + table +" where id ='" + id + "'";
        result = JdbcQueryUtils.performJdbcQuery( jdbcQueryingManager,connectionName , query, Arrays.asList(resolveAsObjectList),null , new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        return success ? null : result;
    }

    static public AuditRecord makeAuditRecord(String id,
                            String nodeid ,
                            Long time ,
                            String type ,
                            String auditLevel,
                            String name ,
                            String message ,
                            String ip_addr,
                            String userName,
                            String userId ,
                            String providerOid ,
                            String signature,
                            String entityClass ,
                            String entityId ,
                            Integer status ,
                                      String requestId ,
                            String serviceGoid ,
                            String operationName ,
                            Boolean authenticated ,
                            String authenticationType,
                            Integer requestLength ,
                            Integer responseLength ,
                            byte[] requestZip ,
                            byte[] responseZip,
                            Integer responseStatus,
                            Integer latency,
                            Integer componentId,
                                      String action,
                  String properties)
    {
        AuditRecordPropertiesHandler  propsHandler = parseRecordProperties(properties);

        AuditRecord record = null;
        userName = userName !=null? userName.length()>0? userName: null: null;
        userId = userId !=null? userId.length()>0? userId: null: null;

        if(type.equals(AuditRecordUtils.TYPE_ADMIN)){
            record = new AdminAuditRecord(
                    Level.parse(auditLevel),
                    nodeid,
                    getGoid(entityClass,entityId),
                    entityClass,
                    name,
                    action.charAt(0),
                    message,
                    getProviderId(providerOid),
                    userName,
                    userId,
                    ip_addr) ;
        } else if(type.equals(AuditRecordUtils.TYPE_MESSAGE)){

            String requestXml = requestZip==null? null: getDecompressedString(requestZip);
            String responseXml = responseZip==null? null: getDecompressedString(responseZip);
            requestXml = requestXml==null ||  requestXml.isEmpty() ? null:requestXml;
            responseXml = responseXml==null ||  responseXml.isEmpty() ? null:responseXml;
            Level level = Level.parse(auditLevel);
            AssertionStatus assStatus = AssertionStatus.fromInt(status);
            if(assStatus.getNumeric() == AssertionStatus.AUTH_FAILED.getNumeric()){
                if(  AssertionStatus.UNAUTHORIZED.getLevel().equals(level)) assStatus = AssertionStatus.UNAUTHORIZED;
                else if( AssertionStatus.AUTH_FAILED.getLevel().equals(level)) assStatus = AssertionStatus.AUTH_FAILED;
            }

            SecurityTokenType tokenType = SecurityTokenType.getByName(authenticationType);
            userName = userName !=null? userName.length()>0? userName: null: null;
            userId = userId !=null? userId.length()>0? userId: null: null;


            record = new MessageSummaryAuditRecord(
                    level,
                    nodeid,
                    requestId,
                    assStatus,
                    ip_addr,
                    requestXml,
                    requestLength,
                    responseXml,
                    responseLength,
                    responseStatus,
                    latency,
                    GoidUpgradeMapper.mapId(EntityType.SERVICE,serviceGoid),
                    name,
                    operationName,
                    authenticated,
                    tokenType,
                    getIdProviderGoid(providerOid),
                    userName,
                    userId ,
                    null ) ;  //mappingValueOidHaver



        } else if(type.equals(AuditRecordUtils.TYPE_SYSTEM)){
            record = new SystemAuditRecord(
                    Level.parse(auditLevel),
                    nodeid,
                    Component.fromId(componentId),
                    message,
                    false, // alwaysAudit - not displayed, only used in flush
                    getIdProviderGoid(providerOid),
                    userName,
                    userId,
                    action,
                    ip_addr);
        }

        record.setSignature(signature);
        record.setMillis(time);

        return record;
    }

    private static Goid getProviderId(String providerOid) {
        if(ValidationUtils.isValidLong(providerOid,false,-3L,0)){
            if(Long.parseLong(providerOid)== IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OLD_OID)
                return IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
        }
        return GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,providerOid);
    }

    private static Goid getGoid(String entityClass,String toGoid) {
        if(toGoid == null || toGoid.isEmpty() || toGoid.equals("-1"))
            return null;
        try{
            return Goid.parseGoid(toGoid);
        }catch(IllegalArgumentException e){
            // must be an old oid
            if(Long.parseLong(toGoid)<0)
                return new Goid(0,Long.parseLong(toGoid));
            EntityType type = EntityTypeRegistry.getEntityType(entityClass);
            return GoidUpgradeMapper.mapId(type,toGoid);
        }
    }

    private static Goid getIdProviderGoid(String toGoid) {
        if(toGoid == null || toGoid.isEmpty()|| toGoid.equals("-1"))
            return null;
        try{
            return Goid.parseGoid(toGoid);
        }catch(IllegalArgumentException e){
            // must be an old oid
            if(Long.parseLong(toGoid)<0)
                return new Goid(0,Long.parseLong(toGoid));
            return GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,toGoid);
        }
    }



    private static String getDecompressedString(byte[] in){
        try {
            return new String(CompressedStringType.decompress(in));
        } catch (SQLException e) {
            return null;
        }
    }

    public static byte[] getByteArrayData(Object o) throws ClassCastException {
        if(o==null)
            return null;
        if(o instanceof byte[])
            return (byte[])o;
        throw new ClassCastException("Unknown type:" + o.getClass());
    }

    public static Long getLongData(Object o)  throws ClassCastException{
        if(o==null)
            return null;
        if(o instanceof Long)
            return (Long)o;
        if(o instanceof BigDecimal)
            return ((BigDecimal) o).longValue();
        throw new ClassCastException("Unknown type:" + o.getClass());
    }

    public static String getStringData(Object o)  throws ClassCastException {
        if(o==null)
            return null;
        if(o instanceof String)
            return (String)o;
        throw new ClassCastException("Unknown type:" + o.getClass());
    }


    public static Boolean getBooleanData(Object o)  throws ClassCastException{
        if(o==null)
            return null;
        if(o instanceof Boolean)
            return (Boolean) o;
        if(o instanceof Integer)
            return (Integer)o == 1 ;
        if(o instanceof  String)
            return o.equals("1");
        throw new ClassCastException("Unknown type:" + o.getClass());
    }

    public static Integer getIntegerData(Object o)  throws ClassCastException{
        if(o==null)
            return null;
        if(o instanceof Integer)
            return (Integer)o;
        if(o instanceof BigDecimal)
            return ((BigDecimal) o).intValue();
        throw new ClassCastException("Unknown type:" + o.getClass());
    }

    private static AuditRecordPropertiesHandler  parseRecordProperties(String props){
        if(props==null)
            return null;
        try {
            AuditRecordPropertiesHandler handler = new AuditRecordPropertiesHandler();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            StringReader sr = new StringReader(props);
            xr.parse(new InputSource(sr));
            return handler;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error parsing audit record properties: ", props);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error parsing audit record properties: ", props);
        }
        return null;
    }

    private static boolean verifyRetreievedAuditRecord( Object resultSet, Goid originalGoid,DefaultKey defaultKey) throws IOException, SignatureException, InvalidKeyException, KeyUsageException, NoSuchAlgorithmException, CertificateParsingException, ClassCastException {
        Map<String,List<Object>> result = ( Map<String,List<Object>>) resultSet;

        String id = getStringData(result.get("id").get(0));
        String nodeid = getStringData(result.get("nodeid").get(0));
        Long time = getLongData(result.get("time").get(0));
        String type = getStringData(result.get("type").get(0));
        String auditLevel = getStringData(result.get("audit_level").get(0));
        String name = getStringData(result.get("name").get(0));
        String message = getStringData(result.get("message").get(0));
        String ip_addr = getStringData(result.get("ip_address").get(0));
        String userName = getStringData(result.get("user_name").get(0));
        String userId = getStringData(result.get("user_id").get(0));
        String providerOid = getStringData(result.get("provider_oid").get(0));
        String signature = getStringData(result.get("signature").get(0));
        String entityClass = getStringData(result.get("entity_class").get(0));
        String entityId = getStringData(result.get("entity_id").get(0));
        Integer status = getIntegerData(result.get("status").get(0));
        String requestId = getStringData(result.get("request_id").get(0));
        String serviceOid = getStringData(result.get("service_oid").get(0));
        String operationName = getStringData(result.get("operation_name").get(0));
        Boolean authenticated = getBooleanData(result.get("authenticated").get(0));
        String authenticationType = getStringData(result.get("authenticationtype").get(0));
        Integer requestLength = getIntegerData(result.get("request_length").get(0));
        Integer responseLength = getIntegerData(result.get("response_length").get(0));
        byte[] requestZip = getByteArrayData(result.get("request_xml").get(0));
        byte[] responseZip = getByteArrayData(result.get("response_xml").get(0));
        Integer responseStatus = getIntegerData(result.get("response_status").get(0));
        Integer latency = getIntegerData(result.get("routing_latency").get(0));
        String properties = getStringData(result.get("properties").get(0));
        Integer componentId = getIntegerData(result.get("component_id").get(0));
        String action = getStringData(result.get("action").get(0));

        AuditRecord record =
                makeAuditRecord(id,nodeid, time, type, auditLevel, name, message ,ip_addr, userName, userId, providerOid, signature,entityClass, entityId, status, requestId, serviceOid,  operationName, authenticated, authenticationType, requestLength,  responseLength, requestZip, responseZip, responseStatus, latency, componentId, action, properties);
        record.setGoid(originalGoid);

        SignerInfo signerInfo = defaultKey.getAuditSigningInfo();
        if (signerInfo == null) signerInfo = defaultKey.getSslInfo();

        X509Certificate cert = signerInfo.getCertificate();
        AuditRecordVerifier verifier = new AuditRecordVerifier(cert);
        return verifier.verifySignatureOfDigest(signature,record.computeSignatureDigest());
    }

    public static AuditDetail makeAuditDetail(int index, String audit_oid, Long time, Integer componentId,Integer ordinal,Integer messageId,String message,String properties ) {
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
        detail.setGoid(new Goid(0,index));

        return detail;
    }

    private static  AuditDetailPropertiesHandler  parseDetailsProperties(String props){
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
}
