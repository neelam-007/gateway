/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.ddtek.jdbc.extensions.ExtEmbeddedConnection;
import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.util.CompressedStringType;
import com.l7tech.util.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.security.*;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import static junit.framework.Assert.assertTrue;

@Ignore
public class AuditSinkSchemaTest {

    private AuditRecordSigner signer;
    private AuditRecordVerifier verifier;

    private String url = "jdbc:l7tech:sqlserver://127.0.0.1:57710;databaseName=audit";
    private String username = "sa";
    private String password = "7layer";
    private String auditRecordTable = "audit_main";
    private String auditDetailTable = "audit_detail";
    private boolean cleanup = false;

    private static final String SAVE_DETAIL_QUERY = "insert into ${auditDetailTable}(id, audit_oid,time,component_id,ordinal,message_id,exception_message,properties) values " +
            "(${i.current.id},${i.current.auditId},${i.current.time},${i.current.componentId},${i.current.ordinal},${i.current.messageId},${i.current.exception},${i.current.properties});";

    private static final String SAVE_RECORD_QUERY = "insert into ${auditRecordTable}(id,nodeid,time,type,audit_level,name,message,ip_address,user_name,user_id,provider_oid,signature,properties," +
            "entity_class,entity_id," +
            "status,request_id,service_oid,operation_name,authenticated,authenticationType,request_length,response_length,request_xml,response_xml,response_status,routing_latency," +
            "component_id,action)" +
            " values " +
            "(${audit.id},${audit.nodeId},${audit.time},${audit.type},${audit.level},${audit.message},${audit.name},${audit.ipAddress},${audit.user.name},${audit.user.id},${audit.user.idProv},${audit.signature},${audit.properties}," +
            "${audit.entity.class},${audit.entity.oid}," +
            "${audit.status},${audit.requestId},${audit.serviceOid},${audit.operationName},${audit.authenticated},${audit.authTypeNum},${audit.request.size},${audit.response.size},${audit.request.mainpart},${audit.response.mainpart},${audit.responseStatus},${audit.latency}," +
            "${audit.componentId},${audit.action});";

    @Before
    public void setUp() throws Exception {
        Pair<X509Certificate, PrivateKey> key =  TestKeys.getCertAndKey("RSA_2048");
        signer =  new AuditRecordSigner(key.right);
        verifier = new AuditRecordVerifier(key.left);
    }

    @Test
    public void testAuditSinkSchema() throws Exception{
        boolean success = true;

        AuditRecord record = testMessageSummaryRecord();
        deleteRow(auditRecordTable,record.getGuid());
        record = testAdminAuditRecord();
        deleteRow(auditRecordTable,record.getGuid());
        record = testSystemAuditRecord();
        AuditDetail detail = testAuditDetail(record);
        deleteRow(auditRecordTable,record.getGuid());
        checkAuditDetail(detail);
    }

    private void checkAuditDetail(AuditDetail detail) throws Exception {
        if(cleanup){
            String query = "select * from "+auditDetailTable+" where id ='"+detail.getGuid()+"'";
            int result = performJdbcQuery( query);
            if(result != 0)
                deleteRow(auditDetailTable,detail.getGuid());
            assertTrue("Audit detail not deleted with corresponding audit record", result == 0  );
        }
    }

    private AuditDetail testAuditDetail(AuditRecord record) throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        int result;
        boolean success;AuditDetail detail = new AuditDetail(Messages.EXCEPTION_SEVERE,new String[]{"param1","param2"}, new RuntimeException("message"));
        detail.setOrdinal(9);
        detail.setAuditGuid(record.getGuid());
        detail.setAuditRecord(record);
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        context = new AuditSinkPolicyEnforcementContext(record, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query = SAVE_DETAIL_QUERY;
        query = query.replaceAll("i.current", "audit.details.0");
        query = query.replace("${auditDetailTable}",auditDetailTable);

        query = resolveContextVariables(context, query);

        result = performJdbcQuery( query);
        success = result==1;
        assertTrue("Failed to save audit detail", success);
        return detail;
    }

    private AuditRecord testSystemAuditRecord() throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        int result;
        boolean success;
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
        signAuditRecord(sar);
        context = new AuditSinkPolicyEnforcementContext(sar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query = SAVE_RECORD_QUERY;
        query = query.replace("${auditRecordTable}", auditRecordTable);
        query = resolveContextVariables(context, query);
        result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save system audit record", success);
        return sar;
    }

    private AuditRecord testAdminAuditRecord() throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        int result;
        boolean success;
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
        signAuditRecord(aar);
        context = new AuditSinkPolicyEnforcementContext(aar, PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,null);
        query = SAVE_RECORD_QUERY;
        query = query.replace("${auditRecordTable}", auditRecordTable);
        query = resolveContextVariables(context, query);
        result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save admin audit record", success);
        return aar;
    }

    private AuditRecord testMessageSummaryRecord() throws Exception {
        boolean success;
        MessageSummaryAuditRecord record = new MessageSummaryAuditRecord(Level.WARNING, UUID.randomUUID().toString() , "requestId", AssertionStatus.NOT_APPLICABLE,
                "clientAddr", "requestXml", 5,
                "responseXml", 6 , 1234, 5678,
                7, "serviceName", "operationNameHaver",
                true, SecurityTokenType.UNKNOWN, -5,
                "userName", "userId", -10);
        signAuditRecord(record);

        AuditSinkPolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(record,  PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,makeContext("<myrequest/>", "<myresponse/>"));
        String query = SAVE_RECORD_QUERY;
        query = query.replace("${auditRecordTable}",auditRecordTable);
        query = resolveContextVariables(context, query);
        int result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save message audit record", success);
        return record;
    }

    private void deleteRow(String table, String id) throws Exception {

        if(cleanup){
            String query;
            int result;
            boolean success;
            query = "delete from " + table +" where id ='" + id + "'";
            result = performJdbcQuery( query );
            success = result==1;
            assertTrue("Delete fail: "+query,success);
        }
    }

    // return number of rows retrieved or number of rows affected
    private int performJdbcQuery( String query) throws Exception {
        Connection conn = null;
        Statement statement = null;
        int results = -1;
        try{
            conn = DriverManager.getConnection(url, username, password);
            if (conn instanceof ExtEmbeddedConnection) {
                ExtEmbeddedConnection embeddedCon = (ExtEmbeddedConnection)conn;
                boolean unlocked = embeddedCon.unlock("Layer7!@Tech#$");
                assertTrue("Unable to unlock Datadirect JDBC drivers",unlocked);
            }

            statement = conn.createStatement();
            boolean isSelectQuery = query.toLowerCase().startsWith("select");
            if(isSelectQuery){
                ResultSet resultSet = statement.executeQuery(query);  
                int i =0;
                while(resultSet.next()){
                    ++i;
                }
                results = i;
            }
            else{
                results = statement.executeUpdate(query);
            }
        } finally {
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(conn);
        }

        return results;
    }

    private void signAuditRecord(AuditRecord auditRecord) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {

        signer.signAuditRecord(auditRecord);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private String resolveContextVariables(AuditSinkPolicyEnforcementContext context, String query) throws NoSuchVariableException {
        String[] vars = Syntax.getReferencedNames(query);
        for(String varRef : vars){
            Object var = ServerVariables.get(varRef, context);
            String varString = "null";
            if(var instanceof String)
                varString = "'"+var.toString()+"'";
            else if (var instanceof Boolean)
                varString = (Boolean)var ? "1":"0";
            else if (var !=null)
                varString = var.toString();

            query = query.replace("${"+varRef+"}",varString);
        }
        return query;
    }

    private List<AuditDetailMessage> messageList = Arrays.asList(
            MessagesUtil.getAuditDetailMessageById(2200),
            MessagesUtil.getAuditDetailMessageById(-5),
            MessagesUtil.getAuditDetailMessageById(7245),
            MessagesUtil.getAuditDetailMessageById(3007),
            MessagesUtil.getAuditDetailMessageById(3214),
            MessagesUtil.getAuditDetailMessageById(-1));

}
