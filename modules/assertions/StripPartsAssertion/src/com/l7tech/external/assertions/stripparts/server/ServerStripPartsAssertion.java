package com.l7tech.external.assertions.stripparts.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.external.assertions.stripparts.StripPartsAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Server side implementation of the StripPartsAssertion.
 *
 * @see com.l7tech.external.assertions.stripparts.StripPartsAssertion
 */
public class ServerStripPartsAssertion extends AbstractServerAssertion<StripPartsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerStripPartsAssertion.class.getName());

    private final StashManagerFactory stashManagerFactory;
    private final Auditor auditor;

    public ServerStripPartsAssertion(StripPartsAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = new Auditor(this, context, logger);
        this.stashManagerFactory = (StashManagerFactory)context.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        StashManager sm = null;
        try {
            Message message = assertion.isActOnRequest() ? context.getRequest() : context.getResponse();
            sm = stashXmlPart(message);
            ContentTypeHeader ctype = message.getMimeKnob().getFirstPart().getContentType();

            if (sm.isByteArrayAvailable(0)) {
                byte[] bytes = sm.recallBytes(0);
                message.initialize(ctype, bytes);
            } else {
                message.initialize(stashManagerFactory.createStashManager(), ctype, sm.recall(0));
            }

            return AssertionStatus.NONE;
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to strip parts: " + ExceptionUtils.getMessage(e) },
                                e);
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to strip parts: " + ExceptionUtils.getMessage(e) },
                                e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (sm != null) sm.close();
        }
    }

    // @return the message stashed as ordinal zero in a new StashManager.  Never null.  Caller must close it.
    private StashManager stashXmlPart(Message message) throws IOException, NoSuchPartException {
        byte[] bytes = message.getMimeKnob().getFirstPart().getBytesIfAlreadyAvailable();
        if (bytes != null) {
            StashManager sm = new ByteArrayStashManager();
            sm.stash(0, bytes);
            return sm;
        }

        InputStream is = null;
        try {
            StashManager sm = stashManagerFactory.createStashManager();
            is = message.getMimeKnob().getFirstPart().getInputStream(true);
            sm.stash(0, is);
            return sm;
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }
}
