package com.l7tech.proxy;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.util.List;

/**
 * A RequestInterceptor that ignores all events.
 */
public class NullRequestInterceptor implements RequestInterceptor {
    public static final NullRequestInterceptor INSTANCE = new NullRequestInterceptor();

    public NullRequestInterceptor() {
    }

    public void onFrontEndRequest(PolicyApplicationContext context) {
    }

    public void onFrontEndReply(PolicyApplicationContext context) {
    }

    public void onBackEndRequest(PolicyApplicationContext context, List<HttpHeader> headersSent) {
    }

    public void onBackEndReply(PolicyApplicationContext context) {
    }

    public void onMessageError(final Throwable t) {
    }

    public void onReplyError(final Throwable t) {
    }

    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
    }

    public void onPolicyError(Ssg ssg, PolicyAttachmentKey binding, Throwable error) {
    }
}
