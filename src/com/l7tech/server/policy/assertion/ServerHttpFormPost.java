package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Extracts fields from an HTML form submission and constructs MIME parts in the current
 * request out of them.  The request must have been received via HTTP.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it
 * with new content!
 */
public class ServerHttpFormPost extends AbstractServerAssertion implements ServerAssertion {
    private static Logger logger = Logger.getLogger(ServerHttpFormPost.class.getName());
    private final Auditor auditor;
    private final HttpFormPost assertion;

    public ServerHttpFormPost(HttpFormPost assertion, ApplicationContext springContext) {
        super(assertion);
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob reqMime = request.getMimeKnob();
        ContentTypeHeader ctype = reqMime.getOuterContentType();
        HttpRequestKnob reqHttp = (HttpRequestKnob) request.getKnob(HttpRequestKnob.class);

        if (reqHttp == null) {
            auditor.logAndAudit(AssertionMessages.HTTPFORM_NON_HTTP);
            return AssertionStatus.NOT_APPLICABLE;
        } else if (GenericHttpClient.METHOD_POST.equalsIgnoreCase(reqHttp.getMethod()) &&
                    ctype.isApplication() && HttpFormPost.X_WWW_FORM_URLENCODED.equals(ctype.getSubtype())) {
            logger.fine("Received POST form");
        } else if (GenericHttpClient.METHOD_GET.equalsIgnoreCase(reqHttp.getMethod()) &&
                           reqHttp.getQueryString() != null && reqHttp.getQueryString().length() > 0) {
            logger.fine("Received GET form");
        } else {
            auditor.logAndAudit(AssertionMessages.HTTPFORM_WRONG_TYPE, new String[] { "Content-Type was " + ctype.getFullValue() });
            return AssertionStatus.NOT_APPLICABLE;
        }

        byte[][] parts = new byte[assertion.getFieldInfos().length][];
        String[] contentTypes = new String[assertion.getFieldInfos().length];

        for (int i = 0; i < assertion.getFieldInfos().length; i++) {
            String partFieldname = assertion.getFieldInfos()[i].getFieldname();
            String partCtype = assertion.getFieldInfos()[i].getContentType();
            ContentTypeHeader partContentType = ContentTypeHeader.parseValue(partCtype);
            String[] partValues = reqHttp.getParameterValues(partFieldname);

            if (partValues == null) {
                auditor.logAndAudit(AssertionMessages.HTTPFORM_NO_SUCH_FIELD, new String[] { partFieldname });
                return AssertionStatus.FAILED;
            } else if (partValues.length > 1) {
                auditor.logAndAudit(AssertionMessages.HTTPFORM_MULTIVALUE, new String[] { partFieldname });
                continue;
            }

            String partValue = partValues[0];
            if (partValue.length() >= 512 * 1024) {
                // TODO do we care about bytes vs. chars?
                auditor.logAndAudit(AssertionMessages.HTTPFORM_TOO_BIG, new String[] {partFieldname});
                return AssertionStatus.FAILED;
            }

            String encoding = partContentType.getEncoding();
            if (encoding == null) encoding = MimeUtil.DEFAULT_ENCODING;
            parts[i] = partValue.getBytes(encoding);
            contentTypes[i] = partContentType.getFullValue();
        }

        byte[] newMessageBytes;
        String outerContentType;
        if (parts.length == 1) {
            newMessageBytes = parts[0];
            outerContentType = contentTypes[0];
        } else {
            byte[] boundary = MimeUtil.randomBoundary();
            newMessageBytes = MimeUtil.makeMultipartMessage(boundary, parts, contentTypes);
            outerContentType = "multipart/related; boundary=\"" + new String(boundary) + "\"";
        }

        try {
            request.initialize(StashManagerFactory.createStashManager(), ContentTypeHeader.parseValue(outerContentType), new ByteArrayInputStream(newMessageBytes));
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.HTTPFORM_BAD_MIME, null, e);
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }
}
