package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ContentTypeAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Server assertion to validate the syntax of a message's content type.
 */
public class ServerContentTypeAssertion extends AbstractMessageTargetableServerAssertion<ContentTypeAssertion> {

    private final Integer fixedMessagePartNum;
    private final ContentTypeHeader fixedContentType;
    private final String[] varsUsed;

    public ServerContentTypeAssertion(final ContentTypeAssertion assertion) throws PolicyAssertionException {
        super(assertion, assertion);

        if (assertion.isChangeContentType() && Syntax.getReferencedNames(assertion.getNewContentTypeValue()).length < 1) {
            fixedContentType = ContentTypeHeader.create(assertion.getNewContentTypeValue());
        } else {
            fixedContentType = null;
        }

        if (assertion.isMessagePart() && Syntax.getReferencedNames(assertion.getMessagePartNum()).length < 1) {
            try {
                fixedMessagePartNum = Integer.parseInt(assertion.getMessagePartNum());
            } catch (NumberFormatException e) {
                throw new PolicyAssertionException(assertion, "Invalid message part number");
            }
        } else {
            fixedMessagePartNum = null;
        }

        varsUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        final Map<String,?> varMap = context.getVariableMap(varsUsed, getAudit());

        try {
            if (!assertion.isChangeContentType())
                return doValidate(message, messageDescription, varMap);

            final ContentTypeHeader ctype = getContentType(varMap);

            if (assertion.isMessagePart()) {
                final int partNum = getMessagePartNum(varMap);
                PartInfo partInfo = message.getMimeKnob().getPart(partNum);
                partInfo.setContentType(ctype);
                return AssertionStatus.NONE;
            } else {
                message.getMimeKnob().setOuterContentType(ctype);
                return AssertionStatus.NONE;
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, messageDescription, assertion.getMessagePartNum());
            return AssertionStatus.FAILED;
        }
    }

    private ContentTypeHeader getContentType(Map<String, ?> varMap) {
        if (fixedContentType != null)
            return fixedContentType;

        return ContentTypeHeader.create(ExpandVariables.process(assertion.getNewContentTypeValue(), varMap, getAudit()));
    }

    private int getMessagePartNum(Map<String, ?> varMap) throws NoSuchPartException {
        if (fixedMessagePartNum != null)
            return fixedMessagePartNum;

        final String partNumStr = assertion.getMessagePartNum();
        try {
            return Integer.parseInt(ExpandVariables.process(partNumStr, varMap, getAudit()));
        } catch (NumberFormatException e) {
            throw new NoSuchPartException();
        }
    }

    private AssertionStatus doValidate(Message message, String messageDescription, Map<String, ?> varMap) throws IOException, NoSuchPartException {
        final ContentTypeHeader ctype;
        if (assertion.isMessagePart()) {
            int partNum = getMessagePartNum(varMap);
            ctype = message.getMimeKnob().getPart(partNum).getContentType();
        } else {
            ctype = message.getMimeKnob().getOuterContentType();
        }

        try {
            ctype.validate();
            return AssertionStatus.NONE;
        } catch (IOException e) {
            logAndAudit(AssertionMessages.MESSAGE_BAD_CONTENT_TYPE, messageDescription, ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
    }
}
