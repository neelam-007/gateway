package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageBufferingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Server implementation for {@link MessageBufferingAssertion}.
 */
public class ServerMessageBufferingAssertion extends AbstractMessageTargetableServerAssertion<MessageBufferingAssertion> {
    public ServerMessageBufferingAssertion(final @NotNull MessageBufferingAssertion assertion) {
        super(assertion);
    }

    public ServerMessageBufferingAssertion(final @NotNull MessageBufferingAssertion assertion, final @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
        throws IOException, PolicyAssertionException
    {
        if (!message.isInitialized()) {
            // Currently can't handle an uninitialized message, though it would nice to be able to flag that (say) a response
            // should not be buffered, before the routing assertion is even run to initialize it
            getAudit().logAndAudit(AssertionMessages.MESSAGE_NOT_INITIALIZED, messageDescription);
            return AssertionStatus.SERVER_ERROR;
        }

        if (assertion.isNeverBuffer()) {
            // Set flag first, then check if we were too late
            message.getMimeKnob().setBufferingDisallowed(true);

            if (message.getMimeKnob().getFirstPart().isBodyStashed()) {
                getAudit().logAndAudit(AssertionMessages.MESSAGE_ALREADY_BUFFERED, messageDescription);
                return AssertionStatus.FAILED;
            }
        }

        if (assertion.isAlwaysBuffer()) {
            message.getMimeKnob().setBufferingDisallowed(false);
            InputStream stream = null;
            try {
                // Force stashing to occur now, for entire message including attachments
                stream = message.getMimeKnob().getEntireMessageBodyAsInputStream(false);
                IOUtils.copyStream(stream, new NullOutputStream());
            } catch (NoSuchPartException e) {
                getAudit().logAndAudit(AssertionMessages.NO_SUCH_PART, messageDescription, String.valueOf(e.getOrdinal()));
                return AssertionStatus.SERVER_ERROR;
            } finally {
                ResourceUtils.closeQuietly(stream);
            }
        }

        return AssertionStatus.NONE;
    }
}
