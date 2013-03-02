package com.l7tech.external.assertions.manipulatemultivaluedvariable.server;

import static org.junit.Assert.*;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.manipulatemultivaluedvariable.ManipulateMultiValuedVariableAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * Test the ManipulateMultiValuedVariableAssertion.
 */
public class ServerManipulateMultiValuedVariableAssertionTest {

    /**
     * Test creating a multi valued variable.
     *
     * Nothing is set in the PEC, this also tests the case when the value does not exist.
     * @throws Exception
     */
    @Test
    public void testSuccess_CreateVariable_NoValueExists() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ass.setVariableValue("myVar");

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);

        assertEquals(AssertionStatus.NONE, status);

        final Object myMultiVar = ctx.getVariable("myMultiVar");
        assertNotNull(myMultiVar);
        assertTrue(myMultiVar instanceof List);
        List<Object> myList = (List<Object>) myMultiVar;
        assertTrue(myList.isEmpty());

        for (String s : testAudit) {
            System.out.println(s);
        }
        testAudit.isAuditPresentContaining("Created variable myMultiVar");
    }

    /**
     * Test the most normal success case - the variable is create and a value is added.
     */
    @Test
    public void testSuccess_VarCreatedAndValuesAdded() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ass.setVariableValue("myVar");
        final Date myDate = new Date();
        ctx.setVariable("myVar", myDate);

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);

        assertEquals(AssertionStatus.NONE, status);

        final Object myMultiVar = ctx.getVariable("myMultiVar");
        assertNotNull(myMultiVar);
        assertTrue(myMultiVar instanceof List);
        List<Object> myList = (List<Object>) myMultiVar;
        assertFalse(myList.isEmpty());
        assertEquals(1, myList.size());

        assertEquals(myDate, myList.get(0));
        for (String s : testAudit) {
            System.out.println(s);
        }
        testAudit.isAuditPresentContaining("Added to variable myMultiVar value");
    }

    /**
     * Test a common success case - the value already exists and a new value is added to it.
     */
    @Test
    public void testSuccess_VarReusedAndValuesAdded() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        final ArrayList<Object> myList = new ArrayList<>();
        final Date myDate = new Date();
        myList.add(myDate);
        ctx.setVariable("myMultiVar", myList);
        ass.setVariableValue("myVar");

        final Date myOtherDate = new Date();
        ctx.setVariable("myVar", myOtherDate);

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);

        assertEquals(AssertionStatus.NONE, status);

        final Object myMultiVar = ctx.getVariable("myMultiVar");
        assertNotNull(myMultiVar);
        assertTrue(myMultiVar instanceof List);
        List<Object> updatedList = (List<Object>) myMultiVar;
        assertFalse(updatedList.isEmpty());
        assertEquals(2, updatedList.size());

        assertEquals(myDate, updatedList.get(0));
        assertEquals(myOtherDate, updatedList.get(1));
        for (String s : testAudit) {
            System.out.println(s);
        }

        // ensure no misleading audit messages
        assertFalse(testAudit.isAuditPresentContaining("Created variable myMultiVar"));

        assertTrue(testAudit.isAuditPresentContaining("Used existing variable myMultiVar"));
        assertTrue(testAudit.isAuditPresentContaining("Added to variable myMultiVar value"));
    }

    /**
     * Tests that all supported types are supported.
     *
     * Also tests that we can add a multi valued variable to an existing multi valued variable.
     *
     */
    @Test
    public void testSuccess_ValidateValueTypes() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ctx.setVariable("myMultiVar", new ArrayList<Object>());


        ass.setVariableValue("myVar");
        final ArrayList<Object> myValidTypesList = new ArrayList<Object>(Arrays.asList(
                new String(), new Integer(0), new Double(0), new Float(0), new Boolean(true), new Date()
        ));

        ctx.setVariable("myVar", myValidTypesList);

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);

        assertEquals(AssertionStatus.NONE, status);

        final Object myMultiVar = ctx.getVariable("myMultiVar");
        assertNotNull(myMultiVar);
        assertTrue(myMultiVar instanceof List);
        List<Object> updatedList = (List<Object>) myMultiVar;
        assertFalse(updatedList.isEmpty());
        assertEquals(6, updatedList.size());

        assertTrue(updatedList.get(0) instanceof String);
        assertTrue(updatedList.get(1) instanceof Integer);
        assertTrue(updatedList.get(2) instanceof Double);
        assertTrue(updatedList.get(3) instanceof Float);
        assertTrue(updatedList.get(4) instanceof Boolean);
        assertTrue(updatedList.get(5) instanceof Date);

        for (String s : testAudit) {
            System.out.println(s);
        }

        // ensure no misleading audit messages
        assertFalse(testAudit.isAuditPresentContaining("Created variable myMultiVar"));

        assertTrue(testAudit.isAuditPresentContaining("Used existing variable myMultiVar"));
        assertTrue(testAudit.isAuditPresentContaining("Added to variable myMultiVar value"));
    }

    @Test
    public void testValuesCloned() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ctx.setVariable("myMultiVar", new ArrayList<Object>());


        ass.setVariableValue("myVar");
        final ArrayList<Object> myValidTypesList = new ArrayList<Object>(Arrays.asList(
                new String(), new Integer(0), new Double(0), new Float(0), new Boolean(true), new Date()
        ));

        ctx.setVariable("myVar", myValidTypesList);

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);

        assertEquals(AssertionStatus.NONE, status);

        final Object myMultiVar = ctx.getVariable("myMultiVar");
        assertNotNull(myMultiVar);
        assertTrue(myMultiVar instanceof List);
        List<Object> updatedList = (List<Object>) myMultiVar;
        assertFalse(updatedList.isEmpty());
        assertEquals(6, updatedList.size());

        assertFalse(updatedList.get(0) == myValidTypesList.get(0));
        assertFalse(updatedList.get(1) == myValidTypesList.get(1));
        assertFalse(updatedList.get(2) == myValidTypesList.get(2));
        assertFalse(updatedList.get(3) == myValidTypesList.get(3));
        assertFalse(updatedList.get(4) == myValidTypesList.get(4));
        assertFalse(updatedList.get(5) == myValidTypesList.get(5));

        for (String s : testAudit) {
            System.out.println(s);
        }

        // ensure no misleading audit messages
        assertFalse(testAudit.isAuditPresentContaining("Created variable myMultiVar"));

        assertTrue(testAudit.isAuditPresentContaining("Used existing variable myMultiVar"));
        assertTrue(testAudit.isAuditPresentContaining("Added to variable myMultiVar value"));
    }


    @Test
    public void testError_VarExistsAndIsNotMultiValuedList() throws Exception {
        PolicyEnforcementContext ctx = getCtx();
        ctx.setVariable("myMultiVar", new Date());

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ass.setVariableValue("myVar");

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FALSIFIED, status);
        testAudit.isAuditPresentContaining("Variable myMultiVar is not a multi valued variable");
    }

    /**
     * Validate that the assertion fails gracefully when the target variable is not modifiable.
     */
    @Test
    public void testError_UnmodifiableTargetVariable() throws Exception {
        PolicyEnforcementContext ctx = getCtx();
        ctx.setVariable("myMultiVar", Arrays.asList("my unmodifiable list"));

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ass.setVariableValue("myVar");
        ctx.setVariable("myVar", "a value");

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresentContaining("Could not add variable value to variable myMultiVar due to : java.lang.UnsupportedOperationException"));
    }

    @Test
    public void testError_CannotSetAnUnsupportedType() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setVariableName("myMultiVar");
        ass.setVariableValue("myVar");
        ctx.setVariable("myVar", TestDocuments.getWssInteropAliceCert());

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertTrue(testAudit.isAuditPresentContaining("Type class sun.security.x509.X509CertImpl is not supported. Cannot add it to multi valued variable."));

    }

    private PolicyEnforcementContext getCtx() throws SAXException {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(XmlUtil.parse("<xml/>")), new Message(XmlUtil.parse("<xml/>")));
    }

}
