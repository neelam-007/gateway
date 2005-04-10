package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
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
 * Extracts fields from HTTP POSTed forms and replaces MIME parts in the current
 * message.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current message and replaces it
 * with new content!
 */
public class ServerHttpFormPost implements ServerAssertion {
    private static Logger logger = Logger.getLogger(ServerHttpFormPost.class.getName());
    private final Auditor auditor;
    private final HttpFormPost assertion;

    public ServerHttpFormPost(HttpFormPost assertion, ApplicationContext springContext) {
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob reqMime = request.getMimeKnob();
        ContentTypeHeader ctype = reqMime.getOuterContentType();
        HttpRequestKnob reqHttp = (HttpRequestKnob) request.getKnob(HttpRequestKnob.class);
        if ( !("application".equals(ctype.getType()) &&
                "x-www-form-urlencoded".equals(ctype.getSubtype()))) {
            auditor.logAndAudit(AssertionMessages.HTTPFORM_WRONG_TYPE, new String[] { ctype.getFullValue() });
            return AssertionStatus.NOT_APPLICABLE;
        }

        if (reqHttp == null) {
            auditor.logAndAudit(AssertionMessages.HTTPFORM_NON_HTTP);
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
                continue;
            } else if (partValues.length > 1) {
                auditor.logAndAudit(AssertionMessages.HTTPFORM_MULTIVALUE, new String[] { partFieldname });
                continue;
            }

            String partValue = partValues[0];
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
