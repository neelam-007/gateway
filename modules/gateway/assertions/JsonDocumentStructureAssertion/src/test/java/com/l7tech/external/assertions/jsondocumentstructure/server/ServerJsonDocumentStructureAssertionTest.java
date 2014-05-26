package com.l7tech.external.assertions.jsondocumentstructure.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.jsondocumentstructure.JsonDocumentStructureAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;

import static com.l7tech.external.assertions.jsondocumentstructure.server.JsonDocumentStructureTestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the JsonDocumentStructureAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerJsonDocumentStructureAssertionTest {
    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private StashManagerFactory stashManagerFactory;

    private StashManager stashManager;
    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        testAudit = new TestAudit();
        stashManager = stashManagerFactory.createStashManager();

        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * When a valid JSON document is within all document structure constraints, the assertion should pass and
     * will not audit any messages.
     */
    @Test
    public void doCheckRequest_AllConstraintsEnabledButNoneViolated_AssertionPasses() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxContainerDepth(20);
        assertion.setCheckContainerDepth(true);
        assertion.setMaxContainerDepth(20);
        assertion.setCheckContainerDepth(true);
        assertion.setMaxArrayEntryCount(20);
        assertion.setCheckArrayEntryCount(true);
        assertion.setMaxObjectEntryCount(20);
        assertion.setCheckObjectEntryCount(true);
        assertion.setMaxEntryNameLength(20);
        assertion.setCheckEntryNameLength(true);
        assertion.setMaxStringValueLength(20);
        assertion.setCheckStringValueLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_OBJECT_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.NONE, status);

        // expect no audit messages
        checkAuditPresence(false, false, false, false, false, false, false);
    }

    /**
     * Given a poorly formed document that violates a constraint before the parser encounters the first unexpected
     * token, the assertion should audit the violation and return FALSIFIED, i.e. stop at the first error encountered.
     */
    @Test
    public void doCheckRequest_GivenBadJsonViolatingEntryNameLengthConstraint_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxEntryNameLength(2);
        assertion.setCheckEntryNameLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, POORLY_FORMED_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, false, false, false, true, false);
    }

    /**
     * When the container depth of the document exceeds the defined container depth constraint, the assertion
     * should audit message 10502 and return FALSIFIED.
     */
    @Test
    public void doCheckRequest_ContainerDepthConstraintViolated_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxContainerDepth(2);
        assertion.setCheckContainerDepth(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_OBJECT_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the container depth violation to be audited
        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * An empty message is not a valid JSON document - the assertion should audit message 10501 and return BAD_REQUEST.
     */
    @Test
    public void doCheckRequest_GivenEmptyMessage_ReturnsBadRequest() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxContainerDepth(20);
        assertion.setCheckContainerDepth(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST, "");

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        // expect the invalid JSON message to be audited
        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Given a document starting with a typed value followed by other tokens, the assertion should audit message
     * 10501 and return BAD_REQUEST.
     */
    @Test
    public void doCheckRequest_GivenBadSingleTypedValueDocumentAndNoConstraints_ReturnsBadRequest() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxArrayEntryCount(20);
        assertion.setCheckArrayEntryCount(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, POORLY_FORMED_SINGLE_TYPED_VALUE_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        // expect the invalid JSON message to be audited
        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Given a poorly formed document that doesn't violate any constraints before validation encounters the first
     * invalid token, the assertion should audit message 10501 and return BAD_REQUEST.
     */
    @Test
    public void doCheckRequest_GivenBadJsonSingleObjectDocumentAndNoConstraints_ReturnsBadRequest() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxStringValueLength(20);
        assertion.setCheckStringValueLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, POORLY_FORMED_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        // expect the invalid JSON message to be audited
        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * When a valid single typed value JSON document is within all document structure constraints, the assertion
     * should pass and will not audit any messages.
     */
    @Test
    public void doCheckRequest_GivenValidSingleTypedValueDocument_AssertionPasses() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxObjectEntryCount(20);
        assertion.setCheckObjectEntryCount(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_TYPED_VALUE_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.NONE, status);

        // expect no audit messages
        checkAuditPresence(false, false, false, false, false, false, false);
    }


    /**
     * When the entry count of an array in the document exceeds the defined array entry count constraint, the assertion
     * should audit message 10504 and return FALSIFIED.
     */
    @Test
    public void doCheckRequest_ArrayEntryCountConstraintViolated_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxArrayEntryCount(4);
        assertion.setCheckArrayEntryCount(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, NESTED_ARRAYS_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the array entry count violation to be audited
        checkAuditPresence(false, false, false, false, true, false, false);
    }

    /**
     * This test checks that a low array entry count constraint doesn't give a false-positive when no individual
     * array exceeds the constraint, but the total number of entries in a set of nested arrays does exceed it.
     * The assertion should pass and will not audit any messages.
     */
    @Test
    public void doCheckRequest_GivenDeepNestedArraysComplyingWithEntryConstraint_AssertionPasses() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxArrayEntryCount(5);
        assertion.setCheckArrayEntryCount(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, NESTED_ARRAYS_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.NONE, status);

        // expect no audit messages
        checkAuditPresence(false, false, false, false, false, false, false);
    }

    /**
     * When the entry count of an object in the document exceeds the defined object entry count constraint, the
     * assertion should audit message 10503 and return FALSIFIED.
     */
    @Test
    public void doCheckRequest_ObjectEntryCountConstraintViolated_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxObjectEntryCount(6);
        assertion.setCheckObjectEntryCount(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_OBJECT_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the object entry count violation to be audited
        checkAuditPresence(false, false, false, true, false, false, false);
    }

    /**
     * When the length of an entry name in the document exceeds the defined entry name length constraint, the
     * assertion should audit message 10505 and return FALSIFIED.
     */
    @Test
    public void doCheckRequest_EntryNameLengthConstraintViolated_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxEntryNameLength(10);
        assertion.setCheckEntryNameLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_OBJECT_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, false, false, false, true, false);
    }

    /**
     * When the length of an string value in the document exceeds the defined string value length constraint, the
     * assertion should audit message 10506 and return FALSIFIED.
     */
    @Test
    public void doCheckRequest_StringValueLengthConstraintViolated_AssertionFalsified() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxStringValueLength(7);
        assertion.setCheckStringValueLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext pec =
                createPolicyEnforcementContext(TargetMessageType.REQUEST, SINGLE_OBJECT_DOCUMENT);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the string value length violation to be audited
        checkAuditPresence(false, false, false, false, false, false, true);
    }

    /**
     * When the content type of the assertion's target message is not 'application/json' the assertion
     * should return NOT_APPLICABLE and not perform any validation.
     */
    @Test
    public void doCheckRequest_GivenTargetMessageWithContentTypeNotJson_ReturnsNotApplicable() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxStringValueLength(7);
        assertion.setCheckStringValueLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        // N.B. the content type header is not application/json
        Message request = new Message(stashManager,
                ContentTypeHeader.XML_DEFAULT, getInputStream(SINGLE_OBJECT_DOCUMENT));

        PolicyEnforcementContext pec =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, createResponse());

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.NOT_APPLICABLE, status);

        // expect the audit indicating the target message is not JSON
        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * When the content type of the assertion's target message is not 'application/json' the assertion
     * should return NOT_APPLICABLE and not perform any validation.
     */
    @Test
    public void doCheckRequest_GivenTargetUninitializedMessage_ReturnsBadRequest() throws Exception {
        JsonDocumentStructureAssertion assertion = new JsonDocumentStructureAssertion();
        assertion.setMaxStringValueLength(7);
        assertion.setCheckStringValueLength(true);

        ServerJsonDocumentStructureAssertion serverAssertion = createServer(assertion);

        // N.B. neither message has been initialized
        PolicyEnforcementContext pec =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(), createResponse());

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue("\"MESSAGE_NOT_INITIALIZED\" audit expected to be present.",
                testAudit.isAuditPresent(AssertionMessages.MESSAGE_NOT_INITIALIZED));

        // expect no audit messages
        checkAuditPresence(false, false, false, false, false, false, false);
    }

    /**
     * Checks presence or absence of audits to confirm the expected audits are present/not present.
     */
    private void checkAuditPresence(boolean targetMessageIsNotJson, boolean targetMessageIsInvalidJson,
                                    boolean containerDepthViolation, boolean objectEntryCountViolation,
                                    boolean arrayEntryCountViolation, boolean entryNameLengthViolation,
                                    boolean stringValueLengthViolation) {
        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_NOT_JSON.getMessage(),
                targetMessageIsNotJson,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_NOT_JSON));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_INVALID_JSON.getMessage(),
                targetMessageIsInvalidJson,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_INVALID_JSON));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_CONTAINER_DEPTH_EXCEEDED.getMessage(),
                containerDepthViolation,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_CONTAINER_DEPTH_EXCEEDED));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_OBJECT_ENTRY_COUNT_EXCEEDED.getMessage(),
                objectEntryCountViolation,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_OBJECT_ENTRY_COUNT_EXCEEDED));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_ARRAY_ENTRY_COUNT_EXCEEDED.getMessage(),
                arrayEntryCountViolation,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_ARRAY_ENTRY_COUNT_EXCEEDED));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_ENTRY_NAME_LENGTH_EXCEEDED.getMessage(),
                entryNameLengthViolation,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_ENTRY_NAME_LENGTH_EXCEEDED));

        assertEquals(AssertionMessages.JSON_THREAT_PROTECTION_STRING_VALUE_LENGTH_EXCEEDED.getMessage(),
                stringValueLengthViolation,
                testAudit.isAuditPresent(AssertionMessages.JSON_THREAT_PROTECTION_STRING_VALUE_LENGTH_EXCEEDED));
    }

    private ServerJsonDocumentStructureAssertion createServer(JsonDocumentStructureAssertion assertion) {
        ServerJsonDocumentStructureAssertion serverAssertion = new ServerJsonDocumentStructureAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(TargetMessageType targetType,
                                                                    @Nullable String jsonDocument) throws IOException {
        Message request;
        Message response;

        if (TargetMessageType.REQUEST == targetType && null != jsonDocument) {
            request = createMessageFromJsonString(jsonDocument);
        } else {
            request = createRequest();
        }

        if (TargetMessageType.RESPONSE == targetType && null != jsonDocument) {
            response = createMessageFromJsonString(jsonDocument);
        } else {
            response = createResponse();
        }

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private Message createMessageFromJsonString(String jsonDocument) throws IOException {
        return new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, getInputStream(jsonDocument));
    }

    private Message createRequest() {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();

        Message request = new Message();
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    private Message createResponse() {
        MockHttpServletResponse hResponse = new MockHttpServletResponse();

        Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));

        return response;
    }
}
