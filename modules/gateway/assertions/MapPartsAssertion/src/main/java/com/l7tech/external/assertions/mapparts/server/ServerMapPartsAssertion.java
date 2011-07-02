package com.l7tech.external.assertions.mapparts.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.external.assertions.mapparts.MapPartsAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server side implementation of the MapPartsAssertion.
 *
 * @see com.l7tech.external.assertions.mapparts.MapPartsAssertion
 */
public class ServerMapPartsAssertion extends AbstractServerAssertion<MapPartsAssertion> {

    public ServerMapPartsAssertion(MapPartsAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            List<String> ret = new ArrayList<String>();

            PartIterator i = context.getRequest().getMimeKnob().getParts();
            while (i.hasNext()) {
                PartInfo pi = i.next();
                ret.add(pi.getContentId(true));
                pi.getInputStream(false).close(); // work around bug present in 4.2
            }

            String[] partIds = ret.toArray(new String[ret.size()]);
            context.setVariable(MapPartsAssertion.REQUEST_PARTS_CONTENT_IDS, partIds);

            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Unable to enumerate parts: " + ExceptionUtils.getMessage( e ) }, e );
            throw new IOException(e); // normally not possible
        }
    }
}
