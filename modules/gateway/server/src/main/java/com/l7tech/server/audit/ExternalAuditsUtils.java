package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.jdbc.JdbcQueryUtils;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.CompressedStringType;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
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
    private static String[]  resolveAsObjectList  = {"audit.resZip","audit.reqZip","audit.status","audit.authenticated","audit.requestContentLength","audit.responseContentLength","audit.responseStatus","audit.routingLatency","audit.componentId"};

    static public String testSystemAuditRecord(String connectionName, String auditRecordTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
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
                -2,
                "user name",
                "user id",
                "action",
                "adminId");
        signAuditRecord(sar,defaultKey);
        context = new AuditSinkPolicyEnforcementContext(sar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query =  ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query, Arrays.asList(resolveAsObjectList),  context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof SqlRowSet && ((SqlRowSet) result).next();
        if(!success)
            return "Failed to get a system audit record";

        boolean verify = verifyRetreievedAuditRecord((SqlRowSet)result,sar.getOid(),defaultKey);

        // delete
        result =  deleteRow(jdbcQueryingManager,auditRecordTable,guid,connectionName);

        if(!verify )
            return "Audit message signature verify failed";
        return result == null? "" : result.toString();
    }

    static public String testAdminAuditRecord(String connectionName, String auditRecordTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        Object result;
        boolean success;

        // test save
        String guid = UUID.randomUUID().toString();
        AdminAuditRecord aar = new AdminAuditRecord(
                Level.FINE,
                UUID.randomUUID().toString(),
                123,
                "entityClassname",
                "name",
                AdminAuditRecord.ACTION_OTHER,
                "msg",
                -2,
                "adminLogin",
                "adminId",
                "0.0.0.0");
        signAuditRecord(aar,defaultKey);
        context = new AuditSinkPolicyEnforcementContext(aar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query =  ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof SqlRowSet && ((SqlRowSet) result).next();
        if(!success)
            return "Failed to get an admin audit record";

        boolean verify = verifyRetreievedAuditRecord((SqlRowSet)result,aar.getOid(),defaultKey);

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

    static public String testMessageSummaryRecord(String connectionName, String auditRecordTable, String auditDetailTable, JdbcQueryingManager jdbcQueryingManager, DefaultKey defaultKey) throws Exception {
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
                7, "serviceName", "operationNameHaver",
                true, SecurityTokenType.UNKNOWN, -5,
                "userName", "userId");
        signAuditRecord(record,defaultKey);

        AuditSinkPolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(record,
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response));

        String query = ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = query.replace("${record.guid}", "'"+guid+"'");
        Object result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName, query,Arrays.asList(resolveAsObjectList), context, new LoggingAudit(logger));
        success = result instanceof Integer && (Integer)result == 1;
        if(!success)
            return result.toString();

        // get data
        query = "select * from "+auditRecordTable+" where id='"+guid+"'";
        result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
        success = result instanceof SqlRowSet && ((SqlRowSet) result).next();
        if(!success)
            return "Failed to get an message summary  audit record";

        boolean verify = verifyRetreievedAuditRecord((SqlRowSet)result,record.getOid(),defaultKey);

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
        success = result instanceof SqlRowSet && ((SqlRowSet) result).next();
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
        if(result instanceof Integer && (Integer)result!= 0){
            // try to clean up audit detail
            query = "delete from "+auditDetailTable+" where id='"+guid+"' and ordinal="+detail.getOrdinal();
            result = JdbcQueryUtils.performJdbcQuery(jdbcQueryingManager,connectionName,query,Arrays.asList(resolveAsObjectList),context, new LoggingAudit(logger));
            return "Audit detail failed to delete with audit record";
        }

        success = result instanceof SqlRowSet && !((SqlRowSet) result).next();

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
                            String serviceOid ,
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
        if(type.equals(AuditRecordUtils.TYPE_ADMIN)){
            record = new AdminAuditRecord(
                    Level.parse(auditLevel),
                    nodeid,
                    Long.parseLong(entityId),
                    entityClass,
                    name,
                    action.charAt(0),
                    message,
                    Long.parseLong(providerOid),
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
            SecurityTokenType tokenType = SecurityTokenType.getByName(authenticationType);


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
                    Long.parseLong(serviceOid),
                    name,
                    operationName,
                    authenticated,
                    tokenType,
                    Long.parseLong(providerOid),
                    userName,
                    userId ) ;  //mappingValueOidHaver



        } else if(type.equals(AuditRecordUtils.TYPE_SYSTEM)){
            record = new SystemAuditRecord(
                    Level.parse(auditLevel),
                    nodeid,
                    Component.fromId(componentId),
                    message,
                    false, // alwaysAudit - not displayed, only used in flush
                    Long.parseLong(providerOid),
                    userName,
                    userId,
                    action,
                    ip_addr);
        }

        record.setSignature(signature);
        record.setMillis(time);

        return record;
    }

    private static String getDecompressedString(byte[] in){
        try {
            return new String(CompressedStringType.decompress(in));
        } catch (SQLException e) {
            return null;
        }
    }

    private static AuditRecordPropertiesHandler  parseRecordProperties(String props){
        try {
            AuditRecordPropertiesHandler handler = new AuditRecordPropertiesHandler();
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


    private static boolean verifyRetreievedAuditRecord( SqlRowSet resultSet, long originalOid,DefaultKey defaultKey) throws IOException, SignatureException, InvalidKeyException, KeyUsageException, NoSuchAlgorithmException, CertificateParsingException {

        String id = resultSet.getString("id");
        String nodeid = resultSet.getString("nodeid");
        Long time = resultSet.getLong("time");
        String type = resultSet.getString("type");
        String auditLevel = resultSet.getString("audit_level");
        String name = resultSet.getString("name");
        String message = resultSet.getString("message");
        String ip_addr = resultSet.getString("ip_address");
        String userName = resultSet.getString("user_name");
        String userId = resultSet.getString("user_id");
        String providerOid = resultSet.getString("provider_oid");
        String signature = resultSet.getString("signature");
        String entityClass = resultSet.getString("entity_class");
        String entityId = resultSet.getString("entity_id");
        Integer status = resultSet.getInt("status");
        String requestId = resultSet.getString("request_id");
        String serviceOid = resultSet.getString("service_oid");
        String operationName = resultSet.getString("operation_name");
        Boolean authenticated = resultSet.getBoolean("authenticated");
        String authenticationType = resultSet.getString("authenticationType");
        Integer requestLength = resultSet.getInt("request_length");
        Integer responseLength = resultSet.getInt("response_length");
        byte[] requestZip = (byte[])resultSet.getObject("request_xml");
        byte[] responseZip = (byte[])resultSet.getObject("response_xml");
        Integer responseStatus = resultSet.getInt("response_status");
        Integer latency = resultSet.getInt("routing_latency");
        String properties = resultSet.getString("properties");
        Integer componentId = resultSet.getInt("component_id");
        String action = resultSet.getString("action");

        AuditRecord record =
                makeAuditRecord(id,nodeid, time, type, auditLevel, name, message ,ip_addr, userName, userId, providerOid, signature,entityClass, entityId, status, requestId, serviceOid,  operationName, authenticated, authenticationType, requestLength,  responseLength, requestZip, responseZip, responseStatus, latency, componentId, action, properties);
        record.setOid(originalOid);

        SignerInfo signerInfo = defaultKey.getAuditSigningInfo();
        if (signerInfo == null) signerInfo = defaultKey.getSslInfo();

        X509Certificate cert = signerInfo.getCertificate();
        AuditRecordVerifier verifier = new AuditRecordVerifier(cert);
        return verifier.verifySignatureOfDigest(signature,record.computeSignatureDigest());
    }

}
