/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
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
import com.l7tech.util.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.security.*;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

import static junit.framework.Assert.assertTrue;

@Ignore
public class AuditSinkSchemaTest {

    private AuditRecordSigner signer;
    private AuditRecordVerifier verifier;

    private String url = "jdbc:datadirect:oracle://qaoracledb.l7tech.com:1521;Database=XE";
    private String username = "TEST";
    private String password = "7layer";
    private String auditRecordTable = "wyn_main";
    private String auditDetailTable = "wyn_detail";
    private boolean cleanup = false;

    @Before
    public void setUp() throws Exception {
        Pair<X509Certificate, PrivateKey> key =  TestKeys.getCertAndKey("RSA_2048");
        signer =  new AuditRecordSigner(key.right);
        verifier = new AuditRecordVerifier(key.left);
    }

    @Test
    public void testAuditSinkSchema() throws Exception{
        String guid = testMessageSummaryRecord();
        deleteRow(auditRecordTable,guid);
        guid = testAdminAuditRecord();
        deleteRow(auditRecordTable,guid);
        guid = testSystemAuditRecord();
        deleteRow(auditRecordTable,guid);
    }

    private String testSystemAuditRecord() throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        int result;
        boolean success;
        String guid = UUID.randomUUID().toString();
        SystemAuditRecord sar = new SystemAuditRecord(
                Level.FINE,
                guid,
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
        query = ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = resolveContextVariables(context, query);
        result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save system audit record", success);
        return guid;
    }

    private String testAdminAuditRecord() throws Exception {
        AuditSinkPolicyEnforcementContext context;
        String query;
        int result;
        boolean success;
        String guid = UUID.randomUUID().toString();
        AdminAuditRecord aar = new AdminAuditRecord(
                Level.FINE,
                guid,
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
        query = ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = query.replace("${auditRecordTable}", auditRecordTable);
        query = resolveContextVariables(context, query);
        result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save admin audit record", success);
        return guid;
    }

    private String testMessageSummaryRecord() throws Exception {
        boolean success;
        String guid = UUID.randomUUID().toString();
        MessageSummaryAuditRecord record = new MessageSummaryAuditRecord(Level.WARNING, UUID.randomUUID().toString() , "requestId", AssertionStatus.NOT_APPLICABLE,
                "clientAddr", "requestXml", 5,
                "responseXml", 6 , 1234, 5678,
                7, "serviceName", "operationNameHaver",
                true, SecurityTokenType.UNKNOWN, -5,
                "userName", "userId", -10);
        signAuditRecord(record);

        AuditSinkPolicyEnforcementContext context = new AuditSinkPolicyEnforcementContext(record,  PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null) ,makeContext("<myrequest/>", "<myresponse/>"));
        String query = ExternalAuditsCommonUtils.saveRecordQuery(auditRecordTable);
        query = query.replace("${auditRecordTable}",auditRecordTable);
        query = query.replace("${record.guid}",guid);
        query = resolveContextVariables(context, query);
        int result = performJdbcQuery(query);
        success = result==1;
        assertTrue("Failed to save message audit record", success);
        return guid;
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
}
