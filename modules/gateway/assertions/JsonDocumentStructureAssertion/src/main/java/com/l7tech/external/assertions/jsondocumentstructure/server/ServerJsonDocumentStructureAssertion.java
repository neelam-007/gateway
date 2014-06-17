package com.l7tech.external.assertions.jsondocumentstructure.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.jsondocumentstructure.JsonDocumentStructureAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.l7tech.external.assertions.jsondocumentstructure.server.JsonDocumentStructureValidationException.ConstraintViolation;

/**
 * Server side implementation of the JsonDocumentStructureAssertion.
 *
 * @see com.l7tech.external.assertions.jsondocumentstructure.JsonDocumentStructureAssertion
 */
public class ServerJsonDocumentStructureAssertion extends AbstractMessageTargetableServerAssertion<JsonDocumentStructureAssertion> {
    private final JsonDocumentStructureValidator documentStructureValidator;

    public ServerJsonDocumentStructureAssertion(final JsonDocumentStructureAssertion assertion) {
        super(assertion);

        documentStructureValidator = new JsonDocumentStructureValidator();
        documentStructureValidator.setMaxContainerDepth(assertion.getMaxContainerDepth());
        documentStructureValidator.setCheckContainerDepth(assertion.isCheckContainerDepth());
        documentStructureValidator.setMaxArrayEntryCount(assertion.getMaxArrayEntryCount());
        documentStructureValidator.setCheckArrayEntryCount(assertion.isCheckArrayEntryCount());
        documentStructureValidator.setMaxObjectEntryCount(assertion.getMaxObjectEntryCount());
        documentStructureValidator.setCheckObjectEntryCount(assertion.isCheckObjectEntryCount());
        documentStructureValidator.setMaxEntryNameLength(assertion.getMaxEntryNameLength());
        documentStructureValidator.setCheckEntryNameLength(assertion.isCheckEntryNameLength());
        documentStructureValidator.setMaxStringValueLength(assertion.getMaxStringValueLength());
        documentStructureValidator.setCheckStringValueLength(assertion.isCheckStringValueLength());
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message msg,
                                             final String targetName,
                                             final AuthenticationContext authContext) 
            throws IOException, PolicyAssertionException {
        final MimeKnob mimeKnob = msg.getKnob(MimeKnob.class);

        if (mimeKnob == null || !msg.isInitialized()) {
            // Uninitialized target message
            logAndAudit(AssertionMessages.MESSAGE_NOT_INITIALIZED, assertion.getTargetName());
            return getBadMessageStatus();
        }

        /**
         * If the message doesn't have 'application/json' content type, it is ignored.
         * This is consistent with the OversizedTextAssertion on which this assertion is based.
         */
        if (!msg.isJson()) {
            logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_NOT_JSON, targetName);
            return AssertionStatus.NOT_APPLICABLE;
        }

        try (InputStream messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream()) {
            documentStructureValidator.validate(messageBodyStream);
        } catch (InvalidJsonException e) { // poorly-formed JSON
            logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_TARGET_INVALID_JSON, assertion.getTargetName());
            return getBadMessageStatus();
        } catch (JsonDocumentStructureValidationException e) { // structure constraints violated
            logConstraintViolation(e.getViolation(), Integer.toString(e.getLine()));
            return AssertionStatus.FALSIFIED;
        } catch (IOException e) {
            logAndAudit(Messages.EXCEPTION_SEVERE_WITH_MORE_INFO, e.getMessage());
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, new String[] {assertion.getTargetName(), e.getWhatWasMissing()},
                    ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private void logConstraintViolation(ConstraintViolation violation, String line) {
        switch (violation) {
            case CONTAINER_DEPTH:
                logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_CONTAINER_DEPTH_EXCEEDED, line);
                break;
            case OBJECT_ENTRY_COUNT:
                logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_OBJECT_ENTRY_COUNT_EXCEEDED, line);
                break;
            case ARRAY_ENTRY_COUNT:
                logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_ARRAY_ENTRY_COUNT_EXCEEDED, line);
                break;
            case ENTRY_NAME_LENGTH:
                logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_ENTRY_NAME_LENGTH_EXCEEDED, line);
                break;
            case STRING_VALUE_LENGTH:
                logAndAudit(AssertionMessages.JSON_THREAT_PROTECTION_STRING_VALUE_LENGTH_EXCEEDED, line);
                break;
        }
    }
}
