package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.ExternalAuditsCommonUtils;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.PolicyUtils;
import org.junit.Test;
import org.w3c.dom.*;

import java.util.List;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;
import static com.l7tech.server.policy.bundle.PolicyUtils.findJdbcReferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * And audit implementation for use in tests
 */
public class ExternalAuditPoliciesTest {
    private final String connectionName = "connection";
    private final String connectionDriverClass = "mysql";
    private final String auditRecordTable = "audit_main";
    private final String auditDetailTable = "audit_detail";
    private final String queryTimeoutVarName = "queryTimeout";
    private final int queryTimeoutValue = 300;
    private final WspReader wspReader;
    {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
    }

    @Test
    public void testCreateAuditSinkPolicy() throws Exception {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", null, false);
        String theXml = getAuditSinkXML();
        policy.setXml(theXml);
    }

    @Test
    public void testCreateAuditLookupPolicy() throws Exception {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", null, false);
        String theXml = getAuditLookupXML();
        policy.setXml(theXml);
        Assertion assertionPolicy = wspReader.parsePermissively( theXml, INCLUDE_DISABLED);
    }

    @Test
    public void allJdbcReferencesConnection() throws Exception {
        checkJdbcReferencesConnection(getAuditSinkXML(),connectionName);
        checkJdbcReferencesConnection(getAuditLookupXML(),connectionName);
    }

    @Test
    public void testTimeoutReferences() throws Exception {
        allJdbcQueryTimeoutReferencesVariable(getAuditSinkXML(), queryTimeoutVarName);
        allJdbcQueryTimeoutReferencesVariable(getAuditLookupXML(), queryTimeoutVarName);

        checkReferencesVariableValue(getAuditSinkXML(), queryTimeoutVarName, queryTimeoutValue);
        checkReferencesVariableValue(getAuditLookupXML(), queryTimeoutVarName, queryTimeoutValue);
    }

    private String getAuditSinkXML(){
        return ExternalAuditsCommonUtils.makeDefaultAuditSinkPolicyXml(connectionName, auditRecordTable, auditDetailTable);
    }

    private String getAuditLookupXML(){
        return ExternalAuditsCommonUtils.makeDefaultAuditLookupPolicyXml(connectionName, connectionDriverClass, auditRecordTable, auditDetailTable);
    }

    private void checkJdbcReferencesConnection(String theXml, String connectionName) throws Exception {
        Document doc = XmlUtil.parse(theXml);
        final List<Element> jdbcReferences = findJdbcReferences(doc.getDocumentElement());

        assertFalse("Jdbc Queries not found", jdbcReferences.isEmpty());
        for (Element jdbcReference : jdbcReferences) {
            final Element connNameElm = XmlUtil.findExactlyOneChildElementByName(jdbcReference, BundleUtils.L7_NS_POLICY, "ConnectionName");
            final String connNameVal = connNameElm.getAttribute("stringValue").trim();
            assertEquals("Unexpected connection",connNameVal,connectionName);
        }
    }

    protected void allJdbcQueryTimeoutReferencesVariable(String theXml, String contextVarName) throws Exception {
        Document doc = XmlUtil.parse(theXml);
        final List<Element> jdbcReferences = findJdbcReferences(doc.getDocumentElement());
        assertFalse("Jdbc Queries not found", jdbcReferences.isEmpty());
        for (Element jdbcReference : jdbcReferences) {
            final Element queryTimeoutElm = XmlUtil.findExactlyOneChildElementByName(jdbcReference, BundleUtils.L7_NS_POLICY, "QueryTimeout");
            final String queryTimeoutVal = queryTimeoutElm.getAttribute("stringValue").trim();
            assertFalse("Query timeout not set", queryTimeoutVal!=null && queryTimeoutVal.isEmpty());
            assertEquals("Unexpected context variable name",queryTimeoutVal,"${"+contextVarName+"}");
        }
    }

    protected void checkReferencesVariableValue(String theXml, String contextVarName, int value)throws Exception {
        Document doc = XmlUtil.parse(theXml);
        final List<Element> contextVariables = PolicyUtils.findContextVariables(doc.getDocumentElement());

        for(Element contextVar: contextVariables){
            final Element variableToSet = XmlUtil.findFirstChildElementByName(contextVar, "http://www.layer7tech.com/ws/policy", "VariableToSet");
            final String varName = variableToSet.getAttribute("stringValue");
            if(varName.equals(contextVarName)){
                final Element expression = XmlUtil.findFirstChildElementByName(contextVar, "http://www.layer7tech.com/ws/policy", "Expression");
                final String varValue = expression.getAttribute("stringValue");
                assertEquals("Unexpected context variable value",varValue,Integer.toString(value));
                return;
            }
        }
        assertTrue("Context variable not defined",false);
    }
}
