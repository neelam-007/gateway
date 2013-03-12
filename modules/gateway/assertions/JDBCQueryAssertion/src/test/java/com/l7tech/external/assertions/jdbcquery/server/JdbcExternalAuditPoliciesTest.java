package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.ExternalAuditPoliciesTest;
import com.l7tech.gateway.common.audit.ExternalAuditsCommonUtils;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;
import static org.junit.Assert.*;

/**
 * And audit implementation for use in tests
 */
public class JdbcExternalAuditPoliciesTest {
    private final String connectionName = "connection";
    private final String connectionDriverClass = "oracle";
    private final String auditRecordTable = "audit_main";
    private final String auditDetailTable = "audit_detail";
    private final String queryTimeoutVarName = "queryTimeout";
    private final int queryTimeoutValue = 300;

    private final WspReader wspReader;
    {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        tmf.registerAssertion(JdbcQueryAssertion.class);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
    }

    @Test
    public void testAuditSinkPolicyJdbcValues() throws Exception {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", null, false);
        String theXml = ExternalAuditsCommonUtils.makeDefaultAuditSinkPolicyXml(connectionName,auditRecordTable,auditDetailTable);
        policy.setXml(theXml);
        Assertion assertionPolicy =  wspReader.parsePermissively( theXml, INCLUDE_DISABLED);

        allJdbcQueryReferencesVariable(assertionPolicy, queryTimeoutVarName);
        checkReferencesVariableValue(assertionPolicy, queryTimeoutVarName, queryTimeoutValue);
    }

    @Test
    public void testCreateAuditLookupPolicyJdbcValues() throws Exception {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", null, false);
        String theXml = ExternalAuditsCommonUtils.makeDefaultAuditLookupPolicyXml(connectionName,connectionDriverClass,auditRecordTable,auditDetailTable);
        policy.setXml(theXml);
        Assertion assertionPolicy = wspReader.parsePermissively( theXml, INCLUDE_DISABLED);

        allJdbcQueryReferencesVariable(assertionPolicy,queryTimeoutVarName);
        checkReferencesVariableValue(assertionPolicy, queryTimeoutVarName, queryTimeoutValue);
    }

    // check all jdbc query assertions reference same context variable
    // must be done inside the jdbc query assertion package
    protected void allJdbcQueryReferencesVariable(Assertion assertionPolicy, String contextVarName) throws Exception {
        if(assertionPolicy instanceof JdbcQueryAssertion){
            String queryTimeoutVal = ((JdbcQueryAssertion) assertionPolicy).getQueryTimeout();
            assertFalse("Query timeout not set", queryTimeoutVal!=null && queryTimeoutVal.isEmpty());
            assertEquals("Unexpected context variable name",queryTimeoutVal,"${"+contextVarName+"}");
        }
        if(assertionPolicy instanceof CompositeAssertion){
            for (Assertion child : ((CompositeAssertion) assertionPolicy).getChildren()) {
                allJdbcQueryReferencesVariable(child,contextVarName);
            }
        }
    }

    // check referenced variable value
    protected void checkReferencesVariableValue(Assertion assertionPolicy, String contextVarName, int value){
        for (Assertion child : ((CompositeAssertion) assertionPolicy).getChildren()) {
            if(child instanceof SetVariableAssertion){
                SetVariableAssertion ass = (SetVariableAssertion)child;
                if(ass.getVariableToSet().equals(contextVarName)){
                    assertEquals("Unexpected context variable value",ass.expression(),Integer.toString(value));
                    return;
                }
            }
        }
        assertTrue("Context variable not defined",false);
    }
}
