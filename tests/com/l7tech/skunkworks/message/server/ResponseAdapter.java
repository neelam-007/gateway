/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
public class ResponseAdapter extends XmlMessageAdapterAdapter implements XmlResponse {
    public ResponseAdapter(MessageContext context) {
        super(context, context.getResponse());
        MessageProcessor.setCurrentResponse(this);
    }

    public void addResult(AssertionResult result) {
        context.addResult(result);
    }

    public void setFaultDetail(SoapFaultDetail sfd) {
        context.setFaultDetail(sfd);
    }

    public SoapFaultDetail getFaultDetail() {
        return context.getFaultDetail();
    }

    public Iterator results() {
        return context.results();
    }

    public Iterator resultsWithStatus(AssertionStatus status) {
        return resultsWithStatus(new AssertionStatus[] { status });
    }

    public Iterator resultsWithStatus(AssertionStatus[] statuses) {
        Iterator results = results();
        List out = new ArrayList();
        while (results.hasNext()) {
            AssertionResult result = (AssertionResult)results.next();
            for (int j = 0; j < statuses.length; j++) {
                AssertionStatus status = statuses[j];
                if (result.getStatus() == status) {
                    out.add(result);
                    break;
                }
            }
        }
        return out.iterator();
    }

    public boolean isAuthenticationMissing() {
        return context.isAuthenticationMissing();
    }

    public void setAuthenticationMissing(boolean authMissing) {
        context.setAuthenticationMissing(authMissing);
    }

    public boolean isPolicyViolated() {
        return context.isPolicyViolated();
    }

    public void setPolicyViolated(boolean policyViolated) {
        context.setPolicyViolated(policyViolated);
    }
}
