package com.l7tech.external.assertions.manipulatemultivaluedvariable.server;

import static com.l7tech.gateway.common.audit.AssertionMessages.*;
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
     * Test the most normal success case - the variable is create and a value is added.
     */
    @Test
    public void testSuccess_VarCreatedAndValuesAdded() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVar");
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

        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertTrue(testAudit.isAuditPresentContaining("Appended to Target Multivalued variable 'myMultiVar' value"));
        validateAuditLevels(testAudit, USERDETAIL_FINEST);
    }

    /**
     * Test a common success case - the value already exists and a new value is added to it.
     */
    @Test
    public void testSuccess_VarReusedAndValuesAdded() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        final ArrayList<Object> myList = new ArrayList<>();
        final Date myDate = new Date();
        myList.add(myDate);
        ctx.setVariable("myMultiVar", myList);
        ass.setSourceVariableName("myVar");

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
        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertFalse(testAudit.isAuditPresentContaining("Created"));

        assertTrue(testAudit.isAuditPresentContaining("Target variable 'myMultiVar' already exists."));
        assertTrue(testAudit.isAuditPresentContaining("Appended to Target Multivalued variable 'myMultiVar' value '"));

        validateAuditLevels(testAudit, USERDETAIL_FINEST);
    }

    /**
     * Test creating a multi valued variable. This supports users who think they need to 'declare' the variable first.
     *
     * Nothing is set in the PEC, this also tests the case when the source variable value does not exist.
     */
    @Test
    public void testSuccess_CreateVariable_NoValueExists() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVar");

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
        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertTrue(testAudit.isAuditPresentContaining("Created Target Multivalued variable myMultiVar"));
        validateAuditLevels(testAudit, USERDETAIL_FINEST);
    }

    /**
     * Tests that the source variable supports array syntax. aka 'syntax'.
     */
    @Test
    public void testSuccess_SourceVariableSupportsSyntax() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVar[0]");
        ctx.setVariable("myVar", "First value");

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

        assertTrue(myList.get(0) instanceof String);
        assertEquals("First value", myList.get(0));

        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertTrue(testAudit.isAuditPresentContaining("Created Target Multivalued variable myMultiVar"));
        assertTrue(testAudit.isAuditPresentContaining("Appended to Target Multivalued variable 'myMultiVar' value 'First value'"));
        validateAuditLevels(testAudit, USERDETAIL_FINEST);

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
        ass.setTargetVariableName("myMultiVar");
        ctx.setVariable("myMultiVar", new ArrayList<Object>());


        ass.setSourceVariableName("myVar");
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
        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertFalse(testAudit.isAuditPresentContaining("Created variable myMultiVar"));

        assertTrue(testAudit.isAuditPresentContaining("Target variable 'myMultiVar' already exists"));
        assertTrue(testAudit.isAuditPresentContaining("Appended to Target Multivalued variable 'myMultiVar' value '"));
        validateAuditLevels(testAudit, USERDETAIL_FINEST);
    }

    @Test
    public void testValuesCloned() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ctx.setVariable("myMultiVar", new ArrayList<Object>());


        ass.setSourceVariableName("myVar");
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
        assertTrue(testAudit.isAuditPresent(USERDETAIL_FINEST));
        assertFalse(testAudit.isAuditPresentContaining("Created variable myMultiVar"));

        assertTrue(testAudit.isAuditPresentContaining("Target variable 'myMultiVar' already exists"));
        assertTrue(testAudit.isAuditPresentContaining("Appended to Target Multivalued variable 'myMultiVar' value '"));
        validateAuditLevels(testAudit, USERDETAIL_FINEST);
    }


    @Test
    public void testError_VarExistsAndIsNotMultiValuedList() throws Exception {
        PolicyEnforcementContext ctx = getCtx();
        ctx.setVariable("myMultiVar", new Date());

        // Context contains nothing
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVar");
        ctx.setVariable("myVar", "a value"); // not used, just used to avoid unnecessary audit

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FAILED, status);
        testAudit.isAuditPresent(USERDETAIL_WARNING);
        testAudit.isAuditPresentContaining("Variable myMultiVar is not a supported Multivalued variable. Variable must be backed by a List.");
        validateAuditLevels(testAudit, USERDETAIL_WARNING);
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
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVar");
        ctx.setVariable("myVar", "a value");

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(USERDETAIL_WARNING));
        assertTrue(testAudit.isAuditPresentContaining("Target variable 'myMultiVar' already exists"));
        assertTrue(testAudit.isAuditPresentContaining("Could not append value to Target Multivalued variable 'myMultiVar' due to : java.lang.UnsupportedOperationException"));
        validateAuditLevels(testAudit, USERDETAIL_WARNING, USERDETAIL_FINEST);
    }

    @Test
    public void testError_CannotSetAnUnsupportedType() throws Exception {
        PolicyEnforcementContext ctx = getCtx();

        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
//        ctx.setVariable("myMultiVar", new ArrayList());
        ass.setSourceVariableName("myVar");
        ctx.setVariable("myVar", TestDocuments.getWssInteropAliceCert());

        ServerManipulateMultiValuedVariableAssertion serverAssertion = new ServerManipulateMultiValuedVariableAssertion(ass);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus status = serverAssertion.checkRequest(ctx);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertTrue(testAudit.isAuditPresent(USERDETAIL_WARNING));
        assertTrue(testAudit.isAuditPresentContaining("Type class sun.security.x509.X509CertImpl is not supported. Cannot add it to target Multivalued variable."));
        validateAuditLevels(testAudit, USERDETAIL_WARNING, USERDETAIL_FINEST);
    }

    //- PRIVATE

    public static final List<M> ALL_USERDETAIL_AUDITS = Arrays.asList(USERDETAIL_FINEST, USERDETAIL_FINER, USERDETAIL_FINE, USERDETAIL_INFO, USERDETAIL_WARNING);

    private void validateAuditLevels(TestAudit testAudit, M ... allowedAudits) {
        final List<M> allowed = Arrays.asList(allowedAudits);

        for (M audit : ALL_USERDETAIL_AUDITS) {
            if (!allowed.contains(audit)) {
                assertFalse("Unexpected User Detail audit found with level '" + audit.getLevelName() + "'", testAudit.isAuditPresent(audit));
            }
        }
    }

    private PolicyEnforcementContext getCtx() throws SAXException {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(XmlUtil.parse("<xml/>")), new Message(XmlUtil.parse("<xml/>")));
    }

}
